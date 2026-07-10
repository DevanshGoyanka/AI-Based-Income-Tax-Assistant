"""Smart Form 26AS test script - works for any client and file.

Flow:
  1. Login with email/password
  2. Enter file path (.zip or .pdf)
  3. ZIP flow:
       - Try to detect PAN from DB by looking up any existing client
       - If client found: use their DOB from DB automatically
       - If client NOT found OR has no DOB: ask for DOB
       - Decrypt and parse
  4. PDF flow:
       - Parse file directly (no password needed)
       - Detect PAN from parsed content
       - If client found: upload to existing client
       - If client NOT found: offer to create new client with name+PAN from file
  5. Display extracted data

Run: python scripts/manual_test.py
"""
import asyncio
import httpx
import os
import sys
from pathlib import Path

BASE = "http://localhost:8001/api/v1"


# ─────────────────────────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────────────────────────

async def login(email: str, password: str) -> str:
    r = httpx.post(f"{BASE}/auth/login", json={"email": email, "password": password}, timeout=15)
    r.raise_for_status()
    return r.json()["access_token"]


async def get_clients(token: str) -> list[dict]:
    async with httpx.AsyncClient(timeout=15) as client:
        r = await client.get(f"{BASE}/clients", headers={"Authorization": f"Bearer {token}"})
        r.raise_for_status()
        return r.json()


async def lookup_client_by_pan(token: str, pan: str) -> dict | None:
    """Look up a client by PAN in the DB."""
    clients = await get_clients(token)
    for c in clients:
        if c.get("pan", "").upper() == pan.upper():
            return c
    return None


async def create_client(token: str, pan: str, name: str, dob: str = "", email: str = "") -> dict:
    """Create a new client in the DB. Sets DOB at creation time if provided."""
    payload = {"pan": pan.upper(), "name": name}
    if email:
        payload["email"] = email
    if dob:
        payload["dob"] = _dob_to_iso(dob)

    async with httpx.AsyncClient(timeout=15) as client:
        r = await client.post(
            f"{BASE}/clients",
            headers={"Authorization": f"Bearer {token}"},
            json=payload,
        )
    if r.status_code not in (200, 201):
        raise Exception(f"Failed to create client: {r.status_code} {r.text}")
    new_client = r.json()

    # If DOB was provided but creation didn't set it (API didn't accept dob field),
    # update via PATCH
    if dob and not new_client.get("dob"):
        patch_payload = {"dob": _dob_to_iso(dob)}
        r2 = await client.patch(
            f"{BASE}/clients/{new_client['id']}",
            headers={"Authorization": f"Bearer {token}"},
            json=patch_payload,
        )
        if r2.status_code == 200:
            new_client = r2.json()

    return new_client


def _dob_to_iso(dob: str) -> str:
    """Convert DOB to YYYY-MM-DD format from any reasonable input format.

    Handles:
      - DDMMYYYY         (25051969)
      - DD MMM YYYY      (25 May 1969)
      - DD-MMM-YYYY      (25-May-1969)
      - DD/MM/YYYY       (25/05/1969)
      - YYYY-MM-DD       (1969-05-25)  — already ISO, returns as-is
    """
    dob = dob.strip()
    # Already ISO
    if len(dob) == 10 and dob[4] == "-" and dob[7] == "-":
        return dob

    # Plain DDMMYYYY — no separators
    if len(dob) == 8 and dob.isdigit():
        return f"{dob[4:]}-{dob[2:4]}-{dob[:2]}"

    # With separators: split by non-digit chars
    dob = dob.replace("-", " ").replace("/", " ")
    parts = dob.split()
    if len(parts) == 3:
        day, month_str, year = parts
        month_map = {
            "jan": "01", "feb": "02", "mar": "03", "apr": "04",
            "may": "05", "jun": "06", "jul": "07", "aug": "08",
            "sep": "09", "oct": "10", "nov": "11", "dec": "12",
            "01": "01", "1": "01", "02": "02", "2": "02",
            "03": "03", "3": "03", "04": "04", "4": "04",
            "05": "05", "5": "05", "06": "06", "6": "06",
            "07": "07", "7": "07", "08": "08", "8": "08",
            "09": "09", "9": "09", "10": "10", "11": "11", "12": "12",
        }
        if month_str.lower() in month_map:
            return f"{year}-{month_map[month_str.lower()]}-{int(day):02d}"

    # Fallback: return as-is (let the API deal with it)
    return dob


