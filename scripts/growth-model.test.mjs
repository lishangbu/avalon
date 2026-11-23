import { test } from "node:test";
import assert from "node:assert/strict";
import {
  halfPointThreshold,
  professionLevel,
  gain,
  seededRandom,
  gatherMaterial,
  professionBudget,
  manufacturingProgress,
  gatheringRecoveryBudget,
  gatheringJourney,
} from "./growth-model.mjs";
import { readContent } from "./content-files.mjs";

test("熟练度假设必须重新计算升级时点，默认仍使用正式单倍规则", () => {
  const nodes = [
    { id: "LOW", skill: "HUNTING", level: 1, product: "HIDE", quantity: 1 },
    { id: "HIGH", skill: "HUNTING", level: 2, product: "HIDE", quantity: 2 },
  ];
  const tool = {
    item: "T",
    skill: "HUNTING",
    tier: 0,
    price: 20,
    breakDenominator: 100,
  };
  const normal = gatheringJourney(nodes, { HIDE: 130 }, [tool], () => 0);
  const doubled = gatheringJourney(
    nodes,
    { HIDE: 130 },
    [tool],
    () => 0,
    undefined,
    { proficiencyMultiplier: 2 },
  );
  assert.equal(normal.stages.LOW.rounds, 128);
  assert.equal(doubled.stages.LOW.rounds, 64);
  assert.equal(doubled.rounds, 97);
  assert.equal(normal.proficiencyMultiplier, 1);
  assert.throws(
    () =>
      gatheringJourney(nodes, { HIDE: 1 }, [tool], () => 0, undefined, {
        proficiencyMultiplier: 0,
      }),
    /1至10整数/,
  );
});

test("采集不能使用未解锁或已关闭节点，没有可用来源时明确拒绝", () => {
  const nodes = [
    {
      id: "OPEN",
      location: "field",
      skill: "HUNTING",
      level: 1,
      product: "HIDE",
      quantity: 1,
    },
    {
      id: "LOCKED",
      location: "late",
      skill: "HUNTING",
      level: 2,
      product: "HIDE",
      quantity: 2,
    },
  ];
  const tool = {
    item: "T",
    skill: "HUNTING",
    tier: 0,
    price: 20,
    breakDenominator: 100,
  };
  const result = gatheringJourney(
    nodes,
    { HIDE: 130 },
    [tool],
    () => 0,
    undefined,
    { nodeAccessible: (node) => node.location === "field" },
  );
  assert.equal(result.rounds, 130);
  assert.equal(result.stages.LOCKED, undefined);
  assert.throws(
    () =>
      gatheringJourney(nodes, { HIDE: 1 }, [tool], () => 0, undefined, {
        nodeAccessible: () => false,
      }),
    /缺少采集来源/,
  );
});

test("连续伙伴资源不足才恢复，末轮不额外恢复且跨批保留资源", () => {
  const node = {
    id: "N",
    location: "field",
    skill: "HUNTING",
    level: 1,
    product: "HIDE",
    quantity: 1,
  };
  const tool = {
    item: "T",
    skill: "HUNTING",
    tier: 0,
    price: 20,
    breakDenominator: 100,
  };
  const events = [];
  const economy = {
    resources: { health: 11, mana: 4 },
    onRecovery: (event) => events.push(event),
  };
  const first = gatheringJourney(
    [node],
    { HIDE: 2 },
    [tool],
    () => 0,
    undefined,
    economy,
  );
  assert.deepEqual(first.resources, { health: 1, mana: 0 });
  assert.equal(first.recoveries, 0);
  const next = gatheringJourney(
    [node],
    { HIDE: 1 },
    [tool],
    () => 0,
    {
      inventory: first.leftovers,
      skills: first.skills,
      heldTools: first.heldTools,
      resources: first.resources,
    },
    economy,
  );
  assert.equal(next.recoveries, 1);
  assert.deepEqual(next.resources, { health: 6, mana: 2 });
  assert.equal(events.length, 1);
  assert.equal(events[0].afterRound, 0);
  assert.throws(
    () =>
      gatheringJourney([node], { HIDE: 1 }, [tool], () => 0, undefined, {
        resources: { health: 5, mana: 4 },
      }),
    /无法完成一轮/,
  );
});

