---
name: avalon-transaction-outbox
description: Use when a committed write in avalon must notify another bounded context, or when implementing or reviewing local transaction plus outbox, dispatcher retries, and idempotency.
---

# Avalon Transaction Outbox

## 概览

使用这个技能，在不破坏事务一致性的前提下实现或审查跨上下文事件发布。

这个技能聚焦事务边界和 outbox 流程。

## 工作流程

### 1. 先判断是否必须使用 outbox

- 阅读 [references/outbox-decision-tree.md](references/outbox-decision-tree.md)
- 如果写入完全停留在单个上下文内，且没有跨上下文通知需求，通常不需要 outbox
- 如果一次成功提交之后必须通知其他上下文，默认使用 outbox

### 2. 画清事务边界

- 保持业务写入和 outbox 插入处于同一个本地事务中
- 不要在提交成功前，把发布到 broker 或 event bus 当成一个独立的 best-effort 副作用去执行
- 不要在事务里夹带不必要的远程调用、重计算或大对象组装

### 3. 实现发布流程

- 阅读 [references/outbox-flow.md](references/outbox-flow.md)
- 同时持久化业务状态和 outbox 事件
- 在提交后异步分发
- 显式记录重试状态和发布结果

### 4. 保护消费端和重试流程

- 阅读 [references/idempotency-and-antipatterns.md](references/idempotency-and-antipatterns.md)
- 让消费端具备幂等性
- 保持重试逻辑可观测
- 拒绝隐藏式双写模式
- 避免在 `writer`、`dispatcher`、`consumer` 多层重复包装同一错误或重复记录同一状态

### 5. 收尾前复核

结束前检查：

1. 业务写入和 outbox 插入是否共享同一个本地事务
2. 关键写路径中是否没有直接发布这种裸第二次写入
3. `dispatcher` 的重试是否是显式设计的
4. 消费端是否有明确的幂等策略
5. 设计是否仍然遵守 bounded context 归属
6. 是否已经对关键 outbox 路径做了最小必要验证
