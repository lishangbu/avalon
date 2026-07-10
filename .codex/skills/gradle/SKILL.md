---
name: gradle
description: "Gradle build work in the Avalon backend. Use when editing build.gradle.kts, settings.gradle.kts, gradle.properties, Gradle wrapper files, version catalogs, plugins, dependency scopes, module inclusion, Java toolchains, KSP, or build and test tasks."
---

# Gradle

## 修改前

- 从 `gradle/libs.versions.toml`、根 `build.gradle.kts`、`settings.gradle.kts` 和目标模块构建文件确认当前配置。
- 使用仓库 Gradle Wrapper；不要依赖系统 Gradle。
- 先运行能暴露现有问题的最小 Gradle task，区分基线失败与本轮失败。

## 项目要求

- 所有依赖版本集中在 version catalog；不在模块构建文件散落版本号，不使用动态版本。
- 根工程统一 Kotlin/JVM、dependency-management、Java 25 toolchain 和 JUnit Platform。
- 只有 `app` 应用 Spring Boot 可运行插件；业务与基础设施模块保持 library。
- 仅在需要 Jimmer 代码生成的模块应用 KSP，并使用 catalog 中的 Jimmer KSP 依赖。
- 用 `api` 暴露真实公共类型，用 `implementation` 隐藏实现依赖；避免跨模块循环依赖。
- 不顺手升级插件、JDK 或依赖；只有用户明确要求或当前任务无法在既有版本完成时才升级。
- 新模块必须有单一职责、明确依赖方向，并加入 `settings.gradle.kts` 与相应验证。

## 变更验证

- 依赖或插件变更先运行受影响模块的 `dependencies`、`compileKotlin` 或目标测试 task。
- version catalog、根插件、toolchain 或 settings 变更运行 `projects` 和完整 `test`。
- 构建缓存、并行或内存参数变更记录可复现的前后证据，不凭感觉调整。

## 完成标准

- Wrapper 能解析所有项目与 catalog alias。
- 受影响模块可编译、测试且没有未使用或重复依赖。
- lock/version 变更只包含已授权升级。
- Windows 使用 `.\gradlew.bat`，Unix 使用 `./gradlew`；报告实际执行的 task。
