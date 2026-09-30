package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [UserEntity::class, TicketEntity::class, CompanyProfileEntity::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun serviceDao(): ServiceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "neo_service_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance

                // Always verify initial admin user & company profile exist
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        populateInitialData(instance.serviceDao())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            populateInitialData(database.serviceDao())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            populateInitialData(database.serviceDao())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: ServiceDao) {
            try {
                if (dao.getUserCount() == 0) {
                    // Clean Database: Insert only essential Administrator account
                    dao.insertUser(
                        UserEntity(
                            username = "admin",
                            password = "admin123",
                            role = "ADMIN",
                            fullName = "Admin User",
                            phone = "+91 98765 00001",
                            email = "admin@neoittech.com"
                        )
                    )
                }

                // Demo tickets are completely removed as requested. Ticket table remains clean.

                if (dao.getCompanyProfileSync() == null) {
                    dao.insertCompanyProfile(
                        CompanyProfileEntity(
                            companyName = "NEO IT TECH",
                            logoResourceName = "ic_neo_launcher_1790656282466"
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
