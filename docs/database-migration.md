# 数据库迁移与恢复

Liquibase 的唯一入口是 `avalon-app/src/main/resources/db/changelog.yaml`。应用配置中的
`spring.liquibase.change-log` 配置为 `classpath:db/changelog.yaml`，指向该入口。

入口按编号包含 001～060 的 YAML 变更文件。表、字段、主键、外键、唯一约束、普通索引使用
Liquibase 结构化变更；`account_credentials` 只读视图使用 `createView.selectQuery` 定义。SQL 语句直接写在 YAML 变更集中。

042 清理调度框架表，不删除市场、组队和战局等业务表。升级现有库须先停止应用，再执行数据库迁移，最后启动新应用；不能让仍使用框架任务表的进程与该变更同时运行。

PostgreSQL 条件索引、检查约束以及 `INSERT … SELECT` 历史集合回填使用 YAML 内的 `sql` 变更，限定
`dbms: postgresql`。这些语句仍由 Liquibase 同一变更集事务管理；不为改写格式而丢失条件、约束或回填语义，
也不引入收费检查约束扩展。普通更新使用结构化 `update`。

## 全新数据库

创建空数据库后，在应用启动前执行完整的 `changelog.yaml`。应用运行时应使用同一数据库连接信息；
生产配置保持 `spring.liquibase.enabled: false`，由发布流程显式执行迁移，确认成功后再启动应用。

local 和 test profile 会自动执行迁移。当前工程不会替用户删除数据库、清理业务数据或执行恢复命令；恢复操作由部署者
根据备份策略完成。

## 已有数据库

060 删除旧任务目标表，将任务日志投影改为 `quest_journal_state`，并要求接取回执与日志关联运行实例。本次新版任务系统不导入旧任务存档；已有旧任务数据的数据库不属于直接升级范围，验收使用独立新库。迁移不会自动清空用户数据。

已有数据库通过 DATABASECHANGELOG 记录已执行变更集。发布前核对变更集身份与校验和，再按顺序执行新增变更。
恢复备份时同时保留业务数据与迁移记录；恢复后核对应用版本和数据库结构，避免重复建表或遗漏变更。
不要通过删除迁移记录、清除校验和来强行重跑已有结构；不匹配的数据库基线须单独设计并验证升级方案。

## 变更约束

- 已执行的 changeSet 不修改；新增结构使用递增编号的新 YAML 文件，并在入口追加 include。
- 不在实体启动时自动创建或更新表结构，Hibernate 继续使用 `validate`。
- `account_credentials` 是身份认证使用的只读视图，必须在 `004-credentials-view.yaml` 执行后存在。
- 迁移验证使用独立测试库，覆盖空库建表、历史数据回填、重复执行和数据库结构一致性；用户数据库由用户自行恢复。
