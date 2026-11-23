import { test } from "node:test";
import assert from "node:assert/strict";
import { productionCost, tradeCost } from "./production-cost-model.mjs";
import { readContent } from "./content-files.mjs";

test("正式护衣及药品供料可以解析至来源且只要求原有图纸", () => {
  const production = readContent("professions/life-production.json");
  const farming = readContent("professions/farming.json");
  const fishing = readContent("professions/fishing.json");
  const result = productionCost({
    recipes: production.recipes,
    items: readContent("items/items.json").items,
    nodes: readContent("professions/gathering.json").nodes,
    crops: farming.crops,
    fish: fishing.fish,
    bait: fishing.bait,
    offers: production.offers,
    toolKinds: {
      ARTISAN: "HAMMER",
      LEATHERWORK: "NEEDLE",
      ALCHEMY: "ROD",
      COOKING: "POT",
      MINING: "PICKAXE",
      LOGGING: "AXE",
      HERBALISM: "PINCERS",
      HUNTING: "TRAP",
      FARMING: "HOE",
      FISHING: "FISHING_ROD",
    },
    demands: {
      E_FORGED_BODY: 1,
      LIFE_HEALTH_POTION_4: 20,
      LIFE_MANA_POTION_4: 10,
    },
  });
  assert.deepEqual(result.external, {});
  assert.deepEqual(result.requiredBlueprints, ["E_FORGE_BLUEPRINT"]);
  assert.equal(result.craftOperations.E_FORGED_BODY, 1);
  assert(result.purchaseGold > 0);
  assert(result.healthSpent > 0 && result.manaSpent > 0);
  assert(result.toolsConsumed.TOOL_GOLD_TRAP > 0);
});
test("逐批成交税不可按整单平均税替代", () => {
  assert.deepEqual(tradeCost(21, 2, 1), {
    quantity: 21,
    unitPrice: 2,
    batchSize: 1,
    gross: 42,
    tax: 21,
    sellerNet: 21,
  });
  assert.equal(tradeCost(21, 2, 99).tax, 3);
  assert.throws(() => tradeCost(1, 1));
  assert.throws(() => tradeCost(Number.MAX_SAFE_INTEGER, 2));
  assert.equal(tradeCost(1_000_000, 2, 1).tax, 1_000_000);
});

test("稀有材料归属实际采集专业，跨专业供料不能漏算交易", () => {
  const result = productionCost({
    recipes: [
      {
        id: "armor",
        product: "armor",
        level: 9,
        quantity: 1,
        skill: "LEATHERWORK",
        ingredients: { rare: 1 },
      },
    ],
    items: [
      {
        id: "needle",
        tool: { kind: "NEEDLE", tier: 4, breakDenominator: 1000 },
      },
      {
        id: "pincers",
        tool: { kind: "PINCERS", tier: 4, breakDenominator: 1000 },
      },
    ],
    nodes: [
      {
        id: "herb",
        product: "herb",
        rare: "rare",
        skill: "HERBALISM",
        level: 9,
        quantity: 1,
      },
    ],
    toolKinds: { LEATHERWORK: "NEEDLE", HERBALISM: "PINCERS" },
    demands: { armor: 1 },
  });
  assert.equal(result.transfers["HERBALISM->LEATHERWORK:rare"], 1);
  assert.equal(result.transfers["EXTERNAL->LEATHERWORK:rare"], undefined);
});
test("制造工具自身损耗的递归期望收敛，不漏算工具补造", () => {
  const result = productionCost({
    recipes: [
      {
        id: "tool",
        product: "tool",
        level: 1,
        quantity: 1,
        skill: "ARTISAN",
        ingredients: { ore: 1 },
      },
    ],
    items: [
      { id: "tool", tool: { kind: "HAMMER", tier: 0, breakDenominator: 100 } },
    ],
    nodes: [],
    toolKinds: { ARTISAN: "HAMMER" },
    demands: { tool: 1 },
    tolerance: 1e-10,
  });
  assert(Math.abs(result.craftOperations.tool - 100 / 99) < 1e-8);
  assert(Math.abs(result.external.ore - 100 / 99) < 1e-8);
});
test("工具升档使用前一档工具", () => {
  const result = productionCost({
    recipes: [
      {
        id: "gold",
        product: "gold",
        level: 9,
        quantity: 1,
        skill: "ARTISAN",
        ingredients: { ore: 1 },
      },
    ],
    items: [
      { id: "gold", tool: { kind: "HAMMER", tier: 4, breakDenominator: 1000 } },
      {
        id: "silver",
        tool: { kind: "HAMMER", tier: 3, breakDenominator: 1000 },
      },
    ],
    nodes: [],
    toolKinds: { ARTISAN: "HAMMER" },
    demands: { gold: 1 },
  });
  assert.equal(result.toolsConsumed.silver, 0.001);
  assert.equal(result.external.silver, 0.001);
  assert.equal(result.toolsConsumed.gold, undefined);
});
test("种植按播种磨损工具，钓获消耗鱼饵；采购与外部缺口分开", () => {
  const result = productionCost({
    recipes: [],
    items: [
      { id: "hoe", tool: { kind: "HOE", tier: 0, breakDenominator: 100 } },
      {
        id: "rod",
        tool: { kind: "FISHING_ROD", tier: 0, breakDenominator: 100 },
      },
    ],
    nodes: [],
    toolKinds: { FARMING: "HOE", FISHING: "FISHING_ROD" },
    crops: [
      { id: "crop", product: "crop", seed: "seed", level: 1, quantity: 2 },
    ],
    fish: [{ id: "fish", level: 1 }],
    bait: "bait",
    offers: [
      { item: "seed", price: 20 },
      { item: "bait", price: 2 },
    ],
    demands: { crop: 4, fish: 3 },
  });
  assert.equal(result.planting.crop, 2);
  assert.equal(result.catches.fish, 3);
  assert.equal(result.purchaseGold, 46);
  assert.equal(result.toolsConsumed.hoe, 0.02);
  assert.equal(result.toolsConsumed.rod, 0.03);
  assert.deepEqual(result.external, { hoe: 0.02, rod: 0.03 });
});
