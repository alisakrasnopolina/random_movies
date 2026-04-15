from datetime import datetime, timedelta, timezone
import hashlib
from jose import jwt
from passlib.context import CryptContext

from app.core.config import settings

"""
Модуль безопасности.

Содержит функции для:
- хэширования и проверки паролей;
- генерации access и refresh JWT-токенов;
- декодирования токенов;
- хэширования refresh token перед сохранением в базе;
- формирования единообразных пользовательских ошибок.
"""

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
"""
Контекст passlib для работы с bcrypt.
"""


def hash_password(password: str) -> str:
    """
    Хэширует пароль пользователя.

    Для bcrypt действует ограничение на длину строки: не более 72 байт.

    :param password: Пароль в открытом виде
    :return: Хэш пароля
    :raises ValueError: Если пароль превышает допустимую длину
    """
    if len(password.encode("utf-8")) > 72:
        raise ValueError("Password is too long for bcrypt (max 72 bytes).")
    return pwd_context.hash(password)


def verify_password(password: str, password_hash: str) -> bool:
    """
    Проверяет соответствие пароля его хэшу.

    :param password: Пароль в открытом виде
    :param password_hash: Хэш пароля из базы данных
    :return: True, если пароль корректен, иначе False
    """
    if len(password.encode("utf-8")) > 72:
        return False
    return pwd_context.verify(password, password_hash)


def create_access_token(user_id: str) -> str:
    """
    Создает JWT access token.

    :param user_id: Идентификатор пользователя
    :return: Подписанный access token
    """
    exp = datetime.now(timezone.utc) + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MIN)
    payload = {
        "sub": user_id,
        "type": "access",
        "exp": exp,
    }
    return jwt.encode(payload, settings.JWT_SECRET, algorithm=settings.JWT_ALG)


def create_refresh_token(user_id: str, session_id: str) -> str:
    """
    Создает JWT refresh token.

    В refresh token дополнительно сохраняется идентификатор серверной сессии.

    :param user_id: Идентификатор пользователя
    :param session_id: Идентификатор сессии
    :return: Подписанный refresh token
    """
    exp = datetime.now(timezone.utc) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)
    payload = {
        "sub": user_id,
        "type": "refresh",
        "sid": session_id,
        "exp": exp,
    }
    return jwt.encode(payload, settings.JWT_SECRET, algorithm=settings.JWT_ALG)


def decode_token(token: str) -> dict:
    """
    Декодирует JWT token.

    :param token: JWT token
    :return: Словарь claims
    """
    return jwt.decode(token, settings.JWT_SECRET, algorithms=[settings.JWT_ALG])


def hash_refresh_token(raw_token: str) -> str:
    """
    Хэширует refresh token перед сохранением в базе данных.

    :param raw_token: Исходный refresh token
    :return: SHA-256 хэш токена
    """
    return hashlib.sha256(raw_token.encode("utf-8")).hexdigest()


def now_utc() -> datetime:
    """
    Возвращает текущее время в UTC.

    :return: Объект datetime с timezone UTC
    """
    return datetime.now(timezone.utc)


def to_user_error(code: str, message: str, status_code: int = 400):
    """
    Выбрасывает HTTP-исключение в унифицированном формате.

    :param code: Машинный код ошибки
    :param message: Текст ошибки для клиента
    :param status_code: HTTP-статус ответа
    :raises fastapi.HTTPException: Исключение для возврата клиенту
    """
    from fastapi import HTTPException
    raise HTTPException(status_code=status_code, detail={"error": code, "message": message})