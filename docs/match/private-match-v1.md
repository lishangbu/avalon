# Private Match v1

本文定义 Avalon 首个真人对战垂直切片。它是实现与验收规格；稳定领域语言见根目录 `CONTEXT.md`，难以逆转的取舍见 `docs/adr/`。

## 目标与范围

首版只支持两名不同账户所属 Trainer 之间的私有直接 Challenge，以及由 Challenge 接受后创建的 `standard-single` Match。Match 持久化参与者、阵容快照、生命周期、最终结果和有限披露；Battle Session 继续只在内存中运行。

首版不实现：

- 玩家自助注册、验证码或找回密码；普通账户由现有管理端或测试数据创建。
- 自动匹配队列、评分、排行榜、观战、好友系统或离线邀请。
- Redis、多节点迁移、Runtime 恢复、消息补发、回放或完整战斗日志。
- WebSocket 业务命令、完整 Battle Session DTO 或对手完整阵容公开。

## 身份与授权

### Account 与 Trainer

- Account 仍是 OAuth 认证主体；Trainer 是独立、持久的游戏身份。
- 一个 Account 可拥有多个 Trainer，但同时最多三个未归档 Trainer。
- 同一 Account 的不同 Trainer 不能同时在线，也不能分别参与多场活跃 Match。
- 玩家接口只要求已认证账户，不要求角色或 RBAC authority；现有 RBAC 只保护管理端基础数据 CRUD。
- 账户层 Trainer 管理只要求 OAuth；Team、Challenge、Match、Presence 与 History 等当前游戏行为同时要求 OAuth 和有效 Trainer Session。

### Trainer displayName

- 内部关系使用数据库 `trainerId`；对外身份只使用 `displayName`，不生成或公开 Trainer Code。
- displayName 创建后不可修改，全服唯一，Trainer 归档也不释放。
- 输入先去除首尾空白，再执行 Unicode NFKC 与忽略英文字母大小写的规范化；规范化键持久化并建立唯一约束。
- 规范化后长度为 2–16 个 Unicode code point，只允许 Unicode 字母、数字、普通空格 U+0020、`_`、`-`；不限制分隔符位于首尾，纯空白无效。
- 公开查找只支持规范化后的完整名称精确匹配，不提供前缀、模糊搜索或候选列表。
- 归档 Trainer 对公开查找表现为不存在；其他不可挑战原因收敛为 `challengeable = false`。

### Sensitive Name Rule

- 敏感词规则是数据库持久化的管理端基础数据，由独立管理权限保护 CRUD。
- 字段至少包括词条、匹配方式 `EXACT | CONTAINS`、启用状态与 revision。
- 词条与名称先按 displayName 规则规范化，再额外移除空格、`_`、`-` 形成 moderation key 后匹配。
- 不支持正则、同形字符、拼音或谐音匹配。
- 新增或修改规则不追溯处置已有 Trainer。
- 基线只以 `EXACT` 预置少量系统保留词：`admin`、`administrator`、`system`、`root`、`官方`、`管理员`。

### Trainer Session 与 Presence

- 账户选择自己拥有的 Trainer 进入游戏后，服务端创建随机 opaque Trainer Session credential。
- 单节点内存中每个 Account 最多一个有效 Trainer Session；选择其他 Trainer、新设备进入或同账户再次进入会使旧会话立即失效。
- 当前 Trainer 有 Active Match 时不能切换，只能恢复该 Match 所属 Trainer。
- Trainer Session 使用 30 分钟滑动空闲期限；合法游戏请求或认证心跳刷新期限。
- HTTP 游戏请求同时携带 OAuth Bearer 与 `X-Trainer-Session`；请求体不接受调用方声明 trainerId。
- WebSocket 在首个应用消息中同时认证 OAuth 与 Trainer Session；凭据不放入 Cookie、URL 或浏览器持久存储。
- Presence 与 Session 分离：认证心跳或合法游戏请求刷新 Presence，建议每 15 秒心跳，45 秒无活动视为离线。
- Presence 丢失不结束 Match，也不重置回合期限。

### OAuth Web Client

