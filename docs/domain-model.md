# 领域模型

## 账号与 PlayerCharacter

Player Account 是玩家认证和安全状态主体；Administrator 是完全独立的管理身份，二者不共享凭据或会话。两类身份均只使用用户名和密码，不建立 MFA 或恢复码。系统不建立 RBAC 模型，管理员能力由 `avalon-admin-server` 注册的 RPC 集合界定。PlayerCharacter 是游戏角色；RPG World Topology、Party、Profession、等级和成长模块均以 `PlayerCharacterID` 关联，当前位置、Discovery 和 Pending Encounter 使用专用关系事实，不压入通用 World State JSON。

一个 Account 最多拥有 3 个未归档 PlayerCharacter。所有权创建后不可转移。PlayerCharacter 可以归档和恢复，但不能物理删除；存在活跃 Battle 时禁止归档。归档会清除活动绑定与 Presence，取消 Pending Challenge，保留 Team、Party、分享和全部历史。

每个 Account 在 `active_player_character` 中最多有一个活动绑定。绑定在全部设备间共享并跨登录持久化；游戏写命令从 Account 解析当前角色，不接受客户端自由声明角色身份。切换使用账号级锁、乐观版本和活动 Battle 校验。

PlayerCharacter 使用 Snowflake Identifier 作为稳定身份，不引入 `handle`。`displayName` 全局唯一且允许修改：

- 写入前执行 Unicode 规范化、全角半角、大小写和空白处理，生成 `displayNameKey`；
- 当前名称和全部历史名称共享数据库唯一约束；
- 旧名称不再用于公开查找，但永久禁止其他角色复用；
- Challenge 和关系使用 Snowflake Identifier，历史快照保留发生时的名称；
- 敏感词规则只阻止新建和改名，规则变化异步生成存量处理报告，不自动改写角色。

公开查询只允许已认证且拥有活动角色的调用者按完整 `displayName` 精确查找。响应只包含展示名称、粗粒度在线状态和是否可挑战，不公开 Account、Team、历史、内部状态或不可挑战原因。

## Presence

活动绑定不代表在线。已认证 WebSocket 或心跳在 server 内存中建立 Presence；所有连接断开或超时后失效。切换 PlayerCharacter 立即清除旧 Presence，并通知同账号客户端同步。Presence 只决定是否可成为新 Challenge 的目标，不承担授权和 Challenge 持久化。

## Team

每个未归档 PlayerCharacter 最多保存 20 支 Team，包含 1–6 名固定位置成员，每名成员包含 1–4 个不重复技能。第一支 Team 自动成为活动 Team，同一角色最多一个活动 Team；Challenge 和练习战仍显式选择 Team。

Team 使用稳定资料 Snowflake Identifier 保存核心引用和约束。保存、分享导入、Challenge 接受与进入对战前，后端都按当前实时资料重新校验 Team；资料被禁用或规则变化时返回逐成员、逐字段问题，不自动改写 Team。分享冻结不可变 Team Snapshot 和 Schema 版本，使用不可猜测分享码；导入创建独立 Team，不持续同步。

## RPG World Topology、Party 与 Encounter

World Topology 由 Region、任意有限深度的 Location 层级和独立有向 Location Exit 组成。PlayerCharacter 通过服务端权威 Traversal 移动；Player Position、move sequence、Map Discovery、Encounter Usage 和 Pending Encounter 均为独立关系事实。首期 Encounter Table 只启用 `walk`，移动成功后在同一事务按版本化服务端 Random Source 抽样并可能创建 Pending Encounter；不提供独立探索命令。

Party 是 PlayerCharacter 从 Owned Creature 中选择的最多六个有序世界编组，与 Team 完全分离。PvE 接受 Pending Encounter 时冻结 Party Snapshot；Battle 期间锁定会改变输入的 Party、Team 和参战 Owned Creature 写操作。Checkpoint 只能在当前地点按条件设置，恢复由服务端失败/死亡流程触发，不提供任意传送。

