# 类宝可梦玩法是否需要 NPC 与 NPC 对战

调研日期：2026-07-15

## 结论

如果 Avalon 仍定位为“玩家组队后进行真人标准单打”的竞技场，NPC 不是上线所必需；练习模式甚至可以只暴露一个无人物设定的 Bot。

如果目标扩大为“玩家可以独自进入、学习、成长、反复游玩，并逐步挑战更强对手”的类宝可梦游戏，则应引入 NPC 对战，而且应把它列为前置能力。官方游戏说明展示的核心单人路径包含道馆挑战、组织手下与首领、引导型训练家和野生宝可梦；Wiki 资料还显示对战设施用连续挑战提供可重复内容，重战用变强的阵容延长既有内容寿命。[S1][S3][S4][S5]

但不应引入一个包办一切的 `Npc` 模型，也不应通过创建假账号、假 Trainer Session 或假在线状态来复用现有真人 Challenge。至少要区分：

1. **内容角色**：承担对白、引导、剧情、商店或任务；不一定能对战。
2. **脚本训练家对手**：有名字、训练家类型、固定或条件化阵容、胜负对白、进度门槛、奖励与重战规则。
3. **通用 AI 对手（Bot）**：只负责替某一战斗 Side 选择行动；可以用于练习、对战设施，也可以被脚本训练家复用，但本身不是人物。
4. **野生宝可梦**：不是 Trainer，也不是 NPC Trainer；它没有账号和训练家身份，失败后的捕捉、掉落、逃跑等结果也不同于训练家对战。[S5][S6]

因此，建议先建设“服务端控制的战斗参与方 + 可插拔行动策略”，再落地最小 NPC 对战；内容角色系统和野生遭遇应分别按实际玩法引入。

## 可验证事实

| Claim | 证据及其设计含义 | 来源 |
| --- | --- | --- |
| 《宝可梦 朱／紫》的官方商品说明把 `catch, battle, and train` 并列为基本循环。 | 若 Avalon 要覆盖类宝可梦的完整单人循环，仅有真人 Match 不够；收集/成长与对战需要共同驱动进度。 | Nintendo 官方商品页[S1] |
| 官方说明中的 Victory Road 要玩家挑战不同地点的道馆并争取 Champion Rank；Starfall Street 要通过 Auto Battle 击败组织手下以接近首领。 | 固定训练家不是单纯匹配池填充物，而是关卡、进度门槛和首领结构的一部分。 | Nintendo 官方商品页[S1] |
| 官方说明称 Nemona 是经验丰富的 Trainer，也是玩家冒险中的可靠向导。 | “内容角色”和“战斗对手”是可叠加的角色能力，但不应因此成为同一个不可拆分的基类。 | Nintendo 官方商品页[S1] |
| 官方说明把 Tera Raid 的目标称为野生宝可梦，并说明击败后可能捕捉。 | 野生战斗在结算时产生捕捉机会，不能等同于击败 NPC Trainer。 | Nintendo 官方商品页[S1] |
| Bulbapedia 将 NPC 定义为行动不能由操作主机的玩家控制的游戏角色。 | NPC 是交互/控制方式分类，不等价于“AI 战斗方”；很多 NPC 不战斗，野生单位也不必建成人形内容角色。 | Bulbapedia NPC 条目[S2] |
| Bulbapedia 将 Trainer class 描述为核心系列中玩家可以对战的非玩家训练家类型；道馆馆主、四天王、冠军和对战设施首领构成不同层级。 | NPC Trainer 需要可配置身份/类型、阵容与内容角色关联；难度与进度可以由数据配置，而非写死在 AI 类中。 | Bulbapedia Pokémon Trainer 条目[S3] |
| 神奇宝贝百科称很多训练家会对战；训练家类型会反映其宝可梦选择、地点、招式训练与水平；挑战并战胜馆主可以获得徽章。 | 中文 Wiki 与 Bulbapedia 相互印证“训练家类型 + 阵容倾向 + 进度奖励”的内容职责。 | 神奇宝贝百科“宝可梦训练家”条目[S7] |
| Bulbapedia 将 Battle facility 描述为与其他 Trainer 对战的地点，通常不获得经验或奖金，但追踪连胜并可获得 Battle Points。 | 对战设施需要的是可复用 Bot、阵容生成/选择、连胜状态和设施奖励，不需要为每个对手建立完整剧情 NPC。 | Bulbapedia Battle facility 条目[S4] |
| Bulbapedia 将 rematch 定义为与 Trainer 或 Pokémon 的第二次及后续对战，并说明训练家通常在满足条件后可重战，届时阵容可能升级、进化或增加成员。 | 重战策略应是内容配置：解锁条件、阵容版本和奖励衰减/刷新；不要复制一批“更强 NPC”。 | Bulbapedia Rematch 条目[S5] |
| Bulbapedia 将 wild Pokémon 定义为当前不属于任何 Trainer 的宝可梦。 | 野生对手不能靠一个 `trainerId` 表达，未来捕捉还会改变归属；它应拥有独立的 Encounter 语义。 | Bulbapedia Wild Pokémon 条目[S6] |

