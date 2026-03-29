---
name: db-changelog
description: Liquibase YAML 数据库变更助手。用于创建、修改或审查 `db/changelog/**/*.yaml`、初始化 schema、外键、索引、唯一约束、seed 兼容性和数据库命名规范相关任务；也用于检查数据库结构变更是否符合仓库约定。
---

# Database Changelog

## Overview

在处理 Liquibase YAML 变更前，先确定仓库里是否存在 `db/changelog/CONVENTIONS.md`。

- 如果存在，优先把它当作 source of truth
- 如果不存在，使用 `references/db-changelog-conventions.md`

不要只盯着单个字段或单个表。数据库变更至少同时评估：

- 外键声明方式
- 外键列是否需要索引
- 是否存在稳定自然键应加唯一约束
- seed 数据是否仍能通过约束校验
- 命名是否符合仓库既有风格

## Workflow

### 1. 建立上下文

- 打开目标 changelog 文件和同类表的历史实现
- 判断当前仓库是“初始化阶段可整理历史文件”，还是“已发版只能追加 changelog”
- 如果是 Avalon 仓库，优先对照 `db/changelog/versions/0.0.1/0.0.1.yaml`

### 2. 设计结构变更

- 被引用表已经在当前 `changeSet` 前面创建，且不需要延迟校验或特殊更新策略时，优先把外键内联到 `createTable.columns[].constraints`
- 自关联外键、延迟外键、特殊 `onDelete`、特殊 `onUpdate` 场景，使用 `addForeignKeyConstraint`
- 不要因为已经加了外键，就跳过索引评估
- 树结构表默认检查 `parent_id` 索引
- 多对多关联表除了复合主键，还要评估反向查询列的单列索引
- 只有稳定自然键才考虑唯一约束，例如 `code`、`key`、`internal_name`

### 3. 校验 seed 与兼容性

- 新增唯一约束前，先确认 seed 数据没有重复
- 新增外键前，先确认 seed 装载顺序可以通过校验
- 如果当前数据集并不完整，不为了理论完整性强行补外键
- 自关联 seed 容易受装载顺序影响时，优先考虑延迟外键

### 4. 保持命名一致

- 索引命名使用 `idx_<table>__<column>` 或 `idx_<table>__<col1>__<col2>`
- 唯一约束命名使用 `uk_<table>__<column>`
- 外键命名使用 `fk_<table>__<column>`

### 5. 收尾检查

提交前至少检查以下内容：

1. 关联列是否评估过索引
2. 稳定自然键是否应加唯一约束
3. seed 数据是否仍能通过全部外键和唯一约束
4. 命名是否和现有 changelog 一致
5. 如果环境允许，验证一次建库流程或关键仓储测试

## Reference

- 优先读取目标仓库中的 `db/changelog/CONVENTIONS.md`
- 若仓库内没有该文件，再读取 `references/db-changelog-conventions.md`
