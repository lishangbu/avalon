// 离线模型与正式规则分开执行，由规则对照测试校验关键边界。
import assert from "node:assert/strict";

/** 无药采集：生命必须留至少1点，魔力可耗至0；只在两段采集之间安排恢复。 */
export function gatheringRecoveryBudget({
  rounds,
  resourceLevel,
  health,
  mana,
  outward,
  homeward,
}) {
  assert(Number.isSafeInteger(rounds) && rounds >= 0, "采集轮数必须是非负整数");
  assert(
    Number.isInteger(resourceLevel) &&
      resourceLevel >= 1 &&
      resourceLevel <= 10,
  );
  assert(
    Number.isSafeInteger(health) &&
      health >= 0 &&
      Number.isSafeInteger(mana) &&
      mana >= 0,
    "需要显式提供伙伴完整生命与魔力容量",
  );
  const tier = Math.floor((resourceLevel - 1) / 2),
    healthCost = 5 * (tier + 1),
    manaCost = 2 * (tier + 1);
  const roundsPerTrip = Math.min(
    Math.floor(Math.max(0, health - 1) / healthCost),
    Math.floor(mana / manaCost),
  );
  assert(!rounds || roundsPerTrip >= 1, "该伙伴即使完全恢复也无法完成一轮采集");
  const trips = rounds ? Math.ceil(rounds / roundsPerTrip) : 0;
  const recoveries = Math.max(0, trips - 1);
  assert(
    Array.isArray(outward) &&
      Array.isArray(homeward) &&
      outward.length &&
      homeward.length,
  );
  assert.equal(outward[0], homeward.at(-1), "恢复返程必须回到采集地点");
  assert.equal(outward.at(-1), homeward[0], "恢复去返必须使用同一设施");
  const finalRounds = rounds ? rounds - (trips - 1) * roundsPerTrip : 0;
  return {
    rounds,
    health,
    mana,
    healthCost,
    manaCost,
    roundsPerTrip,
    trips,
    recoveries,
    recoveryRoadSteps: recoveries * (outward.length + homeward.length - 2),
    finalHealth: health - finalRounds * healthCost,
    finalMana: mana - finalRounds * manaCost,
    excludes:
      "起终点旅行、最后一轮之后的恢复、工具耗尽、地图门禁、伙伴派遣、战败与实际道路耗时；起始假定完全恢复",
  };
}

export const halfPointThreshold = (level) => 256 * (2 ** (level - 1) - 1);
export function professionLevel(points) {
  assert(
    Number.isSafeInteger(points) &&
      points >= 0 &&
      points <= halfPointThreshold(10),
  );
  let level = 1;
  while (level < 10 && points >= halfPointThreshold(level + 1)) level++;
  return level;
}
export function gain(points, contentLevel, basePoints) {
  const difference = professionLevel(points) - contentLevel;
  assert(
    difference >= 0 &&
      contentLevel >= 1 &&
      contentLevel <= 10 &&
      basePoints >= 0,
  );
  return Math.min(
    halfPointThreshold(10),
    points +
      (difference === 0 ? basePoints * 2 : difference === 1 ? basePoints : 0),
  );
}
export function seededRandom(seed) {
  let state = seed >>> 0;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let value = Math.imul(state ^ (state >>> 15), 1 | state);
    value ^= value + Math.imul(value ^ (value >>> 7), 61 | value);
    return ((value ^ (value >>> 14)) >>> 0) / 4294967296;
  };
}

