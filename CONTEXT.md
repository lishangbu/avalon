# Avalon Backend

定义 Avalon Go 后端中账号身份、游戏资料、对战执行与历史分析所使用的稳定领域语言。

## 持久化端口

**Repository**:
关系型数据库中的领域聚合读写端口；实现位于对应领域的 `persistence` 包，并封装 Ent、pgx、事务、审计和幂等细节。
_Avoid_: Store, DAO, Generic Data Access

**Reader**:
返回领域对象或冻结事实的只读端口；它不暴露管理分页投影，也不提供写入能力。
_Avoid_: Query, Repository, Store

**Query**:
返回分页列表、筛选结果或管理诊断投影的只读端口；投影不作为领域聚合继续写回。
_Avoid_: Reader, Repository, Store

**Technical Store**:
Session、Lease、Blob、Idempotency Record 等技术状态设施；`Store` 不用于命名 PostgreSQL 中的业务聚合持久化端口。
_Avoid_: Domain Repository, Business Aggregate Store

## 共享身份

**Snowflake Identifier**:
Avalon 所有持久实体和关系使用的全局正数身份；数据库保存为 BIGINT，Go 使用强类型 ID，Protobuf 和 Web 将其作为不透明十进制字符串传输。它不承担授权、业务时间或严格提交顺序语义。
_Avoid_: UUID, Database Sequence, Public Secret, Business Timestamp

**Snowflake Node Lease**:
PostgreSQL 按数据库时间和 fencing token 独占分配给一个运行进程的短期发号节点；失去租约会停止该进程产生新 Snowflake Identifier。
_Avoid_: Static Worker ID, Hostname Hash, Permanent Node Assignment

## 身份与角色

**Admin Access Token**:
管理员通过 Ed25519 签名、短期有效且只保存在客户端内存中的 Bearer JWT；它只表达管理员账号与设备会话身份，不表达角色或权限。
_Avoid_: Admin Session Cookie, Permission Token, Permanent API Key

**Admin Refresh Token**:
管理员设备会话族中单次使用并原子轮换的随机 opaque token；Web 保存于 localStorage 并显式提交，重放会撤销整个会话族。
_Avoid_: Access Token, Permanent API Key, Player Session

**Valkey Session Store**:
管理员与玩家认证会话的独立事实存储；只保存 token 摘要、会话族、账号、安全版本和期限索引，不保存密码、access token 或业务对象。
_Avoid_: PostgreSQL Session Table, Cookie Store, Asynq Connection

**Account**:
玩家登录与安全状态的主体；一个 Account 可以拥有多个 PlayerCharacter，但同一时刻只能激活一个。
_Avoid_: User, Player, PlayerCharacter

**PlayerCharacter**:
Account 拥有的持久游戏角色，具有全局唯一展示名称、Team、Challenge 和 Battle 历史；未来 RPG 职业与成长系统也以它为角色边界。
_Avoid_: Trainer, Player, Account

**Active PlayerCharacter**:
Account 当前唯一可执行游戏命令的 PlayerCharacter；该绑定持久化于 PostgreSQL，并对该 Account 的全部设备生效。
_Avoid_: Login Session, Presence, Client Selection

**PlayerCharacter Presence**:
活动 PlayerCharacter 最近通过已认证连接持续活动形成的临时在线信号；它只决定能否成为新 Challenge 的目标。
_Avoid_: Active PlayerCharacter, Login Status, Match Reservation

**Display Name**:
PlayerCharacter 的全局唯一公开名称；允许修改，但历史名称永久保留且不能被其他角色复用。
_Avoid_: Handle, Account Username, Internal Code

**Participant**:
PlayerCharacter 或 Bot 在一场 Battle 中冻结的参赛身份；它属于 Battle，而不是登录或公开身份。
_Avoid_: Account, PlayerCharacter, Battle Runtime

## 游戏资料

**Current Game Data**:
直接供 Team 校验、Challenge 和对战创建读取的唯一权威实时资料。
_Avoid_: Content Pack, Runtime Catalog, Draft

