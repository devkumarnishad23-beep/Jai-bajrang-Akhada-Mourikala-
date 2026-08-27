package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        StudentProfile::class,
        AttendanceRecord::class,
        TrainingRecord::class,
        WorkoutRecord::class,
        DailyWorkoutPlan::class,
        StudySubject::class,
        StudyTopic::class,
        Chapter::class,
        Question::class,
        StudyAttempt::class,
        MockTest::class,
        TestAttempt::class,
        Notice::class,
        RecruitmentInfo::class,
        Trainer::class,
        GalleryItem::class,
        SuccessStory::class,
        ContactInfo::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure unique index on student mobile numbers
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_students_mobileNumber` ON `students` (`mobileNumber`)")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jai_bajrang_akhada.db"
                )
                .addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.appDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: AppDao) {
            dao.insertStudents(DemoDataGenerator.getSampleStudents())
            dao.insertAttendanceList(DemoDataGenerator.getSampleAttendance())
            dao.insertTrainingRecords(DemoDataGenerator.getSampleTrainingRecords())
            dao.insertWorkoutRecords(DemoDataGenerator.getSampleWorkoutRecords())
            dao.insertWorkoutPlan(DemoDataGenerator.getSampleWorkoutPlan())
            dao.insertSubjects(DemoDataGenerator.getSampleStudySubjects())
            dao.insertTopics(DemoDataGenerator.getSampleStudyTopics())
            dao.insertChapters(DemoDataGenerator.getSampleChapters())
            dao.insertQuestions(DemoDataGenerator.getSampleQuestions())
            dao.insertStudyAttempts(DemoDataGenerator.getSampleStudyAttempts())
            dao.insertMockTests(DemoDataGenerator.getSampleMockTests())
            DemoDataGenerator.getSampleTestAttempts().forEach { dao.insertTestAttempt(it) }
            dao.insertNotices(DemoDataGenerator.getSampleNotices())
            dao.insertRecruitmentInfos(DemoDataGenerator.getSampleRecruitmentInfo())
        }
    }
}
