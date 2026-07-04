# 现代主系列战斗规则最终覆盖账本

**目标口径：** 最终战斗系统按“可复用、可测试的规则行为”计数，不按每个具体精灵、技能、特性或道具条目逐个计数。每个规则行为至少需要一个公开资料或成熟公开对战引擎行为作为对照场景；同一行为族可通过多条场景覆盖不同资料条目。

截至最终规则边界收口批次，V1 最终目标先定为 `312` 条规则行为，当前已覆盖 `312` 条，剩余 `0` 条。覆盖事实源是 `BattleRuleCoverageLedgerTests` 和各行为测试中的 `assertNamed` 场景名，不再维护独立场景表或额外覆盖服务。

该数字是工程账本，不是永久冻结的产品承诺。后续如果发现某个行为族必须拆成多个独立触发时机，目标总数会上调；如果多个计划项被证明是同一套稳定规则，目标总数会下调。任何调整都必须在本文件说明原因。

## 计数原则

- 一条规则行为必须有明确触发阶段，例如行动选择、命中前、伤害公式、伤害后、回合末或替换入场。
- 一条规则行为必须有明确输入和输出，例如能力阶级、状态、天气、场地、目标范围、随机序列、事件流或最终 HP。
- 文本说明、展示名称、图标、菜单、CRUD 字段不算战斗规则行为。
- 单个资料条目不直接算规则行为；只有它引入新的触发条件、取整位置、事件顺序或状态不变量时才单独计数。
- 已实现项必须进入覆盖账本、至少一条公开规则场景和对应测试记录；仅建表、仅种数据、仅页面展示不算完成。

## 总量拆分

| 分组 | 规则编号区间 | 目标规则数 | 当前已覆盖 | 剩余 | 说明 |
| --- | ---: | ---: | ---: | ---: | --- |
| 对战格式与队伍合法性 | 1-16 | 16 | 16 | 0 | 回合上限、队伍数量、等级统一、重复限制、禁用列表、选择阶段和自定义格式约束。 |
| 初始化、替换、濒死与胜负 | 17-34 | 18 | 18 | 0 | 初始出场、替换重置、强制替换、濒死检查、胜负判定、战斗结束事件。 |
| 回合流程、行动选择与行动排序 | 35-60 | 26 | 26 | 0 | PP、锁招、多回合技能、充能、反动不能行动、优先度、速度、同速随机和行动取消。 |
| 目标选择、双打范围与重定向 | 61-80 | 20 | 20 | 0 | 单体、相邻、全场、己方、随机目标、目标失效重定向和范围伤害。 |
| 命中、保护、替身、免疫与反射 | 81-108 | 28 | 28 | 0 | 命中/闪避、保护、替身、属性/状态免疫、声音穿透、粉末、抢夺、反射类变化技能和行动前目标有效性边界。 |
| 伤害公式、能力值、属性与取整 | 109-150 | 42 | 42 | 0 | 普通伤害、击中要害、属性一致加成、克制、天气/场地修正、攻防能力值修正、固定伤害、比例伤害和 HP 派生直接伤害。 |
| 主要状态、临时状态与持续状态 | 151-184 | 34 | 34 | 0 | 灼伤、麻痹、睡眠、冰冻、中毒、剧毒、混乱、畏缩、回复封锁、挑衅、定身法、无理取闹、束缚、诅咒、着迷和持续回合。 |
| 天气、场地、场地状态和一侧状态 | 185-215 | 31 | 31 | 0 | 晴、雨、沙、雪、电气、青草、薄雾、精神、屏障、顺风、撒场、天气/场地持续时间。 |
| 技能效果行为族 | 216-254 | 39 | 39 | 0 | 能力阶级、主要状态、HP 吸取/反伤/回复、强制替换、复制、封锁、清除、交换、取反和失败条件。 |
| 特性效果行为族 | 255-290 | 36 | 36 | 0 | 入场、攻击前、防守前、命中后、天气/场地联动、属性吸收、状态免疫、规则绕过和伤害修正。 |
| 道具效果行为族 | 291-308 | 18 | 18 | 0 | 消耗、回复、状态解除、伤害增减、持续时间延长、一次性免死、锁招、蓄力跳过和抗性减伤。 |
| 随机、回放和对照测试基础 | 309-312 | 4 | 4 | 0 | 固定随机序列、事件流稳定、回放复算和对照场景账本归档。 |
| **合计** | **1-312** | **312** | **312** | **0** | 当前“已覆盖”以行为测试账本为事实源。 |

## 当前覆盖清单结论

