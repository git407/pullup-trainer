# Инструкция по сборке APK

## Способ 1: Через Android Studio (Рекомендуется)

1. **Откройте проект в Android Studio:**
   - Запустите Android Studio
   - File → Open → выберите папку `C:\www\home\pullup`
   - Дождитесь синхронизации проекта (Gradle sync)

2. **Соберите APK:**
   - В меню выберите: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
   - Или: **Build → Generate Signed Bundle / APK → APK** (для подписанного APK)
   - Дождитесь завершения сборки

3. **Найдите APK файл:**
   - После сборки появится уведомление с кнопкой "locate"
   - Или найдите файл вручную: `app\build\outputs\apk\debug\app-debug.apk` (debug версия)
   - Или: `app\build\outputs\apk\release\app-release.apk` (release версия)

## Способ 2: Через командную строку

1. **Откройте терминал в папке проекта:**
   ```powershell
   cd C:\www\home\pullup
   ```

2. **Запустите сборку:**
   ```powershell
   .\gradlew.bat assembleRelease
   ```
   
   Или используйте скрипт:
   ```powershell
   .\build-apk.bat
   ```

3. **Найдите APK:**
   - Файл будет в: `app\build\outputs\apk\release\app-release.apk`

## Установка APK на устройство

1. Скопируйте APK файл на Android устройство
2. На устройстве откройте файловый менеджер и найдите APK файл
3. Нажмите на файл для установки
4. Если появится предупреждение о неизвестных источниках:
   - Настройки → Безопасность → Разрешить установку из неизвестных источников
   - Или при установке нажмите "Разрешить из этого источника"

## Требования

- Android Studio (уже установлен)
- Android SDK (уже установлен в: `C:\Users\Nep\AppData\Local\Android\Sdk`)
- Java JDK 8 или выше
- Файл `local.properties` уже создан с правильным путем к SDK

## Устранение проблем

Если сборка не работает:
1. Убедитесь, что Android Studio полностью установлена
2. Откройте проект в Android Studio и дождитесь синхронизации Gradle
3. Проверьте, что в Android Studio установлены необходимые SDK компоненты:
   - Tools → SDK Manager → SDK Platforms → Android 14.0 (API 34)
   - SDK Tools → Android SDK Build-Tools
