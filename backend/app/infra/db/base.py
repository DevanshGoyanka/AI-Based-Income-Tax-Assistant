"""Database session and base."""
from __future__ import annotations
from typing import AsyncGenerator
from urllib.parse import urlparse, quote
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.orm import DeclarativeBase
from app.settings import settings


class Base(DeclarativeBase):
    pass


def _build_async_url() -> str:
    """URL-encode the password in the async DB URL so asyncpg parses it correctly."""
    raw = settings.database_url
    if "@" not in raw:
        return raw
    parsed = urlparse(raw)
    user = quote(parsed.username or "", safe="")
    pwd = quote(parsed.password or "", safe="")
    return f"{parsed.scheme}://{user}:{pwd}@{parsed.hostname}:{parsed.port}{parsed.path}"


engine = create_async_engine(
    _build_async_url(),
    echo=settings.debug,
    pool_pre_ping=True,
    pool_size=10,
    max_overflow=20,
)

AsyncSessionLocal = async_sessionmaker(
    engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autoflush=False,
)


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    async with AsyncSessionLocal() as session:
        try:
            yield session
        finally:
            await session.close()
