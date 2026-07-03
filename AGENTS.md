# Avalon 后端 Agent 指南

## 入口规则

在本仓库执行任何后端实现、评审、计划延续或“下一步”判断前，先读取：

```text
.codex/skills/workspace-feature-routing/SKILL.md
```

根据路由结果继续读取命中的项目技能。不要先修改文件再补读技能。

## 仓库边界

- 本仓库是 Avalon 后端仓库。
- 前端管理端在独立仓库 `avalon-admin-ui`。
- 后端采用 Gradle 多模块模块化单体，不拆微服务。
- 没有用户明确要求时，不执行 `git commit`。
- 允许修改本仓库文件；不要跨仓库提交或混合后端、前端改动。

## 技能路由

| 任务类型 | 必读技能 |
| --- | --- |
| Spring Boot、Controller、配置、CORS、应用启动 | `.codex/skills/spring-boot-4/SKILL.md` |
| Jimmer、实体映射、Repository、查询、事务 | `.codex/skills/jimmer-persistence/SKILL.md` |
| 数据库迁移、表、字段、索引、约束 | `.codex/skills/liquibase/SKILL.md` |
| 授权服务器、角色、权限、接口鉴权 | `.codex/skills/security-rbac/SKILL.md` |
| Kotlin 类型建模、对象声明、顶层类型文件组织、惯用写法、测试风格 | `.codex/skills/kotlin-best-practices/SKILL.md` |
| Kotlin、测试、公共 API、领域规则源码注释 | `.codex/skills/source-comments/SKILL.md` |
| 提交说明或实际提交 | `.codex/skills/commit/SKILL.md` |

命中多个领域时全部读取。

## 模块结构

目标模块结构：

```text
app
common-web
common-persistence
s3-core
s3-spring-boot-autoconfigure
s3-spring-boot-starter
migration
security
system
scheduler
game-data
battle-engine
battle-rules
```

模块职责：

- `app`：Spring Boot 启动、运行时装配、全局配置。
- `common-web`：分页请求、错误响应和全局异常处理等共享 Web 边界。
- `common-persistence`：Jimmer 与 CosId 的共享持久化基础设施。
- `s3-core`：S3 操作抽象、对象键、命令对象和 AWS SDK v2 适配。
- `s3-spring-boot-autoconfigure`：S3 Spring Boot 自动配置。
- `s3-spring-boot-starter`：S3 starter 对外依赖入口。
- `migration`：Liquibase 迁移和迁移验证。
- `security`：授权服务器、资源服务器、认证主体和 RBAC 运行时。
- `system`：用户、角色、权限、OAuth client 和 JWK 等系统管理 API。
- `scheduler`：Quartz 定时任务注册、调度、执行记录和持久化。
- `game-data`：游戏资料管理领域。
- `battle-engine`：战斗引擎核心规则和公开规则用例测试。
- `battle-rules`：战斗规则维护 API 和运行时规则快照。

## 依赖约束

- `security` 拥有安全实体、权限模型、授权配置和安全运行时测试。
- `system` 依赖 `security`，承接管理端写入、查询、校验和 DTO，不反向依赖业务模块。
- `common-persistence` 只放共享持久化基础设施，不放业务实体或默认权限数据。
- `migration` 是数据库结构事实来源。
- `app` 负责组合安全、系统管理、迁移和共享模块。

## RBAC 命名

业务代码、表、DTO、API 和用户可见文案围绕：

- `user`
- `role`
- `permission`
- `client`
- `token`
- `jwk`

不要重新引入目录、数据导入或战斗领域命名。

## 实现顺序

优先遵循 `docs/superpowers/plans` 中的计划：

1. `2026-06-22-security-rbac-sas-foundation.md`
2. `2026-06-20-backend-foundation.md`

如果用户给出更新指令，以最新用户指令为准，同时保持 RBAC 模块边界。

## 常用命令

从仓库根目录运行：

```bash
./gradlew help
./gradlew projects
./gradlew test
./gradlew clean test
./gradlew :app:bootRun
```

模块测试：

```bash
./gradlew :app:test
./gradlew :common-web:test
./gradlew :common-persistence:test
./gradlew :s3-core:test
./gradlew :s3-spring-boot-autoconfigure:test
./gradlew :game-data:test
./gradlew :battle-engine:test
./gradlew :battle-rules:test
./gradlew :migration:test
./gradlew :scheduler:test
./gradlew :security:test
./gradlew :system:test
```

## 完成前验证

声明完成前至少确认：

- 已读取所有命中的项目技能。
- 已运行与改动匹配的最小测试或构建命令。
- 新增或修改源码注释时已读取 `.codex/skills/source-comments/SKILL.md`。
- 没有引入模板占位词。
- 没有在业务边界引入源品牌词。
- 没有覆盖用户未要求修改的文件。

如果验证无法运行，说明具体命令和失败原因。
