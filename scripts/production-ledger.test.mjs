import { test } from "node:test";
import assert from "node:assert/strict";
import { tradeCost } from "./production-cost-model.mjs";
import {
  productionLedger,
  recoveryRoute,
  partyProductionLedger,
  tradeFunding,
  placeAccessible,
  routeFactRequirements,
  questPrerequisiteOrder,
  questItemRequirements,
  questGoldBudget,
} from "./production-ledger.mjs";

test("任务金币账拒绝累计溢出及非法单笔金额", () => {
  const steps = [
    { id: "A", effects: [{ kind: "GOLD", amount: Number.MAX_SAFE_INTEGER }] },
    { id: "B", effects: [{ kind: "GOLD", amount: 1 }] },
  ];
  assert.throws(() => questGoldBudget(steps, []), /安全整数/);
  assert.throws(
    () =>
      questGoldBudget(
        [{ id: "A" }],
        [
          { step: "A", gold: Number.MAX_SAFE_INTEGER },
          { step: "A", gold: 1 },
        ],
      ),
    /安全整数/,
  );
  assert.throws(
    () => questGoldBudget([{ id: "A" }], [{ step: "A", gold: 0.5 }]),
    /非负整数/,
  );
  assert.equal(
    questGoldBudget(steps.slice(0, 1), []).income,
    Number.MAX_SAFE_INTEGER,
  );
});

test("任务物资先付款再领奖，不允许用本步未来奖励掩盖启动缺口", () => {
  const result = questGoldBudget(
    [
      { id: "A", effects: [{ kind: "GOLD", amount: 10 }] },
      { id: "B", effects: [{ kind: "GOLD", amount: 3 }] },
    ],
    [
      { step: "A", item: "ORE", gold: 6 },
      { step: "B", item: "WOOD", gold: 5 },
    ],
  );
  assert.equal(result.requiredStartingGold, 6);
  assert.equal(result.income, 13);
  assert.equal(result.spending, 11);
  assert.equal(result.balanceChange, 2);
  assert.throws(
    () => questGoldBudget([], [{ step: "UNKNOWN", gold: 1 }]),
    /未知任务/,
  );
  assert.deepEqual(
    result.events.map((event) => event.balance),
    [-6, 4, -1, 2],
  );
});

test("任务物资先用前序与接取奖励，持有不消耗，交付缺额不当免费取得", () => {
  const result = questItemRequirements([
    {
      id: "A",
      effects: [{ kind: "GIVE_ITEM", reference: "TOKEN", amount: 1 }],
    },
    {
      id: "B",
      acceptEffects: [{ kind: "GIVE_ITEM", reference: "LETTER", amount: 1 }],
      objectives: [{ measure: "HOLDING", code: "ITEM:TOKEN", amount: 1 }],
      effects: [
        { kind: "TAKE_ITEM", reference: "TOKEN", amount: 1 },
        { kind: "TAKE_ITEM", reference: "LETTER", amount: 1 },
      ],
    },
    {
      id: "C",
      objectives: [{ measure: "HOLDING", code: "ITEM:ORE", amount: 2 }],
      effects: [{ kind: "TAKE_ITEM", reference: "ORE", amount: 2 }],
    },
  ]);
  assert.deepEqual(result.external, { ORE: 2 });
  assert.deepEqual(result.shortages, [{ step: "C", item: "ORE", quantity: 2 }]);
  assert.deepEqual(result.projectedInventory, { TOKEN: 0, LETTER: 0, ORE: 0 });
});

test("任务前置顺序先于后继，共享前置不重复，未知和循环均拒绝", () => {
  const steps = [
    { id: "A" },
    { id: "B", requires: ["A"] },
    { id: "C", requires: ["A", "B"] },
  ];
  assert.deepEqual(
    questPrerequisiteOrder(steps, ["C"]).map((step) => step.id),
    ["A", "B", "C"],
  );
  assert.throws(
    () => questPrerequisiteOrder(steps, ["MISSING"]),
    /未知任务前置/,
  );
  assert.throws(
    () =>
      questPrerequisiteOrder(
        [
          { id: "A", requires: ["B"] },
          { id: "B", requires: ["A"] },
        ],
        ["A"],
      ),
    /前置循环/,
  );
});

test("固定往返路拒绝互斥事实，并保留可用的任选分支", () => {
  const places = [
    { id: "entry", requiredFact: "OPEN" },
    { id: "bridge", closedByFact: "OPEN" },
    { id: "branch", requiredFacts: ["OPEN", "ALTERNATIVE"] },
  ];
  assert.equal(
    routeFactRequirements(places, ["entry", "bridge"]).satisfiable,
    false,
  );
  const alternative = routeFactRequirements(places, ["branch", "bridge"]);
  assert.equal(alternative.satisfiable, true);
  assert.deepEqual(alternative.anyOfGroups, [["ALTERNATIVE"]]);
  assert.equal(
    routeFactRequirements(
      [{ id: "only", requiredFacts: ["CLOSE"], closedByFact: "CLOSE" }],
      ["only"],
    ).satisfiable,
    false,
  );
  assert.throws(() => routeFactRequirements(places, ["UNKNOWN"]), /未知地点/);
});

