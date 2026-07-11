# Avalon Backend

定义 Avalon 后端在游戏资料与可执行战斗规则中使用的稳定领域语言。

## Language

### Game Data

**Current Game Data**:
与 NationalDex 当前状态一致的唯一权威资料快照；版本、世代和历史不属于该模型。
_Avoid_: Versioned Data, Historical Dataset, Generation Data

**Internal Code**:
来源于外部页面 slug、但由 Avalon 自己拥有的稳定机器标识；它不是来源追踪信息。
_Avoid_: Source Slug, Source URL

**Form Inheritance**:
同一物种的形态复用其基准生物未变化资料的关系。
_Avoid_: Version Inheritance, Generation Inheritance

**Identifier**:
后端持有的长整型记录身份，在 JSON 契约中作为不透明十进制字符串表达。
_Avoid_: Numeric JSON ID, Sequence Number

**Support Data**:
因进化或战斗规则必须可执行而保留的资料，即使它不属于当前公开资料；它不被视为历史数据。
_Avoid_: Historical Data, Legacy Data

### API Contract

**Admin API Contract**:
后端为管理端消费者提供的权威机器可读接口描述，包含字段存在性、可空性和 Identifier 形态。
_Avoid_: Frontend DTO, Local API Shape

### Battle Rules

**Executable Battle Rule**:
运行时能够执行、且所有资料引用都必须解析到 Current Game Data 或明确 Support Data 的战斗规则。
_Avoid_: Battle Metadata, Historical Rule

### Battle Execution

**Battle Session**:
服务端单节点持有的一场短生命周期战斗执行过程；它拥有双方、权威运行态和已结算回合，但不表达玩家归属，也不是数据库记录。
_Avoid_: Match, Room, Sandbox Replay, Battle Session Table

**Session Runtime**:
在内存中独占一个 Battle Session 并串行处理其命令的执行容器；当前实现可以是进程内组件，未来可由按 sessionId 分片的游戏节点承载。
_Avoid_: Database Transaction, Distributed Lock, Match

**Runtime Capacity**:
单个 Session Runtime 可承载的活跃与近期会话上限；达到活跃上限时拒绝新建，绝不通过淘汰运行中会话释放容量。
_Avoid_: Rate Limit, Database Pool Size, Recent Session TTL

**Session Identifier**:
服务端生成且永不复用的 UUID v4 字符串，用于标识和路由一个内存 Battle Session；它不是数据库长整型 Identifier。
_Avoid_: CosId, Database ID, Numeric Session ID

**Session Roster**:
创建 Battle Session 时由调用方提交的阵容配置，只引用当前资料并描述成员构成；服务端据此生成场内标识并冻结可执行初始状态。
_Avoid_: Battle State, Rule Snapshot Input, Client Actor Identity

**Turn Command**:
请求 Battle Session 恰好推进一次的完整回合命令；它携带幂等标识、预期会话版本以及已聚合的全部行动。
_Avoid_: Action Submission, Player Move, Sandbox Turn Request

**Turn Requirements**:
Battle Session 根据当前权威状态派生的下一回合人工选择要求；它排除锁招、蓄力等引擎自动行动，并作为 Turn Command 人工选择集合完整性的唯一判据。跨行动的组合合法性（例如两个席位换入同一成员）仍由共享 Battle Action Validator 一次性校验。
_Avoid_: UI Validation, Match Rule, Available Moves

**Turn Record**:
Session Runtime 成功执行 Turn Command 后追加的内存回合事实，包含固定顺序的行动、随机轨迹、事件增量和结算时刻；权威当前状态属于 Session Snapshot，历史状态可由初始状态和 Turn Record 前缀严格重放得到。
_Avoid_: Sandbox Replay Pair, Database Transaction, Action Submission

**Battle Record**:
Session Runtime 在会话结束时生成的不可变诊断与复盘材料；首版只随 Recent Session 保留，未来可以异步归档，但它不是恢复 Session 的权威来源。
_Avoid_: Battle Session, Match Result, Runtime State

**Recent Session**:
已经完成或终止、但仍在 Session Runtime 的受限终态缓存中供结果确认的 Battle Session；它会因 TTL 或内存预算被淘汰。
_Avoid_: Battle Record, Persistent History, Active Session

**Random Trace**:
服务端在成功执行 Turn Command 时实际消费并随 Turn Record 保留的有序随机值；它用于复盘，不是调用方可选择的种子。
_Avoid_: Client Random Seed, Retry Seed, Predicted Randomness

**Session Termination**:
由管理员或外部协调者停止仍在运行的 Battle Session；它不伪造引擎结果，也不替 Match 判断真人胜负。
_Avoid_: Battle Completion, Forfeit Result, Failed Turn

**Match**:
真人参与的持久竞争过程，负责把玩家映射到 Battle Session 的双方，并协调各方提交、超时、运行节点丢失和胜负确认。
_Avoid_: Battle Session, Sandbox

**Interrupted Match**:
因承载 Session Runtime 的节点丢失而无法继续、且不产生胜负的 Match；它允许调用方重新匹配或重新开始。
_Avoid_: Completed Match, Session Termination, Forfeit
