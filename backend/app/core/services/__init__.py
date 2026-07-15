"""Owned tax computation services.

Phase 2 — All computation is done by OUR OWN code.
OpenTax vendored code is reference/test-oracle only.

Services:
    slab_tables    — CBDT slab rates, deduction limits, rule constants per AY
    interest_234_engine — Interest u/s 234A, 234B, 234C, 234F (our own impl)
    tax_engine     — Main owned tax engine (replaces OpenTax at runtime)
"""
