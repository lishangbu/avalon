#!/bin/sh
# Author: Shangbu Li
# You must run this script before running the application

# 获取脚本所在的目录并切换到上一级目录
ROOT_DIR=$(dirname "$(dirname "$0")")

cd $ROOT_DIR
tree -A -L 3  -I target -d