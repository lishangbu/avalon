# Centralize turn completeness in Battle Session

Battle Session 根据当前权威 `BattleState` 派生并公开 Turn Requirements，再用同一结果校验 Turn Command 是否包含全部且仅包含必要的人工选择。锁招、蓄力等自动行动由引擎规划，不要求调用方重复提交；缺少必要选择或包含多余选择的命令在进入引擎前被拒绝，且不会推进 revision。Turn Requirements 只负责人工选择集合的完整性；两个席位换入同一成员等跨行动组合约束由共享 Battle Action Validator 在同一临界区内校验，并返回稳定 violation code。底层 Battle Engine 继续保持可复用的宽松执行契约，管理端和未来 Match 都不得各自实现一套完整性或组合校验规则。
