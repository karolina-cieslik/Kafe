package com.karolina.cieslik.kafe.core.data.preferences

import android.content.Context

actual class PreferencesFactory(private val context: Context) {

    actual fun filePath(): String = context.applicationContext.filesDir.resolve(PREFERENCES_FILE_NAME).absolutePath
}
