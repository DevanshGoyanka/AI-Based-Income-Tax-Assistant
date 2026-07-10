"""Unit tests for prefill import parsers."""
import json
import pytest
from app.services.prefill_import import (
    parse_26as_json, parse_ais_json, parse_tis_json,
    summarize_tds, _safe_int,
)


def test_safe_int():
    assert _safe_int(None) == 0
    assert _safe_int("") == 0
    assert _safe_int(123) == 123
    assert _safe_int(123.45) == 123
    assert _safe_int("456") == 456
    assert _safe_int("789.99") == 789
    assert _safe_int("invalid") == 0


def test_parse_26as():
    with open("tests/fixtures/form_26as_sample.json") as f:
        raw = json.load(f)
    records = parse_26as_json(raw, "ABCDE1234F", "2026-27")
    assert len(records) == 3
    assert records[0].deductor_tan == "DELA12345E"
    assert records[0].deductor_name == "ABC Corp Ltd"
    assert records[0].section_code == "192"
    assert records[0].amount_paid == 1200000
    assert records[0].tax_deducted == 120000
    assert records[0].quarter == "Q4"
    assert records[1].section_code == "194A"
    assert records[2].section_code == "194K"


def test_parse_ais():
    with open("tests/fixtures/ais_sample.json") as f:
        raw = json.load(f)
    tds, info = parse_ais_json(raw, "XYZAB5678C", "2026-27")
    assert len(tds) == 2
    assert tds[0].section_code == "192"
    assert tds[0].deductor_tan == "BANG12345A"
    assert tds[0].tax_deducted == 150000
    assert tds[1].section_code == "194J"
    assert tds[1].tax_deducted == 30000

    assert len(info) == 3
    assert info[0].info_type == "interest"
    assert info[0].amount == 45000
    assert info[1].info_type == "interest"
    assert info[2].info_type == "dividend"
    assert info[2].amount == 8000


def test_parse_tis():
    with open("tests/fixtures/tis_sample.json") as f:
        raw = json.load(f)
    records = parse_tis_json(raw, "LMNOP9876D", "2026-27")
    assert len(records) == 2
    assert records[0].deductor_tan == "HYDD12345C"
    assert records[0].section_code == "194C"
    assert records[0].amount_paid == 500000
    assert records[0].tax_deducted == 10000
    assert records[1].deductor_tan == "KOLB23456D"
    assert records[1].section_code == "194A"


def test_summarize_tds():
    from app.schemas.prefill import TDSRecord
    records = [
        TDSRecord(
            deductor_tan="TAN1234567",
            deductor_name="A",
            section_code="192",
            amount_paid=100000,
            tax_deducted=10000,
            tax_deposited=10000,
        ),
        TDSRecord(
            deductor_tan="TAN1234567",
            deductor_name="A",
            section_code="192",
            amount_paid=50000,
            tax_deducted=5000,
            tax_deposited=5000,
        ),
        TDSRecord(
            deductor_tan="TAN9876543",
            deductor_name="B",
            section_code="194A",
            amount_paid=20000,
            tax_deducted=2000,
            tax_deposited=2000,
        ),
    ]
    summary = summarize_tds(records)
    assert summary.total_records == 3
    assert summary.total_tax_deducted == 17000
    assert summary.total_amount_paid == 170000
    assert summary.deductor_count == 2
    assert set(summary.sections_seen) == {"192", "194A"}