async def import_form26as(token: str, client_id: str, file_path: str, dob: str) -> dict:
    """Import Form26AS file for a client. dob is used as password for ZIP files.

    For ZIP files, passes ?dob=YYYY-MM-DD to the API so the password is
    determined by user input, not the client's DB record.
    """
    fname = os.path.basename(file_path)
    fmt = "application/zip" if fname.lower().endswith(".zip") else "application/pdf"

    with open(file_path, "rb") as f:
        async with httpx.AsyncClient(timeout=60) as client:
            url = f"{BASE}/imports/form26as/{client_id}"
            # Pass DOB as query param for ZIP files (overrides DB DOB check)
            if fname.lower().endswith(".zip") and dob:
                url = f"{url}?dob={_dob_to_iso(dob)}"
            r = await client.post(
                url,
                headers={"Authorization": f"Bearer {token}"},
                files={"file": (fname, f, fmt)},
                timeout=60
            )

    if r.status_code == 400:
        # Try to extract error message
        try:
            detail = r.json().get("detail", r.text)
        except Exception:
            detail = r.text
        raise Exception(f"400 Bad Request: {detail}")

    r.raise_for_status()
    return r.json()


def print_results(ext: dict):
    """Pretty-print parsed Form26AS results."""
    hdr = ext.get("header", {})
    tot = ext.get("totals", {})
    parts = ext.get("parts", {})

    print()
    print("═" * 72)
    source = ext.get("source", "?").upper()
    status_icon = "✅" if source in ("ZIP", "PDF") else "❌"
    print(f"  {status_icon} SOURCE: {source}  |  AY: {ext.get('assessment_year', 'N/A')}  |  PAN: {hdr.get('pan', 'N/A')}")
    print("═" * 72)
    print(f"  Name           : {hdr.get('name', 'N/A')}")
    print(f"  Financial Year : {hdr.get('financial_year', 'N/A')}")
    print(f"  PAN Status     : {hdr.get('current_status', 'N/A')}")
    print("─" * 72)
    print(f"  TDS Deductors  : {tot.get('tds_deductors_count', 0)}")
    print(f"  TCS Collectors : {tot.get('tcs_collectors_count', 0)}")
    print(f"  Total Amount   : Rs.{tot.get('total_amount_paid_credited', 0):>16,.2f}")
    print(f"  Total TDS      : Rs.{tot.get('total_tds_deducted', 0):>16,.2f}")
    print(f"  Total TCS      : Rs.{tot.get('total_tcs_collected', 0):>16,.2f}")
    print(f"  Total Refund   : Rs.{tot.get('total_refund_amount', 0):>16,.2f}")
    print("─" * 72)

    # Parts breakdown
    if parts:
        print("  PARTS BREAKDOWN:")
        rows = [
            ("I",   "TDS Deductors",             "part1_tds_deductors"),
            ("II",  "TDS 15G/15H",               "part2_tds_15g15h"),
            ("III", "194B/194R/194S/194BA",      "part3_transactions_194b_194r_194s_194ba"),
            ("IV",  "TDS 194IA/IB/M/S-Seller",   "part4_tds_194ia_194ib_194m_194s_seller"),
            ("V",   "26QE-Seller",               "part5_transactions_26qe_seller"),
            ("VI",  "TCS Collectors",             "part6_tcs_collectors"),
            ("VII", "Paid Refunds",               "part7_paid_refunds"),
            ("VIII","TDS 194IA/IB/M/S-Buyer",    "part8_tds_194ia_194ib_194m_194s_buyer"),
            ("IX",  "26QE-Buyer",                "part9_transactions_26qe_buyer"),
            ("X",   "TDS/TCS Defaults",           "part10_tds_tcs_defaults"),
        ]
        for num, label, key in rows:
            count = parts.get(key, 0)
            mark = "✅" if count > 0 else "  "
            print(f"    {mark} Part-{num:<2} {label:<30}: {count:>4}")

    # TDS entries
    tds_entries = ext.get("tds_entries", [])
    if tds_entries:
        print()
        print("  TDS ENTRIES:")
        print("  " + "─" * 70)
        for e in tds_entries:
            txns = e.get("transactions", [])
            # Field name mapping: importer uses total_amount/total_tds at deductor level
            amt = e.get("total_amount_paid_credited") or e.get("total_amount") or e.get("amount_paid_credited") or 0
            tds = e.get("total_tds_deducted") or e.get("total_tds") or e.get("tax_deducted") or 0
            sr = e.get("sr_no", "?")
            name = e.get("deductor_name", "N/A")
            tan = e.get("tan", "N/A")
            print(f"  {sr}. {name}")
            print(f"     TAN: {tan}  |  Amount: Rs.{amt:>13,.2f}  |  TDS: Rs.{tds:>10,.2f}  |  Txns: {len(txns)}")

            for t in txns[:4]:
                t_amt = t.get("amount_paid_credited") or t.get("amount") or 0
                t_tds = t.get("total_tds") or t.get("tax_deducted") or 0
                t_sec = t.get("section", "?")
                t_dt = t.get("transaction_date", "")
                t_sts = t.get("status_of_booking", "")
                t_sr = t.get("sr_no", "?")
                print(f"       {t_sr:>3}. Sec-{t_sec:<6} {t_dt:<12} Rs.{t_amt:>12,.2f} / TDS Rs.{t_tds:>8,.2f} [{t_sts}]")
            if len(txns) > 4:
                print(f"       ... and {len(txns) - 4} more transactions")

    # TCS entries
    tcs_entries = ext.get("tcs_entries", [])
    if tcs_entries:
        print()
        print("  TCS ENTRIES:")
        print("  " + "─" * 70)
        for e in tcs_entries:
            txns = e.get("transactions", [])
            amt = e.get("total_amount_paid_debited") or 0
            tcs = e.get("total_tcs_collected") or 0
            print(f"  {e.get('sr_no', '?')}. {e.get('collector_name', 'N/A')}")
            print(f"     TAN: {e.get('tan', 'N/A')}  |  Amount: Rs.{amt:>13,.2f}  |  TCS: Rs.{tcs:>10,.2f}  |  Txns: {len(txns)}")

    print()
    print("═" * 72)
    total_txns = sum(len(e.get("transactions", [])) for e in tds_entries)
    total_txns += sum(len(e.get("transactions", [])) for e in tcs_entries)
    print(f"  ✅ DONE!  {len(tds_entries)} deductors  |  {len(tcs_entries)} collectors  |  {total_txns} transactions")


