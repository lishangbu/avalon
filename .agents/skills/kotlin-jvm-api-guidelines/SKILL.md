---
name: kotlin-jvm-api-guidelines
description: 为 Kotlin/JVM 代码提供语法、惯用写法、文件组织与公共 API 设计约束。适用于编写、重构或审查 Kotlin/JVM 代码时决定 nullability、默认参数、扩展函数、顶层函数、sealed/data/value class、一个类一个文件还是按概念聚合、错误处理、KDoc，以及纯 Kotlin API 的稳定性约束；仅在明确存在 Java 调用方时考虑 Java 互操作。
---

# Kotlin/JVM 语法与 API 技能

## 概览

这个技能帮助你把 Kotlin/JVM 代码写得更符合 Kotlin 官方 API guidelines、KDoc 和 coding conventions。

它既适用于日常业务代码，也适用于会被多个模块、多个团队直接依赖的公共 API。越靠近公共边界，这个技能里的 API 设计、KDoc
写法和稳定性要求就越重要。

仓库里哪些单元必须补 KDoc、边界 DTO 该写到多细，以 `avalon-code-commenting` 技能为准；这里主要负责 Kotlin 语言层的写法、惯例和兼容性取舍。

## 何时使用

出现以下任一情况时，使用这个技能：

- 新增或重构 Kotlin/JVM 代码
- 设计或审查公共 Kotlin API
- 决定是否使用顶层函数、扩展函数、成员函数、DSL、默认参数、布尔参数
- 判断 Kotlin 代码应该一个类一个文件，还是按概念聚合到同一个文件
- 设计 sealed/data/value class、错误处理、nullability、集合返回类型
- 在明确存在 Java 调用方时处理互操作，例如 `@JvmStatic`、`@JvmName`、`@JvmOverloads`、`@Throws`
- 为公共 Kotlin 代码补 KDoc 或审查兼容性风险

## 使用流程

1. 先判断这段代码是内部实现，还是稳定公共 API；只有明确存在 Java 调用方时，再把它视为 Java-facing API。
2. 日常写法和语言结构选择，读 [references/syntax-and-idioms.md](references/syntax-and-idioms.md)。
3.
决定文件拆分还是聚合时，读 [references/file-organization-and-aggregation.md](references/file-organization-and-aggregation.md)。
4. 公共 API 设计与行为一致性，读 [references/api-surface-and-design.md](references/api-surface-and-design.md)。
5. 涉及明确的 Java 调用方或 API
   演进时，读 [references/jvm-interop-and-compatibility.md](references/jvm-interop-and-compatibility.md)。
6. 涉及 KDoc 写法、链接、示例和文档生成时，读 [references/kdoc-and-documentation.md](references/kdoc-and-documentation.md)。
7. 收尾前用 [references/review-checklist.md](references/review-checklist.md) 复查。
8. 如果需要追溯来源或继续展开原始资料，读 [references/source-basis.md](references/source-basis.md)。

## 核心原则

- 优先降低 mental complexity，让调用方更容易读懂、猜对和调试。
- 内部实现可以利用 Kotlin 的简洁性；公共 API 要更显式、更稳定、更可演进。
- 优先复用 Kotlin 现有概念和标准类型，不要随意发明近似抽象。
- 先把核心概念做小做稳，再在其上扩展便利 API。
- 在 `avalon` 中，公开顶层类型默认按“一个类型一个文件”组织；`companion object` 不单独计为额外类型。
- 文件组织默认服务“按概念阅读”，而不是为了图省事把不相关声明堆进 `Models.kt`、`Dtos.kt`、`Utils.kt`。
- 默认优先保持 Kotlin-first 设计；只有明确存在 Java 调用方时，才额外考虑 Java 调用体验和互操作包装。
