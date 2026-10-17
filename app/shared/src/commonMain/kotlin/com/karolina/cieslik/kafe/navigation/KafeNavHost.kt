package com.karolina.cieslik.kafe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.karolina.cieslik.kafe.feature.menu.presentation.MenuEvent
import com.karolina.cieslik.kafe.feature.menu.presentation.MenuRoute

// KAFE-41: Graf nawigacji. NavController i ekran startowy są parametrami, żeby testy mogły podać własny
// kontroler (TestNavHostController), a KAFE-31 – start na CafePicker przy pierwszym uruchomieniu.
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
            // TODO KAFE-9: ProductDetailRoute z ProductDetailViewModel.
            PlaceholderScreen(
                title = "Produkt: ${entry.toRoute<KafeRoute.ProductDetail>().productId}",
                onBack = { navController.popBackStack() },
            )
        }
        composable<KafeRoute.CafePicker> {
            // TODO KAFE-29: CafePickerRoute z CafePickerViewModel.
            PlaceholderScreen(
                title = "Wybór lokalu",
                onBack = { navController.popBackStack() },
            )
        }
    }
}
