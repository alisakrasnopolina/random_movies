import logging
import threading
import time
from datetime import timedelta

from fastapi import APIRouter, Depends, Request, Header
from jose import JWTError
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.security import (
    hash_password,
    verify_password,
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_refresh_token,
    now_utc,
    to_user_error,
)
from app.db.session import get_db
from app.models.user import User
from app.models.profile import Profile
from app.models.session import Session as UserSession
from app.schemas.auth import RegisterRequest, LoginRequest, RefreshRequest, LogoutRequest

"""
Модуль auth.

Содержит маршруты аутентификации и управления пользовательскими сессиями:
регистрацию, вход, обновление токенов, выход из текущей сессии
и выход со всех устройств.

Также модуль включает простой in-memory rate limit для защиты
endpoint входа от частого перебора пароля.
"""

router = APIRouter(prefix="/auth", tags=["auth"])
logger = logging.getLogger("auth")


# ---------------------------
# simple in-memory rate-limit for login (stage)
# key=email -> list[timestamps]
# ---------------------------
_LOGIN_ATTEMPTS = {}
_LOGIN_LOCK = threading.Lock()
_LOGIN_WINDOW_SEC = 60
_LOGIN_MAX_ATTEMPTS = 8


def _is_rate_limited(email: str) -> bool:
    """
    Проверяет, превышен ли лимит попыток входа для указанного email.

    :param email: Email пользователя
    :return: True, если число попыток входа превышает лимит за заданное окно времени
    """
    now = time.time()
    with _LOGIN_LOCK:
        arr = _LOGIN_ATTEMPTS.get(email, [])
        arr = [t for t in arr if now - t < _LOGIN_WINDOW_SEC]
        if len(arr) >= _LOGIN_MAX_ATTEMPTS:
            _LOGIN_ATTEMPTS[email] = arr
            return True
        arr.append(now)
        _LOGIN_ATTEMPTS[email] = arr
        return False


def _extract_request_id(request: Request) -> str:
    """
    Возвращает request_id из объекта запроса.

    :param request: Объект HTTP-запроса
    :return: Идентификатор запроса или '-' при отсутствии значения
    """
    return getattr(request.state, "request_id", "-")


@router.post("/register")
def register(payload: RegisterRequest, request: Request, db: Session = Depends(get_db)):
    """
    Регистрирует нового пользователя.

    Выполняет проверку уникальности email, создает пользователя,
    хэширует пароль и автоматически создает связанный профиль.

    :param payload: Данные регистрации пользователя
    :param request: HTTP-запрос
    :param db: Сессия базы данных
    :return: Сообщение об успешной регистрации
    """
    rid = _extract_request_id(request)
    email = payload.email.lower().strip()

    existing = db.query(User).filter(User.email == email).first()
    if existing:
        to_user_error("email_exists", "Email already exists", status_code=409)

    try:
        user = User(
            email=email,
            password_hash=hash_password(payload.password),
            is_active=True,
        )
    except ValueError as e:
        to_user_error("invalid_password", str(e), status_code=422)

    db.add(user)
    db.flush()

    profile = Profile(
        user_id=user.id,
        display_name=payload.display_name.strip(),
        avatar_url=None,
        favorite_genres=None,
    )
    db.add(profile)
    db.commit()

    logger.info("register_ok rid=%s user_id=%s email=%s", rid, user.id, email)
    return {"message": "registered"}


@router.post("/login")
def login(payload: LoginRequest, request: Request, db: Session = Depends(get_db)):
    """
    Выполняет вход пользователя в систему.

    Проверяет rate limit, ищет пользователя по email, валидирует пароль,
    создает серверную сессию и возвращает пару access/refresh токенов.

    :param payload: Данные для входа
    :param request: HTTP-запрос
    :param db: Сессия базы данных
    :return: JSON с токенами и краткой информацией о пользователе
    """
    rid = _extract_request_id(request)
    email = payload.email.lower().strip()

    if _is_rate_limited(email):
        to_user_error("rate_limited", "Too many login attempts. Try again later.", status_code=429)

    user = db.query(User).filter(User.email == email).first()
    if not user:
        logger.warning("login_fail_no_user rid=%s email=%s", rid, email)
        to_user_error("invalid_credentials", "Invalid email or password", status_code=401)

    if not verify_password(payload.password, user.password_hash):
        logger.warning("login_fail_bad_pass rid=%s user_id=%s email=%s", rid, user.id, email)
        to_user_error("invalid_credentials", "Invalid email or password", status_code=401)

    if not user.is_active:
        to_user_error("user_inactive", "User is inactive", status_code=403)

    session_row = UserSession(
        user_id=user.id,
        refresh_token_hash="tmp",
        user_agent=request.headers.get("user-agent"),
        ip=request.client.host if request.client else None,
        expires_at=now_utc() + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS),
    )
    db.add(session_row)
    db.flush()

    refresh_token = create_refresh_token(str(user.id), str(session_row.id))
    session_row.refresh_token_hash = hash_refresh_token(refresh_token)

    access_token = create_access_token(str(user.id))
    db.commit()

    display_name = user.profile.display_name if user.profile else ""
    logger.info("login_ok rid=%s user_id=%s sid=%s", rid, user.id, session_row.id)

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer",
        "expires_in": settings.ACCESS_TOKEN_EXPIRE_MIN * 60,
        "user": {
            "id": str(user.id),
            "email": user.email,
            "display_name": user.profile.display_name if user.profile else user.email,
            "avatar_url": user.profile.avatar_url if user.profile else None,
        },
    }


