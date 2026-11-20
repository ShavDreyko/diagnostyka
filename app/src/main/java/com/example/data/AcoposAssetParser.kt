package com.example.data

import android.content.res.AssetManager

data class RawAcoposEntry(
    val code: Int,
    val titlePl: String,
    val titleEn: String?,
    val infoDetail: String?
)

object AcoposAssetParser {

    const val PL_ASSET_FILE = "b_edy_wittman_acopos_PL_e3174ce4.txt"
    const val EN_ASSET_FILE = "lista_b_ed_w_wittman_ang_2cead588.txt"

    private val plLineRegex = Regex("""^\s*[•\-*]?\s*(\d{1,2}\.\d{3}|\d{1,5})\s*:\s*(.+)$""")
    private val enLineRegex = Regex("""^\s*(\d{1,5})\s*:\s*(.+)$""")
    private val infoLineRegex = Regex("""^\s*Info\((.+)\)\s*:\s*(.+)$""")

    fun loadAllEntries(assetManager: AssetManager): List<RawAcoposEntry> {
        val plMap = parsePolishText(readAssetSafe(assetManager, PL_ASSET_FILE))
        val enMap = parseEnglishText(readAssetSafe(assetManager, EN_ASSET_FILE))
        return mergeEntries(plMap, enMap)
    }

