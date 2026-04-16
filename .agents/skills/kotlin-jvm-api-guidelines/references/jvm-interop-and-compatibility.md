# JVM 互操作与兼容性

在 `avalon` 当前基线下，纯 Kotlin 源码默认按 Kotlin-only 调用面处理。
不要因为运行在 JVM 上，就默认追加面向 Java 的兼容负担。

## 1. 先问一个问题

这段 API 是只给 Kotlin 调用，还是还要给 Java 调用？

- 只给 Kotlin 调用时，优先保持 Kotlin-first 设计。
- 需要给 Java 调用时，再有意识地补 `@Jvm*` 注解和互操作包装。
- 如果仓库内没有明确的 Java 调用入口、Java 模块或对外 Java SDK 约束，就不要把 Java 兼容当默认要求。

## 2. 常见 JVM 互操作决策

- 顶层函数在 JVM 上会编译成静态方法；因此文件名和 `@file:JvmName` 会影响 Java 侧入口名。
- `@JvmStatic` 只在你明确希望 Java 侧以静态方式调用 object / companion 成员时再加。
- `@JvmOverloads` 只在 Java 调用方确实需要重载桥接时再加；不要为了“也许以后会用”就滥用。
- `@JvmName` 只在需要修正 JVM 签名冲突或改进 Java 可读性时使用。
- `@Throws` 只在你希望向 Java 明确暴露受检异常契约时添加。

## 3. interface default methods

- Kotlin/JVM 中 interface 函数默认会编译为 JVM default methods。
- 与旧配置兼容时，要注意 `-jvm-default` 相关策略。
- Kotlin 官方文档已明确 `-jvm-default` 取代了过期的 `-Xjvm-default`；新配置不要继续使用旧选项。

## 4. explicit API mode

- 对库、共享模块和稳定公共 API，优先启用 explicit API mode。
- 它会强制公共声明显式写 visibility 和返回类型，减少无意暴露和推断漂移。
- 如果当前还不适合全面启用，也至少对关键公共模块按 explicit API 思维写代码。
- 这里强调的是 Kotlin 公共 API 的稳定性，而不是额外照顾 Java 调用习惯。

## 5. API 演进与兼容性

- 对公共 API，不要随意改函数签名、参数顺序、返回类型和层级结构。
- Kotlin 的默认参数对 Kotlin 调用体验很好，但对稳定 API 仍不等价于“可随便加参数”。
- 如果一个入口未来很可能继续扩展，优先考虑配置对象或 DSL。
- 需要弃用时，优先用 `@Deprecated` 和清晰迁移路径渐进收口，而不是直接硬删。
- 实验性或尚未稳定的 API，优先用 opt-in 机制，而不是假装稳定。