## 对 Avalon 现状的判断

现有能力清晰地服务于真人 PvP：

- 首版 Challenge 只允许向持有有效 Session 的在线 Trainer 发起，并在接受时再次检查双方在线。[ADR 0029](../adr/0029-limit-first-challenges-to-online-trainers.md)
- 首版 Match 固定为两名 Trainer 的 `standard-single`。[ADR 0034](../adr/0034-limit-first-matches-to-standard-single.md)
- `match` 模块统一拥有 Trainer、Presence、Challenge、Match Runtime 与玩家接口，但 Battle Runtime 已经通过 `BattleSessionHost` seam 隔离。[ADR 0047](../adr/0047-own-player-match-behavior-in-one-deep-module.md)
- 当前数据库的 `match_participant.trainer_id`、`account_id` 和 `winner_trainer_id` 都围绕真人 Trainer 建模；回合提交也以 `trainer_id` 作为身份和幂等边界。[030-match-schema.yaml](../../migration/src/main/resources/db/changelog/baseline/030-match-schema.yaml)
- `advanceAutomaticTurns` 只推进“不需要选择”的空行动回合；它不是会替某一 Side 选择技能或换人的战斗 AI。[MatchService.kt](../../match/src/main/kotlin/io/github/lishangbu/match/game/MatchService.kt)

所以现有引擎和 Session Host 可以复用，但现有 Challenge、Presence 与 Match 参与方模型不能直接代表 NPC。强行复用会产生错误规则，例如要求 NPC 在线、给 NPC 建账号、把野生胜负写入 Trainer 战绩，或让脚本战斗受五分钟真人邀请生命周期约束。

## 推荐的领域边界

### 1. 战斗控制权独立于人物身份

先定义 Side 的控制方式，而不是先定义 `NpcTrainer : Trainer`：

- `HUMAN`：由已认证的 Trainer 提交行动。
- `SERVER`：服务端通过一个 `BattlePolicy`/`ActionSelector` 读取当前可见状态与合法选择并产生行动。

策略至少要可确定性测试，随机选择必须接受服务端提供的已记录随机源。难度可以逐步从合法随机、评分启发式升级，但不应改变 Battle Engine 的规则裁定职责。

### 2. PvE Encounter 与真人 Challenge 分开

建议新增 PvE `Encounter` 应用边界，直接创建一个人类 Side 和一个服务端 Side，并复用 `BattleSessionHost`、队伍快照与战斗视图投影。不要先把现有 `MatchChallenge` 泛化成同时表示邀请、剧情关卡、野外遭遇和设施连胜的万能聚合。

是否让 PvE 结果最终进入统一 `Match History`，可以等产品确定战绩展示后再决定；在此之前至少将 `mode`、参与方种类和结果归属建模清楚，避免把 NPC 胜场计入 PvP 战绩。

### 3. 三类持久化内容分开

- `ContentCharacter`：名称、外观引用、对白/脚本引用、位置或内容标签；可选地关联一个脚本训练家配置。
- `TrainerEncounterDefinition`：阵容版本、AI 策略、规则格式、解锁条件、首次/重复奖励、失败与胜利后续、重战策略。
- `WildEncounterDefinition`：出现条件、物种/等级或生成表、AI 策略、逃跑/捕捉/掉落规则；不含 Trainer 身份。

练习 Bot 和对战设施的临时对手可以只使用“阵容生成器 + AI 策略”，无需创建 `ContentCharacter`。

## 建议实施顺序

