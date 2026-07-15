"""
COMPLIANCE VERIFICATION REPORT
Generated: 2026-07-12
Comparing: C:\Users\Devansh\Desktop\ITR-FilingWebsite-main\backend\app
Against: C:\Users\Devansh\Desktop\ITR-FilingWebsite-main\docs\TO_OPENTAX
"""

import os
from pathlib import Path

# Expected structure from 03_Updated_Project_Structure.md
EXPECTED_STRUCTURE = {
    "core/domain": [
        "client.py",
        "filing.py", 
        "computed_return.py",
        "user.py",
        "document.py",
        "value_objects/pan.py",
        "value_objects/money.py",
        "value_objects/assessment_year.py",
        "value_objects/tax_regime.py",
        "schedules/base.py",
        "schedules/salary.py",
        "schedules/house_property.py",
        "schedules/capital_gains.py",
        "schedules/other_sources.py",
        "schedules/tds.py",
        "schedules/via.py",
        "schedules/it.py",
        "schedules/ba.py"
    ],
    "core/interfaces": [
        "repositories.py",
        "tax_engine.py",
        "itr_builder.py",
        "document_parser.py",
        "storage.py"
    ],
    "core/use_cases/filing": [
        "create_filing.py",
        "compute_tax.py",
        "generate_itr_json.py",
        "import_ais_26as.py"
    ],
    "adapters/opentax": [
        "tax_engine_adapter.py",
        "model_mapper.py",
        "response_mapper.py",
        "vendor/__init__.py"
    ],
    "adapters/repositories": [
        "filing_repository.py",
        "client_repository.py"
    ]
}

# Scan actual implementation
BASE_PATH = Path(r"C:\Users\Devansh\Desktop\ITR-FilingWebsite-main\backend\app")

def scan_directory(path):
    files = []
    for root, dirs, filenames in os.walk(path):
        dirs[:] = [d for d in dirs if d != '__pycache__']
        for f in filenames:
            if f.endswith('.py'):
                rel_path = os.path.relpath(os.path.join(root, f), BASE_PATH)
                files.append(rel_path.replace('\\', '/'))
    return files

actual_files = scan_directory(BASE_PATH)

print("=" * 80)
print("COMPLIANCE REPORT: Implementation vs Documentation")
print("=" * 80)

# Check each expected module
for module, expected_files in EXPECTED_STRUCTURE.items():
    print(f"\n[{module}]")
    for expected in expected_files:
        full_path = f"{module}/{expected}".replace('/', '\\')
        exists = any(full_path in f for f in actual_files)
        status = "✓" if exists else "✗"
        print(f"  {status} {expected}")

print("\n" + "=" * 80)
print("SUMMARY")
print("=" * 80)

total_expected = sum(len(files) for files in EXPECTED_STRUCTURE.values())
total_found = sum(
    1 for module, files in EXPECTED_STRUCTURE.items()
    for expected in files
    if any(f"{module}/{expected}".replace('/', '\\') in f for f in actual_files)
)

print(f"Expected files: {total_expected}")
print(f"Implemented: {total_found}")
print(f"Compliance: {total_found/total_expected*100:.1f}%")
print(f"\nTotal Python files in app/: {len(actual_files)}")