**Stable Code**:
资料的全局唯一英文机器标识；Snowflake Identifier 承担引用身份，资料变更通过停机维护完成。
_Avoid_: Display Name, Database ID, Source Slug

**Element Effectiveness**:
一种攻击属性面对一种防御属性时的规范伤害倍率；未声明的属性组合按中性倍率处理，多防御属性的倍率逐项相乘。
Starllow 的 Creature 最终倍率只作为 Seed 生成期的一致性校验输入，不持久化，也不是第二份权威克制表。
_Avoid_: Element Relation, Type Chart Row, Damage Rule

**Nature**:
Team 成员选择的一种实时资料；非中性 Nature 提升一项可培养战斗能力并降低另一项，中性 Nature 不改变任何能力。
_Avoid_: Personality, Temperament, Stat Preset

**Gender Ratio**:
可参战 Creature 的雄性与雌性出现比率；用八分份精确表达，0:0 表示无性别。它属于 Creature 而非 Species，因为同一 Species 的不同形态可能拥有不同比率。
_Avoid_: Species Gender, Fixed Gender, Gender Dictionary Entry

**Creature Evolution**:
一个来源 Creature 在满足明确条件后转换为目标 Creature 的实时资料关系；条件属于这条有方向的关系，而不属于来源或目标 Creature。常用可执行条件使用结构化字段表达，来源资料中尚未结构化的完整条件仍作为说明保留。
_Avoid_: Species Evolution, Evolution Text, Evolution Chain JSON

**Creature Data Projection**:
从独立 Species、Creature、Form、数值、能力、技能学习、携带物与皮肤关系表按需组装的运行时只读值；它只服务 Team 校验和 Battle Snapshot 冻结，不是持久化聚合或新的 Catalog。
_Avoid_: Creature Catalog, Creature Metadata Row, Runtime Catalog

**Skill Effect**:
招式的完整机制说明；首套资料对应 Starllow `additional_effect`，用于解释结算行为，不承担玩家展示文案的职责。
_Avoid_: Skill Description, Interaction Tags, Flavor Text

**Skill Description**:
招式面向玩家展示的自然语言描述；首套资料对应 Starllow `description`，不作为战斗结算规则来源。
_Avoid_: Skill Effect, Short Effect, Flavor Text

**Skill Rules**:
Skill 主表中按 `onUse` 执行时机组织的强类型战斗规则 JSON；它只组合 Go 已实现的规则原语，保存、导入、启动和 Battle 冻结共用同一编译器校验。
_Avoid_: Skill Detail, Script, Generic DSL

**Ability Rules**:
Ability 主表中按固定 Battle Engine 执行时机组织的强类型战斗规则 JSON；基础资料与展示文案不进入规则文档，规则也不承担通用扩展字段职责，启动和 Battle 冻结使用同一编译器校验。
_Avoid_: Ability Detail, Metadata, Script, Generic DSL

**Item Description**:
物品面向玩家展示的自然语言描述，不作为结构化道具规则的替代品。
_Avoid_: Item Effect, Flavor Text

**Item Catalog Entry**:
从已授权权威资料建立的一条规范道具身份、三语名称、展示文案、图标文件名和目录分类事实；尚未声明可执行用途的记录仍可禁用建档，但不能因此从目录丢失。
_Avoid_: Inventory Stack, Item Effect, Shop Stock

**Catalog-only Item**:
已经成为 Item Catalog Entry、但尚未声明获取方式和可执行用途的禁用道具；它不能进入背包、商店、掉落、Team 或 Battle。
_Avoid_: Disabled Item Effect, Material, Placeholder Item

**Ability Introduction**:
特性面向玩家展示的简要介绍；完整机制仍由特性效果与结构化规则表达。
_Avoid_: Ability Effect, Flavor Text

**Pokédex Entry**:
Creature Species 面向玩家展示的图鉴条目；它描述物种，不参与战斗规则或分类检索。
_Avoid_: Species Genus, Creature Description, Flavor Text

