import { readContent } from "./content-files.mjs";
import { productionCost, tradeCost } from "./production-cost-model.mjs";
const production = readContent("professions/life-production.json");
const { items } = readContent("items/items.json");
const { nodes } = readContent("professions/gathering.json");
const toolKinds = {
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
};
const roles = {
  ARTISAN: "artisan",
  MINING: "artisan",
  LOGGING: "artisan",
  LEATHERWORK: "leatherworker",
  HUNTING: "leatherworker",
  ALCHEMY: "alchemist",
  HERBALISM: "alchemist",
  COOKING: "cook",
  FARMING: "cook",
  FISHING: "cook",
};
const farming = readContent("professions/farming.json");
const fishing = readContent("professions/fishing.json");
const cost = productionCost({
  recipes: production.recipes,
  items,
  nodes,
  toolKinds,
  crops: farming.crops,
  fish: fishing.fish,
  bait: fishing.bait,
  offers: production.offers,
  demands: {
    E_FORGED_BODY: 1,
    LIFE_HEALTH_POTION_4: 20,
    LIFE_MANA_POTION_4: 10,
  },
});
const trades = Object.entries(cost.transfers)
  .filter(([key]) => {
    const [from, to] = key.split(":")[0].split("->");
    return from !== "EXTERNAL" && roles[from] !== roles[to];
  })
  .map(([route, quantity]) => ({ route, ...tradeCost(Math.ceil(quantity)) }));
const output = {
  scope:
    "已有10级专业、图纸、地图及启动工具的四角色长期供料边际预算；含采集及制造工具递归补造",
  roles,
  cost,
  tradesAtMinimumUnitPrice: trades,
  tradeTax: trades.reduce((sum, t) => sum + t.tax, 0),
  assumptions: [
    "单价2金币只用于最低合法单价情景，按每99件成交计算税；不预测市场行情",
    "小数操作和工具用于长期期望，单件启动库存和批次取整不在此模型中",
    "稀有采集普通副产物尚未抵扣，分别采集的预算偏保守",
    "种植假设及时浇水收获；钓鱼假设每次成功，鱼饵和成功钓获的工具损耗已计入，不预测小游戏成功率",
    "purchaseGold为种子与鱼饵等正式商店采购，external仍列出无法生产或购买的输入",
    "生命魔力消耗为累计资源，未设定伙伴容量，不能据此虚构恢复次数或补给价格",
    "免费恢复存在不表示零旅行成本，在线时间仅含采集，不包含钓鱼、种植、往返、恢复、制造和交易",
  ],
};
console.log(JSON.stringify(output, null, 2));
