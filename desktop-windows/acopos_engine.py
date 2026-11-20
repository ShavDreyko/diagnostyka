"""Silnik parsera i diagnostyki błędów serwonapędów B&R ACOPOS / Wittmann (wersja Windows Portable)."""

from __future__ import annotations

import os
import re
import sys
import unicodedata
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Set, Tuple

PL_ASSET_FILE = "b_edy_wittman_acopos_PL_e3174ce4.txt"
EN_ASSET_FILE = "lista_b_ed_w_wittman_ang_2cead588.txt"

SEVERITY_LABELS = {
    "CRITICAL": "Krytyczny",
    "ERROR": "Błąd",
    "WARNING": "Ostrzeżenie",
    "INFO": "Informacja",
}

CATEGORIES: List[Tuple[str, str, str]] = [
    ("POWER_DC_BUS", "Zasilanie i szyna DC", "7200–7227"),
    ("POWER_STAGE_BRAKE", "Stopień mocy i hamulec", "6017–6063, 38000+"),
    ("ENCODER_FEEDBACK", "Enkoder i sprzężenie", "7012–7061, 39001+"),
    ("THERMAL_OVERLOAD", "Temperatura i przeciążenie", "9000–9300, 41001+"),
    ("MOTION_HOMING", "Ruch, limity i bazowanie", "4005–5039"),
    ("SAFETY_SAFEMC", "Bezpieczeństwo SafeMC", "35000–35255"),
    ("NETWORK_COMM", "Sieć CAN / POWERLINK", "1004–1024, 32001+"),
    ("CAM_SYNC", "Krzywki i synchronizacja", "5101–5329, 37101+"),
    ("PLCOPEN_NC", "PLCopen i sterownik NC", "29200–32767"),
    ("PARAMETERS_MEMORY", "Parametry, EEPROM i system", "1–1027, 8001–8020"),
]

CATEGORY_TITLES = {cid: title for cid, title, _ in CATEGORIES}

COMMON_FAULT_CODES = [4007, 6019, 6021, 6028, 7032, 7200, 7215, 7222, 9000, 9050, 4008, 6015]


@dataclass(frozen=True)
class AcoposError:
    code: int
    title_pl: str
    title_en: Optional[str]
    info_detail: Optional[str]
    info_explanation_pl: Optional[str]
    category_id: str
    severity: str
    problem_description: str
    drive_reaction: str
    possible_causes: List[str]
    repair_steps: List[str]
    requires_hv_caution: bool = False
    related_codes: List[int] = field(default_factory=list)

    @property
    def category_title(self) -> str:
        return CATEGORY_TITLES.get(self.category_id, self.category_id)

    @property
    def severity_label(self) -> str:
        return SEVERITY_LABELS.get(self.severity, self.severity)


PL_LINE_RE = re.compile(r"^\s*[•\-*]?\s*(\d{1,2}\.\d{3}|\d{1,5})\s*:\s*(.+)$")
EN_LINE_RE = re.compile(r"^\s*(\d{1,5})\s*:\s*(.+)$")
INFO_LINE_RE = re.compile(r"^\s*Info\((.+)\)\s*:\s*(.+)$")

TITLE_REPLACEMENTS = [
    ("Model temperatury odpowietrzacza", "Model temperatury rezystora hamowania (Bleeder)"),
    ("Odpowietrznik: brak przepływu prądu", "Rezystor hamowania (Bleeder): brak przepływu prądu"),
    ("Bleeder: Overcurrent", "Rezystor hamowania (Bleeder): przetężenie (nadprąd)"),
    ("Automat kamery", "Automat krzywkowy (Cam Automat)"),
    ("automat kamery", "automat krzywkowy"),
    ("danych profilu kamery", "danych profilu krzywki"),
    ("Dysk nie odpowiada", "Napęd nie odpowiada"),
    ("czy dysk jest w sieci", "czy napęd jest podłączony w sieci"),
    ("na dysku", "w napędzie"),
    ("z dysku", "z napędu"),
    ("Dysk nie został zsynchronizowany", "Napęd nie został zsynchronizowany"),
    ("typu CWU", "typu sprzętu (HW)"),
    ("Spust główny", "Wyzwalacz osi głównej (Master trigger)"),
    ("pozycja spustowa", "pozycja wyzwalacza"),
    ("Aktywny bęben", "Aktywny sekwencer bębnowy (Drumsequencer)"),
    ("Faza: Prędkość do wysokiej", "Fazowanie: zbyt wysoka prędkość obrotowa"),
    ("ACOPOS: Nadprądowe", "ACOPOS: Przetężenie (nadprąd)"),
    ("Stopień mocy: strona wysoka: nadprądowa", "Stopień mocy: przetężenie po stronie górnej (High-side)"),
    ("Stopień mocy: strona dolna: nadprądowa", "Stopień mocy: przetężenie po stronie dolnej (Low-side)"),
    ("Stopień mocy: Nadprądowy", "Stopień mocy: przetężenie (nadprąd)"),
    ("Magistrala DC: nadprądowa", "Szyna DC: przetężenie (nadprąd)"),
    ("Kontroler prądu: Nadprądowy", "Regulator prądu: przetężenie (nadprąd)"),
    ("wysyłanie Zapis kupna (błąd sieci?) Błąd", "Błąd wysyłania żądania zapisu (błąd sieci?)"),
    ("nie Położenia przełącznika w kolejności rosnącej: Cam sterowania", "Sterowanie krzywkowe: pozycje przełączania nie są w kolejności rosnącej"),
    ("Nie ważne dane profilu krzywki lub wyłączonym", "Brak prawidłowych danych profilu krzywki lub stan wyłączony"),
    ("Pełny wyzwalacz FIFO pełny", "Bufor FIFO wyzwalacza Master jest pełny"),
    ("Parametr (y) identyfikacyjny brakujące", "Brak wymaganych parametrów identyfikacji"),
    ("Monitorowanie przekroczenia limitu czasu w cyklicznych danych do kierowania", "Przekroczenie limitu czasu monitorowania danych cyklicznych do napędu"),
    ("Enable1: Zwolnienie napięcia", "Wejście Enable1: zapad napięcia"),
    ("Enable2: Zwolnienie napięcia", "Wejście Enable2: zapad napięcia"),
    ("bit błędu hiperface", "bit błędu interfejsu Hiperface"),
    ("zaburzenia przewodu: Resolwer", "Resolwer: zakłócenie lub przerwa w przewodzie"),
    ("Błąd internatora, test CUnit", "Błąd wewnętrzny modułu SafeMC (test CUnit)"),
]