test("连续采集拒绝损坏的库存、熟练度和未知工具状态", () => {
  const node = {
    id: "N",
    skill: "HUNTING",
    level: 1,
    product: "HIDE",
    quantity: 1,
  };
  const tool = {
    item: "T",
    skill: "HUNTING",
    tier: 0,
    price: 20,
    breakDenominator: 100,
  };
  const run = (initial) =>
    gatheringJourney([node], { HIDE: 1 }, [tool], () => 0, initial);
  assert.throws(() => run({ inventory: { HIDE: -1 } }), /初始采集库存/);
  assert.throws(() => run({ inventory: { HIDE: 1.5 } }), /初始采集库存/);
  assert.throws(() => run({ skills: { HUNTING: -1 } }), /初始采集熟练度/);
  assert.throws(() => run({ heldTools: ["UNKNOWN"] }), /不在目录中/);
});

test("按容量回收只改变出售时点，不改变采集与产物守恒", () => {
  const node = {
    id: "N",
    location: "field",
    skill: "HUNTING",
    level: 1,
    product: "HIDE",
    rare: "RARE",
    quantity: 1,
  };
  const tool = {
    item: "T",
    skill: "HUNTING",
    tier: 0,
    price: 20,
    breakDenominator: 100,
  };
  const run = (saleSlotThreshold) =>
    gatheringJourney([node], { RARE: 300 }, [tool], () => 0, undefined, {
      sellPrices: { HIDE: 2 },
      saleSlotThreshold,
    });
  const fixed = run(undefined),
    capacity = run(8);
  assert.equal(capacity.rounds, fixed.rounds);
  assert.equal(capacity.salesGold, fixed.salesGold);
  assert.deepEqual(capacity.leftovers, fixed.leftovers);
  assert(capacity.saleVisits.length < fixed.saleVisits.length);
  assert.throws(() => run(0), /正整数/);
});

test("批次回收保留需求且按99件拆单，库存与出售数量守恒", () => {
  const node = {
    id: "N",
    skill: "HUNTING",
    level: 1,
    product: "HIDE",
    quantity: 1,
  };
  const tool = {
    item: "T",
    skill: "HUNTING",
    tier: 0,
    price: 20,
    breakDenominator: 100,
  };
  const transactions = [];
  const initial = {
    inventory: { HIDE: 250 },
    skills: { HUNTING: 0 },
    heldTools: ["T"],
  };
  const result = gatheringJourney(
    [node],
    { HIDE: 20 },
    [tool],
    () => 0,
    initial,
    {
      sellPrices: { HIDE: 2 },
      onTransaction: (transaction) => transactions.push(transaction),
    },
  );
  assert.equal(result.rounds, 0);
  assert.equal(result.inventory.HIDE, 20);
  assert.equal(result.leftovers.HIDE, 0);
  assert.equal(result.sold.HIDE, 230);
  assert.equal(result.salesGold, 460);
  assert.equal(result.saleVisits.length, 1, "拆单回收不得重复计算回城次数");
  assert.equal(result.saleVisits[0].gold, result.salesGold);
  assert.equal(result.saleVisits[0].afterRound, 0);
  assert.deepEqual(
    transactions.map((transaction) => transaction.quantity),
    [99, 99, 32],
  );
  assert.equal(
    result.sold.HIDE + result.leftovers.HIDE + 20,
    initial.inventory.HIDE,
  );
  assert.deepEqual(initial.inventory, { HIDE: 250 }, "计算不能修改调用者库存");
  assert.equal(result.peakMaterialSlots, 3, "不能只统计出售后的容量");
});

test("已解锁同档高产节点时切换节点，不继续使用最低级来源", () => {
  const nodes = [
    { id: "LOW", skill: "HUNTING", level: 1, product: "HIDE", quantity: 1 },
    { id: "HIGH", skill: "HUNTING", level: 2, product: "HIDE", quantity: 2 },
  ];
  const tool = {
    item: "T0",
    skill: "HUNTING",
    tier: 0,
    price: 20,
    breakDenominator: 100,
  };
  const result = gatheringJourney(nodes, { HIDE: 130 }, [tool], () => 0);
  assert.equal(result.stages.LOW.rounds, 128);
  assert.equal(result.stages.HIGH.rounds, 1);
  assert.equal(result.rounds, 129);
  assert.equal(result.inventory.HIDE, 130);
  assert.equal(
    result.peakMaterialSlots,
    2,
    "同名材料每99件一格，必须记录出售前峰值",
  );
});

