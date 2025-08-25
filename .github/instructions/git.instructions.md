---
description: "辅助生成 Git 提交信息"
---

# Git 提交规范

- 你是一位后端开发专家，精通 Git 操作

## Commit 规范

The commit message should be structured as follows:

    <type>[optional scope]: <description>
    
    [optional body]
    
    [optional footer(s)]

具体要求如下:

1. 注意英文冒号后有一个空格
2. `type` 的枚举值有:

- feat：新功能（feature）
- fix/to：修复bug，可以是QA发现的BUG，也可以是研发自己发现的BUG
  - fix：产生diff并自动修复此问题。适合于一次提交直接修复问题
  - to：只产生diff不自动修复此问题。适合于多次提交。最终修复问题提交时使用fix
- build: 用于修改项目构建系统，例如修改依赖库、外部接口或者升级Jdk版本等
- docs: 文档（documentation）
- style: 格式（不影响代码运行的变动）
- refactor: 重构（即不是新增功能，也不是修改bug的代码变动）
- perf: 优化相关，比如提升性能、体验
- test: 增加测试
- chore: 构建过程或辅助工具的变动
- revert: 回滚到上一个版本
- merge: 代码合并
- sync: 同步主线或分支的Bug
- ci: 用于修改持续集成流程，例如修改 Travis、Jenkins 等工作流配置

3. scope(可选)

- scope用于说明 commit 影响的范围，比如数据层、控制层、视图层等等

4. 保持 `description` 简洁明了，描述清楚变更内容
- `description`是commit目的的简短描述，不超过50个字符
- 建议使用中文
- 结尾不加句号或其他标点符号

## 分支说明

- `main / master`: 主分支
- `gh-pages`: GitHub Pages 构建分支

## 其他

- 禁止自动提交，除非有明确的指示
- 提交前确保代码通过代码校验和单元测试
- 避免大型提交，尽量将变更分解为小的、相关的提交