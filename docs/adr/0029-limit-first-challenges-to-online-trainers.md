# Limit first Challenges to online Trainers

首版只能向当前持有有效 Trainer Session 的在线 Trainer 发起 Challenge，不提供离线邀请箱、推送通知或跨天待办。Challenge 默认有效五分钟且允许服务端配置，创建时按数据库时间固定 `expiresAt`，断线、重连或重复查询都不续期；接受时必须重新确认双方 Trainer 同时在线、账户均无 Active Match，任一方暂时离线或已切换 Trainer 时保持 `PENDING` 并允许期限内重试，超过期限则转为 `EXPIRED`。
