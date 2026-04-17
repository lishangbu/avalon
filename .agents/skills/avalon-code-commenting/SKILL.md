---
name: avalon-code-commenting
description: Use when adding or reviewing Chinese comments and KDoc for avalon module entry files, public contracts, boundary DTOs, shared abstractions, complex services, repositories, security-sensitive paths, or outbox flows.
---

# Avalon 注释技能

## 概览

把长期说明贴近代码，而不是额外堆到仓库外侧。

这个技能只负责注释策略、注释内容和注释验收，不负责模块拆分、`schema` 归属、事务设计或 `outbox` 选型本身。

## 何时使用

出现以下任一情况时，使用这个技能：

- 新增或修改模块入口文件
- 新增或修改接口、抽象契约、公共服务入口或会被其他模块直接依赖的 API
- 新增或修改对外 Request/Response、事件 payload、快照响应等边界 DTO
- 新增或修改 `shared-kernel` 下对外公开的核心抽象
- 新增或修改 `shared-application` 下会被多个上下文依赖的应用层公共契约
- 新增或修改跨上下文公共契约、快照类型、ACL 边界类型
- 新增或修改复杂 `application service`
- 新增或修改复杂 SQL-first repository
- 新增或修改认证、会话、令牌、权限校验等安全敏感路径
- 新增或修改 `outbox writer`、`dispatcher`、幂等处理
- 回补现有代码注释，或审查注释是否过期、机械、分布不当

## 使用流程

1. 先读 [references/comment-scope.md](references/comment-scope.md)，判断这次改动哪些单元必须补注释，哪些不用补。
2. 需要起草或回补注释时，读 [references/comment-templates.md](references/comment-templates.md)。
3.
怀疑当前注释质量不够、写法跑偏或信息密度太低时，读 [references/comment-anti-patterns.md](references/comment-anti-patterns.md)。
4. 收尾前用 [references/comment-review-checklist.md](references/comment-review-checklist.md) 做一次自检。
5.
如果是给现有代码系统性回补注释，再看 [references/comment-remediation-strategy.md](references/comment-remediation-strategy.md)
控制范围和优先级。

## 核心原则

- 注释默认使用中文，专有名词、框架名、协议名、代码标识和数据库对象名保留英文。
- 声明级注释默认使用 KDoc 风格 `/** ... */`；局部实现提示才使用短行注释。
- 注释优先解释为什么这样设计，以及边界、事务、数据流、安全语义和性能取舍。
- 接口、抽象契约和公共方法默认要求逐个方法写 KDoc，说明作用、关键设计思路、`@param`、`@return` 以及必要时的副作用或异常语义。
- 对外边界 DTO 即使结构简单，也默认要求类级 KDoc，并通过 `@property` 说明字段作用、取值语义和必要约束。
- 注释要贴近真正需要解释的代码位置，默认优先模块入口文件、核心类型头注释、接口方法 KDoc 和复杂方法块注释。
- 不要把注释写成代码动作翻译，也不要用注释替代本该更清晰的命名。
- 如果修改了“必须有注释”的单元，却没有同步更新注释，默认视为任务未完成。
