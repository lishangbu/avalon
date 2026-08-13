package battleengine

// EventKind 是 Turn Record 中结构化战斗事件的稳定种类。
type EventKind string

const (
	// EventKindTurnStarted 表示引擎已经接受并开始结算一个完整回合。
	EventKindTurnStarted EventKind = "turnStarted"
	// EventKindSkillUsed 表示一个成员已经消费 PP 并正式宣告技能。
	EventKindSkillUsed EventKind = "skillUsed"
	// EventKindSkillMissed 表示技能完成命中掷骰但没有命中目标槽位。
	EventKindSkillMissed EventKind = "skillMissed"
	// EventKindSkillFailed 表示技能已宣告但因其自身规则前提未满足而没有产生效果。
	EventKindSkillFailed EventKind = "skillFailed"
	// EventKindSkillBlocked 表示技能已经宣告，但被目标的确定性防护规则完整阻止。
	EventKindSkillBlocked EventKind = "skillBlocked"
	// EventKindSkillPrevented 表示成员在消费 PP 前被运行态规则阻止行动。
	EventKindSkillPrevented EventKind = "skillPrevented"
	// EventKindRechargeStarted 表示成功造成目标本体伤害的技能已令使用者在下一次行动前休整。
	EventKindRechargeStarted EventKind = "rechargeStarted"
	// EventKindAccuracyLockStarted 表示技能命中后，使用者已锁定一个具体目标成员的下一回合命中。
	EventKindAccuracyLockStarted EventKind = "accuracyLockStarted"
	// EventKindFlinchApplied 表示技能命中后已为目标写入本回合的畏缩行动阻止。
	EventKindFlinchApplied EventKind = "flinchApplied"
	// EventKindVolatileStatusApplied 表示技能已向成员写入一个具备独立持续时间的易变状态。
	EventKindVolatileStatusApplied EventKind = "volatileStatusApplied"
	// EventKindVolatileStatusCleared 表示易变状态因持续时间结束、换人或完成动作而解除。
	EventKindVolatileStatusCleared EventKind = "volatileStatusCleared"
	// EventKindVolatileStatusDamageApplied 表示束缚或混乱等易变状态造成了生命损失。
	EventKindVolatileStatusDamageApplied EventKind = "volatileStatusDamageApplied"
	// EventKindFieldSpeedOrderStarted 表示技能已经建立会改变同优先度行动速度比较方向的全场效果。
	EventKindFieldSpeedOrderStarted EventKind = "fieldSpeedOrderStarted"
	// EventKindFieldSpeedOrderEnded 表示全场速度顺序效果因自然耗尽或再次使用同一种效果而结束。
	EventKindFieldSpeedOrderEnded EventKind = "fieldSpeedOrderEnded"
	// EventKindLeechSeedPlanted 表示技能成功在目标身上写入寄生种子及其来源场上槽位。
	EventKindLeechSeedPlanted EventKind = "leechSeedPlanted"
	// EventKindLeechSeedDamageApplied 表示寄生种子在回合末从目标抽取了生命值。
	EventKindLeechSeedDamageApplied EventKind = "leechSeedDamageApplied"
	// EventKindLeechSeedHealingApplied 表示寄生种子将抽取量回复给来源场上槽位当前成员。
	EventKindLeechSeedHealingApplied EventKind = "leechSeedHealingApplied"
	// EventKindWeatherStarted 表示技能成功建立或覆盖了普通全场天气。
	EventKindWeatherStarted EventKind = "weatherStarted"
	// EventKindWeatherEnded 表示普通全场天气在回合末自然耗尽。
	EventKindWeatherEnded EventKind = "weatherEnded"
	// EventKindWeatherDamageApplied 表示沙暴在回合末对非免疫成员造成伤害。
	EventKindWeatherDamageApplied EventKind = "weatherDamageApplied"
	// EventKindWeatherHealingApplied 表示特性在匹配普通天气的回合末为成员回复生命。
	EventKindWeatherHealingApplied EventKind = "weatherHealingApplied"
	// EventKindHeldItemHealingApplied 表示持有道具在回合末为持有者按最大生命固定比例回复生命。
	EventKindHeldItemHealingApplied EventKind = "heldItemHealingApplied"
	// EventKindHeldItemDamageApplied 表示持有道具在回合末对持有者按最大生命固定比例造成间接伤害。
	EventKindHeldItemDamageApplied EventKind = "heldItemDamageApplied"
	// EventKindAbilityHPChanged 表示反应型特性在其固定触发窗口造成了实际生命回复或伤害。
	EventKindAbilityHPChanged EventKind = "abilityHpChanged"
	// EventKindAbilityChargeChanged 表示受伤充能特性已建立或消费一次性属性伤害强化。
	EventKindAbilityChargeChanged EventKind = "abilityChargeChanged"
	// EventKindHeldItemElementDamageBoostConsumed 表示一次性属性威力强化道具在造成真实本体伤害后已被消费。
	EventKindHeldItemElementDamageBoostConsumed EventKind = "heldItemElementDamageBoostConsumed"
	// EventKindHeldItemElementDamageReductionConsumed 表示一次性抗性道具在减免真实本体伤害后已被消费。
	EventKindHeldItemElementDamageReductionConsumed EventKind = "heldItemElementDamageReductionConsumed"
	// EventKindHeldItemRecoilDamageApplied 表示伤害强化道具在成功造成伤害后对持有者施加反伤。
	EventKindHeldItemRecoilDamageApplied EventKind = "heldItemRecoilDamageApplied"
	// EventKindHeldItemAirborneEnded 表示气球类持有道具在成员承受真实本体伤害后失去空中效果。
	EventKindHeldItemAirborneEnded EventKind = "heldItemAirborneEnded"
	// EventKindHeldItemStatReactionConsumed 表示能力阶级反应道具已完成效果并被消费。
	EventKindHeldItemStatReactionConsumed EventKind = "heldItemStatReactionConsumed"
	// EventKindHeldItemTriggeredConsumed 表示触发型一次性道具完成效果后已被消费。
	EventKindHeldItemTriggeredConsumed EventKind = "heldItemTriggeredConsumed"
	// EventKindHeldItemActionOrderApplied 表示持有道具已为本回合技能行动施加先行或后行排序层。
	EventKindHeldItemActionOrderApplied EventKind = "heldItemActionOrderApplied"
	// EventKindStrongWeatherStarted 表示特性入场后成功建立或覆盖强天气。
	EventKindStrongWeatherStarted EventKind = "strongWeatherStarted"
	// EventKindStrongWeatherEnded 表示最后一个强天气来源离场或倒下后强天气结束。
	EventKindStrongWeatherEnded EventKind = "strongWeatherEnded"
	// EventKindAbilityWeatherStarted 表示特性入场后成功建立或覆盖普通天气。
	EventKindAbilityWeatherStarted EventKind = "abilityWeatherStarted"
	// EventKindAbilityTerrainStarted 表示特性入场后成功建立或覆盖普通场地。
	EventKindAbilityTerrainStarted EventKind = "abilityTerrainStarted"
	// EventKindSwitchInAllyHealingApplied 表示特性成员入场后已为同侧其它场上成员回复生命。
	EventKindSwitchInAllyHealingApplied EventKind = "switchInAllyHealingApplied"
	// EventKindTerrainStarted 表示技能成功建立或覆盖了普通全场场地。
	EventKindTerrainStarted EventKind = "terrainStarted"
	// EventKindTerrainEnded 表示普通全场场地在回合末自然耗尽。
	EventKindTerrainEnded EventKind = "terrainEnded"
	// EventKindTerrainHealingApplied 表示青草场地在回合末为接地成员回复生命。
	EventKindTerrainHealingApplied EventKind = "terrainHealingApplied"
	// EventKindTailwindStarted 表示技能已经在使用者一方建立顺风。
	EventKindTailwindStarted EventKind = "tailwindStarted"
	// EventKindReflectStarted 表示技能已经在使用者一方建立反射壁。
	EventKindReflectStarted EventKind = "reflectStarted"
	// EventKindReflectEnded 表示反射壁在回合末自然耗尽。
	EventKindReflectEnded EventKind = "reflectEnded"
	// EventKindLightScreenStarted 表示技能已经在使用者一方建立光墙。
	EventKindLightScreenStarted EventKind = "lightScreenStarted"
	// EventKindLightScreenEnded 表示光墙在回合末自然耗尽。
	EventKindLightScreenEnded EventKind = "lightScreenEnded"
	// EventKindAuroraVeilStarted 表示技能已经在使用者一方建立极光幕。
	EventKindAuroraVeilStarted EventKind = "auroraVeilStarted"
	// EventKindAuroraVeilEnded 表示极光幕在回合末自然耗尽。
	EventKindAuroraVeilEnded EventKind = "auroraVeilEnded"
	// EventKindSpikesDamageApplied 表示撒菱在成员换入后造成了生命伤害。
	EventKindSpikesDamageApplied EventKind = "spikesDamageApplied"
	// EventKindStealthRockDamageApplied 表示隐形岩在成员换入后按岩石属性倍率造成了生命伤害。
	EventKindStealthRockDamageApplied EventKind = "stealthRockDamageApplied"
	// EventKindToxicSpikesAbsorbed 表示接地毒属性成员换入后吸收了己方场地的全部毒菱层数。
	EventKindToxicSpikesAbsorbed EventKind = "toxicSpikesAbsorbed"
	// EventKindToxicSpikesStatusApplied 表示毒菱在接地成员换入后施加了普通中毒或剧毒。
	EventKindToxicSpikesStatusApplied EventKind = "toxicSpikesStatusApplied"
	// EventKindStickyWebSpeedLowered 表示黏黏网在接地成员换入后降低了其速度能力阶级。
	EventKindStickyWebSpeedLowered EventKind = "stickyWebSpeedLowered"
	// EventKindSpikesLayerAdded 表示技能已在目标一方场地成功增加一层撒菱。
	EventKindSpikesLayerAdded EventKind = "spikesLayerAdded"
	// EventKindStealthRockStarted 表示技能已在目标一方场地成功布置隐形岩。
	EventKindStealthRockStarted EventKind = "stealthRockStarted"
	// EventKindToxicSpikesLayerAdded 表示技能已在目标一方场地成功增加一层毒菱。
	EventKindToxicSpikesLayerAdded EventKind = "toxicSpikesLayerAdded"
	// EventKindStickyWebStarted 表示技能已在目标一方场地成功布置黏黏网。
	EventKindStickyWebStarted EventKind = "stickyWebStarted"
	// EventKindRapidSpinHazardsCleared 表示快速旋转已清除使用者一方的全部入场危害。
	EventKindRapidSpinHazardsCleared EventKind = "rapidSpinHazardsCleared"
	// EventKindDefogSideConditionsCleared 表示清除浓雾已清除目标一方的屏障和入场危害。
	EventKindDefogSideConditionsCleared EventKind = "defogSideConditionsCleared"
	// EventKindDefogTerrainCleared 表示清除浓雾已清除当前普通场地。
	EventKindDefogTerrainCleared EventKind = "defogTerrainCleared"
	// EventKindAbilitySideDamageReductionsCleared 表示入场特性已清除一方阵营的全部减伤屏障。
	EventKindAbilitySideDamageReductionsCleared EventKind = "abilitySideDamageReductionsCleared"
	// EventKindAbilityCopied 表示入场特性已复制一名当前上场对手的特性及其冻结规则。
	EventKindAbilityCopied EventKind = "abilityCopied"
	// EventKindOpponentHeldItemRevealed 表示入场特性已公开一名存活上场对手的持有道具。
	EventKindOpponentHeldItemRevealed EventKind = "opponentHeldItemRevealed"
	// EventKindOpponentSkillRevealed 表示入场特性已公开一名存活上场对手的最高基础威力技能。
	EventKindOpponentSkillRevealed EventKind = "opponentSkillRevealed"
	// EventKindParticipantTransformed 表示入场特性已复制一名存活上场对手的完整战斗画像。
	EventKindParticipantTransformed EventKind = "participantTransformed"
	// EventKindDangerousOpponentSkillDetected 表示入场特性已侦测到一项对自身危险的对手技能。
	EventKindDangerousOpponentSkillDetected EventKind = "dangerousOpponentSkillDetected"
	// EventKindFormChanged 表示成员因明确的特性规则或有效天气切换了战斗形态。
	EventKindFormChanged EventKind = "formChanged"
	// EventKindSwitchOutHealingApplied 表示成员成功离场特性已经为自身回复生命。
	EventKindSwitchOutHealingApplied EventKind = "switchOutHealingApplied"
	// EventKindHeldItemElementIdentityApplied 表示入场特性已按所持道具的属性伤害强化身份替换成员属性。
	EventKindHeldItemElementIdentityApplied EventKind = "heldItemElementIdentityApplied"
	// EventKindHeldItemHighestStatBoostActivated 表示成员已消耗携带道具并开始持续强化一项最高原始能力。
	EventKindHeldItemHighestStatBoostActivated EventKind = "heldItemHighestStatBoostActivated"
	// EventKindHeldItemTransferred 表示接触规则已将持有者当前道具及其运行时投影转移给攻击者。
	EventKindHeldItemTransferred EventKind = "heldItemTransferred"
	// EventKindSkillChargeSkippedByItem 表示一次性持有道具已消耗，并令蓄力技能跳过本次等待。
	EventKindSkillChargeSkippedByItem EventKind = "skillChargeSkippedByItem"
	// EventKindHeldItemParalysisCured 表示一次性持有道具已在成员获得麻痹后立即消耗并解除该异常。
	EventKindHeldItemParalysisCured EventKind = "heldItemParalysisCured"
	// EventKindHeldItemSleepCured 表示一次性持有道具已在成员获得睡眠后立即消耗并解除该异常。
	EventKindHeldItemSleepCured EventKind = "heldItemSleepCured"
	// EventKindHeldItemPoisonCured 表示一次性持有道具已在成员获得普通中毒或剧毒后立即消耗并解除该异常。
	EventKindHeldItemPoisonCured EventKind = "heldItemPoisonCured"
	// EventKindHeldItemBurnCured 表示一次性持有道具已在成员获得灼伤后立即消耗并解除该异常。
	EventKindHeldItemBurnCured EventKind = "heldItemBurnCured"
	// EventKindHeldItemFreezeCured 表示一次性持有道具已在成员获得冰冻后立即消耗并解除该异常。
	EventKindHeldItemFreezeCured EventKind = "heldItemFreezeCured"
	// EventKindHeldItemAllMajorStatusCured 表示一次性持有道具已在成员获得任一种主要异常后立即消耗并解除该异常。
	EventKindHeldItemAllMajorStatusCured EventKind = "heldItemAllMajorStatusCured"
	// EventKindHeldItemConfusionCured 表示一次性持有道具已在成员获得混乱后立即消耗并解除该易变状态。
	EventKindHeldItemConfusionCured EventKind = "heldItemConfusionCured"
	// EventKindParticipantTerastallized 表示成员已在技能开始结算前使用本方唯一太晶化机会。
	EventKindParticipantTerastallized EventKind = "participantTerastallized"
	// EventKindTailwindEnded 表示一方顺风在回合末自然耗尽。
	EventKindTailwindEnded EventKind = "tailwindEnded"
	// EventKindParticipantSwitched 表示一个场上槽位已经替换为同侧后备成员。
	EventKindParticipantSwitched EventKind = "participantSwitched"
	// EventKindForcedTargetSwitchSelected 表示技能强制目标换人已从健康后备成员中确定替换者。
	// 该事件在实际 ParticipantSwitchedEvent 之前写入，用于审计候选集合和可重放随机选择。
	EventKindForcedTargetSwitchSelected EventKind = "forcedTargetSwitchSelected"
	// EventKindAbilityForcedSwitchSelected 表示半血跨越特性已经从健康后备成员中确定持有者的替换者。
	// 该事件在实际 ParticipantSwitchedEvent 之前写入，用于审计候选集合和可重放随机选择。
	EventKindAbilityForcedSwitchSelected EventKind = "abilityForcedSwitchSelected"
	// EventKindItemForcedSwitchSelected 表示一次性持有道具已确定其强制换人的后备成员。
	// 该事件在实际 ParticipantSwitchedEvent 之前写入，并与持有道具清空共同表达“选择成功才消耗”的规则事实。
	EventKindItemForcedSwitchSelected EventKind = "itemForcedSwitchSelected"
	// EventKindMajorStatusApplied 表示一项主要异常状态已经写入目标成员。
	EventKindMajorStatusApplied EventKind = "majorStatusApplied"
	// EventKindMajorStatusCleared 表示成员当前主要异常状态已经解除。
	EventKindMajorStatusCleared EventKind = "majorStatusCleared"
	// EventKindMajorStatusBlocked 表示主要异常因现有状态或规则免疫没有写入目标。
	EventKindMajorStatusBlocked EventKind = "majorStatusBlocked"
	// EventKindMajorStatusDamageApplied 表示主要异常在回合末扣除了成员生命值。
	EventKindMajorStatusDamageApplied EventKind = "majorStatusDamageApplied"
	// EventKindStatStageChanged 表示成员的一项能力阶级已经发生实际变化。
	EventKindStatStageChanged EventKind = "statStageChanged"
	// EventKindDamageApplied 表示一段伤害已经写入目标当前生命值。
	EventKindDamageApplied EventKind = "damageApplied"
	// EventKindFatalDamageSurvived 表示满生命成员因特性保留了 1 HP，未被本段技能伤害击倒。
	EventKindFatalDamageSurvived EventKind = "fatalDamageSurvived"
	// EventKindSubstituteStarted 表示成员已支付本体生命并成功建立替身。
	EventKindSubstituteStarted EventKind = "substituteStarted"
	// EventKindSubstituteDamageApplied 表示对方技能伤害已写入目标的替身生命值。
	EventKindSubstituteDamageApplied EventKind = "substituteDamageApplied"
	// EventKindSubstituteBroken 表示替身生命值因伤害降为零并立即破裂。
	EventKindSubstituteBroken EventKind = "substituteBroken"
	// EventKindHPAveragedBySkill 表示技能已把双方当前生命分别重设为同一平均值。
	EventKindHPAveragedBySkill EventKind = "hpAveragedBySkill"
	// EventKindSkillHealingApplied 表示技能后效已经回复了使用者生命值。
	EventKindSkillHealingApplied EventKind = "skillHealingApplied"
	// EventKindSkillRecoilDamageApplied 表示技能后效已经扣除了使用者生命值。
	EventKindSkillRecoilDamageApplied EventKind = "skillRecoilDamageApplied"
	// EventKindContactDamageApplied 表示目标特性已因有效接触的本体伤害向攻击者施加反制伤害。
	EventKindContactDamageApplied EventKind = "contactDamageApplied"
	// EventKindSkillSelfSacrificeDamageApplied 表示技能命中后按规则令使用者失去全部当前生命。
	EventKindSkillSelfSacrificeDamageApplied EventKind = "skillSelfSacrificeDamageApplied"
	// EventKindParticipantFainted 表示一名成员的生命值已经降为 0。
	EventKindParticipantFainted EventKind = "participantFainted"
	// EventKindBattleEnded 表示引擎已经根据战斗规则确认终局结果。
	EventKindBattleEnded EventKind = "battleEnded"
	// EventKindTurnEnded 表示当前完整回合的全部行动已经结算完毕。
	EventKindTurnEnded EventKind = "turnEnded"
)

