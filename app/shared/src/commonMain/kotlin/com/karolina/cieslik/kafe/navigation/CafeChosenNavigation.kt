package com.karolina.cieslik.kafe.navigation

import androidx.navigation.NavController

// KAFE-31: Po wyborze lokalu (CafePickerEvent.CafeChosen) – powrót do menu, jeśli picker otwarto z menu;
// przy pierwszym uruchomieniu (picker jako ekran startowy) przejście do menu bez pickera na stosie.
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
