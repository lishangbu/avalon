#!/usr/bin/env bash
# Author: Shangbu Li
# 数据来源https://github.com/renfei/ip2location
# 加速方案:https://github.akams.cn

# 获取 Maven 根目录（向上查找 .mvn）
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
ROOT_DIR="$SCRIPT_DIR"
while [ "$ROOT_DIR" != "/" ]; do
  if [ -d "$ROOT_DIR/.mvn" ]; then
    break
  fi
  ROOT_DIR=$(dirname "$ROOT_DIR")
done

if [ ! -d "$ROOT_DIR/.mvn" ]; then
  echo "无法定位 Maven 根目录（未找到 .mvn）" >&2
  exit 1
fi
# 文件最终目的地
destination="$ROOT_DIR/avalon-extensions/avalon-ip2location-spring-boot-starter/src/main/resources"
mkdir -p "$destination"

get_mtime() {
  if stat -c %Y "$1" >/dev/null 2>&1; then
    stat -c %Y "$1"
  else
    stat -f %m "$1"
  fi
}

# 镜像站点
mirrorSite='https://gh-proxy.org'
# 数据文件版本
dbVersion='2025.12.01'
# 数据文件名
dbFileNames=('IP2LOCATION-LITE-DB11.IPV6.BIN')

urlPrefix="$mirrorSite/https://github.com/renfei/ip2location/releases/download/$dbVersion"

# 循环下载每个文件
for dbFileName in "${dbFileNames[@]}"; do
  destPath="$destination/$dbFileName"

  candidates=()
  if [ -f "$destPath" ]; then
    candidates+=("$destPath")
  fi

  while IFS= read -r found; do
    echo "发现文件: $found"
    candidates+=("$found")
  done < <(find "$ROOT_DIR" -type d -path "$destination" -prune -o -type f -name "$dbFileName" -print 2>/dev/null)

  if [ "${#candidates[@]}" -eq 0 ]; then
    url="$urlPrefix/$dbFileName"
    echo "正在下载: $url"
    curl -fL -o "$destPath" "$url"
    continue
  fi

  newestPath=""
  newestMtime=0
  for p in "${candidates[@]}"; do
    mtime=$(get_mtime "$p" 2>/dev/null || echo 0)
    if [ -z "$newestPath" ] || [ "$mtime" -gt "$newestMtime" ]; then
      newestPath="$p"
      newestMtime="$mtime"
    fi
  done

  echo "保留最新文件: $newestPath"
  if [ "$newestPath" != "$destPath" ]; then
    mv -v "$newestPath" "$destPath"
  fi

  for p in "${candidates[@]}"; do
    if [ "$p" != "$destPath" ] && [ "$p" != "$newestPath" ]; then
      echo "删除旧文件: $p"
      rm -f "$p"
    fi
  done
done