EN_ONLY_PL_TRANSLATIONS: Dict[int, str] = {
    36: "Temperatura radiatora wróciła do dopuszczalnego zakresu pracy (błąd można skasować)",
    29200: "PLCopen: Obiekt osi jest nieprawidłowy",
    29203: "PLCopen: Napęd nie jest gotowy do pracy (Drive not ready)",
    29204: "PLCopen: Nieprawidłowy numer parametru",
    29205: "PLCopen: Oś nie została zbazowana (brak referencji)",
    29206: "PLCopen: Regulator napędu jest wyłączony (Controller off)",
    29207: "PLCopen: Ten typ ruchu jest obecnie niedozwolony",
    29208: "PLCopen: Obiekt osi zmienił się od ostatniego wywołania bloku FB",
    29209: "PLCopen: Napęd znajduje się w stanie błędu",
    29210: "PLCopen: Inicjalizacja parametrów (Global-init) nie powiodła się",
    29211: "PLCopen: Przełączanie hamulca trzymającego niemożliwe — regulator jest włączony",
    29214: "PLCopen: Bazowanie (Homing) jest niemożliwe w bieżącym stanie",
    29215: "PLCopen: Ruch dyskretny (pozycjonowanie) jest niemożliwy",
    29216: "PLCopen: Ruch ciągły (prędkościowy) jest niemożliwy",
    29217: "PLCopen: Nieprawidłowy parametr wejściowy bloku funkcyjnego",
    29218: "PLCopen: Nieznany stan osi PLCopen",
    29219: "PLCopen: Nieprawidłowa wartość parametru PLCopen",
    29221: "PLCopen: Brak zdefiniowanej nazwy profilu krzywki (Cam)",
    29222: "PLCopen: Błąd podczas przesyłania (download) krzywki do napędu",
    29225: "PLCopen: Pozycja docelowa wykracza poza okres osi (Axis period)",
    29226: "PLCopen: Błąd w serwonapędzie — użyj MC_ReadAxisError w celu odczytu szczegółów",
    29229: "PLCopen: Ruch zsynchronizowany jest niemożliwy w bieżącym stanie",
    29238: "PLCopen: Blok funkcyjny (FB) nie może być użyty w bieżącym stanie osi",
    29256: "PLCopen: Wykonywanie wielu poleceń jednocześnie jest niedozwolone",
    29265: "PLCopen: Komunikacja z serwonapędem nie powiodła się",
    29268: "PLCopen: Blok funkcyjny PLCopen został przerwany przez inny blok",
    29295: "PLCopen: Wystąpił błąd — sprawdź wyjście ErrorRecord",
    29312: "PLCopen: Wystąpił błąd podczas testu hamulca trzymającego",
    32300: "Strojenie regulatora: Wymagane wgranie biblioteki acp10tun do sterownika",
    32301: "Strojenie regulatora: Uruchomienie niedozwolone — występują niekasowane błędy",
    32303: "Strojenie regulatora: Oś nie przeszła inicjalizacji globalnej",
    32304: "Strojenie regulatora: Regulator musi być wyłączony przed startem strojenia",
    32305: "Strojenie regulatora: Regulator napędu nie jest gotowy",
    32306: "Strojenie regulatora: Rejestrator Trace jest już aktywny",
    32313: "Strojenie regulatora: Wybrany tryb strojenia wymaga zbazowania osi",
    32316: "Strojenie regulatora: Obliczony moment bezwładności wynosi zero",
}

