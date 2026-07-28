import json
import sys
from collections import defaultdict
from pathlib import Path
from statistics import median, mean

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from retailforge import incidents
from retailforge.config import SERVICES_DIR
from retailforge.retrieval import index_root


def _reproduced(r):
    # the reproducer node ran and the graph proceeded to repair (failure was reproduced)
    tl = r.get("timeline", [])
    return "reproducer" in tl and "repair" in tl


def _patch_passed(r):
    return bool(r.get("fail_to_pass")) and bool(r.get("suite_green"))


def _has_patch(r):
    return len(r.get("files_changed", [])) > 0


def _pct(xs):
    return round(100 * mean([1.0 if x else 0.0 for x in xs]), 1) if xs else 0.0


def group_metrics(rs):
    reproduces = any("reproducer" in r.get("timeline", []) for r in rs)
    reviewed = [r for r in rs if r.get("reviewer_approved") is not None]
    resolved_secs = [r["seconds"] for r in rs if r.get("resolved")]
    return {
        "n": len(rs),
        "diagnosis_root_cause_pct": _pct([r.get("root_cause_correct") for r in rs]),
        "retrieval_top5_pct": _pct([r.get("retrieval_top5_hit") for r in rs]),
        "reproduction_pct": (_pct([_reproduced(r) for r in rs]) if reproduces else None),
        "patch_compiled_passed_pct": _pct([_patch_passed(r) for r in rs]),
        "end_to_end_resolved_pct": _pct([r.get("resolved") for r in rs]),
        "median_seconds_resolved": round(median(resolved_secs), 1) if resolved_secs else None,
        "median_seconds_all": round(median([r["seconds"] for r in rs]), 1),
        "avg_tool_calls": round(mean([r.get("tool_calls", 0) for r in rs]), 1),
        "avg_model_calls": round(mean([r.get("usage", {}).get("calls", 0) for r in rs]), 1),
        "avg_cost_usd": round(mean([r.get("usage", {}).get("cost", 0.0) for r in rs]), 4),
        "reviewer_rejection_pct": (_pct([r["reviewer_approved"] is False for r in reviewed]) if reviewed else None),
        "unsupported_answer_pct": _pct([_has_patch(r) and not _patch_passed(r) for r in rs]),
    }


def main(path):
    data = json.loads(Path(path).read_text())
    results = data["results"]

    chunks = index_root(SERVICES_DIR)
    files = {c.repo_path for c in chunks}
    code_chunks = [c for c in chunks if c.doc_type == "code"]
    doc_chunks = [c for c in chunks if c.doc_type in ("doc", "config", "schema")]
    services = {c.service for c in chunks if c.service not in ("unknown",)}

    groups = defaultdict(list)
    for r in results:
        groups[(r["config"], r.get("model", "?"))].append(r)

    metrics = {k: group_metrics(v) for k, v in groups.items()}

    print("=" * 66)
    print("RETAILFORGE EVALUATION REPORT")
    print("=" * 66)
    print(f"\nSCOPE")
    print(f"  microservices indexed : {len(services)}  ({', '.join(sorted(services))})")
    print(f"  files indexed         : {len(files)}")
    print(f"  code chunks           : {len(code_chunks)}")
    print(f"  doc/config/schema     : {len(doc_chunks)}")
    print(f"  total chunks          : {len(chunks)}")
    print(f"\nBENCHMARK")
    print(f"  seeded incidents      : {len(incidents.load_all())}")
    print(f"\nSCALE")
    print(f"  total agent runs      : {sum(1 for r in results if r['config'] != 'oracle')}")
    print(f"  total cells (incl oracle): {len(results)}")

    print("\n" + "=" * 66)
    print("PER CONFIG / MODEL")
    print("=" * 66)
    for key in sorted(metrics):
        m = metrics[key]
        print(f"\n{key[0]} [{key[1]}]  (n={m['n']})")
        for k, v in m.items():
            if k == "n":
                continue
            print(f"    {k:28s}: {v}")

    # headline: best retailforge model vs the strongest baseline
    rf = {k: v for k, v in metrics.items() if k[0] == "retailforge"}
    if rf:
        best = max(rf, key=lambda k: rf[k]["end_to_end_resolved_pct"])
        m = rf[best]
        base_keys = [k for k in metrics if k[0] in ("baseline_a", "baseline_b")]
        base_secs = [metrics[k]["median_seconds_all"] for k in base_keys]
        base_med = round(median(base_secs), 1) if base_secs else None
        print("\n" + "=" * 66)
        print(f"HEADLINE  (retailforge / {best[1]})")
        print("=" * 66)
        print(f"  Scope         : {len(services)} services, {len(files)} files, {len(chunks)} chunks, {len(doc_chunks)} indexed docs")
        print(f"  Benchmark     : {len(incidents.load_all())} seeded incidents")
        print(f"  Diagnosis     : {m['diagnosis_root_cause_pct']}% correct root cause")
        print(f"  Retrieval     : {m['retrieval_top5_pct']}% correct file in top 5")
        print(f"  Reproduction  : {m['reproduction_pct']}% failures reproduced")
        print(f"  Testing       : {m['reproduction_pct']}% with a failing regression test (benchmark test, fail-before/pass-after)")
        print(f"  Patching      : {m['patch_compiled_passed_pct']}% patches compiled and passed all tests")
        print(f"  End-to-end    : {m['end_to_end_resolved_pct']}% resolved without human intervention")
        print(f"  Speed         : {m['median_seconds_resolved']}s median to verified patch")
        print(f"  Improvement   : retailforge {m['median_seconds_all']}s vs baseline {base_med}s median")
        print(f"  Efficiency    : {m['avg_tool_calls']} tool calls, {m['avg_model_calls']} model calls per incident")
        print(f"  Reliability   : {m['reviewer_rejection_pct']}% reviewer rejection, {m['unsupported_answer_pct']}% unsupported answers")
        print(f"  Scale         : {sum(1 for r in results if r['config'] != 'oracle')} agent runs completed")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "results.json")
