// 统一离线模型入口：所有子进程使用相同工作树，报告不依赖PowerShell重定向编码。
import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import {
  readFileSync,
  readdirSync,
  mkdirSync,
  writeFileSync,
  renameSync,
} from "node:fs";
import path from "node:path";
import { performance } from "node:perf_hooks";

const root = path.resolve(import.meta.dirname, "..");
if (process.argv.length !== 2)
  throw new Error("此入口暂不接受参数；单项模拟可使用对应命令调整种子和样本数");
const content = path.join(root, "avalon-content/src/main/resources/content");
function fingerprint() {
  // 每次重新枚举，新增或删除输入文件也必须使本轮报告失效。
  const inputs = readdirSync(content, { recursive: true })
    .filter((file) => file.endsWith(".json"))
    .sort();
  const scripts = readdirSync(import.meta.dirname)
    .filter(
      (file) =>
        /^(simulate-|audit-postgame|growth-model|production-(cost-model|ledger))/.test(
          file,
        ) && file.endsWith(".mjs"),
    )
    .sort();
  const hash = createHash("sha256");
  for (const file of inputs) {
    hash.update(file.replaceAll("\\", "/"));
    hash.update("\0");
    hash.update(readFileSync(path.join(content, file)));
    hash.update("\0");
  }
  for (const file of [...scripts, "content-files.mjs"]) {
    hash.update(file);
    hash.update("\0");
    hash.update(readFileSync(path.join(import.meta.dirname, file)));
    hash.update("\0");
  }
  return hash.digest("hex");
}
const started = performance.now(),
  before = fingerprint();
const run = (args) => {
  const result = spawnSync(process.execPath, args, {
    cwd: root,
    encoding: "utf8",
    windowsHide: true,
    maxBuffer: 32 * 1024 * 1024,
    timeout: 120000,
  });
  if (result.error) throw result.error;
  if (result.status !== 0)
    throw new Error(
      `模拟失败：${args.join(" ")}\n${result.stderr}\n${result.stdout}`,
    );
  return result.stdout;
};
const tests = run([
  "--test",
  "scripts/growth-model.test.mjs",
  "scripts/production-cost-model.test.mjs",
  "scripts/production-ledger.test.mjs",
]);
process.stdout.write(tests);
const models = {};
for (const [name, file] of Object.entries({
  audit: "audit-postgame-loop.mjs",
  professions: "simulate-postgame-professions.mjs",
  supply: "simulate-postgame-supply.mjs",
  journey: "simulate-production-journey.mjs",
})) {
  const tick = performance.now();
  const result = JSON.parse(run([path.join("scripts", file)]));
  models[name] = {
    result,
    elapsedMillis: Math.round(performance.now() - tick),
  };
  console.log(`${name}: ${models[name].elapsedMillis}ms`);
}
if (fingerprint() !== before)
  throw new Error("运行期间内容或模型发生变化，报告未保存，请重新运行");
const report = {
  generatedAt: new Date().toISOString(),
  fingerprint: before,
  node: process.version,
  scope: "离线预算与情景比较，不是完整自然成长、战斗胜率或经济可达性验收",
  elapsedMillis: Math.round(performance.now() - started),
  models,
};
const directory = path.join(root, "outputs/postgame");
mkdirSync(directory, { recursive: true });
const destination = path.join(directory, "report.json"),
  temporary = destination + ".tmp";
writeFileSync(temporary, JSON.stringify(report, null, 2) + "\n", "utf8");
renameSync(temporary, destination);
console.log(
  `报告：${path.relative(root, destination)}\n指纹：${before}\n总耗时：${report.elapsedMillis}ms`,
);
