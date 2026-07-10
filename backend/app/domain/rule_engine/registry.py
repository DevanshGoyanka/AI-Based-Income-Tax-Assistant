"""TaxYearRulesRegistry - auto-discover AY rule modules."""
from __future__ import annotations
import importlib
import pathlib
from app.domain.rule_engine.base import TaxYearRules


_REGISTRY_CACHE: dict[str, TaxYearRules] = {}


def _module_name_for_ay(ay: str) -> str:
    return f"app.domain.rule_engine.ay_{ay.replace('-', '_')}"


def get_rules(ay: str) -> TaxYearRules:
    """Get TaxYearRules for a given AY. Cached."""
    if ay not in _REGISTRY_CACHE:
        module = importlib.import_module(_module_name_for_ay(ay))
        _REGISTRY_CACHE[ay] = module.RULES
    return _REGISTRY_CACHE[ay]


def list_known_ays() -> list[dict]:
    """Discover all ay_*.py modules in the rule_engine package."""
    pkg = pathlib.Path(__file__).parent
    results = []
    for f in pkg.glob("ay_*.py"):
        ay_str = f.stem.replace("ay_", "").replace("_", "-")
        try:
            r = get_rules(ay_str)
            results.append({
                "ay": ay_str,
                "act": r.governing_act,
                "lifecycle": r.lifecycle.value,
                "version": r.version,
            })
        except Exception:
            pass
    return results


def latest_supported() -> TaxYearRules:
    """Return the latest SUPPORTED rules."""
    supported = [r for r in (get_rules(d["ay"]) for d in list_known_ays()) if r.lifecycle.value == "SUPPORTED"]
    if not supported:
        raise RuntimeError("No SUPPORTED AY rules registered")
    return max(supported, key=lambda r: r.assessment_year)