// FormChangeReason 是 FormChangedEvent 的封闭触发原因。
type FormChangeReason string

const (
	// FormChangeReasonSwitchInAbility 表示成员进入场地时由其特性触发形态切换。
	FormChangeReasonSwitchInAbility FormChangeReason = "switchInAbility"
	// FormChangeReasonWeatherAbility 表示成员按当前有效天气由其特性同步形态。
	FormChangeReasonWeatherAbility FormChangeReason = "weatherAbility"
	// FormChangeReasonSwitchOutAbility 表示成员成功离开场地时由其特性触发形态切换。
	FormChangeReasonSwitchOutAbility FormChangeReason = "switchOutAbility"
)

// Event 是所有版本化战斗事件共同实现的只读边界。
type Event interface {
	// Kind 返回事件用于分派和持久化的稳定种类。
	Kind() EventKind
}

// MemberRef 使用阵营位置和队伍成员位置稳定引用一名参战成员。
type MemberRef struct {
	// Side 是成员所属的稳定阵营位置。
	Side Side `json:"side"`
	// Position 是成员在该方队伍快照中的稳定位置。
	Position MemberPosition `json:"memberPosition"`
}

// TurnStartedEvent 记录完整回合结算边界的开始。
type TurnStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
}

// Kind 返回 turnStarted。
func (event TurnStartedEvent) Kind() EventKind {
	return event.Type
}

// SkillUsedEvent 记录成员正式宣告并消费技能 PP 的事实。
type SkillUsedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是实际使用技能的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// Target 是宣告时选择的目标场上槽位。
	Target SlotRef `json:"target"`
	// SkillPosition 是行动者本场冻结技能列表中的稳定槽位。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是本次使用技能在实时资料中的稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// RemainingPP 是本次消费完成后的技能剩余 PP。
	RemainingPP uint8 `json:"remainingPp"`
}

// Kind 返回 skillUsed。
func (event SkillUsedEvent) Kind() EventKind {
	return event.Type
}

// SkillMissedEvent 记录一次技能命中判定失败的完整可重放事实。
type SkillMissedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是没有命中目标的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// Target 是本次命中判定针对的稳定场上槽位。
	Target SlotRef `json:"target"`
	// SkillPosition 是使用者冻结技能列表中的稳定槽位。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是未命中技能在实时资料中的稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Accuracy 是能力与环境修正后的最终命中率。
	Accuracy uint8 `json:"accuracy"`
	// Roll 是本次 1 至 100 的命中掷骰结果。
	Roll uint8 `json:"roll"`
}

// Kind 返回 skillMissed。
func (event SkillMissedEvent) Kind() EventKind {
	return event.Type
}

// SkillFailureReason 是技能已经宣告但不应继续结算时的稳定规则原因。
type SkillFailureReason string

const (
	// SkillFailureReasonTargetHPNotGreaterThanUserHP 表示目标当前生命不高于使用者，因而 HP 差值伤害失败。
	SkillFailureReasonTargetHPNotGreaterThanUserHP SkillFailureReason = "targetHPNotGreaterThanUserHP"
	// SkillFailureReasonProtectionFailed 表示连续使用保护时，递减成功率的随机判定失败。
	SkillFailureReasonProtectionFailed SkillFailureReason = "protectionFailed"
	// SkillFailureReasonSubstituteAlreadyActive 表示使用者已经拥有未破裂的替身，不能再次建立。
	SkillFailureReasonSubstituteAlreadyActive SkillFailureReason = "substituteAlreadyActive"
	// SkillFailureReasonInsufficientHPForSubstitute 表示使用者当前生命不严格大于建立替身所需费用。
	SkillFailureReasonInsufficientHPForSubstitute SkillFailureReason = "insufficientHPForSubstitute"
	// SkillFailureReasonTargetBehindSubstitute 表示目标替身阻止了必须直接修改目标本体生命的技能。
	SkillFailureReasonTargetBehindSubstitute SkillFailureReason = "targetBehindSubstitute"
	// SkillFailureReasonOneHitKnockOutTargetLevelHigher 表示一击必杀的目标等级高于使用者，技能在命中前失败。
	SkillFailureReasonOneHitKnockOutTargetLevelHigher SkillFailureReason = "oneHitKnockOutTargetLevelHigher"
	// SkillFailureReasonReceivedDamageMemoryUnavailable 表示伤害记忆技能在本回合找不到仍然有效的合格受伤记录。
	//
	// 此失败发生在技能已经宣告并消耗 PP 之后，但早于命中、要害和伤害随机数；因此它不会把“没有可返还
	// 的伤害”伪装成未命中，也不会污染离线回放的随机轨迹。
	SkillFailureReasonReceivedDamageMemoryUnavailable SkillFailureReason = "receivedDamageMemoryUnavailable"
	// SkillFailureReasonLeechSeedTargetAlreadySeeded 表示目标已经携带寄生种子，不能刷新或覆盖原来源槽位。
	SkillFailureReasonLeechSeedTargetAlreadySeeded SkillFailureReason = "leechSeedTargetAlreadySeeded"
	// SkillFailureReasonLeechSeedGrassTarget 表示目标当前拥有草属性，因此免疫寄生种子写入。
	SkillFailureReasonLeechSeedGrassTarget SkillFailureReason = "leechSeedGrassTarget"
	// SkillFailureReasonLeechSeedTargetBehindSubstitute 表示目标替身阻止对手将寄生种子写入其本体。
	SkillFailureReasonLeechSeedTargetBehindSubstitute SkillFailureReason = "leechSeedTargetBehindSubstitute"
	// SkillFailureReasonAccuracyLockTargetBehindSubstitute 表示目标替身阻止对手建立命中锁定。
	SkillFailureReasonAccuracyLockTargetBehindSubstitute SkillFailureReason = "accuracyLockTargetBehindSubstitute"
	// SkillFailureReasonAccuracyLockAlreadyActive 表示使用者已经锁定同一个具体目标，不能刷新持续时间。
	SkillFailureReasonAccuracyLockAlreadyActive SkillFailureReason = "accuracyLockAlreadyActive"
	// SkillFailureReasonWeatherAlreadyActive 表示技能尝试建立当前已生效的同种天气，不能刷新持续时间。
	SkillFailureReasonWeatherAlreadyActive SkillFailureReason = "weatherAlreadyActive"
	// SkillFailureReasonStrongWeatherActive 表示强天气阻止普通天气技能覆盖或建立天气。
	SkillFailureReasonStrongWeatherActive SkillFailureReason = "strongWeatherActive"
	// SkillFailureReasonStrongWeatherNegatesDamagingSkill 表示强日照或强降雨使对应属性伤害技能直接失败。
	SkillFailureReasonStrongWeatherNegatesDamagingSkill SkillFailureReason = "strongWeatherNegatesDamagingSkill"
	// SkillFailureReasonTerrainAlreadyActive 表示技能尝试建立当前已生效的同种场地，不能刷新持续时间。
	SkillFailureReasonTerrainAlreadyActive SkillFailureReason = "terrainAlreadyActive"
	// SkillFailureReasonPsychicTerrainTargetGrounded 表示精神场地阻止正优先度技能影响接地对手。
	SkillFailureReasonPsychicTerrainTargetGrounded SkillFailureReason = "psychicTerrainTargetGrounded"
	// SkillFailureReasonTailwindAlreadyActive 表示技能尝试在已有顺风的一方重新建立顺风，不能刷新持续时间。
	SkillFailureReasonTailwindAlreadyActive SkillFailureReason = "tailwindAlreadyActive"
	// SkillFailureReasonReflectAlreadyActive 表示技能尝试在已有反射壁的一方重新建立反射壁，不能刷新持续时间。
	SkillFailureReasonReflectAlreadyActive SkillFailureReason = "reflectAlreadyActive"
	// SkillFailureReasonLightScreenAlreadyActive 表示技能尝试在已有光墙的一方重新建立光墙，不能刷新持续时间。
	SkillFailureReasonLightScreenAlreadyActive SkillFailureReason = "lightScreenAlreadyActive"
	// SkillFailureReasonAuroraVeilAlreadyActive 表示技能尝试在已有极光幕的一方重新建立极光幕，不能刷新持续时间。
	SkillFailureReasonAuroraVeilAlreadyActive SkillFailureReason = "auroraVeilAlreadyActive"
	// SkillFailureReasonSpikesAtMaximumLayers 表示技能尝试在已有三层撒菱的一方继续增加层数。
	SkillFailureReasonSpikesAtMaximumLayers SkillFailureReason = "spikesAtMaximumLayers"
	// SkillFailureReasonStealthRockAlreadyActive 表示技能尝试在已有隐形岩的一方重复布置隐形岩。
	SkillFailureReasonStealthRockAlreadyActive SkillFailureReason = "stealthRockAlreadyActive"
	// SkillFailureReasonToxicSpikesAtMaximumLayers 表示技能尝试在已有两层毒菱的一方继续增加层数。
	SkillFailureReasonToxicSpikesAtMaximumLayers SkillFailureReason = "toxicSpikesAtMaximumLayers"
	// SkillFailureReasonStickyWebAlreadyActive 表示技能尝试在已有黏黏网的一方重复布置黏黏网。
	SkillFailureReasonStickyWebAlreadyActive SkillFailureReason = "stickyWebAlreadyActive"
)

