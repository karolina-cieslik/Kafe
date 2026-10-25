package com.karolina.cieslik.kafe.feature.cart.presentation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import com.karolina.cieslik.kafe.feature.product.domain.ProductConfiguration
import kafe.app.shared.generated.resources.Res
import kafe.app.shared.generated.resources.cart_action_failed
import kafe.app.shared.generated.resources.cart_item_removed
import kafe.app.shared.generated.resources.cart_limit_reached
import kafe.app.shared.generated.resources.cart_undo
import org.jetbrains.compose.resources.getString

/** Snackbar „Usunięto: … · Cofnij” – osobny typ, żeby [dismissUndoSnackbar] zamykał tylko jego, a nie np. błąd. */
private class UndoSnackbarVisuals(
    override val message: String,
    override val actionLabel: String,
) : SnackbarVisuals {
    override val duration = SnackbarDuration.Long
    override val withDismissAction = false
}

/** Zwykły komunikat bez akcji (limit, błąd zapisu). */
private class MessageSnackbarVisuals(override val message: String) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val duration = SnackbarDuration.Short
    override val withDismissAction = false
}

/**
 * Pokazuje snackbar dla zdarzenia koszyka i zawiesza się do jego zamknięcia:
 * - [CartEvent.ItemRemoved] – „Usunięto: {nazwa}” z „Cofnij” na `SnackbarDuration.Long`; stuknięcie „Cofnij”
 *   wywołuje [onUndo];
 * - [CartEvent.RestoredAtLimit] – „W koszyku jest już maks. 20 szt. tej pozycji” (limit z
 *   [ProductConfiguration.MAX_QUANTITY], ten sam tekst co na ekranie produktu);
 * - [CartEvent.ActionFailed] – prośba o ponowienie.
 *
 * Nowy komunikat zastępuje bieżący, więc kolejne usunięcie zamyka poprzednie „Cofnij”. Dla zdarzeń bez snackbara
 * (nawigacja, [CartEvent.UndoExpired]) wraca od razu i niczego nie zamyka.
 */
internal suspend fun SnackbarHostState.showCartEventSnackbar(event: CartEvent, onUndo: () -> Unit) {
    val visuals: SnackbarVisuals = when (event) {
        is CartEvent.ItemRemoved -> UndoSnackbarVisuals(
            message = getString(Res.string.cart_item_removed, event.productName),
            actionLabel = getString(Res.string.cart_undo),
        )
        is CartEvent.RestoredAtLimit -> MessageSnackbarVisuals(
            getString(Res.string.cart_limit_reached, ProductConfiguration.MAX_QUANTITY),
        )
        CartEvent.ActionFailed -> MessageSnackbarVisuals(getString(Res.string.cart_action_failed))
        CartEvent.UndoExpired, CartEvent.NavigateToMenu, CartEvent.NavigateBack -> return
    }
    currentSnackbarData?.dismiss()
    if (showSnackbar(visuals) == SnackbarResult.ActionPerformed) onUndo()
}

/** Zamyka bieżący snackbar, jeśli to „Cofnij” ([CartEvent.UndoExpired] – po wyczyszczeniu nie ma czego przywracać). */
internal fun SnackbarHostState.dismissUndoSnackbar() {
    val data = currentSnackbarData ?: return
    if (data.visuals is UndoSnackbarVisuals) data.dismiss()
}
