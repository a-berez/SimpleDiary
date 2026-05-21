# Simple Diary

Персональный дневник здоровья на Android для одного пользователя: питание, тренировки и заметки о состоянии.

## Возможности

- **Лента записей (Feed)**: единая хронологическая лента по еде, тренировкам и заметкам.
- **Фильтры ленты**:
  - по типу записи;
  - по диапазону дат (Today / Last 7 days / Last 30 days / All time / Custom range).
- **Карточки в ленте**:
  - разворачивание по тапу;
  - детальная информация;
  - фото в карточках еды и заметок.
- **Еда (Meal)**:
  - текстовая заметка;
  - фото;
  - несколько строк КБЖУ по продуктам.
- **Тренировки (Workout)**:
  - дата/время;
  - категория и тип;
  - длительность;
  - калории;
  - заметка.
- **Заметки состояния (State Note)**:
  - текст;
  - опциональное фото.
- **Итоги дня (Daily Summary)**:
  - факт vs цель по КБЖУ;
  - недельная статистика тренировок.
- **Настройки**:
  - цели КБЖУ;
  - управление категориями и типами тренировок;
  - экспорт CSV;
  - backup/restore ZIP.
- **Фото**:
  - выбор из камеры/галереи;
  - crop через uCrop;
  - сжатие до JPEG (85%, max side 1200px).
- **Статические app shortcuts** (долгое нажатие на иконку):
  - Добавить еду;
  - Добавить тренировку;
  - Добавить заметку.

## Технологии

- **Kotlin**
- **Jetpack Compose (Material 3)**
- **Room**
- **Coroutines / Flow**
- **Navigation Compose**
- **uCrop**
- **Storage Access Framework (SAF)** для экспорта/backup

## Архитектура (кратко)

- `data/local` — Room entities/DAO/migrations.
- `data/repository` — реализация `JournalRepository`.
- `ui/*` — экраны, ViewModel и навигация.
- `core/files` + `data/files` — экспорт CSV, backup/restore ZIP, компрессия фото.

## Сборка проекта

### Требования

- Android Studio (JBR/JDK 17+)
- Android SDK (minSdk 26)

### Debug APK

```powershell
.\gradlew.bat :app:assembleDebug
```

### Release APK

```powershell
.\gradlew.bat :app:assembleRelease
```

APK после сборки:

`app/build/outputs/apk/release/app-release.apk`

## Подпись release

Проект читает параметры подписи из `keystore.properties` в корне.

Пример шаблона: `keystore.properties.example`

```properties
storeFile=keystore/simplediary-release.jks
storePassword=CHANGE_ME
keyAlias=simplediary
keyPassword=CHANGE_ME
```

> Не добавляйте `keystore.properties` и `.jks` в git.

## База данных

Основные сущности:

- `meals`
- `nutrition_rows`
- `workouts`
- `workout_categories`
- `workout_types`
- `state_notes`
- `daily_targets` (ретроспективные цели через `effectiveFrom`)

## Экспорт данных

- CSV экспортируется ZIP-архивом с файлами:
  - `meals.csv`
  - `workouts.csv`
  - `state_notes.csv`
- Разделитель CSV: `;`
- Заголовки колонок: русские.

## Лицензии библиотек

Используются открытые зависимости Android/Jetpack и uCrop. При публикации в store рекомендуется добавить отдельный раздел Third-Party Notices.

