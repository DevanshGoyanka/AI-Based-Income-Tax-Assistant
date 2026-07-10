"""Debug Form 26AS ZIP import - check what data is extracted."""
import asyncio
import httpx
import json

BASE_URL = "http://localhost:8001/api/v1"

async def debug_zip():
    """Debug ZIP import."""
    async with httpx.AsyncClient(timeout=30) as client:
        # Login
        login_res = await client.post(f"{BASE_URL}/auth/login", json={
            "email": "test@example.com",
            "password": "Test123!@#"
        })
        token = login_res.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        
        # Upload ZIP file
        zip_file = r"C:\Users\Devansh\Downloads\ACUPG3482G-2025.zip"
        
        print("Uploading ACUPG3482G-2025.zip (Sunit's data)...")
        with open(zip_file, 'rb') as f:
            files = {'file': ('ACUPG3482G-2025.zip', f, 'application/zip')}
            res = await client.post(
                f"{BASE_URL}/imports/form26as/ee604226-feda-4c8e-9425-18309827b1a7",
                headers=headers,
                files=files
            )
        
        print(f"Status: {res.status_code}")
        data = res.json()
        print(json.dumps(data, indent=2))
        
        print()
        print("=" * 70)
        
        # Check stored data in DB
        from app.infra.db.base import AsyncSessionLocal
        from sqlalchemy import text
        
        async with AsyncSessionLocal() as db:
            result = await db.execute(text('''
                SELECT assessment_year, raw_json 
                FROM form_26as_data 
                WHERE client_id = 'ee604226-feda-4c8e-9425-18309827b1a7'
                ORDER BY uploaded_at DESC
                LIMIT 1
            '''))
            row = result.fetchone()
            if row:
                print(f"Stored AY: {row[0]}")
                raw = row[1]
                print(f"Keys in raw_json: {list(raw.keys()) if isinstance(raw, dict) else 'N/A'}")
                if isinstance(raw, dict):
                    print(f"Part 1 data: {list(raw.get('part1_tds', []))[:3]}")
            else:
                print("No data found in DB")

asyncio.run(debug_zip())
