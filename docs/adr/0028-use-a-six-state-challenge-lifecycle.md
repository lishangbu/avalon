# Use a six-state Challenge lifecycle

Challenge 使用 `PENDING`、`ACCEPTED`、`REJECTED`、`CANCELLED`、`EXPIRED` 和 `SUPERSEDED` 六种状态；只有 `PENDING` 可以变化，接受由目标 Trainer 触发并创建 Match，拒绝只由目标主动触发，过期与取代由系统触发。`CANCELLED` 只允许 `WITHDRAWN`、`TRAINER_ARCHIVED`、`ROSTER_INVALIDATED` 三个稳定原因，不接受自由文本；所有终态不可再次接受，重复请求只能返回同一幂等结果或明确冲突。
