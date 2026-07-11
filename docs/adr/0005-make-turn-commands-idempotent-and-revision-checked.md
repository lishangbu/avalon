# Make turn commands idempotent and revision checked

每个 Turn Command 必须携带 `commandId` 和 `expectedRevision`。Session Runtime 以会话内的 command 索引识别重试并返回该命令的原始结果，以 `expectedRevision` 拒绝基于过期状态的推进；索引只保留原命令与对应 Turn Record，不为每个命令复制当时的历史前缀快照。重试最后一个回合时直接组合当前快照，重试更早回合时从初始状态和 Turn Record 前缀按 Random Trace 严格重放并按需重建原始结果，且不会回退当前 Session。单所有者命令队列串行完成校验、战斗执行、Turn Record 追加和 revision 递增，不依赖数据库事务或分布式锁。节点丢失时整个 Session 失效且对应 Match 进入 INTERRUPTED，因此不会在缺少幂等索引的新 Runtime 上继续旧会话。这让当前管理端和未来实时网关共用稳定的命令语义，而不会在活跃热路径引入持久化 I/O 或随回合数平方增长的历史快照保留。
