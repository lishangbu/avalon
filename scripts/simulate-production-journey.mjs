import assert from "node:assert/strict";
import { readContent, storyContent } from "./content-files.mjs";
import {
  professionBudget,
  manufacturingProgress,
  seededRandom,
  gatheringJourney,
} from "./growth-model.mjs";
import {
  productionLedger,
  recoveryRoute,
  partyProductionLedger,
  tradeFunding,
  routeFactRequirements,
  questPrerequisiteOrder,
  questItemRequirements,
  questGoldBudget,
} from "./production-ledger.mjs";

const production = readContent("professions/life-production.json");
const { recipes } = production;
const { items } = readContent("items/items.json");
const world = readContent("world/opening.json");
const { nodes } = readContent("professions/gathering.json");
const resourceArguments = process.argv.slice(2);
let explicitResources;
if (resourceArguments.length) {
  assert.equal(
    resourceArguments.length,
    4,
    "需要同时提供 --health 整数 --mana 整数",
  );
  const argumentsByName = new Map();
  for (let index = 0; index < resourceArguments.length; index += 2) {
    const key = resourceArguments[index];
    assert(
      ["--health", "--mana"].includes(key) && !argumentsByName.has(key),
      "伙伴容量参数未知或重复",
    );
    assert(
      /^\d+$/.test(resourceArguments[index + 1]),
      "伙伴容量必须是非负整数",
    );
    argumentsByName.set(key, Number(resourceArguments[index + 1]));
  }
  explicitResources = {
    health: argumentsByName.get("--health"),
    mana: argumentsByName.get("--mana"),
  };
  assert(
    Number.isSafeInteger(explicitResources.health) &&
      explicitResources.health > 25 &&
      Number.isSafeInteger(explicitResources.mana) &&
      explicitResources.mana >= 10,
    "伙伴容量须能支持金档一轮采集",
  );
}
const budgets = Object.entries({
  ARTISAN: 9,
  ALCHEMY: 10,
  COOKING: 9,
  LEATHERWORK: 9,
}).map(([skill, target]) => professionBudget(recipes, skill, target));
// 按配方等级逐阶段安排四名制造者，供料顺序明确，不能用最后的全队库存抵扣早期欠料。
const training = budgets
  .flatMap((b) => b.byLevel.map((step) => ({ ...step, skill: b.skill })))
  .sort(
    (a, b) =>
      a.level - b.level ||
      budgets.findIndex((v) => v.skill === a.skill) -
        budgets.findIndex((v) => v.skill === b.skill),
  );
const finalOperations = [
  { recipe: "E_FORGED_BODY", count: 1 },
  { recipe: "LIFE_HEALTH_POTION_4", count: 20 },
  { recipe: "LIFE_MANA_POTION_4", count: 10 },
];
const trainingLedger = productionLedger(recipes, training);
const qualifications = manufacturingProgress(
  recipes,
  [...training, ...finalOperations],
  {
    ARTISAN: 0,
    ALCHEMY: 0,
    COOKING: 0,
    LEATHERWORK: 0,
  },
  ["E_FORGE_BLUEPRINT"],
);
const combined = productionLedger(recipes, [...training, ...finalOperations]);
const finalLedger = productionLedger(
  recipes,
  finalOperations,
  trainingLedger.inventory,
);
const partyLedger = partyProductionLedger(
  recipes,
  [...training, ...finalOperations],
  {
    ARTISAN: "artisan",
    ALCHEMY: "alchemist",
    LEATHERWORK: "leatherworker",
    COOKING: "cook",
  },
);
const owners = {
  ARTISAN: "artisan",
  ALCHEMY: "alchemist",
  LEATHERWORK: "leatherworker",
  COOKING: "cook",
};
const batchedPartyLedger = partyProductionLedger(
  recipes,
  [...training, ...finalOperations],
  owners,
  {},
  99,
);
assert.deepEqual(
  batchedPartyLedger.inventories,
  partyLedger.inventories,
  "集中交易不能改变制造结存",
);
assert.deepEqual(
  batchedPartyLedger.acquired,
  partyLedger.acquired,
  "集中交易不能省略原料缺额",
);
// 两种计算路径必须得到相同的最终库存与外部取得总量。
for (const code of new Set([
  ...Object.keys(combined.acquired),
  ...Object.keys(finalLedger.acquired),
])) {
  assert.equal(
    combined.acquired[code] ?? 0,
    (trainingLedger.acquired[code] ?? 0) + (finalLedger.acquired[code] ?? 0),
    `练级和成品衔接不守恒：${code}`,
  );
}
const bootstrap = items
  .filter((i) => i.tool?.tier === 0)
  .map((tool) => {
    const offer = production.offers.find((o) => o.item === tool.id);
    assert(offer, `首把工具无购买来源：${tool.id}`);
    return {
      tool: tool.id,
      quantity: 1,
      gold: offer.price,
      locations: offer.locations,
    };
  });
const recovery = nodes
  .filter((n) => n.level >= 9)
  .map((node) => {
    const route = recoveryRoute(world.places, node.location);
    const factCompatibility = routeFactRequirements(world.places, [
      ...route.outward,
      ...route.homeward,
    ]);
    assert(factCompatibility.satisfiable, `恢复路线地图条件互斥：${node.id}`);
    return {
      node: node.id,
      location: node.location,
      ...route,
      factCompatibility,
    };
  });
