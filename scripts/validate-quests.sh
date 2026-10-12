#!/usr/bin/env bash
# 离线任务校验：构建宿主及依赖后执行相同的生产目录校验，不启动应用或数据库。
# 可传上次校验导出的JSON清单做兼容检查；成功结果写到标准输出，构建日志写到标准错误。
set -euo pipefail
quest_repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$quest_repo_root"
./mvnw -q -pl avalon-world -am install -DskipTests >&2
quest_classpath_file="$(mktemp)"
trap 'rm -f "$quest_classpath_file"' EXIT
./mvnw -q -pl avalon-world dependency:build-classpath -Dmdep.outputFile="$quest_classpath_file" >&2
java -cp "avalon-world/target/classes:$(cat "$quest_classpath_file")" \
  io.github.lishangbu.avalon.world.quest.QuestContentValidator "$@"
