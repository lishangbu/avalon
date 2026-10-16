# Docker 镜像与生产部署

`docker` Maven profile 使用 Fabric8 docker-maven-plugin 0.49.0，只在 avalon-app 构建应用镜像，不自动推送。版本由 avalon-parent 统一管理。普通 Maven 构建不访问 Docker。

## 构建本地应用镜像

```bash
mise exec -- ./mvnw -Pdocker clean verify
```

默认生成 `slf4j/avalon:latest`，基础镜像为 Docker Hub 官方 `ibm-semeru-runtimes:open-25-jdk`。可以用 `-Davalon.image=仓库/avalon:版本` 指定应用标签，用 `-Davalon.base-image=镜像@sha256:摘要` 固定 JDK 基础镜像。请使用已验证可获取的 IBM Semeru Runtime Open Edition JDK 25 镜像。

默认构建不访问 ECR Public，避免其匿名下载额度耗尽时出现 `Data limit exceeded`。镜像来源见 [IBM Semeru Runtime Open Edition 官方下载页](https://hub.docker.com/_/ibm-semeru-runtimes)。

Dockerfile 使用 `extract --layers` 分层解包 JAR，通过 `java @jvm.options -jar application.jar` 启动。应用继承 IBM Semeru Runtime Open Edition 基础镜像的 root 身份，监听 8080。

## GitHub Actions 发布到 Docker Hub

[镜像发布工作流](../.github/workflows/docker-publish.yaml)在推送 `main` 分支时自动执行，也可在Actions页面选择 `main` 手动触发。它使用 IBM Semeru Runtime Open Edition 25及现有 `docker` Maven profile，先使用已有Docker Hub凭据登录，再执行 `clean verify` 后发布 `latest`；不需要创建版本标签。

在Docker Hub创建应用仓库，并在GitHub仓库的Settings → Secrets and variables → Actions中设置：

- Secret `DOCKERHUB_USERNAME`：具有目标仓库推送权限的Docker Hub账号。
- Secret `DOCKERHUB_TOKEN`：该账号的Docker Hub访问令牌，授予目标仓库写入权限。

每次成功构建都会更新 `docker.io/slf4j/avalon:latest`。自动及手动发布均使用 `main` 分支，避免其他分支覆盖生产镜像。镜像目前仅构建 `linux/amd64`。生产部署使用 `latest` 时，更新前执行 `podman compose -f deploy/compose.yaml pull avalon`，再重建应用容器。

工作流只负责构建和发布，不自动修改生产Compose或部署服务。Compose已使用 `docker.io/slf4j/avalon:latest`，无需另行配置镜像名称。凭据未配置前工作流无法发布；本地检查不代表GitHub远程任务已执行。

## 部署

生产部署统一使用 `deploy/compose.yaml`，数据库地址、用户名和单实例机器号写在 `avalon-app/src/main/resources/application-prod.yaml` 中，容器配置写在Compose中。密码独立保存在同级 `postgres-password.txt`，通过文件来源的 `pg_password` secret供两个服务共用：数据库读取 `/run/secrets/pg_password`，应用通过Spring配置树读取 `/run/secrets/spring.datasource.password`。无需执行 `podman secret create`，也不需要example配置模板。

应用镜像直接启动 Java。readiness 地址为 `/actuator/health/readiness`，可由部署平台探测。无需在运行镜像安装 Python、Maven、Node 或 Docker CLI。

生产数据库由部署方手动维护，Compose通过 `SPRING_LIQUIBASE_ENABLED=false` 关闭自动迁移。应用只执行Hibernate结构校验（`ddl-auto=validate`），不会创建或修改数据库结构；结构与实体不匹配时会启动失败。

应用启动使用 `avalon-app/src/main/docker/jvm.options` 中的固定 1GB 堆、Gencon GC、吞吐优先调优和 OpenJ9 共享类缓存；本机基准也读取该文件。堆外内存、数据库和操作系统需要另留空间。修改该文件后重新构建镜像；临时 JVM 参数可以通过 `JAVA_TOOL_OPTIONS` 提供。共享类缓存目录通过 Compose 的 `./openj9-cache` 持久化，用于后续重启；首次启动不会直接受益。部署前确保该目录可由容器用户读写。

默认不启用 AlwaysPreTouch；应用独占至少 4GB 内存并经过压测后，才可将初始堆和最大堆一起提高到 1536MB。

应用 JVM 固定 UTF-8、`user.language=zh`、`user.country=CN` 和 `user.timezone=Asia/Shanghai`；容器设置 `TZ=Asia/Shanghai` 与 `LANG=C.UTF-8`。IDE 或直接 Java 启动也使用该 JVM 参数文件。前端日期按 zh-CN、Asia/Shanghai 展示，数据库时间点与 API 的 Instant 语义不变。

本地构建不自动推送；GitHub发布工作流在测试通过后推送。需要推送时可以显式执行 `docker push 镜像标签`，或使用插件的 `docker:push`。

依据：[Fabric8 官方文档](https://dmp.fabric8.io/)。

## 正式部署流程

同机 2C4G 预算：应用容器上限 2GB（包含固定 1GB 堆、堆外内存与 tmpfs），数据库约 1GB，操作系统与代理约 1GB。数据库预算须在其部署配置单独落实。Compose 限制应用 CPU 为 2 核、内存为 2GB、禁用应用交换空间；这不是独占 CPU 保证。Maven 构建应在开发机或 CI 执行。当前配置未经容量压测，固定堆不能消除 GC 或数据库等待。

当前使用已确认镜像清单可获取的 IBM Semeru Runtime Open Edition 25 官方基础镜像标签。更新基础镜像时同步修改 POM 与 Dockerfile 的默认值，重新构建和验收。发布环境填写最终应用仓库摘要，保留上一版本摘要用于回滚。

生产部署使用唯一的 [deploy/compose.yaml](../deploy/compose.yaml)，包含 `postgres` 和 `avalon` 两个服务。数据库使用腾讯云镜像地址，保留 `postgres` 数据库和账号；数据直接挂载到同级 `postgresql/` 目录。应用连接内部服务名 `postgres`，容器、数据库与日志均使用东八区。应用机器号在 `application-prod.yaml` 中配置，每个应用实例必须唯一；多实例部署时通过 `COSID_MACHINE_DISTRIBUTOR_MANUAL_MACHINE_ID` 分别覆盖。

应用默认使用 `docker.io/slf4j/avalon:latest`。应用直接使用镜像入口，无需设置启动命令。准备密码文件并启动：

```bash
install -d -m 755 deploy/postgresql
# 首次部署时创建密码；已有文件不要覆盖。
(set -euC; umask 077; openssl rand -hex 32 > deploy/postgres-password.txt)
chmod 600 deploy/postgres-password.txt
podman compose -f deploy/compose.yaml up -d
# 使用Docker时：docker compose -f deploy/compose.yaml up -d
```

首次部署前手动准备数据库结构。Compose等待数据库健康后启动应用；结构校验失败时查看 `podman compose -f deploy/compose.yaml logs avalon`，手动修正数据库结构后重新启动应用。部署平台探测 `/actuator/health/readiness`，只有readiness为UP才接收流量。密码通过文件来源的Compose secret提供给两个服务，不写入Compose、应用镜像或日志。

`deploy/postgres-password.txt` 权限为600并已加入Git忽略。文件secret使用只读挂载；应用保留读取宿主机普通用户持有的600权限文件所需的DAC_OVERRIDE能力。Podman使用 `podman-compose` 执行器，`x-podman.relabel: z` 为共享secret设置SELinux标签；Docker Compose忽略该扩展。数据库独占目录使用 `Z`。密码文件只影响数据库首次初始化；已有数据库轮换密码时，先修改数据库角色密码，再同步密码文件并重建相关容器。切换旧命名卷时，数据不会自动搬入 `postgresql/`，必须先完成迁移。腾讯云镜像地址需在部署网络验证可达性。

单实例升级使用维护窗口，先停止入口流量及旧应用，完成数据库备份并手动执行本次发布需要的结构或数据变更，再更新应用镜像并启动：

```bash
podman compose -f deploy/compose.yaml stop avalon
# 手动完成数据库备份与本次发布需要的数据库变更，再更新应用镜像。
podman compose -f deploy/compose.yaml up -d --force-recreate avalon
```

数据库维护前先完成备份。自动重启策略只处理容器进程退出；Podman主机重启恢复还需启用 `podman-restart.service`，rootless运行时同时启用用户linger。

HTTPS由主机反向代理终止，8080仅绑定回环地址，代理不对外开放 `/actuator`。主机执行 `curl --fail http://127.0.0.1:8080/actuator/health/readiness`，返回200且status=UP后恢复流量。liveness地址为 `/actuator/health/liveness`；Compose本身不会因HTTP探测失败自动重启。各容器日志配置为 `json-file`、单文件上限10MB；Podman将该驱动名映射为k8s-file，不配置Docker专属的多文件保留参数。长期日志由平台另行收集。

每天在数据库主机执行 [scripts/backup-postgres.sh](../scripts/backup-postgres.sh)，备份必须写入独立存储并至少保留最近 7 个日备份。升级前先执行一次带时间戳的备份，并记录当前镜像摘要和 Liquibase 版本。

安装与数据库同版本的 PostgreSQL 客户端，通过受保护的 .pgpass 或环境提供认证；设置 PGHOST、PGUSER、PGDATABASE 和 BACKUP_DIR 后执行 `sh scripts/backup-postgres.sh`。脚本只在导出成功且目录可解析后公布最终文件名，不自动删除备份；异机存储生命周期负责保留。可用 cron 每日执行。使用 `pg_restore --list 备份文件` 检查目录，再在隔离实例执行 `createdb avalon_restore_check` 与 `pg_restore --exit-on-error --no-owner --no-acl --dbname=avalon_restore_check 备份文件`，连接验证应用检查角色、资产及挂单。目录可解析不代表恢复成功，必须定期演练；不要对生产库做恢复演练。

当前方案为逻辑备份，不能保证零数据丢失；如需更低 RPO，应另行配置 WAL 归档与 PITR。数据库恢复会丢失备份之后的数据，切换前必须明确接受该损失。

回滚只回滚应用镜像到上一个已验收摘要；不要自动回滚已经执行的数据库变更。若新版本需要不可逆结构变更，必须先完成兼容旧应用的迁移阶段，再切换镜像。恢复数据库时停止应用、从备份恢复到临时实例验证，再按发布流程切换连接地址。

应用回滚命令仍为 `docker compose -f deploy/compose.yaml up -d`，执行前将 compose.yaml 的 image 改为上一版本摘要，执行后重复 readiness 与业务检查。不要使用 `down -v`，也不要自动执行 Liquibase rollback。

## 文件一致性与待填写项

编译入口为Maven Wrapper 3.10.0、revision/Flatten统一版本；应用镜像为 IBM Semeru Runtime Open Edition 25，root运行，固定1GB堆，中文、UTF-8、Asia/Shanghai。应用非敏感配置随 `application-prod.yaml` 打包，Compose保存容器配置，同级密码文件独立管理。应用发布仓库固定为 `slf4j/avalon`，默认标签为 `latest`；机器号需按实际实例分配。代码配置完成不等于已推送镜像或已部署。

verify-resource-budget.mjs 是独立的资源对比场景，仍采用应用 1CPU/3GB、数据库 1CPU/1GB 及百分比堆，不是生产默认值；其默认 JDK 来源已统一为 IBM Semeru Runtime Open Edition 25。两个验收脚本均支持 AVALON_APP_JAR 指向其他 revision 的制品；运行前按各脚本要求准备隔离数据。
