import logging
import uuid
from fastapi import FastAPI, Request, HTTPException
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api.v1.auth import router as auth_router


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s"
)

app = FastAPI(title="Random Movies Backend", version="0.2.0")
app.include_router(auth_router)


@app.middleware("http")
async def request_id_middleware(request: Request, call_next):
    request_id = str(uuid.uuid4())
    request.state.request_id = request_id
    response = await call_next(request)
    response.headers["X-Request-ID"] = request_id
    return response


@app.get("/health")
def health():
    return {"status": "ok"}


@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    request_id = getattr(request.state, "request_id", "-")

    # если detail уже словарь с error/message — берём его
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
    request_id = getattr(request.state, "request_id", "-")
    # более человекочитаемо
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