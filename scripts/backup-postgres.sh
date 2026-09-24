#!/bin/sh
set -eu
umask 077

# 备份脚本只读取环境变量，不把密码写入命令行参数或输出。
: "${PGHOST:?设置 PGHOST}"
: "${PGPORT:=5432}"
: "${PGUSER:?设置 PGUSER}"
: "${PGDATABASE:?设置 PGDATABASE}"
: "${BACKUP_DIR:?设置 BACKUP_DIR}"
export PGHOST PGPORT PGUSER PGDATABASE

mkdir -p "$BACKUP_DIR"
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
file="$BACKUP_DIR/avalon-$timestamp.dump"
temporary=$(mktemp "$BACKUP_DIR/.avalon-$timestamp.XXXXXX")
trap 'rm -f "$temporary"' EXIT HUP INT TERM
pg_dump --format=custom --file="$temporary" "$PGDATABASE"
pg_restore --list "$temporary" >/dev/null
mv "$temporary" "$file"
printf '%s\n' "$file"
