import re
import shutil
import subprocess
import tempfile
from pathlib import Path

from .config import MAVEN_BIN, SERVICES_DIR, subprocess_env

_SUMMARY = re.compile(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)")


def make_workspace():
    tmp = Path(tempfile.mkdtemp(prefix="retailforge-ws-"))
    dest = tmp / "services"
    shutil.copytree(SERVICES_DIR, dest, ignore=shutil.ignore_patterns("target", "*.class"))
    return dest


def cleanup(services_root):
    shutil.rmtree(Path(services_root).parent, ignore_errors=True)


def run_test(services_root, module, test=None, timeout=300):
    pom = Path(services_root) / module / "pom.xml"
    cmd = [MAVEN_BIN, "-f", str(pom), "test"]
    if test:
        cmd += ["-Dtest=" + test]
    try:
        proc = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout, env=subprocess_env())
        out = proc.stdout + proc.stderr
        rc = proc.returncode
    except subprocess.TimeoutExpired as e:
        return {"passed": False, "timed_out": True, "run": 0, "failures": 0, "errors": 0, "output": str(e)}

    run = fail = err = 0
    for m in _SUMMARY.finditer(out):
        run, fail, err = int(m.group(1)), int(m.group(2)), int(m.group(3))
    compiled = "BUILD FAILURE" not in out or run > 0
    passed = rc == 0 and fail == 0 and err == 0 and compiled
    return {
        "passed": passed,
        "timed_out": False,
        "run": run,
        "failures": fail,
        "errors": err,
        "compiled": "COMPILATION ERROR" not in out and "cannot find symbol" not in out,
        "output": out[-4000:],
    }
