import httpx
import asyncio

async def test():
    r = httpx.post("http://localhost:8001/api/v1/auth/login", json={"email": "test@example.com", "password": "Test123!@#"}, timeout=15)
    token = r.json()["access_token"]
    hdr = {"Authorization": f"Bearer {token}"}

    # Check get_clients response includes dob
    r = httpx.get("http://localhost:8001/api/v1/clients", headers=hdr, timeout=15)
    clients = r.json()
    for c in clients:
        if c.get("pan") == "AIOPG4038L":
            print(f"GET client: {c}")
            break

    # Test PATCH with ISO date string
    cid = "74ecef58-f297-449e-ad71-5c872c16922c"
    r = httpx.patch(
        f"http://localhost:8001/api/v1/clients/{cid}",
        headers=hdr,
        json={"dob": "1969-05-25"},
        timeout=15
    )
    print(f"PATCH status: {r.status_code}")
    print(f"PATCH body: {r.text[:300]}")

asyncio.run(test())
