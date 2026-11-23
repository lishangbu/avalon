import assert from "node:assert/strict";
import { readContent } from "./content-files.mjs";
import { gatheringRecoveryBudget } from "./growth-model.mjs";
import { recoveryRoute } from "./production-ledger.mjs";

// 参数为明确情景输入，不能默认虚构伙伴属性或将采集期望次数直接向下取整。
const options = {};
for (let index = 2; index < process.argv.length; index += 2) {
  const key = process.argv[index];
  assert(
    ["--node", "--rounds", "--health", "--mana"].includes(key),
    `未知参数：${key}`,
  );
  assert(process.argv[index + 1] !== undefined, `缺少参数值：${key}`);
  assert(!Object.hasOwn(options, key), `重复参数：${key}`);
  options[key] = process.argv[index + 1];
}
for (const key of ["--node", "--rounds", "--health", "--mana"])
  assert(Object.hasOwn(options, key), `需要显式输入：${key}`);
const node = readContent("professions/gathering.json").nodes.find(
  (node) => node.id === options["--node"],
);
assert(node, `未知采集点：${options["--node"]}`);
const world = readContent("world/opening.json");
const route = recoveryRoute(world.places, node.location);
console.log(
  JSON.stringify(
    {
      scope: "指定伙伴容量与采集轮数的无药恢复情景，地图前置假定满足",
      node: node.id,
      location: node.location,
      route,
      result: gatheringRecoveryBudget({
        rounds: Number(options["--rounds"]),
        resourceLevel: node.level,
        health: Number(options["--health"]),
        mana: Number(options["--mana"]),
        ...route,
      }),
    },
    null,
    2,
  ),
);
