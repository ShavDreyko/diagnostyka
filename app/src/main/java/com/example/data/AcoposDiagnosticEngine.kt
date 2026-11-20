package com.example.data

import android.content.res.AssetManager
import com.example.model.AcoposError
import com.example.model.ErrorCategory
import com.example.model.ErrorSeverity

object AcoposDiagnosticEngine {

    fun buildCatalog(assetManager: AssetManager): List<AcoposError> {
        val rawEntries = AcoposAssetParser.loadAllEntries(assetManager)
        val allCodeSet = rawEntries.map { it.code }.toSet()
        return rawEntries.map { raw -> buildDiagnosticError(raw, allCodeSet) }
    }

    internal fun buildDiagnosticError(raw: RawAcoposEntry, allCodeSet: Set<Int>): AcoposError {
        val curated = AcoposDiagnosticCurated.entries[raw.code]
        val infoExplanation = explainInfoDetail(raw.infoDetail)

        if (curated != null) {
            return AcoposError(
                code = raw.code,
                titlePl = raw.titlePl,
                titleEn = raw.titleEn,
                infoDetail = raw.infoDetail,
                infoExplanationPl = infoExplanation,
                category = curated.category,
                severity = curated.severity,
                problemDescription = curated.problemDescription,
                driveReaction = curated.driveReaction,
                possibleCauses = curated.possibleCauses,
                repairSteps = curated.repairSteps,
                requiresHighVoltageCaution = curated.requiresHighVoltageCaution,
                relatedCodes = curated.relatedCodes.filter { it in allCodeSet && it != raw.code }
            )
        }

        val category = classifyCategory(raw.code, raw.titlePl, raw.titleEn)
        val severity = classifySeverity(raw.code, raw.titlePl, raw.titleEn, category)
        val requiresHv = requiresHighVoltageWarning(raw.code, raw.titlePl, raw.titleEn, category)
        val synthesized = synthesizeDetails(raw, category, severity)
        val related = computeRelatedCodes(raw.code, allCodeSet)

        return AcoposError(
            code = raw.code,
            titlePl = raw.titlePl,
            titleEn = raw.titleEn,
            infoDetail = raw.infoDetail,
            infoExplanationPl = infoExplanation,
            category = category,
            severity = severity,
            problemDescription = synthesized.problemDescription,
            driveReaction = synthesized.driveReaction,
            possibleCauses = synthesized.possibleCauses,
            repairSteps = synthesized.repairSteps,
            requiresHighVoltageCaution = requiresHv,
            relatedCodes = related
        )
    }

    private fun classifyCategory(code: Int, titlePl: String, titleEn: String?): ErrorCategory {
        val combined = "$titlePl ${titleEn.orEmpty()}".lowercase()
        return when {
            code in 7200..7229 -> ErrorCategory.POWER_DC_BUS
            code in 9000..9399 || code in 41000..41099 || code == 36 -> ErrorCategory.THERMAL_OVERLOAD
            code in 7000..7099 || code in 39000..39099 || code in 31220..31224 -> ErrorCategory.ENCODER_FEEDBACK
            code in 35000..35299 -> ErrorCategory.SAFETY_SAFEMC
            code in 6015..6016 || code in 1004..1024 && (combined.contains("sieci") || combined.contains("network") || combined.contains("can") || combined.contains("powerlink") || combined.contains("cyklicz")) ->
                ErrorCategory.NETWORK_COMM
            code in 6000..6099 || code in 38000..38099 || code in 39300..39399 -> {
                when {
                    combined.contains("sync") || combined.contains("can ") || combined.contains("network") -> ErrorCategory.NETWORK_COMM
                    else -> ErrorCategory.POWER_STAGE_BRAKE
                }
            }
            code in 5100..5399 || code in 37100..37199 -> ErrorCategory.CAM_SYNC
            code in 4000..5099 || code in 64500..64599 -> ErrorCategory.MOTION_HOMING
            code in 29200..29599 || code in 31200..32799 || code in 64000..64099 -> {
                when {
                    combined.contains("can") || combined.contains("powerlink") || combined.contains("sieć") ||
                        combined.contains("sieci") || combined.contains("network") || combined.contains("broadcast") ||
                        combined.contains("cob") || combined.contains("acyc") || combined.contains("node") ||
                        combined.contains("węz") || combined.contains("telegram") -> ErrorCategory.NETWORK_COMM
                    else -> ErrorCategory.PLCOPEN_NC
                }
            }
            else -> ErrorCategory.PARAMETERS_MEMORY
        }
    }

