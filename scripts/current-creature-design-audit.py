"""跨当前交付表核对引用；不调用旧生成器，不把结构检查当作战斗平衡证明。"""

import csv
import json
from collections import defaultdict
from decimal import Decimal
from pathlib import Path

DOCS = Path(__file__).resolve().parents[1] / 'docs'
manifest = json.loads((DOCS / 'current-creature-design.json').read_text())
assert manifest['runtimeReady'] is False
expected = manifest['expected']


def read(name):
    with (DOCS / name).open() as source:
        return list(csv.DictReader(source))


tables = {key: read(name) for key, name in manifest['files'].items()}


def unique(rows, key):
    result = {r[key]: r for r in rows}
    assert len(result) == len(rows), key
    return result


growth = unique(tables['growth'], 'id')
assert len(growth) == expected['species']
families = {r['evolution_family'] for r in growth.values()}
assert len(families) == expected['families']
elements = {'水', '火', '风', '地', '雷', '木', '光', '暗'}
for row in growth.values():
    assert row['element'] in elements and row['evolution_family'] in growth
    assert 1 <= int(row['earliest_numeric_level']) <= 50
    stat_keys = ['health', 'mana', 'physicalAttack', 'magicAttack', 'physicalDefense', 'magicDefense', 'speed']
    assert all(row[k].isdigit() and int(row[k]) > 0 for k in stat_keys), (row['id'], '公开七维必须为正整数')
    assert row['total'].isdigit() and int(row['total']) == sum(int(row[k]) for k in stat_keys)
    if row['evolution']:
        target = growth[row['evolution']]
        assert target['evolution_family'] == row['evolution_family']
        assert int(target['stage']) == int(row['stage']) + 1

effects = unique(tables['abilityInteractions'], 'ability_key')
assert len(effects) == expected['abilityEffects']
assert len({r['ability_name'] for r in effects.values()}) == len(effects)
for key, row in effects.items():
    assert (row['suppressed_by_gas'] == 'True') == (key != 'CLASSIC_NEUTRALIZING_GAS')
    assert (row['copy_allowed'] == 'False') == (key in {'CLASSIC_TRACE', 'CLASSIC_NEUTRALIZING_GAS'})
slots = {(r['species'], r['slot']): r for r in tables['abilities']}
assert len(slots) == len(tables['abilities']) == expected['slots']
for code, row in growth.items():
    for slot, probability in [('A', 45), ('B', 45), ('H', 10)]:
        entry = slots[code, slot]
        assert entry['family'] == row['evolution_family']
        assert int(entry['generation_percent']) == probability
        assert entry['ability_name'] == effects[entry['ability_key']]['ability_name']
        if row['evolution']:
            assert entry['ability_key'] == slots[row['evolution'], slot]['ability_key']

skills = {r['skill'] for r in tables['baseSkillCosts']} | {'普攻'}
for key in ['punchSkills', 'multihitSkills', 'poisonSkills']:
    for row in tables[key]:
        assert row['name'] not in skills
        skills.add(row['name'])
assert len(skills) == expected['skillsIncludingBasicAttack']
assert set(unique(tables['skillContact'], 'skill')) == skills
assert set(unique(tables['skillSecondaryEffects'], 'skill')) == skills
learning = unique(tables['learning'], 'id')
assert set(learning) == set(growth)
for code, row in learning.items():
    pool = set(row['skill_pool'].split(';'))
    assert pool <= skills and row['automatic_learning'] == 'False'
    if growth[code]['evolution']:
        assert pool <= set(learning[growth[code]['evolution']]['skill_pool'].split(';'))
wild = unique(tables['wildInitialSkills'], 'id')
assert len(wild) == expected['wildSpecies'] and set(wild) == families
assert set(unique(tables['wildBookLoot'], 'species_id')) == set(wild)
for row in tables['wildBookLoot']:
    assert sum(int(row[k]) for k in ['book_weight', 'fragment_weight', 'no_book_weight']) == 1000
for code, row in wild.items():
    initial = row['initial_skills'].split(';')
    assert len(initial) == len(set(initial)) <= 7
    assert set(initial) <= set(learning[code]['skill_pool'].split(';'))

encounters = defaultdict(list)
for file in manifest['encounters']:
    for row in read(file):
        assert row['species_id'] in wild
        encounters[row['pool_id']].append(row)
assert len(encounters) == expected['encounterPools']
assert {r['species_id'] for rows in encounters.values() for r in rows} == set(wild)
assert all(sum(int(r['weight']) for r in rows) == 100 for rows in encounters.values())
materials = unique(tables['evolutionMaterials'], 'id')
assert len(materials) == expected['evolutionMaterials']
recipes = unique(tables['evolutionRecipes'], 'source')
assert len(recipes) == expected['evolutionConversions']
assert set(recipes) == {code for code, r in growth.items() if r['evolution']}
stations = unique(tables['researchStations'], 'station')
assert len(stations) == expected['researchStations']
for code, row in recipes.items():
    assert row['target'] == growth[code]['evolution']
    assert row['family'] == growth[code]['evolution_family']
    assert int(row['pet_level']) == int(growth[row['target']]['earliest_numeric_level'])
    assert row['common_material'] in materials and row['family_material'] in materials
    assert row['fallback_requires_quest'] == stations[row['fallback_station']]['requires_quest']
for family in families:
    chain = [r for r in recipes.values() if r['family'] == family]
    assert sum(int(r['common_quantity']) for r in chain) == 6
    assert sum(int(r['family_quantity']) for r in chain) == 6
drops = unique(tables['evolutionDrops'], 'species')
assert set(drops) == set(wild)
for row in drops.values():
    assert row['common_material'] in materials and row['family_material'] in materials
    assert materials[row['family_material']]['family'] == row['family']
    assert int(row['common_weight']) + int(row['common_none_weight']) == 100
    assert int(row['family_weight']) + int(row['family_none_weight']) == 100
offers = {(r['station'], r['material']): r for r in tables['evolutionOffers']}
assert len(offers) == len(tables['evolutionOffers'])
for row in offers.values():
    assert row['requires_quest'] == stations[row['station']]['requires_quest']
    assert row['token_price'] == materials[row['material']]['token_price']
for row in recipes.values():
    assert (row['fallback_station'], row['common_material']) in offers
    assert (row['fallback_station'], row['family_material']) in offers
for row in tables['weather']:
    assert not row['speed_ability'] or row['speed_ability'] in {r['ability_name'] for r in effects.values()}
assert len(unique(tables['terrain'], 'id')) == 2
for row in tables['terrain']:
    assert row['setter_ability'] in {r['ability_name'] for r in effects.values()}
    assert row['boosted_element'] in elements
    assert row['scope'] == 'WHOLE_BATTLE_ALL_ACTIVE' and row['ground_check'] == 'False'
for row in tables['researchCommissions']:
    assert row['record_pool'] in encounters
    assert set(row['eligible_pools'].split(';')) <= set(encounters)
    assert row['station'] in stations
print(f'当前选表跨表检查通过：150形态、{len(effects)}效果、450槽位、23技能、60野生幻兽、29池、90进化、68材料。')
print('未覆盖：完整技能执行、所有道具注册、全图可达、经济与胜率、运行配置兼容。')
