# 开发约定

当前工程为 Spring Boot + Maven 模块化单体，准确版本、执行模型和认证规则见 [后端架构](adr/0001-spring-modular-monolith.md)。
前端为 Vue 3 / Ionic Vue，见 [前端结构](frontend-structure.md)。

## 工程与数据

- Java 源码与测试源码使用 Spring Java Format 默认格式；Maven validate 自动校验，编辑后执行格式化命令，见 [构建约定](maven-build.md)。
- 单仓库 avalon；根目录设有 avalon-parent、avalon-dependencies 及十个后端 Maven 模块，avalon-ui 保留独立 npm 依赖，由 frontend-maven-plugin 构建后随同一个 JAR 发布。
- 使用同步 JDBC、虚拟线程、Spring Data JPA；优先仓库查询，无实体存储使用 JdbcClient，生产仓库不直接使用 EntityManager。
- Liquibase YAML 管理数据库结构和字段 remarks，Hibernate 仅 validate；实体及业务逻辑编写完整中文注释。结构变更遵循 [数据库约定](database-migration.md)。
- CosId Spring Boot Starter 管理共享雪花生成器，Hibernate 主键生成器直接调用该 Bean 发号；机器号在 cosid.machine.distributor.manual.machine-id 配置，生产填写独立 application-prod.yaml，不使用环境变量占位符。实体内为 Long，对外字符串序列化。Lombok 简化访问器和依赖构造器；实体避免 @Data 引发关系或敏感字段的 equals/toString。
- YAML 配置填写具体值，公共配置为 application.yaml，本地和测试分别为 application-local.yaml、application-test.yaml；生产敏感值使用不提交 Git 的外置 application-prod.yaml。自动化隔离数据库可通过 Spring 标准属性覆盖临时连接信息，不在 YAML 中维护自定义环境变量映射。Maven POM 的 revision 等构建属性不属于运行环境变量占位符。
- 根包 io.github.lishangbu.avalon。avalon-dependencies 继承 spring-boot-starter-parent 并补充项目依赖版本，avalon-parent 管理 JDK 与项目专用插件配置，前端版本由 package-lock.json 锁定。
- 内容使用 JSON 与手写 Draft 2020-12 Schema，编号、引用和组合规则启动校验；失败拒绝启动，不热更，不引入内容版本／快照系统。

## 服务端边界

- 服务端权威结算；模块内轻量 CQRS，命令、查询、独立结果、领域规则和持久化分层；实体不跨模块、不作 HTTP 响应。
- 写用例取得角色锁，奖励、扣费、任务推进、战局和幂等记录原子提交；跨账号事务遵守固定锁序。
- 查询不取得角色写锁。同步领域事件参与原事务，失败向外传播，不用异步通知代替原子结算。
- 战局及回执持久化支持同版本重启恢复；改变内容规则时仍须评估未完成战局兼容性，不自动结束或回滚玩家战斗。
- Spring 内存调度负责周期唤醒；业务表保存截止时间并保证事务与幂等。初期单实例，扩容前增加任务领取协调和独立机器号。
- Spring Security 核验不透明 Bearer 会话，刷新令牌摘要存储及轮换；Web 刷新令牌在 localStorage，访问令牌仅在内存。

## 客户端与验证

- pages 按业务目录组装页面，features 管理业务请求和状态，shared 提供公共能力；路由、服务端缓存、身份状态各自独立。
- Ionic 统一 iOS 模式，支持浅色／深色／系统主题；手机优先，当前仅浏览器，不引入 Android 封装。
- 不把页面路由当作地图移动，战斗门禁同时在服务端核验。
- 沿用现有代码格式；前端使用 .prettierrc.json，不引入 Spring Java Format。注释解释业务、事务及异常边界。
- 按改动运行必要测试；当前标准入口 `./mvnw verify`，完整显式验收见 [工程验证](engineering-acceptance.md)。
- 测试使用隔离数据库，不操作用户数据库；不以模拟视口代替真机，以测试耗时代替真人操作时间，或以功能测试代替性能证据。