// 单人固定三个专业；只把实际目录中的采购来源记为金币支出，采集缺额不当作免费库存。
const soloTraining = professionBudget(
  recipes.filter((recipe) => recipe.product.startsWith("LIFE_WRAP_")),
  "LEATHERWORK",
  9,
).byLevel;
const soloOperations = [
  ...soloTraining,
  { recipe: "LIFE_WRAP_4", count: 3 },
  { recipe: "E_FORGED_BODY", count: 1 },
];
const soloLedger = productionLedger(recipes, soloOperations);
const soloGatheringSources = new Set(
  nodes
    .filter((node) => ["HUNTING", "HERBALISM"].includes(node.skill))
    .flatMap((node) => [node.product, node.rare].filter(Boolean)),
);
const soloMaterials = Object.entries(soloLedger.acquired).map(
  ([item, quantity]) => {
    if (soloGatheringSources.has(item))
      return { item, quantity, source: "gathering" };
    const offer = production.offers.find((offer) => offer.item === item);
    assert(offer, `单人路线缺少来源：${item}`);
    return {
      item,
      quantity,
      source: "shop",
      gold: quantity * offer.price,
      locations: offer.locations,
    };
  },
);
const soloTools = items
  .filter(
    (item) =>
      item.tool && ["TRAP", "PINCERS", "NEEDLE"].includes(item.tool.kind),
  )
  .map((item) => {
    const offer = production.offers.find((offer) => offer.item === item.id);
    assert(offer, `单人工具缺少来源：${item.id}`);
    return {
      item: item.id,
      tier: item.tool.tier,
      gold: offer.price,
      locations: offer.locations,
    };
  });
const soloRoute = {
  skills: ["HUNTING", "HERBALISM", "LEATHERWORK"],
  trainingCrafts: soloTraining.reduce(
    (sum, operation) => sum + operation.count,
    0,
  ),
  qualifications: manufacturingProgress(
    recipes,
    soloOperations,
    { LEATHERWORK: 0 },
    ["E_FORGE_BLUEPRINT"],
  ),
  operations: soloOperations,
  ledger: soloLedger,
  materials: soloMaterials,
  firstToolOfEachTier: soloTools,
  materialPurchaseGold: soloMaterials.reduce(
    (sum, item) => sum + (item.gold ?? 0),
    0,
  ),
  firstToolsGold: soloTools.reduce((sum, item) => sum + item.gold, 0),
  exclusions: [
    "本节仅为制造材料账；从零采集见gathering，交错资金见interleavedWithSurplusSales，不表示原料免费或已取得",
    "工具每档每类只列一把，未抵扣教学赠礼，未计随机报废后的替换工具",
    "专业学习费、人物成长、剧情解锁、恢复、旅行、容量及绑定仍未纳入",
    "金币为采购支出，未给定起始金币或抵扣产物出售，不能解释为已通过资金验收",
  ],
};
soloRoute.goldMaterialQuestPrerequisites = {
  gate: "QUEST:J025",
  order: questPrerequisiteOrder(storyContent().steps, ["J025"]),
  interpretation:
    "晨金谷地入口及金档原料的显式任务前置顺序；不执行任务目标、战斗、剧情条件或奖励，不代表已完成",
};
const storySteps = storyContent().steps;
soloRoute.goldMaterialQuestPrerequisites.itemRequirements =
  questItemRequirements(
    soloRoute.goldMaterialQuestPrerequisites.order.map((entry) =>
      storySteps.find((step) => step.id === entry.id),
    ),
  );
const sceneActions = readContent("world/scene-actions.json").actions;
const encounters = readContent("battle/battle.json").encounters;
soloRoute.goldMaterialQuestPrerequisites.externalSourceAudit = Object.entries(
  soloRoute.goldMaterialQuestPrerequisites.itemRequirements.external,
).map(([item, quantity]) => ({
  item,
  quantity,
  shop: production.offers.filter((offer) => offer.item === item),
  gathering: nodes
    .filter((node) => node.product === item || node.rare === item)
    .map((node) => ({
      id: node.id,
      skill: node.skill,
      level: node.level,
      location: node.location,
      rare: node.rare === item,
    })),
  scenes: sceneActions
    .filter(
      (action) =>
        action.type === "COLLECT_TASK" &&
        action.itemCode === item &&
        action.quantity > 0,
    )
    .map((action) => ({
      id: action.id,
      type: action.type,
      location: action.location,
      requires: action.requires,
      quantity: action.quantity,
    })),
  encounters: encounters
    .filter((encounter) => (encounter.victoryDrops?.[item] ?? 0) > 0)
    .map((encounter) => ({
      id: encounter.id,
      location: encounter.location,
      requires: encounter.requires,
      quantity: encounter.victoryDrops[item],
    })),
  questRewards: storySteps
    .filter((step) =>
      [...(step.acceptEffects ?? []), ...(step.effects ?? [])].some(
        (effect) => effect.kind === "GIVE_ITEM" && effect.reference === item,
      ),
    )
    .map((step) => ({ id: step.id, location: step.location })),
  interpretation:
    "正式目录中的候选来源，未执行取得、未验证前置及消耗条件，也未将候选发放计为实际库存",
}));
const assumedStepStates = new Map();
const priorSteps = new Set(),
  priorQuestFacts = new Set();
