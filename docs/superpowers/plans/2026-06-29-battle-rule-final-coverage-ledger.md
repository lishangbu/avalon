# 现代主系列战斗规则最终覆盖账本

**目标口径：** 最终战斗系统按“可复用、可测试的规则行为”计数，不按每个具体生物、技能、特性或道具条目逐个计数。每个规则行为至少需要一个公开资料或成熟公开对战引擎行为作为对照 fixture；同一行为族可通过多条 fixture 覆盖不同资料条目。

截至最终规则边界收口 fixture 接入批次，当前覆盖报表中已有 `91` 个登记项为 `IMPLEMENTED`。其中部分登记项已经覆盖多个规则行为，例如场地持续时间、撒场、技能状态效果和特性标签增伤。折算到本账本后，V1 最终目标先定为 `312` 条规则行为，当前已覆盖 `312` 条，剩余 `0` 条。

该数字是工程账本，不是永久冻结的产品承诺。后续如果发现某个行为族必须拆成多个独立触发时机，目标总数会上调；如果多个计划项被证明是同一套稳定规则，目标总数会下调。任何调整都必须在本文件说明原因。

## 计数原则

- 一条规则行为必须有明确触发阶段，例如行动选择、命中前、伤害公式、伤害后、回合末或替换入场。
- 一条规则行为必须有明确输入和输出，例如能力阶级、状态、天气、场地、目标范围、随机序列、事件流或最终 HP。
- 文本说明、展示名称、图标、菜单、CRUD 字段不算战斗规则行为。
- 单个资料条目不直接算规则行为；只有它引入新的触发条件、取整位置、事件顺序或状态不变量时才单独计数。
- 已实现项必须进入覆盖报表、至少一条 fixture 和对应测试记录；仅建表、仅种数据、仅页面展示不算完成。

## 总量拆分

| 分组 | 目标规则数 | 当前已覆盖 | 剩余 | 说明 |
| --- | ---: | ---: | ---: | --- |
| 对战格式与队伍合法性 | 16 | 16 | 0 | 回合上限、队伍数量、等级统一、重复限制、禁用列表、选择阶段和自定义格式约束。 |
| 初始化、替换、濒死与胜负 | 18 | 18 | 0 | 初始出场、替换重置、强制替换、濒死检查、胜负判定、战斗结束事件。 |
| 回合流程、行动选择与行动排序 | 26 | 26 | 0 | PP、锁招、多回合技能、充能、反动不能行动、优先度、速度、同速随机和行动取消。 |
| 目标选择、双打范围与重定向 | 20 | 20 | 0 | 单体、相邻、全场、己方、随机目标、目标失效重定向和范围伤害。 |
| 命中、保护、替身、免疫与反射 | 28 | 28 | 0 | 命中/闪避、保护、替身、属性/状态免疫、声音穿透、粉末、抢夺、反射类变化技能和行动前目标有效性边界。 |
| 伤害公式、能力值、属性与取整 | 42 | 42 | 0 | 普通伤害、击中要害、属性一致加成、克制、天气/场地修正、攻防能力值修正、固定伤害、比例伤害和 HP 派生直接伤害。 |
| 主要状态、临时状态与持续状态 | 34 | 34 | 0 | 灼伤、麻痹、睡眠、冰冻、中毒、剧毒、混乱、畏缩、回复封锁、挑衅、定身法、无理取闹、束缚、诅咒、着迷和持续回合。 |
| 天气、场地、场地状态和一侧状态 | 31 | 31 | 0 | 晴、雨、沙、雪、电气、青草、薄雾、精神、屏障、顺风、撒场、天气/场地持续时间。 |
| 技能效果行为族 | 39 | 39 | 0 | 能力阶级、主要状态、HP 吸取/反伤/回复、强制替换、复制、封锁、清除、交换、取反和失败条件。 |
| 特性效果行为族 | 36 | 36 | 0 | 入场、攻击前、防守前、命中后、天气/场地联动、属性吸收、状态免疫、规则绕过和伤害修正。 |
| 道具效果行为族 | 18 | 18 | 0 | 消耗、回复、状态解除、伤害增减、持续时间延长、一次性免死、锁招、蓄力跳过和抗性减伤。 |
| 随机、回放和对照测试基础 | 4 | 4 | 0 | 固定随机序列、事件流稳定、回放复算和对照测试结果归档。 |
| **合计** | **312** | **312** | **0** | 当前“已覆盖”按行为账本粗映射；覆盖报表实际登记项为 `91/91`。 |