// SkillFailedEvent 记录一项技能已经消费 PP 并通过命中步骤，但其显式执行规则不允许产生效果的事实。
//
// 这不同于 SkillMissedEvent：后者表示命中判定失败，而本事件表示目标已命中、但例如当前生命差值不满足
// 技能自身的正数伤害前提。把二者分开可使离线重放和统计准确区分失败原因。
type SkillFailedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是已经宣告该技能的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// Target 是命中后发现规则前提不满足的实际目标成员稳定引用。
	Target MemberRef `json:"target"`
	// SkillPosition 是失败技能在行动者冻结技能列表中的稳定槽位。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是失败技能在实时资料中的稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Reason 是使本次已宣告技能停止结算的稳定机器原因。
	Reason SkillFailureReason `json:"reason"`
}

// Kind 返回 skillFailed。
func (event SkillFailedEvent) Kind() EventKind {
	return event.Type
}

// SkillBlockReason 是技能未产生目标效果的确定性防护原因。
type SkillBlockReason string

const (
	// SkillBlockReasonProtection 表示目标在本回合持有保护易变状态。
	SkillBlockReasonProtection SkillBlockReason = "protection"
	// SkillBlockReasonOpponentStatusSkillImmunity 表示目标特性免疫对手使用的变化技能。
	SkillBlockReasonOpponentStatusSkillImmunity SkillBlockReason = "opponentStatusSkillImmunity"
	// SkillBlockReasonPowderSkillImmunity 表示目标持有道具在命中前免疫本次粉末或孢子类技能。
	// 该原因属于道具防护，攻击方无视目标特性不能绕过它。
	SkillBlockReasonPowderSkillImmunity SkillBlockReason = "powderSkillImmunity"
	// SkillBlockReasonNonSuperEffectiveDamageImmunity 表示目标特性免疫本次相性不克制的对手伤害技能。
	SkillBlockReasonNonSuperEffectiveDamageImmunity SkillBlockReason = "nonSuperEffectiveDamageImmunity"
	// SkillBlockReasonPriorityMoveImmunityForSide 表示目标侧当前上场成员的特性阻止了对手正优先度技能。
	// 具体的特性拥有者由 SkillBlockedEvent.Blocker 保存，以区分目标自身阻止与伙伴侧保护。
	SkillBlockReasonPriorityMoveImmunityForSide SkillBlockReason = "priorityMoveImmunityForSide"
	// SkillBlockReasonElementImmunity 表示目标的当前属性对本次技能属性具有完全免疫。
	//
	// 它与“非零但不利或有利的属性倍率”不同：伤害记忆可以资料化地忽略后者，却绝不能穿透该阻止。
	SkillBlockReasonElementImmunity SkillBlockReason = "elementImmunity"
	// SkillBlockReasonOneHitKnockOutSameElementTarget 表示一击必杀被目标持有的同属性天然阻止。
	SkillBlockReasonOneHitKnockOutSameElementTarget SkillBlockReason = "oneHitKnockOutSameElementTarget"
)

// SkillBlockedEvent 记录一次已经消费 PP、但被目标保护完整阻止的技能。
//
// 它不同于 SkillMissedEvent：保护不消费命中随机数；也不同于 SkillFailedEvent：后者由技能自身
// 前提失败导致，而本事件的原因来自目标当前可观察的运行态。
type SkillBlockedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是技能使用者的稳定成员引用。
	Actor MemberRef `json:"actor"`
	// Target 是实际阻止本次技能的成员稳定引用。
	Target MemberRef `json:"target"`
	// Blocker 是提供本次防守特性的当前上场成员；只有侧范围特性等阻止者可能不同于目标的规则才会设置。
	// nil 表示阻止原因没有独立成员来源，避免用零值 MemberRef 伪造一个不存在的场上位置。
	Blocker *MemberRef `json:"blocker,omitempty"`
	// SkillPosition 是已消费 PP 的冻结技能槽位置。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是被阻止的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Reason 是阻止本次技能的封闭规则原因。
	Reason SkillBlockReason `json:"reason"`
}

// Kind 返回 skillBlocked。
func (event SkillBlockedEvent) Kind() EventKind {
	return event.Type
}

// SkillPreventionReason 是行动前阻止技能执行的稳定规则原因。
type SkillPreventionReason string

const (
	// SkillPreventionReasonParalysis 表示麻痹的 25% 判定阻止了本次行动。
	SkillPreventionReasonParalysis SkillPreventionReason = "paralysis"
	// SkillPreventionReasonSleep 表示睡眠消耗了一次剩余行动阻止次数。
	SkillPreventionReasonSleep SkillPreventionReason = "sleep"
	// SkillPreventionReasonFreeze 表示冰冻自然解冻失败并阻止了本次行动。
	SkillPreventionReasonFreeze SkillPreventionReason = "freeze"
	// SkillPreventionReasonFlinch 表示本回合较早命中的技能使成员畏缩。
	SkillPreventionReasonFlinch SkillPreventionReason = "flinch"
	// SkillPreventionReasonConfusion 表示混乱行动判定使成员伤害自身而无法使用技能。
	SkillPreventionReasonConfusion SkillPreventionReason = "confusion"
	// SkillPreventionReasonTaunt 表示挑衅阻止成员使用变化技能。
	SkillPreventionReasonTaunt SkillPreventionReason = "taunt"
	// SkillPreventionReasonDisable 表示成员选择的技能仍处于定身状态。
	SkillPreventionReasonDisable SkillPreventionReason = "disable"
	// SkillPreventionReasonRecharge 表示成员因上一次成功使用的休整技能而必须放弃本次技能行动。
	SkillPreventionReasonRecharge SkillPreventionReason = "recharge"
)

// SkillPreventedEvent 记录成员在技能宣告和 PP 消费前无法行动的事实。
type SkillPreventedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是本次无法行动的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// Reason 是 paralysis 等稳定阻止原因。
	Reason SkillPreventionReason `json:"reason"`
	// TurnsRemainingBefore 是本次消费前的状态剩余阻止次数；不适用时为 0。
	TurnsRemainingBefore int32 `json:"turnsRemainingBefore"`
}

// Kind 返回 skillPrevented。
func (event SkillPreventedEvent) Kind() EventKind {
	return event.Type
}

// RechargeStartedEvent 记录一项技能成功扣除目标本体生命后，使用者获得后续休整状态的事实。
//
// 它不表示当前技能被阻止：当前行动已经完整结算，TurnsRemainingAfterCurrent 只约束未来的技能行动。把该事实
// 独立为事件可使重放、观战和统计明确区分“成功造成伤害”与“下一回合必须空过”。
type RechargeStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是已进入休整状态的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillID 是造成目标本体生命损失并触发休整的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// TurnsRemainingAfterCurrent 是当前回合结束后仍会阻止的技能行动次数，当前规则固定为 1。
	TurnsRemainingAfterCurrent uint8 `json:"turnsRemainingAfterCurrent"`
}

// Kind 返回 rechargeStarted。
func (event RechargeStartedEvent) Kind() EventKind {
	return event.Type
}

// AccuracyLockStartedEvent 记录一项技能已经使命中者在下一回合内锁定当前具体目标成员的命中。
//
// 锁定只跳过后续常规命中骰，不会绕过保护、一击必杀等级限制或属性免疫等更早的规则门槛。目标换出时，
// 引擎会同时清除所有指向该成员的锁定，因而事件中的 Target 永远不表示一个可由槽位继承的抽象目标。
type AccuracyLockStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷���构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是已获得命中锁定的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// Target 是被锁定的当前具体目标成员稳定引用。
	Target MemberRef `json:"target"`
	// SkillPosition 是建立锁定的技能稳定槽位。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是建立锁定的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// TurnsRemaining 是建立锁定后、回合末推进前的剩余阶段数，首版固定为 2。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// Kind 返回 accuracyLockStarted。
func (event AccuracyLockStartedEvent) Kind() EventKind {
	return event.Type
}

// FlinchAppliedEvent 记录一项已命中技能按概率使目标在当前回合无法继续使用技能的事实。
//
// 畏缩不占用 MajorStatus，也不需要跨回合存活：状态快照只保存目标被写入的回合号，回合推进后自然失效。
type FlinchAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是造成畏缩的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// Target 是被写入畏缩状态的目标成员稳定引用。
	Target MemberRef `json:"target"`
	// SkillID 是触发畏缩的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// ChancePercent 是冻结资料中本次畏缩判定使用的成功概率。
	ChancePercent uint8 `json:"chancePercent"`
}

// Kind 返回 flinchApplied。
func (event FlinchAppliedEvent) Kind() EventKind {
	return event.Type
}

// VolatileStatusAppliedEvent 记录一项有明确剩余时长的易变状态已经写入成员。
type VolatileStatusAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是写入本状态的技能使用者。
	Actor MemberRef `json:"actor"`
	// Target 是实际承受或持有该状态的成员。
	Target MemberRef `json:"target"`
	// SkillID 是来源技能的稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Status 是已写入的易变状态稳定代码。
	Status VolatileStatus `json:"status"`
	// TurnsRemaining 是写入后的剩余持续时间或强制行动次数。
	TurnsRemaining uint8 `json:"turnsRemaining"`
	// SkillPosition 只供 charging、lockedMove 和 disable 表示关联的确定技能槽；其它状态为 0。
	SkillPosition SkillPosition `json:"skillPosition"`
}

// Kind 返回 volatileStatusApplied。
func (event VolatileStatusAppliedEvent) Kind() EventKind {
	return event.Type
}