    private fun classifySeverity(
        code: Int,
        titlePl: String,
        titleEn: String?,
        category: ErrorCategory
    ): ErrorSeverity {
        val text = "$titlePl ${titleEn.orEmpty()}".lowercase()
        if (code == 36 || code in 64500..64599 || code == 40000) return ErrorSeverity.INFO
        if (code in 36000..36099 || code in 37100..37199 || code in 41000..41099 || code in 64000..64099) {
            return ErrorSeverity.WARNING
        }
        if (text.contains("warning") || text.contains("ostrzeg") || text.contains("limiter active") ||
            text.contains("aktywny ogranicznik") || text.contains("delay") || text.contains("opóźnienie")
        ) {
            return ErrorSeverity.WARNING
        }
        if (code in 33002..33009 || code in 35000..35255 ||
            text.contains("overcurrent") || text.contains("nadprąd") || text.contains("przetęż") ||
            text.contains("overvoltage") || text.contains("przepięc") ||
            text.contains("ground fault") || text.contains("doziemn") ||
            text.contains("overtemperature") || text.contains("przegrzani") ||
            text.contains("watchdog") || text.contains("power supply fail") || text.contains("awaria zasilania") ||
            text.contains("fail safe") || text.contains("fatal") || text.contains("zwarcie") ||
            text.contains("short circuit") || text.contains("movement stopped") || text.contains("ruch zatrzymany") ||
            category == ErrorCategory.POWER_DC_BUS
        ) {
            return ErrorSeverity.CRITICAL
        }
        return ErrorSeverity.ERROR
    }

    private fun requiresHighVoltageWarning(
        code: Int,
        titlePl: String,
        titleEn: String?,
        category: ErrorCategory
    ): Boolean {
        if (category == ErrorCategory.POWER_DC_BUS) return true
        val text = "$titlePl ${titleEn.orEmpty()}".lowercase()
        return code in listOf(6018, 6019, 6026, 6028, 6045, 6049, 6052, 6053, 6054, 9300, 38008) ||
            text.contains("x5") || text.contains("szyna dc") || text.contains("dc bus") ||
            text.contains("bleeder") || text.contains("stopień mocy") || text.contains("power stage") ||
            text.contains("power mains") || text.contains("fazy")
    }

    private fun explainInfoDetail(infoDetail: String?): String? {
        if (infoDetail.isNullOrBlank()) return null
        val lower = infoDetail.lowercase()
        return when {
            lower.contains("parid") -> "Sterownik zwraca numer parametru (ParID), którego dotyczy błąd — ułatwia to jego natychmiastowe odnalezienie w tabeli parametrów ACOPOS."
            lower.contains("slot") -> "Wartość oznacza numer gniazda (Slot 1–4) karty wtykowej (Plug-in) w serwonapędzie ACOPOS, w której wykryto problem."
            lower.contains("plcopen_fb") -> "Identyfikator bloku funkcyjnego PLCopen MC, z którego zostało wywołane błędne polecenie."
            lower.contains("temperature") -> "Dodatkowa informacja zawiera zmierzoną temperaturę lub przekroczony próg temperaturowy [°C]."
            lower.contains("load limit") || lower.contains("current load") -> "Dodatkowa informacja podaje aktualny poziom obciążenia lub przekroczony limit prądu/mocy."
            lower.contains("phase") -> "Wartość wskazuje konkretny numer fazy silnika (U/V/W) na złączu X5."
            lower.contains("timeout") -> "Podany jest dokładny czas przekroczenia limitu oczekiwania (Timeout)."
            lower.contains("can-id") || lower.contains("can_") -> "Zwracany jest identyfikator ramki CAN (COB-ID) oraz kod statusu funkcji sterownika CAN."
            lower.contains("station") || lower.contains("node") -> "Wskazuje numer stacji / węzła sieciowego ACOPOS, którego dotyczy usterka."
            lower.contains("voltage") -> "Zawiera zmierzone napięcie szyny DC lub wymagany próg napięcia [V]."
            lower.contains("lag error") || lower.contains("difference") -> "Wskazuje dokładną wartość uchybu pozycji w chwili wyzwolenia alarmu."
            lower.contains("speed") || lower.contains("acceleration") -> "Podaje zmierzoną wartość lub przekroczony limit prędkości / przyspieszenia."
            lower.contains("epc") || lower.contains("addr") -> "Adres rejestru procesora (EPC/ADDR) w chwili wystąpienia wyjątku sprzętowego — przydatny dla serwisu B&R."
            else -> "Dodatkowa wartość diagnostyczna zwracana przez serwonapęd ACOPOS wraz z numerem błędu (widoczna w Loggerze / Automation Studio)."
        }
    }