/** 验证制造计划的资格顺序；图纸仅检查持有，不在每件制造后消耗。 */
export function manufacturingProgress(
  recipes,
  operations,
  learned,
  blueprints = [],
) {
  const points = { ...learned };
  const ownedBlueprints = new Set(blueprints);
  const byId = new Map(recipes.map((recipe) => [recipe.id, recipe]));
  const stages = [];
  for (const { recipe: id, count } of operations) {
    const recipe = byId.get(id);
    assert(recipe, `未知配方：${id}`);
    assert(Number.isSafeInteger(count) && count >= 0, `制造次数无效：${id}`);
    if (!count) continue;
    assert(Object.hasOwn(points, recipe.skill), `尚未学习：${recipe.skill}`);
    assert(
      !recipe.blueprint || ownedBlueprints.has(recipe.blueprint),
      `缺少图纸：${recipe.blueprint}`,
    );
    const before = points[recipe.skill];
    for (let index = 0; index < count; index++) {
      assert(
        professionLevel(points[recipe.skill]) >= recipe.level,
        `制造资格不足：${id}`,
      );
      points[recipe.skill] = gain(
        points[recipe.skill],
        recipe.level,
        recipe.points,
      );
    }
    stages.push({
      recipe: id,
      count,
      skill: recipe.skill,
      before,
      after: points[recipe.skill],
      level: professionLevel(points[recipe.skill]),
    });
  }
  return {
    points,
    levels: Object.fromEntries(
      Object.entries(points).map(([skill, value]) => [
        skill,
        professionLevel(value),
      ]),
    ),
    stages,
    blueprints: [...ownedBlueprints],
  };
}

/** 每级选择无图纸且直接原料/熟练度最少的同级配方；不是完整供料最优解。 */
export function professionBudget(recipes, skill, target) {
  assert(Number.isInteger(target) && target >= 1 && target <= 10);
  let points = 0;
  const operations = {};
  const ingredients = {};
  const byLevel = [];
  while (professionLevel(points) < target) {
    const current = professionLevel(points);
    const cost = (recipe) =>
      Object.values(recipe.ingredients).reduce((a, b) => a + b, 0) /
      recipe.points;
    const recipe = recipes
      .filter(
        (r) =>
          r.skill === skill &&
          r.level === current &&
          !r.blueprint &&
          r.points > 0,
      )
      .sort((a, b) => cost(a) - cost(b) || a.id.localeCompare(b.id))[0];
    assert(recipe, `${skill} ${current}级缺少自动学习配方`);
    let count = 0;
    while (professionLevel(points) === current) {
      points = gain(points, recipe.level, recipe.points);
      count++;
    }
    operations[recipe.id] = (operations[recipe.id] ?? 0) + count;
    for (const [code, quantity] of Object.entries(recipe.ingredients))
      ingredients[code] = (ingredients[code] ?? 0) + quantity * count;
    byLevel.push({
      level: current,
      recipe: recipe.id,
      count,
      halfPoints: points,
    });
  }
  return {
    skill,
    target,
    halfPoints: points,
    totalCrafts: Object.values(operations).reduce((a, b) => a + b, 0),
    byLevel,
    operations,
    ingredients,
  };
}

/** 稀有与普通产出同轮抽取；失败仍扣资源、磨损工具，只有成功增加熟练度。 */
export function gatherMaterial(
  node,
  amount,
  startLevel,
  tool,
  random,
  yieldChance,
) {
  assert(
    startLevel >= node.level &&
      startLevel <= 10 &&
      tool.tier >= Math.floor((node.level - 1) / 2),
  );
  let points = halfPointThreshold(startLevel),
    rounds = 0,
    success = 0,
    rare = 0,
    broken = 0,
    elapsedMillis = 0,
    lastBroke = false;
  const isRare = amount.rare;
  while ((isRare ? rare : success * node.quantity) < amount.quantity) {
    const level = professionLevel(points);
    elapsedMillis += Math.max(6000, 10000 - (level - 1) * 500);
    const yielded = random() * 100 < yieldChance(level, node.level);
    if (yielded) {
      success++;
      if (node.rare && random() * 100 < Math.min(8, 5 + level - node.level))
        rare++;
      points = gain(points, node.level, 1);
    }
    lastBroke = Math.floor(random() * tool.breakDenominator) === 0;
    if (lastBroke) broken++;
    if (++rounds > 1000000) throw new Error(`采集预算不收敛：${node.id}`);
  }
  const tier = Math.floor((node.level - 1) / 2);
  return {
    rounds,
    success,
    normalYield: success * node.quantity,
    rareYield: rare,
    brokenTools: broken,
    toolsRequired: rounds ? broken + (lastBroke ? 0 : 1) : 0,
    elapsedMillis,
    healthSpent: rounds * 5 * (tier + 1),
    manaSpent: rounds * 2 * (tier + 1),
    endLevel: professionLevel(points),
  };
}

