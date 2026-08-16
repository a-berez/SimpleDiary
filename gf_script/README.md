# Grow Food parser

Локальный CLI-парсер для личного кабинета Grow Food (`lk.growfood.pro`).

Скрипт не парсит HTML-страницы. Личный кабинет работает как SPA и получает данные через JSON API на `https://admin.growfood.pro`. Этот проект использует тот же API напрямую.

## Что уже умеет скрипт

- Проверяет, что `client_token` рабочий.
- Выгружает профиль клиента.
- Выгружает активные заказы.
- Выгружает подписки.
- Выгружает детали каждого активного заказа.
- Выгружает меню каждого активного заказа.
- Собирает CSV по заказам.
- Собирает CSV по доставкам.
- Собирает CSV по блюдам/пакам с КБЖУ.
- Собирает CSV по пользовательской конфигурации рациона: дата, прием пищи, pack id, удалено/не удалено, КБЖУ.

## Безопасность

Токен авторизации является секретом. Не коммитьте его и не вставляйте в чат.

Проект ожидает токен в одном из двух мест:

1. Переменная окружения `GROWFOOD_CLIENT_TOKEN`.
2. Локальный файл `.env` в корне проекта.

Файл `.env` уже добавлен в `.gitignore`.

Пример `.env`:

```env
GROWFOOD_CLIENT_TOKEN=your_client_token_here
```

## Как получить client_token

1. Откройте `https://lk.growfood.pro`.
2. Войдите в личный кабинет через SMS.
3. Откройте инструменты разработчика браузера.
4. Найдите `client_token` в Cookies или Local Storage.
5. Сохраните значение в `.env` или в переменную окружения.

Сайт сам использует этот токен для API-запросов через HTTP-заголовок:

```http
X-Client-Token: <client_token>
```

## Быстрый старт

Проверить токен:

```powershell
python growfood_parser.py check
```

Сделать выгрузку:

```powershell
python growfood_parser.py sync
```

По умолчанию результат сохраняется в:

```text
data/YYYYMMDD-HHMMSS/
```

Например:

```text
data/20260807-092246/
```

Указать свою папку:

```powershell
python growfood_parser.py sync --out C:\Users\User\Documents\growfood-export
```

Не загружать детали заказов:

```powershell
python growfood_parser.py sync --skip-details
```

Важно: `--skip-details` отключает `order_details/*.json`, а значит `custom_config.csv` может быть пустым или неполным, потому что пользовательская конфигурация рациона лежит в деталях заказа.

## Структура результата

После `sync` создается папка выгрузки с такими файлами:

```text
data/YYYYMMDD-HHMMSS/
  profile.json
  active_orders.json
  subscriptions.json
  orders.csv
  deliveries.csv
  menu_packs.csv
  custom_config.csv
  order_details/
    <order_id_H>.json
  order_menus/
    <order_id_H>.json
```

### `profile.json`

Сырой JSON профиля клиента.

Endpoint:

```http
GET /api/personal-cabinet/v1_1/client-profile
```

### `active_orders.json`

Сырой JSON активных заказов.

Endpoint:

```http
GET /api/personal-cabinet/v2_0/orders/active?brandId_H=lY
```

### `subscriptions.json`

Сырой JSON подписок.

Endpoint:

```http
GET /api/personal-cabinet/v1/client-subscriptions?brandId_H=lY
```

### `order_details/*.json`

Сырой JSON деталей каждого заказа.

Endpoint:

```http
GET /api/personal-cabinet/v1_1/orders/get?orderId_H=<order_id_H>
```

В этих файлах сейчас важны:

- `order.deliveries` - доставки;
- `order.custom_config` - пользовательские замены и исключения по датам и приемам пищи;
- `order.custom_config[].mealDate` - дата приема пищи;
- `order.custom_config[].mealNumber` - номер приема пищи;
- `order.custom_config[].packId` - идентификатор блюда/пака;
- `order.custom_config[].remove` - флаг удаления.

### `order_menus/*.json`

Сырой JSON меню каждого заказа.

Endpoint:

```http
POST /api/personal-cabinet/v1_1/menu/order
```

Payload:

```json
{
  "orderId_H": "<order_id_H>",
  "brandId_H": "lY"
}
```

В этих файлах сейчас важны:

