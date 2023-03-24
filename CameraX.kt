package com.rstech.camerascanner.pdfscanner.camscanner.easy.scan.camscanner.model

import android.content.Context
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * Initiate and Return CameraController and display camera feed in PreviewView - CameraX
 * Change use cases as you require, Activity or Fragment can be lifecycleOwner
 **/
fun LifecycleOwner.initCameraController(
    context: Context,
    previewView: PreviewView,
    hasZoom: Boolean = true,
    hasTapToFocus: Boolean = true,
    vararg useCases: Int = intArrayOf(
        CameraController.IMAGE_CAPTURE,
        CameraController.IMAGE_ANALYSIS
    )
): CameraController {
    val cameraController = LifecycleCameraController(context)
    cameraController.bindToLifecycle(this)
    previewView.controller = cameraController
    cameraController.isPinchToZoomEnabled = hasZoom
    cameraController.isTapToFocusEnabled = hasTapToFocus
    useCases.forEach { cameraController.setEnabledUseCases(it) }
    return cameraController
}

/**
 *Init CameraController and generate a PreviewView for Compose
 *Change Use cases as required, use the resulting PreviewView as required
 */
@Composable
fun InitCameraController(
    hasZoom: Boolean = true,
    hasTapToFocus: Boolean = true,
    onInitialized: (PreviewView, CameraController) -> Unit,
    vararg useCases: Int = intArrayOf(
        CameraController.IMAGE_CAPTURE,
        CameraController.IMAGE_ANALYSIS
    )
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView: PreviewView = remember { PreviewView(context) }
    val cameraController = LifecycleCameraController(context)

    LaunchedEffect(Unit) {
        cameraController.bindToLifecycle(lifecycleOwner)
        previewView.controller = cameraController
        useCases.forEach { cameraController.setEnabledUseCases(it) }
        cameraController.isPinchToZoomEnabled = hasZoom
        cameraController.isTapToFocusEnabled = hasTapToFocus
        onInitialized(previewView, cameraController)
    }
}


/**
 *Init CameraController with Predefined previewView in Compose
 *Change Use cases as required, use the resulting PreviewView as required
 */
@Composable
fun InitCameraController(
    previewView: PreviewView,
    hasZoom: Boolean = true,
    hasTapToFocus: Boolean = true,
    onInitialized: (CameraController) -> Unit,
    vararg useCases: Int = intArrayOf(
        CameraController.IMAGE_CAPTURE,
        CameraController.IMAGE_ANALYSIS
    )
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = LifecycleCameraController(context)

    LaunchedEffect(Unit) {
        cameraController.bindToLifecycle(lifecycleOwner)
        previewView.controller = cameraController
        useCases.forEach { cameraController.setEnabledUseCases(it) }
        cameraController.isPinchToZoomEnabled = hasZoom
        cameraController.isTapToFocusEnabled = hasTapToFocus
        onInitialized(cameraController)
    }
}

/**Init ImageCapture Use case using Camera Provider*/
fun Context.createImageCaptureUseCase(
    lifecycleOwner: LifecycleOwner,
    cameraSelector: CameraSelector,
    previewView: PreviewView,
    onImageCaptureCreated: (ImageCapture) -> Unit
) {
    val future = ProcessCameraProvider.getInstance(this)
    future.addListener({
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
        val imageCapture = ImageCapture.Builder().build()
        val cameraProvider = future.get()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
        onImageCaptureCreated.invoke(imageCapture)
    }, ContextCompat.getMainExecutor(this))
}

/**Init VideoCapture Use case using Camera Provider*/
suspend fun Context.createVideoCaptureUseCase(
    lifecycleOwner: LifecycleOwner,
    cameraSelector: CameraSelector,
    previewView: PreviewView
): VideoCapture<Recorder> {
    val preview = Preview.Builder()
        .build()
        .apply { setSurfaceProvider(previewView.surfaceProvider) }

    val qualitySelector = QualitySelector.from(
        Quality.FHD,
        FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
    )
    val recorder = Recorder.Builder()
        .setExecutor(if (Build.VERSION.SDK_INT >= 28) mainExecutor else Executors.newSingleThreadExecutor())
        .setQualitySelector(qualitySelector)
        .build()
    val videoCapture = VideoCapture.withOutput(recorder)

    val cameraProvider = getCameraProvider()
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        videoCapture
    )

    return videoCapture
}

/**get CameraProvider from Context*/
suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener(
            {
                continuation.resume(future.get())
            }, ContextCompat.getMainExecutor(this)
        )
    }
}
