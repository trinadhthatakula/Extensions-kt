package com.demo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.content.res.Resources.getSystem
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.viewpager2.widget.ViewPager2
import com.demo.BuildConfig
import com.demo.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.*

///get Uri of any resource
internal fun Context.getResourceUri(@AnyRes resourceId: Int): Uri = Uri.Builder()
    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
    .authority(packageName)
    .path(resourceId.toString())
    .build()

///get resource id by name
internal fun Context.resIdByName(resIdName: String?, resType: String): Int {
    resIdName?.let {
        return resources.getIdentifier(it, resType, packageName)
    }
    throw Resources.NotFoundException()
}

///get drawable by id
internal fun Context.drawableIdByName(resIdName: String?): Int {
    return if (resIdName != null) {
        resources.getIdentifier(resIdName, "drawable", packageName)
    } else
        R.drawable.adaptive_icon_foreground
}

///inflate a layout
internal fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return context.layoutInflater.inflate(layoutRes, this, attachToRoot)
}

///get inflater from context
internal val Context.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(this)

///get IMM
internal val Context.inputMethodManager
    get() = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

///i am just lazy 
internal /*inline*/ fun Boolean?.orFalse(): Boolean = this ?: false

///get drawable compat
internal fun Context.getDrawableCompat(@DrawableRes drawable: Int) = ContextCompat.getDrawable(
    this,
    drawable
)

///get Color compat
internal fun Context.getColorCompat(@ColorRes color: Int) = ContextCompat.getColor(this, color)

///set color to textView, but why? => cause I am LAZY
internal fun TextView.setTextColorRes(@ColorRes color: Int) = setTextColor(
    context.getColorCompat(
        color
    )
)

///int to dp or px
val Int.dp: Int get() = (this / getSystem().displayMetrics.density).toInt()
val Int.px: Int get() = (this * getSystem().displayMetrics.density).toInt()

///dp to px
fun dpToPx(dp: Int, context: Context): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
        context.resources.displayMetrics
    ).toInt()
	
///Copy input stream to file
fun File.copyInputStreamToFile(inputStream: InputStream): Boolean {
    return try {
        this.outputStream().use { fileOut ->
            inputStream.copyTo(fileOut)
        }
        true
    } catch (_: Exception) {
        false
    }
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

///Convert a view into bitmap without drawing cache
fun View.toBitmap(): Bitmap {
    var bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bitmap)
    this.draw(canvas)
    return bitmap
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
fun Bitmap.rotateBitmap(contentResolver: ContentResolver, uri: Uri): Bitmap? {

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
fun Bitmap.rotate(angle: Float): Bitmap {
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
fun Uri.copyTo(file: File, context: Context): Boolean {
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


///set Corners to gradient drawables
fun GradientDrawable.setCornerRadius(
    topLeft: Float = 0F,
    topRight: Float = 0F,
    bottomRight: Float = 0F,
    bottomLeft: Float = 0F
) {
    cornerRadii = arrayOf(
        topLeft, topLeft,
        topRight, topRight,
        bottomRight, bottomRight,
        bottomLeft, bottomLeft
    ).toFloatArray()
}

///Copy a string to clipBoard
fun String.copyToClipBoard(context: Context, label: String = ""): Boolean {
    return try {
        val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        val clip = ClipData.newPlainText(label, this)
        clipboard?.setPrimaryClip(clip)
        true
    } catch (e: Exception) {
        false
    }
}

///perform touch on view at x, y. x & y are optional
fun View.performBasicTouchAt(x: Float = 0f, y: Float = 0f) {
    dispatchTouchEvent(
        MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_MOVE,
            x, y, 0
        )
    )

}

///hide softkeyboard
fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}


@RequiresApi(Build.VERSION_CODES.O)
fun daysOfWeekFromLocale(): Array<DayOfWeek> {
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    var daysOfWeek = DayOfWeek.values()
    // Order `daysOfWeek` array so that firstDayOfWeek is at index 0.
    // Only necessary if firstDayOfWeek != DayOfWeek.MONDAY which has ordinal 0.
    if (firstDayOfWeek != DayOfWeek.MONDAY) {
        val rhs = daysOfWeek.sliceArray(firstDayOfWeek.ordinal..daysOfWeek.indices.last)
        val lhs = daysOfWeek.sliceArray(0 until firstDayOfWeek.ordinal)
        daysOfWeek = rhs + lhs
    }
    return daysOfWeek
}