for (const entry of soloRoute.goldMaterialQuestPrerequisites.order) {
  const step = storySteps.find((step) => step.id === entry.id);
  assumedStepStates.set(step.id, {
    steps: new Set(priorSteps),
    facts: new Set([...priorQuestFacts, `QUEST_ACCEPT:${step.quest}`]),
  });
  priorSteps.add(step.id);
  if (step.completes) priorQuestFacts.add(`QUEST:${step.quest}`);
}
soloRoute.goldMaterialQuestPrerequisites.declaredSourceTiming =
  soloRoute.goldMaterialQuestPrerequisites.itemRequirements.shortages.map(
    (shortage) => {
      const state = assumedStepStates.get(shortage.step);
      const sources =
        soloRoute.goldMaterialQuestPrerequisites.externalSourceAudit.find(
          (source) => source.item === shortage.item,
        );
      return {
        ...shortage,
        scenes: sources.scenes.map((scene) => ({
          id: scene.id,
          unmetRequires: (scene.requires ?? []).filter(
            (fact) => !state.facts.has(fact),
          ),
        })),
        encounters: sources.encounters.map((encounter) => ({
          id: encounter.id,
          unmetRequires:
            encounter.requires && !state.steps.has(encounter.requires)
              ? [encounter.requires]
              : [],
        })),
        interpretation:
          "假定显式前置顺序中的此前任务已完成、当前任务已接取；仅检查场景requires与遭遇requires，不检查地点、战斗、事件、其他事实或资金，不是来源已取得",
      };
    },
  );
soloRoute.goldMaterialQuestPrerequisites.purchaseAllListedDeficits =
  questGoldBudget(
    soloRoute.goldMaterialQuestPrerequisites.order.map((entry) =>
      storySteps.find((step) => step.id === entry.id),
    ),
    soloRoute.goldMaterialQuestPrerequisites.itemRequirements.shortages.flatMap(
      (shortage) => {
        const offer = production.offers.find(
          (offer) => offer.item === shortage.item,
        );
        return offer
          ? [{ ...shortage, gold: shortage.quantity * offer.price }]
          : [];
      },
    ),
  );
const soloGatheringNodes = nodes.filter((node) =>
  ["HUNTING", "HERBALISM"].includes(node.skill),
);
const soloGatheringTools = items
  .filter((item) => item.tool && ["TRAP", "PINCERS"].includes(item.tool.kind))
  .map((item) => ({
    item: item.id,
    skill: item.tool.kind === "TRAP" ? "HUNTING" : "HERBALISM",
    tier: item.tool.tier,
    breakDenominator: item.tool.breakDenominator,
    price: production.offers.find((offer) => offer.item === item.id).price,
  }));
soloRoute.shopRoundTrips = soloGatheringNodes.map((node) => {
  const route = recoveryRoute(
    world.places,
    node.location,
    () => true,
    (place) => ["SUPPLY_SHOP", "CITY_SHOP"].includes(place.id),
  );
  assert(["SUPPLY_SHOP", "CITY_SHOP"].includes(route.outward.at(-1)));
  return {
    node: node.id,
    location: node.location,
    ...route,
    roadSteps: route.outward.length + route.homeward.length - 2,
    factCompatibility: routeFactRequirements(world.places, [
      ...route.outward,
      ...route.homeward,
    ]),
    accessRequirements: [...new Set([...route.outward, ...route.homeward])].map(
      (id) => {
        const place = world.places.find((place) => place.id === id);
        return {
          location: id,
          requiredFact: place.requiredFact ?? null,
          anyOfFacts: place.requiredFacts ?? [],
          closedByFact: place.closedByFact ?? null,
        };
      },
    ),
  };
});
for (const route of soloRoute.shopRoundTrips)
  assert(
    route.factCompatibility.satisfiable,
    `报告所选回收路线包含互斥地图条件：${route.node}`,
  );