CURATED: Dict[int, dict] = {
    4007: {
        "category": "MOTION_HOMING",
        "severity": "CRITICAL",
        "problem": "Przekroczony dopuszczalny uchyb nadążny pozycji (Lag Error). Rzeczywista pozycja silnika odbiega od zadanej bardziej niż pozwala parametr AXLIM_DS_STOP.",
        "reaction": "Awaryjne zatrzymanie osi i wyłączenie stopnia mocy po przekroczeniu limitu uchybu.",
        "causes": [
            "Kolizja mechaniczna, zakleszczenie osi, uszkodzone łożysko lub zatarcie prowadnicy.",
            "Niezwalniający się hamulec trzymający silnika lub poślizg sprzęgła mechanicznego.",
            "Zbyt mały moment obrotowy (ograniczenie prądu) albo zbyt agresywne przyspieszenie.",
            "Błąd okablowania faz U/V/W, luźny enkoder lub rozstrojony regulator pozycji/prędkości.",
        ],
        "steps": [
            "Natychmiast sprawdź mechanikę osi pod kątem kolizji, zakleszczenia lub nadmiernych oporów ruchu.",
            "Zweryfikuj działanie hamulca trzymającego (napięcie 24 V DC na cewce hamulca podczas ruchu).",
            "Sprawdź sprzęgło między silnikiem a śrubą/przekładnią pod kątem pęknięcia lub poślizgu.",
            "Skontroluj prąd silnika i obciążenie w diagnostyce (sprawdź, czy nie działają ograniczniki 64502–64505).",
            "Zweryfikuj nastawy regulatora (KV), limit AXLIM_DS_STOP oraz stan kabla enkodera.",
        ],
        "hv": False,
        "related": [4014, 6026, 6028, 6062, 9050],
    },
    4008: {
        "category": "MOTION_HOMING",
        "severity": "ERROR",
        "problem": "Oś osiągnęła sprzętowy dodatni wyłącznik krańcowy (Positive Limit Switch).",
        "reaction": "Natychmiastowe hamowanie awaryjne z blokadą dalszego ruchu w kierunku dodatnim.",
        "causes": [
            "Oś wyjechała poza dopuszczalny obszar roboczy (utrata bazy lub błędna pozycja zadana).",
            "Uszkodzony czujnik krańcowy, przerwany przewód (dla styków NC) lub zanik 24 V DC.",
        ],
        "steps": [
            "Sprawdź fizyczne położenie osi względem krańcówki dodatniej.",
            "Jeśli oś nie dojechała do końca, sprawdź czujnik, diodę LED czujnika oraz ciągłość przewodu (styk NC).",
            "Skasuj błąd i w trybie ręcznym (JOG) zjedź z krańcówki w kierunku ujemnym.",
            "Zweryfikuj limity programowe SW oraz wykonaj ponowne bazowanie osi.",
        ],
        "hv": False,
        "related": [4009, 4010, 5001, 5003, 5010],
    },
    6015: {
        "category": "NETWORK_COMM",
        "severity": "ERROR",
        "problem": "Kontroler magistrali CAN wykrył poważne zakłócenia odbioru ramek — licznik błędów odbioru (REC) przekroczył 96.",
        "reaction": "Zgłoszenie błędu komunikacji CAN; przy dalszych zakłóceniach przejście w Bus-Off i stop osi.",
        "causes": [
            "Uszkodzony przewód CAN, poluzowane zaciski lub brak rezystorów terminujących 120 Ω.",
            "Zakłócenia EMC od przewodów mocy silników lub uszkodzone ekranowanie.",
            "Niezgodna prędkość transmisji (Baudrate) albo konflikt numerów węzłów (Node ID).",
        ],
        "steps": [
            "Zmierz rezystancję między CAN_H i CAN_L przy wyłączonym zasilaniu (powinna wynosić ok. 60 Ω).",
            "Skontroluj wtyczki CAN, uziemienie ekranu oraz oddzielenie od kabli silnikowych.",
            "Sprawdź przełączniki obrotowe adresu węzła (Node ID) i ustawienia Baudrate.",
            "Zidentyfikuj numer gniazda (Slot) z pola Info i sprawdź kartę interfejsu CAN.",
        ],
        "hv": False,
        "related": [6016, 1004, 1012, 32008, 32010],
    },
    6019: {
        "category": "POWER_STAGE_BRAKE",
        "severity": "CRITICAL",
        "problem": "Wykryto sprzętowe przetężenie (nadprąd / zwarcie) w stopniu mocy serwonapędu ACOPOS.",
        "reaction": "Natychmiastowe sprzętowe wyłączenie tranzystorów IGBT stopnia mocy.",
        "causes": [
            "Zwarcie międzyfazowe lub zwarcie doziemne w kablu silnikowym albo w uzwojeniach silnika.",
            "Zalana, zabrudzona lub nadtopiona wtyczka silnikowa (złącze X5 lub gniazdo na silniku).",
            "Błędne parametry silnika lub nieprawidłowe fazowanie (Phasing).",
            "Uszkodzenie modułu mocy IGBT wewnątrz serwonapędu ACOPOS.",
        ],
        "steps": [
            "Odłącz zasilanie maszyny i odczekaj min. 5 minut na rozładowanie szyny DC.",
            "Wypnij złącze X5 i zmierz miernikiem izolacji (500 V) izolację kabla i silnika względem PE oraz rezystancję między fazami U-V-W.",
            "Sprawdź stan styków wtyczek silnikowych (wilgoć, olej, przypalenia) oraz kabel w prowadniku.",
            "Zweryfikuj poprawność danych silnika; jeśli błąd pojawia się przy odpiętym silniku — wymień ACOPOS.",
        ],
        "hv": True,
        "related": [6052, 6053, 6054, 7222, 9300],
    },
    6021: {
        "category": "POWER_STAGE_BRAKE",
        "severity": "WARNING",
        "problem": "Wykryto stan niski (0 V) na sprzętowym wejściu zezwolenia pracy sterownika (Controller Enable / STO).",
        "reaction": "Natychmiastowe wyłączenie stopnia mocy i blokada załączania do powrotu sygnału 24 V.",
        "causes": [
            "Otwarty obwód bezpieczeństwa (E-STOP, drzwi serwisowe, kurtyna świetlna).",
            "Przerwany przewód lub luźna wtyczka wejścia Enable.",
            "Przekaźnik bezpieczeństwa lub PLC nie podaje sygnału 24 V DC.",
        ],
        "steps": [
            "Sprawdź osłony bezpieczeństwa, kurtyny i przyciski E-STOP.",
            "Zmierz napięcie 24 V DC na zaciskach wejścia Enable względem masy GND napędu.",
            "Zweryfikuj stan przekaźnika bezpieczeństwa / modułu SafePLC.",
            "Po przywróceniu stabilnego 24 V skasuj alarm i załącz regulator.",
        ],
        "hv": False,
        "related": [6023, 6058, 6059, 1011],
    },
    6028: {
        "category": "POWER_STAGE_BRAKE",
        "severity": "CRITICAL",
        "problem": "Wykryto zbyt niskie napięcie lub prąd w obwodzie hamulca trzymającego silnika (zerwanie przewodu lub brak 24 V).",
        "reaction": "Zablokowanie napędu i wyłączenie wyjścia sterującego hamulcem.",
        "causes": [
            "Przerwany przewód zasilania hamulca (często w ruchomym prowadniku kablowym robota Wittmann).",
            "Luźne lub cofnięte piny hamulca w złączu X5 albo wtyczce przy silniku.",
            "Spalona cewka hamulca trzymającego w silniku.",
            "Zbyt niskie napięcie zasilania 24 V DC (poniżej 22–23 V).",
        ],
        "steps": [
            "Odłącz zasilanie i sprawdź ciągłość żył hamulca (BR+/BR-) od X5 do silnika, poruszając prowadnikiem kablowym.",
            "Zmierz rezystancję cewki hamulca bezpośrednio na stykach silnika.",
            "Skontroluj dokładnie piny złącza X5 i gniazda przy silniku.",
            "Zmierz napięcie 24 V DC na wejściu zasilania hamulca w serwonapędzie ACOPOS.",
        ],
        "hv": True,
        "related": [6026, 6029, 6055, 6056, 6020],
    },
    7032: {
        "category": "ENCODER_FEEDBACK",
        "severity": "CRITICAL",
        "problem": "Zbyt mała amplituda sygnału przyrostowego enkodera — wykryto zanik połączenia lub silne zakłócenia.",
        "reaction": "Natychmiastowe odłączenie napędu i utrata referencji osi.",
        "causes": [
            "Odpięty, urwany lub złamany w prowadniku przewód enkodera.",
            "Brak zasilania enkodera lub uszkodzona karta interfejsu w slocie ACOPOS.",
            "Uszkodzenie wewnętrzne enkodera w silniku.",
        ],
        "steps": [
            "Sprawdź wtyczki enkodera przy silniku i na karcie w slocie ACOPOS (sprawdź Info: Slot).",
            "Przemierz ciągłość żył przewodu enkodera oraz ekranu 360° (szczególnie w prowadniku kablowym).",
            "Zmierz napięcie zasilania enkodera na złączu.",
            "Jeśli przewód jest sprawny, przetestuj na zapasowej karcie enkodera lub wymień enkoder/silnik.",
        ],
        "hv": False,
        "related": [7029, 7031, 7045, 7046, 7053],
    },
    7200: {
        "category": "POWER_DC_BUS",
        "severity": "CRITICAL",
        "problem": "Przepięcie na szynie pośredniczącej DC (DC Bus Overvoltage) — przekroczono bezpieczny limit napięcia (ok. 820–850 V DC).",
        "reaction": "Natychmiastowe odłączenie stopnia mocy w celu ochrony kondensatorów i IGBT.",
        "causes": [
            "Zbyt gwałtowne hamowanie ciężkiej osi powodujące silny zwrot energii.",
            "Uszkodzony, odłączony lub przegrzany rezystor hamowania (Bleeder).",
            "Zbyt wysokie napięcie sieci zasilającej 3×400 V AC lub uszkodzony tranzystor kluczujący Bleeder.",
        ],
        "steps": [
            "Odłącz zasilanie sieciowe, odczekaj min. 5 minut i upewnij się, że szyna DC rozładowała się poniżej 30 V.",
            "Sprawdź podłączenie i zmierz rezystancję rezystora hamowania (Bleeder).",
            "Zweryfikuj konfigurację rezystora (wewnętrzny / zewnętrzny) w parametrach napędu.",
            "Zmierz napięcie sieci zasilającej 3-fazowej oraz złagodź rampy hamowania osi.",
        ],
        "hv": True,
        "related": [7225, 7227, 9040, 38008, 64506],
    },
    7215: {
        "category": "POWER_DC_BUS",
        "severity": "CRITICAL",
        "problem": "Wykryto zanik co najmniej jednej fazy w trójfazowej sieci zasilającej serwonapęd ACOPOS.",
        "reaction": "Zatrzymanie napędu i blokada załączania stopnia mocy.",
        "causes": [
            "Przepalony bezpiecznik lub wyzwolony wyłącznik nadprądowy jednej z faz.",
            "Wypalony styk stycznika głównego albo poluzowany zacisk L1/L2/L3.",
        ],
        "steps": [
            "Zmierz wszystkie napięcia międzyfazowe L1-L2, L2-L3, L3-L1 bezpośrednio na zaciskach ACOPOS.",
            "Sprawdź bezpieczniki mocy, filtr sieciowy EMC oraz styki stycznika głównego.",
            "Po odłączeniu zasilania dokręć zaciski toru zasilania 3×400 V.",
        ],
        "hv": True,
        "related": [7221, 7211, 7212, 7214],
    },
    7222: {
        "category": "POWER_DC_BUS",
        "severity": "CRITICAL",
        "problem": "Wykryto prąd sumaryczny (zwarcie doziemne / upływ do PE) na złączu silnikowym X5.",
        "reaction": "Natychmiastowe odłączenie stopnia mocy IGBT i blokada napędu.",
        "causes": [
            "Przebicie izolacji lub zwarcie fazy U/V/W do PE / obudowy maszyny.",
            "Przetarty przewód silnikowy w prowadniku (kontakt żyły z ekranem).",
            "Wilgoć, olej lub chłodziwo we wtyczce silnikowej lub puszce silnika.",
        ],
        "steps": [
            "Odłącz zasilanie sieciowe i odczekaj min. 5 minut na całkowite rozładowanie szyny DC.",
            "Wypnij złącze X5 od serwonapędu ACOPOS.",
            "Zmierz miernikiem izolacji (500 V DC) rezystancję izolacji faz U-V-W względem PE i ekranu.",
            "Rozepnij złącze przy samym silniku, aby oddzielnie sprawdzić przewód w prowadniku i uzwojenia silnika.",
            "Osusz/oczyść złącza, wymień uszkodzony element i skasuj alarm.",
        ],
        "hv": True,
        "related": [6019, 6052, 6053, 6054, 7223],
    },
    9000: {
        "category": "THERMAL_OVERLOAD",
        "severity": "CRITICAL",
        "problem": "Czujnik temperatury radiatora ACOPOS przekroczył maksymalną temperaturę — ruch zatrzymany.",
        "reaction": "Zatrzymanie osi i wyłączenie stopnia mocy do czasu ostygnięcia (kasowanie po kodzie 36).",
        "causes": [
            "Zatkane filtry szafy sterowniczej lub zabrudzony radiator modułu ACOPOS.",
            "Uszkodzony wentylator serwonapędu albo niesprawny klimatyzator szafy.",
            "Ciągłe przeciążenie prądowe osi lub zbyt wysoka temperatura otoczenia.",
        ],
        "steps": [
            "Sprawdź obracanie się wentylatora ACOPOS oraz wentylatorów/klimatyzatora szafy sterowniczej.",
            "Wyczyść filtry szafy i przedmuchaj radiator sprężonym powietrzem przy wyłączonym zasilaniu.",
            "Poczekaj na schłodzenie radiatora (pojawienie się statusu 36) i skasuj błąd.",
            "Sprawdź prąd ciągły osi podczas pracy pod kątem oporów mechanicznych.",
        ],
        "hv": False,
        "related": [9001, 41001, 36, 9030, 9078],
    },
    9050: {
        "category": "THERMAL_OVERLOAD",
        "severity": "CRITICAL",
        "problem": "Przeciążenie prądem szczytowym ACOPOS (model I²t przekroczył 100%) — ruch zatrzymany.",
        "reaction": "Natychmiastowe zatrzymanie osi i wyłączenie stopnia mocy.",
        "causes": [
            "Zablokowanie mechaniczne, kolizja lub nagły wzrost oporów na osi.",
            "Niezwalniający się hamulec silnika zmuszający napęd do pracy na prądzie szczytowym.",
            "Zbyt dynamiczne przyspieszenia względem masy narzędzia.",
        ],
        "steps": [
            "Sprawdź mechanikę osi, prowadnice, przekładnię oraz pełne zwalnianie hamulca silnika.",
            "Przeanalizuj przebieg prądu w diagnostyce i znajdź moment przeciążenia w cyklu.",
            "Złagodź przyspieszenia/opóźnienia w programie ruchu, odczekaj regenerację modelu I²t i skasuj alarm.",
        ],
        "hv": False,
        "related": [9051, 9060, 9075, 41051, 4007],
    },
}