test("地图前置遵守单项必须、数组任选及关闭优先规则", () => {
  const place = {
    requiredFact: "MAIN",
    requiredFacts: ["LEFT", "RIGHT"],
    closedByFact: "CLOSED",
  };
  assert.equal(placeAccessible(place, ["MAIN", "LEFT"]), true);
  assert.equal(placeAccessible(place, ["MAIN", "RIGHT"]), true);
  assert.equal(placeAccessible(place, ["LEFT", "RIGHT"]), false);
  assert.equal(placeAccessible(place, ["MAIN"]), false);
  assert.equal(placeAccessible(place, ["MAIN", "LEFT", "CLOSED"]), false);
  assert.equal(placeAccessible({}, []), true);
  const places = [
    { id: "field", exits: ["shop"] },
    { ...place, id: "shop", exits: ["field"] },
  ];
  const route = (facts) =>
    recoveryRoute(
      places,
      "field",
      (p) => placeAccessible(p, facts),
      (p) => p.id === "shop",
    );
  assert.equal(route(["MAIN", "RIGHT"]).outward.at(-1), "shop");
  assert.throws(() => route(["MAIN", "RIGHT", "CLOSED"]));
});

test("周转资金按交易顺序的最大缺口计算，卖方净到账扣税", () => {
  const transactions = [
    { seller: "a", buyer: "b", ...tradeCost(10) },
    { seller: "b", buyer: "a", ...tradeCost(5) },
  ];
  const result = tradeFunding(transactions);
  assert.deepEqual(result.requiredInitialGold, { b: 20, a: 0 });
  assert.deepEqual(result.delta, { b: -11, a: 9 });
  assert.equal(result.totalTax, 2);
  assert.equal(
    tradeFunding(transactions, { a: 0, b: 20 }).firstInsufficient,
    null,
  );
  assert.deepEqual(
    tradeFunding(transactions, { a: 0, b: 0 }).firstInsufficient,
    { index: 0, buyer: "b", required: 20, available: 0 },
  );
});

test("不同角色库存须交易后才能消耗，税按每次成交核对", () => {
  const ledger = partyProductionLedger(
    [
      {
        id: "part",
        skill: "ARTISAN",
        product: "part",
        quantity: 2,
        ingredients: { ore: 3 },
      },
      {
        id: "armor",
        skill: "LEATHERWORK",
        product: "armor",
        quantity: 1,
        ingredients: { part: 1 },
      },
    ],
    [
      { recipe: "part", count: 1 },
      { recipe: "armor", count: 2 },
    ],
    { ARTISAN: "a", LEATHERWORK: "b" },
  );
  assert.equal(ledger.inventories.a.part, 0);
  assert.equal(ledger.inventories.b.armor, 2);
  assert.equal(ledger.transactions.length, 2);
  assert.equal(ledger.tradeTax, 2);
  assert.equal(ledger.acquired.a.ore, 3);
  assert.equal(ledger.acquired.b, undefined);
});

test("按批备料减少成交取整税且不改变原料或产物", () => {
  const recipes = [
    {
      id: "armor",
      skill: "LEATHERWORK",
      product: "armor",
      quantity: 1,
      ingredients: { part: 1 },
    },
  ];
  const initial = { a: { part: 20 } };
  const single = partyProductionLedger(
    recipes,
    [{ recipe: "armor", count: 20 }],
    { LEATHERWORK: "b" },
    initial,
  );
  const batch = partyProductionLedger(
    recipes,
    [{ recipe: "armor", count: 20 }],
    { LEATHERWORK: "b" },
    initial,
    99,
  );
  assert.deepEqual(single.inventories, batch.inventories);
  assert.equal(single.tradeTax, 20);
  assert.equal(batch.tradeTax, 2);
  assert.equal(batch.transactions.length, 1);
});
test("练级产物被后续制造复用，逐件守恒且不重复采购", () => {
  const ledger = productionLedger(
    [
      { id: "part", product: "part", quantity: 2, ingredients: { ore: 3 } },
      { id: "tool", product: "tool", quantity: 1, ingredients: { part: 1 } },
    ],
    [
      { recipe: "part", count: 2 },
      { recipe: "tool", count: 3 },
    ],
  );
  assert.equal(ledger.acquired.ore, 6);
  assert.equal(ledger.acquired.part, 0);
  assert.equal(ledger.reused.part, 3);
  assert.equal(ledger.inventory.part, 1);
  assert.equal(ledger.inventory.tool, 3);
});
test("先扣料后产出，不能倒用未来批次的产物", () => {
  const ledger = productionLedger(
    [{ id: "refine", product: "part", quantity: 2, ingredients: { part: 1 } }],
    [{ recipe: "refine", count: 2 }],
  );
  assert.equal(ledger.acquired.part, 1);
  assert.equal(ledger.inventory.part, 3);
});
test("恢复返程必须单独可达，禁止穿越未开放地图", () => {
  const places = [
    { id: "field", exits: ["clinic"] },
    { id: "clinic", services: ["RECOVERY"], exits: ["bridge"] },
    { id: "bridge", exits: ["field"] },
  ];
  assert.deepEqual(recoveryRoute(places, "field"), {
    outward: ["field", "clinic"],
    homeward: ["clinic", "bridge", "field"],
  });
  assert.throws(() => recoveryRoute(places, "field", (p) => p.id !== "bridge"));
});

test("回收路线只选择指定商店，不能用附近恢复点替代", () => {
  const places = [
    { id: "field", exits: ["clinic", "shop"] },
    { id: "clinic", services: ["RECOVERY"], exits: ["field"] },
    { id: "shop", exits: ["bridge"] },
    { id: "bridge", exits: ["field"] },
  ];
  const destination = (place) => place.id === "shop";
  assert.deepEqual(
    recoveryRoute(places, "field", () => true, destination),
    {
      outward: ["field", "shop"],
      homeward: ["shop", "bridge", "field"],
    },
  );
  assert.throws(() =>
    recoveryRoute(
      places,
      "field",
      (place) => place.id !== "bridge",
      destination,
    ),
  );
});
