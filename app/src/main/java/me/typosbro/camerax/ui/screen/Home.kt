package me.typosbro.camerax.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.typosbro.camerax.MainViewModel
import me.typosbro.camerax.ui.component.CameraPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(context: Context) {
    val score = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE or CameraController.IMAGE_ANALYSIS)
        }
    }
    val viewModel = viewModel<MainViewModel>()
    val bitmaps by viewModel.bitmaps.collectAsState()

    val modifier = Modifier


    fun takePhoto(controller: LifecycleCameraController, onPhotoTaken: (Bitmap) -> Unit) {
        controller.takePicture(
            ContextCompat.getMainExecutor(context),
            object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
//                        postScale(-1f, 1f)
                    }

                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )

                    onPhotoTaken(rotatedBitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    exception.printStackTrace()
                }
            })
    }

    fun openGallery() {
        score.launch {
            scaffoldState.bottomSheetState.expand()
        }
    }

    HomeUi(
        scaffoldState = scaffoldState,
        controller = controller,
        modifier = modifier,
        bitmaps = bitmaps,
        onTakePhoto = {
            takePhoto(
                controller = controller,
                onPhotoTaken = viewModel::onTakePhoto
            )
        },
        onOpenGallery = { openGallery() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeUi(
    scaffoldState: BottomSheetScaffoldState,
    controller: LifecycleCameraController,
    onTakePhoto: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
    bitmaps: List<Bitmap> = emptyList(),
) {
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        content = {
            Content(
                it,
                controller,
                onTakePhoto = onTakePhoto,
                onOpenGallery = onOpenGallery
            )
        },
        sheetContent = { SheetContent(bitmaps = bitmaps, modifier = modifier.fillMaxWidth()) },
        modifier = modifier,
    )
}

@Composable
fun Content(
    padding: PaddingValues,
    controller: LifecycleCameraController,
    onTakePhoto: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        CameraPreview(controller = controller, modifier = modifier.fillMaxSize())

        IconButton(
            onClick = {
                controller.cameraSelector =
                    if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA
                    else CameraSelector.DEFAULT_BACK_CAMERA
            },
            modifier = modifier.offset(16.dp, 16.dp)
        ) {
            Icon(imageVector = Icons.Outlined.Cameraswitch, contentDescription = "Switch Camera")
        }

        // Bottom Row Icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            IconButton(onClick = onOpenGallery) {
                Icon(imageVector = Icons.Outlined.Photo, contentDescription = "Open Gallery")
            }
            IconButton(onClick = onTakePhoto) {
                Icon(imageVector = Icons.Outlined.PhotoCamera, contentDescription = "Take a photo")
            }
            IconButton(onClick = { /*TODO*/ }) {
                Icon(imageVector = Icons.Outlined.Photo, contentDescription = "Open Gallery")
            }
        }


    }
}

@Composable
fun SheetContent(bitmaps: List<Bitmap>, modifier: Modifier = Modifier) {

    if (bitmaps.isEmpty()) {
        return Box(
            modifier = modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No photos taken yet")
        }
    }


    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalItemSpacing = 16.dp,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
    ) {
        items(bitmaps.size) { index ->
            Image(
                bitmap = bitmaps[index].asImageBitmap(),
                contentDescription = "Image",
                modifier = modifier.clip(RoundedCornerShape(10.dp))
            )
        }
    }

}

@Composable
@Preview
fun HomePreview() {
    val context = LocalContext.current
    Home(context = context)
}