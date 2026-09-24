# AOT 验证范围

当前部署基线为 Corretto 27、root、固定 1GB 堆，应用容器上限 2GB／2核。构建和缓存训练命令见 [AOT 部署](aot-deployment.md) 与 [Docker 部署](docker-deployment.md)。

scripts/docker-aot.py 验证隔离数据库迁移、独立配置文件、强制缓存加载、运行 UID、只读根文件系统以及注册、创角和地图读取。结果在所选输出目录的 result.json，失败时检查同目录日志。

scripts/aot.py benchmark 比较普通 JAR、解包、Leyden、Spring AOT 和组合模式。必须使用当前制品重新采样，记录 JDK、主机、JVM 参数、重复次数及 RSS/PSS。启动时间不能代替请求延迟，进程内存不能等同 Java 堆。

当前没有真实 2C4G 环境的完整负载及 P95／P99 延迟验收；不能由启动冒烟推断在线人数。缓存训练仅覆盖启动，不包含全部业务热点。正式发布还需固定应用镜像摘要、填写数据库配置并执行生产迁移。