**Species Reference Copy**:
Species 的四类独立简体中文参考文案：基础介绍、外形与生态、设计原型、补充知识；它们分别对应首套 Starllow 的 `description`、`profile`、`prototype`、`detail`，不参与战斗结算，也不能合并为通用 JSON。
_Avoid_: Species Detail JSON, Generic Metadata, Flavor Text

**Asset**:
经后端确认、校验并记录 SHA-256 的 RustFS 对象；对象可按完整键公开读取，但列举和写入必须认证。被实时资料引用的 Asset 不可覆盖，删除必须先通过引用检查。
_Avoid_: Arbitrary Attachment, Mutable Object, Presigned GET URL

## 队伍与邀请

**Team**:
PlayerCharacter 在 Battle 外维护的命名阵容；进入 Battle 前必须按 Current Game Data 重新校验，引用被禁用的资料时不可参战。
_Avoid_: Team Snapshot, Battle Roster, Inventory

**Team Snapshot**:
Challenge 或 Battle 为一次对局冻结的不可变 Team 事实；它记录来源 Team、版本和实际参战输入。
_Avoid_: Team, Runtime State, Share Snapshot

**Team Preview**:
Battle 启动前双方秘密锁定参战成员集合和初始上场顺序的阶段；双方齐备或到期裁决后才进入 `running`。
_Avoid_: Team Validation, Turn Submission, Matchmaking

**Challenge**:
活动 PlayerCharacter 向另一个在线活动 PlayerCharacter 发出的、具有服务端固定期限的直接对战邀请；它在创建时冻结发起方 Team 和 BattleFormat 输入，双方明确接受后才能创建 PvP Battle。
_Avoid_: Match, Matchmaking Queue, Tournament

## RPG 进度

**Owned Creature**:
PlayerCharacter 实际拥有并持续成长的一只 Creature 实例；它具有独立 Snowflake Identifier、来源、等级、经验、当前生命、亲密度、Nature、特性、技能和培养值，不等同于共享 Creature 资料或 Team 成员配置。
_Avoid_: Creature, Team Member, Participant, Creature Data Projection

**Party**:
PlayerCharacter 从 Owned Creature 中选择、用于世界探索和后续战斗编组的最多六个有序位置；同一 Owned Creature 在 Party 中只能出现一次。
_Avoid_: Team, Team Preview, Participant

**Inventory Stack**:
PlayerCharacter 背包中一种可获取道具的当前非负数量；每次变化必须同时产生不可变 Inventory Transaction。
_Avoid_: Item Catalog Entry, Held Item, Loot Entry

**Inventory Transaction**:
一次道具获得或消耗及提交后余额的不可变事实；原因和可选来源身份用于审计，不能通过改写流水修正当前数量。
_Avoid_: Inventory Stack, Administration Audit Log

**Held Item**:
Owned Creature 当前携带的一件可执行战斗道具；它从 PlayerCharacter 的 Inventory Stack 原子扣除，并在卸下或替换时原子归还。Battle Snapshot 冻结开战时的携带事实，只有权威终局明确记录已触发消费时才永久消耗。
_Avoid_: Equipment Instance, Equipment Loadout, Inventory Stack

**Equipment Catalog Entry**:
以 Item Catalog Entry 为展示与获取身份、描述 PlayerCharacter 可穿戴槽位、资格、属性修正和被动规则的规范装备资料；它不代表玩家实际拥有的某一件装备。
_Avoid_: Equipment Instance, Held Item, Inventory Stack

**Equipment Instance**:
PlayerCharacter 实际拥有的一件不可堆叠装备资产；每次获取都产生独立 Snowflake Identifier，穿戴、出售和审计始终引用该实例，而不以聚合数量替代所有权事实。
_Avoid_: Equipment Catalog Entry, Inventory Stack, Held Item

