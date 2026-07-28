import argparse
import json

from . import incidents
from .runner import RUNNERS


def main():
    ap = argparse.ArgumentParser(description="run one investigation on one incident")
    ap.add_argument("incident")
    ap.add_argument("--config", default="retailforge", choices=list(RUNNERS))
    ap.add_argument("--provider", default="anthropic")
    ap.add_argument("--model", default="claude-sonnet-5")
    ap.add_argument("--review-model", default="claude-haiku-4-5")
    args = ap.parse_args()

    inc = incidents.get(args.incident)
    if args.config == "oracle":
        result = RUNNERS["oracle"](inc)
    elif args.config == "retailforge":
        result = RUNNERS["retailforge"](inc, provider=args.provider, model=args.model, review_model=args.review_model)
    else:
        result = RUNNERS[args.config](inc, provider=args.provider, model=args.model)
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
