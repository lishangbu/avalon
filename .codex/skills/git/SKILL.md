---
name: git
description: "Git work in the Avalon backend repository. Use when reviewing status or diffs, staging files, writing commit messages, committing, signing or re-signing commits, handling hooks, checking signatures, or preparing a backend commit."
---

# Git

## 授权边界

- 只有用户明确要求实际提交时才执行 `git commit`；仅要求提交说明时不暂存。
- 不 push、不改写历史、不删除分支，除非用户分别明确授权。
- 只处理 `avalon` 仓库；前端改动在其仓库独立提交。

## 提交前

1. 运行 `git status --short`，区分本轮改动与用户已有改动。
2. 运行 `git diff --check` 并审查完整 diff。
3. 只暂存本轮相关路径；不使用 `git add .`。
4. 运行 `git diff --cached --check` 并审查 staged diff。
5. 运行所有命中技术栈 skill 的 completion criteria。

## 提交要求

- 使用 Conventional Commits：`<type>[optional scope]: <中文动宾短语>`，不以句号结尾。
- 一个提交表达一个主要意图；不要混合前端或无关用户改动。
- 使用配置好的 GPG key 签名提交。
- hook 失败时先区分代码失败和环境失败；只有完成等价验证后才允许 `--no-verify`，并在交付说明记录。

## 完成标准

- 提交后运行 `git log --show-signature -1`，并确认格式字段 `%G?` 为 `G`。
- 工作区只剩明确未提交的用户改动，或完全干净。
- 报告 commit hash、主题、签名状态、执行的验证和任何跳过项。
- 未获得 push 授权时明确保持本地。
