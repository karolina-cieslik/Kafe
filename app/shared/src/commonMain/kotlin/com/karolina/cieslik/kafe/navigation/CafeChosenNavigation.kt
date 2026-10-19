package com.karolina.cieslik.kafe.navigation

import androidx.navigation.NavController

/**
 * Nawigacja po wyborze lokalu (CafePickerEvent.CafeChosen): powrót do menu, jeśli picker otwarto z menu;
 * przy pierwszym uruchomieniu (picker jako ekran startowy) przejście do menu bez pickera na stosie, żeby
 * „wstecz” z menu zamykało aplikację.
 */
fun NavController.navigateAfterCafeChosen() {
    if (previousBackStackEntry != null) {
        popBackStack()
    } else {
        navigate(KafeRoute.Menu) {
            popUpTo<KafeRoute.CafePicker> { inclusive = true }
            launchSingleTop = true
        }
    }
}
