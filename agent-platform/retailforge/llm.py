import json
import os

# per 1M tokens (input, output); intro pricing where it applies
PRICING = {
    "claude-opus-4-8": (5.0, 25.0),
    "claude-sonnet-5": (2.0, 10.0),
    "claude-haiku-4-5": (1.0, 5.0),
}


def cost_of(model, in_tokens, out_tokens):
    pin, pout = PRICING.get(model, (3.0, 15.0))
    return (in_tokens / 1_000_000) * pin + (out_tokens / 1_000_000) * pout


class Usage:
    def __init__(self):
        self.calls = 0
        self.input_tokens = 0
        self.output_tokens = 0
        self.cost = 0.0

    def add(self, model, in_t, out_t):
        self.calls += 1
        self.input_tokens += in_t
        self.output_tokens += out_t
        self.cost += cost_of(model, in_t, out_t)

    def as_dict(self):
        return {"calls": self.calls, "input_tokens": self.input_tokens,
                "output_tokens": self.output_tokens, "cost": round(self.cost, 4)}


class AnthropicLLM:
    def __init__(self, model, effort="high"):
        import anthropic
        self.client = anthropic.Anthropic()
        self.model = model
        self.effort = effort
        self.usage = Usage()

    def _track(self, resp):
        u = resp.usage
        self.usage.add(self.model, u.input_tokens, u.output_tokens)

    def text(self, system, user, max_tokens=4000):
        resp = self.client.messages.create(
            model=self.model, max_tokens=max_tokens,
            thinking={"type": "adaptive"},
            output_config={"effort": self.effort},
            system=system,
            messages=[{"role": "user", "content": user}],
        )
        self._track(resp)
        return "".join(b.text for b in resp.content if b.type == "text")

    def json(self, system, user, schema, max_tokens=4000):
        resp = self.client.messages.create(
            model=self.model, max_tokens=max_tokens,
            thinking={"type": "adaptive"},
            output_config={"effort": self.effort, "format": {"type": "json_schema", "schema": schema}},
            system=system,
            messages=[{"role": "user", "content": user}],
        )
        self._track(resp)
        raw = "".join(b.text for b in resp.content if b.type == "text")
        return json.loads(raw)


# deterministic offline stand-in: exercises the whole graph without a key or spend.
# it does NOT reason, so its patches usually fail. that is honest — it validates plumbing, not intelligence.
class MockLLM:
    def __init__(self, model="mock", effort="low"):
        self.model = model
        self.usage = Usage()

    def text(self, system, user, max_tokens=4000):
        self.usage.add(self.model, len(user) // 4, 50)
        return "mock response"

    def json(self, system, user, schema, max_tokens=4000):
        self.usage.add(self.model, len(user) // 4, 50)
        return _empty_for_schema(schema)


def _empty_for_schema(schema):
    if schema.get("type") == "object":
        out = {}
        for k, v in schema.get("properties", {}).items():
            out[k] = _empty_for_schema(v)
        return out
    if schema.get("type") == "array":
        return []
    if schema.get("type") == "string":
        return ""
    if schema.get("type") in ("integer", "number"):
        return 0
    if schema.get("type") == "boolean":
        return False
    return None


def make_llm(provider, model=None, effort="high"):
    if provider == "mock":
        return MockLLM(model or "mock")
    if provider == "anthropic":
        if not os.environ.get("ANTHROPIC_API_KEY"):
            raise RuntimeError("ANTHROPIC_API_KEY not set; use provider=mock for an offline dry run")
        return AnthropicLLM(model, effort=effort)
    raise ValueError(f"unknown provider {provider}")
