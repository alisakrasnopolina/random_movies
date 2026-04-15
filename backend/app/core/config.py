from pydantic_settings import BaseSettings, SettingsConfigDict

"""
Модуль конфигурации приложения.

Содержит класс Settings, через который backend получает настройки
из переменных окружения и файла .env.
"""


class Settings(BaseSettings):
    """
    Класс конфигурации backend-приложения.

    Загружает параметры окружения, настройки подключения к базе данных
    и параметры JWT-аутентификации.
    """

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

    APP_ENV: str = "dev"
    """Режим окружения приложения."""

    APP_HOST: str = "0.0.0.0"
    """Хост, на котором запускается backend."""

    APP_PORT: int = 8000
    """Порт приложения."""

    DATABASE_URL: str
    """Строка подключения к PostgreSQL."""

    JWT_SECRET: str
    """Секретный ключ для подписи JWT."""

    JWT_ALG: str = "HS256"
    """Алгоритм подписи JWT."""

    ACCESS_TOKEN_EXPIRE_MIN: int = 15
    """Срок жизни access token в минутах."""

    REFRESH_TOKEN_EXPIRE_DAYS: int = 30
    """Срок жизни refresh token в днях."""


settings = Settings()
"""
Глобальный экземпляр настроек приложения.
"""