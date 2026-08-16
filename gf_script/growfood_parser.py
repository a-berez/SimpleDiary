import argparse
import csv
import json
import os
import sys
from datetime import datetime
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


API_ROOT = "https://admin.growfood.pro"
BRAND_ID_H = "lY"
TOKEN_ENV = "GROWFOOD_CLIENT_TOKEN"


class GrowFoodError(RuntimeError):
    pass


class GrowFoodClient:
    def __init__(self, token: str, api_root: str = API_ROOT) -> None:
        self.token = token
        self.api_root = api_root.rstrip("/")

    def get(self, path: str, params: dict[str, Any] | None = None) -> Any:
        if params:
            path = f"{path}?{urlencode(params)}"
        return self._request("GET", path)

    def post(self, path: str, body: dict[str, Any] | None = None) -> Any:
        return self._request("POST", path, body or {})

    def _request(self, method: str, path: str, body: dict[str, Any] | None = None) -> Any:
        url = f"{self.api_root}{path}"
        payload = None if body is None else json.dumps(body).encode("utf-8")
        request = Request(
            url,
            data=payload,
            method=method,
            headers={
                "Accept": "application/json",
                "Content-Type": "application/json",
                "User-Agent": "growfood-parser/0.1",
                "X-Client-Token": self.token,
            },
        )

        try:
            with urlopen(request, timeout=30) as response:
                text = response.read().decode("utf-8")
                return json.loads(text) if text else None
        except HTTPError as exc:
            text = exc.read().decode("utf-8", errors="replace")
            message = _extract_error_message(text) or text or exc.reason
            raise GrowFoodError(f"API returned {exc.code}: {message}") from exc
        except URLError as exc:
            raise GrowFoodError(f"Network error: {exc.reason}") from exc
        except json.JSONDecodeError as exc:
            raise GrowFoodError(f"API returned non-JSON response from {url}") from exc

    def profile(self) -> Any:
        return self.get("/api/personal-cabinet/v1_1/client-profile")

    def active_orders(self) -> Any:
        return self.get(
            "/api/personal-cabinet/v2_0/orders/active",
            {"brandId_H": BRAND_ID_H},
        )

    def subscriptions(self) -> Any:
        return self.get(
            "/api/personal-cabinet/v1/client-subscriptions",
            {"brandId_H": BRAND_ID_H},
        )

    def order_details(self, order_id_h: str) -> Any:
        return self.get(
            "/api/personal-cabinet/v1_1/orders/get",
            {"orderId_H": order_id_h},
        )

    def order_menu(self, order_id_h: str) -> Any:
        return self.post(
            "/api/personal-cabinet/v1_1/menu/order",
            {"orderId_H": order_id_h, "brandId_H": BRAND_ID_H},
        )


def main() -> int:
    parser = argparse.ArgumentParser(description="Export Grow Food cabinet data.")
    parser.add_argument(
        "--token-env",
        default=TOKEN_ENV,
        help=f"environment variable with client_token, default: {TOKEN_ENV}",
    )
    parser.add_argument("--api-root", default=API_ROOT)

    subparsers = parser.add_subparsers(dest="command", required=True)
    subparsers.add_parser("check", help="check that token can load profile")

    sync_parser = subparsers.add_parser("sync", help="export profile and order data")
    sync_parser.add_argument(
        "--out",
        default=None,
        help="output directory, default: data/YYYYMMDD-HHMMSS",
    )
    sync_parser.add_argument(
        "--skip-details",
        action="store_true",
        help="do not fetch per-order detail JSON files",
    )

    args = parser.parse_args()
    token = os.environ.get(args.token_env) or _read_dotenv_value(Path(".env"), args.token_env)
    if not token:
        print(f"Set {args.token_env} first, or add it to .env.", file=sys.stderr)
        return 2

    client = GrowFoodClient(token=token, api_root=args.api_root)

    try:
        if args.command == "check":
            return run_check(client)
        if args.command == "sync":
            return run_sync(client, args.out, skip_details=args.skip_details)
    except GrowFoodError as exc:
        print(str(exc), file=sys.stderr)
        return 1

    return 0


def run_check(client: GrowFoodClient) -> int:
    profile_response = client.profile()
    profile = _first_dict(profile_response, ("profile", "client", "data")) or {}
    name = " ".join(
        str(profile.get(key, "")).strip()
        for key in ("first_name", "last_name")
        if profile.get(key)
    )
    phone = profile.get("phone") or profile.get("phone_number") or ""
    label = ", ".join(part for part in (name, str(phone)) if part)
    print(f"Token works{': ' + label if label else ''}")
    return 0