    private data class SynthesizedDiagnostic(
        val problemDescription: String,
        val driveReaction: String,
        val possibleCauses: List<String>,
        val repairSteps: List<String>
    )

    private fun synthesizeDetails(
        raw: RawAcoposEntry,
        category: ErrorCategory,
        severity: ErrorSeverity
    ): SynthesizedDiagnostic {
        val text = "${raw.titlePl} ${raw.titleEn.orEmpty()}".lowercase()

        return when {
            // 1. Limiters 64500..64506
            raw.code in 64500..64506 -> SynthesizedDiagnostic(
                problemDescription = "Informacja diagnostyczna: ${raw.titlePl}. Napęd automatycznie ogranicza zadaną wielkość (prędkość, moment obrotowy lub moc zwrotu energii), aby nie przekroczyć zaprogramowanych limitów bezpieczeństwa i możliwości sprzętu.",
                driveReaction = "Oś kontynuuje pracę z ograniczoną dynamiką (bez zatrzymania awaryjnego). Przy długotrwałym ograniczeniu może jednak pojawić się błąd uchybu nadążnego (4007).",
                possibleCauses = listOf(
                    "Zbyt agresywnie zadany profil prędkości, przyspieszenia lub hamowania.",
                    "Zwiększone opory mechaniczne lub większa masa robocza osi.",
                    "Zbyt nisko ustawione limity momentu/prędkości w konfiguracji osi."
                ),
                repairSteps = listOf(
                    "Sprawdź, czy komunikat pojawia się sporadycznie (normalna praca) czy ciągle w trakcie cyklu.",
                    "Zweryfikuj mechanikę osi pod kątem nadmiernych oporów ruchu i stanu smarowania.",
                    "Porównaj zadane przyspieszenia/opóźnienia z parametrami limitów osi (AXLIM).",
                    "Dla kodu 64506 (odzysk energii) sprawdź stan rezystora hamowania (Bleeder) i wydłuż czas hamowania."
                )
            )

            // 2. Thermal warnings 41001..41089 and limiter active 9001/9011/9031...
            raw.code in 41000..41099 || (raw.code in 9000..9090 && (text.contains("ogranicznik") || text.contains("limiter"))) -> SynthesizedDiagnostic(
                problemDescription = "Ostrzeżenie termiczne lub przeciążeniowe: ${raw.titlePl}. Układ osiągnął wysoki próg temperatury lub obciążenia i wymaga kontroli chłodzenia zanim dojdzie do awaryjnego zatrzymania maszyny.",
                driveReaction = "Aktywacja ostrzeżenia lub automatyczne ograniczenie prądu/mocy przez serwonapęd w celu niedopuszczenia do przegrzania.",
                possibleCauses = listOf(
                    "Pogorszone chłodzenie szafy sterowniczej (zabrudzone filtry, słaby nawiew) lub silnika.",
                    "Zbyt intensywny cykl pracy maszyny bez odpowiedniej rezerwy termicznej.",
                    "Narastające opory mechaniczne na osi (zużycie łożysk, brak smarowania)."
                ),
                repairSteps = listOf(
                    "Sprawdź w diagnostyce aktualną temperaturę i procentowe obciążenie termiczne osi.",
                    "Skontroluj drożność filtrów szafy sterowniczej, działanie wentylatorów ACOPOS oraz temperaturę otoczenia.",
                    "Sprawdź temperaturę korpusu silnika / rezystora hamowania oraz stan mechaniki osi.",
                    "Jeśli ostrzeżenie powtarza się cyklicznie, złagodź dynamikę ruchu (przyspieszenia/hamowania) przed wystąpieniem błędu krytycznego."
                )
            )

            // 3. Temperature sensors & thermal faults 9000..9090
            category == ErrorCategory.THERMAL_OVERLOAD -> SynthesizedDiagnostic(
                problemDescription = "Alarm układu termicznego lub prądowego: ${raw.titlePl}. Serwonapęd wykrył przekroczenie dopuszczalnej temperatury, przeciążenie modelu cieplnego lub usterkę samego czujnika temperatury.",
                driveReaction = "Zatrzymanie ruchu osi i odłączenie stopnia mocy w celu ochrony elementów półprzewodnikowych oraz uzwojeń silnika.",
                possibleCauses = listOf(
                    "Przegrzanie stopnia mocy, radiatora, silnika lub rezystora hamowania na skutek przeciążenia albo niesprawnego chłodzenia.",
                    "Uszkodzenie, odłączenie lub zwarcie w przewodzie czujnika temperatury (w przypadku błędów czujnika).",
                    "Zatarcie mechaniczne osi zwiększające długotrwale pobór prądu."
                ),
                repairSteps = listOf(
                    "Odczekaj na schłodzenie podzespołów i sprawdź w diagnostyce rzeczywisty odczyt temperatury.",
                    "Skontroluj wentylatory ACOPOS, czystość radiatorów oraz sprawność klimatyzatora/filtrów szafy sterowniczej.",
                    "Jeśli błąd dotyczy przerwy lub zwarcia czujnika, zmierz rezystancję czujnika i ciągłość jego przewodu.",
                    "Sprawdź mechanikę osi pod kątem oporów ruchu oraz zweryfikuj poprawność parametrów znamionowych silnika i rezystora.",
                    "Po schłodzeniu poniżej progu histerezy skasuj błąd i obserwuj przyrost temperatury podczas pracy."
                )
            )

            // 4. DC Bus & Mains Power 7200..7227
            category == ErrorCategory.POWER_DC_BUS -> SynthesizedDiagnostic(
                problemDescription = "Usterka obwodu zasilania mocy lub szyny pośredniczącej DC: ${raw.titlePl}. Napięcie lub prąd w obwodzie zasilania napędu wyszły poza dopuszczalne granice bezpiecznej pracy.",
                driveReaction = "Natychmiastowe odłączenie stopnia mocy i blokada ponownego załączenia do czasu ustabilizowania zasilania.",
                possibleCauses = listOf(
                    "Niestabilne napięcie sieci zasilającej 3×400 V AC, zanik fazy lub zbyt częste wyłączanie/załączanie stycznika głównego.",
                    "Uszkodzenie lub przeciążenie rezystora ładowania wstępnego albo rezystora hamowania (Bleeder).",
                    "Poluzowane połączenia na szynie DC, złączu płyty bazowej (Backplane) lub zasilaniu sieciowym.",
                    "Zwarcie doziemne w torze mocy."
                ),
                repairSteps = listOf(
                    "Zmierz napięcia zasilania sieciowego L1-L2-L3 na wejściu zasilacza/napędu i zweryfikuj stabilność pod obciążeniem.",
                    "Sprawdź stan bezpieczników mocy, stycznika głównego oraz dokręcenie zacisków zasilania i szyny DC (po odłączeniu napięcia i rozładowaniu kondensatorów!).",
                    "W przypadku błędów rezystora ładowania (np. 7214) ogranicz częstotliwość załączania stycznika głównego i daj rezystorowi ostygnąć.",
                    "Sprawdź poprawność podłączenia modułu do płyty bazowej (Backplane) i obecność stabilnego 24 V DC.",
                    "Skontroluj izolację przewodów silnikowych i rezystora hamowania względem PE."
                )
            )

            // 5. Encoder & Feedback (7012..7061, 39001..39045, 31220..31224)
            category == ErrorCategory.ENCODER_FEEDBACK -> SynthesizedDiagnostic(
                problemDescription = "Błąd układu pomiarowego pozycji (enkoder / resolwer / karta interfejsu): ${raw.titlePl}. Serwonapęd utracił wiarygodny odczyt położenia lub komunikację z enkoderem.",
                driveReaction = if (severity == ErrorSeverity.WARNING) {
                    "Zgłoszenie ostrzeżenia diagnostycznego enkodera; wymagana kontrola serwisowa zanim nastąpi utrata pozycji."
                } else {
                    "Natychmiastowe zatrzymanie osi, wyłączenie regulatora i unieważnienie stanu bazowania osi."
                },
                possibleCauses = listOf(
                    "Uszkodzony, przetarty lub naderwany kabel enkodera (często w ruchomym prowadniku kablowym).",
                    "Słaby styk, wygięty pin lub brak ciągłości ekranowania 360° w złączach enkodera (zakłócenia EMC).",
                    "Rozładowana bateria podtrzymująca (dla enkoderów absolutnych) lub uszkodzenie samego enkodera.",
                    "Nieprawidłowo skonfigurowany typ enkodera lub uszkodzona karta wtykowa w slocie ACOPOS."
                ),
                repairSteps = listOf(
                    "Odczytaj z komunikatu numer gniazda (Info: Slot), aby zlokalizować właściwą kartę interfejsu w serwonapędzie ACOPOS.",
                    "Rozepnij i dokładnie obejrzyj złącza enkodera przy silniku i na karcie ACOPOS (sprawdź piny, ślady oleju/wilgoci oraz zacisk ekranu).",
                    "Sprawdź ciągłość żył przewodu enkodera oraz napięcie zasilania enkodera (5 V / 7–12 V zależnie od typu).",
                    "Dla enkoderów EnDat z komunikatem baterii/oświetlenia/temperatury sprawdź baterię buforową i temperaturę silnika.",
                    "Wykonaj restart zasilania (Power OFF/ON); jeśli błąd wraca, podmień przewód enkodera, a następnie kartę wtykową lub enkoder."
                )
            )

            // 6. SafeMC Safety module 35000..35255
            category == ErrorCategory.SAFETY_SAFEMC -> SynthesizedDiagnostic(
                problemDescription = "Alarm modułu bezpieczeństwa SafeMC (Safe Motion Control): ${raw.titlePl}. Układ bezpieczeństwa wykrył naruszenie funkcji bezpiecznego ruchu (np. SLS, SOS, SS2, SDI, SMP/SLP), niezgodność enkodera bezpiecznego lub błąd parametryzacji bezpieczeństwa.",
                driveReaction = "Natychmiastowe przejście napędu w stan bezpieczny (STO / Fail Safe) i zablokowanie ruchu do czasu potwierdzenia bezpieczeństwa.",
                possibleCauses = listOf(
                    "Przekroczenie bezpiecznego limitu prędkości (SLS/SMS), pozycji (SLP/SMP), kierunku (SDI) lub rampy hamowania podczas aktywnej funkcji bezpieczeństwa.",
                    "Wymiana silnika/enkodera bez ponownego zatwierdzenia danych konfiguracyjnych enkodera w SafeDESIGNER / SafePLC.",
                    "Niespójne parametry bezpieczeństwa lub przekroczenie limitu czasu bezpiecznego bazowania (Safe Homing).",
                    "Wewnętrzny błąd diagnostyczny modułu SafeMC lub zakłócenia komunikacji bezpiecznej z enkoderem EnDat."
                ),
                repairSteps = listOf(
                    "Zweryfikuj, która funkcja bezpieczeństwa była aktywna w momencie błędu (np. tryb nastawczy z otwartymi drzwiami — SLS, zatrzymanie SOS/SS2).",
                    "Jeśli przekroczono bezpieczny limit prędkości/pozycji/rampy, sprawdź nastawy programu ruchu PLC względem progów SafeMC (ruch standardowy musi mieć zapas względem limitów SafeMC).",
                    "Jeśli komunikat dotyczy niezgodności enkodera (35030, 35031, 35234–35237, 35245) — po wymianie silnika/enkodera potwierdź nowy numer seryjny/konfigurację przez procedurę serwisową SafePLC.",
                    "Sprawdź połączenie enkodera bezpiecznego i wykonaj wymagany test bezpiecznego wyjścia / bazowania (Safe Homing).",
                    "W przypadku błędów wewnętrznych SMC Fail Safe (35000–35023) wykonaj pełny restart zasilania 24 V; przy powtarzaniu się alarmu wymień kartę/moduł SafeMC."
                )
            )

            // 7. Holding brake & Power stage & Digital IO (6000..6067, 38000..38101, 39301..39307)
            category == ErrorCategory.POWER_STAGE_BRAKE -> SynthesizedDiagnostic(
                problemDescription = "Alarm stopnia mocy, hamulca, fazowania lub obwodów wejść/wyjść serwonapędu: ${raw.titlePl}.",
                driveReaction = "Zatrzymanie pracy osi i wyłączenie stopnia mocy do czasu usunięcia usterki i skasowania błędu.",
                possibleCauses = listOf(
                    "Usterka okablowania lub cewki hamulca trzymającego, albo niewłaściwa konfiguracja hamulca w parametrach silnika.",
                    "Zanik lub spadek napięcia 24 V DC na wejściach Enable1/Enable2 lub na karcie Digital IO.",
                    "Nieudane fazowanie silnika (zbyt wysoka prędkość, zablokowany wirnik lub błędny tryb PHASING_MODE).",
                    "Przekroczenie limitu prędkości, momentu obrotowego lub usterka sprzętowa stopnia mocy / FPGA."
                ),
                repairSteps = listOf(
                    "Sprawdź napięcie zasilania 24 V DC na zaciskach napędu, wejściach Enable oraz karcie IO.",
                    "Dla alarmów hamulca sprawdź ciągłość żył hamulca na złączu X5, rezystancję cewki oraz brak napięcia obcego na wyjściu.",
                    "Dla alarmów fazowania (Phasing 6043–6051) upewnij się, że wirnik może się swobodnie obracać, hamulec jest zwolniony, a oś nie porusza się przed startem.",
                    "Dla alarmów Digital IO (39301–39307) sprawdź obciążenia podłączone do wskazanych kanałów wyjściowych pod kątem zwarcia lub przegrzania.",
                    "Zweryfikuj poprawność danych silnika i wersji oprogramowania układowego ACOPOS."
                )
            )

            // 8. Motion, Limits, Homing (4005..5039)
            category == ErrorCategory.MOTION_HOMING -> SynthesizedDiagnostic(
                problemDescription = "Błąd sterowania ruchem, limitów programowych/sprzętowych lub procedury bazowania: ${raw.titlePl}.",
                driveReaction = "Przerwanie bieżącej komendy ruchu / bazowania lub blokada uruchomienia kolejnego przejazdu.",
                possibleCauses = listOf(
                    "Zadana pozycja docelowa lub droga hamowania wykracza poza limity programowe SW albo napotyka krańcówkę sprzętową.",
                    "Próba uruchomienia polecenia ruchu lub bazowania w niewłaściwym stanie osi (np. regulator wyłączony, aktywny inny ruch lub rampa zatrzymania).",
                    "Nieprawidłowe parametry bazowania, brak sygnału czujnika referencyjnego albo zamienione kierunki krańcówek.",
                    "Przekroczenie dopuszczalnej prędkości lub przyspieszenia osi (np. po zmianie Override)."
                ),
                repairSteps = listOf(
                    "Sprawdź aktualny stan osi (czy regulator jest załączony, czy nie trwa inny ruch ani procedura zatrzymania).",
                    "Zweryfikuj zadaną pozycję docelową oraz ustawienia limitów programowych SW (dodatniego i ujemnego).",
                    "Dla błędów bazowania (5015–5039) sprawdź działanie czujnika referencyjnego (RefSwitch), krańcówek oraz poprawność wybranego trybu bazowania i typu enkodera.",
                    "Jeśli przekroczono limit prędkości/przyspieszenia (5026, 5027, 5036), sprawdź wartości zadane w programie PLC oraz współczynnik Override.",
                    "Skasuj błąd, wykonaj ręczne wycofanie ze strefy granicznej (jeśli dotyczy) i powtórz operację."
                )
            )

            // 9. Cam profiles & Synchronization (5101..5329, 37101..37113)
            category == ErrorCategory.CAM_SYNC -> SynthesizedDiagnostic(
                problemDescription = "Błąd sprzężenia krzywkowego (Cam Profile / Cam Automat) lub synchronizacji Master–Slave: ${raw.titlePl}.",
                driveReaction = "Przerwanie lub zablokowanie sprzężenia osi Slave z osią Master (albo ograniczenie kompensacji przy kodach 371xx).",
                possibleCauses = listOf(
                    "Nieprawidłowa definicja punktów lub wielomianów profilu krzywki albo brak tabeli krzywki pod wskazanym indeksem.",
                    "Próba modyfikacji parametrów lub pobrania nowej krzywki podczas gdy automat krzywkowy jest aktywny.",
                    "Zbyt częste lub brakujące impulsy wyzwalacza (Trigger FIFO przepełnione/puste, sygnał poza oknem).",
                    "Utrata pozycji cyklicznej z osi Master lub błąd enkodera osi sprzężonej."
                ),
                repairSteps = listOf(
                    "Sprawdź, czy tabela profilu krzywki jest wgrana do sterownika i posiada poprawny indeks oraz monotoniczne punkty Master.",
                    "Przed zapisem parametrów sprzężenia lub pobieraniem nowej krzywki dezaktywuj automat krzywkowy.",
                    "Dla błędów wyzwalacza (Trigger / FIFO) skontroluj czujnik wyzwalający (np. fotokomórkę), ustawione okno czasowe/pozycyjne oraz eliminację drgań styków.",
                    "Sprawdź poprawność pracy i komunikacji osi Master (brak błędów enkodera i brak zaników danych cyklicznych)."
                )
            )

            // 10. Network communication (CAN / POWERLINK / Cyclic / Coupling)
            category == ErrorCategory.NETWORK_COMM -> SynthesizedDiagnostic(
                problemDescription = "Błąd komunikacji sieciowej lub synchronizacji magistrali (POWERLINK / CAN): ${raw.titlePl}.",
                driveReaction = "Utrata wymiany danych z serwonapędem, przerwanie komunikacji cyklicznej lub blokada inicjalizacji sieci NC.",
                possibleCauses = listOf(
                    "Uszkodzony lub odpięty kabel sieciowy POWERLINK / CAN, uszkodzony hub/switch albo brak terminacji.",
                    "Zdublowany lub nieprawidłowy numer węzła (Node ID) w module konfiguracyjnym acp10cfg / na przełącznikach obrotowych ACOPOS.",
                    "Niezgodność czasów cykli między interfejsem sieciowym, klasą zadań NC a konfiguracją sterownika PLC.",
                    "Zbyt duże obciążenie procesora sterownika lub zakłócenia EMC na magistrali."
                ),
                repairSteps = listOf(
                    "Sprawdź fizyczne połączenie sieciowe (diody LINK/ACT, wtyczki RJ45 / CAN) oraz zasilanie wszystkich modułów ACOPOS w łańcuchu.",
                    "Zweryfikuj ustawienie przełączników adresu węzła (Node Number) na serwonapędzie względem konfiguracji w Automation Studio (acp10cfg).",
                    "Sprawdź w Loggerze PLC dodatkowe wpisy sieciowe towarzyszące temu kodowi (zwłaszcza dla serii 32200–32288).",
                    "Zweryfikuj zgodność czasów cykli sieci POWERLINK i klasy zadań menedżera NC.",
                    "Wykonaj restart zasilania sterownika PLC oraz napędów i sprawdź, czy wszystkie węzły przechodzą w stan operacyjny."
                )
            )

            // 11. PLCopen / NC Manager / Tuning / Trace / SDM (29200..32767, 64002..64012)
            category == ErrorCategory.PLCOPEN_NC -> SynthesizedDiagnostic(
                problemDescription = "Błąd warstwy sterowania ruchem PLCopen / Menedżera NC: ${raw.titlePl}.",
                driveReaction = "Odrzucenie wywołania bloku funkcyjnego lub przerwanie operacji inicjalizacji/strojenia/śledzenia.",
                possibleCauses = listOf(
                    "Wywołanie bloku funkcyjnego PLCopen (MC_*) w niewłaściwym stanie osi lub z nieprawidłowymi parametrami wejściowymi.",
                    "Brakujący, uszkodzony lub niezgodny wersją moduł konfiguracyjny/systemowy NC (acp10cfg, acp10sys, acp10bsl, INIT).",
                    "Błąd wtórny wywołany alarmem sprzętowym w samym napędzie ACOPOS (szczególnie 29209 / 29226).",
                    "Konflikt operacji (np. aktywne nagrywanie Trace lub aktywna inna komenda podczas startu strojenia/pobierania)."
                ),
                repairSteps = listOf(
                    "Sprawdź w Loggerze sterownika PLC (System Diagnostics Manager / Automation Studio) dokładny opis zdarzenia i wcześniejsze alarmy napędu.",
                    "Jeśli błąd pochodzi z bloku PLCopen (292xx), odczytaj właściwy kod błędu napędu funkcją MC_ReadAxisError i zweryfikuj stan maszyny stanów osi.",
                    "Sprawdź wersje oprogramowania układowego (acp10sys / BsLoader) oraz kompletność modułów konfiguracyjnych na karcie pamięci PLC.",
                    "Dla kodów 32399 wykonaj ręczny restart zasilania (POWER OFF / ON) serwonapędu ACOPOS po aktualizacji firmware.",
                    "Dla błędów strojenia (32300–32321) upewnij się, że brak aktywnych błędów osi, regulator jest wyłączony, a parametry startowe mieszczą się w limitach."
                )
            )

            // 12. Parameters, EEPROM, Exceptions, System (1..1027, 2001..2006, 7100, 7300..8020, 10000..10500, 33000..33009, 65535)
            else -> {
                val isHardwareException = raw.code in 33002..33009
                val isEeprom = raw.code in 8001..8020
                val isTrace = raw.code in 2001..2006
                when {
                    isHardwareException -> SynthesizedDiagnostic(
                        problemDescription = "Krytyczny wyjątek procesora lub układu FPGA serwonapędu ACOPOS: ${raw.titlePl}.",
                        driveReaction = "Natychmiastowe zatrzymanie systemu operacyjnego napędu i odłączenie stopnia mocy.",
                        possibleCauses = listOf(
                            "Poważny błąd oprogramowania układowego (firmware) lub uszkodzenie pamięci RAM/Flash napędu.",
                            "Silne zakłócenia elektromagnetyczne / przepięcia w obwodzie zasilania elektroniki 24 V.",
                            "Uszkodzenie sprzętowe płyty sterującej serwonapędu ACOPOS."
                        ),
                        repairSteps = listOf(
                            "Zapisz dokładny numer błędu oraz wartość parametru Info (EPC/ADDR) z Loggera na potrzeby zgłoszenia serwisowego.",
                            "Wykonaj pełny restart zasilania (POWER OFF / ON) serwonapędu i sprawdź, czy błąd powraca.",
                            "Zweryfikuj jakość uziemienia PE oraz stabilność napięcia zasilania 24 V DC.",
                            "W razie potrzeby zaktualizuj oprogramowanie układowe (firmware) ACOPOS; jeśli wyjątek powtarza się — wymień moduł ACOPOS."
                        )
                    )
                    isEeprom -> SynthesizedDiagnostic(
                        problemDescription = "Błąd odczytu lub nieprawidłowe dane w pamięci nieulotnej EEPROM/EPROM napędu lub karty wtykowej: ${raw.titlePl}.",
                        driveReaction = "Zablokowanie inicjalizacji karty lub napędu.",
                        possibleCauses = listOf(
                            "Uszkodzone lub niezainicjalizowane dane kalibracyjne w pamięci EEPROM karty rozszerzeń lub napędu.",
                            "Niewłaściwe osadzenie karty wtykowej w gnieździe ACOPOS.",
                            "Uszkodzenie sprzętowe pamięci nieulotnej."
                        ),
                        repairSteps = listOf(
                            "Wyłącz zasilanie napędu, wyjmij i ponownie solidnie zamontuj karty rozszerzeń (Plug-in) w gniazdach ACOPOS.",
                            "Włącz zasilanie i sprawdź, czy błąd ustąpił.",
                            "Zanotuj identyfikator parametru z pola Info(EEPROM Parameter ID) i porównaj konfigurację sprzętową.",
                            "Jeśli alarm powtarza się niezmiennie — karta rozszerzeń lub moduł ACOPOS wymaga wymiany serwisowej."
                        )
                    )
                    isTrace -> SynthesizedDiagnostic(
                        problemDescription = "Konflikt operacji rejestratora przebiegów (Trace): ${raw.titlePl}.",
                        driveReaction = "Odrzucenie bieżącego polecenia Trace; normalna praca osi nie jest przerywana.",
                        possibleCauses = listOf(
                            "Próba uruchomienia, skonfigurowania lub pobrania danych Trace, gdy rejestracja jest już w toku."
                        ),
                        repairSteps = listOf(
                            "Zatrzymaj bieżące nagrywanie Trace lub poczekaj na jego zakończenie.",
                            "Po przejściu rejestratora w stan gotowości ponów odczyt danych lub konfigurację parametrów Trace."
                        )
                    )
                    else -> SynthesizedDiagnostic(
                        problemDescription = "Błąd obsługi parametru, bloku danych lub stanu systemowego sterownika ACOPOS: ${raw.titlePl}.",
                        driveReaction = "Odrzucenie błędnego polecenia zapisu/odczytu lub wstrzymanie inicjalizacji do czasu korekty parametru.",
                        possibleCauses = listOf(
                            "Wartość parametru wykracza poza zakres min/max lub dotyczy nieprawidłowego identyfikatora ParID.",
                            "Próba zapisu parametru w niedozwolonym stanie (np. aktywny ruch, włączony regulator lub zajęty interfejs poleceń).",
                            "Niezgodność wersji oprogramowania układowego (OS/FPROM) z konfiguracją projektu lub uszkodzona suma kontrolna bloku danych."
                        ),
                        repairSteps = listOf(
                            "Sprawdź numer parametru (Info: ParID) oraz jego dozwolone granice min/max w dokumentacji ACOPOS.",
                            "Upewnij się, że oś jest zatrzymana i regulator wyłączony przed zapisem parametrów konfiguracyjnych.",
                            "W przypadku błędów systemu operacyjnego / pobierania (25, 27, 64–66, 72) zweryfikuj zgodność wersji firmware ACOPOS z projektem Automation Studio.",
                            "Jeśli interfejs poleceń jest zajęty (69, 1014), poczekaj na zakończenie poprzedniej operacji serwisowej i ponów żądanie."
                        )
                    )
                }
            }
        }
    }

    private fun computeRelatedCodes(code: Int, allCodeSet: Set<Int>): List<Int> {
        val candidates = LinkedHashSet<Int>()
        // Immediate neighbors within +/- 4
        for (delta in listOf(-2, -1, 1, 2, 3, -3, 10, -10)) {
            val neighbor = code + delta
            if (neighbor in allCodeSet && neighbor != code) {
                candidates.add(neighbor)
            }
            if (candidates.size >= 4) break
        }
        // Pair thermal warnings 41xxx <-> thermal faults 9xxx
        if (code in 41001..41089) {
            val faultPair = 9000 + (code - 41000)
            if (faultPair in allCodeSet) candidates.add(faultPair)
            if ((faultPair - 1) in allCodeSet) candidates.add(faultPair - 1)
        } else if (code in 9000..9089) {
            val warnPair = 41000 + (code - 9000)
            if (warnPair in allCodeSet) candidates.add(warnPair)
            if ((warnPair + 1) in allCodeSet) candidates.add(warnPair + 1)
        }
        return candidates.take(5)
    }
}
