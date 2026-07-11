# Isolate Session Runtime in a pure Kotlin module

Avalon 新增只依赖 `battle-engine` 的纯 Kotlin `battle-session` 模块，用于承载 Session Runtime、生命周期、revision、命令幂等、Turn Requirements、终态缓存和进程内注册表；该模块不依赖 Spring、Jimmer、Jackson、数据库或具体网络传输，并只接收已经装配完成的 BattleInitialState。`battle-rules` 继续负责从 Current Game Data 装配可执行初始状态，并提供当前 REST/OpenAPI 适配器。首版以每个 Session 独立串行化实现单所有者语义，不引入协程或 actor 框架；未来 WebSocket、游戏网关或实时 tick 调度可以通过相同 Runtime 边界接入，而不复制管理 Controller 逻辑。
