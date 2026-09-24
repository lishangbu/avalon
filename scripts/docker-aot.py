#!/usr/bin/env python3
"""用已构建的应用镜像训练 Leyden 缓存，再通过 Fabric8 Maven 插件生成并验收最终镜像。

要求本机 Docker、Python 3.11+、Maven 所需 JDK。训练与验收均在临时网络和数据库中运行。
不推送镜像，不连接开发或生产数据库；失败时清理本次创建的容器与网络。
"""

import argparse
import json
import io
import os
from pathlib import Path
import signal
import subprocess
import time
import tarfile
import uuid

from runtime_checks import READINESS_PATH, http, smoke

ROOT = Path(__file__).resolve().parent.parent


def docker(*args, **kwargs):
    return subprocess.check_output(["docker", *args], text=True, **kwargs).strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True, help="由 -Paot,docker 构建的应用镜像")
    parser.add_argument("--image", required=True, help="最终本地缓存镜像名称")
    parser.add_argument("--postgres-image", default="postgres:17-alpine")
    parser.add_argument("--output", type=Path, default=ROOT / "avalon-app/target/docker-aot")
    args = parser.parse_args()
    output = args.output.resolve()
    if output.exists() and any(output.iterdir()):
        parser.error("输出目录必须为空，避免复用旧缓存")
    output.mkdir(parents=True, exist_ok=True)
    prefix = "avalon-image-" + uuid.uuid4().hex[:12]
    network = prefix + "-net"
    database = prefix + "-db"
    containers = []
    network_created = False
    config_volume = prefix + "-config"
    config_created = False
    # 冻结镜像身份：训练完成前即使同名标签被覆盖，最终镜像也继承相同制品。
    source_id = docker("image", "inspect", "--format", "{{.Id}}", args.source)
    password = uuid.uuid4().hex
    environment = dict(os.environ, POSTGRES_PASSWORD=password)

    def configure(name):
        """仅向临时容器写具体值配置；不落盘凭据、不写入发布镜像。JSON 是合法 YAML。"""
        content = json.dumps({
            "spring": {"datasource": {"url": "jdbc:postgresql://database:5432/avalon_training",
                                      "username": "avalon_training", "password": password}},
            "cosid": {"machine": {"distributor": {"manual": {"machine-id": 7}}}},
        }).encode()
        stream = io.BytesIO()
        with tarfile.open(fileobj=stream, mode="w") as archive:
            directory = tarfile.TarInfo("config")
            directory.type = tarfile.DIRTYPE
            directory.mode = 0o700
            directory.uid = directory.gid = 0
            archive.addfile(directory)
            entry = tarfile.TarInfo("config/application-prod.yaml")
            entry.size = len(content)
            entry.mode = 0o600
            entry.uid = entry.gid = 0
            archive.addfile(entry, io.BytesIO(content))
        subprocess.run(["docker", "cp", "-a", "-", f"{name}:/application/"],
                       input=stream.getvalue(), check=True)

    def start(name, image, *command, ports=False):
        containers.append(name)
        options = ["create", "--name", name, "--network", network]
        options += ["--mount", f"type=volume,source={config_volume},target=/application/config,readonly"]
        if ports:
            options += ["-p", "127.0.0.1::8080", "--read-only", "--tmpfs", "/tmp:rw,nosuid,size=128m"]
            # 与生产应用容器相同的内存预算；只验证启动，不代表并发容量压测。
            options += ["--memory=2g", "--memory-swap=2g", "--cpus=2",
                        "--cap-drop=ALL", "--security-opt=no-new-privileges:true"]
        options += ["-e", "SPRING_PROFILES_ACTIVE=prod", "-e", "MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true"]
        docker(*options, image, *command, env=environment)
        docker("start", name)

    def complete(name, log_name, timeout=180):
        try:
            code = subprocess.check_output(["docker", "wait", name], timeout=timeout, text=True).strip()
        finally:
            with (output / log_name).open("w") as log:
                subprocess.run(["docker", "logs", name], stdout=log, stderr=subprocess.STDOUT, check=False)
        if code != "0":
            raise RuntimeError(f"容器失败，检查 {output / log_name}")

    try:
        docker("volume", "create", config_volume)
        config_created = True
        # 独立桥接网络；数据库不发布端口，只有验收 HTTP 绑定宿主机回环地址。
        docker("network", "create", network)
        network_created = True
        containers.append(database)
        docker("run", "-d", "--name", database, "--network", network, "--network-alias", "database",
               "-e", "POSTGRES_DB=avalon_training", "-e", "POSTGRES_USER=avalon_training",
               "-e", "POSTGRES_PASSWORD", args.postgres_image, env=environment)
        for _ in range(120):
            ready = subprocess.run(["docker", "exec", database, "pg_isready", "-h", "127.0.0.1",
                                    "-U", "avalon_training"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            if ready.returncode == 0:
                break
            time.sleep(0.5)
        else:
            raise RuntimeError("临时数据库未就绪")

        print("准备隔离数据库结构", flush=True)
        migration = prefix + "-migrate"
        # onRefresh 在生命周期启动前退出，数据库迁移已执行，调度器尚未开始工作。
        environment["JAVA_TOOL_OPTIONS"] = "-Dspring.context.exit=onRefresh"
        containers.append(migration)
        options = ["create", "--name", migration, "--network", network,
                   "--mount", f"type=volume,source={config_volume},target=/application/config",
                   "-e", "JAVA_TOOL_OPTIONS", "-e", "SPRING_LIQUIBASE_ENABLED=true"]
        docker(*options, source_id, "plain", env=environment)
        configure(migration)
        docker("start", migration)
        complete(migration, "migration.log")
        environment.pop("JAVA_TOOL_OPTIONS")

        print("在最终运行环境中训练 Spring AOT + Leyden", flush=True)
        trainer = prefix + "-train"
        start(trainer, source_id, "train")
        complete(trainer, "training.log")
        docker("cp", f"{trainer}:/application/app.aot", str(output / "app.aot"))
        if (output / "app.aot").stat().st_size == 0:
            raise RuntimeError("缓存为空")

        # 使用插件构建最终镜像；仅传入训练缓存，不把数据库或日志写入镜像。
        context = output / "context"
        context.mkdir()
        (output / "app.aot").rename(context / "app.aot")
        command = [str(ROOT / "mvnw"), "-B", "-N", "-f", str(ROOT / "avalon-app/pom.xml"), "-Pdocker",
                   f"-Davalon.image={args.image}", f"-Davalon.base-image={source_id}",
                   f"-Davalon.docker-context={context}",
                   f"-Davalon.dockerfile={ROOT / 'avalon-app/src/main/docker/Dockerfile.trained'}",
                   "docker:build"]
        with (output / "image-build.log").open("w") as log:
            subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)

        print("验证最终镜像：强制缓存、root、只读根文件系统、HTTP 业务流程", flush=True)
        runner = prefix + "-verify"
        start(runner, args.image, ports=True)
        port = int(docker("port", runner, "8080/tcp").rsplit(":", 1)[1])
        began = time.monotonic()
        for _ in range(450):
            if docker("inspect", "--format", "{{.State.Running}}", runner) != "true":
                raise RuntimeError("最终镜像提前退出")
            try:
                if http(port, READINESS_PATH)["status"] == "UP":
                    break
            except OSError:
                pass
            time.sleep(0.2)
        else:
            raise RuntimeError("最终镜像 HTTP 就绪超时")
        ready_seconds = time.monotonic() - began
        smoke(port)
        with (output / "runtime.log").open("w") as log:
            subprocess.run(["docker", "logs", runner], stdout=log, stderr=subprocess.STDOUT, check=True)
        assert "Using AOT-linked classes: true" in (output / "runtime.log").read_text()
        assert docker("exec", runner, "id", "-u") == "0"
        image_id = docker("image", "inspect", "--format", "{{.Id}}", args.image)
        (output / "result.json").write_text(json.dumps({
            "source_image": source_id, "final_image": image_id, "tag": args.image,
            "cache_bytes": (context / "app.aot").stat().st_size,
            "readiness_poll_seconds": ready_seconds,
            "verified": ["aot-linked classes", "root uid 0", "read-only rootfs", "registration", "character", "world"],
        }, indent=2) + "\n")
        print(f"完成，本地镜像：{args.image}", flush=True)
    finally:
        for name in reversed(containers):
            # 失败时也保留应用日志，日志不包含本脚本生成的请求令牌。
            with (output / f"{name}.log").open("w") as log:
                subprocess.run(["docker", "logs", name], stdout=log, stderr=subprocess.STDOUT, check=False)
            subprocess.run(["docker", "rm", "-f", "-v", name], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if network_created:
            subprocess.run(["docker", "network", "rm", network], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if config_created:
            subprocess.run(["docker", "volume", "rm", config_volume], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def interrupted(signum, frame):
    raise KeyboardInterrupt()


if __name__ == "__main__":
    signal.signal(signal.SIGTERM, interrupted)
    main()
