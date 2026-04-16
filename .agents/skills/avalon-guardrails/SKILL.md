---
name: avalon-guardrails
description: Avalon 项目级后端架构护栏。用于在 `avalon` 项目中设计或修改模块结构、共享模块、数据库 schema、事务边界、outbox 事件、安全接入、bounded context 集成规则时，统一遵守本仓库的模块化单体、DDD、schema 隔离、禁止跨上下文直连，以及本地事务加 outbox 的核心约束。
---

# Avalon 架构护栏

## 概览

先遵守边界，再写代码。

在本仓库做后端相关工作时，默认按以下顺序执行：

1. 判断这次改动属于哪个 `bounded context`
2. 判断是否触碰跨上下文边界、共享模块、数据库 `schema` 或事务/事件
3. 打开 [references/foundation-constraints.md](references/foundation-constraints.md)
4. 优先确认现有实现、相似模块、相似 SQL、相似 DTO、相似异常处理是否已经存在
5. 只有在约束允许的前提下，才继续设计表结构、应用服务、仓储和接口

`foundation-constraints.md` 现在是索引页。根据任务继续补读：

- 架构边界、模块归属、`shared-*`、`schema`
  、依赖方向：读 [references/architecture-boundaries.md](references/architecture-boundaries.md)
- 本地事务、`outbox`
  、安全基线、API/持久化基线：读 [references/data-and-integration-baseline.md](references/data-and-integration-baseline.md)
-
实现工作流、性能意识、兼容策略、错误处理、验证要求：读 [references/implementation-rules.md](references/implementation-rules.md)
- 清理策略与评审检查：读 [references/cleanup-and-review.md](references/cleanup-and-review.md)
- 涉及注释设计、注释回补或注释评审时，同时使用 `avalon-code-commenting`

## 工作流程

### 1. 先识别主归属上下文

- 优先归入 `identity-access`、`catalog`、`player`、`battle`
- 如果一个需求同时触碰多个上下文，先拆成“拥有者上下文”和“协作上下文”
- 不要因为实现方便，把跨上下文逻辑塞进同一个 `service`

### 2. 先检查是否触碰硬规则

只要涉及以下任一项，就先读参考约束：

- 新增模块或调整模块依赖
- 新增表、字段、索引、外键、唯一约束
- 新增事务边界
- 一个上下文要通知另一个上下文
- 新增共享代码到 `shared-*`
- 接认证、鉴权、角色、权限

### 3. 约束优先于实现技巧

- 不允许跨上下文 `repository` 直接调用
- 不允许跨上下文强外键
- 不允许把持久化行模型或数据库 DTO 直接当接口 DTO
- 不允许把“发消息”写成与业务写库分离的裸双写
- 不允许把共享模块当作杂物箱
- 不允许让 `shared-application` 依赖 `shared-infra`；基础设施只能实现应用层契约
- 实现工作流、兼容策略、错误处理、注释和 `deprecated` API 处理，继续按对应 reference 和 skill 执行

### 4. 需要跨上下文时

- 同步协作用应用服务或 ACL
- 异步协作用领域事件加 outbox
- 只有在上下文内才直接走本地事务

### 5. 收尾时复核

至少复核以下问题：

1. 改动是否仍然只属于一个上下文主导
2. 是否破坏 `schema` 隔离
3. 是否出现跨上下文直连
4. 是否出现双写而没有 outbox
5. 是否把不稳定代码塞进 `shared` 模块
6. 其余实现、注释、`deprecated` API 和验证项是否已按对应专题规则复核
