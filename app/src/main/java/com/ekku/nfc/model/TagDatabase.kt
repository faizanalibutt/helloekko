package com.ekku.nfc.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ekku.nfc.util.AppUtils.STRING_GUID
import com.ekku.nfc.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber

// Annotates class to be a Room Database with a table (entity) of the Word class
@Database(entities = [Tag::class], version = 1, exportSchema = false)
abstract class TagRoomDatabase : RoomDatabase() {

    abstract fun tagDao(): TagDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time.
        @Volatile
        private var INSTANCE: TagRoomDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope?
        ): TagRoomDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        TagRoomDatabase::class.java,
                        "tag_database"
                    )
                        //.addCallback(WordDatabaseCallback(scope))
                        .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }

    private class WordDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
//            INSTANCE?.let { database ->
//                scope.launch {
//                    populateDatabase(database.tagDao())
//                }
//            }
            Timber.d("tag database has been created.")
        }

        suspend fun populateDatabase(tagDao: TagDao) {

            // Add sample tags.
            var tag = Tag(
                tag_uid = "CP2B8DJJ",
                tag_time = TimeUtils.getToday(),
                tag_date = TimeUtils.getFormatDate(TimeUtils.getToday()),
                tag_date_time = TimeUtils.getFormatDateTime(TimeUtils.getToday()),
                tag_phone_uid = STRING_GUID,
                tag_sync = 0
            )
            tagDao.insert(tag)
            tag = Tag(
                tag_uid = "MM2B8DJJ",
                tag_time = TimeUtils.getToday(),
                tag_date = TimeUtils.getFormatDate(TimeUtils.getToday()),
                tag_date_time = TimeUtils.getFormatDateTime(TimeUtils.getToday()),
                tag_phone_uid = STRING_GUID,
                tag_sync = 0
            )
            tagDao.insert(tag)

        }
    }

}