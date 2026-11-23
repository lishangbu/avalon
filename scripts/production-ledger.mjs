import assert from "node:assert/strict";
import { tradeCost } from "./production-cost-model.mjs";

/** 固定成交计划的逐笔现金流；最低起始资金由历史最大缺口求得，不注入游戏资产。 */
export function tradeFunding(transactions, initialGold) {
  const delta = {},
    minimum = {},
    requiredInitialGold = {};
  let totalTax = 0,
    firstInsufficient = null;
  for (const [index, trade] of transactions.entries()) {
    assert(trade.seller !== trade.buyer, "不能给自己成交");
    const quote = tradeCost(trade.quantity, trade.unitPrice, trade.batchSize);
    assert.equal(trade.gross, quote.gross);
    assert.equal(trade.tax, quote.tax);
    assert.equal(trade.sellerNet, quote.sellerNet);
    for (const owner of [trade.buyer, trade.seller]) {
      delta[owner] ??= 0;
      minimum[owner] ??= 0;
    }
    if (
      initialGold &&
      firstInsufficient === null &&
      (initialGold[trade.buyer] ?? 0) + delta[trade.buyer] < trade.gross
    )
      firstInsufficient = {
        index,
        buyer: trade.buyer,
        required: trade.gross,
        available: (initialGold[trade.buyer] ?? 0) + delta[trade.buyer],
      };
    delta[trade.buyer] -= trade.gross;
    delta[trade.seller] += trade.sellerNet;
    minimum[trade.buyer] = Math.min(minimum[trade.buyer], delta[trade.buyer]);
    totalTax += trade.tax;
  }
  for (const owner of Object.keys(delta))
    requiredInitialGold[owner] = Math.max(0, -minimum[owner]);
  assert.equal(
    Object.values(delta).reduce((sum, value) => sum + value, 0),
    -totalTax,
    "交易现金不守恒",
  );
  const balances = Object.fromEntries(
    Object.entries(delta).map(([owner, value]) => [
      owner,
      value + (initialGold?.[owner] ?? requiredInitialGold[owner]),
    ]),
  );
  return {
    requiredInitialGold,
    delta,
    totalTax,
    balances,
    firstInsufficient,
    initialGoldProvided: initialGold !== undefined,
  };
}

