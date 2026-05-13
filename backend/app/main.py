import logging
import uuid
from fastapi import FastAPI, Request, HTTPException
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api.v1.auth import router as auth_router
from app.api.v1.movies import router as movies_router
from app.api.v1.users_me_movies import router as users_me_movies_router
from app.api.v1 import profile
from app.api.v1 import friends
from app.api.v1 import recommendations
from app.api.v1 import friend_sessions

from fastapi.staticfiles import StaticFiles

"""
Главный модуль FastAPI-приложения.

Содержит:
- создание объекта FastAPI;
- подключение маршрутов API;
- middleware генерации request_id;
- endpoint проверки доступности сервиса;
- глобальные обработчики ошибок.
"""

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s"
)

app = FastAPI(title="Random Movies Backend", version="0.2.0")
"""
Экземпляр FastAPI-приложения.
"""

app.mount("/static", StaticFiles(directory="static"), name="static")

app.include_router(auth_router)
app.include_router(movies_router)
app.include_router(users_me_movies_router)
app.include_router(profile.router)
app.include_router(friends.router)
app.include_router(recommendations.router)
app.include_router(friend_sessions.router)


@app.middleware("http")
async def request_id_middleware(request: Request, call_next):
    """
    Добавляет уникальный request_id к каждому HTTP-запросу.

    Идентификатор сохраняется в request.state и дублируется
    в заголовке ответа X-Request-ID.

    :param request: HTTP-запрос
    :param call_next: Следующий обработчик в цепочке middleware
    :return: HTTP-ответ
    """
    request_id = str(uuid.uuid4())
    request.state.request_id = request_id
    response = await call_next(request)
    response.headers["X-Request-ID"] = request_id
    return response


@app.get("/health")
def health():
    """
    Возвращает статус доступности backend-сервиса.

    :return: JSON со статусом сервиса
    """
    return {"status": "ok"}


@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    """
    Глобальный обработчик HTTPException.

    Приводит ошибку к единому JSON-формату с кодом ошибки,
    сообщением и request_id.

    :param request: HTTP-запрос
    :param exc: Исключение FastAPI
    :return: JSON-ответ с описанием ошибки
    """
    request_id = getattr(request.state, "request_id", "-")

    if isinstance(exc.detail, dict):
        error = exc.detail.get("error", "http_error")
        message = exc.detail.get("message", "Request failed")
    else:
        error = "http_error"
        message = str(exc.detail)

    return JSONResponse(
        status_code=exc.status_code,
        content={
            "error": error,
            "message": message,
            "request_id": request_id
        }
    )


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """
    Глобальный обработчик ошибок валидации входных данных.

    Преобразует список ошибок валидации в человекочитаемое сообщение.

    :param request: HTTP-запрос
    :param exc: Ошибка валидации FastAPI
    :return: JSON-ответ с описанием ошибки
    """
    request_id = getattr(request.state, "request_id", "-")
    details = []
    for e in exc.errors():
        loc = ".".join([str(x) for x in e.get("loc", [])])
        details.append(f"{loc}: {e.get('msg', 'invalid')}")

    return JSONResponse(
        status_code=422,
        content={
            "error": "validation_error",
            "message": " ; ".join(details) if details else "Validation failed",
            "request_id": request_id
        }
    )


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    """
    Глобальный обработчик необработанных исключений.

    Возвращает пользователю единое сообщение об internal server error
    и пишет подробности в лог.

    :param request: HTTP-запрос
    :param exc: Необработанное исключение
    :return: JSON-ответ с описанием ошибки
    """
    request_id = getattr(request.state, "request_id", "-")
    logging.exception("unhandled_error request_id=%s", request_id)
    return JSONResponse(
        status_code=500,
        content={
            "error": "internal_error",
            "message": "Unexpected server error",
            "request_id": request_id
        }
    )