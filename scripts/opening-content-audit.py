#!/usr/bin/env python3
"""逐形态比较采用表与运行来源；不导入候选或把七维登记视为可获得。"""
import argparse
import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / 'docs'
CONTENT = ROOT / 'avalon-content/src/main/resources/content'


def read_csv(name):
    with (DOCS / name).open() as source:
        return list(csv.DictReader(source))


def read_content(name):
    if name == 'quests':
        return {'steps': [step for file in sorted((CONTENT / 'quests').rglob('*.json'))
                          if file.name != 'tags.json'
                          for quest in json.loads(file.read_text(encoding='utf-8'))['quests']
                          for step in quest['steps']]}
    matches = [file for file in CONTENT.rglob(name + '.json') if 'schemas' not in file.parts]
    if len(matches) != 1:
        raise ValueError(f'Expected exactly one content file: {name}, found {matches}')
    return json.loads(matches[0].read_text(encoding='utf-8'))


def audit():
    manifest = json.loads((DOCS / 'current-creature-design.json').read_text())
    growth = read_csv(manifest['files']['growth'])
    desired_slots = {(r['species'], r['slot']): r['ability_name'] for r in read_csv(manifest['files']['abilities'])}
    desired_learning = {r['id']: set(r['skill_pool'].split(';')) for r in read_csv(manifest['files']['learning'])}
    pets = {r['id']: r for r in read_content('creatures')['pets']}
    routes = {r['target']: r for r in read_content('evolution')['routes']}
    slots = read_content('ability-slots')['species']
    pools = read_content('learning')['pools']
    wilds = read_content('wilds')['profiles']
    battle = read_content('battle')
    quests = {r['id'] for r in read_content('quests')['steps']}
    places = {r['id'] for r in read_content('opening')['places']}
    drops = read_content('evolution-drops')['species']
    sources = {}
    for encounter in battle['encounters']:
        if encounter.get('boss') or encounter['requires'] not in quests or encounter['location'] not in places:
            continue
        profile = wilds.get(encounter.get('wild'), {})
        for species, value in profile.get('species', encounter.get('species', {})).items():
            level = value['minLevel'] if isinstance(value, dict) else battle['growth'][species]['minLevel']
            sources.setdefault(species, []).append(f"{encounter['location']}/{encounter['id']} Lv.{level} [{encounter['requires']}]")
    result = []
    for row in growth:
        if int(row['earliest_numeric_level']) > 15:
            continue
        code, family = row['id'], row['evolution_family']
        route = routes.get(code)
        source = '; '.join(sources.get(code, [])) if row['stage'] == '1' else (
            f"{route['source']} → {code} Lv.{route['level']}" if route else '')
        absent = []
        if code not in pets and code not in routes:
            absent.append('未注册运行形态')
        if not source:
            absent.append('缺少运行获取来源')
        mismatches = [slot + ':' + slots.get(code, {}).get(slot, '未登记') + '→' + desired_slots[code, slot]
                      for slot in ('A', 'B', 'H') if slots.get(code, {}).get(slot) != desired_slots[code, slot]]
        if mismatches:
            absent.append('特性未对齐')
        missing_skills = sorted(desired_learning[code] - set(pools.get(code, [])))
        if missing_skills:
            absent.append('学习池缺少:' + '/'.join(missing_skills))
        if family not in drops:
            absent.append('缺家族野战材料')
        result.append({'id': code, 'name': row['name'], 'level': row['earliest_numeric_level'],
                       'stage': row['stage'], 'family': family, 'source': source,
                       'ability_differences': '; '.join(mismatches), 'gaps': '; '.join(absent)})
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--strict', action='store_true', help='存在设计差异时返回非零状态，供完整交付验收使用')
    args = parser.parse_args()
    rows = audit()
    output = DOCS / 'opening-content-status.csv'
    with output.open('w') as target:
        writer = csv.DictWriter(target, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    gaps = sum(bool(row['gaps']) for row in rows)
    print(f'{len(rows)}形态；有待对齐项{gaps}；逐项证据：{output.relative_to(ROOT)}')
    if args.strict and gaps:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