**Equipment Loadout**:
PlayerCharacter 当前多槽装备实例关系的单一版本化整体；替换命令校验最终完整状态并原子提交，Battle Reservation 存在时不得变更。
_Avoid_: Party, Team, Held Item, Incremental Equip Command

**Equipment Transaction**:
一件 Equipment Instance 获取、穿戴、卸下或出售的不可变资产流水；同一命令产生的多行事实共享 Operation Identifier 和提交时间。
_Avoid_: Inventory Transaction, Equipment Loadout, Administration Audit Log

**Equipment Asset Diagnostic**:
管理员按角色、装备、来源、穿戴状态或流水动作读取 Equipment Instance 与 Equipment Transaction 的只读审计视图；列表使用绑定资源类型与筛选条件的 keyset cursor，已出售实例保留并显式展示出售终态。
_Avoid_: Mutable Asset Editor, Offset Pagination, Active Inventory Only

**Equipment Sale**:
把一件未穿戴 Equipment Instance 标记为已出售，并按其 Equipment Catalog Entry 冻结声明的货币与价格原子增加 PlayerCharacter Wallet、写入 Currency Transaction 和 Equipment Transaction；客户端不能选择出售所得货币或价格。
_Avoid_: Inventory Sale, Client Price, Equipment Deletion

**Equipment Acquisition**:
从已验证的 Shop Purchase、Quest Reward Claim、Loot Settlement 或管理授予事实建立一件或多件 Equipment Instance 的统一事务；Equipment 类型的 Item 不进入 Inventory Stack，每件实例、资产流水、幂等响应和 Outbox 必须共同提交。
_Avoid_: Client Grant, Equipment Stack, Source Type Parameter

**Shop Purchase**:
PlayerCharacter 在当前 Location 的启用 Shop 按 Shop Item 声明的 Currency 与价格完成的一次不可变购买事实；支付、成交快照与普通道具或 Equipment Instance 交付在同一事务完成，客户端不能提交价格或货币。
_Avoid_: Client Price, Shop Catalog Entry, Inventory Transaction

**Quest Reward Claim**:
PlayerCharacter 对一个已完成 Quest 的明确完成轮次执行的一次性奖励领取事实；同一任务进度与完成轮次最多成功一次，整组奖励要么全部提交，要么全部回滚。
_Avoid_: Quest Progress, Partial Reward, Client Selected Reward

**Loot Settlement**:
Battle 或世界交互按权威随机过程预先建立、归属于一个 PlayerCharacter 的不可变掉落结算；玩家只能领取已冻结的 Item 与数量，不能自行提交 Loot Table、Loot Entry 或数量。
_Avoid_: Client Loot Roll, Loot Table, Inventory Transaction

**Encounter Loot**:
Encounter Entry 可选关联的胜利掉落定义；接受 Pending Encounter 时使用其已持久化 seed 的独立 draw 3 和 4 依次冻结一个加权 Loot Entry 及数量，只有真人胜利终局才建立 Loot Settlement，重试不得重新抽样。
_Avoid_: Terminal Loot Roll, Client Loot Roll, Defeat Reward

**Active Profession Set**:
PlayerCharacter 已拥有职业进度中当前参与装备与规则资格判定的非空集合；切换只改变激活状态，不删除等级与经验，并且当前 Equipment Loadout 在目标集合下不合法时整笔拒绝。
_Avoid_: Profession Deletion, Single Active Profession, Implicit Unequip

**Currency Balance**:
PlayerCharacter 对一种 Currency 的当前非负余额；每次变化必须同时产生不可变 Currency Transaction。
_Avoid_: Item Cost, Account Balance, Inventory Stack

**Region**:
RPG 世界中包含一个或多个层级 Location 的顶层区域。
_Avoid_: Habitat, Location, World State

**Location**:
PlayerCharacter 可以探索、遭遇、交互或设置 Checkpoint 的层级地点；它属于 Region，不等同于 Species 的生态 Habitat。
_Avoid_: Region, Habitat, Encounter Table

