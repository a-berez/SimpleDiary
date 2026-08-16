# Instructions for future agents

This repository contains a local parser for the user's personal Grow Food cabinet data.

The user-facing language in this project is Russian. Keep user-facing docs and responses in Russian unless the user asks otherwise.

## Project state

Main file:

```text
growfood_parser.py
```

Current docs:

```text
README.md
AGENTS.md
```

Local secrets and exports:

```text
.env
data/
```

These are intentionally ignored by git.

## Security rules

- Never print, quote, summarize, or expose `GROWFOOD_CLIENT_TOKEN`.
- Never commit `.env`.
- Never include raw personal data from `profile.json`, `active_orders.json`, `order_details/*.json`, or `order_menus/*.json` in a final response unless the user explicitly asks for that exact data.
- When reporting checks, prefer aggregate counts and file paths.
- Be careful with phone numbers, addresses, names, order ids, delivery ids, dish selections, and nutrition records. Treat them as personal data.
- If you need to inspect raw JSON, avoid pasting sensitive values into the chat. Print keys, counts, schemas, or redacted samples.

## Known API facts

The cabinet site is:

```text
https://lk.growfood.pro
```

The actual API root currently used by the SPA is:

```text
https://admin.growfood.pro
```

The client token is sent as:

```http
X-Client-Token: <client_token>
```

The parser reads the token from:

1. Environment variable `GROWFOOD_CLIENT_TOKEN`.
2. Local `.env` value `GROWFOOD_CLIENT_TOKEN=...`.

Current Grow Food brand id:

```text
lY
```

Confirmed endpoints:

```http
GET  /api/personal-cabinet/v1_1/client-profile
GET  /api/personal-cabinet/v2_0/orders/active?brandId_H=lY
GET  /api/personal-cabinet/v1/client-subscriptions?brandId_H=lY
GET  /api/personal-cabinet/v1_1/orders/get?orderId_H=<order_id_H>
POST /api/personal-cabinet/v1_1/menu/order
```

Confirmed menu payload:

```json
{
  "orderId_H": "<order_id_H>",
  "brandId_H": "lY"
}
```

Known auth failure:

```text
HTTP 401, message: Неавторизован
```

## Current export layout

`python growfood_parser.py sync` creates:

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

`orders.csv` is generated from active orders.

`deliveries.csv` is generated from active orders plus `order_details`.

`menu_packs.csv` is generated from `order_menus[*].packs`.

`custom_config.csv` is generated from `order_details[*].order.custom_config` and joined to `order_menus[*].packs` by `packId`.

Dish CSV files are cumulative:

- A normal timestamped `sync` also updates stable files in `data/food_library/`.
- When exporting into an existing output directory, the dish CSV files in that directory are merged too.

- `menu_packs.csv` keeps old rows and updates/adds rows by `pack_id`.
- `custom_config.csv` keeps old rows and updates/adds rows by `order_id_H + meal_date + meal_number + pack_id`.

This is intentional. The Android app uses these CSV files to populate a reusable food library, so old dishes should remain available even if they disappear from the current active Grow Food order.

## Important JSON structures

Order details:

```text
order.deliveries
order.custom_config
```

Custom config fields observed:

```text
configType
id
isAutoCustom
isFreeCustom
mealDate
mealNumber
packId
remove
```

Order menu fields observed:

```text
menu
packs
menu_type
pagination
customizable
canUseClientRestrictions
```

Pack fields observed:

```text
id
name
description
photoUrl
calories
fats
proteins
carbs
weight
count
price
dishes
```

Dish fields observed:

```text
id
name
ingredients
calories
fats
proteins
carbs
quantity
volume
weight
weightUnit
```

## How to verify changes

Basic syntax check:

```powershell
python -m py_compile growfood_parser.py
```

CLI help:

```powershell
python growfood_parser.py --help
python growfood_parser.py sync --help
```

Token check, requires `.env` or env var:

```powershell
python growfood_parser.py check
```

Full API export, requires network and token:

```powershell
python growfood_parser.py sync
```

When running API calls from Codex, network escalation may be required.

## Development guidance

- Prefer keeping the parser dependency-free unless there is a clear need.
- Keep raw JSON output. It is the best protection against losing data when CSV normalization is incomplete.
- Keep CSV columns stable when possible. If columns change, update `README.md`.
- If adding endpoints, record them in both `README.md` and this file.
- If API response shapes change, update extraction functions and add a short note in the docs.
- Use aggregate verification in final responses: row counts, file counts, output directory.
- Avoid broad refactors unless necessary. This is a small operational script.

## Known gaps

- Historical orders are not implemented.
- Automatic SMS login is not implemented and should not be the default direction. The SMS flow can involve anti-bot tokens, EFST, and recaptcha.
- `custom_config.csv` depends on `order_details`. If `--skip-details` is used, it will be empty or incomplete.
- The meaning of `mealNumber` is not documented by Grow Food. Treat it as an opaque meal index unless verified from UI labels.
