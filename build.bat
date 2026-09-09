@echo off
setlocal

echo ========================================
echo Building Guess Market - Exercise 2
echo ========================================

REM Look for JDK 17 or JDK 21 if default javac is Java 8
set JAVAC_CMD=javac
if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\javac.exe" (
    set "JAVAC_CMD=C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\javac.exe"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot\bin\javac.exe" (
    set "JAVAC_CMD=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot\bin\javac.exe"
)

REM Ensure out directories exist
if not exist "out" mkdir out
if not exist "out\ui" mkdir out\ui

echo Compiling Engine and UI-JFX...
"%JAVAC_CMD%" -d out -cp "lib/*;out" Engine/src/models/*.java Engine/src/lmsr/*.java Engine/src/orderbook/*.java Engine/src/xml/*.java Engine/src/engine/*.java UI-JFX/src/ui/*.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)

echo Copying CSS stylesheets to out\ui...
copy /Y "UI-JFX\src\ui\*.css" "out\ui\" >nul

echo Packaging into GuessMarket.jar...
jar cfe GuessMarket.jar ui.MainLauncher -C out .
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Jar creation failed!
    exit /b %ERRORLEVEL%
)

echo ========================================
echo Build Successful! Created GuessMarket.jar
echo ========================================
