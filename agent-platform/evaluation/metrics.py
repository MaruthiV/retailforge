from statistics import median


def _rate(xs):
    return round(sum(1 for x in xs if x) / len(xs), 3) if xs else 0.0


def aggregate(results):
    if not results:
        return {}
    resolved = [r["resolved"] for r in results]
    retrieval = [r["retrieval_top5_hit"] for r in results]
    rc = [r["root_cause_correct"] for r in results]
    ftp = [r["fail_to_pass"] for r in results]
    tool_calls = [r["tool_calls"] for r in results]
    seconds = [r["seconds"] for r in results]
    costs = [r["usage"].get("cost", 0.0) for r in results]
    reviewed = [r for r in results if r.get("reviewer_approved") is not None]
    rejected = [r for r in reviewed if r["reviewer_approved"] is False]
    return {
        "n": len(results),
        "resolution_rate": _rate(resolved),
        "patch_fail_to_pass_rate": _rate(ftp),
        "root_cause_accuracy": _rate(rc),
        "retrieval_top5_recall": _rate(retrieval),
        "median_tool_calls": median(tool_calls) if tool_calls else 0,
        "median_seconds": round(median(seconds), 1) if seconds else 0,
        "median_cost_usd": round(median(costs), 4) if costs else 0.0,
        "total_cost_usd": round(sum(costs), 4),
        "reviewer_rejection_rate": _rate([True] * len(rejected) + [False] * (len(reviewed) - len(rejected))) if reviewed else None,
    }


def group_key(r):
    return (r["config"], r.get("model", "?"))
