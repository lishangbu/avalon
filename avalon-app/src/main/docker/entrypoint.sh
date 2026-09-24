#!/bin/sh
set -eu
# 训练与缓存运行共用 JVM 参数；修改后必须重新训练镜像。
# 普通模式允许 JAVA_TOOL_OPTIONS，缓存模式拒绝隐式改变训练条件。
mode=${1:-plain}
if [ "$#" -gt 0 ]; then shift; fi
case "$mode" in
  plain)
    exec java @jvm.options \
      -Dspring.aot.enabled=false -jar application.jar "$@"
    ;;
  train|cached)
    if [ -n "${JAVA_TOOL_OPTIONS:-}${JDK_JAVA_OPTIONS:-}${_JAVA_OPTIONS:-}" ]; then
      echo '缓存模式不接受额外 JVM 环境参数；请修改固定参数并重新训练镜像。' >&2
      exit 64
    fi
    if [ "${SPRING_PROFILES_ACTIVE:-prod}" != prod ] || [ "${SPRING_LIQUIBASE_ENABLED:-false}" != false ]; then
      echo 'Spring AOT 要求 prod、迁移关闭；改变条件配置须重新构建。' >&2
      exit 64
    fi
    if [ "$mode" = train ]; then
      exec java @jvm.options \
        -Dspring.aot.enabled=true -Dspring.context.exit=onRefresh \
        -XX:AOTCacheOutput=app.aot -jar application.jar "$@"
    fi
    exec java @jvm.options \
      -Dspring.aot.enabled=true -XX:AOTMode=on -XX:AOTCache=app.aot \
      -Xlog:aot=info -jar application.jar "$@"
    ;;
  *) echo '启动模式只能是 plain、train 或 cached。' >&2; exit 64 ;;
esac
