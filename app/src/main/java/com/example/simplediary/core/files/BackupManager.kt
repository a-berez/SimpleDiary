package com.example.simplediary.core.files

interface BackupManager {
    suspend fun createZipBackup(destinationZipPath: String): String
    suspend fun restoreFromZip(zipPath: String)
}
