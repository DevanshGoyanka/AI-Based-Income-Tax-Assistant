"""Debug mutual fund parsing."""
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app.services.ais_pdf_parser import _identify_section

# Test the section identification for mutual funds
test_cases = [
    ("SFT-18(Pur)", "Purchase of mutual funds (SFT - 018)"),
    ("SFT-018", "Mutual fund"),
    ("SFT-15", "Dividend income"),
]

for info_code, desc in test_cases:
    section = _identify_section(info_code, desc)
    print(f"Info Code: {info_code:20} | Desc: {desc:45} | Section: {section}")
