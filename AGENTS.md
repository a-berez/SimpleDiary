# Simple Diary agent notes

This is a personal Android diary for nutrition, workouts, and state notes.
Keep user-facing conversation in Russian unless the user asks otherwise.

## Current direction

The user wants to reduce manual nutrition entry. The selected product direction is not automatic meal creation from delivery plans. Instead, build a general food library:

- Grow Food imports should populate the food library.
- Manually entered nutrition rows should also populate the food library.
- Actual diary meals remain manual facts: the user chooses what was actually eaten and when.
- Manual entry must remain available at all times.

Do not assume the delivery plan equals what the user ate. The user eats at variable times and may eat only part of a planned ration.

## Implemented so far

- Database version 9 adds optional `hungerBefore` / `satietyAfter` (1–10) on `meals`. They are entered in the meal editor and shown on the collapsed feed card subtitle (`Голод: N · Насыщение: M`). Missing values stay null; old meals are unchanged.
- Database version 8 adds `food_items`.
- `FoodItemDao` supports basic listing/search, source-key lookup, exact manual match lookup, and usage marking.
- Saving a meal now auto-adds named manual nutrition rows to `food_items` or increments usage for an exact manual match.
- `MealViewModel.onSaveClick()` now guards against repeated save launches while busy. This is intended to address observed duplicate meal pairs.
- `MealEditorScreen` has a "From library" bottom sheet. Selecting a library item copies its K/P/F/C into a regular editable nutrition row.
- The library picker supports a portion multiplier with quick values `0.5x`, `1x`, and `1.5x`; macros are scaled before being copied into the meal row.
- Editing a copied library row clears the source-food-item link, so the changed row is treated as manual on save.
- `SettingsScreen` has "Import Grow Food CSV". It imports `menu_packs.csv` or `custom_config.csv` from `gf_script` into `food_items` through `GrowFoodCsvImporter`.
- `GrowFoodCsvImporter` has an internal CSV parser that handles quoted fields and embedded newlines. It intentionally avoids a new dependency.
- `SettingsScreen` links to `FoodLibraryScreen`, where food items can be searched, edited, and deleted.
- Editing or deleting library items does not change already saved diary `nutrition_rows`.
- `gf_script` now preserves old dish rows. A normal timestamped `sync` also updates cumulative CSV files in `data/food_library/`; exporting into an existing `--out` directory merges `menu_packs.csv` by `pack_id` and `custom_config.csv` by order/date/meal/pack.

The app does not yet have direct Android-side Grow Food API sync. Current production flow is: run `gf_script`, import `menu_packs.csv` or `custom_config.csv` from Android settings, then choose actual eaten items manually from the food library.

## Architecture notes

- Main Android app: Kotlin, Jetpack Compose, Room, Navigation Compose.
- Room database is created in `SimpleDiaryApplication`.
- Feed and summary use `JournalRepository`.
- Editor screens currently work directly with DAO objects from `SimpleDiaryApplication`.
- Existing `nutrition_rows` are factual diary rows and should stay independent snapshots. Do not make old diary rows dynamically depend on library items.
- `gf_script/` is a local desktop script for Grow Food export. Treat token and raw exports as sensitive personal data.

## Data and privacy

- `simple_diary_backup/` and similar local backup/export artifacts contain the user's real usage data. They are gitignored (also `*.zip`, `desktop.ini`). Avoid printing raw meal names, notes, addresses, tokens, order ids, photos, or personal records in final responses.
- Prefer aggregate counts, schemas, and redacted samples.
- Never expose `GROWFOOD_CLIENT_TOKEN` or `.env` contents.
- Release signing uses local ignored files: `keystore.properties` and `keystore/simplediary-release.jks`. Never commit or print their secret values. The current release certificate is `CN=aberez` with SHA-256 `5a40785f1f2783686165eea76b2bed731c7125edfecced92c1a997b0cd79ed7d`.
- Android Studio `.idea/` is gitignored and should stay untracked; do not re-add IDE project files.

## Known issues and risks

- Some Russian strings in the project display as mojibake in PowerShell output. Verify encoding carefully before editing user-facing Russian strings.
- Duplicate meal creation was observed in the backup. The likely cause is repeated `Save` clicks starting multiple save coroutines before the UI disables the button.
- Cardio distance logic is tied to the localized category name for cardio. Renaming that category can break distance summaries.
- Current tests are mostly template tests; new database and import behavior should get focused tests when practical.

## Implementation direction for food library

Use a `food_items` table as the reusable library and keep `nutrition_rows` as immutable facts copied into meals. Suggested fields:

- name and normalizedName
- calories/proteins/fats/carbs
- optional weightGrams and ingredients
- source: `MANUAL` or `GROW_FOOD`
- optional sourceKey for imported rows
- useCount, lastUsedAt, createdAt, updatedAt

For future Grow Food work, prefer improving the CSV/JSON import path from `gf_script` before considering direct Android-side API sync. Avoid putting the unofficial Grow Food token flow directly in Android until there is a clear benefit.
