# Use a three-state Battle Session lifecycle

Battle Session 创建时在单所有者 Session Runtime 中完成输入校验和引擎启动，成功后直接成为 `ACTIVE`；引擎自然产生结果时成为 `COMPLETED`；管理员或未来 Match 可以将仍在运行的会话置为 `TERMINATED`，但终止不会伪造 `BattleResult` 或替 Match 判断真人胜负。Turn Command 的校验或执行失败不会提交内存状态变更，Session 保持 `ACTIVE`。首版不引入 `DRAFT`、`READY`、`PAUSED` 或 `FAILED`；承载节点丢失会销毁 Session，并由外部 Match 表达 `INTERRUPTED`。
