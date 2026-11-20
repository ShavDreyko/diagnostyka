package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DiagnosticUiState
import com.example.model.ErrorCategory
import com.example.model.ErrorSeverity
import com.example.ui.components.ErrorListRowCard
import com.example.ui.components.categoryIcon
import com.example.ui.components.severityColors

@Composable
fun CatalogScreen(
    uiState: DiagnosticUiState,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (ErrorCategory?) -> Unit,
    onSeveritySelect: (ErrorSeverity?) -> Unit,
    onErrorClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.catalog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.catalog_count_format, uiState.catalogItems.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = uiState.catalogSearchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_search_input"),
                singleLine = true,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_text_placeholder),
                        style = MaterialTheme.typography.bodyMedium
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
                    if (uiState.catalogSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.clear_search_cd)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // Category chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "cat_all") {
                FilterChip(
                    selected = uiState.selectedCatalogCategory == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text(stringResource(R.string.catalog_all_categories)) },
                    modifier = Modifier.testTag("catalog_cat_all")
                )
            }
            items(
                items = ErrorCategory.entries,
                key = { "cat_${it.name}" }
            ) { category ->
                FilterChip(
                    selected = uiState.selectedCatalogCategory == category,
                    onClick = {
                        onCategorySelect(if (uiState.selectedCatalogCategory == category) null else category)
                    },
                    label = { Text(category.shortTitlePl) },
                    leadingIcon = {
                        Icon(
                            imageVector = categoryIcon(category),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("catalog_cat_${category.name.lowercase()}")
                )
            }
        }

        // Severity chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "sev_all") {
                FilterChip(
                    selected = uiState.selectedCatalogSeverity == null,
                    onClick = { onSeveritySelect(null) },
                    label = { Text(stringResource(R.string.catalog_filter_all)) },
                    modifier = Modifier.testTag("catalog_sev_all")
                )
            }
            items(
                items = ErrorSeverity.entries,
                key = { "sev_${it.name}" }
            ) { sev ->
                val (fg, bg) = severityColors(sev)
                FilterChip(
                    selected = uiState.selectedCatalogSeverity == sev,
                    onClick = {
                        onSeveritySelect(if (uiState.selectedCatalogSeverity == sev) null else sev)
                    },
                    label = { Text(sev.labelPl) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = bg,
                        selectedLabelColor = fg
                    ),
                    modifier = Modifier.testTag("catalog_sev_${sev.name.lowercase()}")
                )
            }
        }

        if (uiState.catalogItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Brak alarmów spełniających kryteria filtrowania",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = uiState.catalogItems,
                    key = { "catalog_${it.code}" }
                ) { error ->
                    ErrorListRowCard(
                        error = error,
                        onClick = { onErrorClick(error.code) },
                        modifier = Modifier.testTag("catalog_item_${error.code}")
                    )
                }
                item(key = "catalog_bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
