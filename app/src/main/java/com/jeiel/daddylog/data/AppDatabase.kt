package com.jeiel.daddylog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        HairPhotoRecord::class,
        ScalpConditionRecord::class,
        HairCareRoutine::class,
        HairCareCheck::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scalpDao(): ScalpDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scalp_care_database"
                )
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Prepopulate routines in background
                val scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
                    val dao = getDatabase(context).scalpDao()
                    val defaultRoutines = listOf(
                        HairCareRoutine(title = "약 복용", category = "MEDICATION"),
                        HairCareRoutine(title = "샴푸", category = "SHAMPOO"),
                        HairCareRoutine(title = "두피 토닉", category = "TONIC"),
                        HairCareRoutine(title = "영양제 복용", category = "SUPPLEMENTS"),
                        HairCareRoutine(title = "두피 마사지", category = "MASSAGE"),
                        HairCareRoutine(title = "병원 방문", category = "HOSPITAL")
                    )
                    defaultRoutines.forEach { dao.insertRoutine(it) }
                }
            }
        }
    }
}
