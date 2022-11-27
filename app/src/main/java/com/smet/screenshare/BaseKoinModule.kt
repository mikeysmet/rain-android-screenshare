package com.smet.screenshare

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.elvishew.xlog.XLog
import com.ironz.binaryprefs.BinaryPreferencesBuilder
import com.smet.screenshare.data.settings.Settings
import com.smet.screenshare.data.settings.SettingsImpl
import com.smet.screenshare.service.helper.NotificationHelper
import com.smet.screenshare.data.settings.SettingsReadOnly
import com.smet.screenshare.settings.old.SettingsDataMigration
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.bind
import org.koin.dsl.module

val baseKoinModule = module {

    single<com.ironz.binaryprefs.Preferences> {
        BinaryPreferencesBuilder(androidApplication())
            .supportInterProcess(true)
            .memoryCacheMode(BinaryPreferencesBuilder.MemoryCacheMode.EAGER)
            .exceptionHandler { ex -> XLog.e(ex) }
            .build()
    }

    single {
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { ex -> XLog.e(ex); emptyPreferences() },
            migrations = listOf(SettingsDataMigration(androidApplication(), get())),
//            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { androidApplication().preferencesDataStoreFile("user_settings") }
        )
    }

    single<Settings> { SettingsImpl(get()) } bind SettingsReadOnly::class

    single { NotificationHelper(androidApplication()) }
}