---
name: kotlin-best-practices
description: "Use when implementing, reviewing, or refactoring Kotlin code, Kotlin tests, Gradle Kotlin DSL, coroutine code, null-safety-heavy code, public APIs, Kotlin object declarations, top-level type file organization, or idiomatic Kotlin style in avalon; especially when choosing between data/value/sealed classes, scope functions, extension functions, collection operations, nullable types, suspend functions, or coroutine error handling."
---

# Kotlin 最佳实践

## 核心原则

以 Kotlin 官方文档为默认依据；Avalon 的模块边界、去品牌化命名、Jimmer/Spring/Liquibase/战斗规则等项目约束优先于通用 Kotlin 建议。

## 工作方式

1. 先判断是否还需要加载项目技能：业务、持久化、迁移、安全、战斗、注释等场景仍按 `workspace-feature-routing` 路由。
2. 先设计类型和边界，再写实现：用 Kotlin 类型系统表达不变量，不用注释、魔法字符串或布尔参数弥补模型含糊。
3. 修改公共 API、领域模型或异步逻辑时，配套补充聚焦测试；不要只靠编译通过证明行为正确。
4. Kotlin 写法以清晰为先。只有在调用点更清楚、错误面更小、测试仍直接时，才使用更“巧”的语言特性。

## 代码组织

- 遵循官方 coding conventions：包名小写，类名/接口名使用 UpperCamelCase，函数和属性使用 lowerCamelCase。
- 文件名优先匹配主要声明；纯扩展或工具函数文件使用能说明领域的名称，不使用 `Utils.kt`、`Common.kt` 这类空泛名称。
- 保持导入显式可读；避免通过通配导入隐藏依赖来源。
- 类头、主构造器参数、长参数列表和链式调用换行后保持稳定缩进，避免一次改动造成大面积格式噪声。
- 优先 `val` 和不可变集合；只有对象生命周期或算法确实需要变更时才使用 `var`/mutable collection。
- 每个 Kotlin 文件最多声明一个顶层类型；不要使用 `*Dtos.kt`、`*Models.kt` 等文件聚合多个 `data class`、`class`、`interface`、`enum` 或 `object`。
- 相关 DTO、模型或测试 helper 按类型名拆成独立文件，用包名表达归属；嵌套类型和 `companion object` 保留在所属类型内部。
- 不使用普通 Kotlin `object` 或 `data object` 聚合常量、函数、工厂或状态；唯一允许的是 `companion object`。
- 需要共享常量时使用顶层 `const val`/`val`，需要工厂或可替换协作方时使用普通 `class` 并通过构造器或 Spring Bean 注入。
- 审查 Kotlin 改动时，发现 `object Xxx` 声明或多顶层类型文件必须拆开；不要用 singleton 聚合器或聚合文件隐藏依赖边界。

## 类型建模

- 简单数据载体用 `data class`；需要受限取值或状态机时用 `sealed interface` / `sealed class`。
- 单字段且有明确领域语义的 ID、code、slug、version 可考虑 `@JvmInline value class`，但要先确认框架序列化、Jimmer、Spring 绑定是否支持。
- 表达“可能不存在”用 nullable 类型或明确结果类型；不要用空字符串、`0`、`-1` 代表缺失。
- 枚举适合稳定小集合；如果将来可能携带行为或分层状态，优先 sealed 层次。
- 扩展函数只用于提升调用点可读性或封装领域语言；不要把跨模块业务流程藏进基础类型扩展。

## 空安全

- 默认使用非空类型，把 nullable 限制在输入边界、外部系统响应和真实可缺失的字段上。
- 处理 nullable 优先用 safe call、Elvis、`let`、提前返回或明确异常。
- 不使用 `!!`，除非上游框架契约无法被类型系统表达，并且同一处代码已经用测试或显式校验覆盖。
- Java/Spring/Jimmer 等平台类型进入 Kotlin 边界时，尽早转换成明确的 nullable 或非空类型，避免平台类型在业务层扩散。

## Scope Functions

| 函数 | 上下文对象 | 返回值 | 推荐用途 |
| --- | --- | --- | --- |
| `let` | `it` | lambda 结果 | nullable 链、局部转换、限制变量作用域 |
| `run` | `this` | lambda 结果 | 对同一对象执行一组计算并返回结果 |
| `with` | `this` | lambda 结果 | 非扩展式地围绕已有对象计算 |
| `apply` | `this` | 对象本身 | 初始化或配置对象 |
| `also` | `it` | 对象本身 | 日志、校验、调试等不改变主链路的副作用 |

- 不嵌套多个 scope function；如果出现两个以上隐式 `this`/`it`，改成具名局部变量。
- 不为了“看起来 Kotlin”而使用 scope function。普通 `if`、局部变量和提前返回经常更清楚。

## 协程

- 保持结构化并发：优先使用调用方提供的 scope、`coroutineScope` 或 `supervisorScope`；不要在业务代码中引入 `GlobalScope`。
- 取消是协作式的。CPU 密集循环要检查 `isActive`、调用 `ensureActive()` 或 `yield()`。
- 不吞掉 `CancellationException`。捕获宽泛异常时先重新抛出取消异常，再处理业务异常。
- 需要子任务互不影响时使用 `supervisorScope` 或 `SupervisorJob`；默认 `coroutineScope` 中任一子任务失败会取消兄弟任务。
- `suspend` 函数内部不要阻塞线程；必须调用阻塞 IO 时，显式切到合适 dispatcher 或使用框架提供的异步 API。

## 测试与审查

- 测试名称说明行为和边界，不重复实现细节。
- 对 sealed 分支、nullable 输入、异常路径、取消路径和集合为空/单个/多个元素的情况做最小覆盖。
- 审查 Kotlin 代码时优先找这些问题：`!!`、平台类型扩散、可变共享状态、嵌套 scope function、未处理取消、非穷尽状态建模、过宽的扩展函数。
- 公共 API 变更时检查二进制/JVM 互操作影响，例如默认参数、value class、internal/public 可见性、反射和序列化。

## 常见错误

- 为了减少行数牺牲可读性。
- 用 `Any`、`Map<String, Any?>` 或字符串状态绕过类型建模。
- 把副作用藏进 collection transform、scope function 或扩展属性。
- 在 `catch (e: Exception)` 中吞掉协程取消。
- 把 Java 平台类型直接传遍业务层。
- 在框架实体、DTO 和领域模型之间复用同一个类型，导致边界含糊。

## 官方依据

需要追溯官方来源时读取 `references/official-kotlin-sources.md`。
