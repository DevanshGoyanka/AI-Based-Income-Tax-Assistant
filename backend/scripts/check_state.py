"""Check DB state and bootstrap cleanly."""
import psycopg
from urllib.parse import urlparse, quote
from app.settings import settings

parsed = urlparse(settings.database_url_sync)
user = quote(parsed.username or "", safe="")
pwd = quote(parsed.password or "", safe="")
db_name = parsed.path.lstrip("/").split("?")[0]
url = f"postgresql://{user}:{pwd}@{parsed.hostname}:{parsed.port}/{db_name}"

with psycopg.connect(url) as conn:
    with conn.cursor() as cur:
        cur.execute("SELECT count(*) FROM information_schema.tables WHERE table_schema='public'")
        n = cur.fetchone()[0]
        print(f"public tables: {n}")
        cur.execute("SELECT to_regclass('public.alembic_version')")
        av = cur.fetchone()[0]
        print(f"alembic_version table: {av}")
        if av:
            cur.execute("SELECT version_num FROM alembic_version")
            print(f"current version: {cur.fetchone()[0]}")
        cur.execute("SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename")
        for (t,) in cur.fetchall():
            print(f"  - {t}")
