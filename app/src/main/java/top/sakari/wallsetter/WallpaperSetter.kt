package top.sakari.wallsetter

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import java.io.File

object WallpaperSetter {

    private const val MAX_DIMENSION = 4096

    fun setFromPath(context: Context, path: String, target: String) {
        val file = File(path)
        require(file.exists()) { "File does not exist: $path" }
        require(file.canRead()) { "Cannot read file: $path" }
        setFromUri(context, Uri.fromFile(file), target)
    }

    fun setFromUri(context: Context, uri: Uri, target: String) {
        val bitmap = decodeBitmap(context, uri)
            ?: throw IllegalArgumentException("Failed to decode image from uri: $uri")

        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val cropHint = buildCenteredCropHint(context, bitmap.width, bitmap.height)
            wallpaperManager.setBitmap(bitmap, cropHint, true, toWallpaperFlag(target))
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }

        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null

        var sampleSize = 1
        while (width / sampleSize > MAX_DIMENSION || height / sampleSize > MAX_DIMENSION) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    private fun toWallpaperFlag(target: String): Int {
        return when (target.lowercase()) {
            WallpaperReceiver.TARGET_HOME -> WallpaperManager.FLAG_SYSTEM
            WallpaperReceiver.TARGET_LOCK -> WallpaperManager.FLAG_LOCK
            else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        }
    }

    private fun buildCenteredCropHint(context: Context, imageWidth: Int, imageHeight: Int): Rect {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.coerceAtLeast(1)
        val screenHeight = displayMetrics.heightPixels.coerceAtLeast(1)

        val targetRatio = screenWidth.toFloat() / screenHeight.toFloat()
        val imageRatio = imageWidth.toFloat() / imageHeight.toFloat()

        val (cropWidth, cropHeight) = if (imageRatio > targetRatio) {
            val width = (imageHeight * targetRatio).toInt().coerceIn(1, imageWidth)
            width to imageHeight
        } else {
            val height = (imageWidth / targetRatio).toInt().coerceIn(1, imageHeight)
            imageWidth to height
        }

        val left = ((imageWidth - cropWidth) / 2).coerceAtLeast(0)
        val top = ((imageHeight - cropHeight) / 2).coerceAtLeast(0)
        val right = (left + cropWidth).coerceAtMost(imageWidth)
        val bottom = (top + cropHeight).coerceAtMost(imageHeight)
        return Rect(left, top, right, bottom)
    }
}
