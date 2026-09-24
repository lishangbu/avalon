# 工程验证与交付边界

本页说明当前工程的验证入口、覆盖范围及交付边界。

## 工程配置

Spring Boot 4.1.1、JDK 27、Maven 3.9.16、Lombok 1.18.48、Jackson 3、NetworkNT 3.0.7。
游戏内容使用 JSON 与 Schema，数据库结构使用 Liquibase YAML，客户端使用 Vue/Ionic。
测试运行在隔离容器中，不操作用户数据库。

HTTP 集成测试统一使用 Spring RestTestClient，由 `@AutoConfigureRestTestClient` 注入并访问随机端口的真实服务器；保留并发请求与事务提交验证。共享 PostgreSQL 容器通过 `@ServiceConnection` 提供连接信息。数据库故障夹具与真实进程恢复测试仍显式控制事务及生命周期。

Jackson 反序列化选项由 application.yaml 管理，周期维护使用显式 ThreadPoolTaskScheduler，在 ApplicationRunner 中注册 fixedDelay 任务。Failsafe 执行阶段沿用 Spring Boot parent 的定义。

部署脚本共用 `scripts/runtime_checks.py` 的 HTTP 和注册→角色→地图检查；JVM 参数由 `avalon-app/src/main/docker/jvm.options` 统一提供给容器及本机基准。

构建使用 Maven Wrapper、根聚合 POM、两个构建管理 POM 与 10 个后端模块 POM、frontend-maven-plugin 和 Spring Boot 可执行 JAR 打包。
生产代码使用 Spring Bean、MVC、Security、Spring Data JPA / JdbcClient 和 Spring 内存调度。
不使用 EntityManager 实现生产仓库；测试夹具允许通过 EntityManager 注入边界数据及数据库故障。

## 验证覆盖

- 后端测试覆盖角色、战斗、捕捉、背包、强化、进化、市场、并发和事务回滚。
- `FormalOpeningBrowserIT`独立启动隔离服务，`formal-opening.spec.ts`从注册开始操作BQ001～BQ030，包含六门学院课程、城郊调查、晚餐及沿相邻道路返乡；捕捉使用正常概率、野战练到5级，不注入经验或任务。任务改造后的最新结果及组合回归入口见[任务系统验收](quest-system-validation.md)，不沿用改造前的通过记录。
- PhaseTwoBrowserIT 的桌面与手机各三条流程已通过：教学与绑定进化、强化装备成交、隐藏伙伴退货与再次成交。最新独立运行共6项通过、42.2秒、退出码0，日志为`/tmp/avalon-phase-two-current.log`及`avalon-app/target/phase-two-browser.log`。初始等级、资产和城市开放事实由隔离夹具提供，不代表15级开篇已开放这些培养系统。
- MarketProcessRecoveryIT 覆盖真实打包进程停止后的市场到期恢复。
- HTTP 契约覆盖请求上限、错误状态码、令牌撤销、无 Servlet 会话、SPA 回退与未知端点 404。
- MaintenanceSchedulingTest 覆盖启动注册、固定周期、异常重试及关闭；CreatureMigrationTest 检查框架任务表已清理。
- Docker 验收覆盖 Corretto 27、root、独立 YAML、迁移、AOT 缓存及注册→创角→地图流程，见 [镜像部署](docker-deployment.md)。

页面专项入口为 `InterfaceBrowserIT`：设置 `AVALON_INTERFACE_BROWSER=1`，运行
`./mvnw -pl avalon-app -am -Dtest=InterfaceBrowserIT -Dsurefire.failIfNoSpecifiedTests=false test`。
它把隔离容器名称、数据库用户和库名传给现有页面夹具，依次执行其余页面专项；正式开篇、
自然成长和第二阶段培养交易保留各自独立入口。修正后的全部界面专项直接复用正在运行的隔离服务，在独立夹具账号上执行：58项得到57通过、0失败、1条件跳过，耗时8.4分钟，退出码0。结果见`/tmp/avalon-interface-final.log`。桌面跳过的是仅针对手机窄屏与横屏的布局用例，该用例已在手机项目通过。该结果不等于自然成长或培养交易专用验收已通过。

报告由当前验证生成，位于各模块 target/surefire-reports、avalon-app/target/failsafe-reports 及本轮浏览器测试日志；clean 会删除报告。文件存在不代表当前代码已通过，应检查本次命令退出码。

## 复验入口

```bash
./mvnw verify
AVALON_PHASE_TWO_BROWSER=1 AVALON_PROCESS_RECOVERY=1 ./mvnw verify
```

前者执行后端测试和完整打包；后者额外在打包后运行浏览器及真实进程恢复验收。
浏览器需要安装 Playwright Chromium，也可设置 PLAYWRIGHT_CHROMIUM_EXECUTABLE。
所有集成测试使用隔离 PostgreSQL 容器和随机 HTTP 端口，结束后关闭测试应用。

## 本轮前15级复验

