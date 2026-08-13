---
status: accepted
---

# 冻结 Battle 输入并恢复原始遭遇

Pending Encounter、Challenge 和 Battle 创建使用显式幂等身份；Encounter 一旦由移动事务产生，就保存 Encounter Table 版本、Entry、随机种子、抽样轨迹、Creature/Form/等级结果和必要 Projection。玩家接受 PvE 或双方接受 PvP 时，服务端校验资格并冻结 Battle Snapshot，之后同一 PlayerCharacter 的世界移动被锁定，实时 Team、Creature Projection 和资料更新不能改写战斗输入。PvE Snapshot 还必须冻结接受时校验的 Party、Bot 稳定代码、策略版本与规范配置摘要。

Battle 或 Encounter 创建、执行、恢复失败时保留原始记录并按幂等身份重试；Battle 重启从最后已提交的状态版本恢复，有界重试仍失败才进入 `interrupted`；不得删除、重新抽样、自动判胜或由客户端提交替代结果。Encounter 的终态包含取消、过期和已解决；Battle 完成与 Inventory、Currency、Owned Creature 等奖励权威事实在同一事务中依据冻结输入结算，地图层不隐式捕获 Creature。

Pending Encounter 玩家侧只提供查询、接受和取消；同一 PlayerCharacter 同时最多一个待处理事件。Checkpoint 只能在当前位置按资料条件显式设置，不提供任意传送。Battle、Traversal 和奖励事务产生的通知与分析通过提交后的 Outbox 投递。

Battle Participant 与 Party 成员使用关系行保存稳定身份，规范化 JSONB Battle Snapshot 保存 schema version、SHA-256 摘要、冻结投影和 Random Source；无法规范化或超过上限时拒绝创建，不截断。Battle 完成时在同一事务写入参战 Owned Creature 的 HP、经验、成长与奖励流水；取消、过期和中断不写胜利成长。Checkpoint 只在服务端标记可恢复的失败/死亡流程中使用，首期没有 `Teleport` 或任意 `RecoverNow` 命令。

该决策优先保证断线重试、服务重启和审计重放的一致性，代价是需要额外的持久化状态、版本校验和 Battle/Encounter 生命周期管理。
