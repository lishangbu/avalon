# Make backend OpenAPI the admin contract authority

Avalon 后端生成的 admin OpenAPI 是管理接口的唯一契约权威，后端负责 required、nullability、Identifier 和路径参数的准确性。管理端从该契约生成类型而不另写 DTO；这减少了两个独立仓库之间的模型漂移，但要求后端契约测试先于前端 Contract Snapshot 同步完成。