/** 已有成品按角色持有；不足时先通过其他角色的既有库存交易，其余列为待取得缺额。 */
export function partyProductionLedger(
  recipes,
  operations,
  ownerForSkill,
  initial = {},
  batchSize = 1,
) {
  assert(Number.isSafeInteger(batchSize) && batchSize >= 1 && batchSize <= 99);
  const inventories = structuredClone(initial),
    acquired = {},
    transactions = [],
    produced = {},
    consumed = {};
  const map = new Map(recipes.map((recipe) => [recipe.id, recipe]));
  const add = (table, owner, code, amount) => {
    table[owner] ??= {};
    table[owner][code] = (table[owner][code] ?? 0) + amount;
  };
  const stock = (owner) => (inventories[owner] ??= {});
  for (const { recipe: id, count } of operations) {
    const recipe = map.get(id);
    assert(recipe, `未知配方：${id}`);
    const owner = ownerForSkill[recipe.skill];
    assert(owner, `未分配制造角色：${recipe.skill}`);
    assert(Number.isSafeInteger(count) && count >= 0);
    for (let operation = 0; operation < count; operation++) {
      if (operation % batchSize === 0) {
        const batchCount = Math.min(batchSize, count - operation);
        for (const [code, quantity] of Object.entries(recipe.ingredients)) {
          let missing = Math.max(
            0,
            quantity * batchCount - (stock(owner)[code] ?? 0),
          );
          for (const seller of Object.keys(inventories).sort()) {
            if (seller === owner || !missing) continue;
            const take = Math.min(missing, stock(seller)[code] ?? 0);
            if (!take) continue;
            stock(seller)[code] -= take;
            stock(owner)[code] = (stock(owner)[code] ?? 0) + take;
            const payment = tradeCost(take);
            transactions.push({ seller, buyer: owner, code, ...payment });
            missing -= take;
          }
        }
      }
      for (const [code, quantity] of Object.entries(recipe.ingredients)) {
        let missing = Math.max(0, quantity - (stock(owner)[code] ?? 0));
        for (const seller of Object.keys(inventories).sort()) {
          if (seller === owner || !missing) continue;
          const take = Math.min(missing, stock(seller)[code] ?? 0);
          if (!take) continue;
          stock(seller)[code] -= take;
          stock(owner)[code] = (stock(owner)[code] ?? 0) + take;
          // 每次实际成交独立计税，不能将几千次小交易合并后才上取整。
          const payment = tradeCost(take);
          transactions.push({ seller, buyer: owner, code, ...payment });
          missing -= take;
        }
        if (missing) {
          add(acquired, owner, code, missing);
          stock(owner)[code] = (stock(owner)[code] ?? 0) + missing;
        }
        stock(owner)[code] -= quantity;
        add(consumed, owner, code, quantity);
      }
      stock(owner)[recipe.product] =
        (stock(owner)[recipe.product] ?? 0) + recipe.quantity;
      add(produced, owner, recipe.product, recipe.quantity);
    }
  }
  const tradedIn = {},
    tradedOut = {};
  for (const trade of transactions) {
    add(tradedIn, trade.buyer, trade.code, trade.quantity);
    add(tradedOut, trade.seller, trade.code, trade.quantity);
  }
  for (const [owner, inventory] of Object.entries(inventories))
    for (const code of new Set([
      ...Object.keys(inventory),
      ...Object.keys(consumed[owner] ?? {}),
    ])) {
      assert.equal(
        (initial[owner]?.[code] ?? 0) +
          (acquired[owner]?.[code] ?? 0) +
          (produced[owner]?.[code] ?? 0) +
          (tradedIn[owner]?.[code] ?? 0),
        (consumed[owner]?.[code] ?? 0) +
          (inventory[code] ?? 0) +
          (tradedOut[owner]?.[code] ?? 0),
        `角色物料不守恒：${owner}/${code}`,
      );
    }
  return {
    inventories,
    acquired,
    produced,
    consumed,
    transactions,
    tradeTax: transactions.reduce((sum, trade) => sum + trade.tax, 0),
  };
}

/** 按实际顺序逐件扣料、产出；同一件的产物不能支付它自己的输入。 */
export function productionLedger(recipes, operations, initial = {}) {
  const inventory = { ...initial };
  const acquired = {};
  const consumed = {};
  const produced = {};
  const reused = {};
  const recipeMap = new Map(recipes.map((recipe) => [recipe.id, recipe]));
  for (const { recipe: id, count } of operations) {
    const recipe = recipeMap.get(id);
    assert(recipe, `未知配方：${id}`);
    assert(
      Number.isSafeInteger(count) && count >= 0,
      `制造次数必须为非负整数：${id}`,
    );
    for (let operation = 0; operation < count; operation++) {
      for (const [code, quantity] of Object.entries(recipe.ingredients)) {
        const used = Math.min(inventory[code] ?? 0, quantity);
        inventory[code] = (inventory[code] ?? 0) - used;
        reused[code] = (reused[code] ?? 0) + used;
        acquired[code] = (acquired[code] ?? 0) + quantity - used;
        consumed[code] = (consumed[code] ?? 0) + quantity;
      }
      inventory[recipe.product] =
        (inventory[recipe.product] ?? 0) + recipe.quantity;
      produced[recipe.product] =
        (produced[recipe.product] ?? 0) + recipe.quantity;
    }
  }
  // 对每种材料检查守恒：初始+取得+制造产出 = 消耗+结存。
  for (const code of new Set([
    ...Object.keys(inventory),
    ...Object.keys(acquired),
  ])) {
    assert.equal(
      (initial[code] ?? 0) + (acquired[code] ?? 0) + (produced[code] ?? 0),
      (consumed[code] ?? 0) + (inventory[code] ?? 0),
      `物料不守恒：${code}`,
    );
  }
  return { acquired, consumed, produced, reused, inventory };
}

