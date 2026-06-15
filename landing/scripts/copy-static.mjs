import { cp, mkdir } from "node:fs/promises";
import { existsSync } from "node:fs";
import { fileURLToPath } from "node:url";

const root = (p) => fileURLToPath(new URL("../" + p, import.meta.url));

const dist = root("dist");
await mkdir(dist, { recursive: true });

// Files/dirs copied verbatim into dist (styles.css is emitted by build:css).
const items = ["index.html", "robots.txt", "sitemap.xml", "assets"];

for (const item of items) {
  const src = root(item);
  if (!existsSync(src)) continue;
  await cp(src, root("dist/" + item), { recursive: true });
}

console.log("Copied static files into dist/");
