".PHONY: help install dev test test-unit test-integration lint format migrate seed run clean build docker-up docker-down"

help:
	@echo "ITR Tax ERP - Make targets:"
	@echo "  install      - Install Python dependencies"
	@echo "  dev          - Run dev server (uvicorn --reload)"
	@echo "  test         - Run all tests"
	@echo "  test-unit    - Run unit tests only"
	@echo "  test-integration - Run integration tests"
	@echo "  lint         - Run ruff + mypy"
	@echo "  format       - Run ruff formatter"
	@echo "  migrate      - Run Alembic migrations"
	@echo "  seed         - Seed demo data"
	@echo "  run          - Run production server"
	@echo "  clean        - Clean cache files"
	@echo "  docker-up    - Start docker compose stack"
	@echo "  docker-down  - Stop docker compose stack"

install:
	pip install -r requirements.txt -r requirements-dev.txt

dev:
	cd backend && uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

test:
	cd backend && pytest

test-unit:
	cd backend && pytest tests/unit -v

test-integration:
	cd backend && pytest tests/integration -v

lint:
	cd backend && ruff check app tests
	cd backend && mypy app

format:
	cd backend && ruff format app tests

migrate:
	cd backend && alembic upgrade head

seed:
	cd backend && python -m scripts.seed_demo_clients

run:
	cd backend && uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4

clean:
	find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name .pytest_cache -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name .mypy_cache -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name .ruff_cache -exec rm -rf {} + 2>/dev/null || true

docker-up:
	docker compose up -d

docker-down:
	docker compose down