def resolve_assets_dir() -> Path:
    """Lokalizuje folder z plikami list błędów (zarówno w wersji portable EXE, jak i w repozytorium)."""
    candidates: List[Path] = []
    if getattr(sys, "frozen", False) and hasattr(sys, "_MEIPASS"):
        meipass = Path(sys._MEIPASS)
        candidates.append(meipass / "assets")
        candidates.append(meipass)
    script_dir = Path(__file__).resolve().parent
    candidates.extend([
        script_dir / "assets",
        script_dir.parent / "public" / "assets" / "aistudio",
        Path.cwd() / "public" / "assets" / "aistudio",
        Path.cwd() / "assets",
    ])
    for path in candidates:
        if (path / PL_ASSET_FILE).is_file() or (path / EN_ASSET_FILE).is_file():
            return path
    return candidates[0]


def normalize_for_search(text: str) -> str:
    lowered = (
        text.lower()
        .replace("ł", "l")
        .replace("ą", "a")
        .replace("ć", "c")
        .replace("ę", "e")
        .replace("ń", "n")
        .replace("ó", "o")
        .replace("ś", "s")
        .replace("ź", "z")
        .replace("ż", "z")
    )
    decomposed = unicodedata.normalize("NFD", lowered)
    return "".join(ch for ch in decomposed if unicodedata.category(ch) != "Mn")


