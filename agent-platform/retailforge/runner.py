import time
from pathlib import Path

from . import graph, incidents, schemas, workspace
from .llm import make_llm
from .retrieval import HybridIndex, index_root
from .tools import Toolbox

MODULE_OF = {"checkout": "checkout-service", "loyalty": "loyalty-service",
             "pricing": "pricing-service", "inventory": "inventory-service", "payment": "payment-simulator"}


def _score(inc, state, toolbox, usage, seconds):
    citations = [c.split(":")[0] for c in state.get("citations", [])]
    expected = set(inc.expected_files)
    top5 = citations[:5]
    retrieval_hit = any(e in top5 for e in expected)
    proposed = state.get("proposed", {})
    rc = (proposed.get("root_cause", "") or "").lower()
    kw_hits = sum(1 for k in inc.root_cause_keywords if k.lower() in rc)
    verify = state.get("verify", {})
    resolved = verify.get("resolved", False)
    return {
        "incident": inc.id,
        "resolved": resolved,
        "fail_to_pass": verify.get("fail_to_pass", False),
        "suite_green": verify.get("suite_green", False),
        "reviewer_approved": verify.get("approved", None),
        "retrieval_top5_hit": retrieval_hit,
        "root_cause_keyword_hits": kw_hits,
        "root_cause_correct": kw_hits >= 2,
        "files_changed": verify.get("written", []),
        "expected_files": inc.expected_files,
        "tool_calls": toolbox.tool_count(),
        "usage": usage.as_dict() if usage else {},
        "seconds": round(seconds, 1),
        "timeline": [e["node"] for e in state.get("timeline", [])],
    }


def _setup(inc):
    ws = workspace.make_workspace()
    incidents.inject(inc, ws)
    idx = HybridIndex(index_root(ws))
    tb = Toolbox(ws, idx)
    return ws, tb


def _combined_usage(*llms):
    from .llm import Usage
    u = Usage()
    for l in llms:
        if l is None:
            continue
        u.calls += l.usage.calls
        u.input_tokens += l.usage.input_tokens
        u.output_tokens += l.usage.output_tokens
        u.cost += l.usage.cost
    return u


def run_retailforge(inc, provider="anthropic", model="claude-sonnet-5", review_model=None, effort="high"):
    review_model = review_model or model
    ws, tb = _setup(inc)
    llm = make_llm(provider, model, effort)
    llm_review = make_llm(provider, review_model, effort)
    ctx = {"incident": inc, "toolbox": tb, "llm": llm, "llm_review": llm_review,
           "use_llm_intake": True, "use_reproduction": True, "allow_retry": True}
    state = {}
    t0 = time.time()
    try:
        graph.build_full_graph().invoke(state, ctx)
    finally:
        seconds = time.time() - t0
        result = _score(inc, state, tb, _combined_usage(llm, llm_review), seconds)
        workspace.cleanup(ws)
    result["config"] = "retailforge"
    result["model"] = model
    return result


def run_baseline_b(inc, provider="anthropic", model="claude-sonnet-5", effort="high"):
    ws, tb = _setup(inc)
    llm = make_llm(provider, model, effort)
    ctx = {"incident": inc, "toolbox": tb, "llm": llm, "llm_review": None,
           "use_reproduction": False, "allow_retry": False}
    state = {}
    t0 = time.time()
    try:
        graph.node_retriever(state, ctx)
        graph.node_repair(state, ctx)
        graph.node_verify(state, ctx)
        graph.node_release(state, ctx)
    finally:
        seconds = time.time() - t0
        result = _score(inc, state, tb, _combined_usage(llm), seconds)
        workspace.cleanup(ws)
    result["config"] = "baseline_b"
    result["model"] = model
    return result


def run_baseline_a(inc, provider="anthropic", model="claude-sonnet-5", effort="high"):
    ws, tb = _setup(inc)
    llm = make_llm(provider, model, effort)
    # single shot with the whole affected service in context, no retrieval / repro / review
    files = []
    for svc in inc.affected_services:
        module = MODULE_OF.get(svc)
        if not module:
            continue
        src = ws / module / "src" / "main" / "java"
        for p in sorted(src.rglob("*.java")):
            files.append((str(p.relative_to(ws)), p.read_text()))
    blocks = "\n\n".join(f"=== {p} ===\n{c}" for p, c in files)
    state = {"citations": []}
    t0 = time.time()
    try:
        prompt = (f"Incident: {inc.incident_report}\nSymptoms: {inc.symptoms}\n\n"
                  f"Full source of the affected service(s):\n{blocks}\n\n"
                  "Diagnose the single root cause and fix it. Return full new content only for files you change.")
        state["proposed"] = llm.json("You fix bugs from an incident report in one shot.", prompt,
                                     schemas.REPAIR, max_tokens=12000)
        graph.node_verify(state, {"incident": inc, "toolbox": tb})
        graph.node_release(state, {"incident": inc, "toolbox": tb})
    finally:
        seconds = time.time() - t0
        result = _score(inc, state, tb, _combined_usage(llm), seconds)
        workspace.cleanup(ws)
    result["config"] = "baseline_a"
    result["model"] = model
    return result


# harness self-test: feed the golden fix as the patch and confirm the scorer marks it resolved.
def run_oracle(inc):
    ws, tb = _setup(inc)
    changes = []
    for m in inc.mutations:
        text = (ws / m.file).read_text()
        changes.append({"path": m.file, "new_content": text.replace(m.replace, m.search, 1)})
    state = {"proposed": {"root_cause": inc.expected_root_cause, "changes": changes},
             "review": {"approved": True}, "citations": [f + ":1-1" for f in inc.expected_files]}
    t0 = time.time()
    try:
        graph.node_verify(state, {"incident": inc, "toolbox": tb})
    finally:
        seconds = time.time() - t0
        result = _score(inc, state, tb, None, seconds)
        workspace.cleanup(ws)
    result["config"] = "oracle"
    result["model"] = "oracle"
    return result


RUNNERS = {"retailforge": run_retailforge, "baseline_a": run_baseline_a,
           "baseline_b": run_baseline_b, "oracle": run_oracle}
