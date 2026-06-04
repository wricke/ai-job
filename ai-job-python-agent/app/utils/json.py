from typing import Any
import json


def dump_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False)


def load_json_object(value: str | None) -> dict[str, Any] | None:
    if value is None:
        return None
    try:
        data = json.loads(value)
        return data if isinstance(data, dict) else {"value": data}
    except json.JSONDecodeError:
        return {"value": value}
