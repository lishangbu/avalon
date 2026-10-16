# 工程验证与交付边界

本页说明当前工程的验证入口、覆盖范围及交付边界。

## 工程配置

Spring Boot 4.1.1、IBM Semeru Runtime Open Edition JDK 25、Maven 3.10.0、Lombok 1.18.48、Jackson 3、CosId 3.4.1、NetworkNT 3.0.8。
前端工具链固定Node.js 24.22.0、Vue 3.5.43、Vue Router 5.4.0、Ionic 9.0.7、Vite 8.3.4及TypeScript 6.0.3。
TypeScript 7.0.2与当前vue-tsc 3.3.13不兼容，构建会报 `ERR_PACKAGE_PATH_NOT_EXPORTED: typescript/lib/tsc`，因此保留已验证的6.0.3；不使用预发布后端依赖。
游戏内容使用 JSON 与 Schema，数据库结构使用 Liquibase YAML，客户端使用 Vue/Ionic。
测试运行在隔离容器中，不操作用户数据库。

HTTP 集成测试统一使用 Spring RestTestClient，由 `@AutoConfigureRestTestClient` 注入并访问随机端口的真实服务器；保留并发请求与事务提交验证。共享 PostgreSQL 容器通过 `@ServiceConnection` 提供连接信息。数据库故障夹具与真实进程恢复测试仍显式控制事务及生命周期。

Jackson 反序列化选项由 application.yaml 管理，周期维护使用显式 ThreadPoolTaskScheduler，在 ApplicationRunner 中注册 fixedDelay 任务。Failsafe 执行阶段沿用 Spring Boot parent 的定义。

运行时负载检查共用 `scripts/runtime_checks.py` 的 HTTP 请求函数；JVM 参数由 `avalon-app/src/main/docker/jvm.options` 统一提供给容器及本机运行，包含 IBM Semeru OpenJ9 的吞吐和共享类缓存设置。

构建结构见 [构建约定](maven-build.md)，生产代码边界见 [后端结构](backend-structure.md)。测试夹具允许通过 EntityManager 注入边界数据及数据库故障。

## 日常模拟