def ask_dob(client_name: str = "client") -> str:
    """Ask user for DOB interactively."""
    print()
    print("  ┌─────────────────────────────────────────────────────────────────┐")
    print("  │  Enter DOB (used as ZIP password in DDMMYYYY format)           │")
    print("  │  Examples: 08022002  |  15061993  |  14 Jul 1974 → 14071974   │")
    print("  └─────────────────────────────────────────────────────────────────┘")
    while True:
        dob = input("  DOB (DDMMYYYY): ").strip()
        if not dob:
            print("  ⚠️  DOB cannot be empty for ZIP files.")
            continue
        if len(dob) == 8 and dob.isdigit():
            return dob
        # Try to parse "DD MMM YYYY" or "DD-MM-YYYY" format
        dob_clean = dob.replace("-", " ").replace("/", " ")
        parts = dob_clean.split()
        if len(parts) == 3:
            day, month_str, year = parts
            month_map = {"jan": "01", "feb": "02", "mar": "03", "apr": "04",
                         "may": "05", "jun": "06", "jul": "07", "aug": "08",
                         "sep": "09", "oct": "10", "nov": "11", "dec": "12",
                         "01": "01", "1": "01", "02": "02", "2": "02",
                         "03": "03", "3": "03", "04": "04", "4": "04",
                         "05": "05", "5": "05", "06": "06", "6": "06",
                         "07": "07", "7": "07", "08": "08", "8": "08",
                         "09": "09", "9": "09", "10": "10", "11": "11", "12": "12"}
            if month_str.lower() in month_map and len(day) <= 2 and len(year) == 4:
                return f"{int(day):02d}{month_map[month_str.lower()]}{year}"
        print("  ⚠️  Invalid format. Use DDMMYYYY (e.g. 08022002) or DD MMM YYYY (e.g. 08 Feb 2002).")


