#!/usr/bin/env python3
"""隔离数据库夹具解锁后，使用正式 HTTP 执行战斗、回合、恢复及相邻移动。"""
import argparse
from concurrent.futures import ThreadPoolExecutor
import json
from pathlib import Path
import subprocess
import time
import uuid
from runtime_checks import http


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port', type=int, required=True)
    parser.add_argument('--database-container', required=True)
    parser.add_argument('--users', type=int, default=12)
    parser.add_argument('--seconds', type=int, default=30)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if not 1 <= args.users <= 50 or not 1 <= args.seconds <= 300:
        parser.error('users 1..50; seconds 1..300')
    if not args.database_container.startswith('avalon-audit-'):
        parser.error('仅接受本轮隔离容器 avalon-audit-*')
    players = []
    for index in range(args.users):
        username = 'load_' + uuid.uuid4().hex[:12]
        name = '战测' + uuid.uuid4().hex[:8]
        token = http(args.port, '/api/auth/register', {'username': username, 'password': 'Isolated-load-1234', 'device': '隔离压测'})['accessToken']
        character = http(args.port, '/api/world/character', {'name': name, 'profession': 'WARRIOR'}, token)
        character_id = int(character['id'])
        statement = f"""UPDATE player_character SET experience=264820, location='RIVERSIDE' WHERE id={character_id};
INSERT INTO character_fact(id,character_id,code,created_at) VALUES ({900000+index},{character_id},'QUEST:BQ004',now());
INSERT INTO quest_receipt(id,character_id,step,quest_code,completed,response,created_at) VALUES ({900000+index},{character_id},'BQ004','BQ004',true,'{{}}',now());"""
        subprocess.run(['docker', 'exec', '-i', args.database_container, 'psql', '-U', 'postgres', '-d', 'avalon', '-v', 'ON_ERROR_STOP=1'], input=statement, text=True, stdout=subprocess.DEVNULL, check=True)
        players.append(token)
    deadline = time.monotonic() + args.seconds

    def play(token):
        times, errors, fights = [], [], 0
        def request(path, body=None):
            started = time.monotonic()
            result = http(args.port, path, body, token)
            times.append((time.monotonic() - started) * 1000)
            return result
        def move(destination):
            world = request('/api/world')
            request('/api/world/move', {'destination': destination, 'expectedRevision': world['character']['revision']})
        try:
            while time.monotonic() < deadline:
                battle = request('/api/battle', {'encounter': 'VILLAGE', 'requestId': str(uuid.uuid4())})
                while battle['status'] == 'ACTIVE':
                    enemy = next(unit for unit in battle['units'] if unit['enemy'] and unit['hp'] > 0)
                    orders = [{'actor': unit['id'], 'action': 'ATTACK', 'target': enemy['id']} for unit in battle['units'] if not unit['enemy'] and unit['hp'] > 0 and (unit['hero'] or unit['position'] < 2)]
                    battle = request(f"/api/battle/{battle['id']}/turn", {'round': battle['round'], 'orders': orders, 'surrender': False, 'replacements': {}})
                if battle['status'] != 'WON':
                    raise RuntimeError('battle did not win')
                fights += 1
                for place in ['STONE_PATH', 'SQUARE', 'PET_CENTER']:
                    move(place)
                request('/api/battle/recover', {})
                for place in ['SQUARE', 'STONE_PATH', 'RIVERSIDE']:
                    move(place)
        except Exception as error:
            errors.append(type(error).__name__)
        return times, errors, fights
    started = time.monotonic()
    with ThreadPoolExecutor(max_workers=args.users) as executor:
        results = list(executor.map(play, players))
    elapsed = time.monotonic() - started
    timings = sorted(t for result in results for t in result[0])
    errors = [error for result in results for error in result[1]]
    result = dict(concurrent_clients=args.users, duration_seconds=elapsed, completed_battles=sum(r[2] for r in results), successful_requests=len(timings), errors=errors, requests_per_second=len(timings)/elapsed,
                  latency_ms={str(q): timings[min(len(timings)-1, int(len(timings)*q))] for q in [.5,.95,.99]} if timings else {},
                  scope='15级单人物夹具；1..3级普通野战、真实回合、相邻移动和恢复；不是自然成长或多人在线容量')
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps(result, ensure_ascii=False))
    return bool(errors)


if __name__ == '__main__':
    raise SystemExit(main())
