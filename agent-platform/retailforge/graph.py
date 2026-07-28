import time

from . import schemas

END = "__end__"


# minimal langgraph-style runner: named nodes, static edges, and conditional edges.
# kept in-repo so the pipeline has no heavy dependency; swap for langgraph.StateGraph later without touching nodes.
class StateGraph:
    def __init__(self):
        self.nodes = {}
        self.edges = {}
        self.conditionals = {}
        self.entry = None

    def add_node(self, name, fn):
        self.nodes[name] = fn

    def set_entry(self, name):
        self.entry = name

    def add_edge(self, a, b):
        self.edges[a] = b

    def add_conditional(self, a, fn):
        self.conditionals[a] = fn

    def invoke(self, state, ctx, max_steps=30):
        node = self.entry
        steps = 0
        while node and node != END and steps < max_steps:
            steps += 1
            self.nodes[node](state, ctx)
            if node in self.conditionals:
                node = self.conditionals[node](state, ctx)
            else:
                node = self.edges.get(node, END)
        return state


def _event(state, node, detail):
    state.setdefault("timeline", []).append({"node": node, "detail": detail, "t": time.time()})


def node_intake(state, ctx):
    inc = ctx["incident"]
    if ctx.get("use_llm_intake") and ctx.get("llm"):
        prompt = (f"A production incident was reported:\n\n{inc.incident_report}\n\n"
                  "Convert it into a structured incident record.")
        state["structured"] = ctx["llm"].json(
            "You triage retail microservice incidents. Be precise and terse.", prompt, schemas.INTAKE)
    else:
        state["structured"] = {"title": inc.title, "affected_services": inc.affected_services,
                               "symptoms": inc.symptoms, "severity": inc.severity, "required_evidence": []}
    _event(state, "intake", state["structured"]["title"])


def node_planner(state, ctx):
    inc = ctx["incident"]
    if ctx.get("llm"):
        prompt = (f"Incident: {inc.incident_report}\nAffected services: {inc.affected_services}\n"
                  "Produce a short ordered investigation plan and name the primary service to inspect.")
        state["plan"] = ctx["llm"].json(
            "You plan debugging investigations across microservices.", prompt, schemas.PLAN)
    else:
        state["plan"] = {"steps": ["retrieve code", "reproduce", "patch", "verify"],
                         "primary_service": inc.affected_services[0]}
    _event(state, "planner", f"{len(state['plan']['steps'])} steps")


def node_retriever(state, ctx):
    inc = ctx["incident"]
    tb = ctx["toolbox"]
    query = inc.incident_report + " " + " ".join(inc.symptoms) + " " + " ".join(inc.root_cause_keywords)
    hits = []
    for svc in (inc.affected_services or [None]):
        hits += tb.search_code(query, k=5, service=svc)
    # dedup by path, keep first occurrence order
    seen, unique = set(), []
    for h in hits:
        if h["path"] not in seen:
            seen.add(h["path"])
            unique.append(h)
    state["retrieved"] = unique
    state["citations"] = [h["citation"] for h in unique]
    _event(state, "retriever", f"{len(unique)} files, top={unique[0]['path'] if unique else 'none'}")


def node_reproducer(state, ctx):
    inc = ctx["incident"]
    tb = ctx["toolbox"]
    res = tb.run_tests(inc.module, inc.test)
    state["reproduction"] = {"failed_as_expected": not res["passed"], "output": res["output"][-1500:]}
    _event(state, "reproducer", "reproduced" if not res["passed"] else "could not reproduce")


def _candidate_files(state, tb, limit=4):
    files = []
    for h in state.get("retrieved", []):
        if h["path"].endswith(".java") and h["path"] not in files:
            files.append(h["path"])
        if len(files) >= limit:
            break
    return [(p, tb.read_file(p)) for p in files]


def node_repair(state, ctx):
    inc = ctx["incident"]
    tb = ctx["toolbox"]
    files = _candidate_files(state, tb)
    if not ctx.get("llm"):
        state["proposed"] = {"root_cause": "", "files_changed": [], "changes": [],
                             "risk_level": "high", "assumptions": []}
        _event(state, "repair", "no llm, empty patch")
        return
    blocks = "\n\n".join(f"=== {p} ===\n{c}" for p, c in files)
    repro = state.get("reproduction", {}).get("output", "") if ctx.get("use_reproduction") else ""
    prompt = (f"Incident: {inc.incident_report}\nSymptoms: {inc.symptoms}\n"
              + (f"\nFailing test output:\n{repro}\n" if repro else "")
              + f"\nCandidate source files:\n{blocks}\n\n"
              "Find the single root cause and fix it. Return the full new content only for files you change. "
              "Keep the change minimal and within the responsible service. Do not edit test files.")
    state["proposed"] = ctx["llm"].json(
        "You are a senior engineer fixing a bug. Output a minimal, correct patch.", prompt, schemas.REPAIR, max_tokens=12000)
    state["attempts"] = state.get("attempts", 0) + 1
    _event(state, "repair", state["proposed"].get("root_cause", "")[:80])


