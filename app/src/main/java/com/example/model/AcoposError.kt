package com.example.model

enum class ErrorSeverity(val labelPl: String, val priority: Int) {
    CRITICAL("Krytyczny", 0),
    ERROR("Błąd", 1),
    WARNING("Ostrzeżenie", 2),
    INFO("Informacja", 3)
}

enum class ErrorCategory(
    val titlePl: String,
    val shortTitlePl: String,
    val rangeHint: String
) {
    POWER_DC_BUS("Zasilanie i szyna DC", "Szyna DC", "7200–7227"),
    POWER_STAGE_BRAKE("Stopień mocy i hamulec", "Moc / Hamulec", "6017–6063, 38000+"),
    ENCODER_FEEDBACK("Enkoder i sprzężenie", "Enkoder", "7012–7061, 39001+"),
    THERMAL_OVERLOAD("Temperatura i przeciążenie", "Termika", "9000–9300, 41001+"),
    MOTION_HOMING("Ruch, limity i bazowanie", "Ruch / Bazow.", "4005–5039"),
    SAFETY_SAFEMC("Bezpieczeństwo SafeMC", "SafeMC", "35000–35255"),
    NETWORK_COMM("Sieć CAN / POWERLINK", "Sieć", "1004–1024, 32001+"),
    CAM_SYNC("Krzywki i synchronizacja", "Krzywki", "5101–5329, 37101+"),
    PLCOPEN_NC("PLCopen i sterownik NC", "PLCopen / NC", "29200–32767"),
    PARAMETERS_MEMORY("Parametry, EEPROM i system", "Parametry", "1–1027, 8001–8020")
}

data class AcoposError(
    val code: Int,
    val titlePl: String,
    val titleEn: String?,
    val infoDetail: String?,
    val infoExplanationPl: String?,
    val category: ErrorCategory,
    val severity: ErrorSeverity,
    val problemDescription: String,
    val driveReaction: String,
    val possibleCauses: List<String>,
    val repairSteps: List<String>,
    val requiresHighVoltageCaution: Boolean = false,
    val relatedCodes: List<Int> = emptyList()
)

enum class MainTab {
    DIAGNOSE,
    CATALOG,
    SAVED
}

enum class SearchInputMode {
    NUMERIC_CODE,
    TEXT_SEARCH
}

data class DiagnosticUiState(
    val isLoading: Boolean = true,
    val totalErrorCount: Int = 0,
    val activeTab: MainTab = MainTab.DIAGNOSE,
    val searchQuery: String = "",
    val searchInputMode: SearchInputMode = SearchInputMode.NUMERIC_CODE,
    val exactMatch: AcoposError? = null,
    val searchResults: List<AcoposError> = emptyList(),
    val commonFaults: List<AcoposError> = emptyList(),
    val categoryCounts: Map<ErrorCategory, Int> = emptyMap(),
    val selectedCatalogCategory: ErrorCategory? = null,
    val selectedCatalogSeverity: ErrorSeverity? = null,
    val catalogSearchQuery: String = "",
    val catalogItems: List<AcoposError> = emptyList(),
    val detailBackStack: List<Int> = emptyList(),
    val selectedError: AcoposError? = null,
    val relatedErrors: List<AcoposError> = emptyList(),
    val bookmarkedCodes: Set<Int> = emptySet(),
    val bookmarkedErrors: List<AcoposError> = emptyList(),
    val recentCodes: List<Int> = emptyList(),
    val recentErrors: List<AcoposError> = emptyList(),
    val checkedStepsByCode: Map<Int, Set<Int>> = emptyMap(),
    val isDarkTheme: Boolean = true
)
