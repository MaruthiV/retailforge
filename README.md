# RetailForge

RetailForge is two things in one repo. The first is a small but believable retail backend built the way a real point of sale system is built, as a set of Spring Boot microservices with their own databases and an event bus. The second is a multi-agent system that debugs that backend. You hand it a bug report, and it retrieves the relevant code, reproduces the failure, writes a regression test, proposes a patch, has a separate agent review the patch, and only calls it fixed when the tests actually pass.

The interesting part is not "an AI that writes code from a prompt." It is a system that works across a repository, APIs, logs, a test runner, and real service boundaries, and that refuses to trust its own fix until a deterministic check confirms it.

![Agent dashboard](assets/dashboard.png)

## why I built it this way

I wanted a project that looks like the actual job of maintaining retail software: point of sale, pricing and promotions, loyalty, inventory, payments, and the messy distributed-systems bugs that show up when those services talk to each other over events. So the bugs here are not generic null-pointer toys. They are things like loyalty points getting awarded twice after a checkout retry, tax getting computed before a discount, a duplicated Kafka event decrementing stock twice, or a cancelled order never releasing its reservation.

That domain is also the point of the research angle below.

## the two halves

### the retail platform (the system under test)

Six Maven modules under `services/`, Java 17 and Spring Boot 3.3:

- `checkout-service` carts, checkout, payment retries, the transaction lifecycle, event publishing
- `pricing-service` base prices, percentage and buy-one-get-one promotions, coupon validation, a price cache
- `loyalty-service` points earn and redeem, tiers, an idempotent ledger
- `inventory-service` reserve, release, and commit stock, with oversell protection
- `payment-simulator` deterministic approve, decline, and timeout outcomes
- `common` the shared event abstraction and money helpers

Everything talks REST with Swagger, uses Postgres at runtime and H2 in tests, and publishes domain events through an `EventPublisher` seam (in memory for tests, Kafka or Redpanda for the running system). All six modules build and their 19 tests pass.

Here is the React POS that drives real transactions against those services:

![Point of sale](assets/pos.png)

### the agent platform

Python, under `agent-platform/`. The investigation runs as a small state machine (a LangGraph-style graph kept in-repo so there is no heavy dependency) with these steps:

`intake` structures the report, `planner` lays out the investigation, `retriever` does hybrid BM25 and semantic search over method-level code chunks, `reproducer` runs the failing test, `repair` proposes a minimal patch from the evidence, an independent `reviewer` judges that patch (never the same agent that wrote it), and `verify` applies the change and runs the test plus the full suite. If the reviewer rejects, the repair gets one more attempt.

The agent only ever touches the world through a controlled tool layer (`search_code`, `read_file`, `write_file`, `run_tests`), so every action is typed and counted. It is backed by the Anthropic API with a mock provider for offline dry runs.

## the actual experiment

The engineering is the portfolio piece. The research question is an ablation: which parts of the pipeline actually move the needle, and does deterministic verification (a failing test before the patch, plus an independent reviewer) reduce false fixes? I compare three configurations on the same benchmark:

- **baseline_a** one model, the whole affected service in context, no retrieval, no reproduction, no review
- **baseline_b** retrieval plus a single repair attempt, but no execution and no review
- **retailforge** the full pipeline above

There is also an **oracle** config that feeds the known-good fix, which exists to prove the scorer itself is correct.

## the benchmark

I use a controlled-injection model instead of hoping for realistic bugs. The base code is correct and its tests pass. Each incident (`incidents/definitions/*.json`) carries an exact source mutation that introduces one bug, the test that should catch it, the expected root cause, and the file that should change. Applying the mutation is the bug, reversing it is the golden fix.

There are 8 incidents today across checkout, loyalty, pricing, and inventory, including the distributed-systems ones. A validation script confirms every one of them reproduces deterministically: the target test fails with the bug applied and passes with it reverted. Adding more is just dropping in another JSON file.

## what the current run shows

I ran the whole matrix in mock mode (no API key, no cost). Resolution and root-cause accuracy are the two metrics that genuinely need a reasoning model, so they read as 0 for the mock configs and that is honest. Everything else is real and already tells a story:

| config | resolution | top-5 retrieval recall | median tool calls | reviewer |
| --- | --- | --- | --- | --- |
| baseline_a | 0% (needs a real model) | 0% | 2 | none |
| baseline_b | 0% (needs a real model) | 100% | 6 | none |
| retailforge | 0% (needs a real model) | 100% | 10 | rejects empty patches |
| oracle | 100% | 100% | 2 | none |

Retrieval works and separates the configs (baseline_a retrieves nothing, the others find the right file). Tool usage climbs as the pipeline deepens. The reviewer correctly rejects empty patches. And the oracle proves that a correct patch scores as resolved, all tests green. The one thing left is running it with a real model to get the resolution numbers, which is cheap (roughly a nickel per incident on the full pipeline).

## running it

You need Java 17+, Maven, Node, Python 3.10+, and Docker for the running system.

```bash
# build and test the retail backend
mvn -f services/pom.xml install

# bring up postgres, redis, redpanda
docker compose -f infrastructure/docker-compose.yml up -d
bash infrastructure/run-services.sh          # all five services, distributed profile

# the agent side (offline, no key needed)
cd agent-platform && pip install -e .
python scripts/validate_incidents.py         # every incident reproduces
python evaluation/run_eval.py --configs oracle          # the scorer self-test
python evaluation/run_eval.py \
  --configs baseline_a,baseline_b,retailforge,oracle \
  --provider mock --models mock                          # the full free matrix

# a real run (costs a little)
export ANTHROPIC_API_KEY=...
python -m retailforge.run chk-loyalty-double --config retailforge --model claude-haiku-4-5

# the frontends
cd apps/pos-web && npm install && npm run dev            # POS on :5173
cd apps/agent-dashboard && npm install && npm run dev    # dashboard on :5174
```

The dashboard reads `apps/agent-dashboard/public/results.json`, so copy the output of any eval run there to see it visualized.

## repo layout

```
services/         six spring boot modules (the system under test)
incidents/        controlled-injection bug definitions
agent-platform/   the agent graph, retrieval, tools, and eval harness
apps/pos-web      react point of sale
apps/agent-dashboard  react dashboard for investigations and metrics
infrastructure/   docker compose and a local run script
```

## status

The backend, the benchmark, the agent pipeline, the evaluation harness, and both frontends are built and, where it is possible without spending money, verified. The retail tests pass, the incidents reproduce, the scorer is proven with the oracle, and the full graph runs end to end in mock mode. The remaining work is the real model run for the headline numbers, a literature and novelty check, and growing the incident set. This is a work in progress, but the machinery is real and it runs.
