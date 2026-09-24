# Docker 镜像与 AOT 缓存

`docker` Maven profile 使用 Fabric8 docker-maven-plugin 0.49.0，只在 avalon-app 构建应用镜像，不自动推送。版本由 avalon-parent 统一管理。普通 Maven 构建不访问 Docker。

## 构建本地应用镜像

```bash
mise exec -- ./mvnw -Paot,docker clean verify
```

默认生成 `avalon:0.1.0-SNAPSHOT`，基础镜像为 AWS ECR 官方 `public.ecr.aws/amazoncorretto/amazoncorretto:27`，POM 和 Dockerfile 固定摘要 `sha256:34423799ede3faf8d1a0646d77d4028252e60775a1db087b3871906d815a92b6`。可以用 `-Davalon.image=仓库/avalon:版本` 指定应用标签，用 `-Davalon.base-image=镜像@sha256:摘要` 固定 JDK 基础镜像。请使用已验证可获取的 JDK 27 镜像；训练和运行必须使用同一发行版、补丁和架构。

Dockerfile 在构建容器中解包 JAR，Dockerfile 不显式指定 USER，继承当前 Corretto 基础镜像的 root 身份，监听 8080。此时默认 `plain` 模式，不要求缓存，可以用于迁移和排障。加 `-Paot` 才能为后续组合训练生成必要的 Spring AOT 代码；只需普通镜像时单独使用 `-Pdocker`。

## 训练并封装缓存镜像

```bash
mise exec -- python3 scripts/docker-aot.py \
  --source avalon:0.1.0-SNAPSHOT \
  --image avalon:0.1.0-SNAPSHOT-aot
```

脚本使用本地 Docker，创建独立临时桥接网络、受限权限的配置卷与 PostgreSQL（数据库不发布端口，验收 HTTP 只绑定回环地址），先用普通 Spring 模式执行 Liquibase，再在原应用镜像中训练缓存。生成的缓存通过第二份 Dockerfile 和 Maven 插件封装，最终镜像继承训练时锁定的源镜像 ID。不会复制宿主机生成的缓存，不使用 docker commit，不把容器环境中的数据库凭据写进最终镜像。

最后自动运行缓存镜像，检查实际 AOT 缓存加载、root 身份、只读根文件系统，以及注册、创角和地图读取。结束或失败都会删除本次临时容器、配置卷和网络；镜像和 target 下的验收日志保留。`--output` 可指定新的空输出目录，`--postgres-image` 可指定隔离数据库镜像。

数据库必须能够在构建机器的 Docker 中启动。当前流程面向本机构建单一平台；不同 CPU 架构应分别构建、训练和验收。镜像仓库不可访问时不会声称远程基础镜像已验收。

## 部署

生产配置使用独立的 application-prod.yaml，配置文件内填写具体值，不使用环境变量占位符。模板和部署命令见下文正式部署流程。

缓存镜像默认 `cached` 模式，以 exec 启动 Java，容器停止信号直接传给 JVM。readiness 地址为 `/actuator/health/readiness`，可由部署平台探测。无需在运行镜像安装 Python、Maven、Node 或 Docker CLI。

需要执行数据库迁移时，使用普通模式、开启 SPRING_LIQUIBASE_ENABLED，并通过 `JAVA_TOOL_OPTIONS=-Dspring.context.exit=onRefresh` 在初始化后退出；这是独立的部署步骤，不在缓存模式启动时执行。正式执行迁移前按项目数据库发布流程准备。

训练与缓存启动使用 `avalon-app/src/main/docker/jvm.options` 中的 `-Xms1g -Xmx1g`，本机基准也读取该文件。这是 2C4G、应用与数据库同机的稳定延迟基线，不等于容器资源配额或已验证的并发容量。堆外内存、数据库和操作系统需要另留空间。需要调整 JVM 参数时修改该文件并重新构建、训练，不能直接复用旧缓存。缓存模式拒绝 JAVA_TOOL_OPTIONS 等额外 JVM 环境参数；普通模式仍可用于诊断。

初始堆与最大堆相等，避免生产请求期间因为堆扩缩产生延迟波动；固定 1GB 堆并不代表 JVM 进程总内存上限为 1GB。本项目不默认启用 AlwaysPreTouch。应用独占至少 4GB 内存并经过压测后，才可将两者一起提高到 1536MB。

应用 JVM 同时固定 UTF-8、`user.language=zh`、`user.country=CN` 和 `user.timezone=Asia/Shanghai`。容器设置 `TZ=Asia/Shanghai` 与 `LANG=C.UTF-8`：操作系统使用可用的 UTF-8 locale，Java 中文区域不依赖镜像安装 zh_CN locale。直接从 IDE 或 java 启动时也应使用该 JVM 参数文件。前端日期显式按 zh-CN、Asia/Shanghai 展示；数据库时间点与 API 的 Instant 仍保留原有语义。变更区域或时区参数后同样重新训练缓存。

Spring AOT 采用 prod Bean 图，不允许运行时改成 test/local、开启迁移器或将调度配置改成 off。更多配置条件及缓存边界见 [AOT 部署](aot-deployment.md)。应用制品、JDK 或架构变化均重新训练。

本地验收完成后才手动推送；构建脚本没有任何推送步骤。需要推送时可以显式执行 `docker push 镜像标签`，或使用插件的 `docker:push`。

