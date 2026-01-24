# Установка иконки Icons8

Я скачал иконку с Icons8. Теперь нужно сгенерировать все размеры для Android.

## Способ 1: Через Android Asset Studio (Онлайн) - РЕКОМЕНДУЕТСЯ

1. **Откройте:** https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html

2. **Загрузите иконку:**
   - Нажмите "Image" в разделе "Foreground Layer"
   - Загрузите файл `icon_source.png` или `icon_source_512.png` (если доступен)

3. **Настройте:**
   - Background Layer: выберите цвет (например, фиолетовый #6200EE) или оставьте прозрачным
   - При необходимости отрегулируйте размер и отступы

4. **Скачайте:**
   - Нажмите "Download" → скачается ZIP архив

5. **Установите:**
   - Распакуйте архив
   - Скопируйте все папки `mipmap-*` из `res/` в `app/src/main/res/`
   - Замените существующие файлы

## Способ 2: Через Android Studio

1. Откройте проект в Android Studio
2. Правой кнопкой на `app/src/main/res` → **New → Image Asset**
3. Выберите "Launcher Icons (Adaptive and Legacy)"
4. В разделе "Foreground Layer":
   - Asset Type: **Image**
   - Path: выберите `icon_source.png` или `icon_source_512.png`
5. В разделе "Background Layer":
   - Asset Type: **Color**
   - Color: выберите цвет (например, #6200EE)
6. Нажмите **Next** → **Finish**

Android Studio автоматически создаст все нужные размеры иконок!

## Файлы иконок находятся в корне проекта:
- `icon_source.png` (100x100)
- `icon_source_512.png` (512x512, если доступен)