// VolatileStatusClearedEvent 记录成员的一个易变状态已经解除。
type VolatileStatusClearedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是状态解除的成员。
	Target MemberRef `json:"target"`
	// Status 是已解除的易变状态稳定代码。
	Status VolatileStatus `json:"status"`
	// SkillPosition 只供 charging、lockedMove 和 disable 表示解除的关联技能槽；其它状态为 0。
	SkillPosition SkillPosition `json:"skillPosition"`
}

// Kind 返回 volatileStatusCleared。
func (event VolatileStatusClearedEvent) Kind() EventKind {
	return event.Type
}

// VolatileStatusDamageAppliedEvent 记录易变状态对成员施加的直接生命损失。
type VolatileStatusDamageAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是实际承受状态伤害的成员。
	Target MemberRef `json:"target"`
	// Status 是造成伤害的易变状态稳定代码。
	Status VolatileStatus `json:"status"`
	// Amount 是本次实际扣除的生命值。
	Amount uint32 `json:"amount"`
	// CurrentHP 是扣除后成员的实际生命值。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 volatileStatusDamageApplied。
func (event VolatileStatusDamageAppliedEvent) Kind() EventKind {
	return event.Type
}

// FieldSpeedOrderStartedEvent 记录技能已经向全场环境建立一个持续的速度排序规则。
//
// 它不会改写任何成员的 Speed 属性或能力阶级；后续行动计划只会在优先度相同时读取其 Kind 来确定有效速度的
// 比较方向。TurnsRemaining 记录建立时的原始时长，状态快照会在同一回合末递减后保存剩余时长。
type FieldSpeedOrderStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是成功建立全场效果的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillPosition 是建立效果的技能在使用者冻结技能列表中的稳定槽位。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是建立效果的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// FieldSpeedOrderKind 是已经写入全场环境的封闭速度排序效果种类。
	FieldSpeedOrderKind FieldSpeedOrderKind `json:"fieldSpeedOrderKind"`
	// TurnsRemaining 是效果建立时声明的剩余完整回合数。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// Kind 返回 fieldSpeedOrderStarted。
func (event FieldSpeedOrderStartedEvent) Kind() EventKind {
	return event.Type
}

// FieldSpeedOrderEndedEvent 记录全场速度排序规则已经恢复为通常速度优先。
//
// Actor、SkillPosition 和 SkillID 同时为零值时表示效果在回合末自然耗尽；三者非零时表示技能再次成功使用
// 相同 kind，触发了“再次使用即解除”的现代规则。
type FieldSpeedOrderEndedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// FieldSpeedOrderKind 是已经结束的封闭速度排序效果种类。
	FieldSpeedOrderKind FieldSpeedOrderKind `json:"fieldSpeedOrderKind"`
	// Actor 是触发再次使用解除语义的技能使用者；自然耗尽时为零值。
	Actor MemberRef `json:"actor,omitempty"`
	// SkillPosition 是触发解除的技能槽；自然耗尽时为 0。
	SkillPosition SkillPosition `json:"skillPosition,omitempty"`
	// SkillID 是触发解除的技能稳定 Identifier；自然耗尽时为空字符串。
	SkillID Identifier `json:"skillId,omitempty"`
}

// Kind 返回 fieldSpeedOrderEnded。
func (event FieldSpeedOrderEndedEvent) Kind() EventKind {
	return event.Type
}

// WeatherStartedEvent 记录技能已经建立或覆盖普通全场天气。
type WeatherStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是建立天气的技能使用者。
	Actor MemberRef `json:"actor"`
	// SkillPosition 是建立天气的技能槽。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是建立天气的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Weather 是已经写入全场环境的天气种类。
	Weather WeatherKind `json:"weather"`
	// TurnsRemaining 是天气建立时的剩余完整回合数。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// Kind 返回 weatherStarted。
func (event WeatherStartedEvent) Kind() EventKind { return event.Type }

// WeatherEndedEvent 记录普通天气在回合末自然耗尽。
type WeatherEndedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Weather 是已经结束的天气种类。
	Weather WeatherKind `json:"weather"`
}

// Kind 返回 weatherEnded。
func (event WeatherEndedEvent) Kind() EventKind { return event.Type }

// StrongWeatherStartedEvent 记录成员入场后成功建立或覆盖强天气。
type StrongWeatherStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号；初始上场效果使用第 0 回合。
	TurnNumber uint32 `json:"turnNumber"`
	// Source 是实际维持本次强天气的入场成员稳定引用。
	Source MemberRef `json:"source"`
	// StrongWeather 是已经写入全场环境的强天气种类。
	StrongWeather StrongWeatherKind `json:"strongWeather"`
}

// Kind 返回 strongWeatherStarted。
func (event StrongWeatherStartedEvent) Kind() EventKind { return event.Type }

// StrongWeatherEndedEvent 记录最后一个同类来源离场或倒下后强天气结束。
type StrongWeatherEndedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// StrongWeather 是已经结束的强天气种类。
	StrongWeather StrongWeatherKind `json:"strongWeather"`
}

// Kind 返回 strongWeatherEnded。
func (event StrongWeatherEndedEvent) Kind() EventKind { return event.Type }

// AbilityWeatherStartedEvent 记录成员入场特性成功建立或覆盖普通天气。
type AbilityWeatherStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号；初始上场效果使用第 0 回合。
	TurnNumber uint32 `json:"turnNumber"`
	// Source 是触发入场天气特性的成员稳定引用。
	Source MemberRef `json:"source"`
	// Weather 是已经写入全场环境的普通天气种类。
	Weather WeatherKind `json:"weather"`
	// TurnsRemaining 是天气建立时的完整剩余回合数。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// Kind 返回 abilityWeatherStarted。
func (event AbilityWeatherStartedEvent) Kind() EventKind { return event.Type }

// AbilityTerrainStartedEvent 记录成员入场特性成功建立或覆盖普通场地。
type AbilityTerrainStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号；初始上场效果使用第 0 回合。
	TurnNumber uint32 `json:"turnNumber"`
	// Source 是触发入场场地特性的成员稳定引用。
	Source MemberRef `json:"source"`
	// Terrain 是已经写入全场环境的普通场地种类。
	Terrain TerrainKind `json:"terrain"`
	// TurnsRemaining 是场地建立时的完整剩余回合数。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// Kind 返回 abilityTerrainStarted。
func (event AbilityTerrainStartedEvent) Kind() EventKind { return event.Type }

// SwitchInAllyHealingAppliedEvent 记录成员入场特性为同侧其它场上成员产生的一段实际回复。
//
// 它与技能回复、天气回复和场地回复保持独立事件种类，调用方可以根据来源生命周期正确重放与展示，不能
// 通过通用“healed”文本事件丢失入场特性的触发事实。
type SwitchInAllyHealingAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号；初始上场的回复不会写入事件流。
	TurnNumber uint32 `json:"turnNumber"`
	// Source 是触发入场回复特性的成员稳定引用。
	Source MemberRef `json:"source"`
	// Recipient 是实际得到回复的同侧其它场上成员稳定引用。
	Recipient MemberRef `json:"recipient"`
	// Amount 是已经按接收者缺失生命夹取的实际回复量。
	Amount uint32 `json:"amount"`
	// CurrentHP 是回复完成后接收者的当前生命。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 switchInAllyHealingApplied。
func (event SwitchInAllyHealingAppliedEvent) Kind() EventKind { return event.Type }

// WeatherDamageAppliedEvent 记录天气在回合末造成的生命伤害。
type WeatherDamageAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是承受天气伤害的成员稳定引用。
	Target MemberRef `json:"target"`
	// Weather 是本次产生伤害的天气种类。
	Weather WeatherKind `json:"weather"`
	// Amount 是已经按当前生命夹取的实际伤害。
	Amount uint32 `json:"amount"`
	// CurrentHP 是伤害后的当前生命。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 weatherDamageApplied。
func (event WeatherDamageAppliedEvent) Kind() EventKind { return event.Type }

// WeatherHealingAppliedEvent 记录特性在匹配普通天气的回合末为一名成员回复生命的结果。
type WeatherHealingAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是实际获得天气回复的成员稳定引用。
	Target MemberRef `json:"target"`
	// Weather 是本次触发回复的普通天气种类。
	Weather WeatherKind `json:"weather"`
	// Amount 是已按目标缺失生命夹取的实际回复量。
	Amount uint32 `json:"amount"`
	// CurrentHP 是回复后的当前生命。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 weatherHealingApplied。
func (event WeatherHealingAppliedEvent) Kind() EventKind { return event.Type }

// HeldItemHealingAppliedEvent 记录持有道具在回合末为当前持有者回复生命的结果。
//
// 回复规则不消费道具也不读取随机数。事件同时保存分母和实际回复量，使回放可以区分固定比例道具回复与天气、
// 场地或技能回复，而无需从前后生命值差推断规则来源。
type HeldItemHealingAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是实际获得持有道具回复的成员稳定引用。
	Target MemberRef `json:"target"`
	// ItemID 是本次仍被持有、提供回复规则的道具稳定 Identifier。
	ItemID Identifier `json:"itemId"`
	// Denominator 是最大生命固定比例的正分母。
	Denominator uint16 `json:"denominator"`
	// Amount 是已经按目标缺失生命夹取的实际回复量。
	Amount uint32 `json:"amount"`
	// CurrentHP 是回复后的当前生命。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 heldItemHealingApplied。
func (event HeldItemHealingAppliedEvent) Kind() EventKind { return event.Type }

// HeldItemDamageAppliedEvent 记录持有道具在回合末对当前持有者造成间接伤害的结果。
//
// 该规则不要求本回合造成技能伤害或发生接触；它不消费道具也不读取随机数，但会被持有者的间接伤害免疫阻止。
// 事件保存分母和实际伤害，使回放能够区分道具自伤、天气、主要异常与接触反制的来源。
type HeldItemDamageAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是实际承受持有道具自伤的成员稳定引用。
	Target MemberRef `json:"target"`
	// ItemID 是本次仍被持有、提供自伤规则的道具稳定 Identifier。
	ItemID Identifier `json:"itemId"`
	// Denominator 是最大生命固定比例的正分母。
	Denominator uint16 `json:"denominator"`
	// Amount 是已经按目标当前生命夹取的实际伤害。
	Amount uint32 `json:"amount"`
	// CurrentHP 是伤害后的当前生命。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 heldItemDamageApplied。
func (event HeldItemDamageAppliedEvent) Kind() EventKind { return event.Type }

// HeldItemElementDamageBoostConsumedEvent 记录一次性属性威力强化道具已经在成功造成本体伤害后被消费。
//
// 该事件只在匹配的有效技能属性造成实际本体生命损失时产生；替身伤害、未命中、无效属性和零伤害均不会消费，
// 因而重放消费者能从事件本身区分“参与伤害公式但未满足消费条件”与“已完成消费”。
type HeldItemElementDamageBoostConsumedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是使用匹配属性技能并消费道具的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillID 是触发本次道具消费的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// ItemID 是刚刚被消费的一次性持有道具稳定 Identifier。
	ItemID Identifier `json:"itemId"`
	// ElementID 是与该道具规则匹配的技能有效属性稳定 Identifier。
	ElementID Identifier `json:"elementId"`
}

// Kind 返回 heldItemElementDamageBoostConsumed。
func (event HeldItemElementDamageBoostConsumedEvent) Kind() EventKind { return event.Type }

// HeldItemElementDamageReductionConsumedEvent 记录一次性抗性道具已减免匹配属性的本体伤害并消费。
//
// 事件在 DamageAppliedEvent 之后产生，记录实际触发成员、攻击技能、道具和属性；替身承伤、属性不匹配以及未满足
// 严格克制条件时不会产生，重放因此能够精确验证消费边界和事件顺序。
type HeldItemElementDamageReductionConsumedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是持有并消费抗性道具的受击成员稳定引用。
	Target MemberRef `json:"target"`
	// SkillID 是触发抗性减伤的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// ItemID 是刚刚消费的持有道具稳定 Identifier。
	ItemID Identifier `json:"itemId"`
	// ElementID 是与道具规则匹配的技能有效属性稳定 Identifier。
	ElementID Identifier `json:"elementId"`
}

// Kind 返回 heldItemElementDamageReductionConsumed。
func (event HeldItemElementDamageReductionConsumedEvent) Kind() EventKind { return event.Type }

// HeldItemRecoilDamageAppliedEvent 记录伤害强化道具按持有者最大生命造成的固定比例反伤。
type HeldItemRecoilDamageAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是承受道具反伤的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillID 是成功造成伤害并触发道具后效的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// ItemID 是本次提供伤害强化但不会消费的持有道具稳定 Identifier。
	ItemID Identifier `json:"itemId"`
	// Amount 是本次实际扣除的生命值。
	Amount uint32 `json:"amount"`
	// CurrentHP 是反伤结算后的当前生命值。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 heldItemRecoilDamageApplied。
func (event HeldItemRecoilDamageAppliedEvent) Kind() EventKind { return event.Type }