def run_sync(client: GrowFoodClient, out: str | None, skip_details: bool = False) -> int:
    out_dir = Path(out) if out else Path("data") / datetime.now().strftime("%Y%m%d-%H%M%S")
    out_dir.mkdir(parents=True, exist_ok=True)

    profile = client.profile()
    active_orders = client.active_orders()
    subscriptions = client.subscriptions()

    _write_json(out_dir / "profile.json", profile)
    _write_json(out_dir / "active_orders.json", active_orders)
    _write_json(out_dir / "subscriptions.json", subscriptions)

    orders = _extract_orders(active_orders)
    details_by_id: dict[str, Any] = {}
    menus_by_id: dict[str, Any] = {}

    if not skip_details:
        details_dir = out_dir / "order_details"
        details_dir.mkdir(exist_ok=True)
        for order in orders:
            order_id = _as_str(order.get("id_H") or order.get("orderId_H") or order.get("order_id_H"))
            if not order_id:
                continue
            try:
                details = client.order_details(order_id)
            except GrowFoodError as exc:
                details = {"error": str(exc)}
            details_by_id[order_id] = details
            _write_json(details_dir / f"{_safe_filename(order_id)}.json", details)

    menus_dir = out_dir / "order_menus"
    menus_dir.mkdir(exist_ok=True)
    for order in orders:
        order_id = _as_str(order.get("id_H") or order.get("orderId_H") or order.get("order_id_H"))
        if not order_id:
            continue
        try:
            menu = client.order_menu(order_id)
        except GrowFoodError as exc:
            menu = {"error": str(exc)}
        menus_by_id[order_id] = menu
        _write_json(menus_dir / f"{_safe_filename(order_id)}.json", menu)

    _write_orders_csv(out_dir / "orders.csv", orders, details_by_id)
    _write_deliveries_csv(out_dir / "deliveries.csv", orders, details_by_id)
    _write_menu_packs_csv(out_dir / "menu_packs.csv", menus_by_id)
    _write_custom_config_csv(out_dir / "custom_config.csv", details_by_id, menus_by_id)

    if out is None:
        library_dir = Path("data") / "food_library"
        library_dir.mkdir(parents=True, exist_ok=True)
        _write_menu_packs_csv(library_dir / "menu_packs.csv", menus_by_id)
        _write_custom_config_csv(library_dir / "custom_config.csv", details_by_id, menus_by_id)
        print(f"Updated cumulative dish CSV(s) in {library_dir.resolve()}")

    print(f"Exported {len(orders)} active order(s) to {out_dir.resolve()}")
    return 0


def _extract_orders(response: Any) -> list[dict[str, Any]]:
    root = _first_dict(response, ("body", "data")) or response
    if isinstance(root, dict):
        for key in ("orders", "activeOrders", "items", "result"):
            value = root.get(key)
            if isinstance(value, list):
                return [item for item in value if isinstance(item, dict)]
    if isinstance(root, list):
        return [item for item in root if isinstance(item, dict)]
    return []


def _write_orders_csv(path: Path, orders: list[dict[str, Any]], details_by_id: dict[str, Any]) -> None:
    fields = [
        "id_H",
        "status",
        "title",
        "tariff",
        "price",
        "start_date",
        "end_date",
        "deliveries_count",
        "raw_detail_file",
    ]
    rows = []
    for order in orders:
        order_id = _as_str(order.get("id_H") or order.get("orderId_H") or order.get("order_id_H"))
        rows.append(
            {
                "id_H": order_id,
                "status": _pick(order, "status", "state", "activity_status", "payment_status"),
                "title": _pick(order, "title", "name", "program_title", "menu_type_name"),
                "tariff": _pick(order, "tariff", "tariff_name", "price_plan_name", "abonement_type"),
                "price": _pick(order, "price", "total_price", "menu_price", "paid_price"),
                "start_date": _pick(order, "start_date", "first_delivery_date", "date_from"),
                "end_date": _pick(order, "end_date", "last_delivery_date", "date_to"),
                "deliveries_count": len(_find_deliveries(order)),
                "raw_detail_file": f"order_details/{_safe_filename(order_id)}.json"
                if order_id in details_by_id
                else "",
            }
        )
    _write_csv(path, fields, rows)


