#!/usr/bin/env python3
"""对隔离打包应用执行有界混合请求压测；只使用 Python 标准库，不把吞吐等同在线人数。"""
import argparse
from concurrent.futures import ThreadPoolExecutor
import json
import time
import uuid
from pathlib import Path
from runtime_checks import http


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port', type=int, required=True)
    parser.add_argument('--users', type=int, default=24)
    parser.add_argument('--seconds', type=int, default=30)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if not 1 <= args.users <= 100 or not 1 <= args.seconds <= 300:
        parser.error('users必须在1～100之间，seconds必须在1～300之间')
    tokens = []
    for _ in range(args.users):
        name = 'load_' + uuid.uuid4().hex[:12]
        token = http(args.port, '/api/auth/register', {'username': name, 'password': 'Isolated-load-1234', 'device': '隔离压测'})['accessToken']
        http(args.port, '/api/world/character', {'name': '压测' + uuid.uuid4().hex[:8], 'profession': 'WARRIOR'}, token)
        tokens.append(token)
    deadline = time.monotonic() + args.seconds
    paths = ['/api/world', '/api/quests', '/api/creatures', '/api/inventory']

    def sample(token):
        timings, errors = [], []
        index = 0
        while time.monotonic() < deadline:
            start = time.monotonic()
            try:
                http(args.port, paths[index % len(paths)], token=token)
                timings.append((time.monotonic() - start) * 1000)
            except Exception as error:
                errors.append(type(error).__name__)
            index += 1
        return timings, errors

    started = time.monotonic()
    with ThreadPoolExecutor(max_workers=args.users) as executor:
        samples = list(executor.map(sample, tokens))
    elapsed = time.monotonic() - started
    timings = sorted(t for values, _ in samples for t in values)
    errors = [e for _, values in samples for e in values]
    result = {'concurrent_clients': args.users, 'duration_seconds': elapsed,
              'successful_requests': len(timings), 'errors': len(errors), 'error_types': sorted(set(errors)),
              'requests_per_second': len(timings) / elapsed,
              'latency_ms': {str(q): timings[min(len(timings)-1, int(len(timings)*q))] for q in (.5,.95,.99)} if timings else {},
              'scope': '已认证地图、任务、幻兽、背包读取；注册创角作为预热；不代表战斗写负载或在线容量'}
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps(result, ensure_ascii=False))
    if errors:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
