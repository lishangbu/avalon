import assert from "node:assert/strict";

/**
 * 成熟供料网络的期望边际需求。允许小数批次：适合长期重复生产，不代表首件成本。
 * 工具本身也需要制造工具，按期望报废率递归展开；小于容差的需求单独报告。
 */
export function productionCost({
  recipes,
  items,
  nodes,
  toolKinds,
  demands,
  crops = [],
  fish = [],
  bait,
  offers = [],
  level = 10,
  tolerance = 1e-8,
}) {
  assert(Number.isInteger(level) && level >= 1 && level <= 10);
  const itemMap = new Map(items.map((item) => [item.id, item]));
  const recipeFor = (code) =>
    recipes
      .filter((recipe) => recipe.product === code && recipe.level <= level)
      .sort((a, b) => a.level - b.level || a.id.localeCompare(b.id))[0];
  const nodeFor = (code) =>
    nodes
      .filter(
        (node) =>
          (node.product === code || node.rare === code) && node.level <= level,
      )
      .sort((a, b) => a.level - b.level || a.id.localeCompare(b.id))[0];
  const queue = Object.entries(demands).map(([code, amount]) => ({
    code,
    amount,
  }));
  const craftOperations = {};
  const gatherOperations = {};
  const external = {};
  const toolsConsumed = {};
  const transfers = {};
  const rawDemand = {};
  const planting = {};
  const catches = {};
  const purchases = {};
  const blueprints = new Set();
  let health = 0,
    mana = 0,
    onlineMillis = 0,
    ignored = 0;
  const add = (target, code, amount) =>
    (target[code] = (target[code] ?? 0) + amount);
  const toolFor = (kind, tier) => {
    const choices = items.filter(
      (item) => item.tool?.kind === kind && item.tool.tier === tier,
    );
    assert.equal(choices.length, 1, `工具类型/档位必须唯一：${kind}/${tier}`);
    return choices[0];
  };
  const enqueue = (code, amount, producer, consumer) => {
    assert(
      Number.isFinite(amount) && amount >= 0,
      `非法需求：${code}/${amount}`,
    );
    if (amount < tolerance) {
      ignored += amount;
      return;
    }
    queue.push({ code, amount });
    if (producer && consumer && producer !== consumer)
      add(transfers, `${producer}->${consumer}:${code}`, amount);
  };
  const producerOf = (code) => {
    const recipe = recipeFor(code);
    const node = nodeFor(code);
    const crop = crops.find((c) => c.product === code && c.level <= level);
    return (
      recipe?.skill ??
      node?.skill ??
      (crop
        ? "FARMING"
        : fish.some((f) => f.id === code && f.level <= level)
          ? "FISHING"
          : "EXTERNAL")
    );
  };
  const wear = (kind, tier, operations, consumer) => {
    const tool = toolFor(kind, tier);
    const amount = operations / tool.tool.breakDenominator;
    add(toolsConsumed, tool.id, amount);
    enqueue(tool.id, amount, producerOf(tool.id), consumer);
  };
  for (let index = 0; index < queue.length; index++) {
    assert(index < 100000, "工具供应递归不收敛");
    const { code, amount } = queue[index];
    assert(Number.isFinite(amount) && amount > 0, `非法需求：${code}`);
    const recipe = recipeFor(code);
    if (recipe) {
      if (recipe.blueprint) blueprints.add(recipe.blueprint);
      const operations = amount / recipe.quantity;
      add(craftOperations, recipe.id, operations);
      const productTool = itemMap.get(code)?.tool;
      const tier = productTool
        ? Math.max(0, productTool.tier - 1)
        : Math.floor((recipe.level - 1) / 2);
      assert(toolKinds[recipe.skill], `未知制造工具：${recipe.skill}`);
      wear(toolKinds[recipe.skill], tier, operations, recipe.skill);
      for (const [ingredient, count] of Object.entries(recipe.ingredients))
        enqueue(
          ingredient,
          count * operations,
          producerOf(ingredient),
          recipe.skill,
        );
      continue;
    }
    const node = nodeFor(code);
    if (node) {
      const chance = Math.min(95, 80 + (level - node.level) * 2) / 100;
      const rare = node.rare === code;
      // 稀有模型单列普通副产物；不将其抵扣其他原料需求，以免假定不存在的精确同步。
      const yieldPerRound =
        chance *
        (rare ? Math.min(8, 5 + level - node.level) / 100 : node.quantity);
      const operations = amount / yieldPerRound;
      add(gatherOperations, node.id, operations);
      add(rawDemand, code, amount);
      const tier = Math.floor((node.level - 1) / 2);
      wear(toolKinds[node.skill], tier, operations, node.skill);
      health += operations * 5 * (tier + 1);
      mana += operations * 2 * (tier + 1);
      onlineMillis += operations * Math.max(6000, 10000 - (level - 1) * 500);
      continue;
    }
    const crop = crops.find((c) => c.product === code && c.level <= level);
    if (crop) {
      const operations = amount / crop.quantity;
      add(planting, crop.id, operations);
      wear(
        toolKinds.FARMING,
        Math.floor((crop.level - 1) / 2),
        operations,
        "FARMING",
      );
      enqueue(crop.seed, operations, "EXTERNAL", "FARMING");
      continue;
    }
    const caught = fish.find((f) => f.id === code && f.level <= level);
    if (caught) {
      assert(bait, "缺少鱼饵配置");
      add(catches, code, amount);
      wear(
        toolKinds.FISHING,
        Math.floor((caught.level - 1) / 2),
        amount,
        "FISHING",
      );
      enqueue(bait, amount, "EXTERNAL", "FISHING");
      continue;
    }
    const offer = offers
      .filter((o) => o.item === code)
      .sort((a, b) => a.price - b.price)[0];
    if (offer) add(purchases, code, amount);
    else add(external, code, amount);
  }
  const purchaseGold = Object.entries(purchases).reduce(
    (sum, [code, amount]) =>
      sum +
      amount *
        Math.min(...offers.filter((o) => o.item === code).map((o) => o.price)),
    0,
  );
  return {
    craftOperations,
    gatherOperations,
    rawDemand,
    toolsConsumed,
    transfers,
    external,
    planting,
    catches,
    purchases,
    purchaseGold,
    requiredBlueprints: [...blueprints],
    healthSpent: health,
    manaSpent: mana,
    onlineSeconds: onlineMillis / 1000,
    ignoredDemand: ignored,
    tolerance,
    processedRequirements: queue.length,
  };
}

/** 金币单价与成交数量保持整数，逐批上取整5%税；这是给定交易计划而非市场价格预测。 */
export function tradeCost(quantity, unitPrice = 2, batchSize = 99) {
  assert(Number.isSafeInteger(quantity) && quantity >= 0);
  assert(Number.isSafeInteger(unitPrice) && unitPrice >= 2);
  assert(Number.isSafeInteger(batchSize) && batchSize > 0);
  const gross = quantity * unitPrice;
  assert(Number.isSafeInteger(gross), "成交金额超出JavaScript精确整数范围");
  const taxFor = (amount) =>
    Math.floor(amount / 20) + (amount % 20 === 0 ? 0 : 1);
  const fullBatches = Math.floor(quantity / batchSize);
  const tax =
    (fullBatches ? fullBatches * taxFor(batchSize * unitPrice) : 0) +
    taxFor((quantity % batchSize) * unitPrice);
  return {
    quantity,
    unitPrice,
    batchSize,
    gross,
    tax,
    sellerNet: gross - tax,
  };
}
