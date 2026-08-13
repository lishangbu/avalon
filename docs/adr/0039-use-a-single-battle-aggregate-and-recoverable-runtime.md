# 0039：使用单一 Battle 聚合与可恢复 Runtime

状态：已接受

## 决策

系统以 `Battle` 聚合和 `avalon.battle.v1` 契约承载 PvE 与 PvP 战斗。当前版本只支持新空库基线。

Battle 使用 `mode=pvp|pve` 与 `source_type=challenge|training|encounter` 表达来源，生命周期固定为 `created → preview → running → completed|canceled|interrupted`。

Server 以 PostgreSQL Runtime Lease 和 fencing token 独占承载 Running Battle。Worker 为缺少有效 Lease 的 Battle 创建最多五次恢复尝试；Server 原子领取后，从初始状态、连续 Turn Record 和与 `state_version` 同事务保存的 Random Source 快照恢复。五次失败后以 `recovery_exhausted` 中断，不产生胜负。

所有 Runtime 状态写入、回合超时和故障中断都必须在事务内校验当前 holder、未过期 Lease 与 fencing token。恢复尝试领取超过一分钟未完成时允许另一 Server 原子重领，完成操作必须匹配当前 `claimed_by`。尚未启动 Runtime 的参与者可以明确取消 Battle；取消与账号占用释放、权威摘要和 Outbox 在同一事务提交。

管理员只获得 Battle 生命周期、Participant、Lease、恢复尝试和待发布 Outbox 的只读运维视图；不提供修改结果的接口或 UI。

## 后果

- Runtime 恢复只承诺已提交状态，进程崩溃时尚未提交的秘密选择由客户端使用幂等键重试。
- Battle 历史、分析和回放使用同一份权威事实。
