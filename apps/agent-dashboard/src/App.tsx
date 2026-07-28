import { useEffect, useMemo, useState } from "react";

type Result = {
  incident: string;
  config: string;
  model: string;
  resolved: boolean;
  fail_to_pass: boolean;
  suite_green: boolean;
  reviewer_approved: boolean | null;
  retrieval_top5_hit: boolean;
  root_cause_correct: boolean;
  files_changed: string[];
  expected_files: string[];
  tool_calls: number;
  usage: { cost?: number };
  seconds: number;
  timeline: string[];
  error?: string;
};

type Payload = { results: Result[]; summary: Record<string, Record<string, number>> };

const NODE_LABELS: Record<string, string> = {
  intake: "Incident received",
  planner: "Plan generated",
  retriever: "Code retrieved",
  reproducer: "Failure reproduced",
  repair: "Patch proposed",
  reviewer: "Review completed",
  verify: "Tests run",
  release: "Release readiness",
};

export default function App() {
  const [data, setData] = useState<Payload | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    fetch("/results.json")
      .then((r) => r.json())
      .then(setData)
      .catch(() => setData({ results: [], summary: {} }));
  }, []);

  const incidents = useMemo(() => {
    if (!data) return [];
    const ids = Array.from(new Set(data.results.map((r) => r.incident)));
    return ids;
  }, [data]);

  const current = useMemo(
    () => data?.results.find((r) => r.incident === selected) ?? null,
    [data, selected]
  );

  if (!data) return <div className="app">loading…</div>;

  return (
    <div className="app">
      <header>
        <h1>RetailForge</h1>
        <span className="sub">agentic debugging &amp; repair · agent dashboard</span>
      </header>

      <section className="metrics">
        <h2>Metrics by configuration</h2>
        <table>
          <thead>
            <tr>
              <th>config</th>
              <th>n</th>
              <th>resolution</th>
              <th>root-cause acc</th>
              <th>top-5 recall</th>
              <th>med tools</th>
              <th>med cost</th>
              <th>reviewer reject</th>
            </tr>
          </thead>
          <tbody>
            {Object.entries(data.summary).map(([k, m]) => (
              <tr key={k}>
                <td>{k}</td>
                <td>{m.n}</td>
                <td>{pct(m.resolution_rate)}</td>
                <td>{pct(m.root_cause_accuracy)}</td>
                <td>{pct(m.retrieval_top5_recall)}</td>
                <td>{m.median_tool_calls}</td>
                <td>${m.median_cost_usd}</td>
                <td>{m.reviewer_rejection_rate == null ? "—" : pct(m.reviewer_rejection_rate)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {Object.keys(data.summary).length === 0 && (
          <p className="hint">
            No results yet. Run <code>python evaluation/run_eval.py</code> and copy the JSON to{" "}
            <code>public/results.json</code>.
          </p>
        )}
      </section>

      <div className="grid">
        <section className="list">
          <h2>Incidents</h2>
          {incidents.map((id) => {
            const r = data.results.find((x) => x.incident === id)!;
            return (
              <button
                key={id}
                className={"row" + (selected === id ? " active" : "")}
                onClick={() => setSelected(id)}
              >
                <span className={"dot " + (r.resolved ? "ok" : "bad")} />
                <span className="id">{id}</span>
                <span className="cfg">{r.config}</span>
              </button>
            );
          })}
        </section>

        <section className="detail">
          {current ? <Investigation r={current} /> : <p className="hint">select an incident</p>}
        </section>
      </div>
    </div>
  );
}

function Investigation({ r }: { r: Result }) {
  return (
    <div>
      <h2>{r.incident}</h2>
      <div className={"status " + (r.resolved ? "ok" : "bad")}>
        {r.resolved ? "RESOLVED" : r.error ? "ERROR" : "UNRESOLVED"}
      </div>

      <h3>Investigation timeline</h3>
      <ol className="timeline">
        {r.timeline.map((n, i) => (
          <li key={i}>{NODE_LABELS[n] ?? n}</li>
        ))}
      </ol>

      <h3>Evidence</h3>
      <ul className="evidence">
        <li>fail-to-pass test: <b>{yesno(r.fail_to_pass)}</b></li>
        <li>full suite green: <b>{yesno(r.suite_green)}</b></li>
        <li>reviewer approved: <b>{r.reviewer_approved == null ? "—" : yesno(r.reviewer_approved)}</b></li>
        <li>retrieval hit expected file (top-5): <b>{yesno(r.retrieval_top5_hit)}</b></li>
        <li>tool calls: <b>{r.tool_calls}</b> · duration: <b>{r.seconds}s</b> · cost: <b>${r.usage.cost ?? 0}</b></li>
      </ul>

      <h3>Patch</h3>
      <div className="patch">
        <div><span className="lbl">files changed</span> {r.files_changed.join(", ") || "none"}</div>
        <div><span className="lbl">expected files</span> {r.expected_files.join(", ")}</div>
      </div>
    </div>
  );
}

const pct = (x: number) => `${Math.round((x ?? 0) * 100)}%`;
const yesno = (b: boolean) => (b ? "yes" : "no");
