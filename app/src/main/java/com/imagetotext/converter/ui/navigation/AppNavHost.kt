package com.imagetotext.converter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.imagetotext.converter.ui.screens.HistoryScreen
import com.imagetotext.converter.ui.screens.HomeScreen
import com.imagetotext.converter.ui.screens.PreviewScreen
import com.imagetotext.converter.ui.screens.ResultScreen
import com.imagetotext.converter.viewmodel.OcrViewModel

/** The four screens of the app: Home, Preview/edit, Result and History. */
object Routes {
    const val HOME = "home"
    const val PREVIEW = "preview"
    const val RESULT = "result"
    const val HISTORY = "history"
}

@Composable
fun AppNavHost(viewModel: OcrViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onImageReady = { navController.navigate(Routes.PREVIEW) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) }
            )
        }
        composable(Routes.PREVIEW) {
            PreviewScreen(
                viewModel = viewModel,
                onChangeImage = { navController.popBackStack() },
                onTextExtracted = {
                    navController.navigate(Routes.RESULT) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
        composable(Routes.RESULT) {
            ResultScreen(
                viewModel = viewModel,
                onBackToHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenResult = {
                    navController.navigate(Routes.RESULT) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
    }
}
