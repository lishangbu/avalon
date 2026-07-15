# Retain Match History without user deletion

首版永久保留 `COMPLETED` 与 `INTERRUPTED` Match 及对应 Trainer Team Snapshot；只有 `COMPLETED` Match 拥有 Match Result，`INTERRUPTED` Match 只保存 interruption reason。用户不能删除历史，Trainer 归档或账户停用也不移除历史事实。列表返回对手开局 displayName、状态、结果或中断原因、开始结束时间和最终回合数，详情增加双方公开 Team Preview、己方完整 Snapshot 与对方 Disclosure Ledger，不提供逐回合日志或回放。有效 Trainer 的历史由当前 Trainer Session 查询；归档 Trainer 无法进入游戏，但所属账户可通过 Sa-Token 账户级只读入口查询，并严格按该 Trainer 视角投影。

