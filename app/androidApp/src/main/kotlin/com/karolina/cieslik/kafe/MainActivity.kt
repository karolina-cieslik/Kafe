package com.karolina.cieslik.kafe

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/** Jedyna aktywność aplikacji – włącza edge-to-edge i hostuje wspólny korzeń UI [App]. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Ekran startowy (menu) ma ciemny nagłówek w obu motywach – jasne ikony paska statusu.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

