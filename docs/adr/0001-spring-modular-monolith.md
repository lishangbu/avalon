# Spring Boot + Maven 模块化单体

状态：已确认。使用 Spring Boot 4.1.1、IBM Semeru Runtime Open Edition JDK 25、Maven 3.10.0。

## 工程及执行模型

单仓库 avalon，根目录包含 avalon-parent、avalon-dependencies 以及 10 个后端 Maven 模块，avalon-app 是唯一启动及打包入口。
avalon-ui 使用 Vue 3、Vite 和 Ionic Vue，frontend-maven-plugin 安装项目专用 Node/npm，
执行 npm ci 与构建，静态产物复制到应用 classpath:/static，最终发布一个可执行 JAR。
开发时 Vite 5173 代理后端 8080。

根聚合使用 POM 4.0.0、modules 与 revision 统一版本，flatten-maven-plugin 生成安装及发布 POM；avalon-dependencies 统一依赖版本，avalon-parent 统一构建插件。
父链依次为 spring-boot-starter-parent、avalon-dependencies、avalon-parent 与业务模块；根聚合也继承 avalon-parent。使用父链继承依赖管理，避免同 reactor 的自身 BOM 导入；详细规则见 [构建约定](../maven-build.md)。

Spring MVC、嵌入式 Tomcat 与虚拟线程承担 HTTP 请求，业务使用同步 JDBC。
构造器注入组织模块，无独立消息总线、读库或事件溯源。命令与查询分层，实体不跨模块暴露。

项目使用 IBM Semeru Runtime Open Edition JDK 25。Spring Boot 4.1.1 官方兼容表暂列 Java 17～26；
项目使用 IBM Semeru Runtime Open Edition JDK 25 的适用范围以工程验收证据为准，不将实测兼容描述为官方承诺。

## 持久化和事务

PostgreSQL、HikariCP、Hibernate 与 Spring Data JPA。仓库使用派生查询、@Query、@Lock 和 Specification；
无实体的维护队列、参战关联和每日计数使用 JdbcClient，加入同一 JPA 数据源事务。
生产代码不直接使用 EntityManager，也不提供接收任意 HQL 的通用业务仓库。

CosId Spring Boot Starter 自动装配共享雪花生成器。普通实体以 `@Id` + `@CosIdGenerated` 标记 Long 主键，创建时保持 null；Hibernate 的 BeforeExecutionGenerator 在 persist 时经当前 SessionFactory 的 Bean 容器按名称获取共享 CosId 生成器并发号，save 返回时即可取得编号。业务服务不手工填充普通实体主键，更新不会重新发号。
YAML 配置手动机器号，部署方保证实例间唯一；对外编号按字段注解序列化为字符串。自然主键 RefreshReceipt、共享主键 CombatVitals 与 InventoryGrant 保留 AssignedEntity，用 Persistable.isNew 区分新增与加载，避免手工赋值主键导致 merge 副本。
物品首次保存生成编号后，与奖励出处建立共享编号关联，全部在同一事务完成；转手、拆分和回滚不能改变来源防重语义。

应用服务定义事务边界，角色行锁、金币、物品、任务和幂等回执在同一事务提交。
跨账号交易按固定锁序处理。同步 Spring 事件监听器加入发布者事务，失败向外传播；
不能改为提交后通知。OSIV 关闭，查询在应用服务事务中完成。

Liquibase YAML 是数据库初始化入口，执行 db/schema.sql 中的表、字段注释、索引、约束及认证视图。
Hibernate 只 validate。测试使用隔离 PostgreSQL 容器；local 自动迁移，生产默认关闭自动迁移。

## 身份及会话

Spring Security 使用只读 account_credentials 视图验证 bcrypt 摘要。
认证使用随机不透明 Bearer Token，访问令牌有效期 15 分钟，设备绝对寿命 30 天；支持摘要存储、刷新轮换和设备撤销。
每次 API 请求查询会话，不建立 Servlet 登录会话或撤销缓存；长期刷新令牌使用浏览器 localStorage。

刷新先按摘要读取标量主键，再持写锁读取完整会话，避免一级缓存中的旧摘要。
重放先提交撤销，再由控制器返回 401，禁止把控制器也包在会回滚撤销的外层事务中。
密码计算不包裹业务写事务。API 使用错误 message 字段、状态码、雪花字符串 ID 和空响应 204。

## 调度与内容

使用 Spring ThreadPoolTaskScheduler 内存调度，四个工作线程分别承接会话清理、市场到期、副本回合与组队超时扫描。
间隔固定为 1 小时、10 秒、1 秒和 3 秒，fixedDelay 保证同一任务不重叠，不提供运行配置及禁用开关。
业务截止时间持久化；启动完成后扫描已到期状态，不重放全部离线回合。每笔独立事务，失败留待重试，关闭最多等待 30 秒。
当前只支持单实例调度，扩容须先实现任务领取协调。

静态内容为 JSON + 手写 Draft 2020-12 Schema，Jackson 3 + NetworkNT 3 校验和解析。
只加载本地 Schema，禁止远程引用。结构校验与 Java 跨文件业务校验都通过才开服，运行期间不热更。
应用配置与 Liquibase 使用 YAML。

## 验收

Maven test 执行单元及真实数据库/HTTP 回归；verify 在打包后执行显式启用的浏览器和进程恢复测试。
验证结果及执行命令见 [工程验收](../engineering-acceptance.md)。
工程验收不等于玩法平衡验收，真人 4～6 小时体验目标仍需实测。
