import fs from 'node:fs';
import path from 'node:path';
import { storyContent } from './content-files.mjs';
const {quests}=storyContent();
const regions={O:'晨露村',N:'诺姆',U:'温蒂妮',S:'希尔芙',F:'沙拉曼达',J:'联合终章',E:'自由冒险'};
const rows=['# 四城任务目录','','本目录从正式任务链 JSON 导出。任务、步骤、分支、对白和前置以 content/quests/ 下的文件为准；编号顺序不代表依赖顺序。','','| 区域 | 类型 | 任务 |','| --- | --- | --- |'];
for(const q of quests.toSorted((a,b)=>Object.keys(regions).indexOf(a.id[0])-Object.keys(regions).indexOf(b.id[0])||a.id.localeCompare(b.id)))rows.push(`| ${regions[q.id[0]]} | ${q.type==='MAIN'?'主线':'支线'} | ${q.id} ${q.title} |`);
fs.writeFileSync(path.resolve(import.meta.dirname,'../docs/quest-catalog.md'),rows.join('\n')+'\n');
console.log(`Exported ${quests.length} quests without modifying runtime content.`);
