#!/usr/bin/env bash
# Author: Shangbu Li
# 说明：在一个临时目录生成 RSA 密钥对，并把相同的密钥复制到多个模块的 resources/rsa 目录中
# 使用：
#   ./rsa-key-pair.sh [keysize] [-f]
#   keysize: RSA 位数，默认 2048
#   -f: 强制覆盖目标已有密钥（会先备份）

set -euo pipefail

# 获取脚本所在目录并切换到上一级目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# 默认要写入密钥的目标模块（相对于 ROOT_DIR）
APPLICATIONS=(
  "avalon-application/avalon-admin-server"
  "avalon-application/avalon-standalone-server"
)


# 参数
KEY_SIZE="${1:-2048}"
FORCE=false
# 如果位置参数中包含 -f 或 --force，则启用强制覆盖
for arg in "$@"; do
  if [ "$arg" = "-f" ] || [ "$arg" = "--force" ]; then
    FORCE=true
  fi
done

KEY_NAME_PRIVATE="private_key.pem"
KEY_NAME_PUBLIC="public_key.pem"

log() { printf "[%s] %s\n" "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

log "在临时目录生成密钥对 (size=${KEY_SIZE})..."
openssl genpkey -algorithm RSA -out "${TMP_DIR}/${KEY_NAME_PRIVATE}" -pkeyopt rsa_keygen_bits:${KEY_SIZE}
openssl rsa -pubout -in "${TMP_DIR}/${KEY_NAME_PRIVATE}" -out "${TMP_DIR}/${KEY_NAME_PUBLIC}"

# 设置合适的权限
chmod 600 "${TMP_DIR}/${KEY_NAME_PRIVATE}"
chmod 644 "${TMP_DIR}/${KEY_NAME_PUBLIC}"

# 目标列表：仅包含 APPLICATIONS 指定的两个应用的 resources/rsa 目录
TARGETS=()
for app in "${APPLICATIONS[@]}"; do
  TARGETS+=("${ROOT_DIR}/${app}/src/main/resources/rsa")
done

# 将密钥复制到每个目标目录，遇到已有文件时先备份（除非 FORCE=true）
TIMESTAMP="$(date +%s)"
for dest in "${TARGETS[@]}"; do
  if [ -z "$dest" ]; then
    continue
  fi
  mkdir -p "$dest"
  if [ -f "$dest/${KEY_NAME_PRIVATE}" ] || [ -f "$dest/${KEY_NAME_PUBLIC}" ]; then
    if [ "$FORCE" = true ]; then
      BACKUP_DIR="$dest/.rsa_backup_${TIMESTAMP}"
      mkdir -p "$BACKUP_DIR"
      [ -f "$dest/${KEY_NAME_PRIVATE}" ] && mv "$dest/${KEY_NAME_PRIVATE}" "$BACKUP_DIR/"
      [ -f "$dest/${KEY_NAME_PUBLIC}" ] && mv "$dest/${KEY_NAME_PUBLIC}" "$BACKUP_DIR/"
      log "已存在旧密钥，移动到备份目录: $BACKUP_DIR"
    else
      # 如果不强制覆盖，则先备份再覆盖（保证一致性）
      BACKUP_DIR="$dest/.rsa_backup_${TIMESTAMP}"
      mkdir -p "$BACKUP_DIR"
      [ -f "$dest/${KEY_NAME_PRIVATE}" ] && mv "$dest/${KEY_NAME_PRIVATE}" "$BACKUP_DIR/"
      [ -f "$dest/${KEY_NAME_PUBLIC}" ] && mv "$dest/${KEY_NAME_PUBLIC}" "$BACKUP_DIR/"
      log "发现旧密钥，已自动备份到: $BACKUP_DIR"
    fi
  fi

  cp "${TMP_DIR}/${KEY_NAME_PRIVATE}" "$dest/${KEY_NAME_PRIVATE}"
  cp "${TMP_DIR}/${KEY_NAME_PUBLIC}" "$dest/${KEY_NAME_PUBLIC}"
  chmod 600 "$dest/${KEY_NAME_PRIVATE}"
  chmod 644 "$dest/${KEY_NAME_PUBLIC}"
  log "已写入密钥到: $dest"
done

log "完成：相同的一对密钥已部署到 ${#TARGETS[@]} 个目标"
log "临时文件将在退出时清理： $TMP_DIR"