def polish_technical_title(raw: str) -> str:
    text = re.sub(r"\s+", " ", raw).strip()
    for src, dst in TITLE_REPLACEMENTS:
        if src in text:
            text = text.replace(src, dst)
    return text[0].upper() + text[1:] if text else text


def translate_fallback_en(en: str) -> str:
    return (
        en.replace("Controller", "Sterownik")
        .replace("Drive", "Napęd")
        .replace("Encoder", "Enkoder")
        .replace("Parameter", "Parametr")
        .replace("Error", "Błąd")
        .replace("Timeout", "Przekroczenie limitu czasu")
        .replace("not possible", "niemożliwe")
        .replace("is invalid", "jest nieprawidłowy")
    )


def parse_polish_text(content: str) -> Dict[int, str]:
    result: Dict[int, str] = {}
    for raw_line in content.splitlines():
        line = raw_line.strip().lstrip("\ufeff")
        m = PL_LINE_RE.match(line)
        if not m:
            continue
        code = int(m.group(1).replace(".", ""))
        title = m.group(2).strip()
        if title:
            result[code] = polish_technical_title(title)
    return result


def parse_english_text(content: str) -> Dict[int, Tuple[str, Optional[str]]]:
    result: Dict[int, Tuple[str, Optional[str]]] = {}
    last_code: Optional[int] = None
    for raw_line in content.splitlines():
        trimmed = raw_line.lstrip("\ufeff").strip()
        if not trimmed:
            continue
        im = INFO_LINE_RE.match(trimmed)
        if im and last_code is not None and last_code in result:
            title_en, prev_info = result[last_code]
            info_str = f"Info({im.group(1).strip()}): {im.group(2).strip()}"
            combined = info_str if prev_info is None else f"{prev_info}\n{info_str}"
            result[last_code] = (title_en, combined)
            continue
        cm = EN_LINE_RE.match(trimmed)
        if cm:
            code = int(cm.group(1))
            result[code] = (cm.group(2).strip(), None)
            last_code = code
    return result


def explain_info_detail(info_detail: Optional[str]) -> Optional[str]:
    if not info_detail:
        return None
    low = info_detail.lower()
    if "parid" in low:
        return "Sterownik zwraca numer parametru (ParID), którego dotyczy błąd."
    if "slot" in low:
        return "Numer gniazda (Slot 1–4) karty rozszerzeń ACOPOS, w której wykryto błąd."
    if "plcopen_fb" in low:
        return "Identyfikator bloku funkcyjnego PLCopen MC, który zgłosił błąd."
    if "temperature" in low:
        return "Zmierzona temperatura lub przekroczony próg temperaturowy [°C]."
    if "load limit" in low or "current load" in low:
        return "Aktualny poziom obciążenia lub przekroczony limit prądu/mocy."
    if "phase" in low:
        return "Numer fazy silnika (U/V/W) na złączu X5."
    if "timeout" in low:
        return "Dokładny czas przekroczenia limitu oczekiwania (Timeout)."
    if "can-id" in low or "can_" in low:
        return "Identyfikator ramki CAN (COB-ID) i status funkcji sterownika CAN."
    if "station" in low or "node" in low:
        return "Numer stacji lub węzła sieciowego ACOPOS."
    if "voltage" in low:
        return "Zmierzone napięcie szyny DC lub wymagany próg napięcia [V]."
    if "lag error" in low or "difference" in low:
        return "Wartość uchybu pozycji w chwili wyzwolenia alarmu."
    return "Dodatkowa wartość diagnostyczna zwracana przez ACOPOS w Loggerze."


def classify_category(code: int, title_pl: str, title_en: Optional[str]) -> str:
    combined = f"{title_pl} {title_en or ''}".lower()
    if 7200 <= code <= 7229:
        return "POWER_DC_BUS"
    if (9000 <= code <= 9399) or (41000 <= code <= 41099) or code == 36:
        return "THERMAL_OVERLOAD"
    if (7000 <= code <= 7099) or (39000 <= code <= 39099) or (31220 <= code <= 31224):
        return "ENCODER_FEEDBACK"
    if 35000 <= code <= 35299:
        return "SAFETY_SAFEMC"
    if code in (6015, 6016) or (
        1004 <= code <= 1024
        and any(k in combined for k in ("sieci", "network", "can", "powerlink", "cyklicz"))
    ):
        return "NETWORK_COMM"
    if (6000 <= code <= 6099) or (38000 <= code <= 38099) or (39300 <= code <= 39399):
        if any(k in combined for k in ("sync", "can ", "network")):
            return "NETWORK_COMM"
        return "POWER_STAGE_BRAKE"
    if (5100 <= code <= 5399) or (37100 <= code <= 37199):
        return "CAM_SYNC"
    if (4000 <= code <= 5099) or (64500 <= code <= 64599):
        return "MOTION_HOMING"
    if (29200 <= code <= 29599) or (31200 <= code <= 32799) or (64000 <= code <= 64099):
        if any(
            k in combined
            for k in ("can", "powerlink", "sieć", "sieci", "network", "broadcast", "cob", "acyc", "node", "węz", "telegram")
        ):
            return "NETWORK_COMM"
        return "PLCOPEN_NC"
    return "PARAMETERS_MEMORY"


def classify_severity(code: int, title_pl: str, title_en: Optional[str], category_id: str) -> str:
    text = f"{title_pl} {title_en or ''}".lower()
    if code in (36, 40000) or (64500 <= code <= 64599):
        return "INFO"
    if (36000 <= code <= 36099) or (37100 <= code <= 37199) or (41000 <= code <= 41099) or (64000 <= code <= 64099):
        return "WARNING"
    if any(k in text for k in ("warning", "ostrzeg", "limiter active", "aktywny ogranicznik", "opóźnienie", "delay")):
        return "WARNING"
    if (
        (33002 <= code <= 33009)
        or (35000 <= code <= 35255)
        or category_id == "POWER_DC_BUS"
        or any(
            k in text
            for k in (
                "overcurrent", "nadprąd", "przetęż", "overvoltage", "przepięc",
                "ground fault", "doziemn", "overtemperature", "przegrzani",
                "watchdog", "power supply fail", "awaria zasilania", "fail safe",
                "fatal", "zwarcie", "short circuit", "movement stopped", "ruch zatrzymany",
            )
        )
    ):
        return "CRITICAL"
    return "ERROR"


