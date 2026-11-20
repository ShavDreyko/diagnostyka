package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AcoposApp
import com.example.ui.theme.AcoposTheme
import com.example.viewmodel.DiagnosticViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val viewModel: DiagnosticViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.isDarkTheme) {
                val style = if (uiState.isDarkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            AcoposTheme(darkTheme = uiState.isDarkTheme) {
                AcoposApp(
                    uiState = uiState,
                    onSelectTab = viewModel::selectTab,
                    onQueryChange = viewModel::updateSearchQuery,
                    onInputModeChange = viewModel::setSearchInputMode,
                    onSubmitSearch = { viewModel.submitSearch() },
                    onOpenErrorDetail = viewModel::openErrorDetail,
                    onNavigateBack = { viewModel.navigateBackFromDetail() },
                    onOpenCategoryInCatalog = viewModel::openCategoryInCatalog,
                    onCatalogSearchChange = viewModel::updateCatalogSearch,
                    onCatalogCategorySelect = viewModel::setCatalogCategory,
                    onCatalogSeveritySelect = viewModel::setCatalogSeverity,
                    onToggleBookmark = viewModel::toggleBookmark,
                    onClearHistory = viewModel::clearRecentHistory,
                    onToggleTheme = viewModel::toggleDarkTheme,
                    onToggleRepairStep = viewModel::toggleRepairStep,
                    onResetRepairSteps = viewModel::resetRepairSteps,
                    buildReport = viewModel::buildDiagnosticReport
                )
            }
        }
    }
}