def ask_choice(options: list[str], prompt: str = "Enter choice") -> int:
    """Ask user to choose from numbered options."""
    for i, opt in enumerate(options, 1):
        print(f"  [{i}] {opt}")
    while True:
        val = input(f"  {prompt} [1-{len(options)}]: ").strip()
        if val.isdigit() and 1 <= int(val) <= len(options):
            return int(val)
        print(f"  ⚠️  Enter a number between 1 and {len(options)}.")


# ─────────────────────────────────────────────────────────────────────────────
# ZIP flow: detect PAN, look up client, get/create DOB, decrypt
# ─────────────────────────────────────────────────────────────────────────────

async def handle_zip_flow(token: str, file_path: str) -> dict:
    """Handle ZIP file: detect PAN from DB, get/create DOB, decrypt, parse."""
    print()
    print("  📦 ZIP FILE DETECTED - encrypted, requires DOB as password")
    print()

    # Step 1: Ask user for PAN since we can't read ZIP without password
    pan_input = input("  Enter client's PAN (10 chars): ").strip().upper()
    if not pan_input or len(pan_input) != 10:
        print("  ❌ Invalid PAN. Must be 10 characters.")
        sys.exit(1)

    # Step 2: Look up client in DB
    client = await lookup_client_by_pan(token, pan_input)

    if client:
        cid = client["id"]
        cname = client["name"]
        print(f"  ✅ Client found in DB: {cname} (PAN: {pan_input})")

        # Step 3: Check if client has DOB
        if client.get("dob"):
            dob = client["dob"]
            if isinstance(dob, str) and "-" in dob:
                # Convert YYYY-MM-DD to DDMMYYYY
                yyyy, mm, dd = dob.split("-")
                dob = f"{dd}{mm}{yyyy[-2:]}"
            elif isinstance(dob, str):
                dob = dob.replace("-", "")
            print(f"  🔑 DOB from DB: {dob}  →  Will use as ZIP password")
            try:
                result = await import_form26as(token, cid, file_path, dob)
                ext = result.get("extracted", {})
                if ext.get("source") == "zip":
                    print_results(ext)
                    return result
                elif ext.get("source") == "pdf":
                    # Try format but show warning
                    print("  ⚠️  ZIP decrypted but content detected as PDF format. Proceeding anyway...")
                    print_results(ext)
                    return result
                else:
                    print(f"  ❌ Parse failed. Errors: {result.get('errors', [])}")
                    print(f"     Response: {ext}")
                    return result
            except Exception as e:
                err = str(e)
                if "400 Bad Request" in err and "DOB" in err:
                    print(f"  ❌ Wrong password! DB DOB ({dob}) didn't work.")
                elif "400 Bad Request" in err:
                    print(f"  ❌ Upload error: {err}")
                else:
                    print(f"  ❌ Upload failed: {err}")
        else:
            print(f"  ⚠️  Client found but has no DOB in DB.")
    else:
        print(f"  ℹ️  Client not found in DB for PAN: {pan_input}")
        print("     Will create new client with DOB.")
        cid = None

    # Step 4: Ask for DOB
    dob = ask_dob(client["name"] if client else pan_input)

    # Step 5: Create or update client so DOB is in DB for the upload
    if not client:
        name = input("  Enter client's full name: ").strip()
        email = input("  Enter email (optional, press ENTER to skip): ").strip()
        try:
            new_client = await create_client(token, pan_input, name or pan_input, dob, email)
            cid = new_client["id"]
            print(f"  ✅ Client created with DOB: {name} (ID: {cid})")
        except Exception as e:
            print(f"  ❌ Could not create client: {e}")
            cid = input("  Enter client ID manually (or Ctrl+C to abort): ").strip()
    elif not client.get("dob"):
        # Update existing client's DOB via PATCH before uploading
        print(f"  🔑 Saving DOB {dob} to client record...")
        from datetime import date as date_cls
        dob_iso = _dob_to_iso(dob)  # "YYYY-MM-DD"
        y, m, d = dob_iso.split("-")
        dob_date = date_cls(int(y), int(m), int(d))
        async with httpx.AsyncClient(timeout=15) as patch_client:
            r = await patch_client.patch(
                f"{BASE}/clients/{client['id']}",
                headers={"Authorization": f"Bearer {token}"},
                json={"dob": dob_date.isoformat()},
            )
            if r.status_code == 200:
                print(f"  ✅ DOB saved for {client['name']}")
            else:
                print(f"  ⚠️  Could not save DOB ({r.status_code}): {r.text[:200]}")
                print(f"     Will still try import with provided DOB...")

    # Step 6: Try import — DOB from user input is the ZIP password regardless of DB state
    print(f"  🔓 Attempting ZIP import with DOB password: {dob}")
    try:
        result = await import_form26as(token, cid, file_path, dob)
        ext = result.get("extracted", {})
        if ext.get("source") == "zip":
            print_results(ext)
            return result
        elif ext.get("source") == "pdf":
            print("  ⚠️  ZIP decrypted but content detected as PDF format. Proceeding anyway...")
            print_results(ext)
            return result
        else:
            print(f"  ❌ Parse failed. Errors: {result.get('errors', [])}")
            return result
    except Exception as e:
        print(f"  ❌ Import failed: {e}")
        sys.exit(1)