def _write_deliveries_csv(path: Path, orders: list[dict[str, Any]], details_by_id: dict[str, Any]) -> None:
    fields = [
        "order_id_H",
        "delivery_id_H",
        "date",
        "time_from",
        "time_to",
        "status",
        "address",
    ]
    rows = []
    for order in orders:
        order_id = _as_str(order.get("id_H") or order.get("orderId_H") or order.get("order_id_H"))
        delivery_sources = [order]
        detail = details_by_id.get(order_id)
        if detail:
            delivery_sources.append(detail)
        seen = set()
        for source in delivery_sources:
            for delivery in _find_deliveries(source):
                delivery_id = _as_str(delivery.get("id_H") or delivery.get("deliveryId_H") or delivery.get("id"))
                dedupe_key = (order_id, delivery_id, _pick(delivery, "date", "deliveryDate", "delivery_date"))
                if dedupe_key in seen:
                    continue
                seen.add(dedupe_key)
                rows.append(
                    {
                        "order_id_H": order_id,
                        "delivery_id_H": delivery_id,
                        "date": _pick(delivery, "date", "deliveryDate", "delivery_date"),
                        "time_from": _pick(delivery, "timeFrom", "time_from", "from"),
                        "time_to": _pick(delivery, "timeTo", "time_to", "to"),
                        "status": _pick(delivery, "status", "state"),
                        "address": _address_to_text(delivery.get("address") or delivery.get("clientAddress")),
                    }
                )
    _write_csv(path, fields, rows)


def _write_menu_packs_csv(path: Path, menus_by_id: dict[str, Any]) -> None:
    fields = [
        "order_id_H",
        "pack_id",
        "pack_name",
        "calories",
        "proteins",
        "fats",
        "carbs",
        "weight",
        "price",
        "dish_names",
        "ingredients",
    ]
    rows = []
    for order_id, menu_response in menus_by_id.items():
        packs = menu_response.get("packs") if isinstance(menu_response, dict) else None
        if not isinstance(packs, dict):
            continue
        for pack_id, pack in packs.items():
            if not isinstance(pack, dict):
                continue
            dishes = pack.get("dishes") if isinstance(pack.get("dishes"), list) else []
            rows.append(
                {
                    "order_id_H": order_id,
                    "pack_id": _as_str(pack.get("id") or pack_id),
                    "pack_name": _pick(pack, "name"),
                    "calories": _pick(pack, "calories"),
                    "proteins": _pick(pack, "proteins"),
                    "fats": _pick(pack, "fats"),
                    "carbs": _pick(pack, "carbs"),
                    "weight": _pick(pack, "weight"),
                    "price": _pick(pack, "price"),
                    "dish_names": "; ".join(_pick(dish, "name") for dish in dishes if isinstance(dish, dict)),
                    "ingredients": "; ".join(
                        _pick(dish, "ingredients") for dish in dishes if isinstance(dish, dict)
                    ),
                }
            )
    _write_csv(path, fields, rows, merge_key_fields=["pack_id"])


def _write_custom_config_csv(
    path: Path,
    details_by_id: dict[str, Any],
    menus_by_id: dict[str, Any],
) -> None:
    fields = [
        "order_id_H",
        "meal_date",
        "meal_number",
        "pack_id",
        "removed",
        "config_type",
        "is_free_custom",
        "is_auto_custom",
        "pack_name",
        "calories",
        "proteins",
        "fats",
        "carbs",
        "weight",
    ]
    rows = []
    for order_id, details in details_by_id.items():
        order = details.get("order") if isinstance(details, dict) else None
        custom_config = order.get("custom_config") if isinstance(order, dict) else None
        if not isinstance(custom_config, list):
            continue
        menu = menus_by_id.get(order_id)
        packs = menu.get("packs") if isinstance(menu, dict) and isinstance(menu.get("packs"), dict) else {}
        for item in custom_config:
            if not isinstance(item, dict):
                continue
            pack_id = _as_str(item.get("packId"))
            pack = packs.get(pack_id, {}) if isinstance(packs, dict) else {}
            rows.append(
                {
                    "order_id_H": order_id,
                    "meal_date": _pick(item, "mealDate"),
                    "meal_number": _pick(item, "mealNumber"),
                    "pack_id": pack_id,
                    "removed": _pick(item, "remove"),
                    "config_type": _pick(item, "configType"),
                    "is_free_custom": _pick(item, "isFreeCustom"),
                    "is_auto_custom": _pick(item, "isAutoCustom"),
                    "pack_name": _pick(pack, "name") if isinstance(pack, dict) else "",
                    "calories": _pick(pack, "calories") if isinstance(pack, dict) else "",
                    "proteins": _pick(pack, "proteins") if isinstance(pack, dict) else "",
                    "fats": _pick(pack, "fats") if isinstance(pack, dict) else "",
                    "carbs": _pick(pack, "carbs") if isinstance(pack, dict) else "",
                    "weight": _pick(pack, "weight") if isinstance(pack, dict) else "",
                }
            )
    _write_csv(
        path,
        fields,
        rows,
        merge_key_fields=["order_id_H", "meal_date", "meal_number", "pack_id"],
    )


