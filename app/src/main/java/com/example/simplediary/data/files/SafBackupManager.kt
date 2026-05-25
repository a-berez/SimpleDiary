package com.example.simplediary.data.files

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.example.simplediary.core.files.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class SafBackupManager(
    private val context: Context,
    private val databaseFileName: String,
    private val photosDirectoryName: String = DEFAULT_PHOTOS_DIRECTORY_NAME,
    private val beforeRestore: (() -> Unit)? = null,
    private val afterRestore: (() -> Unit)? = null,
) : BackupManager {

    override suspend fun createZipBackup(destinationZipUri: Uri): Uri = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val dbFiles = resolveDatabaseFiles(context, databaseFileName)
        val photosDir = File(context.filesDir, photosDirectoryName)

        resolver.openOutputStream(destinationZipUri, "w")?.use { output ->
            ZipOutputStream(output).use { zip ->
                dbFiles.forEach { file ->
                    addFileToZip(
                        zipOutputStream = zip,
                        sourceFile = file,
                        entryName = "database/${file.name}",
                    )
                }
                if (photosDir.exists()) {
                    addDirectoryRecursivelyToZip(
                        zipOutputStream = zip,
                        sourceDir = photosDir,
                        entryPrefix = "photos/",
                    )
                }
            }
        } ?: error("Unable to open destination URI for backup: $destinationZipUri")

        destinationZipUri
    }

    override suspend fun restoreFromZip(sourceZipUri: Uri): Unit = withContext(Dispatchers.IO) {
        beforeRestore?.invoke()
        try {
            val resolver = context.contentResolver
            val databaseDir = context.getDatabasePath(databaseFileName).parentFile
                ?: error("Unable to resolve database directory")
            val photosDir = File(context.filesDir, photosDirectoryName)
            databaseDir.mkdirs()

            deleteDirectoryContents(photosDir)

            resolver.openInputStream(sourceZipUri)?.use { input ->
                ZipInputStream(input).use { zipInput ->
                    var entry: ZipEntry? = zipInput.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            when {
                                entry.name.startsWith("database/") -> {
                                    val fileName = entry.name.removePrefix("database/").substringAfterLast('/')
                                    if (fileName.isNotBlank()) {
                                        val targetFile = File(databaseDir, fileName)
                                        writeZipEntryToFile(zipInput, targetFile)
                                    }
                                }

                                entry.name.startsWith("photos/") -> {
                                    val relativePath = entry.name.removePrefix("photos/")
                                    if (relativePath.isNotBlank()) {
                                        val targetFile = File(photosDir, relativePath)
                                        val photosRootPath = photosDir.canonicalPath + File.separator
                                        if (!targetFile.canonicalPath.startsWith(photosRootPath)) {
                                            zipInput.closeEntry()
                                            entry = zipInput.nextEntry
                                            continue
                                        }
                                        writeZipEntryToFile(zipInput, targetFile)
                                    }
                                }
                            }
                        }
                        zipInput.closeEntry()
                        entry = zipInput.nextEntry
                    }
                }
            } ?: error("Unable to open backup URI for restore: $sourceZipUri")
        } finally {
            afterRestore?.invoke()
        }
    }

    private fun addDirectoryRecursivelyToZip(
        zipOutputStream: ZipOutputStream,
        sourceDir: File,
        entryPrefix: String,
    ) {
        sourceDir.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val relativePath = file.relativeTo(sourceDir).invariantSeparatorsPath
                addFileToZip(zipOutputStream, file, "$entryPrefix$relativePath")
            }
    }

    private fun addFileToZip(
        zipOutputStream: ZipOutputStream,
        sourceFile: File,
        entryName: String,
    ) {
        ZipEntry(entryName).also { entry ->
            zipOutputStream.putNextEntry(entry)
            FileInputStream(sourceFile).use { input -> input.copyTo(zipOutputStream) }
            zipOutputStream.closeEntry()
        }
    }

    private fun writeZipEntryToFile(zipInputStream: ZipInputStream, outputFile: File) {
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { output ->
            zipInputStream.copyTo(output)
            output.flush()
        }
    }

    private fun deleteDirectoryContents(directory: File) {
        if (!directory.exists()) return
        directory.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                deleteDirectoryContents(child)
            }
            child.delete()
        }
    }

    private fun resolveDatabaseFiles(context: Context, databaseFileName: String): List<File> {
        val baseFile = context.getDatabasePath(databaseFileName)
        if (!baseFile.exists()) {
            error("Database file does not exist: ${baseFile.absolutePath}")
        }
        val shmFile = File("${baseFile.absolutePath}-shm")
        val walFile = File("${baseFile.absolutePath}-wal")
        return buildList {
            add(baseFile)
            if (shmFile.exists()) add(shmFile)
            if (walFile.exists()) add(walFile)
        }
    }

    private companion object {
        const val DEFAULT_PHOTOS_DIRECTORY_NAME = "photos"
    }
}
