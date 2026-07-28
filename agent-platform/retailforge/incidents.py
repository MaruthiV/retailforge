import json
from dataclasses import dataclass
from pathlib import Path

from .config import INCIDENTS_DIR


@dataclass
class Mutation:
    file: str
    search: str
    replace: str


@dataclass
class Incident:
    id: str
    title: str
    category: str
    severity: str
    affected_services: list
    incident_report: str
    symptoms: list
    expected_root_cause: str
    expected_files: list
    root_cause_keywords: list
    fail_to_pass: dict
    mutations: list

    @property
    def module(self):
        return self.fail_to_pass["module"]

    @property
    def test(self):
        return self.fail_to_pass["test"]


def load_incident(path):
    data = json.loads(Path(path).read_text())
    data["mutations"] = [Mutation(**m) for m in data["mutations"]]
    return Incident(**data)


def load_all(directory=INCIDENTS_DIR):
    return [load_incident(p) for p in sorted(Path(directory).glob("*.json"))]


def get(incident_id, directory=INCIDENTS_DIR):
    for inc in load_all(directory):
        if inc.id == incident_id:
            return inc
    raise KeyError(f"no incident {incident_id}")


# apply the bug into a services tree; returns True if every mutation matched exactly once
def inject(incident, services_root):
    for m in incident.mutations:
        target = Path(services_root) / m.file
        text = target.read_text()
        count = text.count(m.search)
        if count != 1:
            raise ValueError(f"{incident.id}: search matched {count} times in {m.file}")
        target.write_text(text.replace(m.search, m.replace, 1))
    return True


# the golden fix is just the inverse of the injection
def revert(incident, services_root):
    for m in incident.mutations:
        target = Path(services_root) / m.file
        text = target.read_text()
        if m.replace not in text:
            raise ValueError(f"{incident.id}: cannot revert, replacement not found in {m.file}")
        target.write_text(text.replace(m.replace, m.search, 1))
    return True


def golden_patch_text(incident):
    parts = []
    for m in incident.mutations:
        parts.append(f"--- fix in {m.file}\n- {m.replace.strip()}\n+ {m.search.strip()}")
    return "\n\n".join(parts)