优先运行`scripts/simulate-natural.sh`：启动隔离PostgreSQL和随机HTTP端口，通过公开接口完成注册、BQ001～074、捕捉、课程、恢复、正式战斗并达到16级，不启动浏览器。`AVALON_SIMULATION_ROUTE=level15`运行开篇30步到15级及卫兵装备准备；`both`分别注册新账号验证两条路线。命令及证据范围见[全接口日常模拟](quest-system-validation.md#全接口日常模拟)。

浏览器入口单独负责页面操作、显示和布局；全接口通过不能证明按钮或手机页面正确。现有四职业八条服务层自然成长路线仍保留。

## 违禁文字验证

统一词表与检测边界见[内容维护](content-json.md#玩家自填文字与违禁词)。
`ForbiddenWordsCatalogTest`检查逃避变形、允许边界及无效配置；`ForbiddenWordsTest`通过真实HTTP和隔离数据库，
检查四个写入口、注册回滚、旧账号登录、旧回执重放、正常原文保存及改名费用／次数／准备版本不变。
`ForbiddenWordsBrowserIT`独立验证桌面与手机视口的注册、创角、免费和付费改名拒绝后保留输入、修正重试。
伙伴夹具只用于准备幻兽与金币，不作为自然成长证据。

```bash
mise exec -- ./mvnw -pl avalon-app -am test -Dskip.frontend=true -Dtest=ForbiddenWordsCatalogTest,ForbiddenWordsTest -Dsurefire.failIfNoSpecifiedTests=false
cd avalon-ui
npm run build
npm run test:typecheck
cd ..
AVALON_FORBIDDEN_WORDS_BROWSER=1 PLAYWRIGHT_CHROMIUM_EXECUTABLE=/usr/bin/google-chrome mise exec -- ./mvnw test-compile failsafe:integration-test failsafe:verify -Dskip.frontend=true -Dit.test=ForbiddenWordsBrowserIT -Dfailsafe.failIfNoSpecifiedTests=false
```

浏览器日志在`avalon-app/target/forbidden-words-browser.log`，本次构建及回归日志保存在`outputs/forbidden-words/`。

## 验证覆盖

- 后端测试覆盖角色、战斗、捕捉、背包、强化、进化、市场、并发和事务回滚。
- `FormalOpeningBrowserIT`独立启动隔离服务，`formal-opening.spec.ts`从注册开始操作BQ001～BQ030，包含六门学院课程、城郊调查、晚餐及沿相邻道路返乡；捕捉使用正常概率、野战练到5级，不注入经验或任务。重复训练默认前两场走页面，其余逐场调用同会话正式HTTP接口；移动、恢复与每回合结算均真实执行。任务改造后的最新结果及组合回归入口见[任务系统验收](quest-system-validation.md)，不沿用改造前的通过记录。
- `PhaseTwoBrowserIT`验证教学、绑定进化、强化装备成交、隐藏伙伴退货与再次成交；初始等级、资产和城市开放事实来自隔离夹具。
- MarketProcessRecoveryIT 覆盖真实打包进程停止后的市场到期恢复。
- HTTP 契约覆盖请求上限、错误状态码、令牌撤销、无 Servlet 会话、SPA 回退与未知端点 404。
- MaintenanceSchedulingTest 覆盖启动注册、固定周期、异常重试及关闭；CreatureMigrationTest 检查框架任务表已清理。
- Docker 镜像采用 IBM Semeru Runtime Open Edition 25、分层打包、root 和直接 Java 启动，生产配置关闭自动迁移；隔离验收确认镜像构建及 readiness，见 [镜像部署](docker-deployment.md)。

页面专项入口为`InterfaceBrowserIT`，设置`AVALON_INTERFACE_BROWSER=1`后选择该Failsafe测试类。它把隔离数据库身份传给页面夹具；正式开篇、自然成长和第二阶段培养交易使用各自独立入口。

报告由当前验证生成，位于各模块 target/surefire-reports、avalon-app/target/failsafe-reports 及本轮浏览器测试日志；clean 会删除报告。文件存在不代表当前代码已通过，应检查本次命令退出码。

## 复验入口

```bash
./mvnw verify
AVALON_PHASE_TWO_BROWSER=1 AVALON_PROCESS_RECOVERY=1 ./mvnw verify
```

前者执行后端测试和完整打包；后者额外在打包后运行浏览器及真实进程恢复验收。
浏览器需要安装 Playwright Chromium，也可设置 PLAYWRIGHT_CHROMIUM_EXECUTABLE。
所有集成测试使用隔离 PostgreSQL 容器和随机 HTTP 端口，结束后关闭测试应用。

## 内容、页面与负载入口

本轮已接入28基础种及12种低级进化形态，并更新实际特性、学习池、材料来源及存档迁移。
详细结果以[当前进度](implementation-progress.md)及`outputs/level15/`中的命令日志为准，不沿用上方较早的页面通过记录作为最新全覆盖证明。

```bash
python3 scripts/check-java-imports.py
python3 scripts/opening-content-audit.py --strict
./mvnw -q io.spring.javaformat:spring-javaformat-maven-plugin:0.0.48:validate
./mvnw -pl avalon-app -am -Dskip.frontend=true '-Dtest=*,!*IT' -Dsurefire.failIfNoSpecifiedTests=false test
PLAYWRIGHT_CHROMIUM_EXECUTABLE=/usr/bin/google-chrome AVALON_DUNGEON_BROWSER=1 AVALON_ROWAN_BROWSER=1 AVALON_GARDEN_BROWSER=1 ./mvnw -pl avalon-app -am -Dskip.frontend=true -Dtest=CreatureMigrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=DungeonBrowserIT,RowanBrowserIT,GardenBrowserIT -Dfailsafe.failIfNoSpecifiedTests=false verify
```

浏览器或打包验收使用`-Dskip.frontend=true`前，先在`avalon-ui`运行`npm run build`和`npm run test:typecheck`，保证采用当前页面。纯HTTP模拟无需前端构建。
`NaturalExtendedStoryTest`覆盖四职业从注册至BQ074和16级的成长、恢复及自然材料进化。
自然成长、多人页面及经济计时分别使用上述现行入口，不再保留旧副本成长路线。

`DungeonBrowserIT`隔离运行三账号、推荐12级桌面／手机页面，覆盖真实道路、准备、四场通关、战间休整、掉线拒绝开战、刷新恢复和补员。
其奖励对照使用同一人物及伙伴，在野外完成20场、低资源或每五场计入往返恢复；三个副本账号并行操作，排除专门的掉线及刷新检查。自动化不包含真人思考时间。
首次完整探索的资格、防重、缺场及回滚由`DungeonRewardTest`独立覆盖。运行`python3 scripts/dungeon-reward-comparison.py`根据保存的桌面／手机计时和当前配置重新计算倍率。

执行`python3 scripts/isolated-runtime-load.py`可自动启动临时网络、数据库与应用，压测后清理；默认使用本地`avalon:semeru25`镜像挂载最新JAR。
负载工具为`scripts/runtime-load-check.py`和`scripts/runtime-battle-load-check.py`；前者注册后只读，后者仅允许对`avalon-audit-`前缀的隔离容器写入测试夹具。
保留`isolated-load-expanded.json`、`isolated-battle-load.json`及资源限制记录作为本轮证据。测试夹具和用户数据库隔离。

## 实现约束

内容 Schema 判定必填字段，Jackson 3 解析器对可选原语采用 false/0 默认值。
主键、刷新撤销、事务和调度约定见 [后端架构](adr/0001-spring-modular-monolith.md)。CosIdPersistenceTest 覆盖生成时机、受管实例、更新保号、回滚及字符串序列化。测试上下文替换调度启动 Bean，打包进程使用真实固定周期。

Spring Boot 4.1.1 的官方 Java 兼容表暂列到 26；IBM Semeru Runtime Open Edition JDK 25 使用真实编译和回归验证，不声称获得官方支持保证。

## 验证范围

后端自然成长覆盖BQ001～074及16级，后续成长与第二阶段业务分别有独立入口；页面专项按受影响流程选择，不把某一批用例数量当作全部页面或真机覆盖。
隔离2核4GiB资源限制的认证读取及战斗写入负载已完成；应用1核3GiB、数据库1核1GiB。
24并发读取18569请求无错误，P95 96.56ms；12并发战斗写入15575请求、1019场战斗无错误，P95 62.03ms。
共享宿主机及高等级人物对低级野怪的负载样本不能推算服务器在线容量。
4～6 小时真人体验目标、正式生产上线与未来等级内容仍待验证和交付。

## 副本奖励对照

首次与重复探索的经验效率由`DungeonBrowserIT`记录同一人物与伙伴的20场野战和并行副本计时，使用`scripts/dungeon-reward-comparison.py`读取当前奖励配置复算。该工具拒绝不符合当前协议的旧样本。报告是自动化操作对照，不证明真人职业、网络及思考时长下的固定倍率。