// HeldItemAirborneEndedEvent 记录气球类持有道具因真实本体伤害关闭空中效果。
//
// 道具本身仍由目标持有；事件只冻结运行态效果的关闭时点，并且必定排列在对应 DamageAppliedEvent 之后。
// 替身承伤、免疫、未命中和零伤害均不会生成该事件。
type HeldItemAirborneEndedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是失去道具空中效果的受击成员稳定引用。
	Target MemberRef `json:"target"`
	// ItemID 是仍由目标持有、但已关闭空中效果的道具稳定 Identifier。
	ItemID Identifier `json:"itemId"`
}

// Kind 返回 heldItemAirborneEnded。
func (event HeldItemAirborneEndedEvent) Kind() EventKind { return event.Type }

// HeldItemStatReactionConsumedEvent 记录能力阶级反应道具在全部阶级变化事件之后完成消费。
type HeldItemStatReactionConsumedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是事件载荷版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是事件所属回合。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是触发并消费道具的成员稳定引用。
	Target MemberRef `json:"target"`
	// ItemID 是本次被消费的道具稳定 Identifier。
	ItemID Identifier `json:"itemId"`
	// Reason 是稳定的规则原因标识。
	Reason string `json:"reason"`
}

// Kind 返回 heldItemStatReactionConsumed。
func (event HeldItemStatReactionConsumedEvent) Kind() EventKind { return event.Type }

// TerrainStartedEvent 记录技能已经建立或覆盖普通全场场地。
type TerrainStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是建立场地的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillPosition 是建立场地的技能槽位。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是建立场地的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Terrain 是已经写入全场环境的场地种类。
	Terrain TerrainKind `json:"terrain"`
	// TurnsRemaining 是场地建立时的剩余完整回合数。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// Kind 返回 terrainStarted。
func (event TerrainStartedEvent) Kind() EventKind { return event.Type }

// TerrainEndedEvent 记录普通场地在回合末自然耗尽。
type TerrainEndedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Terrain 是已经结束的场地种类。
	Terrain TerrainKind `json:"terrain"`
}

// Kind 返回 terrainEnded。
func (event TerrainEndedEvent) Kind() EventKind { return event.Type }

// TerrainHealingAppliedEvent 记录青草场地在回合末为一名接地成员回复生命的结果。
type TerrainHealingAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是实际获得青草场地回复的成员稳定引用。
	Target MemberRef `json:"target"`
	// Terrain 固定为本次产生回复的青草场地种类。
	Terrain TerrainKind `json:"terrain"`
	// Amount 是已经按目标缺失生命夹取的实际回复量。
	Amount uint32 `json:"amount"`
	// CurrentHP 是回复后的当前生命。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 terrainHealingApplied。
func (event TerrainHealingAppliedEvent) Kind() EventKind { return event.Type }

// ReflectStartedEvent 记录技能已经在使用者一方建立反射壁。
type ReflectStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是成功建立反射壁的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillPosition 是建立反射壁的技能槽位置。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是建立反射壁的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Side 是获得反射壁的使用者所属阵营位置。
	Side Side `json:"side"`
	// TurnsRemaining 是反射壁建立时声明的剩余完整回合数。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// Kind 返回 reflectStarted。
func (event ReflectStartedEvent) Kind() EventKind { return event.Type }

// ReflectEndedEvent 记录一方反射壁在回合末自然耗尽并从权威侧状态移除。
type ReflectEndedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Side 是反射壁已经结束的阵营位置。
	Side Side `json:"side"`
}

// Kind 返回 reflectEnded。
func (event ReflectEndedEvent) Kind() EventKind { return event.Type }

// LightScreenStartedEvent 记录技能已经在使用者一方建立光墙。
type LightScreenStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是成功建立光墙的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillPosition 是建立光墙的技能槽位置。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是建立光墙的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Side 是获得光墙的使用者所属阵营位置。
	Side Side `json:"side"`
	// TurnsRemaining 是光墙建立时声明的剩余完整回合数。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// Kind 返回 lightScreenStarted。
func (event LightScreenStartedEvent) Kind() EventKind { return event.Type }

// LightScreenEndedEvent 记录一方光墙在回合末自然耗尽并从权威侧状态移除。
type LightScreenEndedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Side 是光墙已经结束的阵营位置。
	Side Side `json:"side"`
}

// Kind 返回 lightScreenEnded。
func (event LightScreenEndedEvent) Kind() EventKind { return event.Type }

// AuroraVeilStartedEvent 记录技能已经在使用者一方建立极光幕。
type AuroraVeilStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是成功建立极光幕的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillPosition 是建立极光幕的技能槽位置。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是建立极光幕的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Side 是获得极光幕的使用者所属阵营位置。
	Side Side `json:"side"`
	// TurnsRemaining 是极光幕建立时声明的剩余完整回合数。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// Kind 返回 auroraVeilStarted。
func (event AuroraVeilStartedEvent) Kind() EventKind { return event.Type }

// AuroraVeilEndedEvent 记录一方极光幕在回合末自然耗尽并从权威侧状态移除。
type AuroraVeilEndedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Side 是极光幕已经结束的阵营位置。
	Side Side `json:"side"`
}

// Kind 返回 auroraVeilEnded。
func (event AuroraVeilEndedEvent) Kind() EventKind { return event.Type }

// SpikesDamageAppliedEvent 记录撒菱在接地成员换入后造成的实际生命伤害。
type SpikesDamageAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是实际换入并承受撒菱伤害的成员稳定引用。
	Target MemberRef `json:"target"`
	// Layers 是触发本次伤害时该方场地已有的撒菱层数。
	Layers uint8 `json:"layers"`
	// Amount 是已经按当前生命夹取的实际伤害。
	Amount uint32 `json:"amount"`
	// CurrentHP 是伤害后的当前生命。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 spikesDamageApplied。
func (event SpikesDamageAppliedEvent) Kind() EventKind { return event.Type }

// StealthRockDamageAppliedEvent 记录隐形岩在成员换入后按冻结岩石属性倍率造成的实际生命伤害。
type StealthRockDamageAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是实际换入并承受隐形岩伤害的成员稳定引用。
	Target MemberRef `json:"target"`
	// EffectivenessNumerator 是岩石属性对目标全部当前属性相乘后的冻结倍率分子。
	EffectivenessNumerator uint32 `json:"effectivenessNumerator"`
	// EffectivenessDenominator 是岩石属性对目标全部当前属性相乘后的冻结倍率分母。
	EffectivenessDenominator uint32 `json:"effectivenessDenominator"`
	// Amount 是已经按当前生命夹取的实际伤害。
	Amount uint32 `json:"amount"`
	// CurrentHP 是伤害后的当前生命。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 stealthRockDamageApplied。
func (event StealthRockDamageAppliedEvent) Kind() EventKind { return event.Type }

// ToxicSpikesAbsorbedEvent 记录接地毒属性成员换入后清除了己方场地全部毒菱层数。
type ToxicSpikesAbsorbedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是实际吸收毒菱的换入成员稳定引用。
	Target MemberRef `json:"target"`
	// Layers 是被该成员一次性清除的毒菱层数。
	Layers uint8 `json:"layers"`
}

// Kind 返回 toxicSpikesAbsorbed。
func (event ToxicSpikesAbsorbedEvent) Kind() EventKind { return event.Type }

// ToxicSpikesStatusAppliedEvent 记录毒菱在接地成员换入后成功施加的一项主要异常。
type ToxicSpikesStatusAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是实际获得主要异常的换入成员稳定引用。
	Target MemberRef `json:"target"`
	// Layers 是触发本次异常施加时该方场地已有的毒菱层数。
	Layers uint8 `json:"layers"`
	// Status 是毒菱已写入目标的普通中毒或剧毒状态。
	Status MajorStatus `json:"status"`
}

// Kind 返回 toxicSpikesStatusApplied。
func (event ToxicSpikesStatusAppliedEvent) Kind() EventKind { return event.Type }

// StickyWebSpeedLoweredEvent 记录黏黏网在接地成员换入后造成的实际速度能力阶级降低。
type StickyWebSpeedLoweredEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是实际受到黏黏网影响的换入成员稳定引用。
	Target MemberRef `json:"target"`
	// Delta 是经过 -6 边界夹取后的实际速度能力阶级变化。
	Delta int8 `json:"delta"`
	// CurrentStage 是速度降低后目标当前速度能力阶级。
	CurrentStage int8 `json:"currentStage"`
}

// Kind 返回 stickyWebSpeedLowered。
func (event StickyWebSpeedLoweredEvent) Kind() EventKind { return event.Type }

// SpikesLayerAddedEvent 记录技能已在目标一方场地成功增加一层撒菱。
type SpikesLayerAddedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是成功布置撒菱的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillPosition 是布置撒菱的技能槽位置。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是布置撒菱的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Side 是获得撒菱的目标成员所属阵营位置。
	Side Side `json:"side"`
	// Layers 是增加后该方场地已有的撒菱层数。
	Layers uint8 `json:"layers"`
}

// Kind 返回 spikesLayerAdded。
func (event SpikesLayerAddedEvent) Kind() EventKind { return event.Type }

// StealthRockStartedEvent 记录技能已在目标一方场地成功布置隐形岩。
type StealthRockStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是成功布置隐形岩的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillPosition 是布置隐形岩的技能槽位置。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是布置隐形岩的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Side 是获得隐形岩的目标成员所属阵营位置。
	Side Side `json:"side"`
}

// Kind 返回 stealthRockStarted。
func (event StealthRockStartedEvent) Kind() EventKind { return event.Type }

// ToxicSpikesLayerAddedEvent 记录技能已在目标一方场地成功增加一层毒菱。
type ToxicSpikesLayerAddedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是成功布置毒菱的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillPosition 是布置毒菱的技能槽位置。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是布置毒菱的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Side 是获得毒菱的目标成员所属阵营位置。
	Side Side `json:"side"`
	// Layers 是增加后该方场地已有的毒菱层数。
	Layers uint8 `json:"layers"`
}

// Kind 返回 toxicSpikesLayerAdded。
func (event ToxicSpikesLayerAddedEvent) Kind() EventKind { return event.Type }

// StickyWebStartedEvent 记录技能已在目标一方场地成功布置黏黏网。
type StickyWebStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是成功布置黏黏网的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillPosition 是布置黏黏网的技能槽位置。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是布置黏黏网的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Side 是获得黏黏网的目标成员所属阵营位置。
	Side Side `json:"side"`
}

// Kind 返回 stickyWebStarted。
func (event StickyWebStartedEvent) Kind() EventKind { return event.Type }

// RapidSpinHazardsClearedEvent 记录快速旋转已从使用者一方场地清除的全部入场危害。
type RapidSpinHazardsClearedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是成功执行快速旋转的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillPosition 是快速旋转的技能槽位置。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是快速旋转的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Side 是被清除入场危害的使用者所属阵营位置。
	Side Side `json:"side"`
	// ClearedSpikesLayers 是被清除前该方的撒菱层数。
	ClearedSpikesLayers uint8 `json:"clearedSpikesLayers"`
	// ClearedStealthRock 表示被清除前该方是否存在隐形岩。
	ClearedStealthRock bool `json:"clearedStealthRock"`
	// ClearedToxicSpikesLayers 是被清除前该方的毒菱层数。
	ClearedToxicSpikesLayers uint8 `json:"clearedToxicSpikesLayers"`
	// ClearedStickyWeb 表示被清除前该方是否存在黏黏网。
	ClearedStickyWeb bool `json:"clearedStickyWeb"`
}

// Kind 返回 rapidSpinHazardsCleared。
func (event RapidSpinHazardsClearedEvent) Kind() EventKind { return event.Type }

// DefogSideConditionsClearedEvent 记录清除浓雾已从目标一方清除的屏障和入场危害。
type DefogSideConditionsClearedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是成功执行清除浓雾的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillPosition 是清除浓雾的技能槽位置。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是清除浓雾的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Side 是被清除屏障和危害的目标成员所属阵营位置。
	Side Side `json:"side"`
	// ClearedReflect 表示目标方是否清除了反射壁。
	ClearedReflect bool `json:"clearedReflect"`
	// ClearedLightScreen 表示目标方是否清除了光墙。
	ClearedLightScreen bool `json:"clearedLightScreen"`
	// ClearedAuroraVeil 表示目标方是否清除了极光幕。
	ClearedAuroraVeil bool `json:"clearedAuroraVeil"`
	// ClearedSpikesLayers 是被清除前目标方的撒菱层数。
	ClearedSpikesLayers uint8 `json:"clearedSpikesLayers"`
	// ClearedStealthRock 表示目标方是否清除了隐形岩。
	ClearedStealthRock bool `json:"clearedStealthRock"`
	// ClearedToxicSpikesLayers 是被清除前目标方的毒菱层数。
	ClearedToxicSpikesLayers uint8 `json:"clearedToxicSpikesLayers"`
	// ClearedStickyWeb 表示目标方是否清除了黏黏网。
	ClearedStickyWeb bool `json:"clearedStickyWeb"`
}

