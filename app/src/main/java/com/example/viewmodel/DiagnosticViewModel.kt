package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AcoposDiagnosticCurated
import com.example.data.AcoposDiagnosticEngine
import com.example.data.DiagnosticPreferencesRepository
import com.example.model.AcoposError
import com.example.model.DiagnosticUiState
import com.example.model.ErrorCategory
import com.example.model.ErrorSeverity
import com.example.model.MainTab
import com.example.model.SearchInputMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer

class DiagnosticViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = DiagnosticPreferencesRepository(application.applicationContext)

    private var allErrors: List<AcoposError> = emptyList()
    private var errorByCode: Map<Int, AcoposError> = emptyMap()
    private var normalizedIndex: Map<Int, String> = emptyMap()

    private val _uiState = MutableStateFlow(DiagnosticUiState())
    val uiState: StateFlow<DiagnosticUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
        observePreferences()
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                AcoposDiagnosticEngine.buildCatalog(getApplication<Application>().assets)
            }
            allErrors = loaded
            errorByCode = loaded.associateBy { it.code }
            normalizedIndex = loaded.associate { err ->
                err.code to normalizeForSearch(
                    "${err.code} ${err.titlePl} ${err.titleEn.orEmpty()} ${err.infoDetail.orEmpty()} ${err.category.titlePl} ${err.problemDescription}"
                )
            }

            val counts = ErrorCategory.entries.associateWith { cat ->
                loaded.count { it.category == cat }
            }
            val commons = AcoposDiagnosticCurated.commonFaultCodes.mapNotNull { errorByCode[it] }

            _uiState.update { state ->
                val updated = state.copy(
                    isLoading = false,
                    totalErrorCount = loaded.size,
                    commonFaults = commons,
                    categoryCounts = counts,
                    bookmarkedErrors = state.bookmarkedCodes.sorted().mapNotNull { errorByCode[it] },
                    recentErrors = state.recentCodes.mapNotNull { errorByCode[it] }
                )
                applyCatalogFilter(applySearch(updated))
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            prefsRepo.preferencesFlow.collect { prefs ->
                _uiState.update { state ->
                    state.copy(
                        bookmarkedCodes = prefs.bookmarkedCodes,
                        bookmarkedErrors = prefs.bookmarkedCodes.sorted().mapNotNull { errorByCode[it] },
                        recentCodes = prefs.recentCodes,
                        recentErrors = prefs.recentCodes.mapNotNull { errorByCode[it] },
                        isDarkTheme = prefs.isDarkTheme
                    )
                }
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setSearchInputMode(mode: SearchInputMode) {
        _uiState.update { it.copy(searchInputMode = mode) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            applySearch(state.copy(searchQuery = query))
        }
    }

    fun submitSearch(): Boolean {
        val state = _uiState.value
        val exact = state.exactMatch
        if (exact != null) {
            openErrorDetail(exact.code)
            return true
        }
        if (state.searchResults.size == 1) {
            openErrorDetail(state.searchResults.first().code)
            return true
        }
        return false
    }

    fun openCategoryInCatalog(category: ErrorCategory) {
        _uiState.update { state ->
            val next = state.copy(
                activeTab = MainTab.CATALOG,
                selectedCatalogCategory = category,
                selectedCatalogSeverity = null,
                catalogSearchQuery = "",
                detailBackStack = emptyList(),
                selectedError = null,
                relatedErrors = emptyList()
            )
            applyCatalogFilter(next)
        }
    }

    fun setCatalogCategory(category: ErrorCategory?) {
        _uiState.update { state ->
            applyCatalogFilter(state.copy(selectedCatalogCategory = category))
        }
    }

    fun setCatalogSeverity(severity: ErrorSeverity?) {
        _uiState.update { state ->
            applyCatalogFilter(state.copy(selectedCatalogSeverity = severity))
        }
    }

    fun updateCatalogSearch(query: String) {
        _uiState.update { state ->
            applyCatalogFilter(state.copy(catalogSearchQuery = query))
        }
    }

    fun openErrorDetail(code: Int) {
        val error = errorByCode[code] ?: return
        val related = error.relatedCodes.mapNotNull { errorByCode[it] }
        _uiState.update { state ->
            val stack = if (state.detailBackStack.lastOrNull() == code) {
                state.detailBackStack
            } else {
                state.detailBackStack + code
            }
            state.copy(
                detailBackStack = stack,
                selectedError = error,
                relatedErrors = related
            )
        }
        viewModelScope.launch {
            prefsRepo.recordRecent(code)
        }
    }

    fun navigateBackFromDetail(): Boolean {
        val stack = _uiState.value.detailBackStack
        if (stack.isEmpty()) return false
        val newStack = stack.dropLast(1)
        val previousCode = newStack.lastOrNull()
        val previousError = previousCode?.let { errorByCode[it] }
        val related = previousError?.relatedCodes?.mapNotNull { errorByCode[it] }.orEmpty()
        _uiState.update {
            it.copy(
                detailBackStack = newStack,
                selectedError = previousError,
                relatedErrors = related
            )
        }
        return true
    }

    fun closeDetailPane() {
        _uiState.update {
            it.copy(
                detailBackStack = emptyList(),
                selectedError = null,
                relatedErrors = emptyList()
            )
        }
    }

    fun toggleBookmark(code: Int) {
        viewModelScope.launch {
            prefsRepo.toggleBookmark(code)
        }
    }

    fun clearRecentHistory() {
        viewModelScope.launch {
            prefsRepo.clearRecents()
        }
    }

    fun toggleDarkTheme() {
        val next = !_uiState.value.isDarkTheme
        viewModelScope.launch {
            prefsRepo.setDarkTheme(next)
        }
    }

    fun toggleRepairStep(code: Int, stepIndex: Int) {
        _uiState.update { state ->
            val currentSet = state.checkedStepsByCode[code].orEmpty()
            val updatedSet = if (stepIndex in currentSet) currentSet - stepIndex else currentSet + stepIndex
            state.copy(checkedStepsByCode = state.checkedStepsByCode + (code to updatedSet))
        }
    }

    fun resetRepairSteps(code: Int) {
        _uiState.update { state ->
            state.copy(checkedStepsByCode = state.checkedStepsByCode - code)
        }
    }

    fun buildDiagnosticReport(error: AcoposError): String {
        val checked = _uiState.value.checkedStepsByCode[error.code].orEmpty()
        return buildString {
            appendLine("=== RAPORT DIAGNOSTYCZNY ACOPOS ===")
            appendLine("Kod alarmu: ${error.code} [${error.severity.labelPl.uppercase()}]")
            appendLine("Kategoria: ${error.category.titlePl}")
            appendLine("Komunikat (PL): ${error.titlePl}")
            if (!error.titleEn.isNullOrBlank()) {
                appendLine("Komunikat (EN): ${error.titleEn}")
            }
            if (!error.infoDetail.isNullOrBlank()) {
                appendLine("Parametr: ${error.infoDetail}")
            }
            appendLine()
            appendLine("Opis problemu:")
            appendLine(error.problemDescription)
            appendLine()
            appendLine("Reakcja napędu:")
            appendLine(error.driveReaction)
            appendLine()
            appendLine("Sugerowane kroki naprawcze:")
            error.repairSteps.forEachIndexed { idx, step ->
                val mark = if (idx in checked) "[x]" else "[ ]"
                appendLine("$mark ${idx + 1}. $step")
            }
        }
    }

    private fun applySearch(state: DiagnosticUiState): DiagnosticUiState {
        val raw = state.searchQuery.trim()
        if (raw.isEmpty()) {
            return state.copy(exactMatch = null, searchResults = emptyList())
        }

        val cleanedDigits = raw.replace(".", "").replace(" ", "")
        val exactCode = cleanedDigits.toIntOrNull()
        val exact = exactCode?.let { errorByCode[it] }

        val results = searchErrors(raw, excludeCode = exact?.code)
        return state.copy(
            exactMatch = exact,
            searchResults = results.take(40)
        )
    }

    private fun applyCatalogFilter(state: DiagnosticUiState): DiagnosticUiState {
        val query = state.catalogSearchQuery.trim()
        val base = if (query.isEmpty()) allErrors else searchErrors(query)
        val filtered = base.filter { err ->
            (state.selectedCatalogCategory == null || err.category == state.selectedCatalogCategory) &&
                (state.selectedCatalogSeverity == null || err.severity == state.selectedCatalogSeverity)
        }
        return state.copy(catalogItems = filtered)
    }

    private fun searchErrors(query: String, excludeCode: Int? = null): List<AcoposError> {
        val trimmed = query.trim()
        val digitsOnly = trimmed.replace(".", "").replace(" ", "")
        val isNumericQuery = digitsOnly.isNotEmpty() && digitsOnly.all { it.isDigit() }
        val normalizedQuery = normalizeForSearch(trimmed)
        val tokens = normalizedQuery.split(" ").filter { it.isNotBlank() }

        val scored = ArrayList<Pair<Int, AcoposError>>()
        for (err in allErrors) {
            if (err.code == excludeCode) continue
            val codeStr = err.code.toString()
            val score = when {
                isNumericQuery && codeStr == digitsOnly -> 1000
                isNumericQuery && codeStr.startsWith(digitsOnly) -> 700 - codeStr.length
                isNumericQuery && codeStr.contains(digitsOnly) -> 400
                else -> {
                    val hay = normalizedIndex[err.code].orEmpty()
                    val titleNorm = normalizeForSearch(err.titlePl)
                    when {
                        tokens.isNotEmpty() && tokens.all { hay.contains(it) } -> {
                            val titleBoost = if (tokens.all { titleNorm.contains(it) }) 250 else 0
                            val exactPhraseBoost = if (titleNorm.contains(normalizedQuery)) 150 else 0
                            200 + titleBoost + exactPhraseBoost
                        }
                        else -> 0
                    }
                }
            }
            if (score > 0) {
                scored.add(score to err)
            }
        }
        return scored
            .sortedWith(compareByDescending<Pair<Int, AcoposError>> { it.first }.thenBy { it.second.code })
            .map { it.second }
    }

    private fun normalizeForSearch(input: String): String {
        val lowered = input.lowercase()
            .replace('ł', 'l')
            .replace('ą', 'a')
            .replace('ć', 'c')
            .replace('ę', 'e')
            .replace('ń', 'n')
            .replace('ó', 'o')
            .replace('ś', 's')
            .replace('ź', 'z')
            .replace('ż', 'z')
        val decomposed = Normalizer.normalize(lowered, Normalizer.Form.NFD)
        return decomposed.replace(Regex("""\p{InCombiningDiacriticalMarks}+"""), "")
    }
}
