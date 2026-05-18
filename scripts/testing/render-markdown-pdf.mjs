import { execFile } from "node:child_process";
import { existsSync } from "node:fs";
import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { promisify } from "node:util";
import { fileURLToPath, pathToFileURL } from "node:url";
import { marked } from "marked";

const execFileAsync = promisify(execFile);
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "../..");
const docsDir = path.join(projectRoot, "docs", "testing");
const markdownPath = path.join(docsDir, "ParkVision系统测试报告.md");
const htmlPath = path.join(docsDir, "ParkVision系统测试报告.html");
const pdfPath = path.join(docsDir, "ParkVision系统测试报告.pdf");
const cssPath = path.join(docsDir, "assets", "report.css");

function decorateHtml(body, css) {
  return `<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>ParkVision 系统测试报告</title>
    <style>${css}</style>
  </head>
  <body>
    <main class="report-body">
      ${body}
    </main>
  </body>
</html>
`;
}

function resolveChromePath() {
  const candidates = [
    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
    "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
    "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
    "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
  ];

  return candidates.find((candidate) => existsSync(candidate));
}

async function main() {
  marked.setOptions({ gfm: true, breaks: false });
  const css = await readFile(cssPath, "utf8");
  const markdown = await readFile(markdownPath, "utf8");
  const html = decorateHtml(marked.parse(markdown), css);
  await writeFile(htmlPath, html, "utf8");

  const browserPath = resolveChromePath();
  if (!browserPath) {
    throw new Error("No Chrome or Edge executable was found for PDF rendering.");
  }

  await execFileAsync(
    browserPath,
    [
      "--headless=new",
      "--disable-gpu",
      "--no-pdf-header-footer",
      `--print-to-pdf=${pdfPath}`,
      pathToFileURL(htmlPath).href,
    ],
    {
      cwd: projectRoot,
      windowsHide: true,
      maxBuffer: 8 * 1024 * 1024,
    },
  );

  console.log(`Rendered ${pdfPath}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
