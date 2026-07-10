"""End-to-end smoke test for Phase 1 - exercises the live API."""
import asyncio
import httpx

BASE = "http://localhost:8001/api/v1"


async def main():
    async with httpx.AsyncClient(base_url=BASE, timeout=10.0) as c:
        print("=" * 60)
        print("PHASE 1 SMOKE TEST")
        print("=" * 60)

        # 1. Health
        r = await c.get("/health")
        assert r.status_code == 200, f"Health: {r.status_code} {r.text}"
        print(f"[1] GET /health                       -> {r.status_code} {r.json()}")

        # 2. Register
        r = await c.post("/auth/register", json={
            "email": "ca@firm.in",
            "password": "test1234",
            "full_name": "Test CA",
        })
        if r.status_code == 409:
            print(f"[2] POST /auth/register              -> 409 (already exists, ok)")
        else:
            assert r.status_code == 201, f"Register: {r.status_code} {r.text}"
            data = r.json()
            print(f"[2] POST /auth/register              -> {r.status_code} user={data['user']['email']}")

        # 3. Login
        r = await c.post("/auth/login", json={"email": "ca@firm.in", "password": "test1234"})
        assert r.status_code == 200, f"Login: {r.status_code} {r.text}"
        data = r.json()
        token = data["access_token"]
        print(f"[3] POST /auth/login                 -> 200 token={token[:30]}...")

        # 4. List clients (empty)
        r = await c.get("/clients", headers={"Authorization": f"Bearer {token}"})
        assert r.status_code == 200, f"List: {r.status_code} {r.text}"
        clients = r.json()
        print(f"[4] GET /clients (auth)              -> 200 count={len(clients)}")

        # 5. Create client
        r = await c.post("/clients", headers={"Authorization": f"Bearer {token}"}, json={
            "pan": "ABCDE1234F",
            "name": "Sample Client",
            "mobile": "9876543210",
            "email": "client@example.com",
        })
        assert r.status_code == 201, f"Create: {r.status_code} {r.text}"
        client = r.json()
        print(f"[5] POST /clients (auth)             -> 201 id={client['id'][:8]}... pan={client['pan']}")

        # 6. Get client by id
        r = await c.get(f"/clients/{client['id']}", headers={"Authorization": f"Bearer {token}"})
        assert r.status_code == 200
        print(f"[6] GET /clients/{{id}} (auth)          -> 200 name={r.json()['name']}")

        # 7. Unauthorized
        r = await c.get("/clients")
        assert r.status_code == 401, f"Expected 401, got {r.status_code}"
        print(f"[7] GET /clients (no auth)           -> 401 (correctly rejected)")

        # 8. Invalid token
        r = await c.get("/clients", headers={"Authorization": "Bearer invalid.token.here"})
        assert r.status_code == 401
        print(f"[8] GET /clients (bad token)         -> 401 (correctly rejected)")

        print()
        print("=" * 60)
        print("ALL PHASE 1 TESTS PASSED")
        print("=" * 60)


if __name__ == "__main__":
    asyncio.run(main())
