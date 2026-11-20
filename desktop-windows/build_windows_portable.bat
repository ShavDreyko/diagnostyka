@echo off
setlocal
echo ====================================================
echo  ACOPOS Diagnosta - Budowanie wersji Windows Portable
echo ====================================================

cd /d "%~dp0\.."

python --version >nul 2>&1
if errorlevel 1 (
    echo BLAD: Nie znaleziono Python 3 w PATH. Zainstaluj Python 3.11+ ze strony python.org.
    pause
    exit /b 1
)

python -m pip install --upgrade pip pyinstaller
if errorlevel 1 (
    echo BLAD: Nie udalo sie zainstalowac PyInstaller.
    pause
    exit /b 1
)

python -m unittest desktop-windows/test_acopos_desktop.py
if errorlevel 1 (
    echo BLAD: Testy silnika diagnostycznego nie powiodly sie.
    pause
    exit /b 1
)

python -m PyInstaller ^
  --noconfirm ^
  --clean ^
  --onefile ^
  --windowed ^
  --name "ACOPOS-Diagnosta-Portable" ^
  --icon "desktop-windows/acopos_icon.ico" ^
  --add-data "public/assets/aistudio;assets" ^
  --add-data "desktop-windows/acopos_icon.ico;." ^
  --distpath "desktop-windows/dist" ^
  --workpath "desktop-windows/build" ^
  --specpath "desktop-windows" ^
  "desktop-windows/acopos_diagnosta.py"

if errorlevel 1 (
    echo BLAD podczas budowania pliku EXE.
    pause
    exit /b 1
)

echo.
echo SUKCES! Plik portable znajduje sie w:
echo   desktop-windows\dist\ACOPOS-Diagnosta-Portable.exe
pause
