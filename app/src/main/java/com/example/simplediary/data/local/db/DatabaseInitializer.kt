package com.example.simplediary.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseInitializer {
    val migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createWorkoutMetadataTables(db)
            seedDefaultWorkoutMetadata(db)
            migrateWorkoutsTable(db)
        }
    }

    val migration3To4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workouts ADD COLUMN note TEXT NOT NULL DEFAULT ''")
        }
    }

    val migration4To5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE daily_targets_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    effectiveFrom INTEGER NOT NULL,
                    targetProteinsGrams REAL NOT NULL,
                    targetFatsGrams REAL NOT NULL,
                    targetCarbsGrams REAL NOT NULL,
                    targetCaloriesKcal REAL NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO daily_targets_new(
                    effectiveFrom,
                    targetProteinsGrams,
                    targetFatsGrams,
                    targetCarbsGrams,
                    targetCaloriesKcal
                )
                SELECT
                    0,
                    targetProteinsGrams,
                    targetFatsGrams,
                    targetCarbsGrams,
                    targetCaloriesKcal
                FROM daily_targets
                ORDER BY dateEpochMillisUtcStart DESC, id DESC
                LIMIT 1
                """.trimIndent()
            )
            db.execSQL("DROP TABLE daily_targets")
            db.execSQL("ALTER TABLE daily_targets_new RENAME TO daily_targets")
            db.execSQL("CREATE INDEX index_daily_targets_effectiveFrom ON daily_targets(effectiveFrom)")
        }
    }

    val migration5To6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workouts ADD COLUMN distanceKm REAL")
        }
    }

    val migration6To7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE state_notes ADD COLUMN category TEXT NOT NULL DEFAULT '🙂'")
        }
    }

    val migration7To8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createFoodItemsTable(db)
        }
    }

    fun onCreate(db: SupportSQLiteDatabase) {
        createWorkoutMetadataTables(db)
        seedDefaultWorkoutMetadata(db)
        createFoodItemsTable(db)
    }

    private fun createFoodItemsTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS food_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                normalizedName TEXT NOT NULL,
                caloriesKcal REAL,
                proteinsGrams REAL,
                fatsGrams REAL,
                carbsGrams REAL,
                weightGrams REAL,
                source TEXT NOT NULL,
                sourceKey TEXT,
                ingredients TEXT,
                useCount INTEGER NOT NULL,
                lastUsedAt INTEGER,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_food_items_normalizedName
            ON food_items(normalizedName)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_food_items_source_sourceKey
            ON food_items(source, sourceKey)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_food_items_lastUsedAt
            ON food_items(lastUsedAt)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_food_items_useCount
            ON food_items(useCount)
            """.trimIndent()
        )
    }

    private fun createWorkoutMetadataTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS workout_categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                sortOrder INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_workout_categories_name
            ON workout_categories(name)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_workout_categories_sortOrder
            ON workout_categories(sortOrder)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS workout_types (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                categoryId INTEGER NOT NULL,
                name TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES workout_categories(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_workout_types_categoryId
            ON workout_types(categoryId)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_workout_types_name
            ON workout_types(name)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_workout_types_sortOrder
            ON workout_types(sortOrder)
            """.trimIndent()
        )
    }

    private fun seedDefaultWorkoutMetadata(db: SupportSQLiteDatabase) {
        val defaults = listOf(
            CategorySeed(
                name = "Кардио",
                sortOrder = 1,
                types = listOf("Бег", "Беговая дорожка", "Эллипс", "Велосипед", "Велотренажёр", "Скакалка"),
            ),
            CategorySeed(
                name = "Силовая",
                sortOrder = 2,
                types = listOf("Грудь", "Спина", "Ноги", "Плечи", "Руки", "Всё тело"),
            ),
            CategorySeed(
                name = "Бассейн",
                sortOrder = 3,
                types = listOf("Плавание"),
            ),
            CategorySeed(
                name = "Другое",
                sortOrder = 4,
                types = listOf("Йога", "Растяжка", "Единоборства"),
            ),
        )

        defaults.forEach { category ->
            val escapedName = category.name.replace("'", "''")
            db.execSQL(
                """
                INSERT OR IGNORE INTO workout_categories(name, sortOrder)
                VALUES ('$escapedName', ${category.sortOrder})
                """.trimIndent()
            )
            category.types.forEachIndexed { index, typeName ->
                val escapedTypeName = typeName.replace("'", "''")
                db.execSQL(
                    """
                    INSERT INTO workout_types(categoryId, name, sortOrder)
                    SELECT id, '$escapedTypeName', ${index + 1}
                    FROM workout_categories
                    WHERE name = '$escapedName'
                    """.trimIndent()
                )
            }
        }
    }

    private fun migrateWorkoutsTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE workouts_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dateEpochMillisUtcStart INTEGER NOT NULL,
                categoryId INTEGER NOT NULL,
                typeId INTEGER,
                durationMinutes INTEGER NOT NULL,
                caloriesBurned INTEGER NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES workout_categories(id) ON DELETE CASCADE,
                FOREIGN KEY(typeId) REFERENCES workout_types(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO workouts_new(id, dateEpochMillisUtcStart, categoryId, typeId, durationMinutes, caloriesBurned)
            SELECT
                w.id,
                w.dateEpochMillisUtcStart,
                CASE w.type
                    WHEN 'CARDIO' THEN (SELECT id FROM workout_categories WHERE name = 'Кардио' LIMIT 1)
                    WHEN 'STRENGTH' THEN (SELECT id FROM workout_categories WHERE name = 'Силовая' LIMIT 1)
                    WHEN 'MOBILITY' THEN (SELECT id FROM workout_categories WHERE name = 'Другое' LIMIT 1)
                    WHEN 'SPORTS' THEN (SELECT id FROM workout_categories WHERE name = 'Другое' LIMIT 1)
                    ELSE (SELECT id FROM workout_categories WHERE name = 'Другое' LIMIT 1)
                END AS categoryId,
                NULL AS typeId,
                w.durationMinutes,
                w.caloriesBurned
            FROM workouts w
            """.trimIndent()
        )

        db.execSQL("DROP TABLE workouts")
        db.execSQL("ALTER TABLE workouts_new RENAME TO workouts")
        db.execSQL("CREATE INDEX index_workouts_dateEpochMillisUtcStart ON workouts(dateEpochMillisUtcStart)")
        db.execSQL("CREATE INDEX index_workouts_categoryId ON workouts(categoryId)")
        db.execSQL("CREATE INDEX index_workouts_typeId ON workouts(typeId)")
    }
}

private data class CategorySeed(
    val name: String,
    val sortOrder: Int,
    val types: List<String>,
)
