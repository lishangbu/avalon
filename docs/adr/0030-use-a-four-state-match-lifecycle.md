# Use a four-state Match lifecycle

Match 使用 `STARTING`、`ACTIVE`、`COMPLETED` 和 `INTERRUPTED` 四种持久状态，合法转换只有 `STARTING → ACTIVE | INTERRUPTED` 与 `ACTIVE → COMPLETED | INTERRUPTED`，两个终态都不可恢复。接受 Challenge 的事务先创建 `STARTING` Match、锁定双方 Snapshot、接受当前 Challenge 并取代其他邀请，提交后同步创建 Battle Session 与 Match Runtime；成功持久化 `ACTIVE` 并直接返回 Match View，失败持久化 `INTERRUPTED / START_FAILED` 并返回稳定错误与 `matchId`。`STARTING` 只承接数据库与内存 Runtime 无法原子提交的内部短暂边界，正常客户端不轮询该状态。
