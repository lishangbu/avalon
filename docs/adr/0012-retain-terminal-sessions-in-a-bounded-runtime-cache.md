# Retain terminal Sessions in a bounded Runtime cache

`ACTIVE` Battle Session 在 Session Runtime 中保留到自然完成、显式终止或承载节点丢失；`COMPLETED` 和 `TERMINATED` Session 成为 Recent Session，默认继续保留十五分钟以供客户端确认结果和管理端查看。终态缓存同时受可配置数量上限约束，达到上限时按 `endedAt` 最早者提前淘汰，结束时间相同时以 `sessionId` 作为稳定 tie-break。Session API 只查询活跃和近期会话，淘汰后返回 404；首版 Battle Record 随 Recent Session 一并淘汰，未来永久真人结果属于 Match，历史归档属于独立 Battle Record sink。