// Kind 返回 defogSideConditionsCleared。
func (event DefogSideConditionsClearedEvent) Kind() EventKind { return event.Type }

// DefogTerrainClearedEvent 记录清除浓雾已清除的普通全场场地。
type DefogTerrainClearedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是成功执行清除浓雾的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillPosition 是清除浓雾的技能槽位置。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是清除浓雾的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Terrain 是被清除的普通场地稳定种类。
	Terrain TerrainKind `json:"terrain"`
}

// Kind 返回 defogTerrainCleared。
func (event DefogTerrainClearedEvent) Kind() EventKind { return event.Type }

// AbilitySideDamageReductionsClearedEvent 记录入场特性从一方阵营清除的全部减伤屏障。
type AbilitySideDamageReductionsClearedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是触发清除特性的当前上场成员。
	Actor MemberRef `json:"actor"`
	// Side 是被清除减伤屏障的阵营位置。
	Side Side `json:"side"`
	// ClearedReflect 表示该方原本存在且已被清除的反射壁。
	ClearedReflect bool `json:"clearedReflect"`
	// ClearedLightScreen 表示该方原本存在且已被清除的光墙。
	ClearedLightScreen bool `json:"clearedLightScreen"`
	// ClearedAuroraVeil 表示该方原本存在且已被清除的极光幕。
	ClearedAuroraVeil bool `json:"clearedAuroraVeil"`
}

// Kind 返回 abilitySideDamageReductionsCleared。
func (event AbilitySideDamageReductionsClearedEvent) Kind() EventKind { return event.Type }

// AbilityCopiedEvent 记录入场成员已经复制一名对手的当前特性。
//
// 该事件只记录本场快照内已经存在的稳定特性身份与成员引用。复制结果在 State 中同时包含对应的强类型规则，
// 重放不应也不能通过实时资料查询补全效果。
type AbilityCopiedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是复制完成后持有新特性的当前上场成员。
	Actor MemberRef `json:"actor"`
	// Source 是被复制特性的当前上场对手。
	Source MemberRef `json:"source"`
	// PreviousAbilityID 是 Actor 复制前持有的稳定特性 Identifier；空字符串表示复制前没有特性。
	PreviousAbilityID Identifier `json:"previousAbilityId,omitempty"`
	// AbilityID 是 Actor 复制后持有的稳定特性 Identifier。
	AbilityID Identifier `json:"abilityId"`
}

// Kind 返回 abilityCopied。
func (event AbilityCopiedEvent) Kind() EventKind { return event.Type }

// OpponentHeldItemRevealedEvent 记录入场特性公开的一名对手持有道具。
//
// 该事件只在目标当前存活且确实持有道具时产生；没有道具的目标不会被编码为空值事件，避免客户端把“无道具”
// 与“尚未公开”混淆。公开本身不消耗道具、不改变目标状态。
type OpponentHeldItemRevealedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是触发公开效果的当前上场成员。
	Actor MemberRef `json:"actor"`
	// Target 是被公开持有道具的当前上场对手。
	Target MemberRef `json:"target"`
	// ItemID 是目标持有道具的稳定 Identifier 文本。
	ItemID Identifier `json:"itemId"`
}

// Kind 返回 opponentHeldItemRevealed。
func (event OpponentHeldItemRevealedEvent) Kind() EventKind { return event.Type }

// OpponentSkillRevealedEvent 记录入场特性公开的一名对手最高基础威力技能。
//
// 目标必须是当前存活上场成员，技能选择只读取冻结的 SkillSnapshot；并列时由 SkillID 的稳定倒序打破，
// 所以数据库读取顺序和客户端数组顺序都不会影响公开结果。
type OpponentSkillRevealedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号；初始入场阶段固定为 0。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是触发公开效果的当前上场成员。
	Actor MemberRef `json:"actor"`
	// Target 是拥有被公开技能的当前上场对手。
	Target MemberRef `json:"target"`
	// SkillID 是目标所选最高基础威力技能的稳定 Identifier 文本。
	SkillID Identifier `json:"skillId"`
}

// Kind 返回 opponentSkillRevealed。
func (event OpponentSkillRevealedEvent) Kind() EventKind { return event.Type }

// ParticipantTransformedEvent 记录一名成员已复制一名对手的种类、基础能力、属性、技能与特性规则。
//
// 该事件不透露变身前快照的内部存储细节。目标的完整画像和原始画像均已冻结在权威状态中，客户端只需根据
// Actor、Target 与 CopiedCreatureID 更新可见战斗表示；成员离场时引擎会无事件地还原原始画像。
type ParticipantTransformedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号；初始入场阶段固定为 0。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是执行变身并持有变身前快照的成员。
	Actor MemberRef `json:"actor"`
	// Target 是提供当前战斗画像的存活上场对手。
	Target MemberRef `json:"target"`
	// CopiedCreatureID 是 Actor 变身后显示和结算所使用的种类稳定 Identifier。
	CopiedCreatureID Identifier `json:"copiedCreatureId"`
}

// Kind 返回 participantTransformed。
func (event ParticipantTransformedEvent) Kind() EventKind { return event.Type }

// DangerousOpponentSkillDetectedEvent 记录入场特性侦测到一项对自身危险的对手技能。
//
// 事件只公开稳定成员引用和技能 Identifier，不改变目标技能的 PP、伤害或任何运行态。
type DangerousOpponentSkillDetectedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号；初始入场阶段固定为 0。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是执行危险技能侦测的成员。
	Actor MemberRef `json:"actor"`
	// Target 是拥有被侦测技能的存活上场对手。
	Target MemberRef `json:"target"`
	// SkillID 是被侦测技能的稳定 Identifier。
	SkillID Identifier `json:"skillId"`
}

// Kind 返回 dangerousOpponentSkillDetected。
func (event DangerousOpponentSkillDetectedEvent) Kind() EventKind { return event.Type }

// TailwindStartedEvent 记录技能已经在使用者一方建立顺风。
type TailwindStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是成功建立顺风的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillPosition 是建立顺风的技能槽位置。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是建立顺风的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Side 是获得顺风的使用者所属阵营位置。
	Side Side `json:"side"`
	// TurnsRemaining 是顺风建立时声明的剩余完整回合数。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// Kind 返回 tailwindStarted。
func (event TailwindStartedEvent) Kind() EventKind { return event.Type }

// TailwindEndedEvent 记录一方顺风在回合末自然耗尽并从权威侧状态移除。
type TailwindEndedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Side 是顺风已经结束的阵营位置。
	Side Side `json:"side"`
}

// Kind 返回 tailwindEnded。
func (event TailwindEndedEvent) Kind() EventKind { return event.Type }

// LeechSeedPlantedEvent 记录技能已经在目标本体写入寄生种子及其来源场上槽位。
//
// SourceSlot 不保存种下种子的稳定成员位置。来源成员换下后，该槽位的替换成员会在回合末获得回复；目标离场时
// 引擎会清除种子，后备成员不会继承本事件表达的持续状态。
type LeechSeedPlantedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是成功种下种子的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// Target 是已经获得寄生种子的实际目标成员稳定引用。
	Target MemberRef `json:"target"`
	// SourceSlot 是种下种子时使用者所处的场上槽位，也是后续回复的稳定目标槽位。
	SourceSlot SlotRef `json:"sourceSlot"`
	// SkillPosition 是种下种子的技能槽位置。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是种下种子的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
}

// Kind 返回 leechSeedPlanted。
func (event LeechSeedPlantedEvent) Kind() EventKind {
	return event.Type
}

// LeechSeedDamageAppliedEvent 记录寄生种子在回合末对目标产生的生命抽取。
type LeechSeedDamageAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是承受寄生种子生命抽取的成员稳定引用。
	Target MemberRef `json:"target"`
	// SourceSlot 是本回合接收回复的来源场上槽位。
	SourceSlot SlotRef `json:"sourceSlot"`
	// Amount 是本次实际从目标扣除的生命值，已经按目标当前生命夹取。
	Amount uint32 `json:"amount"`
	// CurrentHP 是扣除后目标的当前生命值。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 leechSeedDamageApplied。
func (event LeechSeedDamageAppliedEvent) Kind() EventKind {
	return event.Type
}

// LeechSeedHealingAppliedEvent 记录寄生种子将本回合抽取量回复给来源槽位当前成员的结果。
type LeechSeedHealingAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是本回合被抽取生命的寄生种子目标。
	Target MemberRef `json:"target"`
	// Recipient 是来源槽位当前实际获得回复的成员稳定引用。
	Recipient MemberRef `json:"recipient"`
	// Amount 是本次实际回复量，已经按接收者缺失生命夹取。
	Amount uint32 `json:"amount"`
	// CurrentHP 是回复后接收者的当前生命值。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 leechSeedHealingApplied。
func (event LeechSeedHealingAppliedEvent) Kind() EventKind {
	return event.Type
}

// ParticipantSwitchedEvent 记录一个场上槽位完成成员替换的事实。
type ParticipantSwitchedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Slot 是发生成员替换的稳定场上槽位。
	Slot SlotRef `json:"slot"`
	// PreviousMember 是已经离开场上槽位的成员稳定引用。
	PreviousMember MemberRef `json:"previousMember"`
	// NextMember 是已经进入场上槽位的成员稳定引用。
	NextMember MemberRef `json:"nextMember"`
	// Forced 表示本次替换是否由倒下补位或战斗效果强制触发。
	Forced bool `json:"forced"`
}

// Kind 返回 participantSwitched。
func (event ParticipantSwitchedEvent) Kind() EventKind {
	return event.Type
}

// ForcedTargetSwitchSelectedEvent 记录一项技能已确定目标强制换人的后备成员。
//
// 该事件不代表换入已经完成：消费者必须继续读取其后的 ParticipantSwitchedEvent 及完整入场生命周期事件，
// 才能得到危害、入场特性、天气与形态同步后的最终状态。
type ForcedTargetSwitchSelectedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是使用强制换人技能的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// TargetSlot 是将被替换的场上槽位。
	TargetSlot SlotRef `json:"targetSlot"`
	// Target 是替换发生前该槽位中目标成员的稳定引用。
	Target MemberRef `json:"target"`
	// SkillPosition 是触发规则的技能槽位置。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是触发规则的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Candidates 按成员位置升序保存所有健康且未上场的同侧后备成员。
	// 只有候选数大于一时，RandomTrace 才会为这次选择记录一项随机消费。
	Candidates []MemberRef `json:"candidates"`
	// SelectedMember 是本次实际进入 TargetSlot 的后备成员稳定引用。
	SelectedMember MemberRef `json:"selectedMember"`
}

// Kind 返回 forcedTargetSwitchSelected。
func (event ForcedTargetSwitchSelectedEvent) Kind() EventKind {
	return event.Type
}

// AbilityForcedSwitchSelectedEvent 记录半血跨越特性已确定持有成员的强制换人后备成员。
//
// 该事件不代表换入已经完成：消费者必须继续读取其后的 ParticipantSwitchedEvent 及完整入场生命周期事件，
// 才能得到危害、入场特性、天气与形态同步后的最终状态。
type AbilityForcedSwitchSelectedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Source 是触发半血跨越特性的持有成员稳定引用。
	Source MemberRef `json:"source"`
	// TargetSlot 是被替换成员当前占用的场上槽位。
	TargetSlot SlotRef `json:"targetSlot"`
	// Candidates 按成员稳定位置升序列出全部健康后备成员。
	Candidates []MemberRef `json:"candidates"`
	// SelectedMember 是最终实际替换进该场上槽位的后备成员。
	SelectedMember MemberRef `json:"selectedMember"`
}

// Kind 返回 abilityForcedSwitchSelected。
func (event AbilityForcedSwitchSelectedEvent) Kind() EventKind {
	return event.Type
}

// ItemForcedSwitchSelectedEvent 记录一次性持有道具已选中一名成员进行强制换人。
//
// 它不代表换入已经完成：消费者必须继续读取其后的 ParticipantSwitchedEvent 及完整入场生命周期事件，才能得到
// 危害、入场特性、天气与形态同步后的最终状态。只有候选存在且换人真正开始时才产生本事件并消耗 Source 的道具。
type ItemForcedSwitchSelectedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Source 是持有并消耗本道具的成员稳定引用。
	Source MemberRef `json:"source"`
	// Target 是实际被强制换下的成员稳定引用；受伤自换和能力下降自换时它与 Source 相同。
	Target MemberRef `json:"target"`
	// ItemID 是被消耗道具的稳定 Identifier 文本。
	ItemID Identifier `json:"itemId"`
	// Candidates 是按成员位置升序排列的全部健康后备候选。
	Candidates []MemberRef `json:"candidates"`
	// SelectedMember 是已经选中的实际换入成员。
	SelectedMember MemberRef `json:"selectedMember"`
}

