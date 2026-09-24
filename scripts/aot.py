#!/usr/bin/env python3
"""可选的 JVM AOT 部署与隔离基准入口；仅使用 Python 标准库。

训练必须连接准备好的数据库，可能执行单例初始化；不要将训练指向生产库。
缓存与解包后的 JAR、JDK、JVM 参数绑定，变更后必须重新训练。
"""

import argparse
import hashlib
import json
import os
import platform
from pathlib import Path
import shutil
import signal
import socket
import statistics
import subprocess
import time
import urllib.error
from runtime_checks import READINESS_PATH, http, smoke as check_smoke


def default_jvm_args():
    """容器、本机训练和运行共用内存、中文与时区设置。"""
    return (Path(__file__).resolve().parent.parent / "avalon-app/src/main/docker/jvm.options").read_text().split()
import uuid


def java():
    executable = shutil.which("java")
    if not executable:
        raise RuntimeError("未找到 java，请通过 mise exec -- python3 scripts/aot.py 执行")
    return str(Path(executable).resolve())


def digest(path):
    with Path(path).open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def fingerprint(directory, spring_aot, jvm_args, env=None):
    """不落盘数据库凭据；只记录缓存兼容性所需的制品和 JVM 身份。"""
    executable = java()
    environment = os.environ if env is None else env
    injected = {key: environment.get(key, "") for key in
                ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")}
    return {
        "java": executable,
        "java_sha256": digest(executable),
        "java_release": digest(Path(executable).parent.parent / "release"),
        "libjvm_sha256": digest(Path(executable).parent.parent / "lib/server/libjvm.so"),
        "spring_aot": spring_aot,
        "jvm_args": jvm_args,
        "injected_options_sha256": hashlib.sha256(json.dumps(injected).encode()).hexdigest(),
        "jars": {str(p.relative_to(directory)): digest(p)
                 for p in sorted(directory.rglob("*.jar"))},
    }


def extract(jar, directory):
    if directory.exists() and any(directory.iterdir()):
        raise RuntimeError(f"解包目录必须为空，避免混入旧制品：{directory}")
    subprocess.run([java(), "-Djarmode=tools", "-jar", str(jar.resolve()), "extract",
                    "--destination", str(directory.resolve()),
                    "--application-filename", "application.jar"], check=True)


def cache_paths(directory, spring_aot):
    name = "spring-aot" if spring_aot else "jvm"
    return directory / f"{name}.aot", directory / f"{name}.cache.json"


def command(directory, spring_aot, jvm_args, cached=False, env=None):
    result = [java(), *jvm_args, f"-Dspring.aot.enabled={str(spring_aot).lower()}"]
    if cached:
        cache, metadata = cache_paths(directory, spring_aot)
        expected = fingerprint(directory, spring_aot, jvm_args, env)
        if not cache.is_file() or not metadata.is_file():
            raise RuntimeError("缺少缓存或校验清单，请先执行 train")
        recorded = json.loads(metadata.read_text())
        if recorded != {"environment": expected, "cache_sha256": digest(cache)}:
            raise RuntimeError("JDK、JAR、缓存或 JVM 参数已变化，请重新训练")
        # 强制使用缓存，失败直接退出，避免悄悄回退后误认为优化生效。
        result += ["-XX:AOTMode=on", f"-XX:AOTCache={cache.name}"]
    return result + ["-jar", "application.jar"]


def train(directory, spring_aot, jvm_args, env=None):
    cache, metadata = cache_paths(directory, spring_aot)
    metadata.unlink(missing_ok=True)
    cache.unlink(missing_ok=True)
    args = command(directory, spring_aot, jvm_args)
    args[1:1] = [f"-XX:AOTCacheOutput={cache.name}", "-Dspring.context.exit=onRefresh"]
    with (directory / f"{cache.stem}-training.log").open("w") as log:
        subprocess.run(args, cwd=directory, env=env, stdout=log,
                       stderr=subprocess.STDOUT, check=True, timeout=180)
    if not cache.is_file() or cache.stat().st_size == 0:
        raise RuntimeError("训练未生成缓存，请检查 training.log")
    metadata.write_text(json.dumps({
        "environment": fingerprint(directory, spring_aot, jvm_args, env),
        "cache_sha256": digest(cache),
    }, indent=2) + "\n")


def stop(process):
    """只清理本脚本创建的进程，超时才强制终止。"""
    if process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)



