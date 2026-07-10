---
name: kotlin
description: "Kotlin development in the Avalon backend. Use when creating, changing, reviewing, or testing .kt source, Kotlin types, nullability, sealed or value types, extension functions, source organization, KDoc, or Kotlin-Java interoperability."
---

# Kotlin

## 修改前

- 从 `gradle/libs.versions.toml` 确认 Kotlin 版本，从根 `build.gradle.kts` 确认 JVM toolchain 与编译参数。
- 涉及领域词汇时先读 `CONTEXT.md`；涉及已记录取舍时读对应 `docs/adr/`。
- 只有版本敏感语法无法由本地编译器和源码确认时，才查询对应版本的 Kotlin 官方文档。

## 项目要求

- 用类型表达不变量；默认使用非空类型、`val` 和不可变集合。
- 在输入、外部响应或真实可缺失字段处使用 nullable；不要用空字符串、`0` 或 `-1` 表示缺失。
- 不使用 `!!`，除非框架契约无法由类型系统表达，并且同一边界已有显式校验和测试。
- 为稳定小集合使用 enum；为携带状态或行为的封闭分支使用 sealed hierarchy。
- 每个 Kotlin 文件只保留一个主要顶层类型；用领域名称命名扩展文件，不创建 `Utils.kt` 或 `Common.kt`。
- 不用普通 `object` 聚合可注入协作方、工厂或可变状态；常量使用顶层 `const val`，协作方使用类和构造器注入。
- 源码注释与 KDoc 使用中文，标识符保持英文；只解释原因、协议、边界与非显然约束。
- 涉及 Spring、Jimmer、Jackson 或测试 API 时，同时加载对应技术栈 skill。

## 测试驱动

- 新行为和缺陷修复先写能失败的聚焦测试。
- 重构先用 characterization test 锁定外部行为。
- 纯类型或可见性调整至少先获得编译失败或调用点测试证据。

## 完成标准

- 受影响模块的 Kotlin 编译通过。
- 聚焦测试覆盖正常、空值和失败边界；公共 API 变更同时覆盖调用点。
- 没有新增平台类型扩散、未校验 `!!`、含糊状态字符串或与实现不符的注释。
- 跨模块 Kotlin 改动运行受影响模块测试；提交前按风险运行完整 `test`。
