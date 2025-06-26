#!/bin/sh
# Author: Shangbu Li
# You must package project before running this script

# 获取脚本所在的目录并切换到上一级目录
ROOT_DIR=$(dirname "$(dirname "$0")")

JAR_FILE="$ROOT_DIR/avalon-shell/target/avalon-shell.jar"

RUN_COMMAND="java -jar $JAR_FILE"

# 定义 main 函数
main() {
    $RUN_COMMAND help
    echo "正在初始化数据集..."

    echo "正在初始化基准数据..."
    echo "正在填充宝可梦属性表数据..."
    $RUN_COMMAND dataset refresh type
    echo "完成基准数据初始化..."

    echo "正在初始化树果信息..."
    echo "正在填充树果硬度表数据..."
    $RUN_COMMAND dataset refresh berryFirmness
    echo "正在填充树果表数据..."
    $RUN_COMMAND dataset refresh berry
    echo "完成树果信息初始化..."

    echo "正在初始化道具信息..."
    echo "正在填充道具属性表数据..."
    $RUN_COMMAND dataset refresh itemAttribute
    echo "正在填充道具投掷效果表数据..."
    $RUN_COMMAND dataset refresh itemFlingEffect
    echo "正在填充道具口袋表数据..."
    $RUN_COMMAND dataset refresh itemPocket
    echo "正在填充道具分类表数据..."
    $RUN_COMMAND dataset refresh itemCategory
    echo "正在填充道具表数据..."
    $RUN_COMMAND dataset refresh item
    echo "完成道具信息初始化..."

    echo "正在初始化招式信息..."
    echo "正在填充招式导致的状态异常表数据..."
    $RUN_COMMAND dataset refresh moveAilment
    echo "正在填充招式宽泛分类类别表数据..."
    $RUN_COMMAND dataset refresh moveCategory
    echo "正在填充招式伤害类别表数据..."
    $RUN_COMMAND dataset refresh moveDamageClass
    echo "正在填充战斗招式目标表数据..."
    $RUN_COMMAND dataset refresh moveLearnMethod
    echo "正在填充战斗招式目标表数据..."
    $RUN_COMMAND dataset refresh moveTarget
    echo "正在填充招式表数据..."
    $RUN_COMMAND dataset refresh move
    echo "完成招式信息初始化..."

    echo "正在初始化宝可梦信息..."
    echo "正在填充宝可梦属性克制表数据..."
    $RUN_COMMAND dataset refresh typeDamageRelation
    echo "完成宝可梦信息初始化..."
    echo "完成数据集初始化..."
}

# 检查 JAR 文件是否存在
if [ -e "$JAR_FILE" ]; then
    echo "shell jar文件存在，开始执行逻辑"
    main
else
    echo "shell jar文件不存在，请先打包项目"
    exit 1
fi