/** 返回有向道路上的去程和返程；两者分别搜索，不能把去程倒序假作可走返程。 */
export function placeAccessible(place, facts) {
  const owned = new Set(facts);
  return (
    (!place.requiredFact || owned.has(place.requiredFact)) &&
    (!place.requiredFacts?.length ||
      place.requiredFacts.some((fact) => owned.has(fact))) &&
    (!place.closedByFact || !owned.has(place.closedByFact))
  );
}

/** 只判断一条固定路径的门槛是否可同时满足，不证明玩家已完成这些事实。 */
export function routeFactRequirements(places, route) {
  const byId = new Map(places.map((place) => [place.id, place]));
  const selected = [...new Set(route)].map((id) => {
    assert(byId.has(id), `路径包含未知地点：${id}`);
    return byId.get(id);
  });
  const forbidden = new Set(
    selected.map((place) => place.closedByFact).filter(Boolean),
  );
  const mandatory = new Set(
    selected.map((place) => place.requiredFact).filter(Boolean),
  );
  const anyOfGroups = selected
    .filter((place) => place.requiredFacts?.length)
    .map((place) => place.requiredFacts.filter((fact) => !forbidden.has(fact)));
  return {
    mandatory: [...mandatory],
    forbidden: [...forbidden],
    anyOfGroups,
    satisfiable:
      ![...mandatory].some((fact) => forbidden.has(fact)) &&
      anyOfGroups.every((group) => group.length > 0),
    interpretation:
      "仅检查单项、任选与关闭条件的逻辑兼容，不验证任务因果或事实可获得性",
  };
}

/** 只追踪正式步骤的显式requires，不把目标/条件/战斗视为已经满足。 */
export function questPrerequisiteOrder(steps, targets) {
  const byId = new Map(steps.map((step) => [step.id, step]));
  const done = new Set(),
    visiting = new Set(),
    result = [];
  const visit = (id) => {
    if (done.has(id)) return;
    assert(!visiting.has(id), `任务前置循环：${id}`);
    const step = byId.get(id);
    assert(step, `未知任务前置：${id}`);
    visiting.add(id);
    for (const prerequisite of step.requires ?? []) visit(prerequisite);
    visiting.delete(id);
    done.add(id);
    result.push({
      id,
      quest: step.quest,
      location: step.location,
      requires: step.requires ?? [],
    });
  };
  targets.forEach(visit);
  return result;
}

/** 假定目标步骤被执行时的物品账；缺额只登记外部需求，不宣称已取得或任务已完成。 */
export function questItemRequirements(steps) {
  const inventory = {},
    external = {},
    shortages = [];
  const require = (step, item, quantity) => {
    assert(
      Number.isSafeInteger(quantity) && quantity > 0,
      `任务物品数量无效：${step.id}`,
    );
    const missing = Math.max(0, quantity - (inventory[item] ?? 0));
    if (missing) {
      external[item] = (external[item] ?? 0) + missing;
      shortages.push({ step: step.id, item, quantity: missing });
      inventory[item] = (inventory[item] ?? 0) + missing;
    }
  };
  const apply = (step, effects) => {
    for (const effect of effects ?? []) {
      if (effect.kind === "GIVE_ITEM") {
        assert(Number.isSafeInteger(effect.amount) && effect.amount > 0);
        inventory[effect.reference] =
          (inventory[effect.reference] ?? 0) + effect.amount;
      } else if (effect.kind === "TAKE_ITEM") {
        require(step, effect.reference, effect.amount);
        inventory[effect.reference] -= effect.amount;
      }
    }
  };
  for (const step of steps) {
    apply(step, step.acceptEffects);
    for (const objective of step.objectives ?? [])
      if (
        objective.measure === "HOLDING" &&
        objective.code?.startsWith("ITEM:")
      )
        require(step, objective.code.slice(5), objective.amount);
    apply(step, step.effects);
  }
  return {
    projectedInventory: inventory,
    external,
    shortages,
    interpretation:
      "外部缺额为尚需查证来源的物资；只是显式任务物品账，不执行任务、战斗、事件及其他条件",
  };
}

