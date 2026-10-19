package com.karolina.cieslik.kafe

import android.os.Build

/** Android z poziomem API. */
class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

/** Zwraca [AndroidPlatform]. */
actual fun getPlatform(): Platform = AndroidPlatform()