**Player Position**:
PlayerCharacter 当前所在的 Location 及其移动序号事实；它是可并发校验的当前位置，不等同于恢复用的 Checkpoint 或通用 World State。
_Avoid_: Checkpoint, World State Flag, Session Presence

**World Topology**:
由 Region、层级 Location 和有方向的 Location Exit 共同形成的 RPG 世界可达关系；它是服务端判定移动是否合法的唯一权威。
_Avoid_: World Map, Coordinate Grid, Client Route Graph

**Location Exit**:
从一个 Location 指向另一个 Location、具有稳定身份和明确通行条件的世界资料；反向通行必须由另一条 Location Exit 明确声明。
_Avoid_: Bidirectional Link, Client Route, Parent Location

**Location Traversal**:
PlayerCharacter 沿一条可用 Location Exit 从当前位置转移到目标 Location 的服务端权威命令；成功移动可以原子地产生一个待处理的 Pending Encounter。
_Avoid_: Teleport, Coordinate Movement, Exploration Command

**Traversal Idempotency**:
一次 Location Traversal 以 PlayerCharacter、出口和期望位置版本为上下文的可重放命令事实；重复提交只能返回首次结果，不会再次移动、执行副作用或重新抽样。
_Avoid_: At-least-once Move, Client Retry Roll, Duplicate Traversal

**Pending Encounter**:
由 Location Traversal 按目标 Location 启用的遭遇资料产生、等待后续 PvE 或 PvP 处理的持久世界事件；它不是 Battle，也不等同于一次已完成的战斗。
_Avoid_: Passive Encounter, Match, Client Random Roll

**Random Source**:
由服务端版本化算法、seed、用途和 draw 序号组成的可审计随机输入；Encounter 和 Battle 必须保存其抽样轨迹，重试不得改变结果。
_Avoid_: Global RNG, Client Seed, Retry Roll

**Exit Condition**:
Location Exit 使用的后端闭集通行条件，例如等级、Item、Quest Objective、Profession 或 World State 开关；条件不能由客户端或任意脚本解释。
_Avoid_: Client Permission, Script Expression, Coordinate Rule

**Traversal Effect**:
Location Traversal 在同一事务中按白名单规则产生的有限 World State 或 Quest Progress 变化；它不能调用任意业务服务或修改其它 PlayerCharacter。
_Avoid_: Trigger Script, Async Side Effect, Generic Event Handler

**PvE Battle**:
PlayerCharacter 与服务端控制对手之间的 Battle；Pending Encounter 经玩家显式接受后才能进入 PvE Battle。
_Avoid_: Practice Match, Automatic Encounter Fight, NPC Match

**PvP Battle**:
首期两个 PlayerCharacter 经显式同意后进行的 1v1 Battle；地图移动和位置重合不能强制创建 PvP Battle，多人扩展需另行定义。
_Avoid_: Automatic PvP, Collision Battle, Matchmaking Queue

**Map Discovery**:
PlayerCharacter 已经发现的 Location 与 Location Exit 事实集合；它只决定玩家可见的 World Topology，不改变实际可达关系。
_Avoid_: Client Fog State, World State Flag, Full Map Access

**World Map Projection**:
使用坐标、图标和地图资产呈现 World Topology 的独立版本化只读展示视图；展示布局可以单独调整，但不参与移动、距离或通行条件判定。
_Avoid_: World Topology, Battle Grid, Movement Rules

**RPG Map Read**:
地图读取按查看方分为玩家可见子图和管理员完整拓扑；玩家视图只包含 Map Discovery 与展示安全字段，管理员视图还包含条件、遭遇资料和拓扑校验报告。
_Avoid_: Full Client Map, Admin Write Model, Hidden Runtime Graph

**Topology Integrity Report**:
维护阶段对 Region、Location、Location Exit、出生点和规则引用执行的完整性校验结果；失败会阻止资料启用，但报告本身可供管理员读取。
_Avoid_: Runtime Warning, Client Validation, Map Analytics

