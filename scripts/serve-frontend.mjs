import { createReadStream, existsSync, statSync } from "node:fs";
import { createServer } from "node:http";
import { extname, join, resolve } from "node:path";

const root = resolve("frontend/dist");
const port = Number(process.env.PORT || 5173);
const backendUrl = process.env.BACKEND_URL || "http://localhost:8080";

const contentTypes = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".jpg": "image/jpeg",
};

function resolveFile(url) {
  const cleanUrl = decodeURIComponent(url.split("?")[0]);
  const directPath = join(root, cleanUrl);
  if (existsSync(directPath) && statSync(directPath).isFile()) {
    return directPath;
  }
  return join(root, "index.html");
}

async function proxyApi(request, response) {
  const targetUrl = new URL(request.url || "/", backendUrl);
  const headers = { ...request.headers };
  delete headers.host;

  const proxied = await fetch(targetUrl, {
    method: request.method,
    headers,
    body: request.method === "GET" || request.method === "HEAD" ? undefined : request,
    duplex: "half",
  });

  response.writeHead(proxied.status, Object.fromEntries(proxied.headers.entries()));
  response.end(Buffer.from(await proxied.arrayBuffer()));
}

createServer((request, response) => {
  if ((request.url || "").startsWith("/api") || (request.url || "").startsWith("/actuator")) {
    proxyApi(request, response).catch((error) => {
      response.writeHead(502, { "Content-Type": "application/json; charset=utf-8" });
      response.end(JSON.stringify({ success: false, code: "PROXY_ERROR", message: error.message }));
    });
    return;
  }

  const file = resolveFile(request.url || "/");
  const type = contentTypes[extname(file)] || "application/octet-stream";
  response.writeHead(200, { "Content-Type": type });
  createReadStream(file).pipe(response);
}).listen(port, "0.0.0.0", () => {
  console.log(`ParkVision frontend preview: http://localhost:${port}`);
});
