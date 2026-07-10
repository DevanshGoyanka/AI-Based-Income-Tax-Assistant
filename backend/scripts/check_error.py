import asyncio, httpx

async def test():
    r = httpx.post("http://localhost:8001/api/v1/auth/login", json={"email": "test@example.com", "password": "Test123!@#"}, timeout=15)
    token = r.json()["access_token"]
    
    # Try uploading ZIP to client without DOB
    client_id = "d823fdad-1e93-4bc0-9402-b574b09a2814"  # DATTUSING - no DOB
    with open(r"C:\Users\Devansh\Desktop\E-FILE_karo\COVPC5929M-2026.zip", "rb") as f:
        r = httpx.post(
            f"http://localhost:8001/api/v1/imports/form26as/{client_id}",
            headers={"Authorization": f"Bearer {token}"},
            files={"file": ("test.zip", f, "application/zip")},
            timeout=30
        )
    
    print(f"Status: {r.status_code}")
    print(f"Body: {r.text[:500]}")

asyncio.run(test())