**RPG Rule Compiler**:
把 Protobuf 的有限 Exit Condition/Traversal Effect 消息规范化为版本化 JSON 并编译为已注册服务端求值器的唯一规则入口；它不执行脚本，也不接受未知版本。
_Avoid_: JSON Interpreter, Script Engine, Client Rule Parser

**Read Cursor**:
地图分页使用的不落库查询锚点，绑定读取时间、稳定排序键和必要资源版本摘要；它不是全局 revision，也不改变拓扑事实。
_Avoid_: Global Revision, Mutable Page Number, Client Offset

**Checkpoint**:
声明所属 Location、启用状态、可设置条件与恢复条件的稳定 RPG 资料；PlayerCharacter 可以显式选择其中一个。Encounter PvE 创建时冻结 Party 完整战斗输入和当前生命，正常终局在同一事务写回剩余生命；明确落败且满足恢复条件时，再在该事务内恢复位置和该场冻结 Party 的生命。Checkpoint 不提供任意传送，Training、PvP、平局、No Contest、取消或中断均不触发位置恢复。
_Avoid_: Current Location, Fast Travel Point, Spawn Point

**Encounter Table**:
首期仅支持 `walk` 的 Encounter Table：先按整数万分比决定是否产生遭遇，再按正整数权重选择 Creature、Form 和等级区间的实时资料；移动步数冷却和可选次数上限按 PlayerCharacter 与 Encounter Table 组合独立计算。
_Avoid_: Habitat, Loot Table, BattleFormat

**Maintenance Baseline**:
停机期间按 node 0 固定 Snowflake Identifier、依赖顺序和完整性校验建立当前游戏资料的唯一 SQL 基线；重复执行应暴露资料错误，测试夹具不伪装成生产基线。
_Avoid_: Idempotent Seed Guessing, Runtime Migration, Compatibility Fixture

**Reason Code**:
跨 RPC、日志和客户端处理稳定的机器原因标识；简体中文 message 只负责当前产品展示，不替代领域状态或客户端分支条件。
_Avoid_: HTTP Text, Localized Control Flow, Free-form Error

**Quest Progress**:
PlayerCharacter 对一条 Quest 及其结构化 Objective 的生命周期和累计完成事实；Quest 定义是共享资料，进度属于单个 PlayerCharacter。
_Avoid_: World State, Match History, Administration Job

**Available Quest**:
当前 PlayerCharacter 在所在 Location 可从启用发放 NPC 开始、且已满足前置任务并未处于不可重复生命周期的一条 Quest；可用性由服务端实时判定，不是客户端缓存状态。
_Avoid_: Quest Catalog, Client Eligibility, Active Quest

**Quest Completion**:
PlayerCharacter 的全部当前 Objective 达到要求，并在可选交付 NPC 所在 Location 显式提交后发生的 `active -> completed` 转换；它递增完成轮次，但奖励必须通过独立 Quest Reward Claim 领取。
_Avoid_: Quest Reward Claim, Objective Progress, Automatic Claim

**Quest Objective Code**:
结构化 Quest Objective 在全部任务范围内唯一的 Stable Code；Traversal Effect 以它定位唯一目标，数据库身份和已有玩家进度仍由稳定 Objective Identifier 关联。
_Avoid_: Quest-local Code, Objective Description, Player Progress ID

## 对战规则与执行

**BattleFormat**:
Current Game Data 中定义战斗模式、登记人数、选择人数、同时上场人数、限制、Clause 和特殊机制的实时资料。
_Avoid_: Tournament, Bracket, Runtime Flag

**Battle Engine**:
不依赖数据库、HTTP、时钟或 goroutine 的纯 Go 状态机；它接收冻结规则、状态、命令和显式随机源，返回新状态、事件和随机轨迹。
_Avoid_: Match Actor, HTTP Service, Rules Repository

**Battle**:
一场按 `created → preview → running → completed|canceled|interrupted` 演进并持久保存参与者、回合、结果和历史的权威战斗；首期 PvP 为 1v1，PvE 使用 Party，且不要求 PvP 参与者位于同一地图地点；系统只使用 PvE 与 PvP 两种 Battle Mode。
_Avoid_: Match, Matchmaking Queue, Practice Match