@router.post("/refresh")
def refresh(payload: RefreshRequest, request: Request, db: Session = Depends(get_db)):
    """
    Обновляет access и refresh токены по валидному refresh token.

    Проверяет тип токена, наличие сессии, срок действия,
    факт отзыва и совпадение хэша refresh token в базе данных.
    При успешной проверке выполняет ротацию refresh token.

    :param payload: Запрос с refresh token
    :param request: HTTP-запрос
    :param db: Сессия базы данных
    :return: JSON с новой парой токенов
    """
    rid = _extract_request_id(request)

    try:
        claims = decode_token(payload.refresh_token)
    except JWTError:
        to_user_error("invalid_refresh", "Invalid refresh token", status_code=401)

    if claims.get("type") != "refresh":
        to_user_error("invalid_token_type", "Token type must be refresh", status_code=401)

    sid = claims.get("sid")
    sub = claims.get("sub")
    if not sid or not sub:
        to_user_error("malformed_token", "Refresh token payload is invalid", status_code=401)

    session_row = db.query(UserSession).filter(UserSession.id == sid).first()
    if not session_row:
        to_user_error("session_not_found", "Session not found", status_code=401)

    if session_row.revoked_at is not None:
        to_user_error("session_revoked", "Session revoked", status_code=401)

    if session_row.expires_at <= now_utc():
        to_user_error("session_expired", "Session expired", status_code=401)

    incoming_hash = hash_refresh_token(payload.refresh_token)
    if incoming_hash != session_row.refresh_token_hash:
        to_user_error("refresh_mismatch", "Refresh token mismatch", status_code=401)

    new_refresh = create_refresh_token(str(session_row.user_id), str(session_row.id))
    session_row.refresh_token_hash = hash_refresh_token(new_refresh)
    session_row.expires_at = now_utc() + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)

    new_access = create_access_token(str(session_row.user_id))
    db.commit()

    logger.info("refresh_ok rid=%s user_id=%s sid=%s", rid, session_row.user_id, session_row.id)
    return {
        "access_token": new_access,
        "refresh_token": new_refresh,
        "token_type": "bearer",
        "expires_in": settings.ACCESS_TOKEN_EXPIRE_MIN * 60,
    }


@router.post("/logout")
def logout(payload: LogoutRequest, request: Request, db: Session = Depends(get_db)):
    """
    Выполняет выход пользователя из текущей сессии.

    Endpoint работает идемпотентно: даже если refresh token некорректный,
    ответ считается успешным. При валидной сессии она помечается отозванной.

    :param payload: Запрос с refresh token
    :param request: HTTP-запрос
    :param db: Сессия базы данных
    :return: Сообщение о завершении выхода
    """
    rid = _extract_request_id(request)

    try:
        claims = decode_token(payload.refresh_token)
    except JWTError:
        return {"message": "logged out"}

    sid = claims.get("sid")
    if not sid:
        return {"message": "logged out"}

    session_row = db.query(UserSession).filter(UserSession.id == sid).first()
    if session_row and session_row.revoked_at is None:
        session_row.revoked_at = now_utc()
        db.commit()
        logger.info("logout_ok rid=%s user_id=%s sid=%s", rid, session_row.user_id, sid)

    return {"message": "logged out"}


@router.post("/logout-all")
def logout_all(payload: LogoutRequest, request: Request, db: Session = Depends(get_db)):
    """
    Выполняет выход пользователя со всех устройств.

    Находит все активные сессии пользователя и помечает их отозванными.

    :param payload: Запрос с refresh token
    :param request: HTTP-запрос
    :param db: Сессия базы данных
    :return: Сообщение о завершении глобального выхода
    """
    rid = _extract_request_id(request)

    try:
        claims = decode_token(payload.refresh_token)
    except JWTError:
        return {"message": "logged out_all"}

    user_id = claims.get("sub")
    if not user_id:
        return {"message": "logged out_all"}

    active_sessions = (
        db.query(UserSession)
        .filter(UserSession.user_id == user_id, UserSession.revoked_at.is_(None))
        .all()
    )

    now = now_utc()
    for s in active_sessions:
        s.revoked_at = now
    db.commit()

    logger.info("logout_all_ok rid=%s user_id=%s count=%s", rid, user_id, len(active_sessions))
    return {"message": "logged out_all"}

def get_current_user(
        authorization: str | None = Header(default=None),
        db: Session = Depends(get_db),
) -> User:
    """
    Возвращает текущего пользователя по access token из заголовка Authorization.

    Ожидаемый заголовок:
    Authorization: Bearer <access_token>
    """
    if not authorization or not authorization.startswith("Bearer "):
        to_user_error("not_authenticated", "Not authenticated", status_code=401)

    token = authorization.replace("Bearer ", "", 1).strip()

    try:
        claims = decode_token(token)
    except JWTError:
        to_user_error("invalid_token", "Invalid access token", status_code=401)

    if claims.get("type") != "access":
        to_user_error("invalid_token_type", "Token type must be access", status_code=401)

    user_id = claims.get("sub")
    if not user_id:
        to_user_error("malformed_token", "Access token payload is invalid", status_code=401)

    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        to_user_error("user_not_found", "User not found", status_code=401)

    if not user.is_active:
        to_user_error("user_inactive", "User is inactive", status_code=403)

    return user