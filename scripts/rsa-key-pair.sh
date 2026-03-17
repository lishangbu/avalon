#!/usr/bin/env bash
# Author: Shangbu Li
# 说明：在一个临时目录生成 RSA 密钥对，并把相同的密钥复制到多个模块的 resources/rsa 目录中
# 使用：
#   ./rsa-key-pair.sh [keysize] [-f]
#   keysize: RSA 位数，默认 2048
#   -f: 强制覆盖目标已有密钥（会先备份）

set -euo pipefail

# 获取 Maven 根目录（向上查找 .mvn）
SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
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

# 默认要写入密钥的目标模块（相对于 ROOT_DIR）
APPLICATIONS=(
  "avalon-application/avalon-admin-server"
  "avalon-application/avalon-standalone-server"
)


usage() {
  cat <<'EOF'
用法:
  ./rsa-key-pair.sh [keysize] [-f]
  keysize: RSA 位数，默认 2048
  -f|--force: 强制覆盖目标已有密钥（会先备份）
EOF
}

# 参数
KEY_SIZE="2048"
FORCE=false
SEEN_SIZE=false
for arg in "$@"; do
  case "$arg" in
    -f|--force)
      FORCE=true
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      if [[ "$arg" =~ ^[0-9]+$ ]]; then
        if [ "$SEEN_SIZE" = true ]; then
          echo "重复的 keysize 参数: $arg" >&2
          usage >&2
          exit 2
        fi
        KEY_SIZE="$arg"
        SEEN_SIZE=true
      else
        echo "未知参数: $arg" >&2
        usage >&2
        exit 2
      fi
      ;;
  esac
done

KEY_NAME_PRIVATE="private_key.pem"
KEY_NAME_PUBLIC="public_key.pem"

log() { printf "[%s] %s\n" "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || { echo "缺少命令: $1" >&2; exit 1; }
}

if ! [[ "$KEY_SIZE" =~ ^[0-9]+$ ]]; then
  echo "keysize 不是数字: $KEY_SIZE" >&2
  exit 2
fi
if [ "$KEY_SIZE" -lt 1024 ]; then
  echo "keysize 太小（>=1024）: $KEY_SIZE" >&2
  exit 2
fi

require_cmd openssl

# 目标列表：仅包含 APPLICATIONS 指定的两个应用的 resources/rsa 目录
TARGETS=()
for app in "${APPLICATIONS[@]}"; do
  app_dir="${ROOT_DIR}/${app}"
  if [ -d "$app_dir" ]; then
    TARGETS+=("${app_dir}/src/main/resources/rsa")
  else
    log "跳过不存在模块: $app_dir"
  fi
done

if [ "${#TARGETS[@]}" -eq 0 ]; then
  log "未找到任何目标模块，已退出"
  exit 1
fi

if [ "$FORCE" = false ]; then
  existing=()
  for dest in "${TARGETS[@]}"; do
    if [ -f "$dest/${KEY_NAME_PRIVATE}" ] || [ -f "$dest/${KEY_NAME_PUBLIC}" ]; then
      existing+=("$dest")
    fi
  done
  if [ "${#existing[@]}" -gt 0 ]; then
    log "发现已有密钥，未使用 -f，已退出"
    for dest in "${existing[@]}"; do
      log "已存在: $dest"
    done
    exit 1
  fi
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

log "在临时目录生成密钥对 (size=${KEY_SIZE})..."
openssl genpkey -algorithm RSA -out "${TMP_DIR}/${KEY_NAME_PRIVATE}" -pkeyopt rsa_keygen_bits:${KEY_SIZE}
openssl rsa -pubout -in "${TMP_DIR}/${KEY_NAME_PRIVATE}" -out "${TMP_DIR}/${KEY_NAME_PUBLIC}"

# 设置合适的权限
chmod 600 "${TMP_DIR}/${KEY_NAME_PRIVATE}"
chmod 644 "${TMP_DIR}/${KEY_NAME_PUBLIC}"

# 将密钥复制到每个目标目录，遇到已有文件时先备份（除非 FORCE=true）
TIMESTAMP="$(date +%s)"
for dest in "${TARGETS[@]}"; do
  if [ -z "$dest" ]; then
    continue
  fi
  mkdir -p "$dest"
  if [ "$FORCE" = true ] && { [ -f "$dest/${KEY_NAME_PRIVATE}" ] || [ -f "$dest/${KEY_NAME_PUBLIC}" ]; }; then
    BACKUP_DIR="$dest/.rsa_backup_${TIMESTAMP}"
    mkdir -p "$BACKUP_DIR"
    [ -f "$dest/${KEY_NAME_PRIVATE}" ] && mv "$dest/${KEY_NAME_PRIVATE}" "$BACKUP_DIR/"
    [ -f "$dest/${KEY_NAME_PUBLIC}" ] && mv "$dest/${KEY_NAME_PUBLIC}" "$BACKUP_DIR/"
    log "已存在旧密钥，移动到备份目录: $BACKUP_DIR"
  fi

  cp "${TMP_DIR}/${KEY_NAME_PRIVATE}" "$dest/${KEY_NAME_PRIVATE}"
  cp "${TMP_DIR}/${KEY_NAME_PUBLIC}" "$dest/${KEY_NAME_PUBLIC}"
  chmod 600 "$dest/${KEY_NAME_PRIVATE}"
  chmod 644 "$dest/${KEY_NAME_PUBLIC}"
  log "已写入密钥到: $dest"
done

log "完成：相同的一对密钥已部署到 ${#TARGETS[@]} 个目标"
log "临时文件将在退出时清理： $TMP_DIR"