**Battle Mode**:
Battle 的参与者关系类型，取 PvE 或 PvP；它不表示匹配算法、地图类型或客户端页面。
_Avoid_: Match Type, Queue Mode, Battle Format

**Battle Snapshot**:
Battle 创建时冻结的参与者、Party/Team 输入、Creature Projection、BattleFormat、规则资料和 Random Source 事实，并带规范化摘要；Battle Engine 只读取这份输入，不回查实时资料。
_Avoid_: Live Team, Current Game Data, Runtime Draft

**Battle Recovery**:
Battle 或 Pending Encounter 在进程重启、创建失败或暂时不可用时按照原始幂等身份继续处理的能力；恢复从最后已提交的状态版本继续，经过有界重试仍失败才进入 `interrupted`，不得重新抽样或丢弃原始随机轨迹。
_Avoid_: Retry With New Roll, Automatic Victory, Data Reset

**Battle Outbox**:
核心 Battle、Traversal 和奖励事务提交时写入的可靠领域事件事实；Worker 仅在提交后投递通知、分析或恢复任务，不能把外部副作用当作事务的一部分。
_Avoid_: In-transaction HTTP Call, Best-effort Event, Asynq Source of Truth

**Battle Runtime**:
进程内独占承载一个 Running Battle 并串行处理命令的执行器；它受 PostgreSQL Runtime Lease 与 fencing token 保护，可从已提交状态版本、Turn Record 和随机源快照恢复。
_Avoid_: Battle Engine, Database Transaction, Distributed Actor

**Turn Submission**:
Participant 对当前回合己方全部人工选择的一次完整秘密提交；接受后锁定，双方齐备后才交给 Battle Engine。
_Avoid_: Individual Action, Turn Record, Editable Draft

**Turn Record**:
成功执行一个权威命令后持久化的命令、事件、随机轨迹、状态版本和摘要；它用于历史、重放校验和 Battle Recovery，不允许以客户端新命令替代已提交记录。
_Avoid_: Event Store, Runtime Snapshot, Audit Log

**Disclosure Ledger**:
按查看方持久化记录对手已经公开的信息集合；信息只能从未知变为已公开，终局也不会自动揭示全部权威资料。
_Avoid_: Turn Record, Replay, Full Battle State

**Interrupted Battle**:
因 Runtime 未能建立、执行失败、恢复耗尽或租约丢失而无法继续且不产生胜负的 Battle。
_Avoid_: Completed Battle, Forfeit, No Contest

**No Contest**:
按赛制裁决结束但没有胜者的 Battle 结果；它不是 Battle Engine 判定的平局，也不是 Interrupted Battle。
_Avoid_: Draw, Interrupted Battle, Canceled Battle

## Bot 与练习

**Bot**:
由稳定 `botCode` 和 `strategyVersion` 标识、只读取 Participant 可见视图并通过正常命令行动的服务端策略；Battle 创建时冻结策略标识、版本与配置摘要。
_Avoid_: Fake Account, PlayerCharacter, Dynamic Plugin

**Training Battle**:
PlayerCharacter 主动发起、以冻结 Bot Participant 为对手的 PvE Battle；它占用真人 PlayerCharacter 的活跃 Battle 名额，但不进入排名。
_Avoid_: Challenge, PvP Battle, Draft Preview

## 分析

**Authoritative Summary**:
Battle 完成事务内写入的胜负、参与者、BattleFormat、终止原因和时间等不可变摘要；奖励事实与其在同一事务提交。
_Avoid_: Analytics Projection, Runtime State, Public Profile

**Analytics Projection**:
从权威 Battle 与 Turn Record 异步构建、可完整重建的只读统计模型；它必须暴露处理水位或数据截止时间。
_Avoid_: Battle Result, Source of Truth, Independent Write Model
