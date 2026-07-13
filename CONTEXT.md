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

**Trainer Turn Submission**:
Trainer 向 Match 提交的本方完整回合行动；一经接受即锁定，双方提交齐全后由 Match 按稳定顺序聚合为一个 Turn Command。
_Avoid_: Turn Command, Individual Action, Editable Draft

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
真人参与的持久竞争过程，负责把不同账户所属的双方 Trainer 映射到 Battle Session 的双方，并协调各方提交、超时、运行节点丢失和胜负确认。同一账户拥有的全部 Trainer 同时至多参与一场活跃 Match。
_Avoid_: Battle Session, Sandbox

**Match Runtime**:
在内存中协调一个活跃 Match 的双方 Trainer Turn Submission、行动期限和 Battle Session 调用的执行容器；它的丢失会使持久 Match 进入 Interrupted Match，而不是恢复或接续对局。
_Avoid_: Match, Session Runtime, Database Transaction

**Match View**:
Match 为当前 Trainer 投影的对局可见事实，包含己方完整信息、对方公开阵容和已经由战斗事件揭示的信息；它不是 Battle Session 的完整权威快照。
_Avoid_: Battle Session Snapshot, Admin View, Shared Match DTO

**Match Result**:
Completed Match 的不可变最终裁决，由结果形态、完成原因、可选胜者和可选战斗引擎原因共同表达；Interrupted Match 不拥有 Match Result。
_Avoid_: Battle Result, Interruption Reason, Session Termination

**Match History**:
Trainer 参与过的终态 Match 的持久记录集合；它不是 Public Trainer Profile，也不会突破 Match View 对对方隐藏信息的限制。
_Avoid_: Public Battle Record, Leaderboard, Trainer Statistics

**Disclosure Ledger**:
Match 按双方视角持久记录的对方已揭示技能、特性和道具集合；它用于恢复 Match History 的可见信息，但不是事件日志、随机轨迹或战斗回放。
_Avoid_: Battle Record, Turn Record, Replay, Audit Log

**Forfeit**:
Trainer 在 Active Match 中主动放弃对局并确认对方获胜的 Match 裁决；它不是 Battle Session 的引擎结果或单纯的 Session Termination。
_Avoid_: Session Termination, Battle Completion, Match Timeout

**Trainer**:
登录账户拥有的持久游戏身份，具有全局唯一且不可修改的展示名称；一个账户最多拥有三个有效 Trainer，归档 Trainer 会释放名额但保留名称、历史 Match 与战绩。
_Avoid_: Player, Security User, Account, Match Side, Actor

**Public Trainer Profile**:
通过规范化后的完整展示名称精确查找时返回的最小公开身份，只包含展示名称、在线状态和是否可挑战；它不暴露内部 Trainer Identifier、账户、队伍、对局历史、战绩或当前对手。
_Avoid_: Account Profile, Trainer Detail, Match View

**Sensitive Name Rule**:
管理员维护并用于阻止新 Trainer 使用不当展示名称的基础数据；规则只影响创建时的名称，不追溯改变已有 Trainer。
_Avoid_: Trainer Rename, Content Report, Account Ban

**Trainer Team**:
Trainer 唯一拥有并在 Match 外维护的持久阵容配置；每个 Trainer 同时至多存在一支 Team，参与 Challenge 时按各自承诺时点冻结为 Trainer Team Snapshot，之后修改或删除原 Team 不影响该 Challenge 或 Match。
_Avoid_: Trainer Team Snapshot, Session Roster, Inventory

**Trainer Team Snapshot**:
Trainer 为一次 Challenge 锁定的不可变阵容与初始上场成员事实；发起方在发起时生成，接受方在接受时生成，Match 使用双方 Snapshot 构造 Session Roster。
_Avoid_: Trainer Team, Session Roster, Battle Record

**Trainer Session**:
账户选择自己拥有的 Trainer 进入游戏后形成的短生命周期行动身份；游戏行为从该会话取得 Trainer，不接受调用方逐请求声明 Trainer 身份，且每个账户同一时刻至多有一个有效会话。选择其他 Trainer、新设备进入或会话丢失会替换该会话，但不改变 Trainer 所属 Match。
_Avoid_: Login Session, Match, Battle Session, Trainer ID Parameter

**Trainer Presence**:
有效 Trainer Session 最近持续活动所形成的临时在线信号；它只决定能否成为新 Challenge 的目标，不决定 Trainer Session 是否仍可重连，也不改变 Match 生命周期。
_Avoid_: Trainer Session, Login Status, Active Match

**Challenge**:
一个持有有效 Trainer Session 的 Trainer 向另一个在线 Trainer 发出的直接对战邀请；多个待处理 Challenge 可以并存且不占用活跃 Match 名额，接受成功后才创建 Match，并取代双方账户涉及的其他待处理 Challenge。
_Avoid_: Match, Match Request, Matchmaking Queue

**Interrupted Match**:
因 Match Runtime 或 Battle Session 未能建立、执行失败或已经丢失而无法继续、且不产生胜负的 Match；它允许 Trainer 重新挑战或开始新对局。
_Avoid_: Completed Match, Session Termination, Forfeit

**No Contest**:
双方 Trainer 都未在行动期限内提交完整行动时，Match 结束但不确认胜者的最终结果；它不是战斗引擎判定的平局，也不是 Runtime 丢失导致的 Interrupted Match。
_Avoid_: Draw, Interrupted Match, Cancelled Match