test("副产物取得后才回收，保留目标材料且补购工具先于下一批收入", () => {
  const node = {
    id: "N1",
    skill: "HUNTING",
    level: 1,
    product: "HIDE",
    rare: "RARE",
    quantity: 2,
  };
  const tool = {
    item: "T0",
    skill: "HUNTING",
    tier: 0,
    price: 20,
    breakDenominator: 100,
  };
  const transactions = [];
  const economy = {
    sellPrices: { HIDE: 2, RARE: 5 },
    onTransaction: (transaction) => transactions.push(transaction),
  };
  const first = gatheringJourney(
    [node],
    { RARE: 1 },
    [tool],
    () => 0,
    undefined,
    economy,
  );
  assert.deepEqual(first.sold, { HIDE: 2 });
  assert.equal(first.inventory.RARE, 1);
  assert.equal(first.leftovers.RARE, 0);
  const next = gatheringJourney(
    [node],
    { RARE: 1 },
    [tool],
    () => 0,
    {
      inventory: first.leftovers,
      skills: first.skills,
      heldTools: first.heldTools,
    },
    economy,
  );
  assert.equal(next.toolGold, 20);
  assert.equal(next.salesGold, 4);
  assert.deepEqual(
    transactions.map((transaction) => transaction.gold),
    [4, -20, 4],
  );
  assert.equal(first.inventory.HIDE + first.sold.HIDE, 2);
});

test("单人采集先逐级成长再采高档材料，缺升档工具必须拒绝", () => {
  const nodes = [1, 2, 3].map((level) => ({
    id: `N${level}`,
    skill: "HUNTING",
    level,
    product: `P${level}`,
    quantity: 1,
  }));
  const tools = [0, 1].map((tier) => ({
    item: `T${tier}`,
    skill: "HUNTING",
    tier,
    price: 20,
    breakDenominator: 100,
  }));
  const result = gatheringJourney(nodes, { P3: 1 }, tools, () => 0);
  assert.equal(result.stages.N1.success, 128);
  assert.equal(result.stages.N2.success, 256);
  assert.equal(result.stages.N3.success, 1);
  assert.equal(result.levels.HUNTING, 3);
  assert.equal(result.rounds, 385);
  assert.equal(result.toolGold, 384 * 20);
  assert.equal(result.leftovers.P3, 0);
  assert.throws(
    () => gatheringJourney(nodes, { P3: 1 }, tools.slice(0, 1), () => 0),
    /缺少单人工具/,
  );
});

test("普通与稀有采集共用库存，已取得副产物不会重复采集", () => {
  const node = {
    id: "N1",
    skill: "HUNTING",
    level: 1,
    product: "HIDE",
    rare: "RARE",
    quantity: 2,
  };
  const tool = {
    item: "T0",
    skill: "HUNTING",
    tier: 0,
    price: 20,
    breakDenominator: 100,
  };
  const result = gatheringJourney(
    [node],
    { HIDE: 1, RARE: 1 },
    [tool],
    () => 0,
  );
  assert.equal(result.rounds, 1);
  assert.equal(result.broken, 1);
  assert.equal(result.toolGold, 0);
  assert.deepEqual(result.leftovers, { HIDE: 1, RARE: 0 });
  const resumed = gatheringJourney([node], { HIDE: 1 }, [tool], () => 0, {
    inventory: result.leftovers,
    skills: result.skills,
    heldTools: result.heldTools,
  });
  assert.equal(resumed.rounds, 0);
  assert.deepEqual(resumed.heldTools, []);
  assert.deepEqual(resumed.skills, result.skills);
  const next = gatheringJourney([node], { HIDE: 1 }, [tool], () => 0, {
    inventory: resumed.leftovers,
    skills: resumed.skills,
    heldTools: resumed.heldTools,
  });
  assert.equal(next.rounds, 1);
  assert.equal(next.toolGold, 20, "继续采集不能重新领取已报废的教学赠礼");
  assert.equal(next.skills.HUNTING, result.skills.HUNTING + 2);
  assert.throws(
    () => gatheringJourney([node], { HIDE: -1 }, [tool], () => 0),
    /非负整数/,
  );
});