1. **练习 Bot 垂直切片**：玩家使用现有 Team 立即开战；服务端使用预设阵容和最简单合法行动策略；不经过 Trainer 查找、Presence 或 Challenge；结果明确标记为 `PRACTICE`，不改 PvP 战绩。它验证的核心不是内容，而是服务端控制 Side 的完整回合、换人、超时、恢复与投影。
2. **脚本训练家对战**：加入可版本化阵容、难度策略、首次通关、奖励幂等和可选重战。选择一个“新手教练 → 普通训练家 → 小首领”的三战路径验证进度，而不是立即搭完整世界地图。
3. **道馆/联盟与对战设施**：前者组合有身份的固定关卡与进度门槛，后者组合无剧情对手池、阵容生成、连胜和设施奖励；两者共享 AI 控制能力，不共享全部内容模型。
4. **野生遭遇与捕捉**：在宝可梦所有权、队伍/盒子容量、捕捉概率、消耗品、掉落和事务幂等性确定后单独实现。不要为了尽快出现“野怪”而把它伪装成 NPC Trainer。

## 在编码前还需查清的问题

1. 第一版扩充目标是“随时能单人练习”，还是“有进度的短篇 PvE”？前者只需要 Bot；后者从第一天就需要脚本训练家和通关状态。
2. 玩家的 Team 是继续自由编辑，还是由捕捉/奖励获得？如果继续自由编辑，道馆奖励和野生捕捉的长期价值会很弱。
3. PvE 是否沿用标准单打和等级 50 归一化？成长玩法通常需要保留等级差，而竞技设施可以继续归一化。
4. AI 是否允许读取玩家未公开信息？应明确其观测边界，否则“高难度”很容易变成作弊。
5. 失败、断线、服务重启和重复请求如何结算奖励、消耗品和进度？所有奖励必须由一次 Encounter 结果幂等发放。
6. NPC 内容由代码、数据库还是外部内容文件维护？阵容、对白、关卡条件和奖励需要版本化，已开始的 Encounter 应冻结其内容版本。
7. 是否需要可解释的难度档位、队伍克制教学和战后提示？若需要，AI 决策应输出内部 reason trace，但不能向客户端泄露隐藏战斗信息。

## 来源

- [S1] Nintendo, *Pokémon™ Scarlet for Nintendo Switch*, https://www.nintendo.com/us/store/products/pokemon-scarlet-switch/ （访问日期：2026-07-15）。官方商品说明；用于核实捕捉/对战/训练、Victory Road、Starfall Street、Nemona 和 Tera Raid 的官方玩法描述。
- [S2] Bulbapedia, *Non-player character*, https://bulbapedia.bulbagarden.net/wiki/Non-player_character （访问日期：2026-07-15）。社区 Wiki；用于术语定义。
- [S3] Bulbapedia, *Pokémon Trainer*, https://bulbapedia.bulbagarden.net/wiki/Pok%C3%A9mon_Trainer （访问日期：2026-07-15）。社区 Wiki；用于训练家类型与道馆/联盟/设施训练家层级。
- [S4] Bulbapedia, *Battle facility*, https://bulbapedia.bulbagarden.net/wiki/Battle_facility （访问日期：2026-07-15）。社区 Wiki；用于设施挑战、连胜与 Battle Points。
- [S5] Bulbapedia, *Rematch*, https://bulbapedia.bulbagarden.net/wiki/Rematch （访问日期：2026-07-15）。社区 Wiki；用于重战条件与阵容增强。
- [S6] Bulbapedia, *Wild Pokémon*, https://bulbapedia.bulbagarden.net/wiki/Wild_Pok%C3%A9mon （访问日期：2026-07-15）。社区 Wiki；用于野生宝可梦的归属定义。
- [S7] 神奇宝贝百科, *宝可梦训练家*, https://wiki.52poke.com/wiki/%E5%AE%9D%E5%8F%AF%E6%A2%A6%E8%AE%AD%E7%BB%83%E5%AE%B6 （访问日期：2026-07-15）。社区 Wiki；用于中文术语、训练家类型、道馆徽章和联盟层级的交叉核验。

## 来源局限

Nintendo 商品页是一手产品说明，但不是实现规格；它能证明这些玩法在官方产品中的职责，不能证明 Avalon 必须复制其具体规则。Bulbapedia 与神奇宝贝百科是社区维护的二手资料，本报告仅用它们归纳跨世代模式，并用官方商品说明交叉验证关键玩法方向。最终产品边界仍需由 Avalon 的目标体验决定。
