package com.karolina.cieslik.kafe

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider

private const val SPLASH_FADE_OUT_MS = 200L

/** Zapas ponad czas wygaszania, po którym splash jest usuwany nawet wtedy, gdy animacja nie dobiegła końca. */
private const val SPLASH_REMOVE_FALLBACK_MS = SPLASH_FADE_OUT_MS + 300L

/** Jedyna aktywność aplikacji – systemowy splash, edge-to-edge i wspólny korzeń UI [App]. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // Własne wygaszanie splasha tylko przy zimnym starcie – wtedy pod spodem rusza intro na tym samym tle.
        // Po odtworzeniu aktywności (zmiana motywu, obrót) intro się nie powtarza, a Android 12+ i tak pokazuje
        // splash przy relaunchu; bez listenera system zdejmuje go sam po pierwszej klatce, więc nie może
        // zostać nad aplikacją.
        if (savedInstanceState == null) {
            splashScreen.setOnExitAnimationListener { provider -> provider.fadeOutAndRemove() }
        }
        // Paski systemowe przezroczyste i bez scrimu; kolor ikon ustawia każdy ekran (SystemBarsAppearance).
        // Start: jasne ikony na kawowym tle intro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

/**
 * Wygasza widok splasha i usuwa go dokładnie raz. Usunięcie jest gwarantowane: po końcu lub przerwaniu
 * animacji, a w ostateczności po [SPLASH_REMOVE_FALLBACK_MS] – nieusunięty splash przykrywa całą aplikację.
 */
private fun SplashScreenViewProvider.fadeOutAndRemove() {
    var removed = false
    val removeOnce = {
        if (!removed) {
            removed = true
            remove()
        }
    }
    view.animate()
        .alpha(0f)
        .setDuration(SPLASH_FADE_OUT_MS)
        .setListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = removeOnce()
                override fun onAnimationCancel(animation: Animator) = removeOnce()
            },
        )
        .start()
    view.postDelayed(removeOnce, SPLASH_REMOVE_FALLBACK_MS)
}
