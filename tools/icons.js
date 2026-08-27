// Pull every icon out of the prototype and normalise it to SVG path data,
// so the Android set is geometrically identical rather than redrawn by eye.
const fs = require("fs");
const html = fs.readFileSync(
  "/Users/rrajath/code/bloggo-prototype-new-ui/design/bloggo-prototype.html", "utf8");

const n = s => {
  const v = parseFloat(s);
  return Number.isInteger(v) ? String(v) : String(+v.toFixed(3));
};

// circle / rect / line all become path data so Android has one code path
function toPath(tag, at) {
  const g = k => at[k] !== undefined ? parseFloat(at[k]) : undefined;
  if (tag === "path") return at.d;
  if (tag === "circle") {
    const cx = g("cx"), cy = g("cy"), r = g("r");
    return `M ${n(cx - r)} ${n(cy)} a ${n(r)} ${n(r)} 0 1 0 ${n(2 * r)} 0 a ${n(r)} ${n(r)} 0 1 0 ${n(-2 * r)} 0`;
  }
  if (tag === "line") return `M ${n(g("x1"))} ${n(g("y1"))} L ${n(g("x2"))} ${n(g("y2"))}`;
  if (tag === "rect") {
    const x = g("x") || 0, y = g("y") || 0, w = g("width"), h = g("height");
    let r = at.rx !== undefined ? parseFloat(at.rx) : 0;
    r = Math.min(r, w / 2, h / 2);
    if (!r) return `M ${n(x)} ${n(y)} h ${n(w)} v ${n(h)} h ${n(-w)} Z`;
    return `M ${n(x + r)} ${n(y)} h ${n(w - 2 * r)} a ${n(r)} ${n(r)} 0 0 1 ${n(r)} ${n(r)} ` +
           `v ${n(h - 2 * r)} a ${n(r)} ${n(r)} 0 0 1 ${n(-r)} ${n(r)} ` +
           `h ${n(-(w - 2 * r))} a ${n(r)} ${n(r)} 0 0 1 ${n(-r)} ${n(-r)} ` +
           `v ${n(-(h - 2 * r))} a ${n(r)} ${n(r)} 0 0 1 ${n(r)} ${n(-r)} Z`;
  }
  return null;
}

const attrs = s => {
  const o = {};
  for (const m of s.matchAll(/([a-zA-Z-]+)="([^"]*)"/g)) o[m[1]] = m[2];
  return o;
};

// only the app icon set: class="ic ..."
const icons = [];
for (const m of html.matchAll(/<svg class="ic[^"]*"[^>]*>([\s\S]*?)<\/svg>/g)) {
  const inner = m[1];
  const before = html.slice(Math.max(0, m.index - 400), m.index);
  const label =
    (before.match(/aria-label="([^"]+)"(?![\s\S]*aria-label=)/) || [])[1] ||
    (before.match(/title="([^"]+)"(?![\s\S]*title=)/) || [])[1] ||
    (before.match(/data-(?:go|sheet|fmt|visit)="([^"]+)"(?![\s\S]*data-)/) || [])[1] || "";

  const parts = [];
  let filled = false;
  for (const e of inner.matchAll(/<(path|circle|rect|line)\b([^>]*)\/?>/g)) {
    const at = attrs(e[2]);
    if (at.fill && at.fill !== "none") filled = true;
    const d = toPath(e[1], at);
    if (d) parts.push(d.replace(/\s+/g, " ").trim());
  }
  if (!parts.length || filled) continue;
  icons.push({ key: parts.join("|"), label, parts });
}

// dedupe on geometry
const byKey = new Map();
for (const i of icons) {
  if (!byKey.has(i.key)) byKey.set(i.key, { parts: i.parts, labels: new Set() });
  if (i.label) byKey.get(i.key).labels.add(i.label);
}

console.log("total icon instances:", icons.length, " unique:", byKey.size, "\n");
let idx = 0;
for (const [, v] of byKey) {
  console.log(`${String(idx++).padStart(2, "0")}  [${[...v.labels].join(" / ") || "?"}]  ${v.parts.length} path(s)`);
  v.parts.forEach(p => console.log("       " + (p.length > 96 ? p.slice(0, 96) + "…" : p)));
}
fs.writeFileSync("/private/tmp/claude-501/-Users-rrajath-code-bloggo-prototype-new-ui/bd2d2ec2-ff67-4899-8659-0ed7ebab29eb/scratchpad/icons.json",
  JSON.stringify([...byKey.values()].map(v => ({ labels: [...v.labels], parts: v.parts })), null, 2));
