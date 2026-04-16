---
name: avalon-module-scaffold
description: 使用 `avalon` 项目的标准 `Gradle Kotlin DSL`、bounded context 模块布局、分层包结构和依赖规则来创建或调整模块。适用于新增上下文模块、引入 `shared-kernel`、`shared-application`、`shared-infra` 等共享模块、拆分已有模块、建立标准的 `domain/application/infrastructure/interfaces/projection` 结构，整理 `interfaces/http` 的按能力分包与 Kotlin 文件拆分，或检查文件是否放在正确的模块与包路径中。
---

# Avalon 模块脚手架

## 概览

在新增后端代码前，先用这个技能搭好或审查模块结构。
这个技能聚焦模块形状和依赖方向。

## 工作流程

### 1. 先确定归属上下文

- 先把这次工作归到唯一一个主归属上下文
- 默认上下文是 `identity-access`、`catalog`、`player`、`battle`
- 如果需求跨多个上下文，先拆清归属，再创建文件
- 先搜索现有相似模块、相似包结构和相似分层落位，不要不看现状就起新目录

### 2. 选择模块类型

- 业务代码使用 bounded context 模块
- 稳定且业务中性的抽象使用 `shared-kernel`
- 稳定且业务中性的应用层契约使用 `shared-application`
- 技术底座使用 `shared-infra`
- 不要因为“以后也许会复用”就新建共享模块
- 处理顺序固定为：优先复用现有模块，再扩展现有模块，最后才新增模块或目录

### 3. 套用标准布局

- 阅读 [references/module-layout.md](references/module-layout.md) 获取默认目录树
- 当 `interfaces/http` 开始膨胀时，阅读 [references/interfaces-http-layout.md](references/interfaces-http-layout.md)
  获取按能力分包与 Kotlin 文件拆分规则
- 阅读 [references/gradle-conventions.md](references/gradle-conventions.md) 获取模块 `build.gradle.kts` 基线
- 保持 `domain` 不依赖 Quarkus、REST、持久化注解和 reactive client 类型

### 4. 严格执行依赖方向

- 在接模块依赖前，先阅读 [references/dependency-rules.md](references/dependency-rules.md)
- 保持跨上下文依赖显式且最小化
- 拒绝跨上下文 `repository` 调用和基础设施直连耦合

### 5. 收尾前复核

结束前检查：

1. 模块是否只有一个清晰的归属上下文
2. 文件是否放在正确的分层下
3. `shared-kernel`、`shared-application` 和 `shared-infra` 是否没有被当成杂物箱
4. Gradle 依赖是否遵循允许的方向
5. 跨上下文交互是否通过应用服务、ACL、projection 或事件表述
6. 是否为了省事复制了现有模块中的相似逻辑或近似类型
7. `interfaces/http` 的能力包内 Kotlin 类型是否按职责拆成独立文件，避免巨型 `Dtos.kt` / `Resources.kt`