// Kind 返回 itemForcedSwitchSelected。
func (event ItemForcedSwitchSelectedEvent) Kind() EventKind {
	return event.Type
}

// MajorStatusAppliedEvent 记录一项主要异常状态成功写入成员的事实。
type MajorStatusAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是触发本次主要异常效果的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// Target 是实际获得主要异常状态的成员稳定引用。
	Target MemberRef `json:"target"`
	// Status 是已经写入目标的主要异常状态。
	Status MajorStatus `json:"status"`
}

// Kind 返回 majorStatusApplied。
func (event MajorStatusAppliedEvent) Kind() EventKind {
	return event.Type
}

// MajorStatusClearedEvent 记录成员的一项主要异常状态已经解除。
type MajorStatusClearedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是主要异常被解除的成员稳定引用。
	Target MemberRef `json:"target"`
	// Status 是本次已经解除的主要异常状态。
	Status MajorStatus `json:"status"`
}

// Kind 返回 majorStatusCleared。
func (event MajorStatusClearedEvent) Kind() EventKind {
	return event.Type
}

// SwitchOutHealingAppliedEvent 记录成功离场特性为自身产生的一段实际回复。
//
// 它与技能、天气、场地和入场同伴回复保持独立事件种类，使重放和展示层能准确保留“先回复再离场”的生命周期。
type SwitchOutHealingAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Member 是执行成功离场并获得回复的成员稳定引用。
	Member MemberRef `json:"member"`
	// Amount 是本次实际写入成员当前生命的回复量。
	Amount uint32 `json:"amount"`
	// CurrentHP 是回复完成后的成员当前生命值。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 switchOutHealingApplied。
func (event SwitchOutHealingAppliedEvent) Kind() EventKind {
	return event.Type
}

// MajorStatusBlockReason 是主要异常没有写入目标的稳定规则原因。
type MajorStatusBlockReason string

const (
	// MajorStatusBlockReasonExistingStatus 表示目标已经持有另一项主要异常。
	MajorStatusBlockReasonExistingStatus MajorStatusBlockReason = "existingStatus"
	// MajorStatusBlockReasonElementImmunity 表示目标当前属性免疫该主要异常。
	MajorStatusBlockReasonElementImmunity MajorStatusBlockReason = "elementImmunity"
	// MajorStatusBlockReasonTerrainImmunity 表示接地目标被当前普通场地阻止获得该主要异常。
	MajorStatusBlockReasonTerrainImmunity MajorStatusBlockReason = "terrainImmunity"
)

// MajorStatusBlockedEvent 记录一次主要异常效果被明确规则阻止的事实。
type MajorStatusBlockedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是触发本次主要异常效果的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// Target 是拒绝写入主要异常的成员稳定引用。
	Target MemberRef `json:"target"`
	// Status 是本次尝试施加的主要异常状态。
	Status MajorStatus `json:"status"`
	// Reason 是 existingStatus 或 elementImmunity 等稳定原因。
	Reason MajorStatusBlockReason `json:"reason"`
}

// Kind 返回 majorStatusBlocked。
func (event MajorStatusBlockedEvent) Kind() EventKind {
	return event.Type
}

// StatStageChangedEvent 记录技能使目标能力阶级发生的实际变化。
type StatStageChangedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是触发能力阶级效果的技能使用者。
	Actor MemberRef `json:"actor"`
	// Target 是实际发生能力阶级变化的成员。
	Target MemberRef `json:"target"`
	// Stat 是本次发生变化的稳定能力项。
	Stat Stat `json:"stat"`
	// Delta 是经过 -6 至 6 边界夹取后的实际变化量。
	Delta int8 `json:"delta"`
	// CurrentStage 是效果完成后目标该能力的当前阶级。
	CurrentStage int8 `json:"currentStage"`
}

// Kind 返回 statStageChanged。
func (event StatStageChangedEvent) Kind() EventKind {
	return event.Type
}

// MajorStatusDamageAppliedEvent 记录主要异常在回合末产生的一段实际伤害。
type MajorStatusDamageAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是实际承受主要异常伤害的成员稳定引用。
	Target MemberRef `json:"target"`
	// Status 是产生本段伤害的主要异常状态。
	Status MajorStatus `json:"status"`
	// Amount 是本段实际扣除的生命值，不包含溢出伤害。
	Amount uint32 `json:"amount"`
	// CurrentHP 是本段伤害写入后目标的剩余生命值。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 majorStatusDamageApplied。
func (event MajorStatusDamageAppliedEvent) Kind() EventKind {
	return event.Type
}

// DamageAppliedEvent 记录一段技能伤害写入目标生命值的结果。
type DamageAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是造成本段伤害的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// Target 是实际承受伤害的成员稳定引用。
	Target MemberRef `json:"target"`
	// SkillPosition 是伤害来源技能在行动者技能列表中的稳定槽位。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是伤害来源技能在实时资料中的稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Amount 是本段实际扣除的生命值，不包含超过目标剩余生命值的溢出伤害。
	Amount uint32 `json:"amount"`
	// CurrentHP 是本段伤害写入后目标的剩余生命值。
	CurrentHP uint32 `json:"currentHp"`
	// CriticalHit 表示本段普通公式伤害是否按击中要害结算。
	CriticalHit bool `json:"criticalHit"`
	// RandomPercent 是本段普通伤害公式使用的 85 至 100 随机百分比。
	RandomPercent uint8 `json:"randomPercent"`
}

// Kind 返回 damageApplied。
func (event DamageAppliedEvent) Kind() EventKind {
	return event.Type
}

// FatalDamageSurvivedEvent 记录特性或持有道具在满生命成员受到致命对手技能伤害时保留 1 HP 的事实。
//
// 它紧跟同一段 DamageAppliedEvent，保存被特性阻止的伤害与来源，使重放和展示无需从剩余生命反推保命。
type FatalDamageSurvivedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是本段致命技能伤害的来源成员稳定引用。
	Actor MemberRef `json:"actor"`
	// Target 是因保命规则保留 1 HP 的满生命成员稳定引用。
	Target MemberRef `json:"target"`
	// SkillPosition 是伤害来源技能在行动者技能列表中的稳定槽位。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是伤害来源技能在实时资料中的稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// SourceAbilityID 是实际提供本次保命规则的目标特性稳定 Identifier；道具来源时为空。
	SourceAbilityID Identifier `json:"sourceAbilityId,omitempty"`
	// SourceItemID 是实际提供本次保命规则的目标持有道具稳定 Identifier；特性来源时为空。
	SourceItemID Identifier `json:"sourceItemId,omitempty"`
	// IncomingDamage 是防守规则介入前本段技能原本会写入的伤害量。
	IncomingDamage uint32 `json:"incomingDamage"`
	// PreventedDamage 是本次保命规则实际阻止的生命损失量。
	PreventedDamage uint32 `json:"preventedDamage"`
	// CurrentHP 是同一段伤害写入后目标保留的生命值，当前固定为 1。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 fatalDamageSurvived。
func (event FatalDamageSurvivedEvent) Kind() EventKind {
	return event.Type
}

// SubstituteStartedEvent 记录使用者已支付本体生命并建立独立替身生命值的事实。
//
// HPCost 与 SubstituteHP 在成功建立时相同。该事件不属于普通伤害，因而不会错误触发基于造成伤害的
// 吸取、反作用或倒下流程。
type SubstituteStartedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是建立替身的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillPosition 是建立替身的技能在使用者技能列表中的稳定槽位。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是建立替身的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// HPCost 是从使用者本体实际支付的生命值。
	HPCost uint32 `json:"hpCost"`
	// SubstituteHP 是替身建立完成后的初始独立生命值。
	SubstituteHP uint32 `json:"substituteHp"`
}

// Kind 返回 substituteStarted。
func (event SubstituteStartedEvent) Kind() EventKind {
	return event.Type
}

// SubstituteDamageAppliedEvent 记录对方一段技能伤害由替身而非目标本体承受的结果。
type SubstituteDamageAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是造成该段伤害的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// Target 是拥有并承受替身伤害的成员稳定引用。
	Target MemberRef `json:"target"`
	// SkillPosition 是伤害来源技能在行动者技能列表中的稳定槽位。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是伤害来源技能的稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Amount 是本段实际扣除的替身生命值，不包含超过替身剩余生命值的溢出伤害。
	Amount uint32 `json:"amount"`
	// SubstituteHPRemaining 是本段扣除后替身剩余的独立生命值。
	SubstituteHPRemaining uint32 `json:"substituteHpRemaining"`
}

// Kind 返回 substituteDamageApplied。
func (event SubstituteDamageAppliedEvent) Kind() EventKind {
	return event.Type
}

// SubstituteBrokenEvent 记录替身因本次伤害耗尽并立即失效的事实。
type SubstituteBrokenEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是造成替身破裂的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// Target 是替身已经破裂的成员稳定引用。
	Target MemberRef `json:"target"`
	// SkillID 是耗尽该替身的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
}

// Kind 返回 substituteBroken。
func (event SubstituteBrokenEvent) Kind() EventKind {
	return event.Type
}

// HPAveragedBySkillEvent 记录一项技能已完成双方当前生命的平均重分配。
//
// 该事件不表示伤害或回复：它不会成为吸取、反作用、倒下触发器或替身承伤的输入，只用于让重放准确记录
// 本次生命值重设的来源与最终结果。
type HPAveragedBySkillEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是发动当前生命平均规则的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// Target 是参与当前生命平均规则的目标成员稳定引用。
	Target MemberRef `json:"target"`
	// SkillPosition 是执行该规则的技能在使用者技能列表中的稳定槽位。
	SkillPosition SkillPosition `json:"skillPosition"`
	// SkillID 是执行该规则的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// ActorCurrentHP 是写入平均值后使用者的当前生命。
	ActorCurrentHP uint32 `json:"actorCurrentHp"`
	// TargetCurrentHP 是写入平均值后目标的当前生命。
	TargetCurrentHP uint32 `json:"targetCurrentHp"`
}

// Kind 返回 hpAveragedBySkill。
func (event HPAveragedBySkillEvent) Kind() EventKind {
	return event.Type
}

// SkillHealingSource 是技能回复事件使用的稳定生命来源。
type SkillHealingSource string

const (
	// SkillHealingSourceDrain 表示回复量以本次对目标造成的实际伤害为基数。
	SkillHealingSourceDrain SkillHealingSource = "drain"
	// SkillHealingSourceFixed 表示回复量以使用者自身最大生命值为基数。
	SkillHealingSourceFixed SkillHealingSource = "fixed"
	// SkillHealingSourceTargetMaximumHP 表示回复量以技能实际目标的最大生命值为基数。
	SkillHealingSourceTargetMaximumHP SkillHealingSource = "targetMaximumHP"
	// SkillHealingSourceHeldItemDamageDealt 表示回复量来自持有道具按本次实际伤害回算。
	SkillHealingSourceHeldItemDamageDealt SkillHealingSource = "heldItemDamageDealt"
)

// SkillHealingAppliedEvent 记录一次技能成功后实际回复某个成员生命值的事实。
type SkillHealingAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是实际获得回复的成员稳定引用；它可能是技能使用者，也可能是技能目标。
	Actor MemberRef `json:"actor"`
	// SkillID 是产生本次回复的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Source 表示回复量来自本次伤害、使用者最大生命值、目标最大生命值或道具效果。
	Source SkillHealingSource `json:"source"`
	// Amount 是本次实际回复量，已按缺失生命值夹取。
	Amount uint32 `json:"amount"`
	// CurrentHP 是回复写入后的受益成员当前生命值。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 skillHealingApplied。
func (event SkillHealingAppliedEvent) Kind() EventKind {
	return event.Type
}

// SkillRecoilDamageAppliedEvent 记录一次技能成功后实际扣除使用者生命值的事实。
type SkillRecoilDamageAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是承受本次反作用或技能代价的使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillID 是产生本次自身伤害的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Amount 是本次实际扣除生命值，已按使用者当前生命值夹取。
	Amount uint32 `json:"amount"`
	// SourceDamageAmount 是本次以实际命中伤害或最大生命值计算时所用的原始基数。
	SourceDamageAmount uint32 `json:"sourceDamageAmount"`
	// CurrentHP 是自身伤害写入后的使用者当前生命值。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 skillRecoilDamageApplied。
func (event SkillRecoilDamageAppliedEvent) Kind() EventKind {
	return event.Type
}

