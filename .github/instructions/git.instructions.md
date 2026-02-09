---
description: "辅助生成 Git 提交信息"
---

# Git 提交规范

## 格式

    <type>[optional scope]: <description>

## 规则

- 冒号后空格
- description 简洁，不超过 50 字，中文，无标点
- 提交前格式化与测试
- 拆分提交

## type

- feat: 新功能
- fix: bug 修复
- to: bug 修复（diff）
- build: 构建变更
- docs: 文档
- style: 格式调整
- refactor: 重构
- perf: 性能优化
- test: 测试
- chore: 辅助工具
- revert: 回滚
- merge: 合并
- sync: 同步
- ci: CI 变更

## scope

- 可选，说明影响范围

## 分支

- main: 主分支
