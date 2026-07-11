---
name: kotlin
description: "Kotlin development in the Avalon backend. Use when creating, changing, reviewing, or testing production or test .kt source, Kotlin types, nullability, sealed or value types, extension functions, source organization, KDoc, or Kotlin-Java interoperability."
---

# Kotlin

## 修改前

- 从 `gradle/libs.versions.toml` 确认 Kotlin 版本，从根 `build.gradle.kts` 确认 JVM toolchain 与编译参数。
- 涉及领域词汇时先读 `CONTEXT.md`；涉及已记录取舍时读对应 `docs/adr/`。
- 只有版本敏感语法无法由本地编译器和源码确认时，才查询对应版本的 Kotlin 官方文档。

## 类型与空安全

- 用类型表达不变量；默认使用非空类型、`val` 和不可变集合。
- 在输入、外部响应或真实可缺失字段处使用 nullable；不要用空字符串、`0` 或 `-1` 表示缺失。
- 不使用 `!!`，除非框架契约无法由类型系统表达，并且同一边界已有显式校验和测试。
- 为稳定小集合使用 enum；为携带状态或行为的封闭分支使用 sealed hierarchy。
- 涉及 Spring、Jimmer、Jackson 或测试 API 时，同时加载对应技术栈 skill。

## 源码组织

- 生产源码与测试源码遵循同一组织约束；每个 Kotlin 文件最多声明一个顶层类型，包括 `class`、`interface`、`data class`、`enum class`、sealed hierarchy、`value class`、`annotation class` 和 `object`；文件名与该类型名一致。
- DTO、模型和测试 helper 按类型拆成独立文件；不创建 `*Dtos.kt`、`*Models.kt` 等多类型聚合文件。
- 嵌套类型和 `companion object` 保留在所属类型内部，不单独拆文件；除 `companion object` 外，不声明普通 `object` 或 `data object`。
- 顶层函数、扩展函数和常量放入以领域职责命名的文件；不创建 `Utils.kt`、`Common.kt` 等含糊聚合文件。
- 共享常量使用顶层 `const val`/`val`；工厂、可变状态或可替换协作方使用普通类，并通过构造器或 Spring Bean 注入。

## 注释与 KDoc

- 源码注释、KDoc 和用户可见文案使用中文；代码标识符与协议字段保持英文。
- 注释只解释原因、领域约束、规则来源、协议边界与非显然取舍，不复述 Kotlin 语法或代码步骤。
- 公开类型、公开函数、扩展函数、复杂领域模型和跨模块接口必须检查并补充必要 KDoc；公共基础设施的 KDoc 说明模块边界、框架契约和预期接入方式。
- 每个新增或修改的测试类、共享 test fixture、Testcontainers 支撑类型及 mock、fake、handler 补充中文 KDoc，说明受测边界、资源生命周期或替身存在的原因。
- 函数内部只在复杂规则分支、事务边界、数据清洗、外部数据兼容或性能取舍处使用简短 `//` 注释。
- 单个 `@Test` 方法由清晰测试名表达行为时无需重复 KDoc；存在非显然前置条件时，用中文 KDoc 或短注释说明 fixture、随机数、时钟、并发或外部替身的原因，不写复述步骤的 Arrange/Act/Assert 注释。
- 修改带注释的代码时同步更新注释；删除过期注释和注释掉的代码。
- 注释不能替代清晰命名、类型设计、函数拆分或测试。

## 测试驱动

- 新行为和缺陷修复先写能失败的聚焦测试。
- 重构先用 characterization test 锁定外部行为。
- 纯类型或可见性调整至少先获得编译失败或调用点测试证据。

## 完成标准

- 受影响模块的 Kotlin 编译通过。
- 聚焦测试覆盖正常、空值和失败边界；公共 API 变更同时覆盖调用点。
- 每个 Kotlin 文件最多一个顶层类型，普通 `object` 已改为类、顶层声明或 `companion object`。
- 必要 KDoc 已覆盖公共边界、测试类与共享测试基础设施；没有过期、复述实现或与代码不符的注释。
- 没有新增平台类型扩散、未校验 `!!` 或含糊状态字符串。
- 跨模块 Kotlin 改动运行受影响模块测试；提交前按风险运行完整 `test`。
