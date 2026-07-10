"""FastAPI application factory."""
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.settings import settings
from app.api.v1 import auth, clients, health, imports, prefill


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield


def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.app_name,
        debug=settings.debug,
        lifespan=lifespan,
        docs_url="/docs",
        redoc_url="/redoc",
        openapi_url="/openapi.json",
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins_list,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    app.include_router(health.router, prefix=settings.api_prefix, tags=["health"])
    app.include_router(auth.router, prefix=settings.api_prefix, tags=["auth"])
    app.include_router(clients.router, prefix=settings.api_prefix, tags=["clients"])
    app.include_router(imports.router, prefix=settings.api_prefix, tags=["imports"])
    app.include_router(prefill.router, prefix=settings.api_prefix, tags=["prefill"])

    return app


app = create_app()