/** 任务顺序的条件性金币账；外部采购先于本步完成奖励，不补算未列出的战斗成本。 */
export function questGoldBudget(steps, purchases) {
  const stepIds = new Set(steps.map((step) => step.id));
  for (const purchase of purchases)
    assert(stepIds.has(purchase.step), `采购引用未知任务：${purchase.step}`);
  let balance = 0,
    lowest = 0,
    income = 0,
    spending = 0;
  const events = [];
  for (const step of steps) {
    for (const purchase of purchases.filter(
      (purchase) => purchase.step === step.id,
    )) {
      assert(
        Number.isSafeInteger(purchase.gold) && purchase.gold >= 0,
        "任务采购金额必须为非负整数",
      );
      spending += purchase.gold;
      balance -= purchase.gold;
      assert(
        Number.isSafeInteger(spending) && Number.isSafeInteger(balance),
        "任务采购累计金额超出安全整数范围",
      );
      lowest = Math.min(lowest, balance);
      events.push({
        step: step.id,
        kind: "PURCHASE",
        item: purchase.item,
        gold: -purchase.gold,
        balance,
      });
    }
    for (const effect of step.effects ?? []) {
      if (effect.kind !== "GOLD") continue;
      assert(
        Number.isSafeInteger(effect.amount) && effect.amount >= 0,
        "任务奖励金币必须为非负整数",
      );
      income += effect.amount;
      balance += effect.amount;
      assert(
        Number.isSafeInteger(income) && Number.isSafeInteger(balance),
        "任务奖励累计金额超出安全整数范围",
      );
      events.push({
        step: step.id,
        kind: "QUEST_REWARD",
        gold: effect.amount,
        balance,
      });
    }
  }
  assert.equal(balance, income - spending);
  return {
    requiredStartingGold: -lowest,
    income,
    spending,
    balanceChange: balance,
    events,
    interpretation:
      "假定任务可完成后的条件性收支账，不证明已有足够金币或任务实际完成；未计战斗、补药、旅行与其他操作",
  };
}

export function recoveryRoute(
  places,
  start,
  accessible = () => true,
  destination = (place) => place.services?.includes("RECOVERY"),
) {
  const byId = new Map(places.map((place) => [place.id, place]));
  const path = (from, predicate) => {
    const queue = [[from]],
      seen = new Set([from]);
    for (let i = 0; i < queue.length; i++) {
      const current = queue[i],
        last = byId.get(current.at(-1));
      if (!last || !accessible(last)) continue;
      if (predicate(last)) return current;
      for (const next of last.exits ?? [])
        if (!seen.has(next)) {
          seen.add(next);
          queue.push([...current, next]);
        }
    }
    return null;
  };
  assert(byId.has(start), `未知采集地点：${start}`);
  const candidates = places
    .filter((place) => destination(place) && accessible(place))
    .map((place) => ({
      outward: path(start, (p) => p.id === place.id),
      homeward: path(place.id, (p) => p.id === start),
    }))
    .filter((route) => route.outward && route.homeward)
    .sort(
      (a, b) =>
        a.outward.length +
        a.homeward.length -
        (b.outward.length + b.homeward.length),
    );
  assert(candidates.length, `没有可往返的恢复设施：${start}`);
  return candidates[0];
}
