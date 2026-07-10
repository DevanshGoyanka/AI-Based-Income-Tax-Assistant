"""Unit tests for crypto utilities."""
import base64
import pytest
from app.services.importers.crypto import decode_aadhaar, decrypt_ais_json


def test_decode_aadhaar_valid():
    aadhaar = "759832090929"
    encoded = base64.b64encode(aadhaar.encode()).decode()
    assert decode_aadhaar(encoded) == aadhaar


def test_decode_aadhaar_invalid_returns_original():
    invalid = "not-base64-#$%"
    assert decode_aadhaar(invalid) == invalid


def test_decode_aadhaar_empty():
    assert decode_aadhaar("") == ""
    assert decode_aadhaar(None) == ""


def test_decrypt_ais_json_too_short():
    with pytest.raises(ValueError, match="too short"):
        decrypt_ais_json("short", "password")


def test_decrypt_ais_json_invalid_password():
    # Real encrypted sample would be needed for full test
    # For now, test structure validation
    fake_encrypted = "a" * 64 + base64.b64encode(b"x" * 32).decode()
    with pytest.raises(Exception):  # Will fail at decryption stage
        decrypt_ais_json(fake_encrypted, "wrong")
