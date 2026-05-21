package com.example.simplediary.core.files

interface PhotoCompressor {
    suspend fun compressToJpeg(
        inputPath: String,
        outputPath: String,
        maxSidePx: Int = 1200,
        qualityPercent: Int = 85,
    ): String
}
