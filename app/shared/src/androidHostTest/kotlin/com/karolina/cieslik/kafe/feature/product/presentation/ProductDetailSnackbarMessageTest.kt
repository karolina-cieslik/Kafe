package com.karolina.cieslik.kafe.feature.product.presentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.karolina.cieslik.kafe.core.testing.ROBOLECTRIC_SDK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Treść snackbara na ekranie produktu dla zdarzeń dodania do koszyka. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class ProductDetailSnackbarMessageTest {

    @Test
    fun addedToCartNamesTheProduct() = runTest {
        assertEquals(
            "Dodano do koszyka: Cappuccino",
            ProductDetailEvent.AddedToCart("Cappuccino").snackbarMessage(),
        )
    }

    @Test
    fun limitReachedShowsMaxQuantityInsteadOfConfirmation() = runTest {
        assertEquals(
            "W koszyku jest już maks. 20 szt. tej pozycji",
            ProductDetailEvent.AddedToCart("Cappuccino", limitReached = true).snackbarMessage(),
        )
    }

    @Test
    fun addToCartFailedAsksToRetry() = runTest {
        assertEquals(
            "Nie udało się dodać do koszyka. Spróbuj ponownie.",
            ProductDetailEvent.AddToCartFailed.snackbarMessage(),
        )
    }

    @Test
    fun navigateBackHasNoSnackbar() = runTest {
        assertNull(ProductDetailEvent.NavigateBack.snackbarMessage())
    }
}
