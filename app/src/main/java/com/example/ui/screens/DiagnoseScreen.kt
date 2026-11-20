package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AcoposError
import com.example.model.DiagnosticUiState
import com.example.model.ErrorCategory
import com.example.model.SearchInputMode
import com.example.ui.components.AlarmCodePill
import com.example.ui.components.ErrorListRowCard
import com.example.ui.components.SeverityBadge
import com.example.ui.components.categoryIcon
import com.example.ui.components.severityColors
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalDiagnosticColors

@Composable
fun DiagnoseScreen(
    uiState: DiagnosticUiState,
    onQueryChange: (String) -> Unit,
    onInputModeChange: (SearchInputMode) -> Unit,
    onSubmitSearch: () -> Unit,
    onErrorClick: (Int) -> Unit,
    onCategoryClick: (ErrorCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val hasQuery = uiState.searchQuery.isNotBlank()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "hero_console") {
            DiagnosticInputConsole(
                query = uiState.searchQuery,
                inputMode = uiState.searchInputMode,
                totalCount = uiState.totalErrorCount,
                hasExactMatch = uiState.exactMatch != null,
                onQueryChange = onQueryChange,
                onInputModeChange = onInputModeChange,
                onSubmit = {
                    focusManager.clearFocus()
                    onSubmitSearch()
                }
            )
        }

        // Exact match spotlight
        item(key = "exact_match") {
            AnimatedVisibility(
                visible = uiState.exactMatch != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                uiState.exactMatch?.let { match ->
                    ExactMatchSpotlightCard(
                        error = match,
                        onOpen = {
                            focusManager.clearFocus()
                            onErrorClick(match.code)
                        }
                    )
                }
            }
        }

        if (hasQuery) {
            if (uiState.searchResults.isNotEmpty()) {
                item(key = "results_header") {
                    Text(
                        text = stringResource(R.string.section_search_results, uiState.searchResults.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(
                    items = uiState.searchResults,
                    key = { "search_${it.code}" }
                ) { err ->
                    ErrorListRowCard(
                        error = err,
                        onClick = {
                            focusManager.clearFocus()
                            onErrorClick(err.code)
                        },
                        modifier = Modifier.testTag("search_result_${err.code}")
                    )
                }
            } else if (uiState.exactMatch == null) {
                item(key = "no_results") {
                    EmptySearchStateCard(query = uiState.searchQuery.trim())
                }
            }
        } else {
            // Common service faults quick strip
            if (uiState.commonFaults.isNotEmpty()) {
                item(key = "common_faults") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.section_common_faults),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(end = 4.dp)
                        ) {
                            items(
                                items = uiState.commonFaults,
                                key = { "common_${it.code}" }
                            ) { err ->
                                QuickFaultChipCard(
                                    error = err,
                                    onClick = { onErrorClick(err.code) }
                                )
                            }
                        }
                    }
                }
            }

            // Recent diagnoses
            if (uiState.recentErrors.isNotEmpty()) {
                item(key = "recent_header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.section_recent),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                items(
                    items = uiState.recentErrors.take(4),
                    key = { "recent_${it.code}" }
                ) { err ->
                    ErrorListRowCard(
                        error = err,
                        onClick = { onErrorClick(err.code) },
                        modifier = Modifier.testTag("recent_item_${err.code}")
                    )
                }
            }

            // Category 2-column grid
            item(key = "categories_header") {
                Text(
                    text = stringResource(R.string.section_categories),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            val categoryPairs = ErrorCategory.entries.chunked(2)
            items(
                items = categoryPairs,
                key = { pair -> "cat_row_${pair.first().name}" }
            ) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { category ->
                        CategoryTileCard(
                            category = category,
                            count = uiState.categoryCounts[category] ?: 0,
                            onClick = { onCategoryClick(category) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DiagnosticInputConsole(
    query: String,
    inputMode: SearchInputMode,
    totalCount: Int,
    hasExactMatch: Boolean,
    onQueryChange: (String) -> Unit,
    onInputModeChange: (SearchInputMode) -> Unit,
    onSubmit: () -> Unit
) {
    val sem = LocalDiagnosticColors.current
    val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val secondaryGlow = MaterialTheme.colorScheme.secondary.copy(alpha = 0.09f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, sem.subtleBorder),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryGlow, androidx.compose.ui.graphics.Color.Transparent),
                            center = Offset(size.width * 0.92f, size.height * 0.12f),
                            radius = size.width * 0.75f
                        )
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(secondaryGlow, androidx.compose.ui.graphics.Color.Transparent),
                            center = Offset(size.width * 0.08f, size.height * 0.95f),
                            radius = size.width * 0.6f
                        )
                    )
                }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.subtitle_brand),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Wpisz kod alarmu",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, sem.subtleBorder)
                ) {
                    Text(
                        text = "$totalCount KODÓW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            // Input mode toggle chips (123 Numeric vs ABC Text)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = inputMode == SearchInputMode.NUMERIC_CODE,
                    onClick = { onInputModeChange(SearchInputMode.NUMERIC_CODE) },
                    label = { Text(stringResource(R.string.mode_numeric)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Dialpad,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("mode_numeric_chip")
                )
                FilterChip(
                    selected = inputMode == SearchInputMode.TEXT_SEARCH,
                    onClick = { onInputModeChange(SearchInputMode.TEXT_SEARCH) },
                    label = { Text(stringResource(R.string.mode_text)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Keyboard,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag("mode_text_chip")
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alarm_code_input"),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = if (inputMode == SearchInputMode.NUMERIC_CODE) {
                        JetBrainsMonoFontFamily
                    } else {
                        MaterialTheme.typography.titleMedium.fontFamily
                    }
                ),
                placeholder = {
                    Text(
                        text = if (inputMode == SearchInputMode.NUMERIC_CODE) {
                            stringResource(R.string.search_code_placeholder)
                        } else {
                            stringResource(R.string.search_text_placeholder)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.clear_search_cd)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (inputMode == SearchInputMode.NUMERIC_CODE) {
                        KeyboardType.Number
                    } else {
                        KeyboardType.Text
                    },
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSubmit() }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            Button(
                onClick = onSubmit,
                enabled = query.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("diagnose_submit_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (hasExactMatch) {
                        stringResource(R.string.open_diagnostics)
                    } else {
                        stringResource(R.string.diagnose_action)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ExactMatchSpotlightCard(
    error: AcoposError,
    onOpen: () -> Unit
) {
    val (fg, bg) = severityColors(error.severity)
    Surface(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exact_match_card_${error.code}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, fg.copy(alpha = 0.65f)),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = bg
                ) {
                    Text(
                        text = stringResource(R.string.exact_match_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = fg,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                SeverityBadge(severity = error.severity)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = error.code.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = fg
                )
                Text(
                    text = error.titlePl,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = error.problemDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${error.repairSteps.size} kroków naprawczych",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Szczegóły",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickFaultChipCard(
    error: AcoposError,
    onClick: () -> Unit
) {
    val sem = LocalDiagnosticColors.current
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(172.dp)
            .testTag("common_fault_${error.code}"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, sem.subtleBorder),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlarmCodePill(code = error.code, severity = error.severity)
                Icon(
                    imageVector = categoryIcon(error.category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = error.titlePl,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
        }
    }
}

@Composable
private fun CategoryTileCard(
    category: ErrorCategory,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sem = LocalDiagnosticColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.testTag("category_card_${category.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, sem.subtleBorder),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = category.titlePl,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
            Text(
                text = category.rangeHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptySearchStateCard(query: String) {
    val sem = LocalDiagnosticColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, sem.subtleBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = stringResource(R.string.no_results_title, query),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.no_results_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
