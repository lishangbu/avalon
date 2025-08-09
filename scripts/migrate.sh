#!/bin/sh
# Author: Shangbu Li
# You must package project before running this script

# 获取脚本所在的目录并切换到上一级目录
ROOT_DIR=$(dirname "$(dirname "$0")")
JAR_FILE="$ROOT_DIR/avalon-shell/target/avalon-shell.jar"
RUN_COMMAND="java -jar $JAR_FILE"

# 定义初始化数据集的函数
initialize_dataset() {
    local dataset_name=$1
    echo "正在初始化 $dataset_name..."
    $RUN_COMMAND dataset refresh --type "$dataset_name"
    echo "$dataset_name 初始化完成..."
}

# 定义 main 函数
main() {
    # 执行 help 命令
    $RUN_COMMAND help

    # 初始化各类数据集
    echo "正在初始化数据集..."

    # 基准数据
    initialize_dataset "type"

    # 树果信息
    initialize_dataset "berry-firmness"
    initialize_dataset "berry"

    # 道具信息
    initialize_dataset "item-attribute"
    initialize_dataset "item-fling-effect"
    initialize_dataset "item-pocket"
    initialize_dataset "item-category"
    initialize_dataset "item"

    # 招式信息
    initialize_dataset "move-ailment"
    initialize_dataset "move-category"
    initialize_dataset "move-damage-class"
    initialize_dataset "move-learn-method"
    initialize_dataset "move-target"
    initialize_dataset "move"

    # 属性信息
    #initialize_dataset "type-damage-relation"

    echo "数据集初始化完成..."
}

# 检查 JAR 文件是否存在
if [ -f "$JAR_FILE" ]; then
    echo "shell JAR 文件存在，开始执行逻辑..."
    main
else
    echo "shell JAR 文件不存在，请先打包项目..."
    exit 1
fi
