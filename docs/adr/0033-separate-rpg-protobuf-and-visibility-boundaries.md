---
status: superseded
superseded_by: 0036
---

# 分离 RPG Protobuf 与玩家/管理员地图可见边界

地图、Discovery、Traversal、Pending Encounter、Checkpoint 与 RPG 读取契约放入独立的 `avalon.rpg.v1` Protobuf 上下文，不继续扩张通用 `avalon.domain.v1`。玩家写入和读取服务只在玩家进程注册，管理员服务只提供完整拓扑与校验报告的只读读取；两者使用各自 Bearer 安全域，不使用 Cookie 或 CSRF。

玩家读取返回当前位置、已发现子图、展示投影、当前可达性和稳定原因码，不泄露未发现节点、Encounter 权重、随机种子或内部校验细节。管理员读取可分页查看完整 Region/Location/Exit、条件、遭遇资料、Projection 版本和 Topology Integrity Report。地图读取使用有界 opaque cursor，并绑定读取时间、稳定排序键和必要的资源版本摘要；不引入或持久化全局 topology revision。

该边界把玩家可行动信息与管理审计资料分开，避免通过同一 DTO 或通用领域服务意外扩大信息披露范围；代价是需要维护两个视图投影和独立的认证注册。

首期地图与 Encounter 只提供 unary 查询和命令响应，不新增 Map Watch 流；Battle 的参与者披露流继续独立存在。RPC 错误以稳定 Reason Code 和简体中文 message 表达，并使用 Kratos/gRPC/Connect 状态映射，客户端不能依赖 HTTP 状态文本分支。
