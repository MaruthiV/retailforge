import re
from dataclasses import dataclass, field
from pathlib import Path

# method-ish signature: optional annotations handled by scanning, this matches the decl line
_METHOD = re.compile(
    r"^\s*(?:public|private|protected|static|final|\s)*[\w<>\[\],.?& ]+\s+(\w+)\s*\([^;{]*\)\s*(?:throws [\w, .]+)?\s*\{"
)
_TYPE = re.compile(r"\b(class|interface|enum|record)\s+(\w+)")


@dataclass
class Chunk:
    repo_path: str
    service: str
    language: str
    doc_type: str
    class_name: str
    method_name: str
    start_line: int
    end_line: int
    text: str
    tokens: list = field(default_factory=list)

    def citation(self):
        return f"{self.repo_path}:{self.start_line}-{self.end_line}"


def _service_of(rel_path):
    parts = Path(rel_path).parts
    head = parts[0] if parts else ""
    if head == "services" and len(parts) > 1:
        head = parts[1]
    # normalize module dir to the short service name used in incidents
    for suffix in ("-service", "-simulator"):
        if head.endswith(suffix):
            return head[: -len(suffix)]
    return head or "unknown"


def _doc_type(rel_path):
    p = rel_path.lower()
    if "/test/" in p or p.endswith("test.java"):
        return "test"
    if p.endswith(".md"):
        return "doc"
    if p.endswith(".sql"):
        return "schema"
    if p.endswith((".yml", ".yaml")):
        return "config"
    return "code"


def tokenize(text):
    out = []
    for word in re.findall(r"[A-Za-z0-9_]+", text):
        # split camelCase / snake so LoyaltyEventHandler -> loyalty event handler
        parts = re.sub(r"([a-z0-9])([A-Z])", r"\1 \2", word).replace("_", " ").split()
        for p in parts:
            p = p.lower()
            if len(p) > 1:
                out.append(p)
    return out


def _chunk_java(rel_path, text):
    lines = text.splitlines()
    class_name = ""
    for line in lines[:80]:
        m = _TYPE.search(line)
        if m:
            class_name = m.group(2)
            break
    chunks = []
    i = 0
    n = len(lines)
    while i < n:
        if _METHOD.match(lines[i]):
            name = _METHOD.match(lines[i]).group(1)
            depth = 0
            start = i
            seen = False
            while i < n:
                depth += lines[i].count("{") - lines[i].count("}")
                if "{" in lines[i]:
                    seen = True
                i += 1
                if seen and depth <= 0:
                    break
            body = "\n".join(lines[start:i])
            chunks.append((class_name, name, start + 1, i, body))
        else:
            i += 1
    if not chunks:
        chunks.append((class_name, "", 1, n, text))
    return chunks


def index_root(root, globs=("**/*.java", "**/*.md", "**/*.sql", "**/*.yml")):
    root = Path(root)
    chunks = []
    seen = set()
    for pattern in globs:
        for path in root.glob(pattern):
            if "target" in path.parts or "node_modules" in path.parts:
                continue
            if path in seen:
                continue
            seen.add(path)
            rel = str(path.relative_to(root))
            try:
                text = path.read_text()
            except Exception:
                continue
            lang = path.suffix.lstrip(".")
            service = _service_of(rel)
            doc_type = _doc_type(rel)
            if path.suffix == ".java":
                pieces = _chunk_java(rel, text)
            else:
                pieces = [("", "", 1, len(text.splitlines()) or 1, text)]
            for cls, method, start, end, body in pieces:
                c = Chunk(rel, service, lang, doc_type, cls, method, start, end, body)
                c.tokens = tokenize(body + " " + rel)
                chunks.append(c)
    return chunks
