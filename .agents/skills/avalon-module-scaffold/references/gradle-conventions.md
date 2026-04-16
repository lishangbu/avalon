# Gradle 约定

## 1. 构建工具

所有模块统一使用 `Gradle Kotlin DSL`。

## 2. 上下文模块基线

一个 bounded context 模块通常需要：

- Quarkus 平台依赖管理
- 选定 JVM 语言的语言插件
- 面向 `suspend` 应用层和仓储边界的 Kotlin coroutine 支持
- 面向数据库访问的 Vert.x Reactive SQL Client 依赖
- 以集成优先为主的测试依赖

保持模块依赖列表收敛：

- 只有在上下文需要稳定领域抽象时，才依赖 `shared-kernel`
- 只有在上下文需要跨上下文稳定应用契约时，才依赖 `shared-application`
- 只有在使用共享技术底座时，才依赖 `shared-infra`
- 除非属于显式的应用层契约，否则避免直接依赖另一个上下文模块

`shared-application` 模块基线：

- 使用 `java-library` 与 Kotlin JVM 插件
- 默认不引入 Quarkus 平台、REST、Reactive SQL Client、Redis 或 scheduler 依赖
- 包名使用 `io.github.lishangbu.avalon.shared.application.*`

`shared-infra` 可以依赖 `shared-application` 来实现应用层端口；如果公开类的 ABI 暴露了应用层契约，Gradle 可使用
`api(project(":modules:shared-application"))`。

## 3. App 模块职责

`app` 模块应该：

- 组装 Quarkus 运行时 wiring
- 暴露最终可运行应用
- 避免变成领域逻辑的归宿

## 4. 评审清单

1. 模块是否只暴露其他模块真正需要的内容
2. 上下文依赖是否显式，而不是意外产生的传递泄漏
3. `app` 模块是否在做 wiring，而不是持有业务逻辑
