---
name: jackson-3
description: "Jackson 3 JSON work in Avalon. Use when changing ObjectMapper or JsonMapper configuration, JSON serialization or deserialization, JsonProperty annotations, converters, TypeReference, tree models, Jimmer immutable serialization, identifier JSON shape, or Jackson migration code."
---

# Jackson 3

## 修改前

- 确认当前依赖来自 `tools.jackson`，并检查 Spring Boot 是否已经提供配置好的 `ObjectMapper`。
- 涉及 Jimmer immutable 或 Identifier 时同时加载 `jimmer`；涉及 API schema 时同时加载 `springdoc-openapi`。

## 项目要求

- databind、tree model 与 `TypeReference` 统一使用 Jackson 3 的 `tools.jackson` API。
- 不引入 `com.fasterxml.jackson.databind`、旧 core TypeReference 或 Jackson 2 Kotlin module。
- `com.fasterxml.jackson.annotation` 仅作为 Jackson 3 共享注解包使用。
- Spring Bean 优先注入 Boot 配置的 mapper；只在隔离模块或测试确有边界时创建局部 `JsonMapper`。
- 局部 mapper 处理 Jimmer immutable 时注册 `ImmutableModuleV3`，不得使用 V2。
- Identifier 在 JSON 中保持字符串；普通计数、时间戳和度量 Long 保持数字。
- 自定义 converter、module 或 schema 修正器必须说明适用边界，不能全局误改同类型普通数值。

## 测试驱动

- 先写精确 JSON 树或 round-trip 失败测试，再调整 mapper。
- 同时覆盖缺失字段、显式 null、未知字段和 Identifier 精度边界。

## 完成标准

- 序列化与反序列化测试使用生产等价 mapper。
- 没有新增 Jackson 2 databind 引用或手工字符串拼 JSON。
- Jimmer immutable、nullable 与 Identifier 形态均有回归断言。
- API JSON 变化同步运行 Springdoc 契约测试。
