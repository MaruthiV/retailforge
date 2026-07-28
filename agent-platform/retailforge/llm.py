import json
import os
import re
import sys

# per 1M tokens (input, output); intro pricing where it applies
PRICING = {
    "claude-opus-4-8": (5.0, 25.0),
    "claude-sonnet-5": (2.0, 10.0),
    "claude-haiku-4-5": (1.0, 5.0),
}

# only these support adaptive thinking + the effort control; haiku 4.5 and older do not
THINKING_MODELS = ("claude-sonnet-5", "claude-opus-4-8", "claude-opus-4-7",
                   "claude-opus-4-6", "claude-sonnet-4-6", "claude-fable-5", "claude-mythos-5")


def _supports_thinking(model):
    return any(model.startswith(p) for p in THINKING_MODELS)


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


def _extract_json(raw):
    text = raw.strip()
    if text.startswith("```"):
        text = re.sub(r"^```[a-zA-Z]*\n?", "", text)
        text = re.sub(r"\n?```$", "", text).strip()
    start, end = text.find("{"), text.rfind("}")
    if start != -1 and end != -1:
        text = text[start:end + 1]
    return json.loads(text)


class AnthropicLLM:
    def __init__(self, model, effort="high"):
        import anthropic
        self.client = anthropic.Anthropic()
        self.model = model
        self.effort = effort
        self.usage = Usage()

    def _create(self, system, user, max_tokens):
        kw = dict(model=self.model, max_tokens=max_tokens, system=system,
                  messages=[{"role": "user", "content": user}])
        if _supports_thinking(self.model):
            kw["thinking"] = {"type": "adaptive"}
        resp = self.client.messages.create(**kw)
        u = resp.usage
        self.usage.add(self.model, u.input_tokens, u.output_tokens)
        return "".join(b.text for b in resp.content if b.type == "text")

    def text(self, system, user, max_tokens=4000):
        return self._create(system, user, max_tokens)

    def json(self, system, user, schema, max_tokens=6000):
        sys_prompt = (system + "\n\nReturn ONLY a single JSON object, no prose and no markdown fences. "
                      "It must conform to this JSON schema:\n" + json.dumps(schema))
        raw = self._create(sys_prompt, user, max_tokens)
        try:
            return _extract_json(raw)
        except Exception as e:
            print(f"[llm] json parse failed ({e}); returning empty", file=sys.stderr)
            return _empty_for_schema(schema)


# deterministic offline stand-in: exercises the whole graph without a key or spend.
# it does NOT reason, so its patches usually fail. that is honest — it validates plumbing, not intelligence.
class MockLLM:
    def __init__(self, model="mock", effort="low"):
        self.model = model
        self.usage = Usage()

    def text(self, system, user, max_tokens=4000):
        self.usage.add(self.model, len(user) // 4, 50)
        return "mock response"

    def json(self, system, user, schema, max_tokens=6000):
        self.usage.add(self.model, len(user) // 4, 50)
        return _empty_for_schema(schema)


def _empty_for_schema(schema):
    if schema.get("type") == "object":
        return {k: _empty_for_schema(v) for k, v in schema.get("properties", {}).items()}
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
