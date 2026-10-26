package com.karolina.cieslik.kafe.navigation

import androidx.navigation.NavController

/**
 * „Przejdź do menu” z pustego koszyka (CartEvent.NavigateToMenu): zdejmuje ze stosu wszystko nad menu (koszyk
 * i np. szczegóły produktu, z których otwarto koszyk), więc menu zachowuje swój stan. Gdyby menu nie było na stosie,
 * otwiera je w miejsce koszyka.
 */
fun NavController.navigateFromCartToMenu() {
    if (!popBackStack<KafeRoute.Menu>(inclusive = false)) {
        navigate(KafeRoute.Menu) {
            popUpTo<KafeRoute.Cart> { inclusive = true }
            launchSingleTop = true
        }
    }
}
