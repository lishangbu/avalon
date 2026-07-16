# Playable Battle Simulator

Avalon 首发为中文响应式 Web 对战模拟器。正式环境只发布原创或明确授权的 Content Pack；当前导入的宝可梦资料作为开发、测试和第九世代规则对照数据，不得进入生产内容包。

## Player journey

玩家使用邀请码注册账户，首次进入时创建唯一 Trainer 并获得一支合法入门队伍。玩家可以浏览 Player Catalog、维护最多二十支命名队伍、导入导出队伍、选择免费 Creature Skin，并通过精确 Trainer 名称发起好友 Challenge，或立即开始 Practice Battle。玩家端位于现有 `avalon-admin-ui` 的独立 `/play/**` 路由树，并同时支持桌面与手机浏览器。

## Content and catalog

- Content Pack 以 Draft、Published、Retired 生命周期管理中文资料、规则、媒体来源、许可证、署名、版本和摘要；发布是原子操作，Published 内容不可原地覆写。
- Player Catalog 要求账户登录但不要求 Trainer Session，只返回 Current Game Data 中 `enabled=true` 的内容。
- 技能、特性和道具分别保留 `shortEffect`、`effect`、`flavorText` 语义；精灵使用 `genus` 与 `flavorText`，不新增通用 description。
- Creature Skin 不影响战斗。每个 Creature 恰有一个默认 Skin；Team 和 Battle 快照冻结 Skin。首发所有启用 Skin 均可自由选择。
- 公共素材通过稳定资源键和公开只读 CDN 地址交付。每个可选 Creature/Skin 必须有头像、正面图和背面图，每个可携带道具必须有图标。
- 道具使用方式区分 Held、Battle Consumable、Capture、Evolution、Training、Key 和 Material；首发真人对战只允许 Held 道具且不能主动使用背包。

## Teams and format

- 每个 Trainer 最多二十支 Team，并可选择默认 Team。队伍分享使用不可枚举短码，导入创建独立副本且重新校验。
- Team 成员包含 Creature、Skin、1–4 个 Skill、Ability、Held Item、Nature、IV、EV 和 tera element。IV/EV/Nature 在玩家端作为高级设置。
- 内容更新使 Team 不合法时保留并标记 Invalid；不能创建 Challenge 或 Practice Battle，系统不自动修改配置。
- 首发 `standard-single` 固定 6v6、等级 50、每回合 90 秒、整场 30 分钟。
- 赛制采用第九世代 National Dex 全图鉴与合法配招、第九世代战斗语义，只允许 Terastallization；Mega、Z-Move 和 Dynamax 不可用。
- 启用 Species、Item、Sleep、OHKO、Evasion 和 Endless Battle clauses；首发不维护额外 ban list。
- Battle-only form 不能直接组队；形态依赖不合法时服务端返回结构化错误。

## Match experience

- Challenge 阶段不公开队伍。接受后 Match 进入 60 秒 Team Preview，只公开 Creature、战斗前 Form 和 Skin；双方秘密锁定 lead，超时由服务端随机选择。
- Terastallization 与当回合 Skill action 绑定，不能和 Switch 组合；每方整场一次，tera element 在发动前隐藏并在发动后进入 Disclosure Ledger。
- Battle Session 输出稳定 code 与参数组成的 Battle Event，客户端负责中文文本和动画，Match 服务端按视角过滤事件。
- 首发支持好友 Challenge、再战请求和可屏蔽的预设短语；不支持随机匹配、排行榜、观战、自由文本聊天或永久完整回放。
- Runtime 丢失使 Match 或 Practice Battle 中断；当前范围不建设部署排空或运行态故障恢复。

## Rules completeness

- 目标覆盖当前导入资料中全部现代可用技能、全部特性的对战内语义和全部 Held 道具；对战外捕捉、培养、进化与经济效果归未来模块。
- 规则由数据库结构化 effect policy 与 Kotlin 效果族执行器组成，不执行数据库脚本或动态表达式。
- Wiki 和官方资料用于解释规则；固定版本 Pokémon Showdown 用于离线生成并审核 fixtures。常规 CI 只运行已提交的 Kotlin fixtures。
- 单打完整后再发布双打。正式发布前 CI 必须证明现代单打规则、合法性、必需素材和授权元数据达到 100% 门禁。

## Practice

- Practice Battle 不经过 Presence、Challenge 或假账号，不进入正式 Match History、战绩或奖励。
- 玩家使用当前合法 Team；对手来自管理员维护的版本化 Bot Team Template。
- 简单 Bot 从合法行动中随机选择；标准 Bot 评估克制、伤害、击杀、状态、替换与 Terastallization，且只读取自身视角。
- 同一账户最多一场活跃战斗，真人 Match 与 Practice Battle 共享占用约束。Practice Runtime 丢失后中断并允许重开。

## Account and quality

- 邀请码具有使用次数和失效时间；首发不接邮箱、短信或第三方登录，密码由管理员重置。
- 一个账户只有一个 Trainer；名称每三十天可修改一次。账户注销有七天冷静期，到期撤销身份并匿名化历史参与者。
- 只收集服务端 Match 事实的匿名聚合，不收集点击轨迹或设备指纹。
- 首发满足基础 WCAG 2.2 AA。单实例容量目标为 500 WebSocket、100 场活跃战斗、普通回合结算 p95 小于 250ms、Catalog 缓存命中 p95 小于 150ms。

## Deferred RPG seam

首发不实现捕捉、Owned Creature、经验、升级、背包、经济、地图或剧情 NPC。未来模拟器 Team 与 RPG Owned Team 都装配为相同的不可变 Session Roster；公共 Creature 和 Item 资料不能承载玩家拥有状态。
