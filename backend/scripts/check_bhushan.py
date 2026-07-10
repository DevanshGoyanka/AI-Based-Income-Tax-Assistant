import asyncio, sys
sys.path.insert(0, ".")
from app.infra.db.base import AsyncSessionLocal
from sqlalchemy import text

async def check():
    async with AsyncSessionLocal() as db:
        r = await db.execute(text("SELECT id, name, dob FROM clients WHERE pan = 'BRKPT6059A'"))
        row = r.fetchone()
        if row:
            dob = row[2]
            pwd = dob.strftime("%d%m%Y")
            print(f"Client: {row[1]}")
            print(f"DOB: {dob}")
            print(f"DOB as DDMMYYYY: {pwd}")

asyncio.run(check())
