package com.example.simplediary.core.files

import android.net.Uri

interface BackupManager {
    suspend fun createZipBackup(destinationZipUri: Uri): Uri
    suspend fun restoreFromZip(sourceZipUri: Uri)
}
