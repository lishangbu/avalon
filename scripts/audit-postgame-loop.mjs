// 后期闭环的静态供料与补给审计；不把成功采集、制造或战斗当成已验证。
import assert from "node:assert/strict";
import { readContent, storyContent } from "./content-files.mjs";

const production = readContent("professions/life-production.json");
const { nodes } = readContent("professions/gathering.json");
const trial = readContent("dungeons/sealed-trial.json");
const { steps } = storyContent();
const farming = readContent("professions/farming.json");
const fishing = readContent("professions/fishing.json");

// 选择最低等级配方并保留多产库存，后续依赖复用余量，避免重复计算半批产物。
function bill(product, quantity) {
  const pending = new Map([[product, quantity]]);
  const raw = new Map();
  const crafts = [];
  const professions = {};
  const surplus = new Map();
  function expand(code, amount, ancestry = []) {
    assert(
      !ancestry.includes(code),
      `配方循环：${[...ancestry, code].join(" -> ")}`,
    );
    const used = Math.min(amount, surplus.get(code) ?? 0);
    amount -= used;
    surplus.set(code, (surplus.get(code) ?? 0) - used);
    if (!amount) return;
    const recipe = production.recipes
      .filter((r) => r.product === code)
      .sort((a, b) => a.level - b.level || a.id.localeCompare(b.id))[0];
    if (!recipe) {
      raw.set(code, (raw.get(code) ?? 0) + amount);
      return;
    }
    const count = Math.ceil(amount / recipe.quantity);
    surplus.set(
      code,
      (surplus.get(code) ?? 0) + count * recipe.quantity - amount,
    );
    professions[recipe.skill] = Math.max(
      professions[recipe.skill] ?? 0,
      recipe.level,
    );
    crafts.push({
      recipe: recipe.id,
      count,
      produced: count * recipe.quantity,
      required: amount,
      locations: recipe.locations,
      blueprint: recipe.blueprint ?? null,
    });
    for (const [ingredient, needed] of Object.entries(recipe.ingredients)) {
      expand(ingredient, needed * count, [...ancestry, code]);
    }
  }
  for (const [code, amount] of pending) expand(code, amount);
  const materials = [...raw].map(([code, amount]) => ({
    code,
    amount,
    gathering: nodes
      .filter((n) => n.product === code || n.rare === code)
      .map((n) => ({
        node: n.id,
        location: n.location,
        skill: n.skill,
        level: n.level,
        rare: n.rare === code,
        minimumSuccessfulRounds:
          n.product === code ? Math.ceil(amount / n.quantity) : null,
      })),
    farming: farming.crops
      .filter((c) => c.product === code)
      .map((c) => ({
        skill: "FARMING",
        level: c.level,
        seed: c.seed,
        locations: farming.locations,
        harvests: Math.ceil(amount / c.quantity),
      })),
    fishing: fishing.fish
      .filter((f) => f.id === code)
      .map((f) => ({
        skill: "FISHING",
        level: f.level,
        locations: f.locations,
        bait: fishing.bait,
      })),
  }));
  for (const material of materials) {
    assert(
      material.gathering.length ||
        material.farming.length ||
        material.fishing.length,
      `原料缺少来源：${material.code}`,
    );
  }
  return { product, quantity, professions, crafts, raw: materials };
}

const e009 = steps.find((s) => s.id === "E009");
const e010 = steps.find((s) => s.id === "E010");
assert(
  e009.requires.includes("E008") && e010.requires.includes("E008"),
  "制造与试炼必须从E008独立开放",
);
assert(!e010.requires.includes("E009"), "重复试炼不能被可选制造阻挡");
assert(
  e009.objectives.some(
    (o) => o.code === "CRAFT_PRODUCT:E_FORGED_BODY" && o.amount === 1,
  ),
);
const stageGold = trial.dungeon.stages.reduce(
  (sum, s) => sum + s.gold * Math.max(1, s.wildSpecies?.length ?? 0),
  0,
);
const shop = Object.values(production).find(
  (value) =>
    Array.isArray(value) &&
    value.some((e) => e.item === "LIFE_HEALTH_POTION_4"),
);
assert(shop, "缺少药品商店配置");
const price = (code) => {
  const entry = shop.find((p) => p.item === code);
  assert(entry, `缺少补给价格：${code}`);
  return entry.price;
};
const patrol = readContent("battle/wilds.json").profiles.E_RESIDUAL_PATROL;
const levels = Object.values(patrol.species).flatMap((s) => [
  s.minLevel,
  s.maxLevel,
]);
const goldPerFight = [Math.min(...levels), Math.max(...levels)].map(
  (level) => 2 * Math.floor((level + 2) / 3) * patrol.goldMultiplier,
);
const supplyScenarios = [8, 14].map((healthConsumed) => {
  const replacementCost = healthConsumed * price("LIFE_HEALTH_POTION_4");
  const deficit = Math.max(0, replacementCost - stageGold);
  return {
    healthConsumed,
    manaConsumed: 0,
    replacementCost,
    guaranteedStageGold: stageGold,
    deficit,
    recoveryPatrols: [
      Math.ceil(deficit / goldPerFight[1]),
      Math.ceil(deficit / goldPerFight[0]),
    ],
  };
});
console.log(
  JSON.stringify(
    {
      scope:
        "正式内容静态审计：已有资格与图纸，从零原料制造；未计练级、工具损坏、失败、旅行和市场成本",
      branches: {
        manufacture: e009.requires,
        repeatTrial: e010.requires,
        dailyStarts: trial.dungeon.dailyStarts,
      },
      armor: bill("E_FORGED_BODY", 1),
      healthSupply: bill("LIFE_HEALTH_POTION_4", 20),
      manaSupply: bill("LIFE_MANA_POTION_4", 10),
      supplyScenarios,
      exclusions: [
        "补给情景沿用历史8至14瓶生命药消耗样本，不预测战斗结果",
        "不计宝箱随机奖励和首日奖励，不能将这些下界当成净收益",
        "minimumSuccessfulRounds不计失败、资源、工具与稀有掉落概率",
        "原料来源存在不代表工具、种子、地图前置及整条自然成长已经验证",
      ],
    },
    null,
    2,
  ),
);
