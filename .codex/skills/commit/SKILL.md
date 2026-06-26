---
name: commit
description: "Use when the user explicitly asks to create, review, generate, or execute Git commit messages or safe repository commit workflows in the avalon repository."
---

# 后端提交

## 核心原则

没有用户明确允许，不执行 `git commit`。用户只要求生成提交说明时，只输出提交说明，不暂存、不提交。

## 提交边界

- 后端仓库只提交 `avalon` 内文件。
- 前端仓库改动单独提交。
- 一个提交只表达一个主要意图。
- 不使用 `git add .`，只暂存本轮相关文件。

## 提交格式

使用 Conventional Commits：

```text
<type>[optional scope]: <summary>
```

常用 type：

- `feat`
- `fix`
- `refactor`
- `test`
- `docs`
- `build`
- `chore`

summary 使用中文动宾短语，不以句号结尾。

## 提交前验证

提交前至少运行：

```bash
git status --short
git diff --check
```

并运行与改动匹配的模块测试。
