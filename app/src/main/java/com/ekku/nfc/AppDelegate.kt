package com.ekku.nfc

import android.app.Application
import com.ekku.nfc.model.TagRepository
import com.ekku.nfc.model.TagRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber

class AppDelegate : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())
    private val database by lazy { TagRoomDatabase.getDatabase(this, null) }
    val repository by lazy { TagRepository(database.tagDao()) }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree())
    }
}