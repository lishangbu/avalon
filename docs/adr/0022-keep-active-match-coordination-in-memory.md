# Keep active Match coordination in memory

持久 Match 保存双方 Trainer、阵营、生命周期、最终结果和已经完成回合产生的 Disclosure Ledger，不保存尚未凑齐的 Trainer Turn Submission、行动期限调度或幂等缓存；这些高频协调状态由单节点 Match Runtime 保存在内存中。由于 Battle Session 本身无法在 Runtime 丢失后恢复，持久化半边行动不能帮助续局，进程或 Runtime 丢失时应丢弃它们并将 Match 转为 `INTERRUPTED`。
