package com.example.simplediary.domain.usecase

import android.net.Uri
import com.example.simplediary.core.files.BackupManager
import com.example.simplediary.core.files.CsvExporter

class ExportDataUseCase(
    private val csvExporter: CsvExporter,
    private val backupManager: BackupManager,
) {
    suspend fun exportCsv(destinationZipUri: Uri): Uri {
        return csvExporter.exportAllData(destinationZipUri)
    }

    suspend fun createZipBackup(destinationZipUri: Uri): Uri {
        return backupManager.createZipBackup(destinationZipUri)
    }

    suspend fun restoreZipBackup(sourceZipUri: Uri) {
        backupManager.restoreFromZip(sourceZipUri)
    }
}