def requires_hv_warning(code: int, title_pl: str, title_en: Optional[str], category_id: str) -> bool:
    if category_id == "POWER_DC_BUS":
        return True
    text = f"{title_pl} {title_en or ''}".lower()
    return code in {6018, 6019, 6026, 6028, 6045, 6049, 6052, 6053, 6054, 9300, 38008} or any(
        k in text for k in ("x5", "szyna dc", "dc bus", "bleeder", "stopień mocy", "power stage", "fazy")
    )


def synthesize_details(code: int, title_pl: str, title_en: Optional[str], category_id: str, severity: str) -> Tuple[str, str, List[str], List[str]]:
    text = f"{title_pl} {title_en or ''}".lower()
    if 64500 <= code <= 64506:
        return (
            f"Informacja diagnostyczna: {title_pl}. Napęd automatycznie ogranicza zadaną wielkość, aby nie przekroczyć limitów bezpieczeństwa i możliwości sprzętu.",
            "Oś kontynuuje pracę z ograniczoną dynamiką. Długotrwałe ograniczenie może skutkować błędem uchybu nadążnego (4007).",
            [
                "Zbyt agresywnie zadany profil prędkości, przyspieszenia lub hamowania.",
                "Zwiększone opory mechaniczne lub większa masa robocza osi.",
                "Zbyt nisko ustawione limity momentu/prędkości w konfiguracji osi.",
            ],
            [
                "Sprawdź, czy komunikat występuje sporadycznie czy ciągle w trakcie cyklu.",
                "Skontroluj mechanikę osi pod kątem nadmiernych oporów i smarowania.",
                "Porównaj zadane przyspieszenia/opóźnienia z parametrami AXLIM.",
                "Dla kodu 64506 sprawdź stan rezystora hamowania (Bleeder) i wydłuż czas hamowania.",
            ],
        )
    if (41000 <= code <= 41099) or (9000 <= code <= 9090 and ("ogranicznik" in text or "limiter" in text)):
        return (
            f"Ostrzeżenie termiczne lub przeciążeniowe: {title_pl}. Układ zbliża się do granicy termicznej i wymaga kontroli chłodzenia.",
            "Aktywacja ostrzeżenia lub ograniczenia prądu/mocy przez serwonapęd przed wyłączeniem awaryjnym.",
            [
                "Zabrudzone filtry szafy sterowniczej lub słabe chłodzenie silnika.",
                "Zbyt intensywny cykl pracy maszyny lub rosnące opory mechaniczne.",
            ],
            [
                "Sprawdź w diagnostyce aktualną temperaturę oraz procent obciążenia termicznego.",
                "Skontroluj drożność filtrów, wentylatory ACOPOS i temperaturę wewnątrz szafy.",
                "Sprawdź mechanikę osi oraz złagodź dynamikę ruchu przed wystąpieniem błędu krytycznego.",
            ],
        )
    if category_id == "THERMAL_OVERLOAD":
        return (
            f"Alarm układu termicznego lub prądowego: {title_pl}. Wykryto przekroczenie dopuszczalnej temperatury, przeciążenie modelu cieplnego lub usterkę czujnika.",
            "Zatrzymanie ruchu osi i wyłączenie stopnia mocy dla ochrony półprzewodników oraz uzwojeń silnika.",
            [
                "Przegrzanie radiatora, stopnia mocy, silnika lub rezystora hamowania.",
                "Uszkodzenie, przerwa lub zwarcie w przewodzie czujnika temperatury.",
                "Zatarcie mechaniczne zwiększające pobór prądu.",
            ],
            [
                "Odczekaj na ostygnięcie podzespołów i zweryfikuj odczyt temperatury w diagnostyce.",
                "Sprawdź wentylatory ACOPOS, czystość radiatorów oraz filtry/klimatyzację szafy.",
                "Przy błędach czujnika zmierz jego rezystancję i ciągłość przewodu.",
                "Skontroluj mechanikę osi i poprawność parametrów znamionowych silnika.",
            ],
        )
    if category_id == "POWER_DC_BUS":
        return (
            f"Usterka obwodu zasilania mocy lub szyny DC: {title_pl}. Napięcie lub prąd wyszły poza bezpieczny zakres roboczy.",
            "Natychmiastowe odłączenie stopnia mocy i blokada ponownego załączenia.",
            [
                "Niestabilne zasilanie 3×400 V AC, zanik fazy lub zbyt częste załączanie stycznika głównego.",
                "Uszkodzenie lub przeciążenie rezystora ładowania / hamowania (Bleeder).",
                "Poluzowane zaciski zasilania, złącze Backplane lub zwarcie doziemne.",
            ],
            [
                "Zmierz napięcia L1-L2-L3 na wejściu zasilania napędu i sprawdź bezpieczniki oraz stycznik główny.",
                "Po odłączeniu zasilania i rozładowaniu szyny DC dokręć zaciski mocy oraz sprawdź rezystor hamowania.",
                "Skontroluj poprawność osadzenia modułu na płycie bazowej oraz stabilność 24 V DC.",
                "Zmierz izolację przewodów silnikowych i rezystora hamowania względem PE.",
            ],
        )
    if category_id == "ENCODER_FEEDBACK":
        return (
            f"Błąd układu pomiarowego pozycji (enkoder / resolwer): {title_pl}. Serwonapęd utracił poprawny odczyt położenia lub komunikację z enkoderem.",
            "Zgłoszenie ostrzeżenia (dla bitów ostrzegawczych) lub natychmiastowe zatrzymanie osi i utrata referencji.",
            [
                "Uszkodzony lub złamany w prowadniku przewód enkodera.",
                "Słaby styk we wtyczce lub zakłócenia EMC (uszkodzone ekranowanie 360°).",
                "Rozładowana bateria podtrzymująca (enkodery absolutne) lub uszkodzenie enkodera/karty.",
            ],
            [
                "Odczytaj numer gniazda (Info: Slot) i skontroluj złącza enkodera na karcie ACOPOS oraz przy silniku.",
                "Przemierz ciągłość żył przewodu, ekranu oraz napięcie zasilania enkodera.",
                "Dla alarmów baterii EnDat wymień baterię buforową zgodnie z procedurą zachowania bazy.",
                "Wykonaj restart zasilania (Power OFF/ON); przy powtarzaniu błędu podmień kabel lub kartę/enkoder.",
            ],
        )
    if category_id == "SAFETY_SAFEMC":
        return (
            f"Alarm modułu bezpieczeństwa SafeMC (Safe Motion Control): {title_pl}. Wykryto naruszenie funkcji bezpiecznego ruchu, niezgodność enkodera bezpiecznego lub błąd parametryzacji.",
            "Natychmiastowe przejście napędu w stan bezpieczny (STO / Fail Safe) i blokada ruchu.",
            [
                "Przekroczenie bezpiecznego limitu prędkości (SLS/SMS), pozycji (SLP/SMP), kierunku (SDI) lub rampy.",
                "Wymiana silnika/enkodera bez zatwierdzenia danych konfiguracyjnych w SafePLC.",
                "Niespójna parametryzacja bezpieczeństwa lub błąd wewnętrzny karty SafeMC.",
            ],
            [
                "Sprawdź aktywną funkcję bezpieczeństwa (SLS, SOS, SS2, SDI, SLP/SMP) w momencie alarmu.",
                "Porównaj prędkości/pozycje programu PLC z progami bezpieczeństwa SafeMC.",
                "Przy niezgodności enkodera po wymianie silnika potwierdź konfigurację przez procedurę SafePLC.",
                "Sprawdź połączenie enkodera bezpiecznego i wykonaj wymagane bezpieczne bazowanie / restart 24 V.",
            ],
        )
    if category_id == "POWER_STAGE_BRAKE":
        return (
            f"Alarm stopnia mocy, hamulca trzymającego, fazowania lub wejść/wyjść: {title_pl}.",
            "Zatrzymanie pracy osi i wyłączenie stopnia mocy do czasu usunięcia usterki.",
            [
                "Usterka obwodu hamulca trzymającego lub niewłaściwa konfiguracja hamulca w parametrach silnika.",
                "Spadek napięcia 24 V DC na wejściach Enable1/Enable2 lub karcie Digital IO.",
                "Nieudane fazowanie (zablokowany wirnik lub ruch podczas startu) albo przeciążenie wyjść IO.",
            ],
            [
                "Zmierz napięcie 24 V DC na zaciskach zasilania, wejściach Enable oraz karcie Digital IO.",
                "Dla alarmów hamulca sprawdź przewody na X5, rezystancję cewki i brak napięcia obcego.",
                "Dla błędów fazowania upewnij się, że wirnik ma swobodę ruchu i hamulec jest zwolniony.",
                "Dla alarmów Digital IO sprawdź wskazany kanał pod kątem zwarcia lub przegrzania.",
            ],
        )
    if category_id == "MOTION_HOMING":
        return (
            f"Błąd sterowania ruchem, limitów osi lub procedury bazowania: {title_pl}.",
            "Przerwanie bieżącego ruchu / bazowania lub odrzucenie komendy startu.",
            [
                "Pozycja docelowa lub droga hamowania przekracza limit programowy SW albo krańcówkę sprzętową.",
                "Niewłaściwy stan osi (wyłączony regulator, aktywna rampa zatrzymania lub inny ruch).",
                "Nieprawidłowe parametry bazowania, brak sygnału czujnika referencyjnego lub zbyt wysokie Override.",
            ],
            [
                "Sprawdź aktualny stan osi (regulator ON, brak aktywnego ruchu/bazowania).",
                "Zweryfikuj pozycję docelową oraz limity programowe SW i krańcówki sprzętowe.",
                "Dla błędów bazowania sprawdź czujnik referencyjny, tryb bazowania oraz typ enkodera.",
                "Skasuj alarm, wycofaj oś ze strefy granicznej (jeśli dotyczy) i ponów operację.",
            ],
        )
    if category_id == "CAM_SYNC":
        return (
            f"Błąd sprzężenia krzywkowego (Cam Profile / Cam Automat) lub synchronizacji Master–Slave: {title_pl}.",
            "Przerwanie lub zablokowanie sprzężenia osi Slave z osią Master.",
            [
                "Nieprawidłowa definicja profilu krzywki lub brak tabeli pod wskazanym indeksem.",
                "Próba zapisu parametrów przy aktywnym automacie krzywkowym.",
                "Zbyt częste lub brakujące impulsy wyzwalacza (Trigger FIFO / okno wyzwalacza).",
            ],
            [
                "Zweryfikuj poprawność tabeli profilu krzywki (indeks, punkty, wielomiany).",
                "Dezaktywuj automat krzywkowy przed modyfikacją parametrów lub pobraniem nowej krzywki.",
                "Sprawdź czujnik wyzwalający (Trigger), okno pomiarowe oraz poprawność osi Master.",
            ],
        )
    if category_id == "NETWORK_COMM":
        return (
            f"Błąd komunikacji sieciowej lub synchronizacji magistrali (POWERLINK / CAN): {title_pl}.",
            "Utrata wymiany danych cyklicznych/acyklicznych lub blokada inicjalizacji sieci NC.",
            [
                "Uszkodzony lub odpięty kabel sieciowy, brak terminacji CAN albo zakłócenia EMC.",
                "Błędny lub zdublowany numer węzła (Node ID) w acp10cfg / na przełącznikach ACOPOS.",
                "Niezgodne czasy cykli sieci POWERLINK i klasy zadań NC.",
            ],
            [
                "Sprawdź diody LINK/RUN/ERR, wtyczki sieciowe oraz zasilanie wszystkich modułów w sieci.",
                "Zweryfikuj przełączniki numeru węzła (Node ID) względem konfiguracji acp10cfg.",
                "Sprawdź dodatkowe wpisy w Loggerze PLC oraz zgodność czasów cykli sieci i NC.",
                "Wykonaj restart zasilania PLC i napędów oraz zweryfikuj nawiązanie komunikacji.",
            ],
        )
    if category_id == "PLCOPEN_NC":
        return (
            f"Błąd warstwy sterowania ruchem PLCopen / Menedżera NC: {title_pl}.",
            "Odrzucenie wywołania bloku funkcyjnego lub przerwanie inicjalizacji / strojenia / śledzenia.",
            [
                "Wywołanie bloku MC_* w niewłaściwym stanie osi lub z błędnymi parametrami wejściowymi.",
                "Brakujący lub niezgodny wersją moduł NC (acp10cfg, acp10sys, acp10bsl, INIT).",
                "Błąd wtórny wywołany alarmem sprzętowym napędu ACOPOS (sprawdź MC_ReadAxisError).",
            ],
            [
                "Sprawdź pełne wpisy w Loggerze PLC (System Diagnostics Manager / Automation Studio).",
                "Odczytaj właściwy błąd napędu funkcją MC_ReadAxisError i zweryfikuj stan osi.",
                "Sprawdź zgodność wersji firmware (acp10sys / BsLoader) oraz modułów na karcie PLC.",
                "Dla kodu 32399 wykonaj ręczny restart zasilania (POWER OFF/ON) napędu ACOPOS.",
            ],
        )
    return (
        f"Błąd parametru, pamięci EEPROM/EPROM lub stanu systemowego ACOPOS: {title_pl}.",
        "Odrzucenie operacji zapisu/odczytu parametru lub wstrzymanie inicjalizacji modułu.",
        [
            "Wartość parametru poza zakresem min/max lub błędny identyfikator ParID.",
            "Próba zapisu parametru podczas aktywnego ruchu lub włączonego regulatora.",
            "Uszkodzenie danych EEPROM karty rozszerzeń lub niezgodna wersja firmware ACOPOS.",
        ],
        [
            "Sprawdź identyfikator parametru (Info: ParID) oraz jego dopuszczalne granice.",
            "Zatrzymaj oś i wyłącz regulator przed zapisem parametrów konfiguracyjnych.",
            "Dla błędów EEPROM wyłącz zasilanie i ponownie osadź karty rozszerzeń (Plug-in).",
            "Zweryfikuj zgodność wersji systemu operacyjnego ACOPOS z projektem Automation Studio.",
        ],
    )


