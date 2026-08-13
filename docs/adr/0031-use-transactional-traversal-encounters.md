---
status: accepted
---

# 使用事务化的移动触发遭遇

成功的 Location Traversal 在同一 PostgreSQL 事务中更新 Player Position、移动序号和 Map Discovery，并依据目标 Location 的 `walk` Encounter Table 执行一次服务端随机判定；命中时同时创建 Pending Encounter，未命中时不创建空记录。移动不提供独立 Exploration 命令，Encounter 产生后必须先接受、放弃或过期，才能继续移动。

Encounter Table 的基础触发概率使用整数万分比，冷却使用移动步数，可选次数上限使用正整数；冷却与次数按 PlayerCharacter 和 Encounter Table 的组合独立记录。Pending Encounter 保存随机种子、抽样轨迹和最终结果摘要，经过玩家显式接受后进入 PvE Battle；PvP Battle 不由移动或位置重合自动触发，必须经过参与者显式同意。

Encounter Table 使用 `walk` 方法。随机源使用版本化 `hmac-sha256-v1`：服务端 CSPRNG 生成 32 字节 seed，再按用途和 draw 序号派生均匀整数；算法、seed、用途、序号和结果全部写入 Encounter 事实，重试不得重新抽样。

Pending Encounter 有独立的服务端固定十分钟期限；玩家可以显式取消一次，取消或过期都解除移动阻塞且不重抽样。逃跑只属于 Battle Engine 命令，Battle 终局后将 Encounter 标记为 `resolved`。Encounter 结果在 Traversal 事务中冻结 Encounter Table 版本、Entry、随机种子/轨迹、Creature/Form/等级结果及必要 Projection；接受时只冻结己方 Party Snapshot，不重新读取表。

出口条件和 Traversal Effect 使用后端闭集规则原语，并在同一事务内执行白名单状态变化。缺失或停用的条件引用按 fail-closed 处理，返回稳定原因码，不执行副作用。任何条件求值、Traversal Effect、位置、移动序号、Map Discovery、Encounter 进度或 Pending Encounter 创建失败，整个 Location Traversal 都回滚，重试继续使用同一幂等身份和随机结果。

服务端在创建 PlayerCharacter 时选择默认启用出生 Location，并原子写入位置序号 `0` 与首次 Discovery；没有合法出生点时拒绝创建或资料发布。稳定 Location、Exit 和 Checkpoint 不物理删除；停用目标拒绝穿越，位于停用 Location 的角色保留位置但禁止移动，由受控维护操作处理。

Traversal 请求必须携带出口身份、期望 Player Position 版本和幂等身份；服务端确认出口来源等于当前位置后才执行。PlayerCharacter、出口和命令作用域内的重复请求只返回首次响应。核心事务提交后写入可靠 Outbox 事件，通知、分析和异步恢复不得在事务内调用外部服务。

该决策牺牲了客户端自由触发遭遇和并行移动的简单性，换取位置、探索、随机结果和遭遇生命周期的一致性、幂等重试能力与可审计重放。