玩家读取只返回 Discovery 裁剪子图和展示安全字段；管理员读取完整拓扑、规则、Encounter 和不可变 Topology Integrity Report。地图资料通过停机 SQL 基线维护，稳定资料只停用不物理删除。

## 实时游戏资料

技能、道具、特性、生物和战斗规则直接保存在 PostgreSQL 实时资料表中。Snowflake Identifier 是跨领域引用身份；Stable Code 全局唯一。资料不物理删除，主体使用 `enabled=false` 禁用，无启用字段的技术明细记录明确停用时间。资料变更通过停机维护执行，管理写命令携带资源版本和幂等键。

系统不保存游戏资料历史版本。Battle 创建时冻结规范化 Snapshot，并使用 Random Source 与 Turn Record 恢复和重放；历史查询与分析只读取 Battle 权威事实。

## 效果与 BattleFormat

技能、特性、道具和 BattleFormat 效果采用“代码实现 + 资料参数化”。每个 `kind/schemaVersion` 在 Go 中显式注册，提供参数解码、结构校验、领域校验和运行时编译。未知版本阻止退出维护窗口，不使用反射扫描、`init()` 隐式注册、动态脚本或插件。

Protobuf 使用显式 `kind` 与参数消息表达每种效果参数。CI 比较 Protobuf 支持项和代码注册表，并覆盖合法、非法和边界样本。

BattleFormat 是实时游戏资料，包含模式、登记人数、选择人数、同时上场人数、等级规则、限制、Clause 和特殊机制。Clause、Restriction 与 Special Mechanic 的种类由代码注册，资料只组合和参数化已实现种类。

BattleFormat 声明可用于 Challenge、训练 PvE 或仅管理员预览。普通创建命令从当前启用资料选择 Format；`standard-single` 是默认项。这里的高级赛制只指复杂 BattleFormat，不包含 Tournament、Bracket 或排名流程。

## Challenge

新 Challenge 要求双方账号可用、目标 PlayerCharacter 是其账号当前活动角色且在线，双方均无活跃 Battle。创建时冻结：

- 发起方 Team Snapshot；
- 当前启用的 BattleFormat 与相关实时资料；
- BattleFormat 及其修订；
- 双方 Snowflake Identifier 和当时展示名称；
- 5 分钟到期时间。

同一对 PlayerCharacter 不分方向同时最多一个 Pending Challenge。创建后短暂离线不取消邀请；接受时再次校验双方活动角色、账号占用、接收方 Team 和冻结规则。接收方在接受事务中冻结自己的 Team。

接受、创建 PvP Battle、占用双方 PlayerCharacter 和把双方参与的其他 Pending Challenge 标记为 `Superseded` 在同一事务完成。Challenge 终态长期保留；队伍失效、拒绝、撤回和过期使用明确状态及原因。

## Battle 与 Team Preview

Battle 生命周期为 `created → preview → running → completed|canceled|interrupted`。PvP Battle 创建后进入 Team Preview，双方秘密提交符合 Format `selectCount` 的参战成员集合及初始上场顺序；超时未选时使用版本化随机规则生成合法选择并记录轨迹。

每个 PlayerCharacter 同时最多参与一个活跃 Battle。终局后释放占用。

回合命令携带幂等键和 `expectedStateVersion`。一个 Participant 必须一次提交己方全部人工 Requirement 的完整决策；整组原子校验和锁定。对方在执行前只能得知是否已锁定。双方齐备后 Runtime 一次调用 Battle Engine。

API 使用 Battle 内稳定的 `side/memberPosition/slotPosition`。位置在 Battle 创建后稳定，切换上场不改变成员位置。