soloRoute.gathering = gatheringJourney(
  soloGatheringNodes,
  Object.fromEntries(
    soloMaterials
      .filter((item) => item.source === "gathering")
      .map((item) => [item.item, item.quantity]),
  ),
  soloGatheringTools,
  seededRandom(1),
);
soloRoute.gathering.assumptions = [
  "采集专业已学习且熟练度为零，教学赠送两把木工具；后续工具按城市商品价格补购",
  "所有节点和城市已解锁，资源不足时假定可恢复；未扣生命魔力容量、恢复金币或旅行时间",
  "普通与稀有产物共用库存，超额产物未出售；未检查背包及绑定",
  "本节单独计算采集，交错现金流另见interleavedCash与interleavedWithSurplusSales；两节支出不能重复相加",
];
soloRoute.gathering.timeSensitivity = [60, 80, 100].map((targetHours) => ({
  targetHours,
  currentHours: soloRoute.gathering.elapsedMillis / 3600000,
  uniformTimerSpeedMultiplier:
    soloRoute.gathering.elapsedMillis / (targetHours * 3600000),
  interpretation:
    "仅在采集轮数、节点选择与随机结果不变，所有轮次计时统一缩短时成立；改变产量或熟练度必须重新模拟，不可按此倍率推算",
}));
// 仅制造侧现金流：每99次操作后出售本批练级包覆，采购须发生在下一件制造之前。
function soloManufacturingCash(
  random,
  includeGathering = false,
  sellGatheringSurplus = false,
  retainOnlyCurrentBatch = false,
  saleSlotThreshold,
  resourceLimits,
  proficiencyMultiplier = 1,
) {
  let balance = 0,
    lowest = 0,
    sales = 0,
    purchases = 0,
    broken = 0;
  const heldTools = new Map([["TOOL_WOOD_NEEDLE", 1]]);
  const stages = [];
  const manufacturingSaleTravel = { visits: 0, roadSteps: 0 };
  const debit = (amount) => {
    purchases += amount;
    balance -= amount;
    lowest = Math.min(lowest, balance);
  };
  let gatheringState;
  let peakMaterialSlots = 0;
  const saleTravel = {
    visits: 0,
    roadSteps: 0,
    unresolvedInitialInventoryVisits: 0,
  };
  const recoveryTravel = { visits: 0, roadSteps: 0 };
  const transferTravel = {
    moves: 0,
    roadSteps: 0,
    start: "GNOME_LEATHERWORKS",
  };
  let location = transferTravel.start;
  const transferRoutes = new Map();
  const travelTo = (destination) => {
    if (location === destination) return;
    const key = `${location}->${destination}`;
    if (!transferRoutes.has(key)) {
      const route = recoveryRoute(
        world.places,
        location,
        () => true,
        (place) => place.id === destination,
      ).outward;
      assert(
        routeFactRequirements(world.places, route).satisfiable,
        `转场道路条件互斥：${key}`,
      );
      transferRoutes.set(key, route);
    }
    transferTravel.moves++;
    transferTravel.roadSteps += transferRoutes.get(key).length - 1;
    location = destination;
  };
  const recoveryRoutes = new Map();
  let peakManufacturingSlots = 0;
  let batchMaterials = {};
  const gatheringTotals = {
    rounds: 0,
    toolGold: 0,
    salesGold: 0,
    elapsedMillis: 0,
    healthSpent: 0,
    manaSpent: 0,
  };
  const purchaseTravel = { visits: 0, roadSteps: 0 };
  const purchaseRoutes = new Map();
  const recordPurchaseVisit = (item) => {
    if (!includeGathering) return;
    const offer = production.offers.find((offer) => offer.item === item);
    assert(offer, `采购道路缺少正式商品：${item}`);
    const key = `${location}:${item}`;
    if (!purchaseRoutes.has(key)) {
      const route = recoveryRoute(
        world.places,
        location,
        () => true,
        (place) => offer.locations.includes(place.id),
      );
      assert(
        routeFactRequirements(world.places, [
          ...route.outward,
          ...route.homeward,
        ]).satisfiable,
        `采购道路事实互斥：${item}`,
      );
      purchaseRoutes.set(key, route);
    }
    const route = purchaseRoutes.get(key);
    purchaseTravel.visits++;
    purchaseTravel.roadSteps +=
      route.outward.length + route.homeward.length - 2;
  };
  const acquire = (demands, facility) => {
    if (!includeGathering) return;
    const initialLocation = location;
    const result = gatheringJourney(
      soloGatheringNodes,
      demands,
      soloGatheringTools,
      random,
      gatheringState,
      {
        saleSlotThreshold,
        resources: resourceLimits,
        proficiencyMultiplier,
        onNode: (node) => travelTo(node.location),
        onRecovery: (event) => {
          if (!recoveryRoutes.has(event.location))
            recoveryRoutes.set(
              event.location,
              recoveryRoute(world.places, event.location),
            );
          const route = recoveryRoutes.get(event.location);
          assert(
            routeFactRequirements(world.places, [
              ...route.outward,
              ...route.homeward,
            ]).satisfiable,
            `恢复事件采用了互斥道路：${event.node}`,
          );
          recoveryTravel.visits++;
          recoveryTravel.roadSteps +=
            route.outward.length + route.homeward.length - 2;
        },
        sellPrices: sellGatheringSurplus
          ? Object.fromEntries(
              items
                .filter(
                  (item) =>
                    item.sellPrice > 0 &&
                    (retainOnlyCurrentBatch ||
                      !soloMaterials.some(
                        (material) => material.item === item.id,
                      )),
                )
                .map((item) => [item.id, item.sellPrice]),
            )
          : {},
        onTransaction: (transaction) => {
          if (transaction.gold < 0) {
            recordPurchaseVisit(transaction.item);
            debit(-transaction.gold);
          } else {
            sales += transaction.gold;
            balance += transaction.gold;
          }
        },
      },
    );
    gatheringState = {
      inventory: result.leftovers,
      skills: result.skills,
      heldTools: result.heldTools,
      lastNode: result.lastNode,
      resources: result.resources,
    };
    peakMaterialSlots = Math.max(peakMaterialSlots, result.peakMaterialSlots);
    for (const key in gatheringTotals) gatheringTotals[key] += result[key];
    for (const visit of result.saleVisits) {
      if (visit.afterRound === 0) {
        const route = recoveryRoute(
          world.places,
          initialLocation,
          () => true,
          (place) => ["SUPPLY_SHOP", "CITY_SHOP"].includes(place.id),
        );
        assert(
          routeFactRequirements(world.places, [
            ...route.outward,
            ...route.homeward,
          ]).satisfiable,
        );
        saleTravel.visits++;
        saleTravel.roadSteps +=
          route.outward.length + route.homeward.length - 2;
        continue;
      }
      if (!visit.node) {
        saleTravel.unresolvedInitialInventoryVisits++;
        continue;
      }
      const route = soloRoute.shopRoundTrips.find(
        (route) => route.node === visit.node,
      );
      assert(route, `回收事件缺少正式道路：${visit.node}`);
      saleTravel.visits++;
      saleTravel.roadSteps += route.roadSteps;
    }
    travelTo(facility);
  };
  for (const operation of soloTraining) {
    const recipe = recipes.find((recipe) => recipe.id === operation.recipe);
    const tool = items.find(
      (item) =>
        item.tool?.kind === "NEEDLE" &&
        item.tool.tier === Math.floor((recipe.level - 1) / 2),
    );
    const price = production.offers.find(
      (offer) => offer.item === tool.id,
    ).price;
    const sellPrice = items.find(
      (item) => item.id === recipe.product,
    ).sellPrice;
    assert(Number.isSafeInteger(sellPrice) && sellPrice > 0);
    const saleRoute = recoveryRoute(
      world.places,
      recipe.locations[0],
      () => true,
      (place) => ["SUPPLY_SHOP", "CITY_SHOP"].includes(place.id),
    );
    assert(
      routeFactRequirements(world.places, [
        ...saleRoute.outward,
        ...saleRoute.homeward,
      ]).satisfiable,
      `制造回收道路条件互斥：${recipe.id}`,
    );
    let pending = 0,
      produced = 0;
    const before = balance;
    for (let piece = 0; piece < operation.count; piece++) {
      if (piece % 99 === 0) {
        const batch = Math.min(99, operation.count - piece);
        batchMaterials = Object.fromEntries(
          Object.entries(recipe.ingredients).map(([item, count]) => [
            item,
            count * batch,
          ]),
        );
        acquire(
          Object.fromEntries(
            Object.entries(recipe.ingredients).map(([item, count]) => [
              item,
              count * batch,
            ]),
          ),
          recipe.locations[0],
        );
      }
      if (!(heldTools.get(tool.id) > 0)) {
        recordPurchaseVisit(tool.id);
        debit(price);
        heldTools.set(tool.id, 1);
      }
      produced += recipe.quantity;
      pending += recipe.quantity;
      for (const [item, count] of Object.entries(recipe.ingredients)) {
        batchMaterials[item] -= count;
        assert(batchMaterials[item] >= 0, "本批材料不能倒用后续采集");
      }
      peakManufacturingSlots = Math.max(
        peakManufacturingSlots,
        Object.entries(batchMaterials).reduce(
          (sum, [item, quantity]) =>
            sum +
            Math.ceil((quantity + (gatheringState?.inventory[item] ?? 0)) / 99),
          0,
        ) +
          Object.entries(gatheringState?.inventory ?? {})
            .filter(([item]) => !(item in batchMaterials))
            .reduce((sum, [, quantity]) => sum + Math.ceil(quantity / 99), 0) +
          Math.ceil(pending / 99),
      );
      if (random() < 1 / tool.tool.breakDenominator) {
        heldTools.set(tool.id, 0);
        broken++;
      }
      if ((piece + 1) % 99 === 0 || piece + 1 === operation.count) {
        // 超过99件时拆成多笔正式回收，单价不变且没有市场交易税。
        sales += pending * sellPrice;
        balance += pending * sellPrice;
        manufacturingSaleTravel.visits++;
        manufacturingSaleTravel.roadSteps +=
          saleRoute.outward.length + saleRoute.homeward.length - 2;
        pending = 0;
      }
    }
    assert.equal(produced, operation.count * recipe.quantity);
    assert.equal(pending, 0);
    stages.push({
      recipe: recipe.id,
      sold: produced,
      goldChange: balance - before,
      cumulativeGoldChange: balance,
    });
  }
  // 最终护衣的采购与制造针具补购独立列入；三份四级包覆与护衣保留，不出售。
  const finalDemands = {};
  for (const operation of soloOperations.slice(-2)) {
    const recipe = recipes.find((recipe) => recipe.id === operation.recipe);
    for (const [item, count] of Object.entries(recipe.ingredients))
      if (soloGatheringSources.has(item))
        finalDemands[item] =
          (finalDemands[item] ?? 0) + count * operation.count;
  }
  acquire(
    finalDemands,
    recipes.find((recipe) => recipe.id === "E_FORGED_BODY").locations[0],
  );
  debit(soloRoute.materialPurchaseGold);
  for (const material of soloMaterials.filter(
    (material) => material.source === "shop",
  ))
    recordPurchaseVisit(material.item);
  const goldNeedle = items.find((item) => item.id === "TOOL_GOLD_NEEDLE");
  const goldPrice = production.offers.find(
    (offer) => offer.item === goldNeedle.id,
  ).price;
  for (let piece = 0; piece < 4; piece++) {
    if (!(heldTools.get(goldNeedle.id) > 0)) {
      recordPurchaseVisit(goldNeedle.id);
      debit(goldPrice);
      heldTools.set(goldNeedle.id, 1);
    }
    if (random() < 1 / goldNeedle.tool.breakDenominator) {
      heldTools.set(goldNeedle.id, 0);
      broken++;
    }
  }
  assert.equal(balance, sales - purchases);
  assert.equal(
    manufacturingSaleTravel.visits,
    soloTraining.reduce(
      (sum, operation) => sum + Math.ceil(operation.count / 99),
      0,
    ),
    "制造回收次数按批次而非99件拆单计数",
  );
  const reservedToolSlots = soloTools.length;
  const finalMaterials = productionLedger(
    recipes,
    soloOperations.slice(-2),
  ).acquired;
  const finalInventoryBound = { ...gatheringState?.inventory };
  for (const [item, count] of Object.entries(finalMaterials))
    finalInventoryBound[item] = (finalInventoryBound[item] ?? 0) + count;
  const finalStageSlotBound =
    Object.values(finalInventoryBound).reduce(
      (sum, quantity) => sum + Math.ceil(quantity / 99),
      0,
    ) + 2;
  return {
    requiredStartingGold: -lowest,
    sales,
    purchases,
    broken,
    finalGoldChange: balance,
    stages,
    manufacturingSaleTravel,
    phaseCapacityBound: {
      reservedToolSlots,
      peakManufacturingMaterialSlots: peakManufacturingSlots,
      finalStageSlotBound,
      finalLeftoverMaterialSlots: Object.values(
        gatheringState?.inventory ?? {},
      ).reduce((sum, quantity) => sum + Math.ceil(quantity / 99), 0),
      totalSlots:
        Math.max(
          peakMaterialSlots,
          peakManufacturingSlots,
          finalStageSlotBound,
        ) + reservedToolSlots,
      capacity: 60,
      interpretation:
        "统一未绑定材料模型，预留三类五档各一把工具；最终阶段含原料、包覆及护衣空间。额外装备、补药、绑定分堆和实际往返不在此界内",
    },
    ...(includeGathering
      ? {
          gatheringTotals,
          saleTravel,
          purchaseTravel,
          transferTravel: { ...transferTravel, end: location },
          ...(resourceLimits ? { resourceLimits, recoveryTravel } : {}),
          gatheringState,
          materialCapacityAudit: {
            peakSlots: peakMaterialSlots,
            capacity: 60,
            fits: peakMaterialSlots <= 60,
            interpretation:
              "仅同名未绑定普通材料每99件一格；未计工具、装备、绑定分堆，超限即证明本策略无法照此执行",
          },
        }
      : {}),
  };
}
const cashSamples = Array.from({ length: 100 }, (_, index) =>
  soloManufacturingCash(seededRandom(index + 1)),
);
soloRoute.interleavedCash = {
  seed: 1,
  ...soloManufacturingCash(seededRandom(1), true),
  exclusions: [
    "采集与制造按每99次制造的原料批次交错，专业及工具状态连续保留",
    "采集资源假定可恢复，地图全开；不含恢复、学习费、人物成长、旅行、背包和绑定",
    "多余原料未出售；资金不足仅记录缺口，未自动插入巡猎或其他收入",
  ],
};
soloRoute.interleavedWithSurplusSales = {
  seed: 1,
  ...soloManufacturingCash(seededRandom(1), true, true),
  exclusions: [
    "只出售整条制造材料计划不需要的采集产物，保留所有计划内原料",
    "每99轮采集或本批取得结束后回收；金额按正式单价，多于99件拆单",
    "地图与恢复资源假定可用，未计恢复、往返回收地点、背包、绑定及学习费用",
  ],
};
soloRoute.currentBatchRetention = {
  seed: 1,
  ...soloManufacturingCash(seededRandom(1), true, true, true),
  exclusions: [
    "仅保留本次采集需求，未来材料允许出售并在需要时重新采集",
    "按每99轮或本批结束回收，尚未实际安排往返回收地点与恢复",
    "容量审计只含材料，不含工具、装备、绑定分堆；容量通过也不能证明完整可执行",
  ],
};
soloRoute.capacityTriggeredSales = {
  seed: 1,
  materialSlotThreshold: 35,
  ...soloManufacturingCash(seededRandom(1), true, true, true, 35),
  exclusions: [
    "材料达到35格或本批结束后回收，15格工具预留不变",
    "资金允许记录负余额以计算起始需求；未按金币不足提前回城",
    "仍未模拟资源恢复、节点转场、绑定和额外装备，不代表完整可执行路线",
  ],
};
soloRoute.hypotheticalDoubleGatheringProficiency = {
  productionRules: false,
  seed: 1,
  gatheringProficiencyMultiplier: 2,
  ...soloManufacturingCash(seededRandom(1), true, true, true, 35, undefined, 2),
  interpretation:
    "只将每次成功采集的基础熟练度从1改为2重新运行；制造、产出概率、工具、价格及任务不变。仅数值方案研究，正式游戏未改，沿用容量/剧情/资源等排除项",
};
if (explicitResources) {
  soloRoute.explicitResourceScenario = {
    seed: 1,
    ...soloManufacturingCash(
      seededRandom(1),
      true,
      true,
      true,
      35,
      explicitResources,
    ),
    exclusions: [
      "容量由命令行明确提供，伙伴成长与容量变化未模拟",
      "恢复按正式设施免费，但每次独立往返，不与回收行程合并；地图门槛仍假定已满足",
      "不含节点转场、工具采购路程、启停操作时间、额外装备及绑定；不能视为从零完整实测",
    ],
  };
  assert.equal(
    soloRoute.explicitResourceScenario.gatheringTotals.rounds,
    soloRoute.capacityTriggeredSales.gatheringTotals.rounds,
    "恢复不能改变同种子采集结果",
  );
}
assert.equal(
  soloRoute.capacityTriggeredSales.purchases,
  soloRoute.currentBatchRetention.purchases,
  "仅变更回收时点不能省略工具和辅料采购",
);
assert.equal(
  soloRoute.capacityTriggeredSales.sales,
  soloRoute.currentBatchRetention.sales,
  "仅变更回收时点不能改变出售总量",
);
assert.equal(
  soloRoute.capacityTriggeredSales.gatheringTotals.rounds,
  soloRoute.currentBatchRetention.gatheringTotals.rounds,
  "回城频率不能改变同种子的采集轮数",
);
soloRoute.salePolicyComparison = {
  fixedRound: {
    visits: soloRoute.currentBatchRetention.saleTravel.visits,
    roadSteps: soloRoute.currentBatchRetention.saleTravel.roadSteps,
    startingGold: soloRoute.currentBatchRetention.requiredStartingGold,
  },
  capacityTriggered: {
    visits: soloRoute.capacityTriggeredSales.saleTravel.visits,
    roadSteps: soloRoute.capacityTriggeredSales.saleTravel.roadSteps,
    startingGold: soloRoute.capacityTriggeredSales.requiredStartingGold,
  },
  interpretation:
    "相同种子、产出、采购与总收入，仅比较回收时点及对应道路；未纳入恢复和其他旅行",
};
assert(
  soloRoute.capacityTriggeredSales.phaseCapacityBound.totalSlots <= 60,
  "按容量回收必须继续满足指定生产库存60格约束",
);
assert(
  soloRoute.currentBatchRetention.phaseCapacityBound.totalSlots <= 60,
  "仅保留本批材料的指定未绑定生产库存必须能容纳在60格内；其他物品仍需独立预留",
);
assert(
  soloRoute.interleavedWithSurplusSales.phaseCapacityBound.totalSlots > 60,
  "保留全程原料策略必须如实报告容量失败",
);
const soloSamples = Array.from({ length: 20 }, (_, index) =>
  soloManufacturingCash(seededRandom(index + 1), true, true),
);
const sampleRange = (values) => {
  const sorted = [...values].sort((a, b) => a - b);
  return {
    min: sorted[0],
    median: (sorted[9] + sorted[10]) / 2,
    max: sorted.at(-1),
  };
};
soloRoute.interleavedSampleRanges = {
  seeds: { first: 1, last: 20 },
  onlineHours: sampleRange(
    soloSamples.map((sample) => sample.gatheringTotals.elapsedMillis / 3600000),
  ),
  gatheringToolGold: sampleRange(
    soloSamples.map((sample) => sample.gatheringTotals.toolGold),
  ),
  requiredStartingGold: sampleRange(
    soloSamples.map((sample) => sample.requiredStartingGold),
  ),
  interpretation:
    "固定策略下的20个确定种子样本范围，不是玩家总体分位数或最坏情况保证；沿用交错路线全部排除项",
};
const capacitySamples = Array.from({ length: 20 }, (_, index) =>
  soloManufacturingCash(seededRandom(index + 1), true, true, true, 35),
);
for (const [index, sample] of capacitySamples.entries()) {
  assert(
    sample.phaseCapacityBound.totalSlots <= 60,
    `容量策略种子${index + 1}超出60格`,
  );
  assert.equal(sample.saleTravel.unresolvedInitialInventoryVisits, 0);
}
soloRoute.capacityTriggeredSampleRanges = {
  seeds: { first: 1, last: 20 },
  productionSlots: sampleRange(
    capacitySamples.map((sample) => sample.phaseCapacityBound.totalSlots),
  ),
  startingGold: sampleRange(
    capacitySamples.map((sample) => sample.requiredStartingGold),
  ),
  gatheringSaleVisits: sampleRange(
    capacitySamples.map((sample) => sample.saleTravel.visits),
  ),
  roadStepsExcludingRecovery: sampleRange(
    capacitySamples.map(
      (sample) =>
        sample.saleTravel.roadSteps +
        sample.manufacturingSaleTravel.roadSteps +
        sample.purchaseTravel.roadSteps +
        sample.transferTravel.roadSteps,
    ),
  ),
  interpretation:
    "固定35格回收策略20个种子样本；只计指定未绑定生产库存，未指定伙伴容量，不含恢复及真实剧情顺序，不是最坏情况保证",
};
const hypotheticalSamples = Array.from({ length: 20 }, (_, index) =>
  soloManufacturingCash(
    seededRandom(index + 1),
    true,
    true,
    true,
    35,
    undefined,
    2,
  ),
);
for (const [index, sample] of hypotheticalSamples.entries())
  assert(
    sample.phaseCapacityBound.totalSlots <= 60,
    `双倍熟练度情景种子${index + 1}超出指定库存界`,
  );
