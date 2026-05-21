package com.example.simplediary.core.files

import android.net.Uri

interface CsvExporter {
    suspend fun exportAllData(destinationZipUri: Uri): Uri
}
