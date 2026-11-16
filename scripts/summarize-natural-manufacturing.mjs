import { readFileSync } from 'node:fs';

// 只读取真实自然旅程日志，不估算未发生的操作，也不把局部夹具结果算作完整路线通过。
// 用法：node scripts/summarize-natural-manufacturing.mjs .git/natural-e009-complete.log
const path = process.argv[2];
if (!path) throw new Error('请提供自然制造旅程日志路径');
const bytes = readFileSync(path);
// Windows PowerShell重定向默认写UTF-16LE；同时兼容直接保存的UTF-8日志。
const log = bytes.toString(bytes[0] === 0xff && bytes[1] === 0xfe ? 'utf16le' : 'utf8');
const crafts = {};
const gathering = {};
const transfers = {};
const stageBalances = [];
// 仅汇总有分段计时的新命令；旧日志没有该字段时保留零样本，不能据此推算加速倍数。
const craftingPerformance = { samples: 0, prepareMicros: 0, settleMicros: 0, replayMicros: 0, totalMicros: 0 };
let toolCrafts = 0;
let lostTools = 0;
let directGoldSpent = 0;

// 总耗时还包含结算后成本查询；准备段包含助手内部移动，统计不覆盖Spring启动及外部采集等操作。
for (const match of log.matchAll(/NATURAL_PERF_CRAFT recipe=\S+ prepareMicros=(\d+) settleMicros=(\d+) replayMicros=(\d+) totalMicros=(\d+)/g)) {
  craftingPerformance.samples++;
  for (const [index, key] of ['prepareMicros', 'settleMicros', 'replayMicros', 'totalMicros'].entries()) {
    craftingPerformance[key] += Number(match[index + 1]);
  }
}

// 每条记录对应一次真实命令；制造命令内部的幂等重放不重复记账。
for (const match of log.matchAll(/NATURAL_COST_CRAFT owner=\d+ recipe=(\S+) goldSpent=(\d+) tool=\S+ toolsLost=(\d+)(?: completed=(\d+))?/g)) {
  const [, recipe, gold, lost, completed] = match;
  // 新批次按真实完成件数累计；旧版单件日志省略completed，仍按一件兼容。
  const count = Number(completed ?? 1);
  crafts[recipe] = (crafts[recipe] ?? 0) + count;
  if (recipe.startsWith('TOOL_')) toolCrafts += count;
  lostTools += Number(lost);
  directGoldSpent += Number(gold);
}
// 工具耗尽后的再次派遣有独立记录；分别累计派遣数与已完成回合，避免把心跳数当采集数。
for (const match of log.matchAll(/NATURAL_COST_GATHER owner=\d+ node=(\S+) rounds=(\d+) goldSpent=(\d+) exhausted=(true|false)(?: reason=(\S+))?/g)) {
  const [, node, rounds, gold, exhausted, reason] = match;
  const entry = gathering[node] ??= { trips: 0, rounds: 0, exhaustedTrips: 0, resourceRecoveryTrips: 0 };
  entry.trips++;
  entry.rounds += Number(rounds);
  entry.exhaustedTrips += Number(exhausted === 'true');
  // 资源不足与工具报废分开统计，前者需要正式停止、回城恢复后重新派遣。
  entry.resourceRecoveryTrips += Number(reason === 'RESOURCES_LOW');
  directGoldSpent += Number(gold);
}
// 测试市场固定单价10金币，属于角色间支付，不是全队净消耗；手续费、回收收入另需核算。
for (const match of log.matchAll(/NATURAL_TOOL_TRADE product=(\S+) unitPrice=10 /g)) {
  transfers[match[1]] = (transfers[match[1]] ?? 0) + 1;
}
// 部分起步制造使用较早的直接命令，只有阶段汇总而没有逐件记录；保留原始阶段证据供对照。
// 阶段之间可能嵌套，余额还混合了商店支出和出售收入，因此只展示各阶段差额，不重复求和。
for (const match of log.matchAll(/(NATURAL_[A-Z_]+) ([^\r\n]*\bgoldBefore=(\d+)[^\r\n]*\bgoldAfter=(\d+)[^\r\n]*)/g)) {
  stageBalances.push({ stage: match[1], details: match[2], netGoldChange: Number(match[4]) - Number(match[3]) });
}
console.log(JSON.stringify({
  source: path,
  e009Delivered: log.includes('NATURAL_E009_COMPLETED '),
  // 交付标记只说明执行到交付断言；必须再检查Maven退出码和Surefire报告确认整项通过。
  verification: '交付标记不代替最终进程退出码及测试报告；运行中日志仅代表已发生的部分操作',
  crafts, toolCrafts, lostTools, gathering, transfers, stageBalances, craftingPerformance,
  directGoldSpent,
  interCharacterPayments: Object.values(transfers).reduce((sum, count) => sum + count * 10, 0),
  excludes: '未记录命令、商店购入、恢复、战斗、任务奖赏、回收收入、市场手续费及真人操作耗时；不能作为全队净成本',
}, null, 2));
