---
name: source-comments
description: "Use when adding, reviewing, or refactoring Chinese source comments in avalon Kotlin code, and when implementing or changing public APIs, reusable infrastructure, Spring Boot configuration, Jimmer entities, Liquibase-related code, security/RBAC logic, tests, KDoc, or non-obvious domain algorithms."
---

# Kotlin 源码注释

## 使用范围

用于 `avalon` 后端 Kotlin 源码、测试、RBAC 规则、Jimmer 实体、Spring 配置和公共 API 注释。注释使用中文，代码标识符保持英文。

## 触发条件

- 用户明确要求“加注释”“源码注释”“KDoc”或要求以后默认补注释时，后续相关源码改动都必须使用本技能。
- 新增或修改公共 API、跨模块基础设施、框架扩展点、自动配置、Jimmer 实体、Repository、Spring Bean、领域规则模型或测试夹具时，必须检查是否需要 KDoc 或局部注释。
- 抽取 common 包、persistence 包、security/RBAC 基础设施、迁移相关支撑代码时，公共类型默认补 KDoc，除非该类型只在单个文件内部使用且命名已经完全自解释。
- 修改既有带注释的代码时，同步更新过期注释；发现注释与实现不一致时，优先修正注释或重命名/拆分代码。

## 核心原则

- 用户要求加注释后，不要只在最终说明里提到“可加注释”；应在实现中直接补充必要 KDoc/注释。
- 注释解释“为什么”、领域约束、规则来源和边界条件，不复述 Kotlin 语法。
- 公开类、公开函数、扩展函数、复杂领域模型和跨模块接口优先使用 KDoc。
- 公共基础设施类型的 KDoc 需要说明它服务的模块边界、框架契约和预期接入方式。
- 函数内部只在规则分支、事务边界、数据清洗、兼容源数据差异或性能权衡处写短行注释。
- 注释不得替代清晰命名、类型设计和测试。
- 不保留注释掉的代码；过期注释必须随代码一起更新或删除。
- 注释不得把源品牌词、游戏名、地区名和源世界观词汇带入业务边界。

## Kotlin 与 KDoc 规则

- KDoc 使用 `/** ... */`，第一段写一句清晰摘要。
- `@param`、`@return`、`@throws` 只在调用方需要额外语义时使用，不重复签名。
- 链接同模块声明时使用 KDoc 链接语法，例如 `[CatalogImportService]`。
- `//` 注释用于局部实现决策，保持一到两行。
- 测试中的注释只说明 fixture、随机数、时钟、并发或外部替身的原因。
- 复杂规则优先拆出具名函数，再用 KDoc 描述规则边界。

## 官方依据

- Kotlin Coding conventions: https://kotlinlang.org/docs/coding-conventions.html
- Kotlin KDoc: https://kotlinlang.org/docs/kotlin-doc.html

## 示例

```kotlin
/**
 * 同步内置 RBAC 权限数据。
 *
 * 同步过程只维护稳定权限 code，不根据运行时请求临时生成正式权限。
 */
class RbacPermissionSeeder {
	fun sync() {
		// 先校验权限 code，再在事务内写入角色绑定。
	}
}
```

## 常见错误

- `// 创建对象`、`// 调用方法` 这类重复代码的注释。
- KDoc 中列出每个字段但没有额外语义。
- 在业务层注释中重新引入已移除的目录、数据导入或战斗领域词。
- 注释说明“临时这么做”，但没有测试或 issue 约束。
- 因为注释已经解释复杂逻辑就不拆分函数。
