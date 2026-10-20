/**
 * 本机网络异常的独立诊断入口：浏览器反复加载多份资源，同时由Node执行正式HTTP请求。
 * 仅启动临时回环服务，不访问账号、数据库或外部站点；任何失败都保留错误并返回非零退出码。
 * 通过只说明本次简化场景没有复现，不能据此认定完整主线的网络异常已经修复。
 */
import { createServer } from "node:http";
import { createRequire } from "node:module";
import assert from "node:assert/strict";
import { readdirSync, readFileSync } from "node:fs";

const require = createRequire(
  new URL("../avalon-ui/package.json", import.meta.url),
);
const { chromium } = require("@playwright/test");
const iterations = Number(process.argv[2] ?? 60);
const mode = process.argv[3] ?? "synthetic";
assert(["synthetic", "bundle"].includes(mode), "模式只能是synthetic或bundle");
assert(
  Number.isInteger(iterations) && iterations > 0 && iterations <= 300,
  "轮数必须在1～300之间",
);
const failures = [];
// bundle只加载真实打包资源，不执行入口应用，也不伪造认证或业务接口。
// 资源路径来自构建目录的文件枚举，服务端仅接受精确匹配，不允许任意路径读取。
const assets = new Map();
if (mode === "bundle") {
  const directory = new URL("../avalon-ui/dist/assets/", import.meta.url);
  for (const name of readdirSync(directory).filter((name) =>
    /\.(js|css)$/.test(name),
  )) {
    assets.set(`/assets/${name}`, readFileSync(new URL(name, directory)));
  }
  assert(assets.size > 0, "请先构建avalon-ui，确保dist/assets包含真实资源");
}
let requests = 0;
const server = createServer((request, response) => {
  requests++;
  // 每次导航独立请求模块、样式与查询；只传固定公开数据，不记录请求头或请求体。
  response.setHeader("Cache-Control", "no-store");
  if (assets.has(request.url)) {
    response.setHeader(
      "Content-Type",
      request.url.endsWith(".css") ? "text/css" : "text/javascript",
    );
    response.end(assets.get(request.url));
  } else if (request.url.startsWith("/module/")) {
    response.setHeader("Content-Type", "text/javascript");
    response.end("export const ready = true;");
  } else if (request.url.startsWith("/style/")) {
    response.setHeader("Content-Type", "text/css");
    response.end("body { color: black; }");
  } else if (request.url.startsWith("/api/")) {
    response.setHeader("Content-Type", "application/json");
    response.end('{"ready":true}');
  } else {
    response.setHeader("Content-Type", "text/html");
    if (mode === "bundle") {
      // 模块预加载只测试浏览器读取资源的网络路径；每份资源完成后计数，防止load事件提前结束造成假通过。
      response.end(
        `<!doctype html><html><head><script>window.loaded=0;window.expected=${assets.size}</script>${[
          ...assets.keys(),
        ]
          .map(
            (path) =>
              `<link rel="${path.endsWith(".css") ? "stylesheet" : "modulepreload"}" href="${path}" onload="window.loaded++">`,
          )
          .join("")}</head><body>ready</body></html>`,
      );
      return;
    }
    response.end(
      `<!doctype html><html><head>${Array.from(
        { length: 24 },
        (_, i) =>
          `<link rel="stylesheet" href="/style/${i}"><script type="module" src="/module/${i}"></script>`,
      ).join("")}</head><body>ready</body></html>`,
    );
  }
});
let browser;
try {
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  browser = await chromium.launch();
  const page = await browser.newPage();
  page.on("requestfailed", (request) =>
    failures.push({
      path: new URL(request.url()).pathname,
      error: request.failure()?.errorText,
    }),
  );
  for (let iteration = 0; iteration < iterations; iteration++) {
    // 两种客户端共享同一服务端口，贴近自然路线中接口模拟与页面导航交错执行的条件。
    await Promise.all([
      // load已等待样式与模块脚本，无后台轮询，无须每轮额外等待networkidle的固定静默窗口。
      page.goto(`${base}/world`, { waitUntil: "load", timeout: 15000 }),
      (async () => {
        for (let i = 0; i < 30; i++) {
          const response = await fetch(`${base}/api/world`);
          assert.equal(response.status, 200);
          assert.equal((await response.json()).ready, true);
        }
      })(),
    ]);
    if (mode === "bundle") {
      await page.waitForFunction(
        () => window.loaded === window.expected,
        undefined,
        { timeout: 15000 },
      );
    }
    assert.equal(await page.locator("body").innerText(), "ready");
    assert.deepEqual(failures, [], "浏览器资源加载发生网络异常");
  }
  console.log(
    JSON.stringify({
      result: "PASS",
      mode,
      assets: assets.size,
      iterations,
      requests,
      failures,
    }),
  );
} catch (cause) {
  // 不打印认证信息；本脚本没有任何凭证，错误只包含本机路径及失败原因。
  console.error(
    JSON.stringify({
      result: "FAIL",
      requests,
      failures,
      error: String(cause),
      cause: String(cause.cause ?? ""),
    }),
  );
  process.exitCode = 1;
} finally {
  await browser?.close();
  server.closeAllConnections();
  await new Promise((resolve) => server.close(resolve));
}