期限是冻结 BattleFormat 的一部分，首版基线为：Preview 60 秒、单回合 90 秒、整场 30 分钟。查询、断线和失败命令不延长期限。回合超时只有一方已锁定时该方获胜，双方均未提交为 No Contest；整场超时依次比较存活成员数和确定性总剩余 HP 比例，完全相同为 No Contest。

认输先在数据库持久化胜者与结果，再释放 Runtime。正常完成、认输、回合超时、整场超时、平局、No Contest 和 Interrupted 使用不同稳定原因。

## Battle Engine 与 Turn Record

Battle Engine 是纯函数式核心：输入冻结规则快照、权威状态、完整命令、显式时间输入和版本化随机源；输出新状态、结构化事件和随机轨迹。不得访问数据库、网络、系统时钟、全局随机源或 goroutine。

随机算法版本和初始种子随 Battle Snapshot 冻结，每次消费记录序号、用途和结果。下一回合随机游标与 `state_version` 同事务保存。

Runtime 先计算候选结果，再在数据库事务中写命令幂等结果、Turn Record、状态摘要、Disclosure 增量、状态版本和随机游标；提交成功后才替换内存状态并返回。

Turn Record 使用版本化、语言无关 JSON，保存权威命令、事件、随机轨迹、版本和摘要；Battle 用它从最后提交版本恢复、分析和离线重放。

事件逐类携带稳定 `kind` 和 `schemaVersion`。未知版本不得静默忽略。受控 Battle 回放校验任务逐回合比较事件、状态摘要和最终结果，并且只向管理员暴露安全摘要。

所有玩家响应经过当前 Participant 视角投影。Disclosure Ledger 原子合并已公开技能、特性、道具和属性，且只能增长。Battle 结束也不自动公开对手完整 Team。

## Bot 与训练 PvE

Bot 不拥有 Account 或 PlayerCharacter。它由显式注册的 `botCode/strategyVersion`、配置 Schema 和队伍模板或版本化生成策略组成。Battle 创建时冻结 Bot 策略、配置、队伍和随机源。

Bot 管理记录按 `botCode + version` 不可变保存。创建首个版本和发布后继版本只接受严格 JSON 定义；仅启用版本可创建训练 PvE，禁用不删除既有 Battle 的冻结定义。

Bot 只读取其 Participant 可见视图和合法动作集合，不访问数据库、网络、时钟或共享状态。策略具有确定性搜索预算和墙钟安全超时；失败按 BattleFormat 明确的保底动作或判负规则处理。

训练 PvE Battle 占用 PlayerCharacter 的唯一活跃 Battle 名额，Bot 不产生账号占用，结果不影响排名。玩家不能指定随机种子。

## 历史与分析

Battle 完成事务内写 Authoritative Summary。胜率、使用率和趋势等聚合由 PostgreSQL Outbox + Asynq 异步更新 Analytics Projection。响应返回数据截止时间或投影版本。

## 后台任务与审计

Asynq Worker 负责周期生命周期扫描和分析投影。管理员只能查看脱敏任务视图，并可使用带 `Idempotency-Key` 的专用命令重试或取消任务；相同命令只重放首次结果，同键不同任务返回冲突。运行中任务的取消是协作取消请求，Worker 必须尊重 Context，不能承诺立即中断 goroutine。

管理员审计与玩家管理审计各自维护独立 SHA-256 哈希链。数据库触发器负责顺序号、前置摘要和链尾；两个账本均 append-only，拒绝更新、删除和截断。周期任务独立重算两条链，管理员也可以显式触发校验；成功校验写为受控管理员审计。

每个投影以稳定事件或 Battle ID 幂等处理，并能从 Battle 与 Turn Record 完整重建。重建写影子表或新版本，校验后原子切换；失败不破坏当前查询结果。

Battle、Turn Record 和审计默认长期保留。账号注销时匿名化可识别个人信息，但不破坏对战和审计链路。核心历史清理必须由未来独立数据治理方案决定；游戏资料只保留当前实时状态。
