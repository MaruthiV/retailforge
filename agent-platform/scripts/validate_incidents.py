import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from retailforge import incidents, workspace


def validate(inc):
    ws = workspace.make_workspace()
    try:
        incidents.inject(inc, ws)
        injected = workspace.run_test(ws, inc.module, inc.test)
        incidents.revert(inc, ws)
        reverted = workspace.run_test(ws, inc.module, inc.test)
    finally:
        workspace.cleanup(ws)

    fails_when_injected = not injected["passed"]
    passes_when_fixed = reverted["passed"]
    ok = fails_when_injected and passes_when_fixed
    return ok, injected, reverted


def main():
    all_inc = incidents.load_all()
    print(f"validating {len(all_inc)} incidents\n")
    results = []
    for inc in all_inc:
        try:
            ok, injected, reverted = validate(inc)
        except Exception as e:
            print(f"[ERROR] {inc.id}: {e}")
            results.append((inc.id, False))
            continue
        status = "OK  " if ok else "FAIL"
        print(f"[{status}] {inc.id:26s} injected={'fail' if not injected['passed'] else 'pass'} "
              f"reverted={'pass' if reverted['passed'] else 'fail'} "
              f"(run={reverted['run']})")
        results.append((inc.id, ok))
    good = sum(1 for _, ok in results if ok)
    print(f"\n{good}/{len(results)} incidents valid")
    return 0 if good == len(results) else 1


if __name__ == "__main__":
    sys.exit(main())
