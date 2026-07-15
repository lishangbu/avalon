# Avalon 后端 Agent 指南

## Agent skills

### Issue tracker

Issues 使用 GitHub Issues；外部 PR 不作为分诊请求入口。详见 `docs/agents/issue-tracker.md`。

### Triage labels

分诊使用默认的五类 canonical labels。详见 `docs/agents/triage-labels.md`。

### Domain docs

仓库采用 single-context 布局，领域语言位于根目录 `CONTEXT.md`，决策位于 `docs/adr/`。详见 `docs/agents/domain.md`。

## 仓库边界

- 本仓库是独立 Git 仓库 `avalon`；管理端位于独立仓库 `avalon-admin-ui`。
- 只修改用户要求范围内的后端文件；不要跨仓库暂存、提交或改写历史。
- 用户未明确要求时，不执行 commit 或 push。

## 领域语言与决定

- 领域模型、资料或 API 语义变化前读取 `CONTEXT.md`。
- 遇到难以逆转或不明显的既有决定时读取 `docs/adr/`。
- 新术语即时写入 `CONTEXT.md`；它只保存领域语言，不保存实现细节。
- 只有难以逆转、代码中不明显且存在真实取舍的决定才新增 ADR。

## 技术栈路由

| 变更内容                                     | 必读 skill                                                |
| -------------------------------------------- | --------------------------------------------------------- |
| Kotlin 源码、类型、空安全、KDoc              | `.codex/skills/kotlin/SKILL.md`                           |
| Gradle、模块、插件、依赖、版本               | `.codex/skills/gradle/SKILL.md`                           |
| Spring Boot、Controller、配置、Web、自动配置 | `.codex/skills/spring-boot-web/SKILL.md`                  |
| Jackson、JSON、mapper、序列化                | `.codex/skills/jackson-3/SKILL.md`                        |
| Jimmer、实体、查询、事务、CosId              | `.codex/skills/jimmer/SKILL.md`                           |
| Liquibase、PostgreSQL、schema、seed          | `.codex/skills/liquibase-postgresql/SKILL.md`             |
| Spring Security、OAuth2、token、RBAC         | `.codex/skills/spring-security-oauth2/SKILL.md`           |
| Springdoc、OpenAPI、schema 契约              | `.codex/skills/springdoc-openapi/SKILL.md`                |
| Quartz、定时任务、cron、执行记录             | `.codex/skills/quartz/SKILL.md`                           |
| AWS SDK v2 S3、对象存储、starter             | `.codex/skills/aws-sdk-s3/SKILL.md`                       |
| JUnit、Spring Test、Testcontainers、TDD      | `.codex/skills/junit-spring-test-testcontainers/SKILL.md` |
| Git、暂存、提交、签名                        | `.codex/skills/git/SKILL.md`                              |

命中多个技术栈时全部读取。遇到“继续”或“下一步”，先检查当前对话、计划和 diff，再加载实际命中的 skills；不使用独立 router。

## 跨栈政策

- 版本事实来自 `gradle/libs.versions.toml`、Gradle Wrapper 和构建文件；不顺手升级。
- 新行为与缺陷修复遵循 red-green-refactor；重构先补 characterization test。
- 源码注释、KDoc 和用户可见文案使用中文；代码标识符与协议字段保持英文。
- 后端 OpenAPI 是管理契约权威；Identifier 在 JSON 中是字符串，普通 Long 度量仍是数字。
- Liquibase 默认追加 changeset；只有用户明确授权且无需兼容部署数据库时才重做基线。
- 验证按风险分层；跨栈、构建边界或完成交付前同时运行仓库快速 `test` 与完整 `integrationTest`。

## 完成前

- 确认所有命中 skills 的 completion criteria 已满足。
- 运行聚焦测试，并按风险运行受影响模块；完成交付前运行完整 `test` 与 `integrationTest`。
- 运行 `git diff --check`，保留用户无关改动。
- 无法运行 Docker、外部服务或平台验证时，报告具体命令和未验证范围。