test("无药采集生命保留1点、魔力可耗尽，末段不多算恢复", () => {
  const route = { outward: ["field", "clinic"], homeward: ["clinic", "field"] };
  const one = gatheringRecoveryBudget({
    rounds: 1,
    resourceLevel: 9,
    health: 26,
    mana: 10,
    ...route,
  });
  assert.equal(one.roundsPerTrip, 1);
  assert.equal(one.recoveries, 0);
  assert.equal(one.finalHealth, 1);
  assert.equal(one.finalMana, 0);
  const two = gatheringRecoveryBudget({
    rounds: 2,
    resourceLevel: 9,
    health: 26,
    mana: 10,
    ...route,
  });
  assert.equal(two.recoveries, 1);
  assert.equal(two.recoveryRoadSteps, 2);
  assert.throws(() =>
    gatheringRecoveryBudget({
      rounds: 1,
      resourceLevel: 9,
      health: 25,
      mana: 10,
      ...route,
    }),
  );
  assert.throws(() =>
    gatheringRecoveryBudget({
      rounds: 1,
      resourceLevel: 9,
      health: 26,
      mana: 9,
      ...route,
    }),
  );
});

test("制造计划拒绝未学习、越级和缺图纸，不消耗已持有图纸", () => {
  const recipes = [
    { id: "basic", skill: "ARTISAN", level: 1, points: 3 },
    {
      id: "advanced",
      skill: "ARTISAN",
      level: 2,
      points: 3,
      blueprint: "plan",
    },
  ];
  assert.throws(() =>
    manufacturingProgress(recipes, [{ recipe: "basic", count: 1 }], {}),
  );
  assert.throws(() =>
    manufacturingProgress(
      recipes,
      [{ recipe: "advanced", count: 1 }],
      { ARTISAN: 0 },
      ["plan"],
    ),
  );
  assert.throws(() =>
    manufacturingProgress(recipes, [{ recipe: "advanced", count: 1 }], {
      ARTISAN: 256,
    }),
  );
  const result = manufacturingProgress(
    recipes,
    [
      { recipe: "basic", count: 43 },
      { recipe: "advanced", count: 2 },
    ],
    { ARTISAN: 0 },
    ["plan"],
  );
  assert.equal(result.points.ARTISAN, 270);
  assert.deepEqual(result.blueprints, ["plan"]);
  assert.equal(result.levels.ARTISAN, 2);
});

test("正式熟练度边界、即时升级衰减及封顶", () => {
  assert.equal(halfPointThreshold(2), 256);
  assert.equal(halfPointThreshold(10), 130816);
  assert.equal(professionLevel(255), 1);
  assert.equal(professionLevel(256), 2);
  assert.equal(gain(254, 1, 3), 260);
  assert.equal(gain(260, 1, 3), 263);
  assert.equal(gain(halfPointThreshold(3), 1, 3), halfPointThreshold(3));
  assert.equal(gain(130815, 9, 3), 130816);
  assert.throws(() => gain(0, 2, 1));
});
test("正式同级包覆起步86次制造到3级并保留溢出", () => {
  const recipes = readContent(
    "professions/life-production.json",
  ).recipes.filter((r) => /^LIFE_WRAP_0/.test(r.id));
  const result = professionBudget(recipes, "LEATHERWORK", 3);
  assert.equal(result.totalCrafts, 86);
  assert.equal(result.halfPoints, 774);
});
test("失败扣资源与工具，稀有概率抽取在成功后，末次报废无需额外备用", () => {
  const rolls = [0.99, 0, 0, 0.04, 0];
  let i = 0;
  const result = gatherMaterial(
    { id: "test", level: 9, quantity: 2, rare: "rare" },
    { rare: true, quantity: 1 },
    9,
    { tier: 4, breakDenominator: 100 },
    () => rolls[i++],
    () => 80,
  );
  assert.equal(result.rounds, 2);
  assert.equal(result.success, 1);
  assert.equal(result.rareYield, 1);
  assert.equal(result.brokenTools, 2);
  assert.equal(result.toolsRequired, 2);
  assert.equal(result.healthSpent, 50);
  assert.equal(result.manaSpent, 20);
  assert.equal(result.elapsedMillis, 12000);
});
test("固定种子完全复现且零种子不退化", () => {
  const a = seededRandom(0),
    b = seededRandom(0);
  const sequence = Array.from({ length: 100 }, () => a());
  assert.deepEqual(
    sequence,
    Array.from({ length: 100 }, () => b()),
  );
  assert(new Set(sequence).size > 1);
  assert(sequence.every((n) => n >= 0 && n < 1));
});
