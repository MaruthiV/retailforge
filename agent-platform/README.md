# RetailForge agent platform

Multi-agent debugging/repair pipeline plus the incident benchmark and evaluation harness.

## layout
- `retailforge/incidents.py` — load seeded incidents, inject/revert the controlled bug
- `retailforge/workspace.py` — copy services to a temp tree, run maven module tests
- `retailforge/retrieval/` — method-level java chunking + hybrid bm25/overlap search
- `retailforge/tools.py` — the controlled tool layer (search, read, write, run_tests)
- `retailforge/graph.py` — investigation state machine + agent nodes
- `retailforge/runner.py` — retailforge / baseline_a / baseline_b / oracle runners
- `evaluation/run_eval.py` — the ablation matrix runner

## quick start
```
pip install -e .
# validate the benchmark reproduces (no api key needed)
python scripts/validate_incidents.py
# self-test the scorer with the golden patch (no api key needed)
python evaluation/run_eval.py --configs oracle
```

## real runs (needs ANTHROPIC_API_KEY)
```
export ANTHROPIC_API_KEY=...
# one incident, full pipeline
python -m retailforge.run chk-loyalty-double --config retailforge --model claude-sonnet-5

# the three-way ablation across two models, 3 repeats
python evaluation/run_eval.py \
  --configs baseline_a,baseline_b,retailforge \
  --provider anthropic \
  --models claude-sonnet-5,claude-haiku-4-5 \
  --repeats 3 --out results.json
```

## configs
- `baseline_a` — one llm, whole affected service in context, no retrieval/repro/review
- `baseline_b` — retrieval + single repair, no execution or review
- `retailforge` — intake → plan → retrieve → reproduce → repair → independent review → verify (+1 retry)
- `oracle` — feeds the golden fix to confirm the scorer works

`--provider mock` runs the whole graph offline with a stub llm (validates plumbing, does not fix bugs).
