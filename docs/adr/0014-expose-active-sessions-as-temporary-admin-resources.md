# Expose active Sessions as temporary admin resources

Avalon 通过 `/api/battle-sessions` 向管理端暴露进程内 Battle Session：创建接口返回 `201`；列表只分页查询活跃和 Recent Session，并支持状态与 formatCode 筛选；详情返回当前状态、revision 和 Turn Requirements；turn 子资源同步执行 Turn Command 或读取仍在内存中的 Turn Record；termination 子资源以 commandId 和 expectedRevision 幂等终止。Session 被 TTL 或近期容量淘汰后，详情与回合接口返回 404；接口不提供修改、删除、永久历史或 Battle Record 查询。当前 REST 使用 `battle-sessions:run` 权限并同步返回执行结果，未来实时客户端通过独立游戏网关适配同一 Session Runtime。
