# Interrupt Matches when Session Runtime is lost

Avalon 不为活跃 Battle Session 提供数据库恢复保证。承载 Session Runtime 的节点丢失时，该 Session 立即失效；未来 Match 将自身置为 `INTERRUPTED`，不确认胜负，并允许重新匹配或重新开始。Battle Record 只用于诊断和复盘；即使未来接入异步归档，它也不能恢复或接续一个已经失去内存幂等缓存与权威状态的 Session。若以后业务要求无损故障转移，应通过复制 Runtime 或持久命令日志新增可用性层，而不是让当前高频执行路径同步更新关系数据库。
