package com.rstech.camerascanner.pdfscanner.camscanner.easy.scan.camscanner.model

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

///get Bitmap From VectorResource 
///if we import an svg file to android studio it will be converted to xml file
///Use this method to convert that xml to bitmap
fun bitmapFromVector(context: Context, @DrawableRes vectorResId: Int): BitmapDescriptor {
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
    vectorDrawable!!.setBounds(0, 0, (vectorDrawable.intrinsicWidth),
        (vectorDrawable.intrinsicHeight)
    )
    val bitmap = Bitmap.createBitmap(
        (vectorDrawable.intrinsicWidth),
        (vectorDrawable.intrinsicHeight),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return bitmap
}

///There will be multiple functions for the same task use as per your requirement
///Write a Bitmap into a file
fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, quality: Int = 85) {
    outputStream().use { out ->
        bitmap.compress(format, quality, out)
        out.flush()
    }
}

///Save a bitmap into path
fun Bitmap.save(path: String, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, quality: Int = 85): Boolean {
    return try {
        val output = FileOutputStream(path)
        this.compress(format, quality, output)
        output.close()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

///Save a bitmap into file
fun Bitmap.saveToFile(file: File, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, quality: Int = 85): Boolean {
    return try {
        FileOutputStream(file).use { out ->
            this.compress(format, quality, out)
        }
        true
    } catch (e: IOException) {
        e.printStackTrace()
        false
    }
}

///Save a view to file
fun View.saveToFile(
    file: File,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    quality: Int = 85
): Boolean {
    return toBitmap().saveToFile(file, format, quality)
}

///Convert a view into bitmap without drawing cache
fun View.toBitmap(): Bitmap {
    return try{
        var bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        this.draw(canvas)
        bitmap
    }catch (e: Exception) {
        e.printStackTrace()
        drawToBitmap()
    }
}

///Convert View to Bitmap more of a screenshot with callback
///you can replace activity with other supported variants 
fun View.getScreenShot(activity: Activity, callback: (Bitmap) -> Unit) {
    activity.window?.let { window ->
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val locationOfViewInWindow = IntArray(2)
        getLocationInWindow(locationOfViewInWindow)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PixelCopy.request(
                    window,
                    Rect(
                        locationOfViewInWindow[0],
                        locationOfViewInWindow[1],
                        locationOfViewInWindow[0] + width,
                        locationOfViewInWindow[1] + height
                    ), bitmap, { copyResult ->
                        when (copyResult) {
                            PixelCopy.SUCCESS -> {
                                callback(bitmap)
                            }
                            // possible to handle other result codes ...
                            else -> {

                            }
                        }
                        
                    },
                    Handler(Looper.getMainLooper())
                )
            } else toBitmap()
        } catch (e: IllegalArgumentException) {
            // PixelCopy may throw IllegalArgumentException, make sure to handle it
            e.printStackTrace()
        }
    }
}

@Suppress("DEPRECATION")
fun Uri.getBitmap(context: Context): Bitmap{
    return if(Build.VERSION.SDK_INT < 28) {
        MediaStore.Images.Media.getBitmap(
            context.contentResolver,
            this
        )
    } else {
        val source = ImageDecoder.createSource(context.contentResolver, this)
        ImageDecoder.decodeBitmap(source)
    }
}

// Get average color of a bitmap
fun calculateAverageColor(bitmap: Bitmap, pixelSpacing: Int): Int {
    var r = 0
    var g = 0
    var b = 0
    val height = bitmap.height
    val width = bitmap.width
    var n = 0
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    var i = 0
    while (i < pixels.size) {
        val color = pixels[i]
        r += Color.red(color)
        g += Color.green(color)
        b += Color.blue(color)
        n++
        i += pixelSpacing
    }
    return Color.rgb(r / n, g / n, b / n)
}

///Correct orientation of a bitmap
///In some devices orientation might be wrong in those cases use this fun
fun Bitmap.rotateBitmapByOrientation(contentResolver: ContentResolver, uri: Uri): Bitmap? {

    val matrix = Matrix()
    var exif: ExifInterface?
    return try {
        val inputStream = contentResolver.openInputStream(uri)
        inputStream?.let {
            exif = ExifInterface(inputStream)

            val orientation = exif!!.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            when (orientation) {
                ExifInterface.ORIENTATION_NORMAL -> return this
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    matrix.setRotate(180f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.setRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.setRotate(-90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
                else -> return this
            }

        } ?: return this
        val bmRotated = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
        this.recycle()
        bmRotated.copy(Bitmap.Config.ARGB_8888, true)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

val screenWidth: Int = Resources.getSystem().displayMetrics.widthPixels
val screenHeight: Int = Resources.getSystem().displayMetrics.heightPixels

///Scale Bitmap but keep aspect ratio
fun Bitmap.scaleBitmapAndKeepRation(
    reqHeight: Int = screenHeight,
    reqWidth: Int = screenWidth
): Bitmap {
    val matrix = Matrix()
    matrix.setRectToRect(
        RectF(
            0f, 0f, width.toFloat(),
            height.toFloat()
        ),
        RectF(0f, 0f, reqWidth.toFloat(), reqHeight.toFloat()),
        Matrix.ScaleToFit.CENTER
    )
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

///Rotate bitmap by angle
fun Bitmap.rotateBy(angle: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(this, 0, 0, this.width / 2, this.height / 2, matrix, true)
}

///get Uri form file using file provider please check authority first
fun File.getUri(context: Context): Uri{
    return FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.provider",
        this
    )
}

///Copy content of an uri to another file
fun Uri.copyToFile(file: File, context: Context): Boolean {
    return try {
        context.contentResolver.openInputStream(this).use { input ->
            file.outputStream().use { output ->
                input?.copyTo(output)
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

///Copy contents of a file to another file
fun File.copyTo(file: File): Boolean {
    return try {
        file.outputStream().use { output ->
            this.inputStream().copyTo(output)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}




