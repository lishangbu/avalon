---
name: quartz
description: "Quartz scheduling work in Avalon. Use for scheduled tasks, Quartz jobs, triggers, cron expressions, scheduler configuration, task state, manual triggering, execution records, retries, concurrency, or scheduler persistence tests."
---

# Quartz

## 修改前

- 检查任务定义、Quartz 配置、持久化实体、触发 API 和现有执行测试。
- 涉及数据库或 Spring 装配时同时加载 `jimmer`、`liquibase-postgresql` 或 `spring-boot-web`。

## 项目要求

- 将任务身份、调度表达式和运行状态建模为明确类型，不使用页面文案作为稳定 key。
- 明确 cron 时区；不得依赖部署机器默认时区。
- Job 保持可重入或显式禁止并发；重试、人工触发和定时触发共享同一业务入口。
- 长任务不得占用 Web 请求线程；取消、超时与失败状态必须落入可观察执行结果。
- 调度注册与业务执行分离；Quartz context 不承载不可序列化业务对象。
- 数据库事务只包围必要状态变化，不把远程调用或长计算放在长事务内。
- 日志使用任务 code 与 execution id，不记录凭据或完整敏感 payload。

## 测试驱动

- 使用可控时钟或直接触发 job，避免依赖真实分钟边界和任意 sleep。
- 覆盖注册、暂停、恢复、人工触发、并发限制、失败与执行记录。
- 持久化变化使用 PostgreSQL Testcontainers。

## 完成标准

- scheduler 模块聚焦测试与相关 API 测试通过。
- cron/时区、状态转换和并发语义有明确断言。
- 测试无时间竞争和不可控等待。
- schema 或配置变化同步通过迁移与 app 上下文测试。