def memory(pid):
    """Linux smaps_rollup 的进程 RSS/PSS，单位 MiB；不把它当作 Java 堆大小。"""
    values = {}
    for line in Path(f"/proc/{pid}/smaps_rollup").read_text().splitlines():
        parts = line.split()
        if parts[0] in ("Rss:", "Pss:"):
            values[parts[0][:-1].lower() + "_mib"] = int(parts[1]) / 1024
    return values


def measure(args, directory, env, log_path, settle, smoke=True):
    with socket.socket() as server:
        server.bind(("127.0.0.1", 0))
        port = server.getsockname()[1]
    runtime = dict(env, SERVER_PORT=str(port))
    with log_path.open("w") as log:
        start = time.monotonic()
        process = subprocess.Popen(args, cwd=directory, env=runtime, stdout=log,
                                   stderr=subprocess.STDOUT)
        try:
            while True:
                if process.poll() is not None:
                    raise RuntimeError(f"应用提前退出，请检查 {log_path}")
                if time.monotonic() - start > 90:
                    raise RuntimeError(f"应用就绪超时，请检查 {log_path}")
                try:
                    health = http(port, READINESS_PATH)
                    if health["status"] == "UP":
                        break
                except (OSError, urllib.error.URLError):
                    pass
                time.sleep(0.1)
            ready = time.monotonic() - start
            at_ready = memory(process.pid)
            if smoke:
                check_smoke(port)
            time.sleep(settle)
            if process.poll() is not None:
                raise RuntimeError(f"应用在采样前退出，请检查 {log_path}")
            return {"ready_seconds": ready, "at_ready": at_ready,
                    "after_smoke": memory(process.pid)}
        finally:
            stop(process)


