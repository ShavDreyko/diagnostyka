package com.example.data

import com.example.model.ErrorCategory
import com.example.model.ErrorSeverity

data class CuratedDiagnostic(
    val category: ErrorCategory,
    val severity: ErrorSeverity,
    val problemDescription: String,
    val driveReaction: String,
    val possibleCauses: List<String>,
    val repairSteps: List<String>,
    val requiresHighVoltageCaution: Boolean = false,
    val relatedCodes: List<Int> = emptyList()
)

object AcoposDiagnosticCurated {

    val commonFaultCodes: List<Int> = listOf(
        4007, 6019, 6021, 6028, 7032, 7200, 7215, 7222, 9000, 9050, 4008, 6015
    )

    val entries: Map<Int, CuratedDiagnostic> = mapOf(
        1004 to CuratedDiagnostic(
            category = ErrorCategory.NETWORK_COMM,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Przekroczono dopuszczalny czas oczekiwania na sygnał życia (Life Sign / Heartbeat) z nadrzędnego sterownika PLC w sieci komunikacyjnej.",
            driveReaction = "Natychmiastowe przerwanie ruchu według rampy awaryjnej i przejście napędu w stan błędu.",
            possibleCauses = listOf(
                "Przerwany, luźny lub uszkodzony przewód sieciowy CAN / POWERLINK.",
                "Przeciążenie procesora sterownika PLC lub zawieszenie zadania komunikacyjnego.",
                "Silne zakłócenia elektromagnetyczne (EMC) w szafie sterowniczej lub brak terminacji magistrali."
            ),
            repairSteps = listOf(
                "Sprawdź stan diod LED komunikacji (RUN/ERR/LINK) na module ACOPOS oraz karcie sieciowej PLC.",
                "Skontroluj wtyczki i ciągłość przewodu POWERLINK / CAN oraz poprawność ekranowania.",
                "Zweryfikuj w logach Automation Studio, czy sterownik PLC nie zresetował się lub nie przekroczył czasu cyklu.",
                "Sprawdź poprawność rezystora terminującego na końcu magistrali CAN (jeśli dotyczy).",
                "Po usunięciu przyczyny skasuj błąd i zweryfikuj stabilność komunikacji cyklicznej."
            ),
            relatedCodes = listOf(1012, 1022, 6015, 6016, 32060)
        ),
        1011 to CuratedDiagnostic(
            category = ErrorCategory.MOTION_HOMING,
            severity = ErrorSeverity.WARNING,
            problemDescription = "Aktywowane zostało sprzętowe wejście szybkiego zatrzymania (Quickstop) na serwonapędzie ACOPOS.",
            driveReaction = "Zatrzymanie osi z rampą awaryjnego hamowania Quickstop (AXLIM_DEC_STOP).",
            possibleCauses = listOf(
                "Wciśnięty przycisk zatrzymania awaryjnego E-STOP lub otwarta osłona bezpieczeństwa maszyny.",
                "Zanik sygnału 24V DC na wejściu cyfrowym skonfigurowanym jako Quickstop.",
                "Błędna polaryzacja wejścia lub uszkodzony styk przekaźnika bezpieczeństwa."
            ),
            repairSteps = listOf(
                "Sprawdź stan obwodu bezpieczeństwa maszyny (grzybki E-STOP, kurtyny świetlne, drzwi serwisowe).",
                "Zmierz napięcie 24V DC na zacisku wejścia cyfrowego przypisanego do funkcji Quickstop.",
                "Skontroluj styki przekaźników bezpieczeństwa oraz ciągłość przewodu sterowniczego.",
                "Po przywróceniu sygnału 24V zresetuj układ bezpieczeństwa i skasuj alarm w sterowniku."
            ),
            relatedCodes = listOf(6021, 6023, 6058, 6059)
        ),
        4005 to CuratedDiagnostic(
            category = ErrorCategory.MOTION_HOMING,
            severity = ErrorSeverity.ERROR,
            problemDescription = "Podjęto próbę załączenia regulatora (Controller ON), podczas gdy serwonapęd znajduje się w stanie niekasowanego błędu.",
            driveReaction = "Odrzucenie polecenia załączenia stopnia mocy — oś pozostaje wyłączona.",
            possibleCauses = listOf(
                "Występuje inny wcześniejszy błąd pierwotny w kolejce błędów napędu.",
                "Nie wykonano procedury kasowania alarmu (Acknowledge / Reset) przed załączeniem napędu."
            ),
            repairSteps = listOf(
                "Odczytaj pełną historię błędów (Logger / kolejkę alarmów) — błąd 4005 jest błędem wtórnym.",
                "Zidentyfikuj i usuń pierwszy chronologicznie błąd pierwotny (np. zasilanie, enkoder, Enable).",
                "Wykonaj kasowanie błędów osi (Error Acknowledge).",
                "Dopiero gdy status błędu wynosi 0, załącz ponownie regulator napędu."
            ),
            relatedCodes = listOf(4010, 4012, 6021, 29209)
        ),
        4007 to CuratedDiagnostic(
            category = ErrorCategory.MOTION_HOMING,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Przekroczony dopuszczalny uchyb nadążny pozycji (Lag Error). Rzeczywista pozycja silnika odbiega od pozycji zadanej bardziej niż pozwala parametr AXLIM_DS_STOP.",
            driveReaction = "Awaryjne zatrzymanie osi i wyłączenie stopnia mocy po osiągnięciu limitu uchybu.",
            possibleCauses = listOf(
                "Kolizja mechaniczna, zakleszczenie osi, uszkodzone łożysko lub zatarcie prowadnicy liniowej.",
                "Niezwalniający się hamulec trzymający silnika lub uszkodzone sprzęgło mechaniczne.",
                "Zbyt mały moment obrotowy (ograniczenie prądu, przeciążenie) albo zbyt agresywne przyspieszenie.",
                "Błąd okablowania faz silnika U/V/W, luźny enkoder lub rozstrojony regulator pozycji/prędkości."
            ),
            repairSteps = listOf(
                "Natychmiast sprawdź mechanikę osi: czy nie doszło do kolizji, zakleszczenia lub nadmiernych oporów ruchu.",
                "Zweryfikuj działanie hamulca trzymającego silnika (napięcie 24V DC na cewce hamulca podczas ruchu).",
                "Sprawdź sprzęgło między silnikiem a śrubą/przekładnią pod kątem poślizgu lub pęknięcia.",
                "Skontroluj w diagnostyce prąd silnika i obciążenie — sprawdź, czy nie aktywuje się ogranicznik momentu (64502–64505).",
                "Zweryfikuj nastawy regulatora (KV) oraz limitu AXLIM_DS_STOP w konfiguracji osi, a także stan kabla enkodera."
            ),
            relatedCodes = listOf(4014, 6026, 6028, 6062, 9050)
        ),
        4008 to CuratedDiagnostic(
            category = ErrorCategory.MOTION_HOMING,
            severity = ErrorSeverity.ERROR,
            problemDescription = "Oś osiągnęła sprzętowy dodatni wyłącznik krańcowy (Positive Limit Switch).",
            driveReaction = "Natychmiastowe hamowanie awaryjne osi z blokadą dalszego ruchu w kierunku dodatnim.",
            possibleCauses = listOf(
                "Oś wyjechała poza dopuszczalny obszar roboczy (np. utrata bazy lub błędna pozycja zadana).",
                "Uszkodzony czujnik krańcowy, przerwany przewód (dla styków NC) lub przestawiona krzywka najazdowa.",
                "Zanik zasilania 24V na czujniku krańcowym."
            ),
            repairSteps = listOf(
                "Sprawdź fizyczne położenie osi — czy rzeczywiście znajduje się na krańcówce dodatniej.",
                "Jeśli oś nie dojechała do końca, sprawdź czujnik krańcowy, stan diody LED czujnika oraz ciągłość przewodu (styk NC).",
                "Skasuj błąd i w trybie ręcznym (JOG) zjedź z wyłącznika w kierunku przeciwnym (ujemnym).",
                "Zweryfikuj limity programowe SW (powinny zadziałać przed krańcówką sprzętową) oraz procedurę bazowania."
            ),
            relatedCodes = listOf(4009, 4010, 5001, 5003, 5010)
        ),
        4009 to CuratedDiagnostic(
            category = ErrorCategory.MOTION_HOMING,
            severity = ErrorSeverity.ERROR,
            problemDescription = "Oś osiągnęła sprzętowy ujemny wyłącznik krańcowy (Negative Limit Switch).",
            driveReaction = "Natychmiastowe hamowanie awaryjne osi z blokadą dalszego ruchu w kierunku ujemnym.",
            possibleCauses = listOf(
                "Oś wyjechała poza zakres roboczy w kierunku ujemnym.",
                "Uszkodzony czujnik krańcowy ujemny, przerwany przewód (styk NC) lub zanik zasilania 24V.",
                "Rozkalibrowany punkt bazowy (Home)."
            ),
            repairSteps = listOf(
                "Sprawdź fizyczną pozycję osi względem krańcówki ujemnej.",
                "Jeśli oś jest w strefie bezpiecznej, skontroluj czujnik indukcyjny/mechaniczny i przewód sygnałowy (styk rozwierny NC).",
                "Skasuj błąd i ostrożnie zjedź osią w kierunku dodatnim w trybie ręcznym.",
                "Wykonaj ponowne bazowanie osi i sprawdź poprawność limitów programowych SW."
            ),
            relatedCodes = listOf(4008, 4010, 5002, 5004, 5011)
        ),
        4010 to CuratedDiagnostic(
            category = ErrorCategory.MOTION_HOMING,
            severity = ErrorSeverity.ERROR,
            problemDescription = "Nie można załączyć regulatora, ponieważ oba sprzętowe wyłączniki krańcowe (dodatni i ujemny) sygnalizują jednocześnie stan aktywny.",
            driveReaction = "Blokada załączenia stopnia mocy.",
            possibleCauses = listOf(
                "Zanik wspólnego zasilania 24V DC dla obu czujników krańcowych (przy konfiguracji NC).",
                "Odpięta lub uszkodzona wtyczka wejść cyfrowych / skrzynka rozdzielcza czujników.",
                "Błędna konfiguracja polaryzacji wejść krańcowych (NO zamiast NC)."
            ),
            repairSteps = listOf(
                "Sprawdź obecność napięcia zasilania 24V DC na obwodzie czujników krańcowych.",
                "Skontroluj wtyczkę i okablowanie wejść cyfrowych na serwonapędzie ACOPOS / module IO.",
                "Sprawdź stany wejść cyfrowych w diagnostyce PLC (czy oba bity limitów są w stanie aktywnym).",
                "Zweryfikuj konfigurację aktywnego poziomu logicznego krańcówek."
            ),
            relatedCodes = listOf(4008, 4009, 5020, 39301)
        ),
        5006 to CuratedDiagnostic(
            category = ErrorCategory.MOTION_HOMING,
            severity = ErrorSeverity.WARNING,
            problemDescription = "Nie można rozpocząć zadanego ruchu pozycjonującego, ponieważ oś nie jest zbazowana (brak ważnego punktu odniesienia).",
            driveReaction = "Odrzucenie polecenia startu ruchu.",
            possibleCauses = listOf(
                "Po włączeniu zasilania maszyny lub po błędzie enkodera nie przeprowadzono procedury bazowania (Homing).",
                "Utrata zasilania podtrzymania bateryjnego enkodera wieloobrotowego lub wymiana silnika."
            ),
            repairSteps = listOf(
                "Upewnij się, że obszar roboczy osi jest wolny od przeszkód.",
                "Uruchom procedurę bazowania (Homing / Referencjonowanie) osi z panelu operatorskiego.",
                "Jeśli błąd pojawia się po każdym restarcie mimo enkodera absolutnego, sprawdź konfigurację trybu bazowania oraz baterię enkodera."
            ),
            relatedCodes = listOf(5005, 5017, 5034, 5035, 29205)
        ),
        6015 to CuratedDiagnostic(
            category = ErrorCategory.NETWORK_COMM,
            severity = ErrorSeverity.ERROR,
            problemDescription = "Kontroler magistrali CAN wykrył poważne zakłócenia odbioru ramek — licznik błędów odbioru (REC) przekroczył wartość 96.",
            driveReaction = "Zgłoszenie błędu komunikacji CAN; przy dalszym wzroście błędów przejście w stan Bus-Off i zatrzymanie osi.",
            possibleCauses = listOf(
                "Uszkodzony przewód CAN, poluzowane zaciski lub błędna terminacja (brak rezystorów 120 Ω na końcach magistrali).",
                "Zakłócenia EMC — prowadzenie przewodu CAN równolegle z kablami silnikowymi bez właściwego ekranowania.",
                "Niezgodna prędkość transmisji (Baudrate) lub konflikt numerów węzłów (Node ID)."
            ),
            repairSteps = listOf(
                "Sprawdź wtyczki CAN oraz zmierz rezystancję między CAN_H i CAN_L przy wyłączonym zasilaniu (powinna wynosić ok. 60 Ω).",
                "Skontroluj uziemienie ekranu kabla CAN oraz oddzielenie go od przewodów mocy silników.",
                "Sprawdź przełączniki obrotowe adresu węzła (Node ID) i ustawienia prędkości transmisji.",
                "Zwróć uwagę na wartość parametru Info(Slot), aby zidentyfikować konkretny moduł interfejsu CAN."
            ),
            relatedCodes = listOf(6016, 1004, 1012, 32008, 32010)
        ),
        6016 to CuratedDiagnostic(
            category = ErrorCategory.NETWORK_COMM,
            severity = ErrorSeverity.ERROR,
            problemDescription = "Kontroler magistrali CAN wykrył zakłócenia transmisji — licznik błędów nadawania (TEC) przekroczył wartość 96.",
            driveReaction = "Zgłoszenie błędu sieci CAN; ryzyko przejścia węzła w stan Bus-Off i utraty sterowania.",
            possibleCauses = listOf(
                "Brak innego aktywnego odbiornika na magistrali CAN lub przerwana linia transmisyjna.",
                "Zwarcie między liniami CAN_H, CAN_L lub do masy, brak terminacji 120 Ω.",
                "Silne zakłócenia elektromagnetyczne od falowników lub styczników."
            ),
            repairSteps = listOf(
                "Sprawdź ciągłość przewodów CAN_H / CAN_L oraz obecność dwóch rezystorów terminujących 120 Ω na końcach linii.",
                "Zweryfikuj, czy sterownik nadrzędny i pozostałe węzły CAN są zasilone i aktywne.",
                "Popraw mocowanie ekranów kabli sieciowych i silnikowych na szynie PE w szafie.",
                "Po naprawie okablowania zresetuj komunikację sieciową."
            ),
            relatedCodes = listOf(6015, 1004, 1012, 32009, 32011)
        ),
        6018 to CuratedDiagnostic(
            category = ErrorCategory.POWER_STAGE_BRAKE,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Wewnętrzna awaria szyny zasilania 15 V w serwonapędzie ACOPOS.",
            driveReaction = "Natychmiastowe wyłączenie stopnia mocy (Coast stop) i blokada napędu.",
            possibleCauses = listOf(
                "Uszkodzenie wewnętrznego zasilacza impulsowego w module ACOPOS.",
                "Zwarcie w karcie rozszerzeń (Plug-in module) lub w podłączonym obwodzie zewnętrznym obciążającym przetwornicę.",
                "Przeciążenie zasilania pomocniczego 24 V DC."
            ),
            repairSteps = listOf(
                "Odłącz zasilanie 24 V DC i sieciowe, sprawdź stabilność zewnętrznego zasilacza 24 V DC pod obciążeniem.",
                "Wyjmij i sprawdź wizualnie karty wtykowe (Plug-in) pod kątem uszkodzeń lub wygiętych pinów.",
                "Uruchom napęd ponownie; jeśli alarm 6018 powraca natychmiast bez obciążenia kart, moduł ACOPOS wymaga wymiany serwisowej."
            ),
            requiresHighVoltageCaution = true,
            relatedCodes = listOf(6020, 7224, 6032)
        ),
        6019 to CuratedDiagnostic(
            category = ErrorCategory.POWER_STAGE_BRAKE,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Wykryto sprzętowe przetężenie (nadprąd / zwarcie) w stopniu mocy serwonapędu ACOPOS.",
            driveReaction = "Natychmiastowe sprzętowe odcięcie tranzystorów IGBT stopnia mocy (wybieg swobodny lub załączenie hamulca).",
            possibleCauses = listOf(
                "Zwarcie międzyfazowe lub zwarcie doziemne w kablu silnikowym albo w uzwojeniach silnika.",
                "Uszkodzona wtyczka silnikowa (wilgoć, opiłki metalu, nadtopione piny na złączu X5).",
                "Błędne parametry silnika lub nieprawidłowe fazowanie (Phasing) silnika synchronicznego.",
                "Uszkodzenie modułu mocy IGBT wewnątrz serwonapędu ACOPOS."
            ),
            repairSteps = listOf(
                "Odłącz całkowicie zasilanie maszyny i odczekaj min. 5 minut na rozładowanie szyny DC.",
                "Odłącz wtyczkę silnikową X5 od serwonapędu i zmierz miernikiem izolacji (induktorem) rezystancję izolacji kabla i silnika względem PE oraz rezystancję między fazami U-V-W.",
                "Skontroluj stan styków wtyczki na silniku i na napędzie (ślady wilgoci, oleju, przepaleń).",
                "Sprawdź, czy przewód silnikowy w prowadniku kablowym nie jest przetarty lub załamany.",
                "Jeśli silnik i kabel są w pełni sprawne, zweryfikuj poprawność danych silnika i nastaw regulatora prądu; przy powtarzaniu się błędu na odpiętym silniku — wymień ACOPOS."
            ),
            requiresHighVoltageCaution = true,
            relatedCodes = listOf(6052, 6053, 6054, 7222, 9300)
        ),
        6020 to CuratedDiagnostic(
            category = ErrorCategory.POWER_STAGE_BRAKE,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Awaria lub zbyt niski poziom zasilania sterowniczego 24 V DC doprowadzanego do serwonapędu ACOPOS.",
            driveReaction = "Natychmiastowe wyłączenie stopnia mocy i załączenie hamulca postojowego.",
            possibleCauses = listOf(
                "Przeciążony lub uszkodzony zasilacz 24 V DC w szafie sterowniczej.",
                "Spadek napięcia na zbyt cienkich przewodach zasilających lub poluzowany zacisk 24V / GND.",
                "Chwilowe zwarcie w obwodzie 24V (np. cewka hamulca lub zawór pneumatyczny)."
            ),
            repairSteps = listOf(
                "Zmierz napięcie 24 V DC bezpośrednio na zaciskach zasilania serwonapędu ACOPOS (dopuszczalne zazwyczaj 24 V ±20%, zalecane >23 V pod obciążeniem).",
                "Dokręć zaciski na listwie zasilania 24 V oraz wtyczkach na szynie zbiorczej / płycie bazowej.",
                "Sprawdź obciążenie zasilacza 24 V DC w momencie zwalniania hamulców osi.",
                "Sprawdź stan bezpieczników i wyłączników nadprądowych obwodu 24 V."
            ),
            relatedCodes = listOf(6018, 6028, 7224, 39301)
        ),
        6021 to CuratedDiagnostic(
            category = ErrorCategory.POWER_STAGE_BRAKE,
            severity = ErrorSeverity.WARNING,
            problemDescription = "Wykryto stan niski (0 V) na sprzętowym wejściu zezwolenia pracy sterownika (Controller Enable / STO).",
            driveReaction = "Natychmiastowe wyłączenie stopnia mocy; nie można załączyć osi do czasu przywrócenia stanu wysokiego (24 V).",
            possibleCauses = listOf(
                "Otwarty obwód bezpieczeństwa (E-STOP, drzwi, kurtyna) odcinający sygnał Enable / STO.",
                "Przerwany przewód lub luźna wtyczka na wejściu Enable (np. X1 / X2).",
                "Sterownik PLC lub przekaźnik bezpieczeństwa nie podaje sygnału zezwolenia 24 V."
            ),
            repairSteps = listOf(
                "Sprawdź, czy wszystkie osłony bezpieczeństwa są zamknięte i obwód E-STOP jest uzbrojony.",
                "Zmierz napięcie 24 V DC na zaciskach wejścia Enable (Enable1 / Enable2) względem masy GND napędu.",
                "Zweryfikuj stan przekaźnika bezpieczeństwa / modułu SafePLC sterującego sygnałem Enable.",
                "Po przywróceniu stabilnego 24 V skasuj błąd i załącz napęd."
            ),
            relatedCodes = listOf(6023, 6058, 6059, 1011)
        ),
        6023 to CuratedDiagnostic(
            category = ErrorCategory.POWER_STAGE_BRAKE,
            severity = ErrorSeverity.ERROR,
            problemDescription = "Wykryto chwilowy zapad napięcia (spadek poniżej progu logicznego) na wejściu zezwolenia Enable podczas pracy napędu.",
            driveReaction = "Natychmiastowe wyłączenie stopnia mocy z powodu utraty zezwolenia.",
            possibleCauses = listOf(
                "Drgający lub wypalony styk przekaźnika bezpieczeństwa w torze Enable.",
                "Chwilowe zapady napięcia zasilacza 24 V podczas załączania dużych obciążeń (styczniki, hamulce).",
                "Luźny przewód w złączu wejścia Enable."
            ),
            repairSteps = listOf(
                "Sprawdź i dokręć wszystkie połączenia w torze sygnału Enable od przekaźnika bezpieczeństwa do ACOPOS.",
                "Sprawdź styki przekaźnika bezpieczeństwa (czy nie ma zakłóceń / chwilowych przerw podczas wibracji maszyny).",
                "Zweryfikuj stabilność napięcia 24 V DC w chwili załączania hamulców i elektrozaworów.",
                "Sprawdź, czy sygnały Enable1 i Enable2 nie wykazują również błędów 6058/6059."
            ),
            relatedCodes = listOf(6021, 6058, 6059, 6020)
        ),
        6026 to CuratedDiagnostic(
            category = ErrorCategory.POWER_STAGE_BRAKE,
            severity = ErrorSeverity.ERROR,
            problemDescription = "Podczas zwalniania hamulca trzymającego przekroczony został limit prądu stojana silnika.",
            driveReaction = "Przerwanie procedury załączania i wyłączenie stopnia mocy.",
            possibleCauses = listOf(
                "Hamulec mechaniczny nie zwolnił się (sklejona tarcza, uszkodzona cewka lub brak 24 V).",
                "Zbyt duże obciążenie zewnętrzne (np. oś pionowa Z bez przeciwwagi) lub zakleszczenie mechaniczne.",
                "Zbyt krótki czas zwalniania hamulca ustawiony w parametrach silnika."
            ),
            repairSteps = listOf(
                "Sprawdź, czy hamulec silnika faktycznie zwalnia się z wyraźnym kliknięciem przy podaniu 24 V.",
                "Zmierz napięcie 24 V DC bezpośrednio na przewodach hamulca (złącze X5 / puszka silnika).",
                "Sprawdź mechanikę osi pod kątem zablokowania lub nadmiernego obciążenia statycznego.",
                "Zweryfikuj czasy załączania/zwalniania hamulca w tabeli parametrów silnika."
            ),
            requiresHighVoltageCaution = true,
            relatedCodes = listOf(6028, 6029, 6048, 6055, 6056)
        ),
        6028 to CuratedDiagnostic(
            category = ErrorCategory.POWER_STAGE_BRAKE,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Wykryto zbyt niskie napięcie lub zbyt niski prąd w obwodzie hamulca trzymającego silnika (typowy objaw przerwania żyły przewodu hamulca lub braku 24 V).",
            driveReaction = "Zablokowanie napędu i wyłączenie wyjścia sterującego hamulcem.",
            possibleCauses = listOf(
                "Przerwany przewód zasilania hamulca (często w ruchomym prowadniku kablowym robota Wittmann).",
                "Poluzowane lub wypalone styki pinów hamulca we wtyczce silnikowej X5 lub przy silniku.",
                "Spalona cewka hamulca trzymającego w silniku (przerwa w obwodzie).",
                "Zbyt niskie napięcie zasilacza 24 V DC (poniżej ok. 22–23 V) lub brak zasilania 24 V na obwodzie hamulca."
            ),
            repairSteps = listOf(
                "Odłącz zasilanie napędu i sprawdź ciągłość żył hamulca (BR+ / BR-) od złącza X5 napędu do wtyczki silnika — poruszaj prowadnikiem kablowym podczas pomiaru.",
                "Zmierz rezystancję cewki hamulca bezpośrednio na stykach silnika (typowo kilkadziesiąt omów; nieskończoność = uszkodzona cewka).",
                "Sprawdź dokładnie piny w złączu X5 oraz gnieździe na silniku (czy nie są cofnięte lub zaśniedziałe).",
                "Zmierz napięcie zasilania 24 V DC na wejściu zasilania hamulca w serwonapędzie ACOPOS (wymagane stabilne 24 V DC).",
                "Jeśli w tym silniku nie ma fizycznego hamulca, sprawdź konfigurację parametrów silnika (czy wyjście hamulca nie jest błędnie włączone)."
            ),
            requiresHighVoltageCaution = true,
            relatedCodes = listOf(6029, 6055, 6056, 6047, 6020)
        ),
        6045 to CuratedDiagnostic(
            category = ErrorCategory.POWER_STAGE_BRAKE,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Stopień mocy (złącze X5): nie wykryto przepływu prądu w co najmniej jednej fazie silnika podczas załączania lub fazowania.",
            driveReaction = "Natychmiastowe zatrzymanie i wyłączenie stopnia mocy.",
            possibleCauses = listOf(
                "Odłączona lub niedokładnie wpięta wtyczka silnikowa X5 albo wtyczka na korpusie silnika.",
                "Przerwana jedna z żył fazowych (U, V lub W) w przewodzie silnikowym (np. złamana w prowadniku).",
                "Rozwarty stycznik bezpieczeństwa między napędem a silnikiem (jeśli występuje w układzie).",
                "Przerwa w uzwojeniu stojana silnika."
            ),
            repairSteps = listOf(
                "Odłącz zasilanie sieciowe i odczekaj na rozładowanie szyny DC.",
                "Sprawdź solidność osadzenia wtyczki X5 w module ACOPOS oraz złącza na silniku.",
                "Zmierz rezystancję między parami faz U-V, V-W i W-U (wartości powinny być symetryczne i niskie — typowo 0,5–15 Ω).",
                "Sprawdź ciągłość każdej żyły przewodu silnikowego pod kątem pęknięcia w prowadniku kablowym.",
                "Sprawdź parametr Info(Phase), który wskazuje dokładnie uszkodzoną fazę."
            ),
            requiresHighVoltageCaution = true,
            relatedCodes = listOf(6049, 6044, 6046, 6019)
        ),
        6052 to CuratedDiagnostic(
            category = ErrorCategory.POWER_STAGE_BRAKE,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Wykryto przetężenie (nadprąd) po górnej stronie (High-side) mostka tranzystorów IGBT w stopniu mocy.",
            driveReaction = "Natychmiastowe sprzętowe wyłączenie stopnia mocy.",
            possibleCauses = listOf(
                "Zwarcie fazy silnika do masy (PE) lub między fazami.",
                "Uszkodzony kabel silnikowy w prowadniku lub zalana wtyczka.",
                "Uszkodzenie górnego tranzystora IGBT w stopniu mocy ACOPOS."
            ),
            repairSteps = listOf(
                "Odłącz zasilanie i odczekaj na pełne rozładowanie kondensatorów szyny DC.",
                "Odłącz wtyczkę silnikową X5 i wykonaj pomiar rezystancji izolacji kabla i silnika względem PE (min. kilka MΩ).",
                "Sprawdź dokładnie wtyczki silnikowe pod kątem zwarć, wilgoci i opiłków.",
                "Jeśli po odłączeniu silnika błąd występuje nadal przy próbie załączenia — moduł mocy ACOPOS jest uszkodzony i wymaga wymiany."
            ),
            requiresHighVoltageCaution = true,
            relatedCodes = listOf(6053, 6054, 6019, 7222)
        ),
        6053 to CuratedDiagnostic(
            category = ErrorCategory.POWER_STAGE_BRAKE,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Wykryto przetężenie (nadprąd) po dolnej stronie (Low-side) mostka tranzystorów IGBT w stopniu mocy.",
            driveReaction = "Natychmiastowe sprzętowe wyłączenie stopnia mocy.",
            possibleCauses = listOf(
                "Zwarcie w kablu silnikowym lub uzwojeniach silnika (zwarcie doziemne / międzyfazowe).",
                "Uszkodzenie dolnego tranzystora IGBT w module ACOPOS."
            ),
            repairSteps = listOf(
                "Odłącz zasilanie i odczekaj min. 5 minut na rozładowanie szyny DC.",
                "Wypnij złącze X5 i przemierz izolację kabla oraz uzwojeń silnika względem uziemienia PE oraz rezystancję międzyfazową.",
                "Skontroluj prowadnik kablowy pod kątem przetarć izolacji.",
                "Jeśli obwód silnika jest w pełni sprawny, a błąd wraca natychmiast — konieczna jest wymiana modułu ACOPOS."
            ),
            requiresHighVoltageCaution = true,
            relatedCodes = listOf(6052, 6054, 6019, 7222)
        ),
        6054 to CuratedDiagnostic(
            category = ErrorCategory.POWER_STAGE_BRAKE,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Przetężenie stopnia mocy serwonapędu ACOPOS — przekroczono bezpieczny próg prądowy układu wyjściowego.",
            driveReaction = "Natychmiastowe odłączenie stopnia mocy.",
            possibleCauses = listOf(
                "Zwarcie na wyjściu silnikowym X5 lub uszkodzenie izolacji uzwojeń silnika.",
                "Nieprawidłowo dobrane parametry silnika lub regulatora prądu.",
                "Zablokowanie mechaniczne w połączeniu z nieprawidłowym limitem prądu."
            ),
            repairSteps = listOf(
                "Odłącz zasilanie i rozładuj szynę DC przed inspekcją złącza X5.",
                "Zmierz rezystancję uzwojeń oraz izolację kabla i silnika względem PE.",
                "Zweryfikuj poprawność wybranego typu silnika i parametrów fazowania w konfiguracji.",
                "Jeśli błąd pojawia się nawet przy odłączonym silniku, stopień mocy ACOPOS jest uszkodzony."
            ),
            requiresHighVoltageCaution = true,
            relatedCodes = listOf(6019, 6052, 6053, 7222, 9300)
        ),
        7012 to CuratedDiagnostic(
            category = ErrorCategory.ENCODER_FEEDBACK,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Enkoder z interfejsem Hiperface zgłosił wewnętrzny bit błędu sprzętowego lub komunikacyjnego.",
            driveReaction = "Natychmiastowe zatrzymanie osi i zablokowanie załączania napędu.",
            possibleCauses = listOf(
                "Zakłócenia EMC na kablu enkodera lub uszkodzony ekran przewodu.",
                "Uszkodzenie samego enkodera (zabrudzona tarcza optyczna, wstrząs mechaniczny, przegrzanie).",
                "Niestabilne zasilanie enkodera lub luźna wtyczka na karcie wtykowej ACOPOS (sprawdź numer Slot)."
            ),
            repairSteps = listOf(
                "Odczytaj numer gniazda (Slot) z pola Info i sprawdź kod statusu enkodera (powiązany błąd 7013).",
                "Sprawdź wtyczkę kabla enkodera przy silniku oraz na karcie wtykowej ACOPOS (wygięte piny, wilgoć, ekran).",
                "Upewnij się, że ekran kabla enkodera jest solidnie zaciśnięty na obejmie uziemiającej 360°.",
                "Wykonaj restart zasilania (Power OFF/ON) napędu, aby ponownie zainicjalizować enkoder.",
                "Jeśli błąd natychmiast wraca, podmień przewód enkodera; jeśli problem pozostaje — wymień silnik/enkoder."
            ),
            relatedCodes = listOf(7013, 7014, 7015, 7021, 7032)
        ),
        7029 to CuratedDiagnostic(
            category = ErrorCategory.ENCODER_FEEDBACK,
            severity = ErrorSeverity.ERROR,
            problemDescription = "Amplituda sygnałów przyrostowych (sin/cos) odbieranych z enkodera jest poniżej dopuszczalnego progu minimalnego.",
            driveReaction = "Awaryjne zatrzymanie osi i utrata referencji pozycji.",
            possibleCauses = listOf(
                "Przerwanie lub częściowe uszkodzenie żył sygnałowych w przewodzie enkodera.",
                "Słaby styk w złączu enkodera (utlenione lub cofnięte piny).",
                "Zbyt długi kabel enkodera powodujący spadek napięcia zasilania enkodera.",
                "Zabrudzenie lub zużycie głowicy optycznej / magnetycznej enkodera."
            ),
            repairSteps = listOf(
                "Zidentyfikuj właściwą kartę rozszerzeń ACOPOS na podstawie wartości Info(Slot).",
                "Rozepnij i skontroluj wtyczkę enkodera na karcie oraz przy silniku.",
                "Zmierz napięcie zasilania enkodera oraz ciągłość wszystkich żył i ekranu przewodu.",
                "W przypadku enkodera zewnętrznego (liniału) sprawdź szczelinę powietrzną i czystość głowicy czytającej.",
                "Podmień kabel enkodera w celu wykluczenia uszkodzenia w prowadniku kablowym."
            ),
            relatedCodes = listOf(7030, 7031, 7032, 7039, 7040)
        ),
        7032 to CuratedDiagnostic(
            category = ErrorCategory.ENCODER_FEEDBACK,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Zbyt mała amplituda sygnału przyrostowego enkodera — wykryto zanik połączenia lub silne zakłócenie toru sygnałowego.",
            driveReaction = "Natychmiastowe odłączenie napędu i wyłączenie regulatora (utrata sprzężenia zwrotnego).",
            possibleCauses = listOf(
                "Odpięty, urwany lub złamany w prowadniku przewód enkodera.",
                "Brak zasilania enkodera lub uszkodzona karta interfejsu enkodera w slocie ACOPOS.",
                "Uszkodzenie wewnętrzne enkodera w silniku."
            ),
            repairSteps = listOf(
                "Sprawdź fizyczne podłączenie wtyczki enkodera na silniku oraz na wskazanej karcie wtykowej ACOPOS (Info: Slot).",
                "Przemierz ciągłość przewodu enkodera oraz ekranu (szczególnie w miejscach zginania prowadnika kablowego robota).",
                "Sprawdź na wtyczce obecność napięcia zasilania enkodera (zwykle 5 V lub 7–12 V w zależności od typu).",
                "Jeśli przewód jest sprawny, przetestuj na zapasowej karcie enkodera lub wymień enkoder/silnik."
            ),
            relatedCodes = listOf(7029, 7031, 7045, 7046, 7053)
        ),
        7045 to CuratedDiagnostic(
            category = ErrorCategory.ENCODER_FEEDBACK,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Resolwer zgłosił zakłócenie sygnału (niezaliczony test wiarygodności sygnałów sin/cos względem wzbudzenia).",
            driveReaction = "Natychmiastowe zatrzymanie awaryjne osi i wyłączenie napędu.",
            possibleCauses = listOf(
                "Uszkodzony ekran przewodu resolwera lub prowadzenie kabla bezpośrednio przy przewodach mocy.",
                "Poluzowane piny lub uszkodzony przewód sygnałowy resolwera.",
                "Niewłaściwa konfiguracja typu karty interfejsu lub uszkodzenie uzwojeń resolwera."
            ),
            repairSteps = listOf(
                "Sprawdź złącze resolwera przy silniku i na karcie ACOPOS (sprawdź numer gniazda z Info: Slot).",
                "Zweryfikuj ciągłość ekranu 360° na obudowie wtyczki i na listwie uziemiającej w szafie.",
                "Zmierz rezystancję uzwojeń wzbudzenia (Ref) oraz sin/cos na odpiętym przewodzie resolwera.",
                "Jeżeli zakłócenia pojawiają się podczas pracy osi, sprawdź przewód w prowadniku kablowym."
            ),
            relatedCodes = listOf(7046, 7032, 39002)
        ),
        7046 to CuratedDiagnostic(
            category = ErrorCategory.ENCODER_FEEDBACK,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Wykryto przerwę lub zwarcie w przewodzie resolwera.",
            driveReaction = "Natychmiastowe wyłączenie stopnia mocy i zgłoszenie błędu enkodera.",
            possibleCauses = listOf(
                "Wypięta wtyczka resolwera przy silniku lub przy serwonapędzie.",
                "Złamana żyła w kablu sygnałowym resolwera (częsta usterka w prowadnikach osi ruchomych).",
                "Uszkodzone uzwojenie resolwera w silniku."
            ),
            repairSteps = listOf(
                "Sprawdź solidność wpięcia wtyczek resolwera na karcie w slocie ACOPOS i na silniku.",
                "Przemierz multimetrem przejście wszystkich par przewodów (Ref+, Ref-, Sin+, Sin-, Cos+, Cos-) oraz brak zwarć do ekranu.",
                "Poruszaj przewodem w prowadniku podczas testu ciągłości, aby wykryć mikropęknięcia żył.",
                "Po naprawie/wymianie przewodu zresetuj błąd i wykonaj ruch testowy."
            ),
            relatedCodes = listOf(7045, 7032, 31221)
        ),
        7200 to CuratedDiagnostic(
            category = ErrorCategory.POWER_DC_BUS,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Przepięcie na szynie pośredniczącej DC (DC Bus Overvoltage). Napięcie obwodu pośredniego przekroczyło bezpieczną wartość graniczną (typowo ok. 820–850 V DC dla sieci 400 V).",
            driveReaction = "Natychmiastowe odłączenie stopnia mocy (wybieg swobodny lub załączenie hamulca mechanicznego) w celu ochrony kondensatorów i IGBT.",
            possibleCauses = listOf(
                "Zbyt gwałtowne hamowanie ciężkiej osi powodujące duży zwrot energii do szyny DC.",
                "Uszkodzony, odłączony lub przegrzany zewnętrzny/wewnętrzny rezystor hamowania (Bleeder).",
                "Zbyt wysokie napięcie sieci zasilającej 3×400 V AC lub przepięcia z sieci zakładowej.",
                "Uszkodzony tranzystor kluczujący rezystor hamowania (czoper hamowania) w ACOPOS."
            ),
            repairSteps = listOf(
                "Odłącz zasilanie sieciowe, odczekaj min. 5 minut i sprawdź miernikiem bezpieczne rozładowanie szyny DC (<30 V).",
                "Sprawdź podłączenie i zmierz rezystancję zewnętrznego rezystora hamowania (Bleeder) — upewnij się, że nie ma przerwy ani przepalonego bezpiecznika termicznego.",
                "Zweryfikuj w konfiguracji, czy wybrano właściwy rezystor hamowania (wewnętrzny vs zewnętrzny) i czy limit mocy nie jest zaniżony.",
                "Zmierz napięcie sieci zasilającej 3-fazowej pod kątem skoków powyżej 440–480 V AC.",
                "Wydłuż czas hamowania (zmniejsz wartość opóźnienia / rampy hamowania) osi generującej zwrot energii."
            ),
            requiresHighVoltageCaution = true,
            relatedCodes = listOf(7225, 7227, 9040, 38008, 64506)
        ),
        7211 to CuratedDiagnostic(
            category = ErrorCategory.POWER_DC_BUS,
            severity = ErrorSeverity.ERROR,
            problemDescription = "Wykryto spadek napięcia na szynie pośredniczącej DC poniżej progu ostrzegawczego / dolnego limitu roboczego.",
            driveReaction = "Zatrzymanie osi i wyłączenie stopnia mocy.",
            possibleCauses = listOf(
                "Chwilowy zapad napięcia w sieci zakładowej 3×400 V AC.",
                "Wyłączenie stycznika głównego zasilania podczas pracy napędu.",
                "Poluzowany zacisk zasilania fazowego L1/L2/L3 lub uszkodzony bezpiecznik jednej z faz."
            ),
            repairSteps = listOf(
                "Zmierz napięcia międzyfazowe L1-L2, L2-L3, L3-L1 na wejściu zasilania ACOPOS.",
                "Sprawdź stan stycznika głównego, bezpieczników mocy oraz dokręcenie zacisków wejściowych.",
                "Zweryfikuj, czy obwód bezpieczeństwa nie otwiera stycznika głównego zanim napędy wyhamują.",
                "Sprawdź wartość progu z parametru Info(Low voltage limit)."
            ),
            requiresHighVoltageCaution = true,
            relatedCodes = listOf(7212, 7215, 7219, 7221)
        ),
        7215 to CuratedDiagnostic(
            category = ErrorCategory.POWER_DC_BUS,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Wykryto zanik co najmniej jednej fazy w trójfazowej sieci zasilającej serwonapęd ACOPOS.",
            driveReaction = "Zatrzymanie napędu i blokada załączania stopnia mocy.",
            possibleCauses = listOf(
                "Przepalony bezpiecznik lub wyzwolony wyłącznik nadprądowy jednej z faz zasilania.",
                "Wypalony styk w styczniku głównym lub rozłączniku głównym szafy.",
                "Poluzowany przewód fazowy L1, L2 lub L3 na zaciskach wejściowych zasilacza/napędu."
            ),
            repairSteps = listOf(
                "Zmierz wszystkie trzy napięcia międzyfazowe (L1-L2, L2-L3, L3-L1) bezpośrednio na zaciskach wejściowych serwonapędu ACOPOS.",
                "Sprawdź bezpieczniki, wyłączniki silnikowe oraz napięcia przed i za stycznikiem głównym.",
                "Po odłączeniu zasilania dokręć zaciski śrubowe toru zasilania mocy 3×400 V.",
                "Skontroluj również stan filtra sieciowego EMC (czy nie ma przerwy na jednej fazie)."
            ),
            requiresHighVoltageCaution = true,
            relatedCodes = listOf(7221, 7211, 7212, 7214)
        ),
        7222 to CuratedDiagnostic(
            category = ErrorCategory.POWER_DC_BUS,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Wykryto prąd sumaryczny (zwarcie doziemne / upływ prądu do PE) na złączu silnikowym X5 przekraczający dopuszczalny limit.",
            driveReaction = "Natychmiastowe odłączenie stopnia mocy IGBT i zablokowanie napędu.",
            possibleCauses = listOf(
                "Przebicie izolacji lub zwarcie jednej z faz silnika (U/V/W) do przewodu ochronnego PE / obudowy maszyny.",
                "Przetarty lub zgnieciony kabel silnikowy w prowadniku kablowym (kontakt żyły z ekranem).",
                "Zawilgocenie, olej lub chłodziwo wewnątrz puszki przyłączeniowej albo wtyczki silnika.",
                "Zbyt długi kabel silnikowy generujący duży pojemnościowy prąd upływowy do ekranu."
            ),
            repairSteps = listOf(
                "Odłącz zasilanie sieciowe i odczekaj min. 5 minut na całkowite rozładowanie szyny DC.",
                "Odłącz wtyczkę silnikową X5 od serwonapędu ACOPOS.",
                "Wykonaj pomiar rezystancji izolacji (miernikiem izolacji 500 V DC) między zwartymi fazami U-V-W a przewodem ochronnym PE i ekranem — wartość powinna wynosić co najmniej kilka–kilkadziesiąt MΩ.",
                "Rozepnij złącze przy samym silniku, aby ustalić osobno, czy uszkodzony jest przewód w prowadniku czy uzwojenie silnika.",
                "Osusz i oczyść złącza, wymień uszkodzony odcinek przewodu lub silnik, następnie skasuj alarm."
            ),
            requiresHighVoltageCaution = true,
            relatedCodes = listOf(6019, 6052, 6053, 6054, 7223)
        ),
        7225 to CuratedDiagnostic(
            category = ErrorCategory.POWER_DC_BUS,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Sprzętowe przepięcie na szynie DC — napięcie przekroczyło maksymalny próg bezpieczeństwa elektroniki mocy.",
            driveReaction = "Natychmiastowe odcięcie stopnia mocy.",
            possibleCauses = listOf(
                "Niesprawny lub odłączony rezystor hamowania (Bleeder) podczas hamowania osi.",
                "Zbyt duża energia zwrotna z dynamicznego hamowania ciężkiego mechanizmu.",
                "Przepięcie sieciowe lub błędne zasilanie (np. 480 V na module 230 V)."
            ),
            repairSteps = listOf(
                "Odłącz zasilanie, rozładuj szynę DC i zweryfikuj ciągłość oraz rezystancję rezystora hamowania (Bleeder).",
                "Sprawdź napięcie sieci zasilającej i porównaj z tabliczką znamionową modułu ACOPOS.",
                "Złagodź rampy hamowania w programie ruchu, jeśli błąd występuje zawsze przy zatrzymaniu konkretnej osi.",
                "Jeśli rezystor jest sprawny i dobrze skonfigurowany, sprawdź stopień kluczujący Bleeder w ACOPOS."
            ),
            requiresHighVoltageCaution = true,
            relatedCodes = listOf(7200, 7227, 9040, 38008)
        ),
        7227 to CuratedDiagnostic(
            category = ErrorCategory.POWER_DC_BUS,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Wykryto przetężenie (nadprąd) w obwodzie rezystora hamowania (Bleeder).",
            driveReaction = "Wyłączenie obwodu hamowania i natychmiastowe odłączenie stopnia mocy.",
            possibleCauses = listOf(
                "Zwarcie w przewodach lub na zaciskach zewnętrznego rezystora hamowania.",
                "Podłączony zewnętrzny rezystor hamowania o zbyt małej rezystancji (poniżej wartości minimalnej dla tego ACOPOS).",
                "Uszkodzony wewnętrzny tranzystor kluczujący rezystor hamowania."
            ),
            repairSteps = listOf(
                "Odłącz zasilanie sieciowe i poczekaj na całkowite rozładowanie szyny DC.",
                "Odłącz zewnętrzny rezystor hamowania i zmierz jego rezystancję oraz izolację względem uziemienia PE.",
                "Porównaj zmierzoną rezystancję z dokumentacją techniczną danego modelu ACOPOS (nie może być mniejsza od R_min).",
                "Sprawdź przewody rezystora pod kątem nadtopionej izolacji lub zwarcia do obudowy szafy."
            ),
            requiresHighVoltageCaution = true,
            relatedCodes = listOf(7200, 7225, 9040, 38008)
        ),
        9000 to CuratedDiagnostic(
            category = ErrorCategory.THERMAL_OVERLOAD,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Czujnik temperatury radiatora serwonapędu ACOPOS przekroczył maksymalną dopuszczalną temperaturę — ruch osi został zatrzymany.",
            driveReaction = "Natychmiastowe zatrzymanie ruchu i wyłączenie stopnia mocy do czasu ostygnięcia radiatora (ponowne kasowanie możliwe po wystąpieniu kodu 36).",
            possibleCauses = listOf(
                "Zabrudzone lub zatkane filtry powietrza w szafie sterowniczej albo zapchane żebra radiatora ACOPOS.",
                "Uszkodzony wentylator chłodzący w module ACOPOS lub niesprawny klimatyzator szafy sterowniczej.",
                "Zbyt wysoka temperatura otoczenia w szafie (>40–50 °C) lub brak zachowanych odstępów montażowych nad/pod napędem.",
                "Ciągłe przeciążenie prądowe osi (cykl pracy przekracza możliwości chłodzenia)."
            ),
            repairSteps = listOf(
                "Sprawdź, czy wentylator serwonapędu ACOPOS oraz wentylatory/klimatyzator szafy sterowniczej obracają się prawidłowo.",
                "Wyczyść maty filtracyjne szafy sterowniczej i przedmuchaj radiator serwonapędu sprężonym powietrzem (przy wyłączonym zasilaniu).",
                "Zmierz temperaturę wewnątrz szafy sterowniczej — upewnij się, że nie przekracza specyfikacji.",
                "Poczekaj na schłodzenie radiatora (aż pojawi się status 36 lub temperatura spadnie poniżej progu) i dopiero wtedy skasuj błąd.",
                "Sprawdź obciążenie prądowe osi podczas cyklu roboczego pod kątem mechanicznych oporów."
            ),
            relatedCodes = listOf(9001, 41001, 36, 9030, 9078)
        ),
        9010 to CuratedDiagnostic(
            category = ErrorCategory.THERMAL_OVERLOAD,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Czujnik temperatury silnika (lub dławika / czujnik zewnętrzny) przekroczył maksymalny dopuszczalny próg temperatury.",
            driveReaction = "Zatrzymanie ruchu i wyłączenie stopnia mocy w celu ochrony uzwojeń silnika przed spaleniem.",
            possibleCauses = listOf(
                "Długotrwałe przeciążenie mechaniczne silnika (zatarte prowadnice, zbyt duży ciężar, niecałkowicie zwolniony hamulec).",
                "Niesprawny wentylator obcego chłodzenia silnika (jeśli występuje) lub wysoka temperatura otoczenia przy formie wtryskarki.",
                "Błędne parametry prądowe silnika lub źle dobrany typ czujnika (PTC / KTY / PT1000) w konfiguracji."
            ),
            repairSteps = listOf(
                "Sprawdź ostrożnie temperaturę korpusu silnika (uwaga na oparzenie!) i porównaj z odczytem temperatury w diagnostyce napędu.",
                "Pozwól silnikowi ostygnąć, a w tym czasie skontroluj mechanikę osi pod kątem oporów ruchu i stanu hamulca.",
                "Sprawdź czystość żeber chłodzących silnika oraz działanie wentylatora (jeśli silnik ma chłodzenie wymuszone).",
                "Jeśli silnik jest zimny, a błąd występuje — sprawdź obwód czujnika temperatury (błędy 9012/9013) i konfigurację typu sensora."
            ),
            relatedCodes = listOf(9011, 9012, 9013, 41011, 9070)
        ),
        9012 to CuratedDiagnostic(
            category = ErrorCategory.THERMAL_OVERLOAD,
            severity = ErrorSeverity.ERROR,
            problemDescription = "Czujnik temperatury silnika / dławika jest niepodłączony, uszkodzony lub wykryto przerwę w jego obwodzie.",
            driveReaction = "Zablokowanie załączania napędu lub zatrzymanie osi w celu ochrony termicznej.",
            possibleCauses = listOf(
                "Przerwana żyła czujnika temperatury (T+ / T-) w przewodzie silnikowym lub enkodera.",
                "Poluzowane lub cofnięte piny we wtyczce przy silniku albo na napędzie.",
                "Uszkodzony wewnętrzny termistor silnika lub błędnie ustawiony typ czujnika w parametrach."
            ),
            repairSteps = listOf(
                "Odłącz wtyczkę z żyłami czujnika temperatury i zmierz jego rezystancję multimetrem (dla KTY84 ok. 600 Ω w 25°C, dla PT1000 ok. 1097 Ω, dla PTC ok. 20–100 Ω).",
                "Jeśli miernik wskazuje przerwę (OL), rozepnij przewód przy silniku, aby odróżnić uszkodzenie kabla od uszkodzenia czujnika w silniku.",
                "Sprawdź styki złącza pod kątem utlenienia lub wygiętych pinów.",
                "Zweryfikuj konfigurację parametrów czujnika temperatury silnika w ustawieniach ACOPOS."
            ),
            relatedCodes = listOf(9013, 9010, 9011, 9002)
        ),
        9040 to CuratedDiagnostic(
            category = ErrorCategory.THERMAL_OVERLOAD,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Model termiczny rezystora hamowania (Bleeder) wykrył przekroczenie dopuszczalnej temperatury — ruch został zatrzymany.",
            driveReaction = "Zatrzymanie osi i wyłączenie stopnia mocy do czasu schłodzenia modelu termicznego rezystora hamowania.",
            possibleCauses = listOf(
                "Zbyt częste lub zbyt gwałtowne hamowania ciężkich osi w krótkim czasie cyklu.",
                "Zbyt mała moc ciągła dobranego rezystora hamowania (wewnętrznego zamiast zewnętrznego).",
                "Zbyt wysokie napięcie sieci zasilającej powodujące częste włączanie obwodu Bleeder."
            ),
            repairSteps = listOf(
                "Odczekaj na ostygnięcie rezystora hamowania i obniżenie obciążenia termicznego w diagnostyce.",
                "Sprawdź wentylację wokół rezystora hamowania (uwaga — rezystor może być bardzo gorący!).",
                "Zredukuj dynamikę hamowania (wydłuż rampy zwalniania) w cyklu pracy maszyny.",
                "Zweryfikuj poprawność wpisanej mocy i rezystancji rezystora hamowania w parametrach ACOPOS oraz sprawdź napięcie sieci 3×400 V."
            ),
            relatedCodes = listOf(9041, 41041, 7200, 7227, 64506)
        ),
        9050 to CuratedDiagnostic(
            category = ErrorCategory.THERMAL_OVERLOAD,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Przeciążenie prądem szczytowym serwonapędu ACOPOS (model I²t stopnia mocy przekroczył 100%) — ruch zatrzymany.",
            driveReaction = "Natychmiastowe zatrzymanie osi i wyłączenie stopnia mocy.",
            possibleCauses = listOf(
                "Zablokowanie mechaniczne, kolizja lub gwałtowny wzrost oporów na osi.",
                "Niezwalniający się hamulec silnika zmuszający napęd do pracy na prądzie maksymalnym.",
                "Zbyt dynamiczny profil ruchu (za duże przyspieszenia / ciężkie narzędzie robota)."
            ),
            repairSteps = listOf(
                "Sprawdź mechanikę osi, prowadnice, przekładnię oraz prawidłowość zwalniania hamulca trzymającego.",
                "Przeanalizuj przebieg prądu osi (Trace / diagnostyka) — znajdź fazę cyklu, w której prąd utrzymuje się na szczycie.",
                "Złagodź przyspieszenia i opóźnienia w programie ruchu lub zmniejsz masę chwytaka / detalu.",
                "Odczekaj chwilę na regenerację modelu termicznego prądu szczytowego i skasuj alarm."
            ),
            relatedCodes = listOf(9051, 9060, 9075, 41051, 4007)
        ),
        9060 to CuratedDiagnostic(
            category = ErrorCategory.THERMAL_OVERLOAD,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Przeciążenie prądem ciągłym serwonapędu ACOPOS — średnie obciążenie prądowe przekroczyło znamionowy prąd ciągły napędu.",
            driveReaction = "Zatrzymanie osi i wyłączenie stopnia mocy.",
            possibleCauses = listOf(
                "Stałe zwiększone opory mechaniczne (brak smarowania, zatarte łożysko, krzywa prowadnica).",
                "Zbyt ciężki cykl roboczy (brak przerw między dynamicznymi ruchami) lub niedowymiarowany napęd.",
                "Błędna konfiguracja parametrów silnika powodująca nieefektywne sterowanie prądem."
            ),
            repairSteps = listOf(
                "Sprawdź układ smarowania osi oraz opory ręcznego przesuwu po zwolnieniu hamulca.",
                "Porównaj prąd ciągły pobierany przez oś z prądem znamionowym serwonapędu i silnika.",
                "Zwiększ czas cyklu lub obniż przyspieszenia, aby obniżyć średni prąd RMS.",
                "Zweryfikuj parametry silnika oraz chłodzenie szafy sterowniczej."
            ),
            relatedCodes = listOf(9061, 9050, 9070, 9075, 41061)
        ),
        9300 to CuratedDiagnostic(
            category = ErrorCategory.THERMAL_OVERLOAD,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Regulator prądu wykrył nadprąd przekraczający dopuszczalny limit programowy/sprzętowy.",
            driveReaction = "Natychmiastowe wyłączenie stopnia mocy.",
            possibleCauses = listOf(
                "Błędne fazowanie silnika (Phasing), zamienione fazy U/V/W lub przesunięty enkoder na wale.",
                "Zbyt agresywne nastawy regulatora prądu (oscylacje prądu) albo nieprawidłowa indukcyjność/rezystancja w danych silnika.",
                "Zwarcie częściowe w przewodzie lub uzwojeniu silnika."
            ),
            repairSteps = listOf(
                "Odłącz zasilanie i sprawdź rezystancję oraz izolację kabla i uzwojeń silnika (złącze X5).",
                "Zweryfikuj kolejność faz U-V-W oraz poprawność wszystkich parametrów silnika w konfiguracji.",
                "W przypadku wymiany silnika lub enkodera wykonaj poprawną procedurę fazowania (Phasing).",
                "Sprawdź nastawy regulatora prądu (lub przywróć wartości fabryczne z bazy silników)."
            ),
            requiresHighVoltageCaution = true,
            relatedCodes = listOf(6019, 6044, 6054, 7222)
        ),
        39010 to CuratedDiagnostic(
            category = ErrorCategory.ENCODER_FEEDBACK,
            severity = ErrorSeverity.CRITICAL,
            problemDescription = "Enkoder absolutny EnDat zgłosił bit alarmu: wymagana natychmiastowa wymiana baterii podtrzymującej pamięć wieloobrotową.",
            driveReaction = "Zgłoszenie alarmu enkodera — po wyłączeniu zasilania maszyny pozycja absolutna (wieloobrotowa) zostanie utracona.",
            possibleCauses = listOf(
                "Rozładowana bateria buforowa podtrzymująca licznik obrotów enkodera absolutnego.",
                "Słaby styk w koszyku baterii lub uszkodzony przewód zasilania bateryjnego."
            ),
            repairSteps = listOf(
                "NIE wyłączaj zasilania sterownika, jeśli chcesz zachować pozycję bazową podczas wymiany baterii (zgodnie z instrukcją maszyny).",
                "Zlokalizuj moduł / zasobnik baterii podtrzymania enkodera dla osi wskazanej w Info(Slot).",
                "Wymień baterię na nową, zgodnego typu i napięcia, zachowując polaryzację.",
                "Skasuj błąd enkodera i zweryfikuj poprawność pozycji osi (w razie utraty zasilania wykonaj ponowne bazowanie osi)."
            ),
            relatedCodes = listOf(39014, 39029, 39003, 39028)
        ),
        39301 to CuratedDiagnostic(
            category = ErrorCategory.POWER_STAGE_BRAKE,
            severity = ErrorSeverity.ERROR,
            problemDescription = "Karta cyfrowych wejść/wyjść (Digital IO) w slocie ACOPOS wykryła zanik zasilania pomocniczego 24 V DC.",
            driveReaction = "Wyłączenie wyjść cyfrowych na karcie IO i zgłoszenie błędu napędu.",
            possibleCauses = listOf(
                "Brak napięcia 24 V DC na zaciskach zasilania karty wejść/wyjść w module ACOPOS.",
                "Wyzwolony bezpiecznik obwodu 24 V dla układów IO lub luźna wtyczka zasilająca kartę.",
                "Zwarcie na jednym z wyjść cyfrowych przeciążające linię 24 V."
            ),
            repairSteps = listOf(
                "Zidentyfikuj numer gniazda (Slot) z pola Info i zlokalizuj właściwą kartę Digital IO w napędzie ACOPOS.",
                "Zmierz napięcie 24 V DC bezpośrednio na zaciskach zasilania tej karty.",
                "Sprawdź bezpiecznik i okablowanie doprowadzające 24 V DC oraz masę GND.",
                "Jeśli napięcie spada po załączeniu wyjść, sprawdź błędy diagnostyczne kanałów (39302–39306) pod kątem zwarcia."
            ),
            relatedCodes = listOf(39302, 39303, 39305, 39306, 6020)
        )
    )
}
