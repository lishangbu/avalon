# 可选 AOT 构建与 JVM 缓存

开发环境默认使用普通 Spring Boot 启动。生产制品可以选择 Spring AOT、JVM AOT Cache（Project Leyden 已交付的能力），或叠加两者。仍然运行在 JDK 上，不生成 GraalVM 原生可执行文件。

## 构建

```bash
# 普通开发与发布制品
mise exec -- ./mvnw clean verify

# 可选 Spring AOT 制品：使用 prod 环境分析 Bean 图
mise exec -- ./mvnw -Paot clean verify
```

切换构建模式时使用 clean，防止上次生成的 AOT 类混入下一次制品。AOT 生成在 avalon-app 的打包过程中执行；普通业务模块没有额外的 AOT 生命周期。
生成代码位于 avalon-app/target/spring-aot。打包后仍可通过 `-Dspring.aot.enabled=false` 使用普通容器；启用 AOT 需显式传入 `-Dspring.aot.enabled=true`。

Spring AOT 分析固定条件 Bean 和 profile。当前 aot 构建采用 prod，并明确排除 Liquibase：数据库结构应在部署前通过普通模式执行迁移。数据库地址、密码、手动机器号由运行环境提供，不能把真实凭据写进 POM。

维护任务始终启用，周期固定在代码中，不提供运行时开关。CosId 开关、机器号分配方式等影响自动装配的设置必须与构建时一致。仅使用 JVM 缓存、关闭 Spring AOT 时，没有 Spring Bean 图固化这一限制。

## 解包、训练与启动

入口为 scripts/aot.py，要求 Linux、Python 3.11+ 和项目 JDK 27；基准测试额外要求 Docker。所有目录可以自行指定，示例使用 target 以免缓存进入源码管理。

```bash
mise exec -- python3 scripts/aot.py extract \
  avalon-app/target/avalon-app-0.1.0-SNAPSHOT.jar avalon-app/target/aot-runtime

# 在解包目录 config/application-prod.yaml 中填写隔离训练库及独占机器号。
# 按 deploy/application-prod.yaml.example 创建具体值配置，禁止连接生产库训练。
export SPRING_PROFILES_ACTIVE=prod

# JVM 缓存；不要求 Spring AOT 制品
mise exec -- python3 scripts/aot.py train avalon-app/target/aot-runtime
mise exec -- python3 scripts/aot.py run avalon-app/target/aot-runtime --cache

# Spring AOT + JVM 缓存；要求使用 -Paot 构建的制品
mise exec -- python3 scripts/aot.py train avalon-app/target/aot-runtime --spring-aot
mise exec -- python3 scripts/aot.py run avalon-app/target/aot-runtime --spring-aot --cache
```

解包目标必须为空，避免旧依赖残留。run 在前台运行，交由 systemd／容器管理生命周期；省略 --cache 即为同一解包制品的无缓存启动。仅使用 Spring AOT 时传 --spring-aot，不传 --cache。

train、run 与 benchmark 默认读取 `avalon-app/src/main/docker/jvm.options`，统一内存、UTF-8、中文及东八区设置。需要覆盖时可用 `--jvm-arg=-Xms1g --jvm-arg=-Xmx1g` 逐项追加；训练和启动必须一致。脚本保存 JAR、JDK 核心文件、缓存校验值和 JVM 参数清单，启动前发现变化会拒绝使用缓存。JVM 以 AOTMode=on 强制加载，避免静默回退而误报优化成功。缓存不能跨 JDK 版本、应用版本或平台复用，也不要在生成后修改依赖的时间戳。

训练使用 Spring 官方的 `spring.context.exit=onRefresh`：实例化单例后退出，未开始完整服务生命周期。但 Liquibase、JPA 初始化等仍可能连接数据库，因此训练必须有可用且结构完整的隔离数据库。该训练只覆盖启动，不包含战斗、市场等实际请求的热点预热。

## 可复现基准

```bash
mise exec -- python3 scripts/aot.py benchmark \
  avalon-app/target/avalon-app-0.1.0-SNAPSHOT.jar \
  avalon-app/target/aot-benchmark --repeats 3 --settle 10
```

输入必须是 AOT 制品，因为基准包含组合模式。脚本创建独立临时 PostgreSQL、随机本地端口，不连接现有开发库。先完成建表，再生成两份缓存。结束或失败时停止本次创建的应用及数据库；只清理自己创建的资源。

对比五种模式：普通可执行 JAR、解包无缓存、解包加 JVM 缓存、解包加 Spring AOT、两者叠加。每轮轮换执行顺序；使用相同 JAR、数据库、JDK、堆参数和生产调度配置。

时间从创建 Java 进程前开始，直到 readiness 健康端点首次返回 UP，轮询间隔 100ms。每次再通过真实 HTTP 验证注册、创角、地图读取与雪花 ID 字符串序列化，等待指定秒数后采集 RSS/PSS。原始结果和中位数写入 results.json，日志分别保留，不记录注册令牌与数据库密码。

RSS/PSS 是 Linux 进程内存，不是 Java 堆使用量。CPU 配额由 Docker 控制，JVM 自动识别容器可用处理器；本机暖文件缓存、低负载的短时采样不能代替 2C4G 长期负载、GC 和容量测试。readiness 只证明服务就绪，不能证明全部业务被预热。具体结果见 [实测记录](aot-validation.md)。

依据：[Spring Boot AOT Cache](https://docs.spring.io/spring-boot/reference/packaging/aot-cache.html)、[Spring AOT 条件限制](https://docs.spring.io/spring-boot/how-to/aot.html)、[Spring Framework JVM 缓存](https://docs.spring.io/spring-framework/reference/integration/aot-cache.html)。
