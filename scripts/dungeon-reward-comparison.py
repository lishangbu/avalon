#!/usr/bin/env python3
"""读取页面计时和正式奖励，报告单位时间倍率；不自动覆盖配置。"""
import argparse
import json
from pathlib import Path
import statistics

root = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--evidence', type=Path, default=root / 'outputs/level15')
args = parser.parse_args()
files = sorted(args.evidence.glob('dungeon-reward-baseline-*.json'))
if not files:
    parser.error('缺少 DungeonBrowserIT 生成的桌面／手机计时证据')
bundle = json.loads((root / 'avalon-content/src/main/resources/content/dungeons/forest-pen.json').read_text(encoding='utf-8'))
dungeon, rewards = bundle['dungeon'], bundle['rewards']
rows = []
for path in files:
    sample = json.loads(path.read_text())
    if sample.get('battles') != 20 or '低资源' not in sample.get('scope', ''):
        parser.error(f'{path.name}不是当前并行20场对照协议，请先重跑浏览器基线')
    level = sample['level']
    percent = max(50, min(150, 100 + 2 * (dungeon['level'] - level)))
    stage_xp = sum(stage['experience'] * percent // 100 * max(1, len(stage.get('wildSpecies', [])))
                   for stage in dungeon['stages'])
    repeat = stage_xp + rewards['completionExperience']
    first = repeat + rewards['firstCompletionExperience']
    comparable_wild = sample['wildExperience'] / sample['wildMs'] * sample['dungeonMs']
    rows.append({'device': path.stem.removeprefix('dungeon-reward-baseline-'), **sample,
                 'repeatExperience': repeat, 'firstExperience': first,
                 'repeatRateRatio': repeat / comparable_wild, 'firstRateRatio': first / comparable_wild,
                 'targetRepeatTotal': round(comparable_wild * 2),
                 'targetFirstTotal': round(comparable_wild * 5), 'stageExperience': stage_xp})
output = {'samples': rows, 'medianRepeatRatio': statistics.median(r['repeatRateRatio'] for r in rows),
          'medianFirstRatio': statistics.median(r['firstRateRatio'] for r in rows),
          'scope': '自动化操作协议的经验／时间对照；不证明真人2倍／5倍效率，金币和装备未折算经验'}
(args.evidence / 'dungeon-reward-comparison.json').write_text(json.dumps(output, ensure_ascii=False, indent=2))
print(json.dumps(output, ensure_ascii=False, indent=2))
