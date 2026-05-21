package com.example.simplediary.data.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.simplediary.core.files.PhotoCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

class AndroidPhotoCompressor : PhotoCompressor {
    override suspend fun compressToJpeg(
        inputPath: String,
        outputPath: String,
        maxSidePx: Int,
        qualityPercent: Int,
    ): String = withContext(Dispatchers.IO) {
        require(maxSidePx > 0) { "maxSidePx must be > 0" }
        require(qualityPercent in 1..100) { "qualityPercent must be in 1..100" }

        val inputFile = File(inputPath)
        require(inputFile.exists()) { "Input image not found: $inputPath" }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(inputPath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image: $inputPath" }

        val longestSide = max(bounds.outWidth, bounds.outHeight).toFloat()
        val ratio = if (longestSide <= maxSidePx) 1f else maxSidePx / longestSide
        val targetWidth = (bounds.outWidth * ratio).roundToInt().coerceAtLeast(1)
        val targetHeight = (bounds.outHeight * ratio).roundToInt().coerceAtLeast(1)

        val sampleSize = calculateInSampleSize(
            srcWidth = bounds.outWidth,
            srcHeight = bounds.outHeight,
            reqWidth = targetWidth,
            reqHeight = targetHeight,
        )

        val decodedBitmap = BitmapFactory.decodeFile(
            inputPath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: error("Failed to decode image: $inputPath")

        val bitmapToSave = if (decodedBitmap.width == targetWidth && decodedBitmap.height == targetHeight) {
            decodedBitmap
        } else {
            Bitmap.createScaledBitmap(decodedBitmap, targetWidth, targetHeight, true).also {
                decodedBitmap.recycle()
            }
        }

        val outputFile = File(outputPath).apply {
            parentFile?.mkdirs()
        }

        FileOutputStream(outputFile).use { out ->
            check(bitmapToSave.compress(Bitmap.CompressFormat.JPEG, qualityPercent, out)) {
                "Failed to compress image to JPEG: $outputPath"
            }
            out.flush()
        }

        bitmapToSave.recycle()
        outputFile.absolutePath
    }

    private fun calculateInSampleSize(
        srcWidth: Int,
        srcHeight: Int,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        var inSampleSize = 1
        if (srcHeight > reqHeight || srcWidth > reqWidth) {
            var halfHeight = srcHeight / 2
            var halfWidth = srcWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
