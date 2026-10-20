import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const directory = path.join(root, 'avalon-content/src/main/resources/content');
import { storyContent } from './content-files.mjs';
const { steps, quests } = storyContent();
const groups = { combat: [], resources: [], creatures: [], auxiliary: [] };
const mixed = [];
for (const quest of quests) {
  const taskSteps = steps.filter(step => step.quest === quest.id);
  const codes = taskSteps.flatMap(step => step.objectives ?? []).map(o => o.code);
  const categories = new Set(codes.map(code => /^(VICTORY[:_]|KILL_|TRAINING:)/.test(code) ? 'combat'
    : /^(CAPTURE[:_]|PET:)/.test(code) ? 'creatures'
      : /^(ITEM:|CRAFT_|GATHER_)/.test(code) ? 'resources' : 'auxiliary'));
  if (taskSteps.some(step => step.conditions?.some(condition => condition.kind === 'EQUIPPED_ITEM')))
    categories.add('resources');
  // Each task counts once. A combat/material alternative is counted as combat,
  // with every mixed task also listed so this convention remains reviewable.
  const category = ['combat', 'resources', 'creatures', 'auxiliary'].find(value => categories.has(value)) ?? 'auxiliary';
  groups[category].push(quest.id);
  if (categories.size > 1) mixed.push({ id: quest.id, categories: [...categories] });
}
const ordered = groups.combat.length > groups.resources.length && groups.resources.length > groups.creatures.length
  && groups.creatures.length > groups.auxiliary.length;
const report = { scope: 'Configured objectives and actual equipment conditions; not natural-play or time proportions. Other empty objectives and scene interactions count as auxiliary.',
  total: quests.length,
  counts: Object.fromEntries(Object.entries(groups).map(([key, ids]) => [key, ids.length])),
  orderedAsRequested: ordered,
  groups, mixed,
  auxiliaryDetails: groups.auxiliary.map(id => ({ id, title: quests.find(q => q.id === id).title,
    codes: steps.filter(step => step.quest === id).flatMap(step => step.objectives ?? []).map(o => o.code) })) };
console.log(JSON.stringify(process.argv.includes('--details') ? report : {
  total: report.total, counts: report.counts, orderedAsRequested: ordered, mixedTasks: mixed.length,
}, null, 2));
const markdownIndex = process.argv.indexOf('--markdown');
if (markdownIndex >= 0) {
  if (!process.argv[markdownIndex + 1]) throw Error('Missing Markdown output path');
  const names = { combat: '战斗', resources: '人物装备、道具与资源', creatures: '捕捉与拥有幻兽', auxiliary: '辅助目标' };
  const lines = ['# 任务主要目标审计', '',
    '按正式配置逐任务统计，每项只计一次；这不是游玩时间比例或自然验收结果。运行 `node scripts/audit-story-objectives.mjs --markdown docs/quest-objective-audit.md --check` 可复算。', '',
    '| 主要目标 | 数量 | 比例 |', '| --- | ---: | ---: |'];
  for (const [category, ids] of Object.entries(groups))
    lines.push(`| ${names[category]} | ${ids.length} | ${(ids.length / quests.length * 100).toFixed(1)}% |`);
  lines.push('', `共${quests.length}项。数量顺序${ordered ? '符合' : '不符合'}战斗、物品资源、幻兽、辅助的约定。`, '',
    '判定使用实际目标代码及穿戴条件，不读取标题或标签。战斗和物资任选的混合任务按战斗计入；其余按物品资源、幻兽、辅助的优先级归类。永久事实、阅读、现场操作和仅领奖仍计辅助，不能因奖励装备或金币就算收集任务。', '',
    '高价值幻兽展示仅检查当前拥有；蓝鳍鱼、白羽鹤、影尾狐的可选捕捉来源不等于展示任务要求新捕捉。新增普通幻兽调查要求正式捕获事件，捕获个体保留，不增加主线门槛。', '',
    '| 区域 | 战斗 | 物品资源 | 幻兽 | 辅助 |', '| --- | ---: | ---: | ---: | ---: |');
  for (const prefix of ['O', 'N', 'U', 'S', 'F', 'J', 'E'])
    lines.push(`| ${prefix} | ${Object.values(groups).map(ids => ids.filter(id => id.startsWith(prefix)).length).join(' | ')} |`);
  fs.writeFileSync(path.resolve(root, process.argv[markdownIndex + 1]), lines.join('\n') + '\n');
}
if (process.argv.includes('--check') && !ordered) process.exitCode = 1;
