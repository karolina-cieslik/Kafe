package com.karolina.cieslik.kafe.navigation

import androidx.compose.runtime.Composable
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
            ProductDetailRoute(
                productId = entry.toRoute<KafeRoute.ProductDetail>().productId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<KafeRoute.CafePicker> {
            CafePickerRoute(
                onCafeChosen = { navController.navigateAfterCafeChosen() },
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
