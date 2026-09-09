@echo off
setlocal

REM Find modern Java runtime (Java 17 / 21) or fallback to java
set JAVA_CMD=java
if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\java.exe"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot\bin\java.exe"
)

REM If GuessMarket.jar does not exist, build it first
if not exist "GuessMarket.jar" (
    echo GuessMarket.jar not found. Building first...
    call build.bat
)

echo Starting Guess Market UI...
"%JAVA_CMD%" --module-path lib --add-modules javafx.controls,javafx.fxml -cp "GuessMarket.jar;lib/*" ui.MainLauncher %*
