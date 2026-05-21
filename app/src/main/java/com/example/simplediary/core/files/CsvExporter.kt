package com.example.simplediary.core.files

interface CsvExporter {
    suspend fun exportAllData(destinationDirectoryPath: String): List<String>
}
