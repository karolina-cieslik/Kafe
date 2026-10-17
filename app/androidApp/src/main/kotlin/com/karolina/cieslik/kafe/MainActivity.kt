package com.karolina.cieslik.kafe

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

private const val SPLASH_FADE_OUT_MS = 200L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // KAFE-42: Systemowy splash (Theme.Kafe.Starting) znika łagodnie, odsłaniając intro w Compose
        // na tym samym kawowym tle – bez białego błysku i bez skoku.
        installSplashScreen().setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(SPLASH_FADE_OUT_MS)
                .withEndAction { provider.remove() }
                .start()
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
