#!/bin/sh
# Author: Shangbu Li
# 数据来源https://github.com/renfei/ip2location
# 加速方案:https://github.akams.cn

# 获取脚本所在的目录并切换到上一级目录
ROOT_DIR=$(dirname "$(dirname "$0")")
# 文件最终目的地
destination="$ROOT_DIR/avalon-extensions/avalon-ip2location-spring-boot-starter/src/main/resources"

# 镜像站点
mirrorSite='https://gh-proxy.org'
# 数据文件版本
dbVersion='2025.12.01'
# 数据文件名
dbFileNames=('IP2LOCATION-LITE-DB11.IPV6.BIN')

urlPrefix="$mirrorSite/https://github.com/renfei/ip2location/releases/download/$dbVersion"

# 循环下载每个文件
for dbFileName in "${dbFileNames[@]}"; do
  url="$urlPrefix/$dbFileName"
  echo "正在下载: $url"
  curl -O "$url"  # -O 用来指定保存文件的名字与 URL 文件名一致
  mv -v $dbFileName "${destination/$dbFileName}"
done


