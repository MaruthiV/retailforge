import argparse
import json
import sys
import time
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from retailforge import incidents
from retailforge.runner import RUNNERS
from evaluation import metrics


def run_cell(config, inc, provider, model, review_model):
    if config == "oracle":
        return RUNNERS["oracle"](inc)
    if config == "retailforge":
        return RUNNERS["retailforge"](inc, provider=provider, model=model, review_model=review_model)
    return RUNNERS[config](inc, provider=provider, model=model)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--configs", default="oracle")
    ap.add_argument("--provider", default="mock")
    ap.add_argument("--models", default="claude-sonnet-5")
    ap.add_argument("--review-model", default="claude-haiku-4-5")
    ap.add_argument("--incidents", default="all")
    ap.add_argument("--repeats", type=int, default=1)
    ap.add_argument("--out", default="")
    args = ap.parse_args()

    all_inc = incidents.load_all()
    if args.incidents != "all":
        wanted = set(args.incidents.split(","))
        all_inc = [i for i in all_inc if i.id in wanted]
    configs = args.configs.split(",")
    models = args.models.split(",")

    results = []
    for config in configs:
        cfg_models = ["oracle"] if config == "oracle" else models
        for model in cfg_models:
            for rep in range(args.repeats):
                for inc in all_inc:
                    t0 = time.time()
                    try:
                        r = run_cell(config, inc, args.provider, model, args.review_model)
                    except Exception as e:
                        r = {"incident": inc.id, "config": config, "model": model, "resolved": False,
                             "error": str(e), "retrieval_top5_hit": False, "root_cause_correct": False,
                             "fail_to_pass": False, "suite_green": False, "reviewer_approved": None,
                             "tool_calls": 0, "usage": {}, "seconds": round(time.time() - t0, 1)}
                    r["repeat"] = rep
                    results.append(r)
                    tag = f"{config}/{model}" if config != "oracle" else "oracle"
                    mark = "RESOLVED" if r.get("resolved") else ("ERROR" if r.get("error") else "unresolved")
                    print(f"[{tag:28s}] {inc.id:24s} {mark:10s} "
                          f"ftp={r.get('fail_to_pass')} recall={r.get('retrieval_top5_hit')} "
                          f"tools={r.get('tool_calls')} ${r.get('usage', {}).get('cost', 0)}")

    print("\n=== aggregate by config/model ===")
    grouped = defaultdict(list)
    for r in results:
        grouped[metrics.group_key(r)].append(r)
    summary = {}
    for key, rs in sorted(grouped.items()):
        agg = metrics.aggregate(rs)
        summary["/".join(key)] = agg
        print(f"\n{key[0]} [{key[1]}]")
        for k, v in agg.items():
            print(f"    {k}: {v}")

    out = args.out or f"eval-results-{int(time.time())}.json"
    Path(out).write_text(json.dumps({"results": results, "summary": summary}, indent=2))
    print(f"\nwrote {out}")


if __name__ == "__main__":
    main()
