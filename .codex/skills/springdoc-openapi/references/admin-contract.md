# Admin API Contract

当 admin OpenAPI 集合、schema 或前端 Contract Snapshot 变化时读取。

- 权威端点是后端生成的 `/v3/api-docs/admin`。
- 先通过后端文档测试证明运行时 DTO 与 schema，再交接前端同步。
- 对每个变更断言路径、HTTP 方法、请求体、响应体、required、nullable 和 Identifier。
- 分页包装保持统一；错误响应保持稳定且不泄密。
- 前端的 `openapi.json` 和 `schema.d.ts` 是消费者快照，不反向定义后端。
- 两个仓库分别验证、分别提交；不要求共享 commit。