def _find_deliveries(value: Any) -> list[dict[str, Any]]:
    deliveries: list[dict[str, Any]] = []

    def walk(node: Any, parent_key: str = "") -> None:
        if isinstance(node, dict):
            for key, child in node.items():
                if isinstance(child, list) and "deliver" in key.lower():
                    deliveries.extend(item for item in child if isinstance(item, dict))
                else:
                    walk(child, key)
        elif isinstance(node, list) and "deliver" in parent_key.lower():
            deliveries.extend(item for item in node if isinstance(item, dict))

    walk(value)
    return deliveries


def _write_json(path: Path, data: Any) -> None:
    path.write_text(
        json.dumps(data, ensure_ascii=False, indent=2, sort_keys=True),
        encoding="utf-8",
    )


def _write_csv(
    path: Path,
    fields: list[str],
    rows: list[dict[str, Any]],
    merge_key_fields: list[str] | None = None,
) -> None:
    if merge_key_fields and path.exists():
        rows = _merge_csv_rows(path, fields, rows, merge_key_fields)

    with path.open("w", newline="", encoding="utf-8-sig") as file:
        writer = csv.DictWriter(file, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def _merge_csv_rows(
    path: Path,
    fields: list[str],
    new_rows: list[dict[str, Any]],
    key_fields: list[str],
) -> list[dict[str, Any]]:
    existing_rows = _read_existing_csv_rows(path, fields)
    merged_rows: list[dict[str, Any]] = []
    index_by_key: dict[tuple[str, ...], int] = {}

    for row in existing_rows:
        key = _csv_row_key(row, key_fields)
        if key is not None and key in index_by_key:
            continue
        if key is not None:
            index_by_key[key] = len(merged_rows)
        merged_rows.append(row)

    for row in new_rows:
        normalized_row = _normalize_csv_row(row, fields)
        key = _csv_row_key(normalized_row, key_fields)
        if key is not None and key in index_by_key:
            merged_rows[index_by_key[key]] = normalized_row
            continue
        if key is not None:
            index_by_key[key] = len(merged_rows)
        merged_rows.append(normalized_row)

    return merged_rows


def _read_existing_csv_rows(path: Path, fields: list[str]) -> list[dict[str, Any]]:
    with path.open("r", newline="", encoding="utf-8-sig") as file:
        reader = csv.DictReader(file)
        return [_normalize_csv_row(row, fields) for row in reader]


def _normalize_csv_row(row: dict[str, Any], fields: list[str]) -> dict[str, Any]:
    return {field: _as_str(row.get(field)) for field in fields}


def _csv_row_key(row: dict[str, Any], key_fields: list[str]) -> tuple[str, ...] | None:
    key = tuple(_as_str(row.get(field)).strip() for field in key_fields)
    return key if any(key) else None


def _first_dict(value: Any, keys: tuple[str, ...]) -> dict[str, Any] | None:
    if not isinstance(value, dict):
        return None
    for key in keys:
        child = value.get(key)
        if isinstance(child, dict):
            return child
    return value


def _pick(source: dict[str, Any], *keys: str) -> str:
    for key in keys:
        value = source.get(key)
        if value is not None and not isinstance(value, (dict, list)):
            return str(value)
    return ""


def _address_to_text(value: Any) -> str:
    if isinstance(value, str):
        return value
    if not isinstance(value, dict):
        return ""
    parts = []
    for key in ("full", "full_address", "address", "street", "house", "flat"):
        if value.get(key):
            parts.append(str(value[key]))
    return ", ".join(parts)


def _extract_error_message(text: str) -> str:
    try:
        data = json.loads(text)
    except json.JSONDecodeError:
        return ""
    for key in ("message", "messages", "error", "errors"):
        value = data.get(key) if isinstance(data, dict) else None
        if isinstance(value, str):
            return value
        if isinstance(value, list):
            return "; ".join(str(item) for item in value)
    return ""


def _read_dotenv_value(path: Path, key: str) -> str:
    if not path.exists():
        return ""
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        name, value = line.split("=", 1)
        if name.strip() != key:
            continue
        return value.strip().strip('"').strip("'")
    return ""


def _safe_filename(value: str) -> str:
    safe = "".join(char if char.isalnum() or char in ("-", "_") else "_" for char in value)
    return safe or "unknown"


def _as_str(value: Any) -> str:
    return "" if value is None else str(value)


if __name__ == "__main__":
    raise SystemExit(main())
