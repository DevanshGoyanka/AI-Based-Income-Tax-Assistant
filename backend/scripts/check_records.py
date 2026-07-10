import asyncio, sys
sys.path.insert(0, ".")
from app.infra.db.base import AsyncSessionLocal
from sqlalchemy import text

async def check():
    async with AsyncSessionLocal() as db:
        r = await db.execute(text("""
            SELECT f.client_id, f.ay, f.pan, f.name, 
                   (SELECT COUNT(*) FROM form_26as_data fd WHERE fd.client_id = f.client_id AND fd.ay = f.ay) as record_count
            FROM form_26as_data f
            GROUP BY f.client_id, f.ay, f.pan, f.name
            ORDER BY f.client_id
        """))
        rows = r.fetchall()
        print(f"Found {len(rows)} unique client/AY records in form_26as_data:")
        for row in rows:
            print(f"  Client={row[0]} | AY={row[1]} | PAN={row[2]} | Name={row[3]} | Records={row[4]}")

asyncio.run(check())
