@echo off
echo Building APK...
echo.
echo Убедитесь, что Android SDK установлен и local.properties настроен правильно.
echo.

REM Установка переменных окружения для Android SDK
set ANDROID_HOME=C:\Users\Nep\AppData\Local\Android\Sdk
set ANDROID_SDK_ROOT=C:\Users\Nep\AppData\Local\Android\Sdk

REM Попытка использовать gradlew.bat
if exist gradlew.bat (
    call gradlew.bat assembleRelease
) else (
    echo gradlew.bat не найден. Используйте Android Studio для сборки:
    echo 1. Откройте проект в Android Studio
    echo 2. Build -^> Build Bundle(s) / APK(s) -^> Build APK(s)
    echo 3. Или Build -^> Generate Signed Bundle / APK
    pause
    exit /b 1
)

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo APK успешно собран!
    echo ========================================
    echo Файл находится в: app\build\outputs\apk\release\app-release.apk
    echo.
) else (
    echo.
    echo Ошибка при сборке APK
    echo Попробуйте собрать через Android Studio:
    echo Build -^> Build Bundle(s) / APK(s) -^> Build APK(s)
    echo.
)
pause
