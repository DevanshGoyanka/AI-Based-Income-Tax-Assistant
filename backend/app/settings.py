"""Application settings loaded from .env."""
from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict

# Look for .env in project root first, then CWD
_ROOT_ENV = Path(__file__).resolve().parent.parent.parent / ".env"
_CWD_ENV = Path.cwd() / ".env"
_ENV_FILE = str(_ROOT_ENV if _ROOT_ENV.exists() else _CWD_ENV)


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=_ENV_FILE, env_file_encoding="utf-8", extra="ignore")

    app_name: str = "ITR Tax ERP"
    debug: bool = False
    api_prefix: str = "/api/v1"

    # Database - LOCAL PostgreSQL only (no cloud)
    database_url: str = "postgresql+asyncpg://postgres:Dsg@1234@localhost:5432/itr_filing_db"
    database_url_sync: str = "postgresql+psycopg://postgres:Dsg@1234@localhost:5432/itr_filing_db"

    jwt_secret: str = "change-me-in-production"
    jwt_algorithm: str = "HS256"
    jwt_expiration_seconds: int = 86400

    cors_origins: str = "http://localhost:5173,http://localhost:3000"

    @property
    def cors_origins_list(self) -> list[str]:
        return [o.strip() for o in self.cors_origins.split(",") if o.strip()]

    backup_dir: str = "./backups"
    backup_retention_days: int = 90
    backup_encryption_key: str = "change-me-in-production"

    storage_dir: str = "./storage"
    document_dir: str = "./storage/documents"
    pdf_dir: str = "./storage/pdfs"
    json_dir: str = "./storage/json"


settings = Settings()