- Web 使用 Avalon 自定义 password grant 的公共 Client，`client_authentication_method = none`，不使用 client secret 或 HTTP Basic。
- token 请求提交 `client_id`、用户名、密码、scope 与自定义 password grant type；生产环境必须 HTTPS。
- access token 约 30 分钟；旋转 refresh token 每次成功刷新后重新获得 8 小时期限，token family 从首次登录起最长 7 天。
- 旧 refresh token 重放会撤销整个 token family；同一标签页串行刷新并共享结果。
- Web 只在当前标签页会话保存 refresh token，不使用 Cookie 或跨浏览器重启的持久存储。
- 刷新成功不重建 Trainer Session；新 access token 可继续搭配原 Trainer Session。
- 普通退出只撤销当前 token family，并结束由该 family 建立的 Trainer Session。
- 修改或重置密码、账户禁用会撤销该账户全部 token family、Trainer Session 与 Presence。
- token 或 Trainer Session 失效只按断线处理，不暂停或直接裁决 Match。

## Trainer 生命周期

- 创建 Trainer 使用 `commandId` 保证幂等；名称冲突、敏感词命中、有效 Trainer 已达三个或相同 commandId 携带不同载荷时拒绝。
- Trainer 只归档，不物理删除；归档释放三个有效 Trainer 的名额。
- 当前 Trainer 或仍有 Active Match 的 Trainer 不能归档。
- 归档事务取消该 Trainer 的全部 Pending Challenge，原因 `TRAINER_ARCHIVED`。
- 恢复归档 Trainer 时重新检查有效 Trainer 少于三个；恢复不自动建立 Trainer Session。
- 归档 Trainer 的 Team、Match History 与 displayName 永久保留；所属账户可通过 OAuth 账户级只读接口查看其历史。

## Trainer Team

- 每个 Trainer 同时最多一支 Team；服务端只保存完整合法 Team，不保存草稿。
- Team 包含 1–6 名成员；每名成员包含 creature、1–4 个 skills、ability、item、IV、EV、nature。
- 未填写时使用现有默认：IV 31、EV 0、中性 nature。
- 允许重复 creature 与重复 item，遵循现有 `standard-single` 规则。
- Trainer Team 不保存可修改等级；真人 Match 构造 Session Roster 时全员固定为 50 级。
- 可编辑 Team 使用关系表；进入 Challenge 或 Match 的不可变 Trainer Team Snapshot 使用带 `schemaVersion` 的 JSONB。
- Team 更新要求 `expectedRevision`；服务端只提交一次完整替换后的合法聚合。
- Challenge 不接收 teamId，自动读取当前 Trainer 唯一 Team。

## Challenge

### 创建与可见性

- 发起方身份来自 Trainer Session；请求通过目标的完整 displayName 精确定位对方。
- 双方 Trainer 必须属于不同 Account；首版只允许挑战当前在线 Trainer。
- 发起方必须拥有合法 Team，并在创建时选择 Lead；服务端立即冻结发起方 Trainer Team Snapshot。
- Challenge 默认 5 分钟有效，创建后不因查询、断线或重连续期。
- Pending Challenge 不占用 Active Match 名额。
- 同一 Trainer 对不分方向最多一个 Pending Challenge；并发创建由数据库唯一约束与事务裁决。
- 收到方接受前只看方向、双方 displayName、规则、人数、状态与时间，不看发起方 creature、Lead、skills、item、ability 或数值。

### 生命周期

允许状态：

- `PENDING`
- `ACCEPTED`
- `REJECTED`
- `CANCELLED`
- `EXPIRED`
- `SUPERSEDED`

只允许 `PENDING` 转入任一终态。`CANCELLED` 原因仅：

- `WITHDRAWN`
- `TRAINER_ARCHIVED`
- `ROSTER_INVALIDATED`

### 接受

- 接受方必须在线，拥有合法且与发起方人数相同的 Team，并在接受时选择 Lead；服务端此时冻结接受方 Snapshot。
- 接受事务原子检查 Challenge 仍为 Pending 且未过期、双方仍在线、双方 Account 均无 Active Match、发起方 Snapshot 仍可由 Current Game Data 执行。
- 接受方 Team 当前无效只拒绝本次接受，不改变 Challenge；发起方 Snapshot 因 Current Game Data 变化失效则自动 `CANCELLED / ROSTER_INVALIDATED`。
- 接受成功后创建 Match，并把双方 Account 涉及的其他 Pending Challenge 原子转为 `SUPERSEDED`。
- 接口同步启动 Battle Session Runtime；成功直接返回 ACTIVE Match View，启动失败返回持久化的 `INTERRUPTED / START_FAILED` Match。

### 保留

- 终态 Challenge 保留 30 天。
- 未进入 Match 的 Snapshot 在 Challenge 清理时删除。
- Accepted Challenge 的双方 Snapshot 转归 Match，与 Match History 永久保留。

## Match

### 不变量与标识

