@echo off
title BlackBox Clean Build
color 0A

echo.
echo  ============================================
echo   BlackBox ^| Clean Build
echo  ============================================
echo.

cd /d "%~dp0"

echo [1/3] Cleaning previous build...
call gradlew.bat clean
if %errorlevel% neq 0 (
    echo.
    echo  [ERROR] Clean failed! Check Gradle output above.
    pause
    exit /b 1
)

echo.
echo [2/3] Building debug APK...
call gradlew.bat assembleDebug
if %errorlevel% neq 0 (
    echo.
    echo  [ERROR] Build failed! Check Gradle output above.
    pause
    exit /b 1
)

echo.
echo  ============================================
echo   Build SUCCESS
echo  ============================================
echo.

:: Find and print the output APK path
for /r "." %%f in (*-debug.apk) do (
    echo   APK: %%f
)

echo.
pause
