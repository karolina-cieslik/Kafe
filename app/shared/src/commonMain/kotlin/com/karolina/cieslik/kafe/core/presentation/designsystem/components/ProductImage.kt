package com.karolina.cieslik.kafe.core.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.ic_coffee
import org.jetbrains.compose.resources.painterResource

private val GrayscaleFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

// KAFE-25: Zdjęcie produktu z Coil. Pod spodem zawsze leży placeholder z ikoną kawy, widoczny
// przy braku URL, w trakcie ładowania i po błędzie. grayscale = produkt chwilowo niedostępny.
@Composable
fun ProductImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    grayscale: Boolean = false,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(KafeTheme.colors.primaryContainer),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_coffee),
            contentDescription = null,
            tint = if (grayscale) KafeTheme.colors.outline else KafeTheme.colors.primary,
            modifier = Modifier.size(KafeTheme.spacing.iconLarge),
        )
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                colorFilter = if (grayscale) GrayscaleFilter else null,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