def compute_related_codes(code: int, all_codes: Set[int]) -> List[int]:
    rel: List[int] = []
    for delta in (-2, -1, 1, 2, 3, -3, 10, -10):
        cand = code + delta
        if cand in all_codes and cand != code and cand not in rel:
            rel.append(cand)
        if len(rel) >= 4:
            break
    if 41001 <= code <= 41089:
        pair = 9000 + (code - 41000)
        for p in (pair, pair - 1):
            if p in all_codes and p not in rel:
                rel.append(p)
    elif 9000 <= code <= 9089:
        pair = 41000 + (code - 9000)
        for p in (pair, pair + 1):
            if p in all_codes and p not in rel:
                rel.append(p)
    return rel[:5]


def load_catalog(assets_dir: Optional[Path] = None) -> List[AcoposError]:
    directory = assets_dir or resolve_assets_dir()
    pl_path = directory / PL_ASSET_FILE
    en_path = directory / EN_ASSET_FILE

    pl_text = pl_path.read_text(encoding="utf-8", errors="replace") if pl_path.is_file() else ""
    en_text = en_path.read_text(encoding="utf-8", errors="replace") if en_path.is_file() else ""

    pl_map = parse_polish_text(pl_text)
    en_map = parse_english_text(en_text)
    all_codes = sorted(set(pl_map.keys()) | set(en_map.keys()))
    all_code_set = set(all_codes)

    catalog: List[AcoposError] = []
    for code in all_codes:
        en_pair = en_map.get(code)
        title_en = en_pair[0] if en_pair else None
        info_detail = en_pair[1] if en_pair else None
        title_pl = (
            pl_map.get(code)
            or EN_ONLY_PL_TRANSLATIONS.get(code)
            or translate_fallback_en(title_en or "Nieznany błąd ACOPOS")
        )
        info_exp = explain_info_detail(info_detail)

        curated = CURATED.get(code)
        if curated:
            catalog.append(
                AcoposError(
                    code=code,
                    title_pl=title_pl,
                    title_en=title_en,
                    info_detail=info_detail,
                    info_explanation_pl=info_exp,
                    category_id=curated["category"],
                    severity=curated["severity"],
                    problem_description=curated["problem"],
                    drive_reaction=curated["reaction"],
                    possible_causes=curated["causes"],
                    repair_steps=curated["steps"],
                    requires_hv_caution=curated["hv"],
                    related_codes=[c for c in curated["related"] if c in all_code_set and c != code],
                )
            )
            continue

        cat_id = classify_category(code, title_pl, title_en)
        sev = classify_severity(code, title_pl, title_en, cat_id)
        hv = requires_hv_warning(code, title_pl, title_en, cat_id)
        prob, react, causes, steps = synthesize_details(code, title_pl, title_en, cat_id, sev)
        related = compute_related_codes(code, all_code_set)
        catalog.append(
            AcoposError(
                code=code,
                title_pl=title_pl,
                title_en=title_en,
                info_detail=info_detail,
                info_explanation_pl=info_exp,
                category_id=cat_id,
                severity=sev,
                problem_description=prob,
                drive_reaction=react,
                possible_causes=causes,
                repair_steps=steps,
                requires_hv_caution=hv,
                related_codes=related,
            )
        )
    return catalog


