from pathlib import Path

from . import workspace


# the controlled tool layer. agents never touch the shell or fs directly, they go through here,
# so every action is typed, logged, and countable.
class Toolbox:
    def __init__(self, services_root, index):
        self.services_root = Path(services_root)
        self.index = index
        self.calls = []

    def _log(self, name, **kw):
        self.calls.append({"tool": name, **kw})

    def search_code(self, query, k=6, service=None, exclude_tests=True):
        self._log("search_code", query=query[:80], k=k, service=service)
        hits = self.index.search(query, k=k, service=service, exclude_tests=exclude_tests)
        return [{"citation": c.citation(), "path": c.repo_path, "service": c.service,
                 "class": c.class_name, "method": c.method_name, "text": c.text} for c in hits]

    def read_file(self, path):
        self._log("read_file", path=path)
        target = self.services_root / path
        return target.read_text()

    def write_file(self, path, content):
        self._log("write_file", path=path)
        target = self.services_root / path
        target.write_text(content)

    def run_tests(self, module, test=None):
        self._log("run_tests", module=module, test=test)
        return workspace.run_test(self.services_root, module, test)

    def tool_count(self):
        return len(self.calls)
