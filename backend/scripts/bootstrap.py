"""One-shot bootstrap: create DB if missing, run Alembic migrations (sync)."""
import sys
from urllib.parse import urlparse, quote
import psycopg
from alembic.config import Config
from alembic import command
from app.settings import settings


def parse_db_name(url: str) -> str:
    return urlparse(url).path.lstrip("/").split("?")[0]


def parse_admin_url(url: str) -> str:
    if "+psycopg" in url:
        url = url.replace("postgresql+psycopg://", "postgresql://", 1)
    parsed = urlparse(url)
    user = quote(parsed.username or "", safe="")
    pwd = quote(parsed.password or "", safe="")
    return f"{parsed.scheme}://{user}:{pwd}@{parsed.hostname}:{parsed.port}/postgres"


def ensure_database():
    db_name = parse_db_name(settings.database_url_sync)
    admin_url = parse_admin_url(settings.database_url_sync)

    print(f"[1/2] Checking database '{db_name}' on local Postgres...")
    try:
        with psycopg.connect(admin_url, autocommit=True) as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT 1 FROM pg_database WHERE datname = %s", (db_name,))
                if cur.fetchone():
                    print(f"      + Database '{db_name}' already exists")
                else:
                    cur.execute(f'CREATE DATABASE "{db_name}"')
                    print(f"      + Database '{db_name}' created")
    except Exception as e:
        print(f"      X Cannot connect to local Postgres: {e}")
        print("      Make sure PostgreSQL is running on localhost:5432 (user 'postgres', password 'Dsg@1234')")
        sys.exit(1)


def run_migrations():
    from urllib.parse import quote
    parsed = urlparse(settings.database_url)
    user = quote(parsed.username or "", safe="")
    pwd = quote(parsed.password or "", safe="")
    sync_url = f"postgresql+psycopg://{user}:{pwd}@{parsed.hostname}:{parsed.port}{parsed.path}"

    print(f"[2/2] Running Alembic migrations on '{parsed.hostname}:{parsed.port}/{parse_db_name(settings.database_url)}'...")
    import os
    cfg = Config(os.path.join(os.path.dirname(__file__), "..", "alembic.ini"))
    # Bypass configparser % interpolation by patching the section directly
    sections = cfg.file_config._sections  # type: ignore[attr-defined]
    sections[cfg.config_ini_section]["sqlalchemy.url"] = sync_url
    command.upgrade(cfg, "head")
    print("      + Migrations applied")


if __name__ == "__main__":
    ensure_database()
    run_migrations()
    print("\n+ Bootstrap complete. Boot server:")
    print('  cd "C:\\Users\\Devansh\\Desktop\\ITR-FilingWebsite-main"')
    print("  python -m uvicorn app.main:app --app-dir backend --port 8000")
