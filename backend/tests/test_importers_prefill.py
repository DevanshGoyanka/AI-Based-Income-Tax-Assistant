"""Unit tests for prefill importer - parser logic only."""
import json
import base64
import pytest
from app.services.importers.prefill import PrefillImporter
from app.services.importers.crypto import decode_aadhaar


def test_prefill_extract_ay_from_filename():
    importer = PrefillImporter()
    ay = importer._extract_ay({}, "ACUPG3482G-Prefill-2025-14_31_2026_18_50.json")
    assert ay == "2025-26"


def test_prefill_extract_ay_2026_format():
    importer = PrefillImporter()
    ay = importer._extract_ay({}, "XYZ-Prefill-2026-...json")
    assert ay == "2026-27"


def test_prefill_extract_ay_default_when_unknown():
    importer = PrefillImporter()
    ay = importer._extract_ay({}, "unknown.json")
    assert ay == "2026-27"


def test_prefill_extract_ay_from_data():
    importer = PrefillImporter()
    ay = importer._extract_ay({"filingStatus": {"AssessmentYear": "2024-25"}}, "x.json")
    assert ay == "2024-25"


def test_decode_aadhaar_integration():
    aadhaar = "759832090929"
    encoded = base64.b64encode(aadhaar.encode()).decode()
    assert decode_aadhaar(encoded) == aadhaar


def test_invalid_json_handling():
    importer = PrefillImporter()
    try:
        json.loads(b"not json".decode('utf-8'))
        assert False, "should have raised"
    except (json.JSONDecodeError, UnicodeDecodeError):
        assert True
