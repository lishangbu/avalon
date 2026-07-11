# Bound in-memory Session Runtime capacity

单个 Session Runtime 默认最多承载 1000 个 `ACTIVE` Battle Session 和 1000 个 Recent Session，两个上限均可配置，近期会话还受十五分钟 TTL 限制。达到活跃上限时，创建接口返回 `503`、稳定错误码 `battle-session.capacity-exhausted` 和 `Retry-After`，且绝不淘汰运行中会话；会话进入终态时立即释放活跃名额，近期缓存达到上限则按 `endedAt` 最早者淘汰，并以 `sessionId` 稳定打破并列。该容量指标未来可以直接供多节点 Session Router 选择承载节点。