- `menu` - меню по датам;
- `packs` - справочник блюд/паков;
- `packs[pack_id].name` - название;
- `packs[pack_id].calories` - калории;
- `packs[pack_id].proteins` - белки;
- `packs[pack_id].fats` - жиры;
- `packs[pack_id].carbs` - углеводы;
- `packs[pack_id].weight` - вес;
- `packs[pack_id].dishes` - составные блюда внутри пака.

## CSV-файлы

Обычный запуск `sync` создает снимок в папке с датой и отдельно обновляет накопительные файлы для мобильного справочника:

```text
data/food_library/menu_packs.csv
data/food_library/custom_config.csv
```

Если `sync` запускается в уже существующую папку через `--out`, файлы с блюдами в этой папке тоже становятся накопительными:

- `menu_packs.csv` сохраняет старые блюда и обновляет/добавляет строки по `pack_id`;
- `custom_config.csv` сохраняет старые строки и обновляет/добавляет строки по связке `order_id_H`, `meal_date`, `meal_number`, `pack_id`.

Это нужно для импорта в мобильный справочник еды: блюдо остается доступным, даже если его больше нет в текущем активном заказе Grow Food.

### `orders.csv`

Базовая таблица активных заказов.

Поля:

- `id_H`
- `status`
- `title`
- `tariff`
- `price`
- `start_date`
- `end_date`
- `deliveries_count`
- `raw_detail_file`

### `deliveries.csv`

Таблица доставок, собранная из активных заказов и деталей заказов.

Поля:

- `order_id_H`
- `delivery_id_H`
- `date`
- `time_from`
- `time_to`
- `status`
- `address`

### `menu_packs.csv`

Справочник доступных блюд/паков по каждому заказу.

Поля:

- `order_id_H`
- `pack_id`
- `pack_name`
- `calories`
- `proteins`
- `fats`
- `carbs`
- `weight`
- `price`
- `dish_names`
- `ingredients`

### `custom_config.csv`

Пользовательская конфигурация рациона по датам и приемам пищи. Это наиболее полезная таблица, если нужно понять, какие конкретные паки были выбраны, заменены или исключены.

Поля:

- `order_id_H`
- `meal_date`
- `meal_number`
- `pack_id`
- `removed`
- `config_type`
- `is_free_custom`
- `is_auto_custom`
- `pack_name`
- `calories`
- `proteins`
- `fats`
- `carbs`
- `weight`

`removed=True` означает, что запись в `custom_config` помечена как удаленная. Для аналитики фактически выбранных блюд обычно нужно фильтровать `removed=False`.

## Проверенные API-находки

Базовый API:

```text
https://admin.growfood.pro
```

Текущий `brandId_H` для Grow Food:

```text
lY
```

Сайт `lk.growfood.pro` вычисляет `brandId_H` во фронтенде. Для Grow Food это `lY`; для других брендов может быть другое значение.

Авторизация:

```http
X-Client-Token: <client_token>
```

Если токен неверный или истек, API возвращает:

```json
{
  "success": false,
  "messages": ["Неавторизован"]
}
```

Обычно это HTTP `401`.

## Текущие ограничения

- Скрипт выгружает только активные заказы, потому что подтвержденный endpoint сейчас именно `orders/active`.
- История старых заказов пока не реализована.
- Автоматический SMS-вход не реализован намеренно: он может требовать антибот/EFST/recaptcha-токены, поэтому надежнее использовать готовый `client_token`.
- CSV-структура сделана прагматично и может потребовать доработки, если API изменит форму ответа.

## Типовой сценарий анализа

Сделать свежую выгрузку:

```powershell
python growfood_parser.py sync
```

Открыть последнюю папку в `data/`.

Для списка заказов смотреть:

```text
orders.csv
```

Для доставок:

```text
deliveries.csv
```

Для КБЖУ и блюд:

```text
menu_packs.csv
```

Для выбранных/исключенных позиций по датам:

```text
custom_config.csv
```

## Разработка

Проверить синтаксис:

```powershell
python -m py_compile growfood_parser.py
```

Посмотреть справку:

```powershell
python growfood_parser.py --help
python growfood_parser.py sync --help
```

Проект специально написан без внешних Python-зависимостей: используется только стандартная библиотека.
