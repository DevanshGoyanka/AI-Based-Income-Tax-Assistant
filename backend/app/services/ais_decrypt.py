"""AIS JSON Decryptor — exact replication of github.com/iambalaji-k/aisdecrypt.

The AIS Utility from ITD portal exports a .json file that is AES-256-CBC
encrypted. Decryption:

  Password  = PAN(lowercase) + "GQ39%*g" + DOB(ddmmyyyy)
  KDF       = PBKDF2-HMAC-SHA256(password, salt, dkLen=32, count=1000)
  Cipher    = AES-256-CBC(key, iv)
  Payload   = {iv_hex: 32 chars}{salt_hex: 32 chars}{base64_ciphertext}

Usage:
  decrypted = decrypt_ais(file_bytes: bytes, pan: str, dob: str) -> dict
  decrypted = decrypt_ais_str(encrypted_text: str, pan: str, dob: str) -> dict
"""
from __future__ import annotations

import base64
import json
from pathlib import Path

# PyCryptodome — same library used by the reference implementation
from Crypto.Cipher import AES
from Crypto.Protocol.KDF import PBKDF2
from Crypto.Util.Padding import unpad
from Crypto.Hash import SHA256

# Constant from the ITD AIS utility
SECRET = "GQ39%*g"


def decrypt_ais(file_bytes: bytes, pan: str, dob: str) -> dict:
    """Decrypt an encrypted AIS JSON file.

    Args:
        file_bytes: Raw bytes of the encrypted .json file (UTF-8 text).
        pan: 10-char PAN (case-insensitive; converted to lowercase internally).
        dob: Date of birth in DDMMYYYY format (e.g. "25051969").

    Returns:
        The decrypted AIS JSON as a Python dict.

    Raises:
        ValueError: If the file is malformed or decryption fails.
    """
    # Read the encrypted string from the file
    encrypted_text = file_bytes.decode("utf-8").strip()

    if len(encrypted_text) < 64:
        raise ValueError(
            "Encrypted text too short (need at least 64 chars for IV+salt). "
            "Make sure you are passing the raw encrypted .json file."
        )

    try:
        # Parse the encrypted payload
        # Format: {iv_hex: 32 chars}{salt_hex: 32 chars}{base64_ciphertext}
        iv_hex = encrypted_text[:32]
        salt_hex = encrypted_text[32:64]
        ciphertext_b64 = encrypted_text[64:].strip()

        iv = bytes.fromhex(iv_hex)
        salt = bytes.fromhex(salt_hex)
        ciphertext = base64.b64decode(ciphertext_b64)

        # Build password: PAN(lowercase) + SECRET + DOB  (no separator)
        password = (pan.strip().lower() + SECRET + dob.strip()).encode()

        # Derive key: PBKDF2-HMAC-SHA256, 1000 iterations, 32-byte output
        key = PBKDF2(
            password,
            salt,
            dkLen=32,
            count=1000,
            hmac_hash_module=SHA256,
        )

        # Decrypt with AES-256-CBC
        cipher = AES.new(key, AES.MODE_CBC, iv)
        decrypted_padded = cipher.decrypt(ciphertext)

        # Remove PKCS7 padding
        decrypted = unpad(decrypted_padded, AES.block_size)

        # Parse JSON
        decoded = decrypted.decode("utf-8")
        return json.loads(decoded)

    except (json.JSONDecodeError, ValueError, Exception) as e:
        raise ValueError(
            f"AIS decryption failed: {e}. "
            "Check that PAN and DOB are correct."
        ) from e


def decrypt_ais_str(encrypted_text: str, pan: str, dob: str) -> dict:
    """Decrypt from a string (instead of file bytes). Same as decrypt_ais."""
    return decrypt_ais(encrypted_text.encode("utf-8"), pan, dob)


def decrypt_ais_file(file_path: str, pan: str, dob: str) -> dict:
    """Convenience wrapper: read an encrypted file and decrypt it.

    Args:
        file_path: Path to the encrypted AIS .json file.
        pan: 10-char PAN.
        dob: DOB in DDMMYYYY format.

    Returns:
        Decrypted AIS JSON as dict.
    """
    path = Path(file_path)
    if not path.exists():
        raise FileNotFoundError(f"File not found: {file_path}")
    return decrypt_ais(path.read_bytes(), pan, dob)