| 规则族 code | 单测覆盖状态 | 缺口 | 可合并评估 |
| --- | --- | ---: | --- |
| `format-and-team-validation` | 已绑定行为测试和公开场景锚点 | 0 | 不合并；这是战斗开始前的输入合法性边界。 |
| `lifecycle-switch-faint-result` | 已绑定行为测试和公开场景锚点 | 0 | 不合并；它维护替换、濒死和胜负事件顺序。 |
| `turn-flow-action-ordering` | 已绑定行为测试和公开场景锚点 | 0 | 不合并；行动顺序和随机消耗是独立高风险边界。 |
| `target-scope-redirection` | 已绑定行为测试和公开场景锚点 | 0 | 不合并；目标选择必须独立于命中和伤害公式。 |
| `hit-protect-substitute-immunity-reflect` | 已绑定行为测试和公开场景锚点 | 0 | 不合并；这是命中前目标门禁规则族。 |
| `damage-formula-stat-element-rounding` | 已绑定行为测试和公开场景锚点 | 0 | 不合并；取整、属性和能力值修正需要集中维护。 |
| `major-volatile-persistent-status` | 已绑定行为测试和公开场景锚点 | 0 | 不合并；状态持续回合和行动阻止共享同一族内不变量。 |
| `weather-terrain-field-side-condition` | 已绑定行为测试和公开场景锚点 | 0 | 不合并；天气、场地和一侧状态共享环境生命周期边界。 |
| `skill-effect-family` | 已绑定行为测试和公开场景锚点 | 0 | 不合并；技能效果是资料扩展最频繁的运行时入口。 |
| `ability-effect-family` | 已绑定行为测试和公开场景锚点 | 0 | 不合并；特性触发时机跨入场、命中前后和回合末。 |
| `item-effect-family` | 已绑定行为测试和公开场景锚点 | 0 | 不合并；道具消费、一次性和非消耗效果需要独立检查。 |
| `random-replay-public-reference` | 已绑定行为测试和公开场景锚点 | 0 | 不合并；它是其它规则族的可复现测试基础。 |

当前没有建议继续合并的规则族。12 个规则族已经是比 312 条逐条维护更小的账本边界；如果后续证明两个规则族共享
同一个触发阶段、事件顺序和状态不变量，再同时修改 `BattleRuleCoverageLedgerTests` 与本文件。

## 规则族到测试文件矩阵

这张矩阵是 `battle-engine/src/test/kotlin/io/github/lishangbu/battleengine/BattleRuleCoverageLedgerTests.kt`
的人工可读版本。测试类仍然是事实源；文档只负责帮助开发时快速定位“某个规则族应该看哪批测试”。新增规则时，
先判断它是否只是资料条目扩展；如果不是，必须把对应行为测试补到下表所属规则族，必要时再调整规则族计数。

账本测试会根据下表顺序自动生成 `1..312` 的规则编号到 `assertNamed` 公开场景锚点映射。映射只使用同一规则族内
的测试场景；当前口径要求每个规则编号绑定族内唯一场景名，避免多个编号复用同一个锚点后掩盖缺口。这样既能从
任意规则编号定位到具体测试类和场景名，也不用维护一张容易过期的 312 行手写表。

