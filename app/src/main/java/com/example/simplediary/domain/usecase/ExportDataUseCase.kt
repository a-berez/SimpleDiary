package com.example.simplediary.domain.usecase

import com.example.simplediary.core.files.BackupManager
import com.example.simplediary.core.files.CsvExporter

class ExportDataUseCase(
    private val csvExporter: CsvExporter,
    private val backupManager: BackupManager,
) {
    suspend fun exportCsv(destinationDirectoryPath: String): List<String> {
        return csvExporter.exportAllData(destinationDirectoryPath)
    }

    suspend fun createZipBackup(destinationZipPath: String): String {
        return backupManager.createZipBackup(destinationZipPath)
    }
}
