# Use revisions for player resource concurrency

Trainer、Trainer Team、Challenge 与 Match 使用单调递增 revision；Trainer displayName 不可修改，Trainer 的 revision 只保护归档、恢复等状态变化，其他聚合的更新、删除、接受、拒绝、取消和 Forfeit 同样携带 `expectedRevision`。不匹配统一返回 `409` 与稳定错误码；创建 Trainer 或 Challenge 使用客户端 `commandId` 防止重试产生重复记录。WebSocket 通知只携带最新 revision，Trainer Turn Submission 使用当前 Match revision，但单方锁定不推进 revision，避免泄露对方提交状态。
