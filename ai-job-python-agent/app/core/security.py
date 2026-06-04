import base64
from datetime import datetime, timedelta, timezone
import hashlib
import hmac
import json
import secrets

from fastapi import HTTPException, status

from app.core.config import settings


def hash_password(password: str) -> str:
    salt = secrets.token_bytes(16)
    iterations = 120_000
    digest = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, iterations)
    return "pbkdf2_sha256${}${}${}".format(
        iterations,
        base64.b64encode(salt).decode(),
        base64.b64encode(digest).decode(),
    )


def verify_password(password: str, stored: str) -> bool:
    try:
        algorithm, iterations, salt_b64, digest_b64 = stored.split("$", 3)
        if algorithm != "pbkdf2_sha256":
            return False
        salt = base64.b64decode(salt_b64)
        expected = base64.b64decode(digest_b64)
        actual = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, int(iterations))
        return hmac.compare_digest(actual, expected)
    except Exception:
        return False


def _b64url_encode(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def _b64url_decode(value: str) -> bytes:
    padding = "=" * (-len(value) % 4)
    return base64.urlsafe_b64decode(value + padding)


def create_access_token(user_id: int, username: str) -> str:
    header = {"alg": "HS256", "typ": "JWT"}
    expires_at = datetime.now(timezone.utc) + timedelta(hours=settings.jwt_expire_hours)
    payload = {"sub": str(user_id), "username": username, "exp": int(expires_at.timestamp())}
    signing_input = ".".join(
        [
            _b64url_encode(json.dumps(header, separators=(",", ":")).encode()),
            _b64url_encode(json.dumps(payload, separators=(",", ":")).encode()),
        ]
    ).encode()
    signature = hmac.new(settings.jwt_secret_key.encode(), signing_input, hashlib.sha256).digest()
    return signing_input.decode() + "." + _b64url_encode(signature)


def decode_access_token(token: str) -> dict[str, object]:
    try:
        header_b64, payload_b64, signature_b64 = token.split(".", 2)
        signing_input = f"{header_b64}.{payload_b64}".encode()
        expected = hmac.new(settings.jwt_secret_key.encode(), signing_input, hashlib.sha256).digest()
        actual = _b64url_decode(signature_b64)
        if not hmac.compare_digest(expected, actual):
            raise ValueError("bad signature")
        payload = json.loads(_b64url_decode(payload_b64))
        if int(payload.get("exp", 0)) < int(datetime.now(timezone.utc).timestamp()):
            raise ValueError("expired token")
        return payload
    except Exception as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="invalid token") from exc