## 规则族到测试文件矩阵

这张矩阵是 `battle-engine/src/test/kotlin/io/github/lishangbu/battleengine/BattleRuleCoverageLedgerTests.kt`
的人工可读版本。测试类仍然是事实源；文档只负责帮助开发时快速定位“某个规则族应该看哪批测试”。新增规则时，
先判断它是否只是资料条目扩展；如果不是，必须把对应行为测试补到下表所属规则族，必要时再调整规则族计数。

| 规则族 code | 规则数 | 主要测试文件 |
| --- | ---: | --- |
| `format-and-team-validation` | 16 | `BattleFormatValidationTests`、`BattlePreparationValidatorTests` |
| `lifecycle-switch-faint-result` | 18 | `BattleLifecycleSwitchPublicReferenceTests`、`BattleFormatLifecycleBoundaryPublicReferenceTests` |
| `turn-flow-action-ordering` | 26 | `BattleActionOrderingPublicReferenceTests`、`BattleActionValidatorTests`、`BattleActionFlowBoundaryTests` |
| `target-scope-redirection` | 20 | `BattleTargetScopePublicReferenceTests`、`BattleTargetRedirectionPublicReferenceTests`、`BattleRandomTargetPublicReferenceTests` |
| `hit-protect-substitute-immunity-reflect` | 28 | `BattleHitDefenseBoundaryPublicReferenceTests`、`BattleSubstituteTests`、`BattleImmunityTests` |
| `damage-formula-stat-element-rounding` | 42 | `damage/BattleDamageFormulaBoundaryPublicReferenceTests`、`damage/BattleDamageCalculatorTests`、`BattleCriticalHitFlowTests` |
| `major-volatile-persistent-status` | 34 | `BattleResidualStatusTests`、`BattleVolatileStatusTests`、`BattleBindingStatusTests`、`BattleDisableTests` |
| `weather-terrain-field-side-condition` | 31 | `BattleWeatherEffectTests`、`BattleTerrainEffectTests`、`BattleEnvironmentFieldBoundaryPublicReferenceTests` |
| `skill-effect-family` | 39 | `BattleSkillEffectBoundaryPublicReferenceTests`、`BattleSkillStatStageEffectTests`、`BattleSkillHpEffectTests` |
| `ability-effect-family` | 36 | `BattleSwitchInAbilityTests`、`BattleAbilityItemBoundaryPublicReferenceTests`、`BattleTargetAbilityIgnoreTests` |
| `item-effect-family` | 18 | `BattleHeldItemPublicReferenceTests`、`BattleElementDamageReductionItemTests`、`BattleStatusCureItemTests` |
| `random-replay-public-reference` | 4 | `random/ScriptedBattleRandomTests`、`BattleReplayRecorderTests`、`BattleReplayPublicReferenceTests` |

## 当前报表口径

当前 `BattleRuleCoverageService` 的报表是第一阶段的上线清单，不是最终系统清单：

- 报表登记项：`91`
- 已实现：`91`
- 部分实现：`0`
- 计划中：`0`
- 报表口径剩余：`0`

由于部分报表项已经覆盖多个行为，例如场地持续时间、撒场、技能状态效果或特性标签增伤，映射到最终账本时会折算为多个行为；所以上方总量拆分里的“当前已覆盖”是 `312`，而不是报表登记项 `91`。

## 统一测试节奏

后续按用户要求调整为：

- 小批开发期间不每条规则都跑完整测试。
- 复杂建模时只做必要的轻量编译或局部检查，避免长时间卡在重复测试上。
- 每累计完成 20 条规则行为，对这 20 条规则相关测试做一次整体回归，确认批内规则之间没有互相破坏。
- 一个规则批次完成后统一跑：`./gradlew :battle-engine:test :battle-rules:test :migration:test`。
- 涉及应用启动、OpenAPI、真实数据库迁移或管理端页面时，再额外重启前后端并做运行态验证。

## 最终收口状态

1. 命中、保护、替身、免疫与反射分组已通过最终边界 fixture 收口，额外覆盖不受保护影响技能和目标倒下后的行动取消。
2. 技能效果行为族已通过最终边界 fixture 收口，额外覆盖全场能力阶级清除、0% 状态效果、复制、交换和取反边界。
3. 道具效果行为族已通过最终边界 fixture 收口，额外覆盖蓄力跳过道具消费、免死后不继续触发同道具回复，以及非消耗治愈道具。
