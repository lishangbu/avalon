#!/bin/sh
# Author: Shangbu Li
# You must run this script before running the application

# 获取脚本所在的目录并切换到上一级目录
ROOT_DIR=$(dirname "$(dirname "$0")")

cd $ROOT_DIR/avalon-support/avalon-security-support/src/main/resources
mkdir rsa
cd rsa

openssl genpkey -algorithm RSA -out private.key -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in private.key -out public.key

