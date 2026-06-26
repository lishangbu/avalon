---
name: workspace-feature-routing
description: "Use when implementing, extending, reviewing, or deciding the next backend feature in the avalon repository, especially when the user says 继续, 下一步, 接下去, backend, database, security, RBAC, Jimmer, Liquibase, comments, 注释, or asks what to do next."
---

# 后端功能路由

## 使用范围

这是 `avalon` 后端仓库的入口技能。遇到后端实现、评审、下一步判断或弱指令时，先用本技能判断需要加载哪些领域技能，再读代码或修改文件。

弱指令包括：`继续`、`下一步`、`接下去`、`1`、`2`、`3`、`continue`、`next`。

## 仓库边界

- 本技能只适用于 `avalon` 后端仓库。
- 后端采用 Gradle 多模块模块化单体。
- 没有用户明确要求提交时，不执行 `git commit`。
- 修改文件前先完成技能路由，不要先改代码再补读技能。

## 路由表

| 触发内容 | 必须加载的项目技能 |
| --- | --- |
| Spring Boot、Controller、配置、Web、测试切片、应用启动 | `spring-boot-4` |
| Jimmer、实体映射、Repository、查询、事务、存储层 | `jimmer-persistence` |
| `db/changelog`、表、字段、索引、约束、迁移验证 | `liquibase` |
| 授权服务器、角色、权限、登录态、接口鉴权 | `security-rbac` |
| Kotlin 类型建模、对象声明、顶层类型文件组织、惯用写法、测试风格 | `kotlin-best-practices` |
| Kotlin、测试、公共 API、领域规则源码注释、中文注释 | `source-comments` |
| 提交说明、实际提交、拆分提交 | `commit` |

命中多个领域时全部读取。例如安全表结构变更同时使用 `security-rbac`、`jimmer-persistence` 和 `liquibase`。

## 模块定位

- `app`：启动和装配。
- `common-persistence`：Jimmer 与 CosId 共享持久化基础设施。
- `migration`：Liquibase 迁移和迁移验证。
- `security`：授权服务器、资源服务器、认证主体和 RBAC 运行时。
- `system`：用户、角色、权限、OAuth client 和 JWK 等系统管理 API。

## 完成前检查

- 是否根据最新指令完成技能路由？
- 是否读取了所有命中的项目技能？
- 是否避免重新引入目录、数据导入或战斗领域命名？
- 新增或修改源码注释时是否使用中文并符合 Kotlin/KDoc 约束？
- 是否运行了与改动匹配的最小验证？
