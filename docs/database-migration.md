# 数据库结构与初始化

数据库结构由各模块的 Hibernate 实体映射统一维护，不再使用 Liquibase 或独立建库 SQL。实体声明表、字段、类型、长度、可空性、默认值、注释、唯一约束、索引和检查约束；认证直接通过 AccountRepository 查询 Account 实体，不创建认证视图。

## 初始化

创建 PostgreSQL 空数据库并授权后启动应用即可。所有 profile 默认使用 `spring.jpa.hibernate.ddl-auto=update`；DDL 执行错误会阻止启动。数据库账号需要建表、建索引、修改表和注释的权限。

已有数据库沿用现有表和数据，不执行清库或重建。原 Liquibase 跟踪表不再使用，也不自动删除。Hibernate update 不提供跨实例迁移锁，生产升级应停止旧应用，由单个新实例完成结构更新后再接收流量。

update 不是版本迁移工具：字段改名、删除旧字段、数据回填、已有检查约束或索引定义的替换，仍需在维护窗口显式处理。不要把改名理解为自动搬迁数据，也不要在持久化环境使用 create 或 create-drop。升级前备份，回滚时评估数据库结构兼容性。

## 维护与验证

使用 Jakarta Persistence 的 @Table(comment=...)、@Column(comment=...) 维护中文注释；约束和普通索引使用标准注解，默认值使用 @ColumnDefault。跨模块以标量编号关联，@DatabaseForeignKey 将标量外键加入 Hibernate 元数据，@PartialIndex 维护 PostgreSQL 部分索引，不引入跨模块 JPA 对象关联。

JDBC 存储使用所属模块的只读 *Row 实体声明结构，业务仍通过 JDBC 读写；市场集合表由 @ElementCollection 维护。修改结构时同步更新映射和真实 PostgreSQL 测试，覆盖账号认证、字段注释、索引、约束及业务事务。
用户数据库和备份由部署者管理；工程清理不执行删除、恢复或重置用户数据库。

旧数据库中的 account_credentials 视图不再被应用使用，Hibernate update 不会自动删除旧视图；确认没有其他消费者后，可在数据库维护时删除。