依据：[Fabric8 官方文档](https://dmp.fabric8.io/)、[Spring Boot 缓存打包](https://docs.spring.io/spring-boot/how-to/aot-cache.html)。

## 正式部署流程

同机 2C4G 预算：应用容器上限 2GB（包含固定 1GB 堆、堆外内存与 tmpfs），数据库约 1GB，操作系统与代理约 1GB。数据库预算须在其部署配置单独落实。Compose 限制应用 CPU 为 2 核、内存为 2GB、禁用应用交换空间；这不是独占 CPU 保证。Maven 构建应在开发机或 CI 执行。当前配置未经容量压测，固定堆不能消除 GC 或数据库等待。

当前使用已下载验证的 Corretto 27 官方基础镜像并固定摘要。更新基础镜像时同步修改 POM 与 Dockerfile 的默认值，重新构建、训练和验收。发布环境填写最终应用仓库摘要，保留上一版本摘要用于回滚。

生产部署使用 [deploy/compose.prod.yaml](../deploy/compose.prod.yaml) 和受保护的 `deploy/application-prod.yaml`。镜像标签必须使用不可变版本或摘要，禁止使用 `latest`。数据库使用独立 PostgreSQL 实例，应用账号只授予业务数据库所需权限，`cosid.machine.distributor.manual.machine-id` 在每个应用实例间唯一。

首次部署或升级数据库时，先执行一次迁移容器：

```bash
docker run --rm --read-only --tmpfs /tmp:rw,nosuid,size=128m \
  --volume "$(pwd)/deploy/application-prod.yaml:/application/config/application-prod.yaml:ro" \
  -e SPRING_LIQUIBASE_ENABLED=true \
  -e JAVA_TOOL_OPTIONS=-Dspring.context.exit=onRefresh \
  registry.example.com/avalon:版本 plain
```

确认迁移退出码为 0 后，再启动缓存应用：`docker compose -f deploy/compose.prod.yaml up -d`。部署平台探测 `/actuator/health/readiness`；只有 readiness 为 UP 才接收流量。日志输出到标准输出，由平台收集并限制保留周期；不要把数据库密码写入日志或镜像。

复制 application-prod.yaml.example 为 application-prod.yaml，填写实际配置并设置 owner=root、mode=600；Compose 以只读挂载提供给应用。直接编辑 compose.prod.yaml 的 image 为已验收摘要。真实配置已加入 Git 忽略，模板不能直接用于生产。单实例升级使用维护窗口，先停止入口流量及旧应用，再执行备份、迁移和新应用启动。迁移采用 onRefresh 退出，不执行 ApplicationRunner 调度启动逻辑；它仍会执行 Spring Bean 初始化，不能替代数据库备份。

HTTPS 由主机反向代理终止，8080 仅绑定回环地址，代理不对外开放 /actuator。主机执行 `curl --fail http://127.0.0.1:8080/actuator/health/readiness`，返回 200 且 status=UP 后恢复流量。liveness 探测地址为 `/actuator/health/liveness`。readiness 失败时摘流量，持续 liveness 失败时由监控平台决定重启；Compose 本身不会因 HTTP 探测失败自动重启。容器日志轮转为每份 10MB、保留 3 份，长期日志由平台另行收集。

每天在数据库主机执行 [scripts/backup-postgres.sh](../scripts/backup-postgres.sh)，备份必须写入独立存储并至少保留最近 7 个日备份。升级前先执行一次带时间戳的备份，并记录当前镜像摘要和 Liquibase 版本。

安装与数据库同版本的 PostgreSQL 客户端，通过受保护的 .pgpass 或环境提供认证；设置 PGHOST、PGUSER、PGDATABASE 和 BACKUP_DIR 后执行 `sh scripts/backup-postgres.sh`。脚本只在导出成功且目录可解析后公布最终文件名，不自动删除备份；异机存储生命周期负责保留。可用 cron 每日执行。使用 `pg_restore --list 备份文件` 检查目录，再在隔离实例执行 `createdb avalon_restore_check` 与 `pg_restore --exit-on-error --no-owner --no-acl --dbname=avalon_restore_check 备份文件`，连接验证应用检查角色、资产及挂单。目录可解析不代表恢复成功，必须定期演练；不要对生产库做恢复演练。

当前方案为逻辑备份，不能保证零数据丢失；如需更低 RPO，应另行配置 WAL 归档与 PITR。数据库恢复会丢失备份之后的数据，切换前必须明确接受该损失。

回滚只回滚应用镜像到上一个已验收摘要；不要自动回滚已经执行的数据库变更。若新版本需要不可逆结构变更，必须先完成兼容旧应用的迁移阶段，再切换镜像。恢复数据库时停止应用、从备份恢复到临时实例验证，再按发布流程切换连接地址。

应用回滚命令仍为 `docker compose -f deploy/compose.prod.yaml up -d`，执行前将 compose.prod.yaml 的 image 改为上一版本摘要，执行后重复 readiness 与业务检查。不要使用 `down -v`，也不要自动执行 Liquibase rollback。

## 文件一致性与待填写项

编译入口为 Maven Wrapper 3.9.16、revision/Flatten 统一版本；应用镜像为固定摘要的 Corretto 27，root 运行，固定 1GB 堆，中文、UTF-8、Asia/Shanghai。生产配置通过独立 YAML 挂载，不使用环境变量占位符。Compose 的 registry.example.com 是待填写的发布仓库示例，application-prod.yaml.example 中的数据库、密码和机器号必须按生产环境填写。代码配置完成不等于已推送镜像或已部署。

verify-resource-budget.mjs 是独立的资源对比场景，仍采用应用 1CPU/3GB、数据库 1CPU/1GB 及百分比堆，不是生产默认值；其默认 JDK 来源已统一为 Corretto 27。两个验收脚本均支持 AVALON_APP_JAR 指向其他 revision 的制品；运行前按各脚本要求准备隔离数据。
