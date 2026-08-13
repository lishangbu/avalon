---
status: accepted
---

# 使用 Worker 执行受控校验任务

## 背景

Battle 回放和审计哈希链校验需要统一的认证、审计、幂等与重试边界，不能形成独立的操作入口。

## 决策

- 生产制品固定为 `avalon-server`、`avalon-admin-server` 和 `avalon-worker` 三个命令。
- Battle 回放校验由 Asynq Worker 执行，并由管理端专用命令受控创建。任务读取 Battle 权威事实，通过 Battle Engine 重放并比较事件、状态摘要和最终结果。
- 审计哈希链由 Worker 周期校验，同时允许管理员通过专用命令显式触发。两种入口共用同一任务实现和安全结果模型。
- 任务参数保存发起管理员与请求事实，成功操作写入管理员审计；后台任务列表只暴露安全摘要，不暴露完整回放、命令、事件、随机轨迹、成员快照或审计业务 changes。
- 黄金样本校验保留在 Battle Engine 测试和构建质量门禁中，不提供生产命令入口。
- 校验能力位于 `internal/battle`、`internal/platform/audit` 与 `internal/battleengine` 的现役边界。

## 后果

部署只需要构建和管理三个命令，所有人工运维校验都经过管理员认证、幂等命令与审计边界。周期校验和人工触发复用 Worker 的重试、取消和可观测能力；管理端不得成为读取完整 Battle 权威事实或审计业务内容的旁路。
