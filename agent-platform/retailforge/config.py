import os
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SERVICES_DIR = REPO_ROOT / "services"
INCIDENTS_DIR = REPO_ROOT / "incidents" / "definitions"
DOCS_DIR = REPO_ROOT / "docs"

# maven lives under homebrew on this machine, make sure subprocesses can find it
MAVEN_BIN = os.environ.get("RETAILFORGE_MVN", "/opt/homebrew/bin/mvn")
EXTRA_PATH = "/opt/homebrew/bin"

# two models for the ablation, cheapest-capable first
DEFAULT_MODELS = ["claude-sonnet-5", "claude-haiku-4-5"]
DEFAULT_EFFORT = "high"

# languages/globs we index for retrieval
CODE_GLOBS = ["**/*.java", "**/*.md", "**/*.yml", "**/*.sql"]


def subprocess_env():
    env = dict(os.environ)
    env["PATH"] = EXTRA_PATH + os.pathsep + env.get("PATH", "")
    return env
