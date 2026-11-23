import assert from "node:assert/strict";
import { readContent } from "./content-files.mjs";
import {
  professionBudget,
  seededRandom,
  gatherMaterial,
} from "./growth-model.mjs";

const { recipes } = readContent("professions/life-production.json");
const { nodes } = readContent("professions/gathering.json");
const { items } = readContent("items/items.json");
const skills = {
  MINING: "PICKAXE",
  LOGGING: "AXE",
  HERBALISM: "PINCERS",
  HUNTING: "TRAP",
};
const targets = { LEATHERWORK: 9, ARTISAN: 9, ALCHEMY: 10, COOKING: 9 };
const budgets = Object.entries(targets).map(([skill, target]) =>
  professionBudget(recipes, skill, target),
);
const armor = recipes.find((r) => r.id === "E_FORGED_BODY");
assert(armor);
const rareDemands = Object.entries(armor.ingredients).filter(([code]) =>
  code.startsWith("RARE_"),
);
const reports = rareDemands.map(([code, quantity]) => {
  const candidates = nodes
    .filter((n) => n.rare === code)
    .sort((a, b) => a.level - b.level || a.id.localeCompare(b.id));
  assert(candidates.length, `${code}缺少稀有采集来源`);
  const node = candidates[0];
  const tool = items
    .filter(
      (i) =>
        i.tool?.kind === skills[node.skill] &&
        i.tool.tier === Math.floor((node.level - 1) / 2),
    )
    .sort((a, b) => a.id.localeCompare(b.id))[0];
  assert(tool, `${node.id}缺少同档工具`);
  const samples = Array.from({ length: 1000 }, (_, index) =>
    gatherMaterial(
      node,
      { rare: true, quantity },
      node.level,
      tool.tool,
      seededRandom(20261025 + index),
      (level, resource) => Math.min(95, 80 + (level - resource) * 2),
    ),
  );
  const stats = (key) => {
    const s = samples.map((v) => v[key]).sort((a, b) => a - b);
    return { min: s[0], p50: s[499], p90: s[899], max: s.at(-1) };
  };
  return {
    code,
    quantity,
    node: node.id,
    location: node.location,
    skill: node.skill,
    startLevel: node.level,
    tool: tool.id,
    breakDenominator: tool.tool.breakDenominator,
    rounds: stats("rounds"),
    toolsRequired: stats("toolsRequired"),
    healthSpent: stats("healthSpent"),
    manaSpent: stats("manaSpent"),
    onlineSeconds: Object.fromEntries(
      Object.entries(stats("elapsedMillis")).map(([k, v]) => [k, v / 1000]),
    ),
  };
});
console.log(
  JSON.stringify(
    {
      scope:
        "四角色制造专业从零熟练度的直接配方预算，以及E009稀有原料的1000种子采集模型",
      budgets,
      rareMaterials: reports,
      exclusions: [
        "练级原料仅列直接需求，未递归计入供应专业练级与工具制造成本",
        "稀有采集从已有技能资格和工具开始；缺工具或生命魔力时假定正常回补，未计恢复与旅行耗时",
        "普通产物与稀有产物同时记录，不把它们的成本相加；不证明完整经济闭环",
        "不同原料样本独立，不能直接相加分位数推断总耗时分位数",
      ],
    },
    null,
    2,
  ),
);
