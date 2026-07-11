# Keep active Battle Sessions in memory

活跃 Battle Session 由单个 Session Runtime 在内存中独占并串行执行，不创建 battle_session、battle_session_side 或 battle_session_turn 表，也不在 Turn Command 或未来实时 tick 的热路径同步写关系数据库。Runtime 只保留一份当前权威 Session Snapshot；每个 Turn Record 保留行动、Random Trace 与事件增量而不复制回合后状态，历史快照需要时从初始状态严格重放。当前实现通过存储与路由接口使用进程内 Runtime；未来可以按 sessionId 把 Runtime 分片到游戏节点，并把 Turn Record 或实时事件异步批量归档为 Battle Record。持久化 Match 只保存玩家、配对、生命周期和最终结果，不承担战斗运行态；这一边界以节点故障时中断 Match 为代价，换取稳定的低延迟执行路径。
