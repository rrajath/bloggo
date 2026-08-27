const fs = require("fs");
const icons = JSON.parse(fs.readFileSync(__dirname + "/icons.json", "utf8"));

// index -> Kotlin name, read off the extracted geometry
const NAMES = [
  "Search", "Push", "Branch", "ExternalLink", "ChevronLeft", "MoreVertical", "Link",
  "ListNumbered", "Plus", "FocusMode", "Commit", "Share", "Globe", "Close", "Settings",
  "Filter", "Mic", "Upload", "Check", "Framework", "File", "Image", "TextLines",
  "Taxonomy", "ChevronRight", "Download", "Sun", "Typeface", "Warning", "Hash", "Info",
  "Send", "Code", "Minus", "Aside", "Figure", "Menu", "Library", "Inbox", "Pen", "MediaImage",
];

if (NAMES.length !== icons.length) {
  throw new Error(`have ${icons.length} icons but ${NAMES.length} names`);
}

const k = s => s.replace(/\\/g, "\\\\").replace(/"/g, '\\"');

const body = icons.map((ic, i) => {
  const paths = ic.parts.map(p => `      "${k(p)}",`).join("\n");
  return `  val ${NAMES[i]}: BloggoIcon = BloggoIcon(\n    name = "${NAMES[i]}",\n    paths = listOf(\n${paths}\n    ),\n  )`;
}).join("\n\n");

const out = `package com.rrajath.bloggo.designsystem.icon

/**
 * The prototype's icon set.
 *
 * GENERATED from design/bloggo-prototype.html. Every path here is the literal
 * \`d\` attribute from the prototype's inline SVG, with \`<circle>\`, \`<rect>\` and
 * \`<line>\` converted to equivalent path data. Nothing was redrawn by eye, so the
 * Android icons are geometrically identical to the web ones.
 *
 * Regenerate rather than hand editing. See docs/DESIGN_SYSTEM.md.
 */
object BloggoIcons {

${body}

  /** Every icon, for gallery previews and tests. */
  val all: List<BloggoIcon> = listOf(
${NAMES.map(n => `    ${n},`).join("\n")}
  )
}
`;

const dest = "/Users/rrajath/code/bloggo-prototype-new-ui/android/designsystem/src/main/java/com/rrajath/bloggo/designsystem/icon/BloggoIcons.kt";
fs.mkdirSync(require("path").dirname(dest), { recursive: true });
fs.writeFileSync(dest, out);
console.log(`wrote ${icons.length} icons, ${out.split("\n").length} lines`);
