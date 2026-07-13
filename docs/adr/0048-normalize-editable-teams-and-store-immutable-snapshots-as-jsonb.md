# Normalize editable Teams and store immutable Snapshots as JSONB

Trainer、唯一 Trainer Team、Team Member 与技能引用使用关系表，保证所有权、唯一性和 Current Game Data 外键校验；Challenge/Match 的不可变 Trainer Team Snapshot 与按双方视角保存的 Disclosure Ledger 使用服务端生成、带 `schemaVersion` 的 PostgreSQL `jsonb`。Challenge 状态、Match 生命周期、结果、参与 Trainer、时间和 revision 保持普通关系列以支持查询与加锁，客户端不能直接提交原始 Snapshot JSON。
