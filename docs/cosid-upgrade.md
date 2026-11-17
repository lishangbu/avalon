# CosId 5 升级与部署

本次使用 CosId BOM 5.0.1 和 Spring Boot 4.1.2。旧的 `IdGeneratorProvider`、共享生成器名称、机器号分配配置和注册器依赖均已移除。

## 已启用的 5.x 能力

- Starter 为每个 Spring 上下文管理独立 `CosIdRuntime`，关闭上下文时释放运行时；业务服务和 Hibernate 使用同一 `default` 生成器，避免静态单例跨上下文共享。
- 使用 `Layout.SNOWFLAKE_MS` 保留旧纪元 `2019-12-24T16:00:00Z` 及 41/10/12 位布局。数据库主键仍为 Long，API 仍为十进制字符串；显式配置十进制 codec，可通过运行时编码、解码与 `Layout.unpack` 排查 ID。
- 时钟回拨最多 5ms 时借用逻辑时间，超过 5ms、最多 1s 时等待时钟恢复，更大回拨直接拒绝发号，避免无限等待。
- 静态机器槽位使用状态文件持久化发号时间窗口，保护跨重启时钟回拨。生产挂载 `deploy/cosid-state/` 到 `/application/cosid`，本地使用 `.cosid/local-slot-0.state`。测试不落共享文件，使用隔离数据库与独立槽位。
- 启用 `cosid.generator.generated` function counter；Starter 自动接入 Actuator 健康检查、时钟回拨等指标。仅暴露原有 health HTTP 端点，运行时计数也可通过 `CosIdRuntime.generated("default")` 读取。

静态槽位仍由部署方保证唯一，范围为 0–1023。生产默认 1；多个实例必须分别设置 `COSID_MACHINE_STATIC_SLOT`，并分别使用独立、持久化的状态目录。不要让两个进程同时使用相同槽位，也不要复制或删除正在使用的状态文件。默认提前持久化窗口为 10s，异常退出后重启可能等待该窗口过去；应给启动探针留出余量。

## 从 4.x 首次切换

当前项目仅使用固定机器号的毫秒雪花算法，布局未改变，可采用官方支持的停机迁移，无需创建 CosId 存储表或修改业务数据库结构。

1. 备份数据库，记录旧镜像摘要与配置，关闭入口流量，停止 `avalon` 命名空间的全部 4.x 实例，记录最后停止时间。4.x 和 5.x 不能混跑。
2. 检查所有主机 NTP 同步。停机至少 1 分钟，且不短于最大主机时钟偏差；计入旧版允许的最多 10s 超前。无法确认时钟或完整停机时，先按官方迁移工具流程核验。
3. 创建并确认 `deploy/cosid-state/` 可由容器用户读写。核对每个实例的唯一槽位和独立状态目录。
4. 首次启动还可设置 `COSID_MACHINE_STATIC_NOT_BEFORE` 为“最后停止时间 + 最大时钟偏差”的实际 UTC 时间戳，作为首次创建状态文件的下界；不要照抄固定示例日期。
5. 部署新镜像，检查 readiness、`/actuator/health/cosid` 和真实业务写入，再恢复流量。保留状态目录供后续重启使用。

若回退到 4.x，同样先停止所有 5.x 实例，等待足够时钟偏差窗口并核对 NTP，保留旧布局与机器号唯一性；不能直接滚动回退或让新旧版本并行发号。

## 验证入口

`CosIdRuntimeTest` 验证小幅时钟回拨继续单调发号、大幅回拨拒绝及状态文件重启后不重复。`CosIdPersistenceTest` 使用真实 PostgreSQL 验证持久化发号时机、事务回滚不复用、旧布局、十进制 codec 和运行时指标。

官方依据：[4.x → 5.x 迁移](https://cosid.ahoo.me/guide/migration/from-4x.html)、[配置参考](https://cosid.ahoo.me/reference/config/basic.html)、[指标与健康检查](https://cosid.ahoo.me/guide/operations/monitoring.html)。
