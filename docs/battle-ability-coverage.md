# 战斗特性覆盖矩阵

> 本文件由 `./gradlew :battle-rules:generateBattleAbilityCoverage` 生成，请勿手工编辑。

## 汇总

- 主系列特性：310
- 已启用资料：310
- Jimmer 规则读取完整：310
- 运行时策略完整：310
- 具备行为测试证据：304
- 明确无效果契约：6
- 待补缺口：0

## 待补缺口

| 特性 | 资料 | 规则策略 | Jimmer | 运行时 | 行为测试 | 待补策略 |
| --- | --- | --- | --- | --- | --- | --- |

## 完整矩阵

| 特性 | 资料 | 规则策略 | Jimmer | 运行时 | 行为测试 | 待补策略 |
| --- | --- | --- | --- | --- | --- | --- |
| `adaptability` 适应力 | 启用 | `same-element-bonus-double` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `aerilate` 飞行皮肤 | 启用 | `normal-to-flying-damage-boost` | 完整 | 支持 | `BattleSkillElementOverrideAbilityTests` | — |
| `aftermath` 引爆 | 启用 | `contact-faint-attacker-max-hp-quarter-damage` | 完整 | 支持 | `BattleFaintRetaliationAbilityTests` | — |
| `air-lock` 气闸 | 启用 | `weather-effect-suppression` | 完整 | 支持 | `BattleDamageAbsorbingFormAbilityTests`, `BattleForecastAbilityTests`, `BattleWeatherSuppressionAbilityTests` | — |
| `analytic` 分析 | 启用 | `target-already-acted-damage-thirteen-tenths` | 完整 | 支持 | `BattleAnalyticAbilityTests` | — |
| `anger-point` 愤怒穴位 | 启用 | `critical-damage-set-attack-plus-six` | 完整 | 支持 | `BattleDamageThresholdAbilityTests` | — |
| `anger-shell` 愤怒甲壳 | 启用 | `cross-half-hp-anger-shell-stages` | 完整 | 支持 | `BattleDamageThresholdAbilityTests` | — |
| `anticipation` 危险预知 | 启用 | `switch-in-detect-dangerous-opponent-skill` | 完整 | 支持 | `BattleSwitchInDangerDetectionAbilityTests` | — |
| `arena-trap` 沙穴 | 启用 | `grounded-opponent-switch-restriction` | 完整 | 支持 | `BattleSwitchRestrictionAbilityTests` | — |
| `armor-tail` 尾甲 | 启用 | `side-priority-move-immunity` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattlePriorityAbilityTests` | — |
| `aroma-veil` 芳香幕 | 启用 | `side-volatile-status-immunity-aroma-veil` | 完整 | 支持 | `BattleAromaVeilAbilityTests` | — |
| `as-one-glastrier` 人马一体 | 启用 | `caused-faint-attack-plus-one` | 完整 | 支持 | `BattleFaintAbilityTests` | — |
| `as-one-spectrier` 人马一体 | 启用 | `caused-faint-special-attack-plus-one` | 完整 | 支持 | `BattleFaintAbilityTests` | — |
| `aura-break` 气场破坏 | 启用 | `field-damage-aura-reversal` | 完整 | 支持 | `BattleFieldDamageAuraAbilityTests` | — |
| `bad-dreams` 梦魇 | 启用 | `sleeping-opponents-end-turn-damage-eighth` | 完整 | 支持 | `BattleEndTurnAbilityCoverageTests` | — |
| `ball-fetch` 捡球 | 启用 | `single-battle-no-effect` | 完整 | 支持 | 不适用（明确无效果） | — |
| `battery` 蓄电池 | 启用 | `ally-special-damage-boost-thirteen-tenths` | 完整 | 支持 | `BattleAllyAuraAbilityTests` | — |
| `battle-armor` 战斗盔甲 | 启用 | `critical-hit-immunity` | 完整 | 支持 | `BattleCriticalHitImmunityAbilityTests` | — |
| `battle-bond` 牵绊变身 | 启用 | `caused-faint-once-attack-special-attack-speed-plus-one` | 完整 | 支持 | `BattleFaintAbilityTests` | — |
| `beads-of-ruin` 灾祸之玉 | 启用 | `opponent-special-defense-stat-three-quarters` | 完整 | 支持 | `BattleRuinFluffyAbilityTests` | — |
| `beast-boost` 异兽提升 | 启用 | `caused-faint-highest-stat-plus-one` | 完整 | 支持 | `BattleFaintAbilityTests` | — |
| `berserk` 怒火冲天 | 启用 | `cross-half-hp-special-attack-plus-one` | 完整 | 支持 | `BattleDamageThresholdAbilityTests` | — |
| `big-pecks` 健壮胸肌 | 启用 | `stat-drop-immunity-defense` | 完整 | 支持 | `BattleStatReductionImmunityAbilityTests` | — |
| `blaze` 猛火 | 启用 | `low-hp-fire-boost` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `bulletproof` 防弹 | 启用 | `projectile-skill-immunity` | 完整 | 支持 | `BattleSkillTagAbilityTests` | — |
| `cheek-pouch` 颊囊 | 启用 | `berry-consumption-heal-third` | 完整 | 支持 | `BattleBerryConsumptionAbilityTests` | — |
| `chilling-neigh` 苍白嘶鸣 | 启用 | `caused-faint-attack-plus-one` | 完整 | 支持 | `BattleFaintAbilityTests` | — |
| `chlorophyll` 叶绿素 | 启用 | `weather-speed-sun` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleWeatherEffectTests`, `BattleWeatherSuppressionAbilityTests` | — |
| `clear-body` 恒净之躯 | 启用 | `stat-drop-immunity-all` | 完整 | 支持 | `BattleStatReductionImmunityAbilityTests` | — |
| `cloud-nine` 无关天气 | 启用 | `weather-effect-suppression` | 完整 | 支持 | `BattleDamageAbsorbingFormAbilityTests`, `BattleForecastAbilityTests`, `BattleWeatherSuppressionAbilityTests` | — |
| `color-change` 变色 | 启用 | `received-damage-element-change` | 完整 | 支持 | `BattleColorChangeAbilityTests` | — |
| `comatose` 绝对睡眠 | 启用 | `always-treated-asleep-major-status-immunity` | 完整 | 支持 | `BattleAlwaysTreatedAsleepAbilityTests`, `BattlePassiveSuppressionAbilityTests` | — |
| `commander` 发号施令 | 启用 | `single-battle-no-effect` | 完整 | 支持 | 不适用（明确无效果） | — |
| `competitive` 好胜 | 启用 | `opponent-stat-drop-special-attack-plus-two` | 完整 | 支持 | `BattleStatTransformAbilityTests` | — |
| `compound-eyes` 复眼 | 启用 | `accuracy-multiplier-thirteen-tenths` | 完整 | 支持 | `BattleAccuracyAbilityTests` | — |
| `contrary` 唱反调 | 启用 | `stat-stage-delta-reverse` | 完整 | 支持 | `BattleStatTransformAbilityTests` | — |
| `corrosion` 腐蚀 | 启用 | `poison-element-status-bypass` | 完整 | 支持 | `BattleStatusImmunityAndGroundingTests` | — |
| `costar` 同台共演 | 启用 | `switch-in-ally-stat-stage-copy` | 完整 | 支持 | `BattleSwitchInAllyFieldAbilityTests` | — |
| `cotton-down` 棉絮 | 启用 | `received-damage-all-other-speed-minus-one` | 完整 | 支持 | `BattleDisableCottonSteadfastAbilityTests` | — |
| `cud-chew` 反刍 | 启用 | `end-turn-next-turn-consumed-berry-replay` | 完整 | 支持 | `BattleConsumedBerryReplayAbilityTests` | — |
| `curious-medicine` 怪药 | 启用 | `switch-in-ally-stat-stage-reset` | 完整 | 支持 | `BattleSwitchInAllyFieldAbilityTests` | — |
| `cursed-body` 诅咒之躯 | 启用 | `received-damage-disable-attacker-skill-thirty-percent` | 完整 | 支持 | `BattleDisableCottonSteadfastAbilityTests` | — |
| `cute-charm` 迷人之躯 | 启用 | `contact-opposite-gender-infatuation-thirty-percent` | 完整 | 支持 | `BattleCuteCharmAbilityTests` | — |
| `damp` 湿气 | 启用 | `explosion-effect-suppression` | 完整 | 支持 | `BattleFaintRetaliationAbilityTests` | — |
| `dancer` 舞者 | 启用 | `dance-move-copy` | 完整 | 支持 | `BattleDancerAbilityTests` | — |
| `dark-aura` 暗黑气场 | 启用 | `element-dark-damage-boost-four-thirds` | 完整 | 支持 | `BattleFieldDamageAuraAbilityTests` | — |
| `dauntless-shield` 不屈之盾 | 启用 | `switch-in-self-defense-plus-one` | 完整 | 支持 | `BattleGuardDogAbilityTests`, `BattleReactiveStatCopyItemTests`, `BattleStatReductionImmunityAbilityTests`, `BattleStatStageReflectionAbilityTests`, `BattleSwitchInAbilityTests` | — |
| `dazzling` 鲜艳之躯 | 启用 | `side-priority-move-immunity` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattlePriorityAbilityTests` | — |
| `defeatist` 软弱 | 启用 | `half-hp-attack-stat-half`, `half-hp-special-attack-stat-half` | 完整 | 支持 | `BattleConditionalStatAbilityTests`, `BattleDamageCalculatorTests` | — |
| `defiant` 不服输 | 启用 | `opponent-stat-drop-attack-plus-two` | 完整 | 支持 | `BattleStatTransformAbilityTests` | — |
| `delta-stream` 德尔塔气流 | 启用 | `switch-in-strong-weather-strong-winds` | 完整 | 支持 | `BattleStrongWeatherAbilityTests` | — |
| `desolate-land` 终结之地 | 启用 | `switch-in-strong-weather-harsh-sunlight` | 完整 | 支持 | `BattleStrongWeatherAbilityTests` | — |
| `disguise` 画皮 | 启用 | `damage-absorbing-form-change-mimikyu` | 完整 | 支持 | `BattleDamageAbsorbingFormAbilityTests` | — |
| `download` 下载 | 启用 | `switch-in-opponent-defense-comparison-attack-plus-one` | 完整 | 支持 | `BattleDownloadAbilityTests` | — |
| `dragons-maw` 龙颚 | 启用 | `element-dragon-damage-boost` | 完整 | 支持 | `BattleDamageCalculatorTests`, `BattleElementDamageAbilityTests`, `BattlePassiveSuppressionAbilityTests` | — |
| `drizzle` 降雨 | 启用 | `switch-in-weather-rain` | 完整 | 支持 | `BattleSwitchInAbilityTests` | — |
| `drought` 日照 | 启用 | `switch-in-weather-sun` | 完整 | 支持 | `BattleSwitchInAbilityTests` | — |
| `dry-skin` 干燥皮肤 | 启用 | `sun-end-turn-damage-eighth`, `weather-heal-rain-eighth` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleEndTurnAbilityCoverageTests`, `BattleEndTurnPipelineTests`, `BattleEnvironmentFieldBoundaryPublicReferenceTests`, `BattleWeatherEffectTests` | — |
| `early-bird` 早起 | 启用 | `sleep-duration-half` | 完整 | 支持 | `BattlePpSleepSideAbilityTests` | — |
| `earth-eater` 食土 | 启用 | `element-ground-absorb-heal` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleElementAbsorbAbilityTests`, `BattleTargetAbilityIgnoreTests`, `BattleWeatherElementOverrideTests` | — |
| `effect-spore` 孢子 | 启用 | `contact-random-poison-paralysis-sleep` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests` | — |
| `electric-surge` 电气制造者 | 启用 | `switch-in-terrain-electric` | 完整 | 支持 | `BattleSwitchInAbilityTests` | — |
| `electromorphosis` 电力转换 | 启用 | `received-damage-next-electric-damage-double` | 完整 | 支持 | `BattleReceivedDamageChargeAbilityTests`, `BattleWindPowerAbilityTests` | — |
| `embody-aspect-cornerstone` 面影辉映（础石面具） | 启用 | `terastallization-stat-defense-plus-one` | 完整 | 支持 | `BattleTerastallizationTests` | — |
| `embody-aspect-hearthflame` 面影辉映（火灶面具） | 启用 | `terastallization-stat-attack-plus-one` | 完整 | 支持 | `BattleTerastallizationTests` | — |
| `embody-aspect-teal` 面影辉映（碧草面具） | 启用 | `terastallization-stat-speed-plus-one` | 完整 | 支持 | `BattleTerastallizationTests` | — |
| `embody-aspect-wellspring` 面影辉映（水井面具） | 启用 | `terastallization-stat-special-defense-plus-one` | 完整 | 支持 | `BattleTerastallizationTests` | — |
| `emergency-exit` 危险回避 | 启用 | `cross-half-hp-force-self-switch` | 完整 | 支持 | `BattleThresholdSwitchAbilityTests` | — |
| `fairy-aura` 妖精气场 | 启用 | `element-fairy-damage-boost-four-thirds` | 完整 | 支持 | `BattleFieldDamageAuraAbilityTests` | — |
| `filter` 过滤 | 启用 | `super-effective-damage-reduction` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `flame-body` 火焰之躯 | 启用 | `contact-burn` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests`, `BattleContactProtectionBypassAbilityTests`, `BattleEngineSingleTurnTests`, `BattleImmunityTests`, `BattleStatusCureItemTests` | — |
| `flare-boost` 受热激升 | 启用 | `burn-special-attack-stat-one-and-half` | 完整 | 支持 | `BattleConditionalStatAbilityTests`, `BattleDamageCalculatorTests` | — |
| `flash-fire` 引火 | 启用 | `element-fire-absorb-damage-boost-one-and-half` | 完整 | 支持 | `BattleFlashFireAbilityTests` | — |
| `flower-gift` 花之礼 | 启用 | `sun-attack-stat-four-thirds` | 完整 | 支持 | `BattleConditionalStatAbilityTests`, `BattleDamageCalculatorTests` | — |
| `flower-veil` 花幕 | 启用 | `side-grass-major-status-immunity`, `side-grass-stat-drop-immunity` | 完整 | 支持 | `BattleFlowerVeilAbilityTests` | — |
| `fluffy` 毛茸茸 | 启用 | `received-contact-damage-half`, `received-fire-damage-double` | 完整 | 支持 | `BattleElementDamageAbilityTests`, `BattleRuinFluffyAbilityTests` | — |
| `forecast` 阴晴不定 | 启用 | `weather-form-change-castform` | 完整 | 支持 | `BattleForecastAbilityTests` | — |
| `forewarn` 预知梦 | 启用 | `switch-in-reveal-opponent-highest-power-skill` | 完整 | 支持 | `BattleSwitchInRevealAbilityTests` | — |
| `friend-guard` 友情防守 | 启用 | `ally-received-damage-three-quarters` | 完整 | 支持 | `BattleAllyAuraAbilityTests` | — |
| `frisk` 察觉 | 启用 | `switch-in-reveal-opponent-held-items` | 完整 | 支持 | `BattleSwitchInRevealAbilityTests` | — |
| `full-metal-body` 金属防护 | 启用 | `stat-drop-immunity-all` | 完整 | 支持 | `BattleStatReductionImmunityAbilityTests` | — |
| `fur-coat` 毛皮大衣 | 启用 | `defense-stat-double` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `gale-wings` 疾风之翼 | 启用 | `full-hp-flying-skill-priority-plus-one` | 完整 | 支持 | `BattleSpecializedPriorityAbilityTests` | — |
| `galvanize` 电气皮肤 | 启用 | `normal-to-electric-damage-boost` | 完整 | 支持 | `BattleSkillElementOverrideAbilityTests` | — |
| `gluttony` 贪吃鬼 | 启用 | `low-hp-item-trigger-threshold-half` | 完整 | 支持 | `BattleLowHpItemThresholdAbilityTests` | — |
| `good-as-gold` 黄金之躯 | 启用 | `opponent-status-skill-immunity` | 完整 | 支持 | `BattleMyceliumMightAbilityTests`, `BattleStatusAbilityRulesTests` | — |
| `gooey` 黏滑 | 启用 | `contact-attacker-speed-minus-one` | 完整 | 支持 | `BattleReceivedDamageStatAbilityTests` | — |
| `gorilla-tactics` 一猩一意 | 启用 | `attack-stat-one-and-half` | 完整 | 支持 | `BattleConditionalStatAbilityTests`, `BattleDamageCalculatorTests` | — |
| `grass-pelt` 草之毛皮 | 启用 | `grassy-terrain-defense-stat-boost` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `grassy-surge` 青草制造者 | 启用 | `switch-in-terrain-grassy` | 完整 | 支持 | `BattleSwitchInAbilityTests` | — |
| `grim-neigh` 漆黑嘶鸣 | 启用 | `caused-faint-special-attack-plus-one` | 完整 | 支持 | `BattleFaintAbilityTests` | — |
| `guard-dog` 看门犬 | 启用 | `switch-in-attack-drop-react-attack-plus-one`, `forced-switch-immunity` | 完整 | 支持 | `BattleForcedSwitchSkillTests`, `BattleGuardDogAbilityTests` | — |
| `gulp-missile` 一口导弹 | 启用 | `post-skill-hp-form-change-cramorant`, `received-damage-form-retaliation-cramorant-gulping`, `received-damage-form-retaliation-cramorant-gorging` | 完整 | 支持 | `BattleGulpMissileAbilityTests` | — |
| `guts` 毅力 | 启用 | `major-status-attack-stat-boost-ignore-burn-drop` | 完整 | 支持 | `BattleConditionalStatAbilityTests`, `BattleDamageCalculatorTests` | — |
| `hadron-engine` 强子引擎 | 启用 | `switch-in-terrain-electric`, `electric-terrain-special-attack-stat-four-thirds` | 完整 | 支持 | `BattleConditionalStatAbilityTests`, `BattleDamageCalculatorTests`, `BattleSwitchInAbilityTests` | — |
| `harvest` 收获 | 启用 | `end-turn-consumed-berry-restore-half-sun-guaranteed` | 完整 | 支持 | `BattleConsumedBerryRestoreAbilityTests` | — |
| `healer` 治愈之心 | 启用 | `end-turn-ally-major-status-cure-thirty-percent` | 完整 | 支持 | `BattleEndTurnAbilityCoverageTests` | — |
| `heatproof` 耐热 | 启用 | `received-fire-damage-half` | 完整 | 支持 | `BattleElementDamageAbilityTests`, `BattleRuinFluffyAbilityTests` | — |
| `heavy-metal` 重金属 | 启用 | `weight-double` | 完整 | 支持 | `BattleDamageCalculatorTests`, `BattleSkillWeightEffectTests` | — |
| `honey-gather` 采蜜 | 启用 | `single-battle-no-effect` | 完整 | 支持 | 不适用（明确无效果） | — |
| `hospitality` 款待 | 启用 | `switch-in-ally-heal-quarter` | 完整 | 支持 | `BattleSwitchInAllyFieldAbilityTests` | — |
| `huge-power` 大力士 | 启用 | `attack-stat-double` | 完整 | 支持 | `BattleConditionalStatAbilityTests`, `BattleDamageCalculatorTests` | — |
| `hunger-switch` 饱了又饿 | 启用 | `end-turn-form-toggle-morpeko` | 完整 | 支持 | `BattleHungerSwitchAbilityTests` | — |
| `hustle` 活力 | 启用 | `attack-stat-one-and-half`, `physical-accuracy-multiplier-four-fifths` | 完整 | 支持 | `BattleAccuracyAbilityTests`, `BattleConditionalStatAbilityTests`, `BattleDamageCalculatorTests` | — |
| `hydration` 湿润之躯 | 启用 | `end-turn-major-status-cure-rain` | 完整 | 支持 | `BattleEndTurnAbilityCoverageTests` | — |
| `hyper-cutter` 怪力钳 | 启用 | `stat-drop-immunity-attack` | 完整 | 支持 | `BattleStatReductionImmunityAbilityTests` | — |
| `ice-body` 冰冻之躯 | 启用 | `weather-heal-snow` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleEndTurnPipelineTests`, `BattleEnvironmentFieldBoundaryPublicReferenceTests`, `BattleWeatherEffectTests` | — |
| `ice-face` 结冻头 | 启用 | `damage-absorbing-form-change-eiscue`, `snow-form-restore-eiscue` | 完整 | 支持 | `BattleDamageAbsorbingFormAbilityTests` | — |
| `ice-scales` 冰鳞粉 | 启用 | `special-damage-reduction` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `illuminate` 发光 | 启用 | `accuracy-multiplier-eleven-tenths` | 完整 | 支持 | `BattleAccuracyAbilityTests` | — |
| `illusion` 幻觉 | 启用 | `switch-in-disguise-as-last-healthy-ally` | 完整 | 支持 | `BattleIllusionAbilityTests` | — |
| `immunity` 免疫 | 启用 | `major-status-immunity-poison` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests`, `BattleStatusAbilityRulesTests`, `BattleStatusImmunityAndGroundingTests`, `BattleTargetAbilityIgnoreTests` | — |
| `imposter` 变身者 | 启用 | `switch-in-transform-into-opponent` | 完整 | 支持 | `BattleSwitchInTransformAbilityTests` | — |
| `infiltrator` 穿透 | 启用 | `opponent-barrier-bypass` | 完整 | 支持 | `BattleInfiltratorAbilityTests` | — |
| `innards-out` 飞出的内在物 | 启用 | `faint-attacker-damage-taken` | 完整 | 支持 | `BattleFaintRetaliationAbilityTests` | — |
| `inner-focus` 精神力 | 启用 | `volatile-status-immunity-flinch` | 完整 | 支持 | `BattleStatusImmunityAndGroundingTests` | — |
| `insomnia` 不眠 | 启用 | `major-status-immunity-sleep` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests`, `BattleStatusAbilityRulesTests`, `BattleStatusImmunityAndGroundingTests`, `BattleTargetAbilityIgnoreTests` | — |
| `intimidate` 威吓 | 启用 | `switch-in-opponents-attack-down` | 完整 | 支持 | `BattleGuardDogAbilityTests`, `BattleReactiveStatCopyItemTests`, `BattleStatReductionImmunityAbilityTests`, `BattleStatStageReflectionAbilityTests`, `BattleSwitchInAbilityTests` | — |
| `intrepid-sword` 不挠之剑 | 启用 | `switch-in-self-attack-plus-one` | 完整 | 支持 | `BattleGuardDogAbilityTests`, `BattleReactiveStatCopyItemTests`, `BattleStatReductionImmunityAbilityTests`, `BattleStatStageReflectionAbilityTests`, `BattleSwitchInAbilityTests` | — |
| `iron-barbs` 铁刺 | 启用 | `contact-damage-to-attacker-eighth` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests`, `BattleContactSuppressionAbilityTests` | — |
| `iron-fist` 铁拳 | 启用 | `punch-based-skill-damage-boost` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `justified` 正义之心 | 启用 | `received-dark-attack-plus-one` | 完整 | 支持 | `BattleReceivedDamageStatAbilityTests` | — |
| `keen-eye` 锐利目光 | 启用 | `stat-drop-immunity-accuracy`, `ignore-opponent-accuracy-stat-stages` | 完整 | 支持 | `BattleAccuracyStatStageIgnoreAbilityTests`, `BattleStatReductionImmunityAbilityTests` | — |
| `klutz` 笨拙 | 启用 | `held-item-effect-suppression` | 完整 | 支持 | `BattlePassiveSuppressionAbilityTests` | — |
| `leaf-guard` 叶子防守 | 启用 | `sun-major-status-immunity-all` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests`, `BattleStatusAbilityRulesTests`, `BattleStatusImmunityAndGroundingTests`, `BattleTargetAbilityIgnoreTests` | — |
| `levitate` 飘浮 | 启用 | `ground-immunity` | 完整 | 支持 | `BattleActionOrderingPublicReferenceTests`, `BattleDamageCalculatorTests`, `BattleGroundingItemTests`, `BattlePsychicTerrainTests`, `BattleStatusImmunityAndGroundingTests` | — |
| `libero` 自由者 | 启用 | `first-skill-element-change-since-switch-in` | 完整 | 支持 | `BattleFirstSkillElementChangeAbilityTests` | — |
| `light-metal` 轻金属 | 启用 | `weight-half` | 完整 | 支持 | `BattleDamageCalculatorTests`, `BattleSkillWeightEffectTests` | — |
| `lightning-rod` 避雷针 | 启用 | `element-electric-absorb-special-attack-up` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleElementAbsorbStatAbilityTests` | — |
| `limber` 柔软 | 启用 | `major-status-immunity-paralysis` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests`, `BattleStatusAbilityRulesTests`, `BattleStatusImmunityAndGroundingTests`, `BattleTargetAbilityIgnoreTests` | — |
| `lingering-aroma` 甩不掉的气味 | 启用 | `contact-replace-attacker-ability-with-holder` | 完整 | 支持 | `BattleAbilityReplacementTests` | — |
| `liquid-ooze` 污泥浆 | 启用 | `drain-healing-reversal` | 完整 | 支持 | `BattleLiquidOozeAbilityTests` | — |
| `liquid-voice` 湿润之声 | 启用 | `sound-to-water` | 完整 | 支持 | `BattleSkillElementOverrideAbilityTests` | — |
| `long-reach` 远隔 | 启用 | `contact-suppression` | 完整 | 支持 | `BattleContactSuppressionAbilityTests` | — |
| `magic-bounce` 魔法镜 | 启用 | `opponent-targeted-status-skill-reflection` | 完整 | 支持 | `BattleMagicBounceAbilityTests` | — |
| `magic-guard` 魔法防守 | 启用 | `indirect-damage-immunity` | 完整 | 支持 | `BattleHeldItemPublicReferenceTests`, `BattleIndirectDamageImmunityTests`, `BattleStruggleTests` | — |
| `magician` 魔术师 | 启用 | `damaging-skill-steal-target-held-item` | 完整 | 支持 | `BattleAbilityItemStealTests` | — |
| `magma-armor` 熔岩铠甲 | 启用 | `major-status-immunity-freeze` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests`, `BattleStatusAbilityRulesTests`, `BattleStatusImmunityAndGroundingTests`, `BattleTargetAbilityIgnoreTests` | — |
| `magnet-pull` 磁力 | 启用 | `steel-opponent-switch-restriction` | 完整 | 支持 | `BattleSwitchRestrictionAbilityTests` | — |
| `marvel-scale` 神奇鳞片 | 启用 | `major-status-defense-stat-one-and-half` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `mega-launcher` 超级发射器 | 启用 | `pulse-based-skill-damage-boost-one-and-half` | 完整 | 支持 | `BattleSkillTagAbilityTests` | — |
| `merciless` 不仁不义 | 启用 | `poisoned-target-guaranteed-critical-hit` | 完整 | 支持 | `BattleStatusAbilityRulesTests` | — |
| `mimicry` 拟态 | 启用 | `terrain-element-identity` | 完整 | 支持 | `BattleMimicryAbilityTests` | — |
| `minds-eye` 心眼 | 启用 | `normal-fighting-type-immunity-bypass`, `ignore-opponent-accuracy-stat-stages`, `switch-in-attack-drop-immunity` | 完整 | 支持 | `BattleAccuracyStatStageIgnoreAbilityTests`, `BattleStatReductionImmunityAbilityTests`, `BattleTypeImmunityAbilityTests` | — |
| `minus` 负电 | 启用 | `ally-group-plus-minus-membership`, `ally-group-plus-minus-special-attack-one-and-half` | 完整 | 支持 | `BattleAllyAuraAbilityTests` | — |
| `mirror-armor` 镜甲 | 启用 | `opponent-stat-stage-reduction-reflection` | 完整 | 支持 | `BattleStatStageReflectionAbilityTests` | — |
| `misty-surge` 薄雾制造者 | 启用 | `switch-in-terrain-misty` | 完整 | 支持 | `BattleSwitchInAbilityTests` | — |
| `mold-breaker` 破格 | 启用 | `ignore-target-ability-effects` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleAbilityShieldItemTests`, `BattleContactAbilityPublicReferenceTests`, `BattleSoundAbilityTests`, `BattleStatusImmunityAndGroundingTests`, `BattleTargetAbilityIgnoreTests` | — |
| `moody` 心情不定 | 启用 | `end-turn-random-stat-plus-two-minus-one` | 完整 | 支持 | `BattleEndTurnAbilityCoverageTests` | — |
| `motor-drive` 电气引擎 | 启用 | `element-electric-absorb-speed-up` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleElementAbsorbStatAbilityTests` | — |
| `moxie` 自信过度 | 启用 | `caused-faint-attack-plus-one` | 完整 | 支持 | `BattleFaintAbilityTests` | — |
| `multiscale` 多重鳞片 | 启用 | `full-hp-damage-reduction` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `multitype` 多属性 | 启用 | `held-item-element-identity`, `held-item-removal-immunity` | 完整 | 支持 | `BattleHeldItemElementIdentityAbilityTests`, `BattlePassiveSuppressionAbilityTests` | — |
| `mummy` 木乃伊 | 启用 | `contact-replace-attacker-ability-with-holder` | 完整 | 支持 | `BattleAbilityReplacementTests` | — |
| `mycelium-might` 菌丝之力 | 启用 | `status-skill-moves-last-ignore-target-ability` | 完整 | 支持 | `BattleMyceliumMightAbilityTests` | — |
| `natural-cure` 自然回复 | 启用 | `switch-out-major-status-cure` | 完整 | 支持 | `BattleSwitchOutAbilityTests` | — |
| `neuroforce` 脑核之力 | 启用 | `super-effective-damage-boost-quarter` | 完整 | 支持 | `BattleElementDamageAbilityTests` | — |
| `neutralizing-gas` 化学变化气体 | 启用 | `field-ability-suppression` | 完整 | 支持 | `BattleForecastAbilityTests`, `BattleMimicryAbilityTests`, `BattlePassiveSuppressionAbilityTests` | — |
| `no-guard` 无防守 | 启用 | `accuracy-always-hit` | 完整 | 支持 | `BattleAccuracyAbilityTests`, `BattlePassiveSuppressionAbilityTests` | — |
| `normalize` 一般皮肤 | 启用 | `all-to-normal-damage-boost` | 完整 | 支持 | `BattleSkillElementOverrideAbilityTests` | — |
| `oblivious` 迟钝 | 启用 | `volatile-status-immunity-infatuation-taunt` | 完整 | 支持 | `BattleStatusImmunityAndGroundingTests` | — |
| `opportunist` 跟风 | 启用 | `opponent-stat-stage-increase-copy` | 完整 | 支持 | `BattleOpponentStatStageIncreaseCopyAbilityTests` | — |
| `orichalcum-pulse` 绯红脉动 | 启用 | `switch-in-weather-sun`, `sun-attack-stat-four-thirds` | 完整 | 支持 | `BattleConditionalStatAbilityTests`, `BattleDamageCalculatorTests`, `BattleSwitchInAbilityTests` | — |
| `overcoat` 防尘 | 启用 | `powder-skill-immunity`, `weather-damage-immunity-sandstorm` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleStatusImmunityAndGroundingTests`, `BattleWeatherEffectTests` | — |
| `overgrow` 茂盛 | 启用 | `low-hp-grass-boost` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `own-tempo` 我行我素 | 启用 | `volatile-status-immunity-confusion` | 完整 | 支持 | `BattleStatusImmunityAndGroundingTests` | — |
| `parental-bond` 亲子爱 | 启用 | `single-target-second-hit-quarter-damage` | 完整 | 支持 | `BattleSingleTargetSecondHitAbilityTests` | — |
| `pastel-veil` 粉彩护幕 | 启用 | `side-major-status-immunity-poison` | 完整 | 支持 | `BattleStatusImmunityAndGroundingTests` | — |
| `perish-body` 灭亡之躯 | 启用 | `received-contact-shared-perish-countdown-three` | 完整 | 支持 | `BattlePerishBodyAbilityTests` | — |
| `pickpocket` 顺手牵羊 | 启用 | `contact-steal-attacker-held-item` | 完整 | 支持 | `BattleAbilityItemStealTests` | — |
| `pickup` 捡拾 | 启用 | `end-turn-pickup-last-consumed-item` | 完整 | 支持 | `BattlePickupAbilityTests` | — |
| `pixilate` 妖精皮肤 | 启用 | `normal-to-fairy-damage-boost` | 完整 | 支持 | `BattleSkillElementOverrideAbilityTests` | — |
| `plus` 正电 | 启用 | `ally-group-plus-minus-membership`, `ally-group-plus-minus-special-attack-one-and-half` | 完整 | 支持 | `BattleAllyAuraAbilityTests` | — |
| `poison-heal` 毒疗 | 启用 | `poison-status-end-turn-heal-eighth` | 完整 | 支持 | `BattleEndTurnAbilityTests` | — |
| `poison-point` 毒刺 | 启用 | `contact-poison` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests`, `BattleContactProtectionBypassAbilityTests`, `BattleEngineSingleTurnTests`, `BattleImmunityTests`, `BattleStatusCureItemTests` | — |
| `poison-puppeteer` 毒傀儡 | 启用 | `poison-application-confusion` | 完整 | 支持 | `BattlePoisonApplicationConfusionAbilityTests` | — |
| `poison-touch` 毒手 | 启用 | `dealt-contact-damage-poison-thirty-percent` | 完整 | 支持 | `BattleStatusAbilityRulesTests` | — |
| `power-construct` 群聚变形 | 启用 | `end-turn-hp-form-change-zygarde-complete` | 完整 | 支持 | `BattleHpFormAbilitiesTests` | — |
| `power-of-alchemy` 化学之力 | 启用 | `fainted-ally-ability-copy` | 完整 | 支持 | `BattleAbilityReplacementTests` | — |
| `power-spot` 能量点 | 启用 | `ally-damage-boost-thirteen-tenths` | 完整 | 支持 | `BattleAllyAuraAbilityTests` | — |
| `prankster` 恶作剧之心 | 启用 | `status-skill-priority-boost` | 完整 | 支持 | `BattleActionOrderingPublicReferenceTests`, `BattleStatusPriorityAbilityTests` | — |
| `pressure` 压迫感 | 启用 | `opponent-skill-pp-cost-plus-one` | 完整 | 支持 | `BattlePpSleepSideAbilityTests` | — |
| `primordial-sea` 始源之海 | 启用 | `switch-in-strong-weather-heavy-rain` | 完整 | 支持 | `BattleStrongWeatherAbilityTests` | — |
| `prism-armor` 棱镜装甲 | 启用 | `super-effective-damage-reduction` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `propeller-tail` 螺旋尾鳍 | 启用 | `single-battle-no-effect` | 完整 | 支持 | 不适用（明确无效果） | — |
| `protean` 变幻自如 | 启用 | `first-skill-element-change-since-switch-in` | 完整 | 支持 | `BattleFirstSkillElementChangeAbilityTests` | — |
| `protosynthesis` 古代活性 | 启用 | `sun-highest-stat-boost` | 完整 | 支持 | `BattleEnvironmentHighestStatAbilityTests` | — |
| `psychic-surge` 精神制造者 | 启用 | `switch-in-terrain-psychic` | 完整 | 支持 | `BattleSwitchInAbilityTests` | — |
| `punk-rock` 庞克摇滚 | 启用 | `sound-based-skill-damage-boost`, `sound-based-skill-damage-reduction` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `pure-power` 瑜伽之力 | 启用 | `attack-stat-double` | 完整 | 支持 | `BattleConditionalStatAbilityTests`, `BattleDamageCalculatorTests` | — |
| `purifying-salt` 洁净之盐 | 启用 | `major-status-immunity-all`, `received-ghost-damage-half` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests`, `BattleElementDamageAbilityTests`, `BattleRuinFluffyAbilityTests`, `BattleStatusAbilityRulesTests`, `BattleStatusImmunityAndGroundingTests`, `BattleTargetAbilityIgnoreTests` | — |
| `quark-drive` 夸克充能 | 启用 | `electric-terrain-highest-stat-boost` | 完整 | 支持 | `BattleEnvironmentHighestStatAbilityTests` | — |
| `queenly-majesty` 女王的威严 | 启用 | `side-priority-move-immunity` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattlePriorityAbilityTests` | — |
| `quick-draw` 速击 | 启用 | `random-action-order-boost-thirty-percent` | 完整 | 支持 | `BattleAbilityReplacementTests`, `BattleQuickDrawAbilityTests` | — |
| `quick-feet` 飞毛腿 | 启用 | `major-status-speed-one-and-half-ignore-paralysis` | 完整 | 支持 | `BattleConditionalStatAbilityTests` | — |
| `rain-dish` 雨盘 | 启用 | `weather-heal-rain` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleEndTurnPipelineTests`, `BattleEnvironmentFieldBoundaryPublicReferenceTests`, `BattleWeatherEffectTests` | — |
| `rattled` 胆怯 | 启用 | `received-bug-dark-ghost-speed-plus-one` | 完整 | 支持 | `BattleReceivedDamageStatAbilityTests` | — |
| `receiver` 接球手 | 启用 | `fainted-ally-ability-copy` | 完整 | 支持 | `BattleAbilityReplacementTests` | — |
| `reckless` 舍身 | 启用 | `recoil-skill-damage-six-fifths` | 完整 | 支持 | `BattleElementDamageAbilityTests` | — |
| `refrigerate` 冰冻皮肤 | 启用 | `normal-to-ice-damage-boost` | 完整 | 支持 | `BattleSkillElementOverrideAbilityTests` | — |
| `regenerator` 再生力 | 启用 | `switch-out-heal-third` | 完整 | 支持 | `BattleSwitchOutAbilityTests` | — |
| `ripen` 熟成 | 启用 | `berry-effect-double` | 完整 | 支持 | `BattleBerryEffectMultiplierAbilityTests` | — |
| `rivalry` 斗争心 | 启用 | `target-gender-damage-five-quarters-three-quarters` | 完整 | 支持 | `BattleRivalryAbilityTests` | — |
| `rks-system` ＡＲ系统 | 启用 | `held-item-element-identity`, `held-item-removal-immunity` | 完整 | 支持 | `BattleHeldItemElementIdentityAbilityTests`, `BattlePassiveSuppressionAbilityTests` | — |
| `rock-head` 坚硬脑袋 | 启用 | `skill-recoil-damage-immunity` | 完整 | 支持 | `BattleSkillEffectBoundaryPublicReferenceTests`, `BattleSkillRecoilImmunityAbilityTests`, `BattleStruggleTests` | — |
| `rocky-payload` 搬岩 | 启用 | `element-rock-damage-boost` | 完整 | 支持 | `BattleDamageCalculatorTests`, `BattleElementDamageAbilityTests`, `BattlePassiveSuppressionAbilityTests` | — |
| `rough-skin` 粗糙皮肤 | 启用 | `contact-damage-to-attacker-eighth` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests`, `BattleContactSuppressionAbilityTests` | — |
| `run-away` 逃跑 | 启用 | `single-battle-no-effect` | 完整 | 支持 | 不适用（明确无效果） | — |
| `sand-force` 沙之力 | 启用 | `weather-sandstorm-rock-ground-steel-damage-boost`, `weather-damage-immunity-sandstorm` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleDamageCalculatorTests`, `BattleWeatherEffectTests` | — |
| `sand-rush` 拨沙 | 启用 | `weather-speed-sandstorm` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleWeatherEffectTests`, `BattleWeatherSuppressionAbilityTests` | — |
| `sand-spit` 吐沙 | 启用 | `received-damage-weather-sandstorm` | 完整 | 支持 | `BattleDamageEnvironmentAbilityTests` | — |
| `sand-stream` 扬沙 | 启用 | `switch-in-weather-sandstorm` | 完整 | 支持 | `BattleSwitchInAbilityTests` | — |
| `sand-veil` 沙隐 | 启用 | `opponent-accuracy-sandstorm-four-fifths` | 完整 | 支持 | `BattleAccuracyAbilityTests` | — |
| `sap-sipper` 食草 | 启用 | `element-grass-absorb-attack-up` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleElementAbsorbStatAbilityTests` | — |
| `schooling` 鱼群 | 启用 | `end-turn-hp-form-change-wishiwashi` | 完整 | 支持 | `BattleHpFormAbilitiesTests` | — |
| `scrappy` 胆量 | 启用 | `normal-fighting-type-immunity-bypass`, `ignore-opponent-accuracy-stat-stages`, `switch-in-attack-drop-immunity` | 完整 | 支持 | `BattleAccuracyStatStageIgnoreAbilityTests`, `BattleStatReductionImmunityAbilityTests`, `BattleTypeImmunityAbilityTests` | — |
| `screen-cleaner` 除障 | 启用 | `switch-in-clear-all-side-damage-reductions` | 完整 | 支持 | `BattleSwitchInAllyFieldAbilityTests` | — |
| `seed-sower` 掉出种子 | 启用 | `received-damage-terrain-grassy` | 完整 | 支持 | `BattleDamageEnvironmentAbilityTests` | — |
| `serene-grace` 天恩 | 启用 | `secondary-effect-chance-double` | 完整 | 支持 | `BattleSereneGraceAbilityTests` | — |
| `shadow-shield` 幻影防守 | 启用 | `full-hp-damage-reduction` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `shadow-tag` 踩影 | 启用 | `opponent-switch-restriction-same-effect-immunity` | 完整 | 支持 | `BattleSwitchRestrictionAbilityTests` | — |
| `sharpness` 锋锐 | 启用 | `slicing-based-skill-damage-boost` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `shed-skin` 蜕皮 | 启用 | `end-turn-major-status-cure-third` | 完整 | 支持 | `BattleEndTurnAbilityCoverageTests` | — |
| `sheer-force` 强行 | 启用 | `secondary-effects-suppressed-damage-thirteen-tenths` | 完整 | 支持 | `BattleSheerForceAbilityTests` | — |
| `shell-armor` 硬壳盔甲 | 启用 | `critical-hit-immunity` | 完整 | 支持 | `BattleCriticalHitImmunityAbilityTests` | — |
| `shield-dust` 鳞粉 | 启用 | `damaging-skill-secondary-effect-immunity` | 完整 | 支持 | `BattleSecondaryEffectImmunityAbilityTests` | — |
| `shields-down` 界限盾壳 | 启用 | `end-turn-hp-form-change-minior` | 完整 | 支持 | `BattleHpFormAbilitiesTests` | — |
| `simple` 单纯 | 启用 | `stat-stage-delta-double` | 完整 | 支持 | `BattleStatTransformAbilityTests` | — |
| `skill-link` 连续攻击 | 启用 | `multi-hit-maximum` | 完整 | 支持 | `BattleSkillShapeAbilityTests` | — |
| `slow-start` 慢启动 | 启用 | `first-five-turns-attack-speed-half` | 完整 | 支持 | `BattleSlowStartAbilityTests` | — |
| `slush-rush` 拨雪 | 启用 | `weather-speed-snow` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleWeatherEffectTests`, `BattleWeatherSuppressionAbilityTests` | — |
| `sniper` 狙击手 | 启用 | `critical-hit-damage-boost-one-and-half` | 完整 | 支持 | `BattleElementDamageAbilityTests` | — |
| `snow-cloak` 雪隐 | 启用 | `opponent-accuracy-snow-four-fifths` | 完整 | 支持 | `BattleAccuracyAbilityTests` | — |
| `snow-warning` 降雪 | 启用 | `switch-in-weather-snow` | 完整 | 支持 | `BattleSwitchInAbilityTests` | — |
| `solar-power` 太阳之力 | 启用 | `sun-special-attack-stat-one-and-half`, `sun-end-turn-damage-eighth` | 完整 | 支持 | `BattleConditionalStatAbilityTests`, `BattleDamageCalculatorTests`, `BattleEndTurnAbilityCoverageTests` | — |
| `solid-rock` 坚硬岩石 | 启用 | `super-effective-damage-reduction` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `soul-heart` 魂心 | 启用 | `any-faint-special-attack-plus-one` | 完整 | 支持 | `BattleFaintAbilityTests` | — |
| `soundproof` 隔音 | 启用 | `sound-based-skill-immunity` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleHitDefenseBoundaryPublicReferenceTests`, `BattleSoundAbilityTests`, `BattleTargetScopePublicReferenceTests` | — |
| `speed-boost` 加速 | 启用 | `end-turn-speed-plus-one` | 完整 | 支持 | `BattleEndTurnAbilityTests` | — |
| `stakeout` 蹲守 | 启用 | `switched-in-target-damage-double` | 完整 | 支持 | `BattleStakeoutAbilityTests` | — |
| `stall` 慢出 | 启用 | `forced-last-action-order` | 完整 | 支持 | `BattleAbilityReplacementTests`, `BattleStallAbilityTests` | — |
| `stalwart` 坚毅 | 启用 | `single-battle-no-effect` | 完整 | 支持 | 不适用（明确无效果） | — |
| `stamina` 持久力 | 启用 | `received-damage-defense-plus-one` | 完整 | 支持 | `BattleReceivedDamageStatAbilityTests` | — |
| `stance-change` 战斗切换 | 启用 | `stance-change-aegislash` | 完整 | 支持 | `BattleStanceChangeAbilityTests` | — |
| `static` 静电 | 启用 | `contact-paralysis` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests`, `BattleContactProtectionBypassAbilityTests`, `BattleEngineSingleTurnTests`, `BattleImmunityTests`, `BattleStatusCureItemTests` | — |
| `steadfast` 不屈之心 | 启用 | `flinch-speed-plus-one` | 完整 | 支持 | `BattleDisableCottonSteadfastAbilityTests` | — |
| `steam-engine` 蒸汽机 | 启用 | `received-fire-water-speed-plus-six` | 完整 | 支持 | `BattleReceivedDamageStatAbilityTests` | — |
| `steelworker` 钢能力者 | 启用 | `element-steel-damage-boost` | 完整 | 支持 | `BattleDamageCalculatorTests`, `BattleElementDamageAbilityTests`, `BattlePassiveSuppressionAbilityTests` | — |
| `steely-spirit` 钢之意志 | 启用 | `element-steel-damage-boost-one-and-half` | 完整 | 支持 | `BattleDamageCalculatorTests`, `BattleElementDamageAbilityTests`, `BattlePassiveSuppressionAbilityTests` | — |
| `stench` 恶臭 | 启用 | `additional-flinch-chance-ten-percent` | 完整 | 支持 | `BattleStenchAbilityTests` | — |
| `sticky-hold` 黏着 | 启用 | `held-item-transfer-immunity` | 完整 | 支持 | `BattleStickyHoldAbilityTests` | — |
| `storm-drain` 引水 | 启用 | `element-water-absorb-special-attack-up` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleElementAbsorbStatAbilityTests` | — |
| `strong-jaw` 强壮之颚 | 启用 | `bite-based-skill-damage-boost-one-and-half` | 完整 | 支持 | `BattleSkillTagAbilityTests` | — |
| `sturdy` 结实 | 启用 | `full-hp-fatal-damage-survival` | 完整 | 支持 | `BattleDamageDealtHealingItemTests`, `BattleFatalDamageSurvivalTests`, `BattleSubstituteTests`, `BattleTargetAbilityIgnoreTests` | — |
| `suction-cups` 吸盘 | 启用 | `forced-switch-immunity` | 完整 | 支持 | `BattleForcedSwitchSkillTests` | — |
| `super-luck` 超幸运 | 启用 | `critical-hit-stage-plus-one` | 完整 | 支持 | `BattleCriticalHitFlowTests` | — |
| `supersweet-syrup` 甘露之蜜 | 启用 | `switch-in-opponents-evasion-minus-one` | 完整 | 支持 | `BattleGuardDogAbilityTests`, `BattleReactiveStatCopyItemTests`, `BattleStatReductionImmunityAbilityTests`, `BattleStatStageReflectionAbilityTests`, `BattleSwitchInAbilityTests` | — |
| `supreme-overlord` 大将 | 启用 | `fainted-ally-damage-boost-tenth-up-to-five` | 完整 | 支持 | `BattleSupremeOverlordAbilityTests` | — |
| `surge-surfer` 冲浪之尾 | 启用 | `terrain-speed-electric` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleTerrainEffectTests` | — |
| `swarm` 虫之预感 | 启用 | `low-hp-bug-boost` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `sweet-veil` 甜幕 | 启用 | `side-major-status-immunity-sleep` | 完整 | 支持 | `BattleStatusImmunityAndGroundingTests` | — |
| `swift-swim` 悠游自如 | 启用 | `weather-speed-rain` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleWeatherEffectTests`, `BattleWeatherSuppressionAbilityTests` | — |
| `sword-of-ruin` 灾祸之剑 | 启用 | `opponent-defense-stat-three-quarters` | 完整 | 支持 | `BattleRuinFluffyAbilityTests` | — |
| `symbiosis` 共生 | 启用 | `ally-item-consumption-transfer` | 完整 | 支持 | `BattleAllyItemConsumptionTransferAbilityTests` | — |
| `synchronize` 同步 | 启用 | `opponent-major-status-reflection` | 完整 | 支持 | `BattleMajorStatusReflectionAbilityTests` | — |
| `tablets-of-ruin` 灾祸之简 | 启用 | `opponent-attack-stat-three-quarters` | 完整 | 支持 | `BattleRuinFluffyAbilityTests` | — |
| `tangled-feet` 蹒跚 | 启用 | `opponent-accuracy-confusion-half` | 完整 | 支持 | `BattleAccuracyAbilityTests` | — |
| `tangling-hair` 卷发 | 启用 | `contact-attacker-speed-minus-one` | 完整 | 支持 | `BattleReceivedDamageStatAbilityTests` | — |
| `technician` 技术高手 | 启用 | `base-power-at-most-sixty-damage-one-and-half` | 完整 | 支持 | `BattleSkillShapeAbilityTests` | — |
| `telepathy` 心灵感应 | 启用 | `ally-damage-immunity` | 完整 | 支持 | `BattleAllyAuraAbilityTests` | — |
| `tera-shell` 太晶甲壳 | 启用 | `full-hp-effectiveness-half` | 完整 | 支持 | `BattleTeraShellAbilityTests` | — |
| `tera-shift` 太晶变形 | 启用 | `switch-in-form-change-terapagos` | 完整 | 支持 | `BattleSwitchInAbilityTests` | — |
| `teraform-zero` 归零化境 | 启用 | `terastallization-environment-clear` | 完整 | 支持 | `BattleTerastallizationEnvironmentClearAbilityTests` | — |
| `teravolt` 兆级电压 | 启用 | `ignore-target-ability-effects` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleAbilityShieldItemTests`, `BattleContactAbilityPublicReferenceTests`, `BattleSoundAbilityTests`, `BattleStatusImmunityAndGroundingTests`, `BattleTargetAbilityIgnoreTests` | — |
| `thermal-exchange` 热交换 | 启用 | `received-fire-attack-plus-one`, `major-status-immunity-burn` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests`, `BattleReceivedDamageStatAbilityTests`, `BattleStatusAbilityRulesTests`, `BattleStatusImmunityAndGroundingTests`, `BattleTargetAbilityIgnoreTests` | — |
| `thick-fat` 厚脂肪 | 启用 | `received-fire-ice-damage-half` | 完整 | 支持 | `BattleElementDamageAbilityTests`, `BattleRuinFluffyAbilityTests` | — |
| `tinted-lens` 有色眼镜 | 启用 | `not-very-effective-damage-boost-double` | 完整 | 支持 | `BattleElementDamageAbilityTests` | — |
| `torrent` 激流 | 启用 | `low-hp-water-boost` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `tough-claws` 硬爪 | 启用 | `contact-based-skill-damage-boost` | 完整 | 支持 | `BattleDamageCalculatorTests` | — |
| `toxic-boost` 中毒激升 | 启用 | `poison-attack-stat-one-and-half` | 完整 | 支持 | `BattleConditionalStatAbilityTests`, `BattleDamageCalculatorTests` | — |
| `toxic-chain` 毒锁链 | 启用 | `dealt-damage-poison-thirty-percent` | 完整 | 支持 | `BattleStatusAbilityRulesTests` | — |
| `toxic-debris` 毒满地 | 启用 | `received-physical-damage-opponent-toxic-spikes` | 完整 | 支持 | `BattleToxicDebrisAbilityTests` | — |
| `trace` 复制 | 启用 | `switch-in-copy-opponent-ability` | 完整 | 支持 | `BattleAbilityReplacementTests` | — |
| `transistor` 电晶体 | 启用 | `element-electric-damage-boost` | 完整 | 支持 | `BattleDamageCalculatorTests`, `BattleElementDamageAbilityTests`, `BattlePassiveSuppressionAbilityTests` | — |
| `triage` 先行治疗 | 启用 | `healing-skill-priority-plus-three` | 完整 | 支持 | `BattleSpecializedPriorityAbilityTests` | — |
| `truant` 懒惰 | 启用 | `every-other-active-turn-action-block` | 完整 | 支持 | `BattleTruantAbilityTests` | — |
| `turboblaze` 涡轮火焰 | 启用 | `ignore-target-ability-effects` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleAbilityShieldItemTests`, `BattleContactAbilityPublicReferenceTests`, `BattleSoundAbilityTests`, `BattleStatusImmunityAndGroundingTests`, `BattleTargetAbilityIgnoreTests` | — |
| `unaware` 纯朴 | 启用 | `ignore-opponent-damage-stat-stages`, `ignore-opponent-accuracy-stat-stages` | 完整 | 支持 | `BattleAccuracyStatStageIgnoreAbilityTests`, `BattleDamageStatStageIgnoreAbilityTests`, `BattleTargetAbilityIgnoreTests` | — |
| `unburden` 轻装 | 启用 | `item-lost-speed-double` | 完整 | 支持 | `BattleUnburdenAbilityTests` | — |
| `unnerve` 紧张感 | 启用 | `opponent-berry-consumption-prevention` | 完整 | 支持 | `BattleOpponentBerryPreventionAbilityTests` | — |
| `unseen-fist` 无形拳 | 启用 | `contact-skill-protection-bypass` | 完整 | 支持 | `BattleContactProtectionBypassAbilityTests` | — |
| `vessel-of-ruin` 灾祸之鼎 | 启用 | `opponent-special-attack-stat-three-quarters` | 完整 | 支持 | `BattleRuinFluffyAbilityTests` | — |
| `victory-star` 胜利之星 | 启用 | `accuracy-multiplier-eleven-tenths` | 完整 | 支持 | `BattleAccuracyAbilityTests` | — |
| `vital-spirit` 干劲 | 启用 | `major-status-immunity-sleep` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests`, `BattleStatusAbilityRulesTests`, `BattleStatusImmunityAndGroundingTests`, `BattleTargetAbilityIgnoreTests` | — |
| `volt-absorb` 蓄电 | 启用 | `element-electric-absorb-heal` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleElementAbsorbAbilityTests`, `BattleTargetAbilityIgnoreTests`, `BattleWeatherElementOverrideTests` | — |
| `wandering-spirit` 游魂 | 启用 | `contact-swap-abilities` | 完整 | 支持 | `BattleAbilityReplacementTests` | — |
| `water-absorb` 储水 | 启用 | `element-water-absorb-heal` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleElementAbsorbAbilityTests`, `BattleTargetAbilityIgnoreTests`, `BattleWeatherElementOverrideTests` | — |
| `water-bubble` 水泡 | 启用 | `element-water-damage-boost-double`, `received-fire-damage-half`, `major-status-immunity-burn` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests`, `BattleDamageCalculatorTests`, `BattleElementDamageAbilityTests`, `BattlePassiveSuppressionAbilityTests`, `BattleRuinFluffyAbilityTests`, `BattleStatusAbilityRulesTests`, `BattleStatusImmunityAndGroundingTests`, `BattleTargetAbilityIgnoreTests` | — |
| `water-compaction` 遇水凝固 | 启用 | `received-water-defense-plus-two` | 完整 | 支持 | `BattleReceivedDamageStatAbilityTests` | — |
| `water-veil` 水幕 | 启用 | `major-status-immunity-burn` | 完整 | 支持 | `BattleContactAbilityPublicReferenceTests`, `BattleStatusAbilityRulesTests`, `BattleStatusImmunityAndGroundingTests`, `BattleTargetAbilityIgnoreTests` | — |
| `weak-armor` 碎裂铠甲 | 启用 | `received-physical-defense-minus-one-speed-plus-two` | 完整 | 支持 | `BattleReceivedDamageStatAbilityTests` | — |
| `well-baked-body` 焦香之躯 | 启用 | `element-fire-absorb-defense-up-two` | 完整 | 支持 | `BattleAbilityItemBoundaryPublicReferenceTests`, `BattleElementAbsorbStatAbilityTests` | — |
| `white-smoke` 白色烟雾 | 启用 | `stat-drop-immunity-all` | 完整 | 支持 | `BattleStatReductionImmunityAbilityTests` | — |
| `wimp-out` 跃跃欲逃 | 启用 | `cross-half-hp-force-self-switch` | 完整 | 支持 | `BattleThresholdSwitchAbilityTests` | — |
| `wind-power` 风力发电 | 启用 | `received-wind-damage-next-electric-damage-double` | 完整 | 支持 | `BattleReceivedDamageChargeAbilityTests`, `BattleWindPowerAbilityTests` | — |
| `wind-rider` 乘风 | 启用 | `wind-skill-immunity-attack-plus-one` | 完整 | 支持 | `BattleWindRiderAbilityTests` | — |
| `wonder-guard` 神奇守护 | 启用 | `non-super-effective-damage-immunity` | 完整 | 支持 | `BattleMyceliumMightAbilityTests`, `BattleTypeImmunityAbilityTests` | — |
| `wonder-skin` 奇迹皮肤 | 启用 | `status-skill-accuracy-cap-half` | 完整 | 支持 | `BattleAccuracyAbilityTests` | — |
| `zen-mode` 达摩模式 | 启用 | `end-turn-hp-form-change-darmanitan` | 完整 | 支持 | `BattleHpFormAbilitiesTests` | — |
| `zero-to-hero` 全能变身 | 启用 | `switch-out-form-change-palafin` | 完整 | 支持 | `BattleSwitchOutAbilityTests` | — |
