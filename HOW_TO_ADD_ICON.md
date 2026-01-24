# Как добавить иконку приложения

## Способ 1: Android Asset Studio (РЕКОМЕНДУЕТСЯ)

1. **Найдите иконку:**
   - Откройте https://www.flaticon.com
   - Поиск: "pull up", "chin up", "exercise", "workout"
   - Выберите SVG или PNG (минимум 512x512)

2. **Сгенерируйте иконки:**
   - Откройте https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html
   - Загрузите ваш файл (SVG или PNG)
   - Настройте цвет фона (или оставьте прозрачным)
   - Нажмите "Download" → скачается ZIP архив

3. **Установите иконки:**
   - Распакуйте архив
   - Скопируйте папки `mipmap-*` из `res/` в `app/src/main/res/`
   - Замените существующие файлы

## Способ 2: Вручную (если есть PNG разных размеров)

Если у вас есть PNG файлы нужных размеров:

1. **Разместите файлы:**
   ```
   app/src/main/res/
   ├── mipmap-mdpi/
   │   ├── ic_launcher.png (48x48)
   │   └── ic_launcher_round.png (48x48)
   ├── mipmap-hdpi/
   │   ├── ic_launcher.png (72x72)
   │   └── ic_launcher_round.png (72x72)
   ├── mipmap-xhdpi/
   │   ├── ic_launcher.png (96x96)
   │   └── ic_launcher_round.png (96x96)
   ├── mipmap-xxhdpi/
   │   ├── ic_launcher.png (144x144)
   │   └── ic_launcher_round.png (144x144)
   └── mipmap-xxxhdpi/
       ├── ic_launcher.png (192x192)
       └── ic_launcher_round.png (192x192)
   ```

2. **Удалите старые XML файлы** (если они есть):
   - Удалите все `ic_launcher.xml` и `ic_launcher_round.xml` из папок `mipmap-*`

## Способ 3: Через Android Studio

1. **File → New → Image Asset**
2. Выберите "Launcher Icons (Adaptive and Legacy)"
3. Загрузите ваше изображение
4. Настройте и нажмите "Next" → "Finish"

## Рекомендуемые сайты для поиска:

- **Flaticon**: https://www.flaticon.com (бесплатно с указанием автора)
- **Icons8**: https://icons8.com (бесплатно с указанием автора)
- **Material Icons**: https://fonts.google.com/icons (полностью бесплатно)
- **The Noun Project**: https://thenounproject.com

## Поисковые запросы:

- "pull up"
- "chin up"
- "exercise"
- "workout"
- "fitness"
- "gym"
- "training"

## Формат файлов:

- **SVG** (лучший вариант) - можно конвертировать автоматически
- **PNG** (минимум 512x512) - для генерации всех размеров
