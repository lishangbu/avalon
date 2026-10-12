#!/usr/bin/env python3
"""在临时 Docker 网络中复验总计 2 核 / 4 GiB 的读写负载；不使用开发数据库。"""
import argparse
import json
from pathlib import Path
import subprocess
import sys
import time
import urllib.request
import uuid


def main():
    root = Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--image', default='avalon:corretto27')
    parser.add_argument('--port', type=int, default=18793)
    parser.add_argument('--seconds', type=int, default=30)
    parser.add_argument('--output', type=Path, default=root / 'outputs/level15')
    args = parser.parse_args()
    if not 1 <= args.port <= 65535 or args.seconds < 1:
        parser.error('端口必须在1～65535范围内，持续秒数必须为正数')
    jar = root / 'avalon-app/target/avalon-app-0.1.0-SNAPSHOT.jar'
    if not jar.is_file():
        parser.error('请先打包 avalon-app')
    args.output.mkdir(parents=True, exist_ok=True)
    name = 'avalon-audit-' + uuid.uuid4().hex[:8]
    db, app = name + '-db', name + '-app'

    def run(*command):
        return subprocess.check_output(command, text=True, stderr=subprocess.STDOUT).strip()

    try:
        run('docker', 'network', 'create', name)
        run('docker', 'run', '-d', '--name', db, '--network', name, '--cpus', '1', '--memory', '1g',
            '-e', 'POSTGRES_PASSWORD=isolated-audit-only', '-e', 'POSTGRES_DB=avalon', 'postgres:18.6-trixie')
        for _ in range(40):
            ready = subprocess.run(['docker', 'exec', db, 'pg_isready', '-U', 'postgres'],
                                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            if ready.returncode == 0:
                break
            time.sleep(1)
        else:
            raise RuntimeError('隔离数据库启动超时')
        run('docker', 'run', '-d', '--name', app, '--network', name, '--cpus', '1', '--memory', '3g',
            '-p', f'127.0.0.1:{args.port}:8080', '-v', f'{jar}:/audit.jar:ro', '--entrypoint', 'java',
            args.image, '-Xms512m', '-Xmx1536m', '-jar', '/audit.jar',
            f'--spring.datasource.url=jdbc:postgresql://{db}:5432/avalon',
            '--spring.datasource.username=postgres', '--spring.datasource.password=isolated-audit-only',
            '--spring.liquibase.enabled=true', '--cosid.machine.distributor.manual.machine-id=997',
            '--management.endpoint.health.probes.enabled=true')
        opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
        for _ in range(120):
            try:
                with opener.open(f'http://127.0.0.1:{args.port}/actuator/health/readiness', timeout=2) as response:
                    if response.status == 200:
                        break
            except (OSError, TimeoutError):
                pass
            time.sleep(1)
        else:
            raise RuntimeError('隔离应用启动超时')
        for script, users, filename, extra in [
            ('runtime-load-check.py', 24, 'isolated-load-expanded.json', []),
            ('runtime-battle-load-check.py', 12, 'isolated-battle-load.json', ['--database-container', db]),
        ]:
            subprocess.run([sys.executable, str(root / 'scripts' / script), '--port', str(args.port),
                            '--users', str(users), '--seconds', str(args.seconds),
                            '--output', str(args.output / filename), *extra], check=True)
        resources = {'app': {'cpus': 1, 'memory_gib': 3}, 'postgres': {'cpus': 1, 'memory_gib': 1},
                     'note': '共享宿主机上的容器上限；依次执行认证混合读取与战斗写入，不代表在线容量'}
        (args.output / 'isolated-load-resources.json').write_text(json.dumps(resources, ensure_ascii=False, indent=2))
    finally:
        for container in (app, db):
            logs = subprocess.run(['docker', 'logs', container], capture_output=True, text=True)
            (args.output / f'{container}.log').write_text(logs.stdout + logs.stderr)
            subprocess.run(['docker', 'rm', '-f', container], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run(['docker', 'network', 'rm', name], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


if __name__ == '__main__':
    main()