# ─────────────────────────────────────────────────────────────────────────────
# PDF flow: parse directly, detect PAN, look up/create client
# ─────────────────────────────────────────────────────────────────────────────

async def handle_pdf_flow(token: str, file_path: str) -> dict:
    """Handle PDF file: parse directly, detect PAN, look up/create client."""
    print()
    print("  📄 PDF FILE DETECTED - not encrypted, parsing directly...")

    # ── Step 1: Parse the PDF using the first client that has a DOB ─────────
    # We need any valid client_id to upload. Find one with DOB (they all pass).
    r = httpx.post(f"{BASE}/auth/login", json={"email": "test@example.com", "password": "Test123!@#"}, timeout=15)
    r.raise_for_status()
    test_token = r.json()["access_token"]
    clients = await get_clients(test_token)

    if not clients:
        print("  ❌ No clients in DB. Create a client first.")
        sys.exit(1)

    # PDF doesn't need DOB — use first client just to get a valid client_id
    placeholder = clients[0]
    print(f"  ℹ️  Parsing with: {placeholder['name']}")

    # DOB placeholder (empty for PDF — no password needed)
    dob = ""

    with open(file_path, "rb") as f:
        try:
            result = await import_form26as(test_token, placeholder["id"], file_path, "")
        except httpx.HTTPStatusError as e:
            print(f"  ❌ Parse failed ({e.response.status_code}): {e.response.text[:200]}")
            sys.exit(1)
        except Exception as e:
            print(f"  ❌ Parse failed: {e}")
            sys.exit(1)

    ext = result.get("extracted", {})
    if not ext or ext.get("source") not in ("pdf", "zip"):
        print(f"  ❌ PDF parse failed: {result.get('errors', [])}")
        print(f"     Response: {ext}")
        sys.exit(1)

    # ── Step 2: Detect PAN + name from parsed content ──────────────────────
    hdr = ext.get("header", {})
    file_pan = hdr.get("pan", "").strip().upper()
    file_name = hdr.get("name", "").strip()
    file_ay = ext.get("assessment_year", "")
    print(f"  ✅ PDF parsed successfully!")
    print(f"     PAN: {file_pan}  |  Name: {file_name}  |  AY: {file_ay}")

    # ── Step 3: Look up client by PAN ───────────────────────────────────────
    client = await lookup_client_by_pan(token, file_pan) if file_pan else None

    if client:
        cid = client["id"]
        cname = client["name"]
        print(f"  ✅ Client found: {cname} (ID: {cid})")
    else:
        if file_pan:
            print(f"  ℹ️  Client not found for PAN: {file_pan}")

        print()
        opts = ["Create new client with name from PDF", "Enter client ID manually", "Skip upload — just show parsed data"]
        choice = ask_choice(opts, "What would you like to do")

        if choice == 1:
            if not file_name:
                file_name = input("  Enter client name: ").strip() or file_pan
            email = input("  Enter email (optional, press ENTER to skip): ").strip()
            try:
                # DOB will be empty string for PDF (no password needed)
                new_client = await create_client(token, file_pan, file_name, dob, email)
                cid = new_client["id"]
                cname = new_client["name"]
                print(f"  ✅ Client created: {cname} (ID: {cid})")
                client = new_client
            except Exception as e:
                print(f"  ❌ Failed to create client: {e}")
                cid = input("  Enter client ID manually: ").strip()
                cname = "Unknown"
        elif choice == 2:
            cid = input("  Enter existing client ID: ").strip()
            cname = "Unknown"
        else:
            print_results(ext)
            return result

    # ── Step 4: Upload to the correct client ────────────────────────────────
    print(f"  📤 Uploading to client: {cname} (ID: {cid})")
    try:
        result = await import_form26as(token, cid, file_path, "")
        ext = result.get("extracted", {})
        print_results(ext)
        return result
    except httpx.HTTPStatusError as e:
        print(f"  ❌ Upload failed ({e.response.status_code}): {e.response.text[:200]}")
        sys.exit(1)
    except Exception as e:
        print(f"  ❌ Upload failed: {e}")
        sys.exit(1)