本轮已接入28基础种及12种低级进化形态，并更新实际特性、学习池、材料来源及存档迁移。
详细结果以[当前进度](implementation-progress.md)及`outputs/level15/`中的命令日志为准，不沿用上方较早的页面通过记录作为最新全覆盖证明。

```bash
python3 scripts/check-java-imports.py
python3 scripts/opening-content-audit.py --strict
./mvnw -q io.spring.javaformat:spring-javaformat-maven-plugin:0.0.48:validate
./mvnw -pl avalon-app -am -Dskip.frontend=true '-Dtest=*,!*IT' -Dsurefire.failIfNoSpecifiedTests=false test
PLAYWRIGHT_CHROMIUM_EXECUTABLE=/usr/bin/google-chrome AVALON_DUNGEON_BROWSER=1 AVALON_ROWAN_BROWSER=1 AVALON_GARDEN_BROWSER=1 ./mvnw -pl avalon-app -am -Dskip.frontend=true -Dtest=CreatureMigrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=DungeonBrowserIT,RowanBrowserIT,GardenBrowserIT -Dfailsafe.failIfNoSpecifiedTests=false verify
```

执行跳过前端的Maven命令前，先在`avalon-ui`运行`npm run build`和`npm run test:typecheck`，保证打包采用当前页面。
`NaturalExtendedStoryTest`覆盖四职业从注册至BQ066和15级的成长、恢复及自然材料进化。
自然成长、多人页面及经济计时分别使用上述现行入口，不再保留旧副本成长路线。

`DungeonBrowserIT`隔离运行三账号、推荐12级桌面／手机页面，覆盖真实道路、准备、四场通关、战间休整、掉线拒绝开战、刷新恢复和补员。
其奖励对照使用同一人物及伙伴，在野外完成20场、低资源或每五场计入往返恢复；三个副本账号并行操作，排除专门的掉线及刷新检查。自动化不包含真人思考时间。
首次完整探索的资格、防重、缺场及回滚由`DungeonRewardTest`独立覆盖。运行`python3 scripts/dungeon-reward-comparison.py`根据保存的桌面／手机计时和当前配置重新计算倍率。

执行`python3 scripts/isolated-runtime-load.py`可自动启动临时网络、数据库与应用，压测后清理；默认使用本地`avalon:corretto27`镜像挂载最新JAR。
负载工具为`scripts/runtime-load-check.py`和`scripts/runtime-battle-load-check.py`；前者注册后只读，后者仅允许对`avalon-audit-`前缀的隔离容器写入测试夹具。
保留`isolated-load-expanded.json`、`isolated-battle-load.json`及资源限制记录作为本轮证据。测试夹具和用户数据库隔离。

## 实现约束

内容 Schema 判定必填字段，Jackson 3 解析器对可选原语采用 false/0 默认值。
普通雪花主键通过 Hibernate 的 CosId 生成器在 persist 时赋值；自然／共享主键使用 Persistable 生命周期，防止 save 错用 merge。CosIdPersistenceTest 覆盖生成时机、受管实例、更新保号、回滚及字符串序列化。
刷新令牌先读取标量 ID 再锁定实体；撤销必须先提交，HTTP 层再返回错误。
同步领域事件加入原事务；重启后扫描数据库中的到期状态，不追算全部离线回合。测试上下文单独替换调度启动 Bean，打包进程使用真实固定周期。

Spring Boot 4.1.1 的官方 Java 兼容表暂列到 26；JDK 27 使用真实编译和回归验证，不声称获得官方支持保证。

## 验证范围

后端测试覆盖 1～15 级与第二阶段业务断言，
浏览器专项覆盖六条复杂流程，未把它描述为全部现有 Playwright 脚本或所有真机已复验。
隔离2核4GiB资源限制的认证读取及战斗写入负载已完成；应用1核3GiB、数据库1核1GiB。
24并发读取18569请求无错误，P95 96.56ms；12并发战斗写入15575请求、1019场战斗无错误，P95 62.03ms。
共享宿主机及高等级人物对低级野怪的负载样本不能推算服务器在线容量。
4～6 小时真人体验目标、正式生产上线与未来等级内容仍待验证和交付。

## 奖励校准记录（2026-09-22）

12级阶段经验合计2994，加通关200为3194；首次完整探索独立加4800，共7994。
阶段基础经验调整为外径每敌320、三名首领各720，沿用原等级修正和取整；金币与非经验奖励保持原值。
桌面并行四场31979ms、野外20场含恢复51145ms/2548经验；手机30845ms、50391ms/2573经验。
由最终配置计算的重复／首次效率分别为桌面2.005／5.018、手机2.028／5.076。
计时使用调整前经验值，人物在对照前后均处于12级，未改变战斗参数；这是一组自动化基线校准，不宣称覆盖真人职业、网络及思考时长差异。
报告为`outputs/level15/dungeon-reward-comparison.json`，最终配置的副本及内容专项85项通过，打包退出码0，见`outputs/level15/final-rewards-summary.json`。
