package com.karolina.cieslik.kafe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.karolina.cieslik.kafe.feature.cafe.presentation.CafePickerRoute
import com.karolina.cieslik.kafe.feature.menu.presentation.MenuEvent
import com.karolina.cieslik.kafe.feature.menu.presentation.MenuRoute
import com.karolina.cieslik.kafe.feature.product.presentation.ProductDetailRoute

/**
 * Graf nawigacji aplikacji.
 *
 * Przejścia z menu używają launchSingleTop, więc szybkie podwójne kliknięcie nie otwiera ekranu dwa razy.
 * Powrót zdejmuje ekran ze stosu, a menu zachowuje swój stan.
 *
 * @param navController kontroler nawigacji; parametr, żeby testy mogły podać TestNavHostController.
 * @param startDestination ekran startowy; parametr, żeby pierwsze uruchomienie mogło zacząć od wyboru lokalu.
 */
@Composable
fun KafeNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: KafeRoute = KafeRoute.Menu,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<KafeRoute.Menu> {
            MenuRoute(
                onNavigateToProduct = { productId ->
                    navController.navigate(MenuEvent.NavigateToProduct(productId).toRoute()) { launchSingleTop = true }
                },
                onNavigateToCafePicker = {
                    navController.navigate(MenuEvent.NavigateToCafePicker.toRoute()) { launchSingleTop = true }
                },
            )
        }
        composable<KafeRoute.ProductDetail> { entry ->
            ProductDetailRoute(
                productId = entry.toRoute<KafeRoute.ProductDetail>().productId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<KafeRoute.CafePicker> {
            // Liczone raz na wpis stosu: przy animacji wyjścia (po wyborze z menu) poprzedni wpis jest już zdjęty
            // i bieżące odczytanie dałoby false – nagłówek mignąłby jako powitalny.
            val canNavigateBack = remember { navController.previousBackStackEntry != null }
            CafePickerRoute(
                onCafeChosen = { navController.navigateAfterCafeChosen() },
                // Bezpiecznik: zdjęcie jedynego wpisu stosu zostawiłoby pusty ekran.
                onNavigateBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() },
                canNavigateBack = canNavigateBack,
            )
        }
    }
}