# ─────────────────────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────────────────────

async def main():
    print()
    print("╔" + "═" * 70 + "╗")
    print("║" + "  FORM 26AS SMART TEST  -  Any client, any file".center(70) + "║")
    print("╚" + "═" * 70 + "╝")

    # ── Step 1: Login ────────────────────────────────────────────────────────
    print()
    print("[1] LOGIN")
    print("─" * 40)
    email = input("  Email    [test@example.com]: ").strip() or "test@example.com"
    password = input("  Password [Test123!@#]    : ").strip() or "Test123!@#"

    try:
        token = await login(email, password)
        print(f"  ✅ Logged in as {email}!")
    except httpx.HTTPStatusError as e:
        print(f"  ❌ Login failed ({e.response.status_code}): {e.response.text[:100]}")
        sys.exit(1)
    except Exception as e:
        print(f"  ❌ Login failed: {e}")
        sys.exit(1)

    # ── Step 2: File path ────────────────────────────────────────────────────
    print()
    print("[2] FILE PATH")
    print("─" * 40)
    # Expand ~ and strip outer quotes (user often pastes paths with "..." wrapping)
    def _clean_path(p: str) -> str:
        p = p.strip()
        p = os.path.expanduser(p)
        # Strip outer matching quotes (both "..." and '...')
        if len(p) >= 2 and ((p[0] == '"' and p[-1] == '"') or (p[0] == "'" and p[-1] == "'")):
            p = p[1:-1]
        return p

    file_path = input("  Enter full path to Form26AS file (.zip or .pdf): ").strip()
    if not file_path:
        print("  ❌ No file path entered.")
        sys.exit(1)

    file_path = _clean_path(file_path)
    if not Path(file_path).exists():
        print(f"  ❌ File not found: {file_path}")
        sys.exit(1)

    fname = os.path.basename(file_path)
    is_zip = fname.lower().endswith(".zip")
    is_pdf = fname.lower().endswith(".pdf")

    if not is_zip and not is_pdf:
        print(f"  ❌ Unsupported file type. Use .zip or .pdf")
        sys.exit(1)

    print(f"  📄 File: {fname}")
    print(f"  📦 Type: {'ZIP (TRACES - encrypted)' if is_zip else 'PDF (e-Filing Portal - unencrypted)'}")

    # ── Step 3: Process based on type ────────────────────────────────────────
    print()
    print("[3] PROCESSING...")
    print("─" * 40)

    if is_zip:
        await handle_zip_flow(token, file_path)
    else:
        await handle_pdf_flow(token, file_path)

    # ── Done ─────────────────────────────────────────────────────────────────
    print()
    retry = input("  Test another file? [y/N]: ").strip().lower()
    if retry == "y":
        await main()