/** 固定专业从零熟练度采集；缺资格时先在当前最高可用节点练级，副产物入同一库存。 */
export function gatheringJourney(
  nodes,
  demands,
  tools,
  random,
  initial,
  economy,
) {
  if (economy?.saleSlotThreshold !== undefined)
    assert(
      Number.isSafeInteger(economy.saleSlotThreshold) &&
        economy.saleSlotThreshold > 0,
      "回收容量阈值必须是正整数",
    );
  const inventory = { ...initial?.inventory },
    skills = { ...initial?.skills },
    held = new Set(initial?.heldTools ?? []),
    stages = {};
  const proficiencyMultiplier = economy?.proficiencyMultiplier ?? 1;
  assert(
    Number.isSafeInteger(proficiencyMultiplier) &&
      proficiencyMultiplier >= 1 &&
      proficiencyMultiplier <= 10,
    "采集熟练度情景倍率必须为1至10整数",
  );
  for (const quantity of Object.values(inventory))
    assert(
      Number.isSafeInteger(quantity) && quantity >= 0,
      "初始采集库存必须为非负整数",
    );
  for (const points of Object.values(skills))
    assert(
      Number.isSafeInteger(points) && points >= 0,
      "初始采集熟练度必须为非负整数",
    );
  for (const item of held)
    assert(
      tools.some((tool) => tool.item === item),
      `初始持有工具不在目录中：${item}`,
    );
  let rounds = 0,
    healthSpent = 0,
    manaSpent = 0,
    elapsedMillis = 0,
    toolGold = 0,
    broken = 0,
    salesGold = 0;
  let peakMaterialSlots = 0;
  const observeSlots = () => {
    peakMaterialSlots = Math.max(
      peakMaterialSlots,
      Object.values(inventory).reduce(
        (sum, quantity) => sum + Math.ceil(quantity / 99),
        0,
      ),
    );
  };
  observeSlots();
  const sold = {};
  const limits = economy?.resources;
  let resources = initial?.resources
    ? { ...initial.resources }
    : limits
      ? { health: limits.health, mana: limits.mana }
      : undefined;
  let recoveries = 0;
  if (limits) {
    assert(
      Number.isSafeInteger(limits.health) &&
        limits.health > 0 &&
        Number.isSafeInteger(limits.mana) &&
        limits.mana >= 0,
      "需要有效伙伴容量",
    );
    assert(
      Number.isSafeInteger(resources.health) &&
        resources.health >= 1 &&
        resources.health <= limits.health &&
        Number.isSafeInteger(resources.mana) &&
        resources.mana >= 0 &&
        resources.mana <= limits.mana,
      "伙伴当前资源超出容量",
    );
  } else assert(!resources, "延续伙伴资源时必须显式提供容量");
  let currentNode = initial?.lastNode;
  const saleVisits = [];
  const sellSurplus = () => {
    let visitGold = 0;
    for (const [item, price] of Object.entries(economy?.sellPrices ?? {})) {
      assert(Number.isSafeInteger(price) && price > 0);
      const quantity = Math.max(
        0,
        (inventory[item] ?? 0) - (demands[item] ?? 0),
      );
      if (!quantity) continue;
      inventory[item] -= quantity;
      sold[item] = (sold[item] ?? 0) + quantity;
      salesGold += quantity * price;
      visitGold += quantity * price;
      for (let remaining = quantity; remaining > 0; remaining -= 99) {
        const batch = Math.min(99, remaining);
        economy?.onTransaction?.({
          kind: "SELL_SURPLUS",
          item,
          quantity: batch,
          gold: batch * price,
        });
      }
    }
    if (visitGold > 0)
      saleVisits.push({
        node: currentNode?.id ?? null,
        location: currentNode?.location ?? null,
        afterRound: rounds,
        gold: visitGold,
      });
  };
  if (!initial)
    for (const tool of tools.filter((tool) => tool.tier === 0))
      held.add(tool.item);
  const accessibleNodes = economy?.nodeAccessible
    ? nodes.filter(economy.nodeAccessible)
    : nodes;
  for (const [product, quantity] of Object.entries(demands)) {
    assert(
      Number.isSafeInteger(quantity) && quantity >= 0,
      "采集需求必须为非负整数",
    );
    const target = accessibleNodes
      .filter((node) => node.product === product || node.rare === product)
      .sort((a, b) => a.level - b.level)[0];
    assert(target, `缺少采集来源：${product}`);
    while ((inventory[product] ?? 0) < quantity) {
      const points = skills[target.skill] ?? 0;
      const level = professionLevel(points);
      const node =
        level >= target.level
          ? accessibleNodes
              .filter(
                (node) =>
                  node.skill === target.skill &&
                  node.level <= level &&
                  (node.product === product || node.rare === product),
              )
              .sort((a, b) => b.level - a.level || a.id.localeCompare(b.id))[0]
          : accessibleNodes
              .filter(
                (node) => node.skill === target.skill && node.level <= level,
              )
              .sort((a, b) => b.level - a.level)[0];
      assert(node, `采集专业无法起步：${target.skill}`);
      economy?.onNode?.(node);
      currentNode = node;
      const tier = Math.floor((node.level - 1) / 2);
      if (resources) {
        const healthCost = 5 * (tier + 1),
          manaCost = 2 * (tier + 1);
        assert(
          limits.health > healthCost && limits.mana >= manaCost,
          `伙伴容量无法完成一轮：${node.id}`,
        );
        if (resources.health <= healthCost || resources.mana < manaCost) {
          economy?.onRecovery?.({
            node: node.id,
            location: node.location,
            afterRound: rounds,
            before: { ...resources },
          });
          resources = { health: limits.health, mana: limits.mana };
          recoveries++;
        }
        resources.health -= healthCost;
        resources.mana -= manaCost;
      }
      const tool = tools.find(
        (tool) => tool.skill === node.skill && tool.tier === tier,
      );
      assert(tool, `缺少单人工具：${node.id}`);
      if (!held.has(tool.item)) {
        assert(Number.isSafeInteger(tool.price) && tool.price > 0);
        toolGold += tool.price;
        economy?.onTransaction?.({
          kind: "BUY_TOOL",
          item: tool.item,
          quantity: 1,
          gold: -tool.price,
        });
        held.add(tool.item);
      }
      const stage = (stages[node.id] ??= {
        skill: node.skill,
        level: node.level,
        rounds: 0,
        success: 0,
        rare: 0,
      });
      stage.rounds++;
      elapsedMillis += Math.max(6000, 10000 - (level - 1) * 500);
      healthSpent += 5 * (tier + 1);
      manaSpent += 2 * (tier + 1);
      if (random() * 100 < Math.min(95, 80 + (level - node.level) * 2)) {
        inventory[node.product] =
          (inventory[node.product] ?? 0) + node.quantity;
        stage.success++;
        if (node.rare && random() * 100 < Math.min(8, 5 + level - node.level)) {
          inventory[node.rare] = (inventory[node.rare] ?? 0) + 1;
          stage.rare++;
        }
        skills[node.skill] = gain(points, node.level, proficiencyMultiplier);
      }
      if (Math.floor(random() * tool.breakDenominator) === 0) {
        held.delete(tool.item);
        broken++;
      }
      observeSlots();
      if (++rounds > 1000000) throw new Error(`单人采集计划不收敛：${product}`);
      const materialSlots = Object.values(inventory).reduce(
        (sum, quantity) => sum + Math.ceil(quantity / 99),
        0,
      );
      if (
        economy?.saleSlotThreshold
          ? materialSlots >= economy.saleSlotThreshold
          : rounds % 99 === 0
      )
        sellSurplus();
    }
  }
  sellSurplus();
  const leftovers = { ...inventory };
  for (const [product, quantity] of Object.entries(demands)) {
    assert((leftovers[product] ?? 0) >= quantity);
    leftovers[product] -= quantity;
  }
  return {
    rounds,
    healthSpent,
    manaSpent,
    elapsedMillis,
    toolGold,
    broken,
    heldTools: [...held],
    lastNode: currentNode,
    sold,
    saleVisits,
    salesGold,
    proficiencyMultiplier,
    ...(resources ? { resources, recoveries } : {}),
    peakMaterialSlots,
    skills,
    levels: Object.fromEntries(
      Object.entries(skills).map(([skill, points]) => [
        skill,
        professionLevel(points),
      ]),
    ),
    inventory,
    leftovers,
    stages,
  };
}
