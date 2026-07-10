import httpx
import asyncio

async def test():
    r = httpx.post("http://localhost:8001/api/v1/auth/login", json={"email": "test@example.com", "password": "Test123!@#"}, timeout=15)
    token = r.json()["access_token"]
    hdr = {"Authorization": f"Bearer {token}"}

    # Get ASHA client (no DOB in DB)
    clients = httpx.get("http://localhost:8001/api/v1/clients", headers=hdr, timeout=15).json()
    asha = next((c for c in clients if c["pan"] == "AIOPG4038L"), None)
    cid = asha["id"]
    print(f"Client: {asha['name']} | DOB: {asha.get('dob')} | ID: {cid}")

    # Simulate the ZIP flow:
    # 1. Ask for DOB -> 25051969 -> _dob_to_iso -> 1969-05-25
    # 2. PATCH client with DOB
    # 3. Upload with ?dob=1969-05-25

    def _dob_to_iso(dob):
        dob = dob.strip()
        if len(dob) == 10 and dob[4] == "-" and dob[7] == "-":
            return dob
        if len(dob) == 8 and dob.isdigit():
            return f"{dob[4:]}-{dob[2:4]}-{dob[:2]}"
        dob = dob.replace("-", " ").replace("/", " ")
        parts = dob.split()
        if len(parts) == 3:
            day, month_str, year = parts
            month_map = {"jan": "01", "feb": "02", "mar": "03", "apr": "04", "may": "05", "jun": "06", "jul": "07", "aug": "08", "sep": "09", "oct": "10", "nov": "11", "dec": "12", "01": "01", "1": "01", "02": "02", "2": "02", "03": "03", "3": "03", "04": "04", "4": "04", "05": "05", "5": "05", "06": "06", "6": "06", "07": "07", "7": "07", "08": "08", "8": "08", "09": "09", "9": "09", "10": "10", "11": "11", "12": "12"}
            if month_str.lower() in month_map:
                return f"{year}-{month_map[month_str.lower()]}-{int(day):02d}"
        return dob

    dob = "25051969"
    dob_iso = _dob_to_iso(dob)
    print(f"User DOB: {dob} -> ISO: {dob_iso}")

    # Step 1: PATCH DOB
    r = httpx.patch(f"http://localhost:8001/api/v1/clients/{cid}", headers=hdr, json={"dob": dob_iso}, timeout=15)
    print(f"PATCH: {r.status_code} | {r.json().get('dob')}")

    # Step 2: Upload with ?dob= (mimics the new script logic)
    with open(r"C:\Users\Devansh\Downloads\AIOPG4038L-2026.zip", "rb") as f:
        r = httpx.post(
            f"http://localhost:8001/api/v1/imports/form26as/{cid}?dob={dob_iso}",
            headers=hdr,
            files={"file": ("AIOPG4038L-2026.zip", f, "application/zip")},
            timeout=30
        )

    print(f"Upload: {r.status_code}")
    result = r.json()
    ext = result.get("extracted", {})
    print(f"Source: {ext.get('source')}")
    print(f"PAN: {ext.get('header', {}).get('pan')}")
    print(f"AY: {ext.get('assessment_year')}")
    print(f"Deductors: {ext.get('totals', {}).get('tds_deductors_count')}")
    print(f"Errors: {result.get('errors', [])}")

asyncio.run(test())