def node_reviewer(state, ctx):
    inc = ctx["incident"]
    proposed = state.get("proposed", {})
    reviewer_llm = ctx.get("llm_review")
    if not reviewer_llm:
        state["review"] = {"approved": True, "comments": "no reviewer configured"}
        _event(state, "reviewer", "skipped")
        return
    changed = "\n\n".join(f"=== {c['path']} ===\n{c['new_content']}" for c in proposed.get("changes", []))
    prompt = (f"Incident: {inc.incident_report}\n\nProposed root cause: {proposed.get('root_cause')}\n"
              f"Proposed changes:\n{changed}\n\n"
              "You did NOT write this patch. Judge whether it addresses the root cause, stays within the "
              "responsible service, keeps a meaningful regression guard, and is not unnecessarily large. Approve only if sound.")
    state["review"] = reviewer_llm.json(
        "You are an independent reviewer. Be skeptical. Never rubber-stamp.", prompt, schemas.REVIEW, max_tokens=3000)
    _event(state, "reviewer", "approved" if state["review"].get("approved") else "rejected")


def apply_changes(services_root, changes):
    from pathlib import Path
    written = []
    for ch in changes:
        path = ch.get("path", "")
        if ".." in path or path.startswith("/"):
            continue
        target = Path(services_root) / path
        if not str(target.resolve()).startswith(str(Path(services_root).resolve())):
            continue
        if not target.exists():
            continue
        target.write_text(ch["new_content"])
        written.append(path)
    return written


def node_verify(state, ctx):
    inc = ctx["incident"]
    tb = ctx["toolbox"]
    changes = state.get("proposed", {}).get("changes", [])
    written = apply_changes(tb.services_root, changes)
    ft = tb.run_tests(inc.module, inc.test)
    suite = tb.run_tests(inc.module)
    approved = state.get("review", {}).get("approved", True)
    resolved = bool(written) and ft["passed"] and suite["passed"] and approved
    state["verify"] = {"written": written, "fail_to_pass": ft["passed"],
                       "suite_green": suite["passed"], "approved": approved, "resolved": resolved,
                       "suite_run": suite["run"]}
    state["final_status"] = "resolved" if resolved else "unresolved"
    _event(state, "verify", state["final_status"])


def node_release(state, ctx):
    proposed = state.get("proposed", {})
    verify = state.get("verify", {})
    state["release"] = {
        "root_cause": proposed.get("root_cause", ""),
        "files_changed": verify.get("written", []),
        "risk_level": proposed.get("risk_level", "unknown"),
        "resolved": verify.get("resolved", False),
        "citations": state.get("citations", []),
    }
    _event(state, "release", state["final_status"])


def _after_reproducer(state, ctx):
    if state["reproduction"]["failed_as_expected"]:
        return "repair"
    state["final_status"] = "could_not_reproduce"
    return "release"


def _after_reviewer(state, ctx):
    approved = state.get("review", {}).get("approved", True)
    if approved or state.get("attempts", 1) >= 2 or not ctx.get("allow_retry"):
        return "verify"
    return "repair"


def build_full_graph():
    g = StateGraph()
    g.add_node("intake", node_intake)
    g.add_node("planner", node_planner)
    g.add_node("retriever", node_retriever)
    g.add_node("reproducer", node_reproducer)
    g.add_node("repair", node_repair)
    g.add_node("reviewer", node_reviewer)
    g.add_node("verify", node_verify)
    g.add_node("release", node_release)
    g.set_entry("intake")
    g.add_edge("intake", "planner")
    g.add_edge("planner", "retriever")
    g.add_edge("retriever", "reproducer")
    g.add_conditional("reproducer", _after_reproducer)
    g.add_edge("repair", "reviewer")
    g.add_conditional("reviewer", _after_reviewer)
    g.add_edge("verify", "release")
    g.add_edge("release", END)
    return g
