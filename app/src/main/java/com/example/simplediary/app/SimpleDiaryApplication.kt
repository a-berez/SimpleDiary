package com.example.simplediary.app

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.simplediary.core.files.BackupManager
import com.example.simplediary.core.files.CsvExporter
import com.example.simplediary.core.files.PhotoCompressor
import com.example.simplediary.data.files.AndroidPhotoCompressor
import com.example.simplediary.data.files.SafBackupManager
import com.example.simplediary.data.files.ZipCsvExporter
import com.example.simplediary.data.local.db.AppDatabase
import com.example.simplediary.data.local.db.DatabaseInitializer
import com.example.simplediary.data.repository.JournalRepositoryImpl
import com.example.simplediary.domain.repository.JournalRepository

class SimpleDiaryApplication : Application() {
    lateinit var appDatabase: AppDatabase
        private set

    lateinit var journalRepository: JournalRepository
        private set

    lateinit var photoCompressor: PhotoCompressor
        private set

    lateinit var backupManager: BackupManager
        private set

    lateinit var csvExporter: CsvExporter
        private set

    override fun onCreate() {
        super.onCreate()

        appDatabase = createDatabase()
        recreateDataServices()
        photoCompressor = AndroidPhotoCompressor()
        backupManager = SafBackupManager(
            context = applicationContext,
            databaseFileName = DATABASE_NAME,
            beforeRestore = { appDatabase.close() },
            afterRestore = {
                appDatabase = createDatabase()
                recreateDataServices()
            },
        )
    }

    private fun createRepository(): JournalRepository {
        return JournalRepositoryImpl(
            feedDao = appDatabase.feedDao(),
            mealDao = appDatabase.mealDao(),
            dailyTargetDao = appDatabase.dailyTargetDao(),
            workoutDao = appDatabase.workoutDao(),
        )
    }

    private fun createDatabase(): AppDatabase {
        return Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            DATABASE_NAME,
        )
            .addMigrations(
                DatabaseInitializer.migration2To3,
                DatabaseInitializer.migration3To4,
                DatabaseInitializer.migration4To5,
                DatabaseInitializer.migration5To6,
                DatabaseInitializer.migration6To7,
            )
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        DatabaseInitializer.onCreate(db)
                    }
                }
            )
            .build()
    }

    private fun recreateDataServices() {
        journalRepository = createRepository()
        csvExporter = ZipCsvExporter(
            context = applicationContext,
            mealDao = appDatabase.mealDao(),
            workoutDao = appDatabase.workoutDao(),
            workoutCategoryDao = appDatabase.workoutCategoryDao(),
            workoutTypeDao = appDatabase.workoutTypeDao(),
            stateNoteDao = appDatabase.stateNoteDao(),
        )
    }

    companion object {
        const val DATABASE_NAME = "simple_diary.db"
    }
}
