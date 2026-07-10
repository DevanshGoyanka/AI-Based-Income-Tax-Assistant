"""End-to-end test for Phase 2 - 26AS / AIS / TIS import."""
import asyncio
import httpx
from pathlib import Path

BASE = "http://localhost:8001/api/v1"
FIXTURES = Path(__file__).parent.parent / "tests" / "fixtures"


async def main():
    async with httpx.AsyncClient(base_url=BASE, timeout=15.0) as c:
        r = await c.post("/auth/login", json={"email": "ca@firm.in", "password": "test1234"})
        assert r.status_code == 200, f"Login: {r.text}"
        token = r.json()["access_token"]
        auth = {"Authorization": f"Bearer {token}"}

        print("=" * 60)
        print("PHASE 2 SMOKE TEST - 26AS / AIS / TIS IMPORT")
        print("=" * 60)

        # 1. Upload 26AS
        with open(FIXTURES / "form_26as_sample.json", "rb") as f:
            r = await c.post(
                "/prefill/26as",
                files={"file": ("form_26as.json", f, "application/json")},
                data={"ay": "2026-27"},
                headers=auth,
            )
        assert r.status_code == 200, f"26AS import: {r.status_code} {r.text}"
        d = r.json()
        print(f"[1] POST /prefill/26as (PAN {d['pan']})  -> {r.status_code}")
        print(f"    summary: {d['summary']}")
        assert d["source"] == "26AS"
        assert d["stored"] == 3
        assert d["summary"]["total_tax_deducted"] == 127500
        assert d["summary"]["deductor_count"] == 3
        assert "192" in d["summary"]["sections_seen"]
        assert "194A" in d["summary"]["sections_seen"]

        # 2. Upload AIS
        with open(FIXTURES / "ais_sample.json", "rb") as f:
            r = await c.post(
                "/prefill/ais",
                files={"file": ("ais.json", f, "application/json")},
                data={"ay": "2026-27"},
                headers=auth,
            )
        assert r.status_code == 200, f"AIS import: {r.status_code} {r.text}"
        d = r.json()
        print(f"[2] POST /prefill/ais  (PAN {d['pan']})  -> {r.status_code}")
        print(f"    summary: {d['summary']}")
        assert d["source"] == "AIS"
        assert d["stored"] == 2
        assert d["summary"]["total_tax_deducted"] == 180000

        # 3. Upload TIS
        with open(FIXTURES / "tis_sample.json", "rb") as f:
            r = await c.post(
                "/prefill/tis",
                files={"file": ("tis.json", f, "application/json")},
                data={"ay": "2026-27"},
                headers=auth,
            )
        assert r.status_code == 200, f"TIS import: {r.status_code} {r.text}"
        d = r.json()
        print(f"[3] POST /prefill/tis  (PAN {d['pan']})  -> {r.status_code}")
        print(f"    summary: {d['summary']}")
        assert d["source"] == "TIS"
        assert d["stored"] == 2

        # 4. List TDS for AIS client
        r = await c.get("/clients", headers=auth)
        clients = r.json()
        ais_client = next((c for c in clients if c["pan"] == "XYZAB5678C"), None)
        assert ais_client, "AIS client not found"
        cid = ais_client["id"]
        r = await c.get(f"/prefill/tds/{cid}?ay=2026-27", headers=auth)
        assert r.status_code == 200
        d = r.json()
        print(f"[4] GET /prefill/tds/{{id}}  -> {r.status_code} count={d['count']} total_tds={d['total_tax_deducted']}")
        assert d["count"] == 2
        assert d["total_tax_deducted"] == 180000

        # 5. Re-upload 26AS (idempotent)
        with open(FIXTURES / "form_26as_sample.json", "rb") as f:
            r = await c.post(
                "/prefill/26as",
                files={"file": ("form_26as.json", f, "application/json")},
                data={"ay": "2026-27"},
                headers=auth,
            )
        assert r.status_code == 200
        d = r.json()
        assert d["stored"] == 3
        print(f"[5] POST /prefill/26as (re-upload)        -> {r.status_code} (idempotent: stored={d['stored']})")

        # 6. Invalid JSON
        r = await c.post(
            "/prefill/26as",
            files={"file": ("bad.json", b"not json", "application/json")},
            data={"ay": "2026-27"},
            headers=auth,
        )
        assert r.status_code == 400
        print(f"[6] POST /prefill/26as (invalid JSON)     -> 400 (correctly rejected)")

        # 7. No auth
        r = await c.post(
            "/prefill/26as",
            files={"file": ("x.json", b"{}", "application/json")},
            data={"ay": "2026-27"},
        )
        assert r.status_code == 401
        print(f"[7] POST /prefill/26as (no auth)          -> 401 (correctly rejected)")

        print()
        print("=" * 60)
        print("ALL PHASE 2 TESTS PASSED")
        print("=" * 60)


if __name__ == "__main__":
    asyncio.run(main())
