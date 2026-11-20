package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AcoposError
import com.example.model.DiagnosticUiState
import com.example.model.ErrorCategory
import com.example.model.ErrorSeverity
import com.example.model.MainTab
import com.example.model.SearchInputMode
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.DiagnoseScreen
import com.example.ui.screens.ErrorDetailScreen
import com.example.ui.screens.SavedScreen
import com.example.ui.theme.LocalDiagnosticColors
import kotlinx.coroutines.launch

private data class NavDestination(
    val tab: MainTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val navDestinations = listOf(
    NavDestination(
        tab = MainTab.DIAGNOSE,
        labelRes = R.string.nav_diagnose,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
        testTag = "nav_diagnose"
    ),
    NavDestination(
        tab = MainTab.CATALOG,
        labelRes = R.string.nav_catalog,
        selectedIcon = Icons.AutoMirrored.Filled.ListAlt,
        unselectedIcon = Icons.AutoMirrored.Outlined.ListAlt,
        testTag = "nav_catalog"
    ),
    NavDestination(
        tab = MainTab.SAVED,
        labelRes = R.string.nav_saved,
        selectedIcon = Icons.Filled.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder,
        testTag = "nav_saved"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcoposApp(
    uiState: DiagnosticUiState,
    onSelectTab: (MainTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onInputModeChange: (SearchInputMode) -> Unit,
    onSubmitSearch: () -> Unit,
    onOpenErrorDetail: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenCategoryInCatalog: (ErrorCategory) -> Unit,
    onCatalogSearchChange: (String) -> Unit,
    onCatalogCategorySelect: (ErrorCategory?) -> Unit,
    onCatalogSeveritySelect: (ErrorSeverity?) -> Unit,
    onToggleBookmark: (Int) -> Unit,
    onClearHistory: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleRepairStep: (Int, Int) -> Unit,
    onResetRepairSteps: (Int) -> Unit,
    buildReport: (AcoposError) -> String,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val copiedToastText = stringResource(R.string.detail_copied_toast)

    BackHandler(enabled = uiState.selectedError != null) {
        onNavigateBack()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isExpanded = maxWidth >= 840.dp
        val showCompactDetail = !isExpanded && uiState.selectedError != null

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (!showCompactDetail) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MonitorHeart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = onToggleTheme,
                                modifier = Modifier.testTag("toggle_theme_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = stringResource(R.string.toggle_theme_cd),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            bottomBar = {
                if (!isExpanded && !showCompactDetail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars,
                        tonalElevation = 3.dp
                    ) {
                        navDestinations.forEach { dest ->
                            val selected = uiState.activeTab == dest.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Wczytywanie bazy błędów ACOPOS…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        navDestinations.forEach { dest ->
                            val selected = uiState.activeTab == dest.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onSelectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                    VerticalDivider(color = LocalDiagnosticColors.current.subtleBorder)

                    Box(
                        modifier = Modifier
                            .weight(0.46f)
                            .fillMaxHeight()
                    ) {
                        TabContentHost(
                            uiState = uiState,
                            onQueryChange = onQueryChange,
                            onInputModeChange = onInputModeChange,
                            onSubmitSearch = onSubmitSearch,
                            onOpenErrorDetail = onOpenErrorDetail,
                            onOpenCategoryInCatalog = onOpenCategoryInCatalog,
                            onCatalogSearchChange = onCatalogSearchChange,
                            onCatalogCategorySelect = onCatalogCategorySelect,
                            onCatalogSeveritySelect = onCatalogSeveritySelect,
                            onClearHistory = onClearHistory
                        )
                    }

                    VerticalDivider(color = LocalDiagnosticColors.current.subtleBorder)

                    Box(
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight()
                    ) {
                        val detail = uiState.selectedError
                        if (detail != null) {
                            ErrorDetailScreen(
                                error = detail,
                                relatedErrors = uiState.relatedErrors,
                                isBookmarked = detail.code in uiState.bookmarkedCodes,
                                checkedSteps = uiState.checkedStepsByCode[detail.code].orEmpty(),
                                showBackButton = uiState.detailBackStack.size > 1,
                                onBack = onNavigateBack,
                                onToggleBookmark = { onToggleBookmark(detail.code) },
                                onCopyReport = {
                                    clipboardManager.setText(AnnotatedString(buildReport(detail)))
                                    scope.launch { snackbarHostState.showSnackbar(copiedToastText) }
                                },
                                onToggleStep = { idx -> onToggleRepairStep(detail.code, idx) },
                                onResetSteps = { onResetRepairSteps(detail.code) },
                                onOpenRelated = onOpenErrorDetail
                            )
                        } else {
                            ExpandedDetailPlaceholder()
                        }
                    }
                }
            } else {
                // Compact / Medium layout
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val detail = uiState.selectedError
                    if (detail != null) {
                        ErrorDetailScreen(
                            error = detail,
                            relatedErrors = uiState.relatedErrors,
                            isBookmarked = detail.code in uiState.bookmarkedCodes,
                            checkedSteps = uiState.checkedStepsByCode[detail.code].orEmpty(),
                            showBackButton = true,
                            onBack = onNavigateBack,
                            onToggleBookmark = { onToggleBookmark(detail.code) },
                            onCopyReport = {
                                clipboardManager.setText(AnnotatedString(buildReport(detail)))
                                scope.launch { snackbarHostState.showSnackbar(copiedToastText) }
                            },
                            onToggleStep = { idx -> onToggleRepairStep(detail.code, idx) },
                            onResetSteps = { onResetRepairSteps(detail.code) },
                            onOpenRelated = onOpenErrorDetail
                        )
                    } else {
                        TabContentHost(
                            uiState = uiState,
                            onQueryChange = onQueryChange,
                            onInputModeChange = onInputModeChange,
                            onSubmitSearch = onSubmitSearch,
                            onOpenErrorDetail = onOpenErrorDetail,
                            onOpenCategoryInCatalog = onOpenCategoryInCatalog,
                            onCatalogSearchChange = onCatalogSearchChange,
                            onCatalogCategorySelect = onCatalogCategorySelect,
                            onCatalogSeveritySelect = onCatalogSeveritySelect,
                            onClearHistory = onClearHistory
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabContentHost(
    uiState: DiagnosticUiState,
    onQueryChange: (String) -> Unit,
    onInputModeChange: (SearchInputMode) -> Unit,
    onSubmitSearch: () -> Unit,
    onOpenErrorDetail: (Int) -> Unit,
    onOpenCategoryInCatalog: (ErrorCategory) -> Unit,
    onCatalogSearchChange: (String) -> Unit,
    onCatalogCategorySelect: (ErrorCategory?) -> Unit,
    onCatalogSeveritySelect: (ErrorSeverity?) -> Unit,
    onClearHistory: () -> Unit
) {
    AnimatedContent(
        targetState = uiState.activeTab,
        label = "tabTransition"
    ) { tab ->
        when (tab) {
            MainTab.DIAGNOSE -> DiagnoseScreen(
                uiState = uiState,
                onQueryChange = onQueryChange,
                onInputModeChange = onInputModeChange,
                onSubmitSearch = onSubmitSearch,
                onErrorClick = onOpenErrorDetail,
                onCategoryClick = onOpenCategoryInCatalog
            )
            MainTab.CATALOG -> CatalogScreen(
                uiState = uiState,
                onSearchChange = onCatalogSearchChange,
                onCategorySelect = onCatalogCategorySelect,
                onSeveritySelect = onCatalogSeveritySelect,
                onErrorClick = onOpenErrorDetail
            )
            MainTab.SAVED -> SavedScreen(
                uiState = uiState,
                onErrorClick = onOpenErrorDetail,
                onClearHistory = onClearHistory
            )
        }
    }
}

@Composable
private fun ExpandedDetailPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MonitorHeart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "Wybierz kod alarmu ACOPOS",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Wpisz numer błędu po lewej stronie lub wybierz pozycję z katalogu, aby wyświetlić opis problemu i listę kroków naprawczych.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