async def quick_test_all():
    """Run quick automated tests against existing clients."""
    import sys as _sys
    _sys.path.insert(0, ".")
    from app.infra.db.base import AsyncSessionLocal
    from sqlalchemy import text

    print("╔" + "═" * 70 + "╗")
    print("║" + "  QUICK AUTO TEST  -  All clients with DOB".center(70) + "║")
    print("╚" + "═" * 70 + "╝")

    # Get clients with DOB from DB
    async with AsyncSessionLocal() as db:
        result = await db.execute(text("""
            SELECT id, pan, name, dob FROM clients WHERE dob IS NOT NULL ORDER BY name
        """))
        clients = [dict(row._mapping) for row in result.fetchall()]

    if not clients:
        print("  ❌ No clients with DOB found.")
        return

    r = httpx.post(f"{BASE}/auth/login", json={"email": "test@example.com", "password": "Test123!@#"}, timeout=15)
    token = r.json()["access_token"]

    print(f"  Found {len(clients)} clients with DOB")
    print()

    files = {
        "zip": r"C:\Users\Devansh\Desktop\E-FILE_karo\COVPC5929M-2026.zip",
        "pdf": r"C:\Users\Devansh\Desktop\E-FILE_karo\ACUPG3482G-2025.pdf",
    }

    all_pass = True

    for ft, fp in files.items():
        if not Path(fp).exists():
            print(f"  ⏭️  {ft.upper()} file not found: {fp}")
            continue

        print(f"  ── {ft.upper()} TESTS ──")
        for c in clients:
            dob = c["dob"]
            yyyy, mm, dd = str(dob).split("-")
            password = f"{dd}{mm}{yyyy[-2:]}"

            with open(fp, "rb") as f:
                try:
                    r = httpx.post(
                        f"{BASE}/imports/form26as/{c['id']}",
                        headers={"Authorization": f"Bearer {token}"},
                        files={"file": (Path(fp).name, f, "application/zip" if ft == "zip" else "application/pdf")},
                        timeout=30
                    )
                    ext = r.json().get("extracted", {})
                    passed = ext.get("source") == ft
                    mark = "Y" if passed else "N"
                    pan = ext.get("header", {}).get("pan", "-")
                    ay = ext.get("assessment_year", "-")
                    ded = ext.get("totals", {}).get("tds_deductors_count", 0)
                    err = len(r.json().get("errors", []))
                    print(f"  [{mark}] {c['name']:<28} | PAN={pan:<12} | AY={ay:<8} | Ded={ded} | Err={err}")
                    if not passed:
                        all_pass = False
                except Exception as e:
                    print(f"  [N] {c['name']:<28} | ERROR: {e}")
                    all_pass = False
        print()

    print("═" * 72)
    print(f"  {'✅ ALL PASSED!' if all_pass else '❌ SOME FAILED'}")
    print("═" * 72)


if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1 and sys.argv[1] == "--auto":
        asyncio.run(quick_test_all())
    else:
        try:
            asyncio.run(main())
        except KeyboardInterrupt:
            print("\n\n  👋 Goodbye!")