// ContactDamageAppliedEvent 记录目标特性因本次有效接触本体伤害而向攻击者施加的实际反制伤害。
//
// 它与技能反作用独立建模：伤害基数来自攻击者最大生命而非本段实际命中伤害，且由受击方的特性提供，
// 因此回放和展示不能复用 skillRecoilDamageApplied 事件或从生命变化反推来源。
type ContactDamageAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Source 是提供接触反制特性的受击方成员稳定引用。
	Source MemberRef `json:"source"`
	// Target 是实际承受反制伤害的原技能攻击者稳定引用。
	Target MemberRef `json:"target"`
	// SkillID 是造成有效接触本体伤害的原技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// SourceAbilityID 是提供本次反制规则的受击方特性稳定 Identifier。
	SourceAbilityID Identifier `json:"sourceAbilityId,omitempty"`
	// SourceItemID 是提供本次反制规则的受击方持有道具稳定 Identifier；特性来源时保持为空。
	SourceItemID Identifier `json:"sourceItemId,omitempty"`
	// Denominator 是攻击者最大生命按比例计算反制伤害时使用的冻结分母。
	Denominator uint16 `json:"denominator"`
	// Amount 是本次实际扣除攻击者生命值，已按其当前生命夹取。
	Amount uint32 `json:"amount"`
	// CurrentHP 是反制伤害写入后的攻击者当前生命值。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 contactDamageApplied。
func (event ContactDamageAppliedEvent) Kind() EventKind {
	return event.Type
}

// HeldItemTransferredEvent 记录接触规则在目标实际受到本体伤害后完成的一次道具所有权转移。
//
// 它只陈述已完成的运行态所有权变化；具体触发条件由目标持有道具的冻结规则决定，因此回放消费者不必查询
// 实时 Item Metadata，也不能根据道具显示名称猜测该事件。
type HeldItemTransferredEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// From 是失去当前持有道具的受击方成员稳定引用。
	From MemberRef `json:"from"`
	// To 是获得道具的无道具攻击方成员稳定引用。
	To MemberRef `json:"to"`
	// ItemID 是实际转移的当前持有道具稳定 Identifier。
	ItemID Identifier `json:"itemId"`
	// SkillID 是造成有效接触本体伤害并触发本次转移的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
}

// Kind 返回 heldItemTransferred。
func (event HeldItemTransferredEvent) Kind() EventKind {
	return event.Type
}

// SkillChargeSkippedByItemEvent 记录携带道具已令一次蓄力技能直接进入本回合结算。
type SkillChargeSkippedByItemEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是消耗道具并直接继续结算技能的成员稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillID 是跳过蓄力等待的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// ItemID 是已被消费的一次性持有道具稳定 Identifier。
	ItemID Identifier `json:"itemId"`
}

// Kind 返回 skillChargeSkippedByItem。
func (event SkillChargeSkippedByItemEvent) Kind() EventKind {
	return event.Type
}

// HeldItemParalysisCuredEvent 记录一次性持有道具已经解除成员刚成功获得的麻痹。
//
// 该事件只在麻痹先被真实写入后产生，因而事件流可保留“异常施加、道具消费、异常解除”的完整生命周期。
type HeldItemParalysisCuredEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Member 是被道具解除麻痹并失去道具的成员稳定引用。
	Member MemberRef `json:"member"`
	// ItemID 是本次已经消费的持有道具稳定 Identifier 文本。
	ItemID Identifier `json:"itemId"`
	// Status 固定为 paralysis，显式保留可审计的异常种类，避免消费者从事件名猜测语义。
	Status MajorStatus `json:"status"`
}

// Kind 返回 heldItemParalysisCured。
func (event HeldItemParalysisCuredEvent) Kind() EventKind {
	return event.Type
}

// HeldItemSleepCuredEvent 记录一次性持有道具已经解除成员刚成功获得的睡眠。
//
// 该事件只在睡眠先被真实写入、睡眠时长随机轨迹已经消费后产生，事件流因此可以完整重放睡眠与道具的先后关系。
type HeldItemSleepCuredEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Member 是被道具解除睡眠并失去道具的成员稳定引用。
	Member MemberRef `json:"member"`
	// ItemID 是本次已经消费的持有道具稳定 Identifier 文本。
	ItemID Identifier `json:"itemId"`
	// Status 固定为 sleep，显式保留可审计的异常种类，避免消费者从事件名猜测语义。
	Status MajorStatus `json:"status"`
}

// Kind 返回 heldItemSleepCured。
func (event HeldItemSleepCuredEvent) Kind() EventKind {
	return event.Type
}

// HeldItemPoisonCuredEvent 记录一次性持有道具已经解除成员刚成功获得的普通中毒或剧毒。
// 该事件在对应异常写入后立即产生，并显式保留状态种类，使剧毒计数的清理在回放中可审计。
type HeldItemPoisonCuredEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Member 是被道具解除中毒并失去道具的成员稳定引用。
	Member MemberRef `json:"member"`
	// ItemID 是本次已经消费的持有道具稳定 Identifier 文本。
	ItemID Identifier `json:"itemId"`
	// Status 是被解除的 poison 或 badPoison，不能由消费者从事件名推断。
	Status MajorStatus `json:"status"`
}

// Kind 返回 heldItemPoisonCured。
func (event HeldItemPoisonCuredEvent) Kind() EventKind { return event.Type }

// HeldItemBurnCuredEvent 记录一次性持有道具已经解除成员刚成功获得的灼伤。
// 该事件在对应异常写入后立即产生，使道具消费与灼伤清理可以由回放逐项审计。
type HeldItemBurnCuredEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Member 是被道具解除灼伤并失去道具的成员稳定引用。
	Member MemberRef `json:"member"`
	// ItemID 是本次已经消费的持有道具稳定 Identifier 文本。
	ItemID Identifier `json:"itemId"`
	// Status 固定为 burn，显式保留可审计的被解除主要异常种类。
	Status MajorStatus `json:"status"`
}

// Kind 返回 heldItemBurnCured。
func (event HeldItemBurnCuredEvent) Kind() EventKind { return event.Type }

// HeldItemFreezeCuredEvent 记录一次性持有道具已经解除成员刚成功获得的冰冻。
// 该事件在对应异常写入后立即产生，使道具消费与冰冻清理可以由回放逐项审计。
type HeldItemFreezeCuredEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Member 是被道具解除冰冻并失去道具的成员稳定引用。
	Member MemberRef `json:"member"`
	// ItemID 是本次已经消费的持有道具稳定 Identifier 文本。
	ItemID Identifier `json:"itemId"`
	// Status 固定为 freeze，显式保留可审计的被解除主要异常种类。
	Status MajorStatus `json:"status"`
}

// Kind 返回 heldItemFreezeCured。
func (event HeldItemFreezeCuredEvent) Kind() EventKind { return event.Type }

// HeldItemAllMajorStatusCuredEvent 记录一次性持有道具已经解除成员刚成功获得的任一种主要异常。
// 该事件显式保留状态种类，使睡眠和剧毒附属计数的清理可以与对应异常写入一起回放审计。
type HeldItemAllMajorStatusCuredEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Member 是被道具解除主要异常并失去道具的成员稳定引用。
	Member MemberRef `json:"member"`
	// ItemID 是本次已经消费的持有道具稳定 Identifier 文本。
	ItemID Identifier `json:"itemId"`
	// Status 是本次被解除的实际主要异常，属于规则资料声明的六种可治疗状态之一。
	Status MajorStatus `json:"status"`
}

// Kind 返回 heldItemAllMajorStatusCured。
func (event HeldItemAllMajorStatusCuredEvent) Kind() EventKind { return event.Type }

// HeldItemConfusionCuredEvent 记录一次性持有道具已经解除成员刚成功获得的混乱。
// 该事件在易变状态写入后立即产生，使混乱持续回合的清理与道具消费可由回放逐项审计。
type HeldItemConfusionCuredEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Member 是被道具解除混乱并失去道具的成员稳定引用。
	Member MemberRef `json:"member"`
	// ItemID 是本次已经消费的持有道具稳定 Identifier 文本。
	ItemID Identifier `json:"itemId"`
	// Status 固定为 confusion，显式保留可审计的被解除易变状态种类。
	Status VolatileStatus `json:"status"`
}

// Kind 返回 heldItemConfusionCured。
func (event HeldItemConfusionCuredEvent) Kind() EventKind { return event.Type }

// SkillSelfSacrificeDamageAppliedEvent 记录直接伤害技能在目标受击后令使用者失去全部当前生命的代价。
//
// 它与按伤害量计算的反作用分开建模，避免重放消费者误把自我牺牲当作可被反作用免疫规则阻止的伤害。
type SkillSelfSacrificeDamageAppliedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Actor 是支付全部当前生命代价的技能使用者稳定引用。
	Actor MemberRef `json:"actor"`
	// SkillID 是导致本次自我牺牲的技能稳定 Identifier。
	SkillID Identifier `json:"skillId"`
	// Amount 是本次实际扣除的生命值，等于使用者写入前的当前生命。
	Amount uint32 `json:"amount"`
	// CurrentHP 是生命代价写入后的当前生命，固定为 0。
	CurrentHP uint32 `json:"currentHp"`
}

// Kind 返回 skillSelfSacrificeDamageApplied。
func (event SkillSelfSacrificeDamageAppliedEvent) Kind() EventKind {
	return event.Type
}

// FaintCause 是成员倒下事件使用的稳定伤害来源种类。
type FaintCause string

const (
	// FaintCauseSkillDamage 表示成员因技能直接伤害倒下。
	FaintCauseSkillDamage FaintCause = "skillDamage"
	// FaintCauseMajorStatusDamage 表示成员因主要异常的回合末伤害倒下。
	FaintCauseMajorStatusDamage FaintCause = "majorStatusDamage"
	// FaintCauseSkillRecoil 表示成员因技能反作用或技能生命代价倒下。
	FaintCauseSkillRecoil FaintCause = "skillRecoil"
	// FaintCauseContactDamage 表示成员因目标特性在有效接触后施加的反制伤害倒下。
	FaintCauseContactDamage FaintCause = "contactDamage"
	// FaintCauseSkillSelfSacrifice 表示成员因直接伤害技能要求的自我牺牲倒下。
	FaintCauseSkillSelfSacrifice FaintCause = "skillSelfSacrifice"
	// FaintCauseVolatileStatusDamage 表示成员因束缚、混乱等易变状态的伤害倒下。
	FaintCauseVolatileStatusDamage FaintCause = "volatileStatusDamage"
	// FaintCauseLeechSeed 表示成员因寄生种子在回合末的生命抽取倒下。
	FaintCauseLeechSeed FaintCause = "leechSeed"
	// FaintCauseWeather 表示成员因回合末天气伤害倒下。
	FaintCauseWeather FaintCause = "weather"
	// FaintCauseHeldItemDamage 表示成员因持有道具在回合末造成的间接伤害倒下。
	FaintCauseHeldItemDamage FaintCause = "heldItemDamage"
	// FaintCauseEntryHazard 表示成员因换入时触发的入场危害倒下。
	FaintCauseEntryHazard FaintCause = "entryHazard"
)

// ParticipantFaintedEvent 记录一名成员生命值首次降为 0 的事实。
type ParticipantFaintedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// Target 是已经倒下的成员稳定引用。
	Target MemberRef `json:"target"`
	// Cause 是 skillDamage 或 majorStatusDamage 等稳定来源种类。
	Cause FaintCause `json:"cause"`
	// SkillID 是导致成员倒下的技能稳定 Identifier；非技能伤害时为空。
	SkillID Identifier `json:"skillId,omitempty"`
	// MajorStatus 是导致成员倒下的主要异常；非异常伤害时为空。
	MajorStatus MajorStatus `json:"majorStatus,omitempty"`
	// VolatileStatus 是导致成员倒下的易变状态；非易变状态伤害时为空。
	VolatileStatus VolatileStatus `json:"volatileStatus,omitempty"`
}

// Kind 返回 participantFainted。
func (event ParticipantFaintedEvent) Kind() EventKind {
	return event.Type
}

// BattleEndedEvent 记录纯引擎已经确认的终局事实。
type BattleEndedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是确认终局结果的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
	// WinningSide 是胜方阵营位置；0 表示没有胜方的引擎平局。
	WinningSide Side `json:"winningSide,omitempty"`
	// Reason 是说明引擎终局原因的稳定代码。
	Reason BattleResultReason `json:"reason"`
}

// Kind 返回 battleEnded。
func (event BattleEndedEvent) Kind() EventKind {
	return event.Type
}

// TurnEndedEvent 记录一个完整回合结算边界的结束。
type TurnEndedEvent struct {
	// Type 是用于 JSON discriminator 的稳定事件种类。
	Type EventKind `json:"kind"`
	// SchemaVersion 是该事件载荷结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// TurnNumber 是本事件所属的回合编号。
	TurnNumber uint32 `json:"turnNumber"`
}

// Kind 返回 turnEnded。
func (event TurnEndedEvent) Kind() EventKind {
	return event.Type
}
