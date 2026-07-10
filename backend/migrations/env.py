"""Alembic env file - sync SQLAlchemy for migrations."""
import sys
from logging.config import fileConfig
from sqlalchemy import pool
from sqlalchemy.engine import Connection
from sqlalchemy import create_engine
from alembic import context

from app.infra.db.base import Base
from app.infra.db.models import *  # noqa - register all models
from app.settings import settings

config = context.config
# Force sync URL for Alembic (no asyncpg, no event loop)
config.set_main_option("sqlalchemy.url", settings.database_url_sync)

if config.config_file_name is not None:
    fileConfig(config.config_file_name)

target_metadata = Base.metadata


def run_migrations_offline() -> None:
    url = config.get_main_option("sqlalchemy.url")
    context.configure(url=url, target_metadata=target_metadata, literal_binds=True, dialect_opts={"paramstyle": "named"})
    with context.begin_transaction():
        context.run_migrations()


def do_run_migrations(connection: Connection) -> None:
    context.configure(connection=connection, target_metadata=target_metadata)
    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online() -> None:
    from urllib.parse import urlparse, quote
    # URL-encode the password (e.g. Dsg@1234 -> Dsg%401234) so psycopg doesn't
    # mis-parse the '@' as a host separator.
    parsed = urlparse(settings.database_url_sync)
    user = quote(parsed.username or "", safe="")
    pwd = quote(parsed.password or "", safe="")
    sync_url = f"{parsed.scheme}://{user}:{pwd}@{parsed.hostname}:{parsed.port}{parsed.path}"
    connectable = create_engine(sync_url, poolclass=pool.NullPool)
    with connectable.connect() as connection:
        do_run_migrations(connection)
    connectable.dispose()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