def search_catalog(
    catalog: List[AcoposError],
    query: str,
    category_id: Optional[str] = None,
    severity: Optional[str] = None,
) -> List[AcoposError]:
    trimmed = query.strip()
    digits = trimmed.replace(".", "").replace(" ", "")
    is_numeric = bool(digits) and digits.isdigit()
    norm_q = normalize_for_search(trimmed)
    tokens = [t for t in norm_q.split() if t]

    scored: List[Tuple[int, AcoposError]] = []
    for err in catalog:
        if category_id and err.category_id != category_id:
            continue
        if severity and err.severity != severity:
            continue
        if not trimmed:
            scored.append((1, err))
            continue
        code_str = str(err.code)
        score = 0
        if is_numeric:
            if code_str == digits:
                score = 1000
            elif code_str.startswith(digits):
                score = 700 - len(code_str)
            elif digits in code_str:
                score = 400
        if score == 0 and tokens:
            hay = normalize_for_search(
                f"{err.code} {err.title_pl} {err.title_en or ''} {err.info_detail or ''} {err.category_title} {err.problem_description}"
            )
            if all(t in hay for t in tokens):
                title_norm = normalize_for_search(err.title_pl)
                boost = 250 if all(t in title_norm for t in tokens) else 0
                phrase_boost = 150 if norm_q in title_norm else 0
                score = 200 + boost + phrase_boost
        if score > 0:
            scored.append((score, err))

    scored.sort(key=lambda pair: (-pair[0], pair[1].code))
    return [pair[1] for pair in scored]


def build_diagnostic_report(err: AcoposError, checked_indices: Set[int]) -> str:
    lines = [
        "=== RAPORT DIAGNOSTYCZNY ACOPOS ===",
        f"Kod alarmu: {err.code} [{err.severity_label.upper()}]",
        f"Kategoria: {err.category_title}",
        f"Komunikat (PL): {err.title_pl}",
    ]
    if err.title_en:
        lines.append(f"Komunikat (EN): {err.title_en}")
    if err.info_detail:
        lines.append(f"Parametr: {err.info_detail}")
    lines.extend([
        "",
        "Opis problemu:",
        err.problem_description,
        "",
        "Reakcja napędu:",
        err.drive_reaction,
        "",
        "Sugerowane kroki naprawcze:",
    ])
    for idx, step in enumerate(err.repair_steps):
        mark = "[x]" if idx in checked_indices else "[ ]"
        lines.append(f"{mark} {idx + 1}. {step}")
    return "\n".join(lines)
