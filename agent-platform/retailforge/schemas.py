def _obj(props, required):
    return {"type": "object", "additionalProperties": False,
            "properties": props, "required": required}


_str = {"type": "string"}
_str_arr = {"type": "array", "items": {"type": "string"}}

INTAKE = _obj({
    "title": _str,
    "affected_services": _str_arr,
    "symptoms": _str_arr,
    "severity": {"type": "string", "enum": ["low", "medium", "high", "critical"]},
    "required_evidence": _str_arr,
}, ["title", "affected_services", "symptoms", "severity", "required_evidence"])

PLAN = _obj({
    "steps": _str_arr,
    "primary_service": _str,
}, ["steps", "primary_service"])

CHANGE = _obj({"path": _str, "new_content": _str}, ["path", "new_content"])

REPAIR = _obj({
    "root_cause": _str,
    "files_changed": _str_arr,
    "changes": {"type": "array", "items": CHANGE},
    "risk_level": {"type": "string", "enum": ["low", "medium", "high"]},
    "assumptions": _str_arr,
}, ["root_cause", "files_changed", "changes", "risk_level", "assumptions"])

REVIEW = _obj({
    "addresses_root_cause": {"type": "boolean"},
    "within_service_boundaries": {"type": "boolean"},
    "regression_meaningful": {"type": "boolean"},
    "unnecessarily_large": {"type": "boolean"},
    "approved": {"type": "boolean"},
    "comments": _str,
}, ["addresses_root_cause", "within_service_boundaries", "regression_meaningful",
    "unnecessarily_large", "approved", "comments"])