    internal fun parsePolishText(content: String): Map<Int, String> {
        val result = LinkedHashMap<Int, String>()
        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim().removePrefix("\uFEFF")
            val match = plLineRegex.matchEntire(line) ?: return@forEach
            val codeStr = match.groupValues[1].replace(".", "")
            val code = codeStr.toIntOrNull() ?: return@forEach
            val rawTitle = match.groupValues[2].trim()
            if (rawTitle.isNotEmpty()) {
                result[code] = polishTechnicalTitle(rawTitle)
            }
        }
        return result
    }

    internal fun parseEnglishText(content: String): Map<Int, Pair<String, String?>> {
        val result = LinkedHashMap<Int, Pair<String, String?>>()
        var lastCode: Int? = null
        content.lineSequence().forEach { rawLine ->
            val line = rawLine.removePrefix("\uFEFF")
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach

            val infoMatch = infoLineRegex.matchEntire(trimmed)
            if (infoMatch != null && lastCode != null) {
                val existing = result[lastCode!!]
                if (existing != null) {
                    val infoFormatted = "Info(${infoMatch.groupValues[1].trim()}): ${infoMatch.groupValues[2].trim()}"
                    val combinedInfo = if (existing.second == null) infoFormatted else "${existing.second}\n$infoFormatted"
                    result[lastCode!!] = existing.first to combinedInfo
                }
                return@forEach
            }

            val codeMatch = enLineRegex.matchEntire(trimmed)
            if (codeMatch != null) {
                val code = codeMatch.groupValues[1].toIntOrNull()
                if (code != null) {
                    val titleEn = codeMatch.groupValues[2].trim()
                    result[code] = titleEn to null
                    lastCode = code
                }
            }
        }
        return result
    }

    internal fun mergeEntries(
        plMap: Map<Int, String>,
        enMap: Map<Int, Pair<String, String?>>
    ): List<RawAcoposEntry> {
        val allCodes = (plMap.keys + enMap.keys).toSortedSet()
        return allCodes.map { code ->
            val enPair = enMap[code]
            val titleEn = enPair?.first
            val info = enPair?.second
            val titlePl = plMap[code]
                ?: enOnlyPolishTranslations[code]
                ?: translateFallbackEnglishTitle(titleEn ?: "Nieznany błąd ACOPOS")
            RawAcoposEntry(
                code = code,
                titlePl = titlePl,
                titleEn = titleEn,
                infoDetail = info
            )
        }
    }

    private fun readAssetSafe(assetManager: AssetManager, fileName: String): String {
        return try {
            assetManager.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Poprawia oczywiste kalki z tłumaczenia automatycznego pliku źródłowego (np. bleeder -> odpowietrzacz,
     * drive -> dysk, Cam Automat -> automat kamery) na poprawną polską terminologię automatyki przemysłowej.
     */
    private fun polishTechnicalTitle(raw: String): String {
        var text = raw.replace(Regex("""\s+"""), " ").trim()
        val replacements = listOf(
            "Model temperatury odpowietrzacza" to "Model temperatury rezystora hamowania (Bleeder)",
            "Odpowietrznik: brak przepływu prądu" to "Rezystor hamowania (Bleeder): brak przepływu prądu",
            "Bleeder: Overcurrent" to "Rezystor hamowania (Bleeder): przetężenie (nadprąd)",
            "Automat kamery" to "Automat krzywkowy (Cam Automat)",
            "automat kamery" to "automat krzywkowy",
            "danych profilu kamery" to "danych profilu krzywki",
            "Dysk nie odpowiada" to "Napęd nie odpowiada",
            "czy dysk jest w sieci" to "czy napęd jest podłączony w sieci",
            "na dysku" to "w napędzie",
            "z dysku" to "z napędu",
            "Dysk nie został zsynchronizowany" to "Napęd nie został zsynchronizowany",
            "typu CWU" to "typu sprzętu (HW)",
            "Spust główny" to "Wyzwalacz osi głównej (Master trigger)",
            "pozycja spustowa" to "pozycja wyzwalacza",
            "Aktywny bęben" to "Aktywny sekwencer bębnowy (Drumsequencer)",
            "Faza: Prędkość do wysokiej" to "Fazowanie: zbyt wysoka prędkość obrotowa",
            "ACOPOS: Nadprądowe" to "ACOPOS: Przetężenie (nadprąd)",
            "Stopień mocy: strona wysoka: nadprądowa" to "Stopień mocy: przetężenie po stronie górnej (High-side)",
            "Stopień mocy: strona dolna: nadprądowa" to "Stopień mocy: przetężenie po stronie dolnej (Low-side)",
            "Stopień mocy: Nadprądowy" to "Stopień mocy: przetężenie (nadprąd)",
            "Magistrala DC: nadprądowa" to "Szyna DC: przetężenie (nadprąd)",
            "Kontroler prądu: Nadprądowy" to "Regulator prądu: przetężenie (nadprąd)",
            "wysyłanie Zapis kupna (błąd sieci?) Błąd" to "Błąd wysyłania żądania zapisu (błąd sieci?)",
            "nie Położenia przełącznika w kolejności rosnącej: Cam sterowania" to "Sterowanie krzywkowe: pozycje przełączania nie są w kolejności rosnącej",
            "Nie ważne dane profilu krzywki lub wyłączonym" to "Brak prawidłowych danych profilu krzywki lub stan wyłączony",
            "Pełny wyzwalacz FIFO pełny" to "Bufor FIFO wyzwalacza Master jest pełny",
            "Parametr (y) identyfikacyjny brakujące" to "Brak wymaganych parametrów identyfikacji",
            "Monitorowanie przekroczenia limitu czasu w cyklicznych danych do kierowania" to "Przekroczenie limitu czasu monitorowania danych cyklicznych do napędu",
            "Enable1: Zwolnienie napięcia" to "Wejście Enable1: zapad napięcia",
            "Enable2: Zwolnienie napięcia" to "Wejście Enable2: zapad napięcia",
            "bit błędu hiperface" to "bit błędu interfejsu Hiperface",
            "zaburzenia przewodu: Resolwer" to "Resolwer: zakłócenie lub przerwa w przewodzie",
            "Błąd internatora, test CUnit" to "Błąd wewnętrzny modułu SafeMC (test CUnit)"
        )
        for ((from, to) in replacements) {
            if (text.contains(from)) {
                text = text.replace(from, to)
            }
        }
        return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun translateFallbackEnglishTitle(en: String): String {
        return en
            .replace("Controller", "Sterownik")
            .replace("Drive", "Napęd")
            .replace("Encoder", "Enkoder")
            .replace("Parameter", "Parametr")
            .replace("Error", "Błąd")
            .replace("Timeout", "Przekroczenie limitu czasu")
            .replace("not possible", "niemożliwe")
            .replace("is invalid", "jest nieprawidłowy")
    }

    private val enOnlyPolishTranslations: Map<Int, String> = mapOf(
        36 to "Temperatura radiatora wróciła do dopuszczalnego zakresu pracy (błąd można skasować)",
        29200 to "PLCopen: Obiekt osi jest nieprawidłowy",
        29203 to "PLCopen: Napęd nie jest gotowy do pracy (Drive not ready)",
        29204 to "PLCopen: Nieprawidłowy numer parametru",
        29205 to "PLCopen: Oś nie została zbazowana (brak referencji)",
        29206 to "PLCopen: Regulator napędu jest wyłączony (Controller off)",
        29207 to "PLCopen: Ten typ ruchu jest obecnie niedozwolony",
        29208 to "PLCopen: Obiekt osi zmienił się od ostatniego wywołania bloku FB",
        29209 to "PLCopen: Napęd znajduje się w stanie błędu",
        29210 to "PLCopen: Inicjalizacja parametrów (Global-init) nie powiodła się",
        29211 to "PLCopen: Przełączanie hamulca trzymającego niemożliwe — regulator jest włączony",
        29214 to "PLCopen: Bazowanie (Homing) jest niemożliwe w bieżącym stanie",
        29215 to "PLCopen: Ruch dyskretny (pozycjonowanie) jest niemożliwy",
        29216 to "PLCopen: Ruch ciągły (prędkościowy) jest niemożliwy",
        29217 to "PLCopen: Nieprawidłowy parametr wejściowy bloku funkcyjnego",
        29218 to "PLCopen: Nieznany stan osi PLCopen",
        29219 to "PLCopen: Nieprawidłowa wartość parametru PLCopen",
        29221 to "PLCopen: Brak zdefiniowanej nazwy profilu krzywki (Cam)",
        29222 to "PLCopen: Błąd podczas przesyłania (download) krzywki do napędu",
        29225 to "PLCopen: Pozycja docelowa wykracza poza okres osi (Axis period)",
        29226 to "PLCopen: Błąd w serwonapędzie — użyj MC_ReadAxisError w celu odczytu szczegółów",
        29227 to "PLCopen: Napęd nie może wysyłać kolejnej pozycji Master w sieci",
        29228 to "PLCopen: Napęd nie może odczytać kolejnej pozycji Master z sieci",
        29229 to "PLCopen: Ruch zsynchronizowany jest niemożliwy w bieżącym stanie",
        29230 to "PLCopen: Błąd wewnętrzny podczas transferu listy parametrów",
        29231 to "PLCopen: Nieprawidłowa prędkość osi Master (0 lub wartość ujemna)",
        29232 to "PLCopen: Błąd wewnętrzny — nieprawidłowy typ zasobu SPT",
        29233 to "PLCopen: Brak dostępnego zasobu SPT wymaganego typu",
        29234 to "PLCopen: Zbyt mała liczba wolnych zasobów SPT",
        29235 to "PLCopen: Funkcja nie jest dostępna dla bieżącego typu osi",
        29237 to "PLCopen: Błąd w parametrach wejścia wyzwalającego (TriggerInput)",
        29238 to "PLCopen: Blok funkcyjny (FB) nie może być użyty w bieżącym stanie osi",
        29239 to "PLCopen: Funkcja nie jest obsługiwana przez magistralę CAN-Bus",
        29240 to "PLCopen: Podany ParID nie może być użyty ze względu na rozmiar typu danych",
        29241 to "PLCopen: Niewłaściwy typ danych dla określonego ParID",
        29242 to "PLCopen: Bufor cyklicznego odczytu danych jest pełny",
        29244 to "PLCopen: Błąd wewnętrzny podczas konfiguracji danych cyklicznych",
        29246 to "PLCopen: Nieprawidłowe okno pomiarowe funkcji TouchProbe",
        29247 to "PLCopen: Nie można osiągnąć pozycji synchronizacji osi Master",
        29250 to "PLCopen: Nieprawidłowy identyfikator tabeli krzywki (CamTableID)",
        29251 to "PLCopen: Błąd podczas pobierania tabeli parametrów do napędu",
        29252 to "PLCopen: Błąd inicjalizacji listy parametrów",
        29253 to "PLCopen: Błąd podczas pobierania sekwencji parametrów",
        29254 to "PLCopen: Błąd inicjalizacji sekwencji parametrów",
        29255 to "PLCopen: Inicjalizacja niemożliwa — sprzężenie osi jest aktywne",
        29256 to "PLCopen: Wykonywanie wielu poleceń jednocześnie jest niedozwolone",
        29257 to "PLCopen: Podany adres danych jest nieprawidłowy",
        29260 to "PLCopen: Nie określono nazwy obiektu danych",
        29261 to "PLCopen: Nieprawidłowy indeks obiektu danych",
        29262 to "PLCopen: Kanał Master jest już w użyciu",
        29263 to "PLCopen: Kanał Slave jest już w użyciu",
        29264 to "PLCopen: Bufor cyklicznego zapisu danych jest pełny",
        29265 to "PLCopen: Komunikacja z serwonapędem nie powiodła się",
        29266 to "PLCopen: Parametr MasterParID zmienił się od ostatniego wywołania FB",
        29267 to "PLCopen: Nieprawidłowa liczba wielomianów profilu krzywki",
        29268 to "PLCopen: Blok funkcyjny PLCopen został przerwany przez inny blok",
        29269 to "PLCopen: Błąd podczas zapisywania modułu parametrów NC-INIT",
        29270 to "PLCopen: Błąd podczas wczytywania modułu parametrów NC-INIT",
        29271 to "PLCopen: Wybrany blok MC_TouchProbe nie jest aktywny",
        29272 to "PLCopen: Błąd inicjalizacji automatu krzywkowego",
        29273 to "PLCopen: Podany parametr „Subject” jest nieprawidłowy",
        29274 to "PLCopen: Błąd podczas inicjalizacji parametrów",
        29275 to "PLCopen: Zmieniono wartość wejściową podczas gdy Enable = TRUE",
        29276 to "PLCopen: Przesunięcie fazowe (Phase shift) jest już w toku",
        29277 to "PLCopen: Przesunięcie offsetu (Offset shift) jest już w toku",
        29278 to "PLCopen: Wybrany tryb wymaga podania okresu (Period)",
        29279 to "PLCopen: Nie można obliczyć wartości wyjściowej",
        29280 to "PLCopen: Brak zdefiniowanej prawidłowej osi Master",
        29281 to "PLCopen: Funkcja nie jest dostępna dla napędów ACOPOSmulti",
        29282 to "PLCopen: Polecenie nie może być obecnie wykonane",
        29283 to "PLCopen: Pozycja Master lub Slave pierwszego punktu krzywki nie wynosi 0",
        29284 to "PLCopen: Zbyt mała liczba punktów krzywej krzywki",
        29285 to "PLCopen: Nieprawidłowy typ sekcji profilu krzywki",
        29286 to "PLCopen: Nieprawidłowy tryb dla ostatniego punktu profilu krzywki",
        29287 to "PLCopen: Nieprawidłowa pozycja Master lub Slave ostatniego punktu krzywki",
        29288 to "PLCopen: Pozycje osi Master nie rosną ściśle monotonicznie",
        29289 to "PLCopen: Nieprawidłowe parametry brzegowe krzywej",
        29290 to "PLCopen: Zbyt duża liczba wielomianów profilu krzywki",
        29291 to "PLCopen: Punkt zwrotny znajduje się poza sekcją profilu krzywki",
        29292 to "PLCopen: Identyczne pozycje osi Slave są niedozwolone",
        29293 to "PLCopen: Podana długość danych wynosi 0 lub jest zbyt mała",
        29294 to "PLCopen: Nie można ustalić tekstu błędu — sprawdź łańcuch tekstowy błędu",
        29295 to "PLCopen: Wystąpił błąd — sprawdź wyjście ErrorRecord",
        29297 to "PLCopen: Problem ze zmienną w pamięci nieulotnej (Permanent memory)",
        29299 to "PLCopen: Wystąpił błąd podczas operacji strojenia (Setup)",
        29300 to "PLCopen: Nieprawidłowa liczba wielomianów w krzywce",
        29301 to "PLCopen: Nie można obliczyć wartości krzywki",
        29302 to "PLCopen: Jedna instancja bloku funkcyjnego jest już aktywna",
        29303 to "PLCopen: Podany czas interwału (IntervalTime) jest zbyt krótki",
        29305 to "PLCopen: Nie można odczytać ParID w tym trybie pracy",
        29306 to "PLCopen: Nieprawidłowy tryb interpolatora",
        29307 to "PLCopen: Okres osi Master wynosi zero",
        29308 to "PLCopen: Wewnętrzny błąd obliczeniowy",
        29309 to "PLCopen: Ogólny błąd wewnętrzny bloku PLCopen",
        29310 to "PLCopen: Obliczona kompensacja przekracza wartości graniczne",
        29311 to "PLCopen: Upłynął maksymalny dopuszczalny czas operacji",
        29312 to "PLCopen: Wystąpił błąd podczas testu hamulca trzymającego",
        29489 to "PLCopen: Wewnętrzne wartości struktury osi są nieprawidłowe",
        29490 to "PLCopen: Błąd inicjalizacji wewnętrznej (Global init)",
        29491 to "PLCopen: Błąd inicjalizacji wewnętrznej (limity programowe SW)",
        29492 to "PLCopen: Błąd inicjalizacji wewnętrznej (bazowanie osi wirtualnej)",
        29498 to "Biblioteka ACP10_MC: Przerwano inicjalizację",
        29499 to "Biblioteka ACP10_MC: Błąd ze szczegółami w polu ASCII Data",
        32294 to "Menedżer NC: Wskaźnik funkcji nctune_sequ() ma wartość zero",
        32295 to "Menedżer NC: Zerowy adres struktury danych przy wywołaniu nctune_sequ()",
        32296 to "Menedżer NC: Nieprawidłowy rozmiar struktury danych przy wywołaniu nctune_sequ()",
        32297 to "Menedżer NC: Zerowy adres obiektu NC przy wywołaniu nctune_sequ()",
        32298 to "Menedżer NC: Nieznany numer błędu podczas wywołania nctune_sequ()",
        32300 to "Strojenie regulatora: Wymagane wgranie biblioteki acp10tun do sterownika",
        32301 to "Strojenie regulatora: Uruchomienie niedozwolone — występują niekasowane błędy (count.error > 0)",
        32303 to "Strojenie regulatora: Oś nie przeszła inicjalizacji globalnej",
        32304 to "Strojenie regulatora: Regulator musi być wyłączony przed startem strojenia",
        32305 to "Strojenie regulatora: Regulator napędu nie jest gotowy",
        32306 to "Strojenie regulatora: Rejestrator Trace jest już aktywny",
        32307 to "Strojenie regulatora: Procent prądu znamionowego przekracza wartość maksymalną",
        32308 to "Strojenie regulatora: Procent prądu znamionowego poniżej wartości minimalnej",
        32309 to "Strojenie regulatora: Procent prędkości przekracza wartość maksymalną",
        32310 to "Strojenie regulatora: Procent prędkości poniżej wartości minimalnej",
        32311 to "Strojenie regulatora: Dystans ruchu przekracza wartość maksymalną",
        32312 to "Strojenie regulatora: Dystans ruchu poniżej wartości minimalnej",
        32313 to "Strojenie regulatora: Wybrany tryb strojenia wymaga zbazowania osi",
        32314 to "Strojenie regulatora: Nieprawidłowy tryb strojenia",
        32315 to "Strojenie regulatora: Nieprawidłowy tryb strojenia w bibliotece",
        32316 to "Strojenie regulatora: Obliczony moment bezwładności wynosi zero",
        32317 to "Strojenie regulatora: Wzmocnienie regulatora prędkości SCTRL_KV wynosi zero",
        32318 to "Strojenie regulatora: Wzmocnienie regulatora pozycji POS_CTRL_KV wynosi zero",
        32319 to "Strojenie regulatora: Nie udało się obliczyć parametrów wynikowych",
        32320 to "Strojenie regulatora: Limity prędkości są zbyt wysokie względem MOTOR_SPEED_RATED",
        32321 to "Strojenie regulatora: Współczynnik a0 wynosi zero (filtr wycinający ncISQ_F1_NOTCH niemożliwy)"
    )
}
