# 发布耗时核查

## 线上基线

2026-11-17 通过 GitHub 公共 REST API 读取 docker-publish.yaml 最近五次运行的作业与步骤元数据，耗时按步骤开始、结束时间计算。

| 运行 | 结果 | 作业耗时 | 测试与镜像构建 | 推送 |
| --- | --- | --- | --- | --- |
| [44045276427](https://github.com/lishangbu/avalon/actions/runs/44045276427) | 取消 | 45分40秒 | 45分03秒，取消 | 未执行 |
| [44030988906](https://github.com/lishangbu/avalon/actions/runs/44030988906) | 取消 | 45分49秒 | 44分57秒，取消 | 未执行 |
| [43796520660](https://github.com/lishangbu/avalon/actions/runs/43796520660) | 成功 | 14分19秒 | 13分41秒 | 5秒 |
| [40179732706](https://github.com/lishangbu/avalon/actions/runs/40179732706) | 成功 | 18分44秒 | 18分08秒 | 4秒 |
| [39834171415](https://github.com/lishangbu/avalon/actions/runs/39834171415) | 失败 | 18分44秒 | 18分37秒 | 未执行 |

瓶颈落在合并的测试与构建步骤；已有成功记录中的镜像推送只占数秒。现有元数据不能进一步区分 Maven 下载、前端、测试和镜像构建的占比。日志下载 API 返回 403，未获得日志级缓存命中证据。

同日读取 Actions 缓存清单，共五条，包含 JDK、Wrapper 和一条约216 MB 的 Maven 缓存，没有 npm 或 Docker 构建层缓存。Maven 缓存最后访问时间为11月16日，不能据此声称17日两次运行已命中。

## 已实施的调整

- 移除长时间自然成长路线测试及其专用工作流，保留任务阶段、制造、采集与资产操作专项回归。
- 发布流程分别显示前端安装、前端构建、后端验证、镜像构建与发布的耗时；前端只构建一次，后端资源复制和打包内容验证仍执行。
- Maven 使用显式恢复、保存和版本键回退，在镜像阶段之前保存依赖；不缓存编译产物。
- npm 缓存下载内容，仍按锁文件重新安装；Node 版本与 mise.toml 一致。
- 使用 Buildx 的 GitHub Actions 缓存导入、导出，保留现有 Dockerfile 的 Spring Boot 分层布局。路径上下文明确包含本次验证通过的 application.jar，只有 verify 成功才发布。
- 合作测试只准备路线所需的七个任务事实，保留 J025 跨城捷径，避免改变原有成本测量路线；真实采集心跳、资源恢复、市场交易、请求重放和制造流程继续运行。

参数依据：[setup-node](https://github.com/actions/setup-node)、[Actions cache](https://github.com/actions/cache)、[Buildx 构建与推送](https://github.com/docker/build-push-action)。本地 Maven docker 配置继续可用。

## 复测口径

LifeCooperationTest 优化前单独运行5项全部通过，耗时144.9秒；优化后5项全部通过、无跳过，耗时66.55秒，减少约54%。这是同一机器的两次单类测量，仍受运行环境波动影响。此前整体验证中的126.1秒来自另一轮完整构建，不与本次单类基线混用。

本地按新流程执行 npm ci、npm run build，通过类型检查和生产构建；再以 skip.frontend=true 执行 clean verify，选择 PackagedContentIT（本轮不重复运行其他单元测试），结果1项通过、无跳过。真实 JAR 在空 PostgreSQL 启动，首页和17个静态资源通过 HTTP 验证，认证、物品详情、临时课程消耗及永久库存保持也通过。Java 格式校验随 Maven 执行通过。

actionlint 1.7.12 静态检查通过；其内置运行器列表尚未收录现有工作流已使用的 ubuntu-26.04，本轮仅忽略该标签提示，未更换运行器或忽略其他检查。

使用本次通过打包验证的 JAR、本仓库 Dockerfile 和相同上下文布局，执行 docker buildx build --load 成功，镜像保存在本地 avalon:ci-verify；分层提取和最终镜像导出均成功。本地没有执行远程镜像推送或 GitHub Actions 缓存导入、导出。

推送后需分别观察首次冷缓存运行和至少一次同分支热缓存运行，记录各阶段耗时、Maven/npm 命中信息、Buildx 的 CACHED 输出及测试数量。不能以本地测试耗时推断 GitHub 托管运行器的发布耗时，也不能在尚未执行新工作流时宣称线上缓存已经命中。