soloRoute.hypotheticalDoubleProficiencyRanges = {
  productionRules: false,
  seeds: { first: 1, last: 20 },
  onlineHours: sampleRange(
    hypotheticalSamples.map(
      (sample) => sample.gatheringTotals.elapsedMillis / 3600000,
    ),
  ),
  gatheringToolGold: sampleRange(
    hypotheticalSamples.map((sample) => sample.gatheringTotals.toolGold),
  ),
  startingGold: sampleRange(
    hypotheticalSamples.map((sample) => sample.requiredStartingGold),
  ),
  productionSlots: sampleRange(
    hypotheticalSamples.map((sample) => sample.phaseCapacityBound.totalSlots),
  ),
  interpretation:
    "仅分析双倍成功采集熟练度；正式规则未改，样本不含恢复、剧情解锁和额外库存，也不是总体最坏情况保证",
};
assert.deepEqual(
  soloSamples[0],
  soloManufacturingCash(seededRandom(1), true, true),
  "完整交错路线同种子必须复现",
);
assert.equal(
  soloRoute.interleavedWithSurplusSales.purchases,
  soloRoute.interleavedCash.purchases,
  "出售额外材料不能省略采购",
);
assert.equal(
  soloRoute.interleavedWithSurplusSales.gatheringTotals.rounds,
  soloRoute.interleavedCash.gatheringTotals.rounds,
  "出售计划外材料不能改变需求采集轮数",
);
assert.equal(
  soloRoute.interleavedWithSurplusSales.sales,
  soloRoute.interleavedCash.sales +
    soloRoute.interleavedWithSurplusSales.gatheringTotals.salesGold,
  "副产物回收不能重复计入制造收入",
);
soloRoute.manufacturingCash = {
  seeds: { first: 1, last: 100 },
  requiredStartingGold: {
    min: Math.min(...cashSamples.map((sample) => sample.requiredStartingGold)),
    max: Math.max(...cashSamples.map((sample) => sample.requiredStartingGold)),
  },
  sample: cashSamples[0],
  noBreaks: soloManufacturingCash(() => 1),
  everyOperationBreaks: soloManufacturingCash(() => 0),
  exclusions: [
    "木针按首次学习赠送一把；包覆原料、采集工具及恢复成本未纳入",
    "每批售卖假定可往返诺姆城商店，未计旅行与背包容量；不是最低金币或实际利润证明",
    "百个种子只描述此模型样本，不是报废概率的最坏情况保证",
  ],
};
assert.deepEqual(
  cashSamples[0],
  soloManufacturingCash(seededRandom(1)),
  "相同种子现金流必须复现",
);
assert.equal(
  soloRoute.manufacturingCash.noBreaks.purchases,
  soloRoute.materialPurchaseGold +
    soloTools
      .filter((tool) => tool.item.endsWith("_NEEDLE") && tool.tier > 0)
      .reduce((sum, tool) => sum + tool.gold, 0),
  "无报废情景只应购买每档首把针和最终辅料",
);
assert.equal(
  soloRoute.manufacturingCash.everyOperationBreaks.broken,
  soloRoute.trainingCrafts + 4,
  "每次报废情景不能漏算或额外补造最后一件之后的工具",
);
console.log(
  JSON.stringify(
    {
      scope:
        "四角色制造练级与成品的顺序库存账；所有地图已开放的恢复道路检查，不是从零自然路线通过",
      training: training.map(({ level, skill, recipe, count }) => ({
        level,
        skill,
        recipe,
        count,
      })),
      trainingLedger,
      qualifications,
      combined,
      finalManufacturing: finalLedger,
      partyLedger,
      batchedPartyLedger,
      tradeFunding: {
        single: tradeFunding(partyLedger.transactions),
        batched: tradeFunding(batchedPartyLedger.transactions),
        zeroBalanceCheck: tradeFunding(
          batchedPartyLedger.transactions,
          Object.fromEntries(Object.values(owners).map((owner) => [owner, 0])),
        ),
      },
      startupTools: {
        purchases: bootstrap,
        totalGold: bootstrap.reduce((sum, t) => sum + t.gold, 0),
      },
      recovery,
      soloRoute,
      exclusions: [
        "账目中的acquired为需要从正式采集、制造或交易取得的缺额，不表示免费发放",
        "工具采购列每种木工具一把的启动方案，不含升级工具、练级损耗和角色重复工具；专业教学赠礼未抵扣",
        "练级产物保留全部，未计背包、仓库、交易和物品绑定限制，不作可执行路线承诺",
        "地图检查假定全部既定事实已取得；恢复前必须停止采集、解除伙伴派遣，不自动推进心跳",
        "报告保留去返完整节点路径，不将道路条数冒充真人旅行时间",
        "当前没有把成熟供料成本套在低级角色身上，两类报告必须分开解释",
      ],
    },
    null,
    2,
  ),
);
