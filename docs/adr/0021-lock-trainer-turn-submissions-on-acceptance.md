# Lock Trainer Turn Submissions on acceptance

Match 对每个 Trainer、每个回合只接受一份完整行动，接受后立即锁定；请求携带 `submissionId` 与双方共享的 `expectedRevision`，单方锁定不推进公开 revision，相同 ID 与内容可幂等重试，相同 ID 携带不同内容或旧 revision 都返回冲突。Trainer 在锁定自己的行动前不能得知对方是否已经提交，任何时候都不能读取对方的行动内容、提交时间或请求标识；双方提交都锁定后，由服务端生成 Battle Session commandId，并按稳定顺序发送一次完整 Turn Command。没有人工 Turn Requirements 的一方自动就绪，双方都无需选择时逐回合串行自动推进；连续计数在再次出现人工要求时归零，默认达到可配置上限一百仍未结束或产生人工要求时，以 `RUNTIME_FAILED` 中断 Match。