- Match 固定两个不同 Account 所属 Trainer；同一 Account 的全部 Trainer 同时最多一场 Active Match。
- 玩家只看到 `matchId`，永远不看到或调用 `battleSessionId`。
- 服务端随机分配双方到 Battle Session side，并持久化映射；未知或不一致的 side 映射不能猜测结果。
- 首版格式固定为 `standard-single`，双方 Team 人数必须一致，全员 50 级。

### 生命周期

状态：

- `STARTING`
- `ACTIVE`
- `COMPLETED`
- `INTERRUPTED`

只允许：

- `STARTING -> ACTIVE | INTERRUPTED`
- `ACTIVE -> COMPLETED | INTERRUPTED`

中断原因：

- `START_FAILED`
- `RUNTIME_LOST`
- `RUNTIME_FAILED`

应用启动时把遗留 `STARTING` 转为 `INTERRUPTED / START_FAILED`，把遗留 `ACTIVE` 转为 `INTERRUPTED / RUNTIME_LOST`。首版不尝试恢复 Battle Session 或 Match Runtime。

### Match Result

Completed Match 必须拥有不可变 Result：

- `outcome`: `WIN | DRAW | NO_CONTEST`
- `reason`: `BATTLE | FORFEIT | TIMEOUT`
- `winnerTrainerId`: 仅 `WIN` 必填，其余为空。
- `battleReason`: 仅 `reason = BATTLE` 时存在。

合法组合仅：

- `WIN + BATTLE`
- `DRAW + BATTLE`
- `WIN + FORFEIT`
- `WIN + TIMEOUT`
- `NO_CONTEST + TIMEOUT`

无法从 Battle Session side 映射确定结果时转为 `INTERRUPTED / RUNTIME_FAILED`，不得猜测胜者。

## 回合协调

- 每方按当前 Match View 的人工 Turn Requirements 提交一份完整 Trainer Turn Submission。
- 提交携带 `submissionId` 与 `expectedRevision`；接受后不可修改。
- 单方提交不推进对外公开 Match revision，对方在自己提交前也看不到其是否已提交。
- 双方人工提交齐全后，Match 以稳定 side 顺序聚合为一个完整 Turn Command，并为 Battle Session 生成服务端 commandId。
- 无人工 Turn Requirements 的一方自动就绪；双方都无人工选择时自动推进。
- 连续自动回合默认最多 100，超过转为 `INTERRUPTED / RUNTIME_FAILED`。
- 每回合默认期限 90 秒，以数据库/服务端时钟固定 deadline；断线、重连、查询或 token 刷新都不重置。
- 单方未在期限内提交：`WIN + TIMEOUT`，另一方获胜。
- 双方都未提交：`NO_CONTEST + TIMEOUT`。
- Forfeit 只允许当前 Trainer 在 ACTIVE Match 发起；先持久化 `WIN + FORFEIT` Result，再尽力终止 Battle Session。
- 战斗完成、Forfeit、Timeout 与 Runtime failure 并发时，首个成功提交终态事务者生效，其他观察既有终态并幂等返回或冲突。

## 隐藏信息与 Match View

- 所有 Match API 必须根据当前 Trainer 投影 View，不得透传 Battle Session Snapshot。
- 开局公开对方 creature 种类、形态和 Team 数量。
- 隐藏对方 skills、item、ability、nature、IV、EV 与具体数值，只按战斗事件逐步揭示。
- 对局结束后也不自动公开对方完整 Team。
- 每个成功回合持久化精简 Disclosure Ledger，按双方视角记录已经揭示的对方 skills、ability 与 item。
- 不持久化完整事件流、随机轨迹、逐回合日志或回放。

## Match History

- `COMPLETED` 与 `INTERRUPTED` Match、Result、双方 Snapshot 与 Disclosure Ledger 永久保留，用户不可删除。
- Trainer 表不冗余保存胜负计数；Match Result 是战绩唯一事实来源。
- 列表返回：对手开局 displayName、Match 状态、Result 或 interruption reason、开始/结束时间、最终回合数。
- 详情增加：双方公开 Team Preview、己方完整 Snapshot、对方 Disclosure Ledger。
- 有效 Trainer 通过 Trainer Session 查询自己的历史；归档 Trainer 由所属 Account 通过 OAuth 账户级只读接口按该 Trainer 视角查询。

## API 与并发约定