| 规则族 code | 规则编号区间 | 规则数 | 主要测试文件 |
| --- | ---: | ---: | --- |
| `format-and-team-validation` | 1-16 | 16 | `BattleFormatValidationTests`、`BattlePreparationValidatorTests`、`BattleValidationPublicReferenceTests` |
| `lifecycle-switch-faint-result` | 17-34 | 18 | `BattleLifecycleSwitchPublicReferenceTests`、`BattleFormatLifecycleBoundaryPublicReferenceTests`、`BattleEnginePublicReferenceTests`、`BattleEngineSingleTurnTests`、`BattleEntryHazardTests`、`BattleFinalRuleBoundaryPublicReferenceTests` |
| `turn-flow-action-ordering` | 35-60 | 26 | `BattleActionOrderingPublicReferenceTests`、`BattleActionValidatorTests`、`BattleActionFlowBoundaryTests`、`BattleChargeSkillTests`、`BattleLockedMoveTests`、`BattleMultiHitSkillTests`、`BattleRechargeSkillTests` |
| `target-scope-redirection` | 61-80 | 20 | `BattleTargetScopePublicReferenceTests`、`BattleTargetRedirectionPublicReferenceTests`、`BattleRandomTargetPublicReferenceTests` |
| `hit-protect-substitute-immunity-reflect` | 81-108 | 28 | `BattleHitDefenseBoundaryPublicReferenceTests`、`BattleSubstituteTests`、`BattleImmunityTests`、`BattleAccuracyStatStageIgnoreAbilityTests`、`BattlePsychicTerrainTests`、`BattleSoundAbilityTests`、`BattleStatusImmunityAndGroundingTests` |
| `damage-formula-stat-element-rounding` | 109-150 | 42 | `damage/BattleDamageFormulaBoundaryPublicReferenceTests`、`damage/BattleDamageCalculatorTests`、`damage/BattleDamageStatStageIgnoreAbilityTests`、`BattleCriticalHitFlowTests`、`BattleCriticalHitImmunityAbilityTests`、`BattleFixedDamageSkillTests`、`BattleHpDerivedDamageSkillTests`、`BattleOneHitKnockOutSkillTests`、`BattleProportionalDamageSkillTests`、`BattleSpeedRatioPowerSkillTests` |
| `major-volatile-persistent-status` | 151-184 | 34 | `BattleResidualStatusTests`、`BattleVolatileStatusTests`、`BattleBindingStatusTests`、`BattleDisableTests`、`BattleFreezeStatusTests`、`BattleHealBlockTests`、`BattleParalysisStatusTests`、`BattleSleepStatusTests`、`BattleTauntTests`、`BattleTormentTests` |
| `weather-terrain-field-side-condition` | 185-215 | 31 | `BattleWeatherEffectTests`、`BattleTerrainEffectTests`、`BattleEnvironmentFieldBoundaryPublicReferenceTests`、`BattleEnvironmentDurationTests`、`BattleSkillEnvironmentEffectTests`、`BattleWeatherElementOverrideTests` |
| `skill-effect-family` | 216-254 | 39 | `BattleSkillEffectBoundaryPublicReferenceTests`、`BattleSkillStatStageEffectTests`、`BattleSkillHpEffectTests`、`BattlePostDamageStatusCureSkillTests`、`BattleUserElementRemovalSkillTests`、`BattleForcedSwitchSkillTests`、`BattleSkillRecoilImmunityAbilityTests`、`BattleStatStageOperationSkillTests`、`BattleSkillWeightEffectTests` |
| `ability-effect-family` | 255-290 | 36 | `BattleSwitchInAbilityTests`、`BattleAbilityItemBoundaryPublicReferenceTests`、`BattleTargetAbilityIgnoreTests`、`BattleContactAbilityPublicReferenceTests`、`BattleElementAbsorbAbilityTests`、`BattleElementAbsorbStatAbilityTests`、`BattleIndirectDamageImmunityTests`、`BattlePriorityAbilityTests`、`BattleStatusPriorityAbilityTests` |
| `item-effect-family` | 291-308 | 18 | `BattleHeldItemPublicReferenceTests`、`BattleConditionalDamageBoostItemTests`、`BattleDamageDealtHealingItemTests`、`BattleElementDamageBoostItemTests`、`BattleElementDamageReductionItemTests`、`BattleFatalDamageSurvivalTests`、`BattleStatusCureItemTests`、`BattleVolatileStatusCureItemTests` |
| `random-replay-public-reference` | 309-312 | 4 | `random/ScriptedBattleRandomTests`、`BattleReplayRecorderTests`、`BattleReplayPublicReferenceTests` |

## 底层状态迁移补充测试

规则族矩阵只登记带 `assertNamed` 的公开规则场景；本轮新增的成员运行态操作测试属于更底层的防回归测试，不直接
增加 312 条规则行为数量。它们覆盖的是规则场景会反复依赖的不可变快照迁移边界：

- `BattleParticipantHealthOperationsTests`：HP 扣减/回复夹取、可战斗判断、替身建立和替身 HP 扣减。
- `BattleParticipantSkillOperationsTests`：技能槽替换、讲究类锁定、携带道具消费、蓄力/休整/锁招计数清理。
- `BattleParticipantStatusOperationsTests`：主要异常状态计数、睡眠阻止递减、临时状态不刷新和绑定字段清理。
- `BattleParticipantStatOperationsTests`：能力阶级夹取、0 阶级不写入运行态、连续保护/剧毒计数和离场清理边界。

## 废弃口径处理

历史规划里的额外覆盖服务和场景管理表已经废弃。当前只保留测试源码账本：

- `BattleRuleCoverageLedgerTests` 固定 12 个规则族、`1..312` 连续编号和唯一 `assertNamed` 场景锚点。
- 行为测试类是事实源；规则覆盖页面若后续需要展示，也应读取测试账本产物或构建产物，而不是重新维护一套业务表。
- 新增规则时先补行为测试，再调整账本规则族数量；不新增场景表、运行记录表或旧口径适配层。

## 统一测试节奏

后续按用户要求调整为：

- 小批开发期间不每条规则都跑完整测试。
- 复杂建模时只做必要的轻量编译或局部检查，避免长时间卡在重复测试上。
- 每累计完成 20 条规则行为，对这 20 条规则相关测试做一次整体回归，确认批内规则之间没有互相破坏。
- 一个规则批次完成后统一跑：`./gradlew :battle-engine:test :battle-rules:test :migration:test`。
- 涉及应用启动、OpenAPI、真实数据库迁移或管理端页面时，再额外重启前后端并做运行态验证。

## 最终收口状态

1. 命中、保护、替身、免疫与反射分组已通过最终边界场景收口，额外覆盖不受保护影响技能和目标倒下后的行动取消。
2. 技能效果行为族已通过最终边界场景收口，额外覆盖全场能力阶级清除、0% 状态效果、复制、交换和取反边界。
3. 道具效果行为族已通过最终边界场景收口，额外覆盖蓄力跳过道具消费、免死后不继续触发同道具回复，以及非消耗治愈道具。
