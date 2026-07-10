---
name: springdoc-openapi
description: "Springdoc OpenAPI work in Avalon. Use when changing OpenAPI groups, /v3/api-docs/admin, schema generation, required or nullable properties, identifier formats, path parameters, operation metadata, customizers, generated admin contracts, or OpenAPI documentation tests."
---

# Springdoc OpenAPI

## 修改前

- 先读 `docs/adr/0003-admin-openapi-authority.md` 与 `CONTEXT.md` 中的 Admin API Contract。
- 检查 Controller、请求/响应 DTO、Jackson/Jimmer 注解和现有 OpenAPI customizer。
- 契约同步细节读取 [references/admin-contract.md](references/admin-contract.md)。

## 项目要求

- 后端生成的 admin OpenAPI 是唯一契约权威；不得为迎合手写前端 DTO 而伪造 schema。
- 非空响应属性进入 `required`；真实 nullable 属性不得被误标为 required。
- Identifier、`*_id`、`*Id` 和相应路径参数使用 JSON `string`；普通 Long 度量保持 `integer/int64`。
- 请求体、响应体、分页包装和错误结构与运行时 Jackson 输出一致。
- customizer 只修正可证明的 springdoc/Jimmer 边界，必须避免按宽泛类型全局改写。
- 管理端与公开 API 使用明确 group；内部端点不得意外进入 admin contract。
- operation 描述和用户可见说明使用中文，稳定字段与 code 保持英文。

## 测试驱动

- 先在 `OpenApiDocumentationTests` 添加失败断言，再改 DTO 或 customizer。
- 同时断言正例与反例，例如 Identifier 为字符串而普通计数仍为数字。
- 新集合端点覆盖路径、方法、请求 schema、响应 schema 和权限相关响应。

## 完成标准

- OpenAPI 文档测试通过，且实际序列化测试与 schema 一致。
- required/nullability、数组元素和路径 Identifier 无漂移。
- Contract Snapshot 变化只来自已确认后端契约。
- 影响前端时明确交接更新 `openapi.json` 与生成类型，但不跨仓库提交。
