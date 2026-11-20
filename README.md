# ACOPOS Diagnosta

Aplikacja diagnostyczna offline w języku polskim do wyszukiwania kodów alarmów oraz sugerowanych kroków naprawczych dla serwonapędów **B&R ACOPOS / Wittmann** — dostępna jako aplikacja **Android (.apk)** oraz wersja **Windows Portable (.exe)**.

## Funkcje
- Wyszukiwanie po numerze błędu (np. `4007`, `6028`, `7200`, `7222`, `9000`) lub po objawie tekstowym (np. `hamulec`, `enkoder`, `szyna DC`, `przegrzanie`).
- Pełna baza ponad 750 kodów błędów z opisem problemu, reakcją napędu, najczęstszymi przyczynami, parametrem `Info(...)` oraz ostrzeżeniami BHP dla obwodów wysokiego napięcia.
- Interaktywna lista sugerowanych kroków naprawczych z możliwością odhaczania sprawdzonych punktów.
- Kopiowanie gotowego raportu diagnostycznego do schowka, zakładki serwisowe oraz motyw ciemny/jasny.

## Automatyczne budowanie na GitHub (GitHub Actions)

Repozytorium zawiera gotowy plik workflow `.github/workflows/build-release.yml`, który automatycznie buduje obie wersje:
1. **Android APK** (`ACOPOS-Diagnosta-Android-debug.apk` oraz opcjonalnie podpisany `release.apk`).
2. **Windows Portable EXE** (`ACOPOS-Diagnosta-Portable.exe` oraz archiwum `ACOPOS-Diagnosta-Windows-Portable.zip`) — pojedynczy plik wykonywalny niewymagający instalacji.

### Jak pobrać gotowe pliki z GitHuba?
- **Z zakładki Actions:** Po wypchnięciu kodu na gałąź `main`/`master` (lub ręcznym kliknięciu **Run workflow** w zakładce **Actions → Build Android APK & Windows Portable EXE**) pobierz gotowe paczki w sekcji **Artifacts**.
- **Automatyczny GitHub Release:** Utwórz i wypchnij tag wersji, np.:
  ```bash
  git tag v1.0.0
  git push origin v1.0.0
  ```
  GitHub Actions automatycznie utworzy wydanie (**Releases**) z załączonymi plikami `.apk`, `.exe` i `.zip`.

### Opcjonalne podpisywanie wersji Android Release
Aby GitHub budował także podpisany plik `ACOPOS-Diagnosta-Android-release.apk`, dodaj w **Settings → Secrets and variables → Actions** trzy sekrety:
- `KEYSTORE_BASE64` — zakodowany w Base64 plik klucza `.jks`,
- `STORE_PASSWORD` — hasło do keystore,
- `KEY_PASSWORD` — hasło do klucza (`alias: upload`).

## Budowanie lokalne

### Windows Portable (`.exe`)
Na komputerze z systemem Windows (Python 3.11+) uruchom dwuklikiem:
```bat
desktop-windows\build_windows_portable.bat
```
Gotowy plik przenośny powstanie w `desktop-windows\dist\ACOPOS-Diagnosta-Portable.exe`. Zakładki i historia zapisują się obok pliku `.exe` w `acopos_data.json`.

### Android (`.apk`)
```bash
gradle :app:assembleDebug
```
Plik APK powstanie w `app/build/outputs/apk/debug/app-debug.apk`.
