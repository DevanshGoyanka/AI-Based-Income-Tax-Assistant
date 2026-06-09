#!/usr/bin/env python3
"""Test AIS JSON decryption with provided PAN/DOB."""

import base64
import json
from pathlib import Path
from Crypto.Cipher import AES
from Crypto.Hash import SHA256
from Crypto.Protocol.KDF import PBKDF2
from Crypto.Util.Padding import unpad

PASSWORD_MIDDLE = "GQ39%*g"

def decrypt_ais_json(filepath, pan, dob):
    """Decrypt AIS JSON file."""
    raw = Path(filepath).read_text(encoding="utf-8").strip()

    # Some tools export as JSON string
    if raw.startswith('"') and raw.endswith('"'):
        raw = json.loads(raw)

    if len(raw) < 64:
        raise ValueError("File too short for IV + salt + ciphertext")

    iv_hex = raw[:32]
    salt_hex = raw[32:64]
    ciphertext_part = raw[64:]

    try:
        iv = bytes.fromhex(iv_hex)
        salt = bytes.fromhex(salt_hex)
    except ValueError as exc:
        raise ValueError("First 64 chars must be hex (IV + salt)") from exc

    try:
        ciphertext = base64.b64decode(ciphertext_part, validate=True)
    except Exception:
        try:
            ciphertext = bytes.fromhex(ciphertext_part)
        except ValueError as exc:
            raise ValueError("Ciphertext must be Base64 or hex") from exc

    if not ciphertext:
        raise ValueError("Ciphertext payload is empty")

    # Build password candidates
    pan_lower = pan.lower()
    pan_upper = pan.upper()

    candidates = []
    seen = set()

    def add(c):
        if c not in seen:
            seen.add(c)
            candidates.append(c)

    add(f"{pan_lower}{PASSWORD_MIDDLE}{dob}")
    add(f"{pan_lower}{dob}")
    add(f"{pan_upper}{dob}")

    plaintext = None
    last_error = None

    for candidate in candidates:
        try:
            key = PBKDF2(
                candidate.encode("utf-8"),
                salt,
                dkLen=32,
                count=1000,
                hmac_hash_module=SHA256,
            )
            cipher = AES.new(key, AES.MODE_CBC, iv)
            plaintext = unpad(cipher.decrypt(ciphertext), AES.block_size).decode("utf-8")
            print(f"SUCCESS with password: {candidate}")
            break
        except Exception as exc:
            last_error = exc
            print(f"FAILED with password: {candidate} -> {exc}")

    if plaintext is None:
        raise ValueError(f"All password candidates failed. Last error: {last_error}")

    return plaintext

if __name__ == "__main__":
    import sys

    filepath = r"C:\Users\Devansh\Desktop\ITR-FilingWebsite-main\import_prefill\XXXPG3482X_2024-25_AIS_08062026.json"
    pan = "ACUPG3482G"
    dob = "14061974"

    print(f"File: {filepath}")
    print(f"PAN: {pan}")
    print(f"DOB: {dob}")
    print(f"Password candidates:")
    print(f"  1. {pan.lower()}{PASSWORD_MIDDLE}{dob}")
    print(f"  2. {pan.lower()}{dob}")
    print(f"  3. {pan.upper()}{dob}")
    print()
    print("=" * 60)
    print("Decrypting...")
    print("=" * 60)

    try:
        result = decrypt_ais_json(filepath, pan, dob)
        print()
        print("=" * 60)
        print("DECRYPTION SUCCESSFUL!")
        print("=" * 60)
        print(f"Decrypted JSON length: {len(result)} chars")
        print()

        # Parse and pretty-print
        data = json.loads(result)
        print("JSON Structure (top-level keys):")
        for key in data.keys():
            print(f"  - {key}")
        print()

        # Save the decrypted file
        out_path = Path(filepath).with_name("XXXPG3482X_2024-25_AIS_decrypted.json")
        Path(out_path).write_text(result, encoding="utf-8")
        print(f"Saved to: {out_path}")

        # Print key AIS data summary
        print()
        print("=" * 60)
        print("AIS DATA SUMMARY:")
        print("=" * 60)

        # Try to find key information
        def find_value(obj, *keys, default=None):
            try:
                for k in keys:
                    if isinstance(obj, dict):
                        obj = obj.get(k, default)
                    elif isinstance(obj, list) and len(obj) > 0:
                        obj = obj[0].get(k, default) if isinstance(obj[0], dict) else default
                    else:
                        return default
                return obj
            except:
                return default

        # Print first level of each section
        for key, value in data.items():
            if isinstance(value, dict):
                subkeys = list(value.keys())[:5]
                print(f"\n{key}:")
                for k in subkeys:
                    v = value[k]
                    if isinstance(v, str):
                        print(f"  {k}: {v[:80]}{'...' if len(str(v)) > 80 else ''}")
                    elif isinstance(v, (int, float)):
                        print(f"  {k}: {v}")
                    elif isinstance(v, list):
                        print(f"  {k}: [list of {len(v)} items]")
                    elif isinstance(v, dict):
                        print(f"  {k}: {{dict with keys: {list(v.keys())[:5]}}}")
                    else:
                        print(f"  {k}: {type(v).__name__}")

    except Exception as exc:
        print(f"\nDECRYPTION FAILED: {exc}")
        import traceback
        traceback.print_exc()