- 玩家写命令统一使用 REST；WebSocket 只做认证心跳与最小失效通知。
- WebSocket 通知只包含 `type`、资源 ID 与 `revision`；客户端收到通知、revision 跳号或重连后通过 REST 拉取权威 View。
- Trainer、Team、Challenge、Match 使用 Long/CosId 内部 Identifier，并在 JSON 中以不透明十进制字符串表达；外部不得据此查找其他 Trainer。
- Trainer、Team、Challenge、Match 持久化 `revision`；更新命令携带 `expectedRevision`，不匹配返回 HTTP 409。
- 创建 Trainer 与 Challenge 携带 `commandId`；重复相同命令返回同一结果，不同载荷复用 commandId 返回 HTTP 409。
- Player OpenAPI 与管理端 OpenAPI 保持边界，不把 Battle Session 管理 DTO 复用为玩家契约。

建议资源族：

- Account Trainer：创建、列表、归档、恢复、归档历史。
- Trainer Session：进入、退出、当前状态。
- Trainer Team：读取、完整保存。
- Public Trainer Profile：按完整 displayName 精确查询。
- Challenge：创建、列表、详情、接受、拒绝、撤回。
- Match：当前对局、View、提交回合、Forfeit、History。
- Sensitive Name Rule Admin：分页、创建、更新、删除或停用。

具体 URL 与 DTO 名称由 OpenAPI interface 在实现切片中确定，但必须保持以上身份、可见性、幂等和 revision 语义。

## 模块与持久化边界

- 新增单个深 `match` Gradle module，内部可按 trainer、challenge、match package 组织，不拆成多个 Gradle module。
- Module 统一拥有 Trainer、Team、Trainer Session/Presence、Challenge、Match Runtime、持久化和玩家 interface。
- Module 定义小型 `BattleSessionHost` seam；`battle-rules` 提供生产 Adapter，测试提供内存 Adapter。
- Battle Session 继续只在内存中运行，不建表；`battle-session` 与 `battle-engine` 不知道 Account、Trainer 或 Match。
- 可编辑聚合关系化持久化；不可变 Trainer Team Snapshot 与 Disclosure Ledger 使用带 schemaVersion 的 JSONB。

## 前端范围

- 在 `avalon-admin-ui` 新增独立 `/play` 区域，与管理菜单、管理角色判断和 Admin Resource 页面分离。
- 无管理菜单的已登录账户默认进入 `/play`；管理员保持现有首页，并可切换进入 `/play`。
- 进入流程：无 Trainer 时显示创建引导；存在 Active Match 时只能恢复对应 Trainer；否则明确选择一个有效 Trainer；刷新页面后重新进入 Trainer，Active Match 自动限定可恢复身份。
- `/play` 首版覆盖 Trainer、唯一 Team、Challenge、Match 与 History。
- 敏感词库作为管理端基础数据页面，不放入 `/play`。
- Flutter、uni-app 等未来客户端复用相同 REST、OAuth、Trainer Session 与 WebSocket 契约。

## 验收策略

后端：

- 玩家 REST/OpenAPI interface 使用 PostgreSQL/Testcontainers，并替换为内存 `BattleSessionHost`。
- `BattleSessionHost` 契约测试同时约束内存测试 Adapter 与 `battle-rules` 生产 Adapter。
- 覆盖 displayName 规范化/唯一性/敏感词、三 Trainer 上限、归档恢复、Trainer Session 单账户替换、Challenge 并发接受、Match 终态竞争、超时、Runtime 丢失和按 Trainer 隐藏信息投影。
- 覆盖公共 password client、无 secret token 请求、refresh token 旋转/重放/滑动期限/七天上限，以及密码变化和账户禁用的全局撤销。

前端：

- `/play` 路由与进入流程组件测试。
- OAuth 单次串行刷新、Trainer Session header 注入、通知后 REST 刷新测试。
- 敏感词管理页 Contract Snapshot 与权限菜单测试。

端到端：

- 使用两个无管理角色的真实账户与两个独立浏览器上下文，不使用玩家业务 mock。
- 完成登录、创建/选择 Trainer、保存 Team、在线发现、Challenge、接受、双方提交、结果或 Forfeit、History 查询。
- WebSocket 只验证首帧认证、心跳、最小通知与通知后的 REST 刷新。
- 验证任一视角都不能读取对方未揭示的 skills、item、ability、nature 或数值。

## 完成定义

- 后端完整测试通过，Liquibase 从空 PostgreSQL 基线成功迁移。
- 前端 `npm run verify` 通过，OpenAPI 生成契约无手写漂移。
- 两账户真实 E2E 通过，并验证 Runtime 丢失后的 `INTERRUPTED` 行为。
- 除明确标记 superseded 的历史 ADR 外，`CONTEXT.md`、本规格与实现不存在 Trainer Code、可重命名 displayName、Battle Session 持久化或玩家 RBAC 等已被否决的模型残留。
