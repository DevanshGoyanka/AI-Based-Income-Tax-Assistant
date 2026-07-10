"""Drop all tables in the target DB and let Alembic rebuild from scratch."""
import psycopg
from urllib.parse import urlparse, quote
from app.settings import settings

parsed = urlparse(settings.database_url_sync)
user = quote(parsed.username or "", safe="")
pwd = quote(parsed.password or "", safe="")
db_name = parsed.path.lstrip("/").split("?")[0]
url = f"postgresql://{user}:{pwd}@{parsed.hostname}:{parsed.port}/{db_name}"

print(f"Dropping all tables in {db_name}...")
with psycopg.connect(url, autocommit=True) as conn:
    with conn.cursor() as cur:
        cur.execute("DROP SCHEMA public CASCADE")
        cur.execute("CREATE SCHEMA public")
        cur.execute("GRANT ALL ON SCHEMA public TO postgres")
        cur.execute("GRANT ALL ON SCHEMA public TO public")
print("Done. Now run: python -m scripts.bootstrap")
