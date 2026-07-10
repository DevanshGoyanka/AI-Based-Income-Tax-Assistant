"""Test configuration."""
import asyncio
import pytest
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker

from app.infra.db.base import Base, get_db
from app.main import create_app
from app.settings import settings


@pytest.fixture
def event_loop():
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


@pytest_asyncio.fixture
async def test_db():
    test_engine = create_async_engine(
        "sqlite+aiosqlite:///:memory:",
        echo=False,
    )
    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    
    TestSessionLocal = async_sessionmaker(test_engine, expire_on_commit=False)
    
    async def override_get_db():
        async with TestSessionLocal() as session:
            yield session
    
    app = create_app()
    app.dependency_overrides[get_db] = override_get_db
    yield app, TestSessionLocal
    await test_engine.dispose()


@pytest_asyncio.fixture
async def client(test_db):
    app, _ = test_db
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        yield ac