def benchmark(jar, output, repeats, settle):
    """随机端口、独立临时 PostgreSQL，不读取现有开发库配置。"""
    if output.exists() and any(output.iterdir()):
        raise RuntimeError("基准输出目录必须为空")
    output.mkdir(parents=True, exist_ok=True)
    # 固定本轮输入，防止另一个构建覆盖 target 中的 JAR，导致各模式不是同一制品。
    shutil.copy2(jar, output / "source.jar")
    jar = output / "source.jar"
    directory = output / "application"
    extract(jar, directory)
    name = "avalon-aot-" + uuid.uuid4().hex[:12]
    password = uuid.uuid4().hex
    # 清除可能改变 Spring 图和 JVM 行为的外部配置，确保各模式使用同一组参数。
    env = {key: value for key, value in os.environ.items()
           if not key.startswith(("AVALON_", "SPRING_", "COSID_", "SERVER_", "MANAGEMENT_"))
           and key not in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")}
    flags = default_jvm_args()
    flags += ["-Xlog:aot=info"]
    result = {"java": subprocess.check_output([java(), "-version"], stderr=subprocess.STDOUT, text=True),
              "host": {"platform": platform.platform(), "logical_cpus": os.cpu_count()},
              "jvm_args": flags, "repeats": repeats, "settle_seconds": settle,
              "jar_sha256": digest(jar), "runs": []}
    started = False
    try:
        subprocess.run(["docker", "run", "--rm", "-d", "--name", name,
                        "-p", "127.0.0.1::5432", "-e", "POSTGRES_DB=avalon_aot",
                        "-e", "POSTGRES_USER=avalon_aot", "-e", "POSTGRES_PASSWORD",
                        "postgres:17-alpine"], env=dict(env, POSTGRES_PASSWORD=password),
                       check=True, stdout=subprocess.DEVNULL)
        started = True
        db_port = subprocess.check_output(["docker", "port", name, "5432/tcp"], text=True).strip().rsplit(":", 1)[1]
        for attempt in range(60):
            if subprocess.run(["docker", "exec", name, "pg_isready", "-h", "127.0.0.1", "-U", "avalon_aot"],
                              stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode == 0:
                break
            time.sleep(0.5)
        else:
            raise RuntimeError("隔离数据库未就绪")
        env.update(SPRING_PROFILES_ACTIVE="prod", SPRING_DATASOURCE_URL=f"jdbc:postgresql://127.0.0.1:{db_port}/avalon_aot",
                   SPRING_DATASOURCE_USERNAME="avalon_aot", SPRING_DATASOURCE_PASSWORD=password, COSID_MACHINE_DISTRIBUTOR_MANUAL_MACHINEID="3",
                   SERVER_ADDRESS="127.0.0.1", MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED="true",
                   SPRING_LIQUIBASE_ENABLED="false")
        # 迁移单独执行，不把首次建表成本计入任何一种模式。
        measure(command(directory, False, flags), directory, dict(env, SPRING_LIQUIBASE_ENABLED="true"),
                output / "database-init.log", 0, smoke=False)
        for spring_aot in (False, True):
            print(f"训练 Spring AOT={spring_aot}", flush=True)
            train(directory, spring_aot, flags, env)
        modes = [("jar", False, False), ("extracted", False, False),
                 ("leyden", False, True), ("spring-aot", True, False), ("combined", True, True)]
        # 轮换执行顺序以减少时间趋势偏差；OS 页缓存保持暖态，不冒充冷磁盘启动测试。
        for repeat in range(repeats):
            for mode, spring_aot, cached in modes[repeat % len(modes):] + modes[:repeat % len(modes)]:
                args = command(directory, spring_aot, flags, cached, env)
                if mode == "jar":
                    args[-1] = str(jar.resolve())
                sample = measure(args, directory, env, output / f"{mode}-{repeat + 1}.log", settle)
                result["runs"].append(dict(mode=mode, repeat=repeat + 1, **sample))
                (output / "results.json").write_text(json.dumps(result, indent=2) + "\n")
                print(f"{mode} #{repeat + 1}: {sample}", flush=True)
        result["medians"] = {}
        for mode, _, _ in modes:
            samples = [r for r in result["runs"] if r["mode"] == mode]
            result["medians"][mode] = {
                "ready_seconds": statistics.median(r["ready_seconds"] for r in samples),
                "rss_mib": statistics.median(r["after_smoke"]["rss_mib"] for r in samples),
                "pss_mib": statistics.median(r["after_smoke"]["pss_mib"] for r in samples),
            }
        (output / "results.json").write_text(json.dumps(result, indent=2) + "\n")
    finally:
        if started:
            subprocess.run(["docker", "rm", "-f", name], check=False, stdout=subprocess.DEVNULL)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="action", required=True)
    prepare = sub.add_parser("extract")
    prepare.add_argument("jar", type=Path)
    prepare.add_argument("directory", type=Path)
    for action in ("train", "run"):
        p = sub.add_parser(action)
        p.add_argument("directory", type=Path)
        p.add_argument("--spring-aot", action="store_true")
        p.add_argument("--jvm-arg", action="append", default=[])
        if action == "run":
            p.add_argument("--cache", action="store_true")
    bench = sub.add_parser("benchmark")
    bench.add_argument("jar", type=Path)
    bench.add_argument("output", type=Path)
    bench.add_argument("--repeats", type=int, default=3)
    bench.add_argument("--settle", type=float, default=10)
    args = parser.parse_args()
    if args.action == "extract":
        extract(args.jar, args.directory.resolve())
    elif args.action == "benchmark":
        if args.repeats < 1 or args.settle < 0:
            parser.error("repeats 必须大于零，settle 不得为负数")
        benchmark(args.jar.resolve(), args.output.resolve(), args.repeats, args.settle)
    else:
        directory = args.directory.resolve()
        # 显式参数追加在默认值后，允许覆盖；训练与运行仍校验完整参数清单。
        args.jvm_arg = default_jvm_args() + args.jvm_arg
        if args.action == "train":
            train(directory, args.spring_aot, args.jvm_arg)
        else:
            args = command(directory, args.spring_aot, args.jvm_arg, args.cache)
            os.chdir(directory)
            os.execv(args[0], args)


def interrupted(signum, frame):
    """SIGTERM 同样走 finally，清理本次子进程和临时数据库。"""
    raise KeyboardInterrupt()


if __name__ == "__main__":
    signal.signal(signal.SIGTERM, interrupted)
    main()
