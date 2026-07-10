# Identifier 与 JSON

当 Jimmer 主键、外键 DTO、Jackson 或 OpenAPI Identifier 发生变化时读取。

- 数据库存储和 Kotlin 实体使用 `Long/bigint`。
- Jimmer 实体 id 使用 `@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)`。
- 响应 DTO 的 `id`、`...Id` 与集合 Identifier 使用 `@JsonConverter(LongToStringConverter::class)`。
- 不把 Long 手工 `toString()` 后写入字符串 DTO；由 Jimmer/Jackson 边界转换。
- 请求中的 Identifier 按后端 DTO 契约绑定，校验存在性与所属关系。
- 普通数量、伤害值、时间戳和持续时间不因类型同为 Long 而转成字符串。
- 同步用序列化测试和 OpenAPI 正反断言锁定该边界。
