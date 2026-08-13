package battleengine

// resolveSwitchInCopyOpponentAbility 结算成员实际换入后复制一名存活上场对手当前特性的规则。
func resolveSwitchInCopyOpponentAbility(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 || !member.SwitchInCopyOpponentAbility {
		return state, nil
	}
	return applySwitchInCopyOpponentAbility(state, MemberRef{Side: slot.Side, Position: member.Position})
}

// initializeSwitchInCopyOpponentAbilities 按冻结阵营和槽位顺序处理双方初始上场成员的特性复制规则。
//
// 初始阶段只更新权威快照；后续实际换入才发布 AbilityCopiedEvent。复制来源只从当前已上场、存活且有特性的
// 对手中选择，因此不会从后备成员或实时资料读取规则。
func initializeSwitchInCopyOpponentAbilities(state State) State {
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || !member.SwitchInCopyOpponentAbility {
				continue
			}
			state, _ = applySwitchInCopyOpponentAbility(state, MemberRef{
				Side: side.Side, Position: member.Position,
			})
		}
	}
	return state
}

// applySwitchInCopyOpponentAbility 复制一名存活上场对手的特性身份和全部已实现的强类型特性规则。
//
// 双打时来源以对手阵营的稳定快照顺序和场上槽位顺序选择第一个满足条件的成员。选择规则不依赖客户端数组
// 或实时数据库；复制后不会在同一次入场重新触发新取得特性的入场效果，保持“先复制、后在未来生命周期使用”的
// 明确边界。
func applySwitchInCopyOpponentAbility(state State, actor MemberRef) (State, []Event) {
	receiver, found := state.member(actor.Side, actor.Position)
	if !found || receiver.CurrentHP == 0 || !receiver.SwitchInCopyOpponentAbility {
		return state, nil
	}
	sourceRef, source, found := copyableOpponentAbilitySource(state, actor.Side)
	if !found {
		return state, nil
	}
	previousAbilityID := receiver.AbilityID
	receiver = copyAbilityRules(receiver, source)
	state.replaceMember(actor.Side, receiver)
	return state, []Event{AbilityCopiedEvent{
		Type: EventKindAbilityCopied, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actor, Source: sourceRef, PreviousAbilityID: previousAbilityID, AbilityID: receiver.AbilityID,
	}}
}

// copyableOpponentAbilitySource 返回可被复制的第一名存活上场对手及其稳定成员引用。
func copyableOpponentAbilitySource(state State, receiverSide Side) (MemberRef, MemberSnapshot, bool) {
	for _, side := range state.sides {
		if side.Side == receiverSide {
			continue
		}
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if found && member.CurrentHP > 0 && member.AbilityID != 0 {
				return MemberRef{Side: side.Side, Position: member.Position}, member, true
			}
		}
	}
	return MemberRef{}, MemberSnapshot{}, false
}

// copyAbilityRules 用来源成员的当前特性身份完整替换接收者的全部已实现强类型特性规则。
//
// 此处故意逐字段列出，而不是将规则收敛为无类型效果数组：每增加一种可复制特性规则都必须在这里明确审查，
// 以保证复制、快照隔离和重放语义不会静默遗漏新字段。调用者及 replaceMember 均会深复制切片和指针。
func copyAbilityRules(receiver, source MemberSnapshot) MemberSnapshot {
	// 如果接收者此前因自身特性应用过道具属性身份，先回到已冻结的自然属性。复制后的特性会在本次入场
	// 生命周期稍后重新结算，因此不会错误保留已经失去的特性效果，也不会重复读取实时道具资料。
	receiver = restoreHeldItemElementIdentity(receiver)
	receiver.AbilityID = source.AbilityID
	receiver.WeatherDamageImmunities = append([]WeatherKind(nil), source.WeatherDamageImmunities...)
	receiver.WeatherEffectsSuppressed = source.WeatherEffectsSuppressed
	receiver.ReactiveAbilityRules = cloneReactiveAbilityRules(source.ReactiveAbilityRules)
	receiver.BasePowerAtMostDamageBoost = cloneBasePowerAtMostDamageBoost(source.BasePowerAtMostDamageBoost)
	receiver.RecoilSkillDamageBoost = cloneRecoilSkillDamageBoost(source.RecoilSkillDamageBoost)
	receiver.LowHPElementDamageBoost = cloneLowHPElementDamageBoost(source.LowHPElementDamageBoost)
	receiver.WeatherElementDamageBoost = cloneWeatherElementDamageBoost(source.WeatherElementDamageBoost)
	receiver.ElementSkillDamageBoost = cloneElementSkillDamageBoost(source.ElementSkillDamageBoost)
	receiver.SameElementBonusOverride = cloneSameElementBonusOverride(source.SameElementBonusOverride)
	receiver.ContactBasedSkillDamageBoost = cloneContactBasedSkillDamageBoost(source.ContactBasedSkillDamageBoost)
	receiver.CriticalHitDamageBoost = cloneCriticalHitDamageBoost(source.CriticalHitDamageBoost)
	receiver.SuperEffectiveDamageBoost = cloneSuperEffectiveDamageBoost(source.SuperEffectiveDamageBoost)
	receiver.NotVeryEffectiveDamageBoost = cloneNotVeryEffectiveDamageBoost(source.NotVeryEffectiveDamageBoost)
	receiver.TargetGenderDamageMultiplier = cloneTargetGenderDamageMultiplier(source.TargetGenderDamageMultiplier)
	receiver.PunchBasedSkillDamageBoost = clonePunchBasedSkillDamageBoost(source.PunchBasedSkillDamageBoost)
	receiver.SlicingBasedSkillDamageBoost = cloneSlicingBasedSkillDamageBoost(source.SlicingBasedSkillDamageBoost)
	receiver.SoundBasedSkillDamageBoost = cloneSoundBasedSkillDamageBoost(source.SoundBasedSkillDamageBoost)
	receiver.PulseBasedSkillDamageBoost = clonePulseBasedSkillDamageBoost(source.PulseBasedSkillDamageBoost)
	receiver.BiteBasedSkillDamageBoost = cloneBiteBasedSkillDamageBoost(source.BiteBasedSkillDamageBoost)
	receiver.SecondaryEffectsSuppressedDamageBoost = cloneSecondaryEffectsSuppressedDamageBoost(source.SecondaryEffectsSuppressedDamageBoost)
	receiver.SoundBasedSkillDamageReduction = cloneSoundBasedSkillDamageReduction(source.SoundBasedSkillDamageReduction)
	receiver.SuperEffectiveDamageReduction = cloneSuperEffectiveDamageReduction(source.SuperEffectiveDamageReduction)
	receiver.FullHPDamageReduction = cloneFullHPDamageReduction(source.FullHPDamageReduction)
	receiver.DamageClassDamageReduction = cloneDamageClassDamageReduction(source.DamageClassDamageReduction)
	receiver.ElementSkillDamageReduction = cloneElementSkillDamageReduction(source.ElementSkillDamageReduction)
	receiver.ContactBasedSkillDamageReduction = cloneContactBasedSkillDamageReduction(source.ContactBasedSkillDamageReduction)
	receiver.AttackingStatMultiplier = cloneAttackingStatMultiplier(source.AttackingStatMultiplier)
	receiver.OpponentAttackingStatMultiplier = cloneOpponentAttackingStatMultiplier(source.OpponentAttackingStatMultiplier)
	receiver.DefendingStatMultiplier = cloneDefendingStatMultiplier(source.DefendingStatMultiplier)
	receiver.OpponentDefendingStatMultiplier = cloneOpponentDefendingStatMultiplier(source.OpponentDefendingStatMultiplier)
	receiver.AllySkillDamageBoost = cloneAllySkillDamageBoost(source.AllySkillDamageBoost)
	receiver.AllyReceivedDamageReduction = cloneAllyReceivedDamageReduction(source.AllyReceivedDamageReduction)
	receiver.AllyAbilityGroupCode = source.AllyAbilityGroupCode
	receiver.AllyAbilityPresenceAttackingStatMultiplier = cloneAllyAbilityPresenceAttackingStatMultiplier(source.AllyAbilityPresenceAttackingStatMultiplier)
	receiver.AccuracyMultiplier = cloneAccuracyMultiplier(source.AccuracyMultiplier)
	receiver.PhysicalSkillAccuracyMultiplier = cloneAccuracyMultiplier(source.PhysicalSkillAccuracyMultiplier)
	receiver.OpponentAccuracySandstormMultiplier = cloneAccuracyMultiplier(source.OpponentAccuracySandstormMultiplier)
	receiver.OpponentAccuracySnowMultiplier = cloneAccuracyMultiplier(source.OpponentAccuracySnowMultiplier)
	receiver.OpponentAccuracyConfusionMultiplier = cloneAccuracyMultiplier(source.OpponentAccuracyConfusionMultiplier)
	receiver.AccuracyAlwaysHits = source.AccuracyAlwaysHits
	receiver.StatusSkillAccuracyCap = source.StatusSkillAccuracyCap
	receiver.IgnoreOpponentAccuracyStatStages = source.IgnoreOpponentAccuracyStatStages
	receiver.CriticalHitImmunity = source.CriticalHitImmunity
	receiver.SkillRecoilDamageImmunity = source.SkillRecoilDamageImmunity
	receiver.IndirectDamageImmunity = source.IndirectDamageImmunity
	receiver.ContactDamageToAttackerDenominator = source.ContactDamageToAttackerDenominator
	receiver.IgnoreOpponentDamageStatStages = source.IgnoreOpponentDamageStatStages
	receiver.IgnoreTargetAbilityEffects = source.IgnoreTargetAbilityEffects
	receiver.SurviveFatalDamageAtFullHP = source.SurviveFatalDamageAtFullHP
	receiver.OpponentStatusSkillImmunity = source.OpponentStatusSkillImmunity
	receiver.NonSuperEffectiveDamageImmunity = source.NonSuperEffectiveDamageImmunity
	receiver.CriticalHitStageBoost = source.CriticalHitStageBoost
	receiver.MultiHitMaximum = source.MultiHitMaximum
	receiver.DamagingSkillSecondaryEffectImmunity = source.DamagingSkillSecondaryEffectImmunity
	receiver.PriorityMoveImmunityForSideEnabled = source.PriorityMoveImmunityForSideEnabled
	receiver.PriorityMoveImmunityForSideProtectsAllies = source.PriorityMoveImmunityForSideProtectsAllies
	receiver.StatusSkillMovesLastAndIgnoresTargetAbility = source.StatusSkillMovesLastAndIgnoresTargetAbility
	receiver.ContactSkillProtectionBypass = source.ContactSkillProtectionBypass
	receiver.ContactSkillProtectionBypassDamageMultiplier = cloneDamageFraction(source.ContactSkillProtectionBypassDamageMultiplier)
	receiver.SkillWeatherOverride = source.SkillWeatherOverride
	receiver.SkillElementConversion = cloneSkillElementConversion(source.SkillElementConversion)
	receiver.ContactSuppression = source.ContactSuppression
	receiver.ReceivedContactDamageHalved = source.ReceivedContactDamageHalved
	receiver.ReceivedFireDamageDoubled = source.ReceivedFireDamageDoubled
	receiver.ForcedSwitchImmunity = source.ForcedSwitchImmunity
	receiver.OpponentSwitchRestriction = cloneOpponentSwitchRestriction(source.OpponentSwitchRestriction)
	receiver.DamageCrossedHalfHPForceSelfSwitch = source.DamageCrossedHalfHPForceSelfSwitch
	receiver.SwitchOutMajorStatusCure = source.SwitchOutMajorStatusCure
	receiver.SwitchOutHealDenominator = source.SwitchOutHealDenominator
	receiver.WeatherEndTurnHealing = cloneWeatherEndTurnHealing(source.WeatherEndTurnHealing)
	receiver.WeatherSpeedMultipliers = append([]WeatherSpeedMultiplier(nil), source.WeatherSpeedMultipliers...)
	receiver.EnvironmentHighestStatMultiplier = cloneEnvironmentHighestStatMultiplier(source.EnvironmentHighestStatMultiplier)
	receiver.TerastallizationStatStageChange = cloneTerastallizationStatStageChange(source.TerastallizationStatStageChange)
	receiver.TerastallizationEnvironmentClear = source.TerastallizationEnvironmentClear
	receiver.SwitchInStrongWeather = source.SwitchInStrongWeather
	receiver.SwitchInWeather = cloneSwitchInWeather(source.SwitchInWeather)
	receiver.SwitchInTerrain = cloneSwitchInTerrain(source.SwitchInTerrain)
	receiver.SwitchInStatStageChange = cloneSwitchInStatStageChange(source.SwitchInStatStageChange)
	receiver.SwitchInAllyHeal = cloneSwitchInAllyHeal(source.SwitchInAllyHeal)
	receiver.SwitchInOpponentDefenseComparisonBoost = source.SwitchInOpponentDefenseComparisonBoost
	receiver.SwitchInAllyStatStageCopy = source.SwitchInAllyStatStageCopy
	receiver.SwitchInAllyStatStageReset = source.SwitchInAllyStatStageReset
	receiver.SwitchInClearAllSideDamageReductions = source.SwitchInClearAllSideDamageReductions
	receiver.SwitchInCopyOpponentAbility = source.SwitchInCopyOpponentAbility
	receiver.SwitchInRevealOpponentHeldItems = source.SwitchInRevealOpponentHeldItems
	receiver.SwitchInRevealOpponentHighestPowerSkill = source.SwitchInRevealOpponentHighestPowerSkill
	receiver.SwitchInTransformIntoOpponent = source.SwitchInTransformIntoOpponent
	receiver.SwitchInDetectDangerousOpponentSkill = source.SwitchInDetectDangerousOpponentSkill
	receiver.SwitchInDisguiseAsLastHealthyAlly = source.SwitchInDisguiseAsLastHealthyAlly
	receiver.SwitchInHeldItemElementIdentity = source.SwitchInHeldItemElementIdentity
	// 形态规则不仅携带特性自身的 Identifier，还依赖接收者以自身等级和培养值冻结的 FormProfile。不能直接复用来源
	// 成员的画像：那会把来源成员的生命、能力和体重错误应用给接收者。只有接收者已经具备同一组画像时才复制
	// 该规则；否则明确不带入，避免运行期因找不到画像而静默半执行一个不完整的特性。
	receiver.SwitchInFormChange = cloneCopyableSwitchInFormChange(receiver.FormProfiles, source.SwitchInFormChange)
	receiver.SwitchOutFormChange = cloneCopyableSwitchOutFormChange(receiver.FormProfiles, source.SwitchOutFormChange)
	receiver.WeatherFormChange = cloneCopyableWeatherFormChange(receiver.FormProfiles, source.WeatherFormChange)
	return receiver
}

// cloneCopyableSwitchInFormChange 仅在接收者自己的冻结画像完整覆盖入场形态规则时复制该规则。
func cloneCopyableSwitchInFormChange(profiles []FormProfile, value *SwitchInFormChange) *SwitchInFormChange {
	if value == nil || validateSwitchInFormChange(value, profiles) != nil {
		return nil
	}
	return cloneSwitchInFormChange(value)
}

// cloneCopyableSwitchOutFormChange 仅在接收者自己的冻结画像完整覆盖离场形态规则时复制该规则。
func cloneCopyableSwitchOutFormChange(profiles []FormProfile, value *SwitchOutFormChange) *SwitchOutFormChange {
	if value == nil || validateSwitchOutFormChange(value, profiles) != nil {
		return nil
	}
	return cloneSwitchOutFormChange(value)
}

// cloneCopyableWeatherFormChange 仅在接收者自己的冻结画像完整覆盖默认形态与全部天气目标时复制该规则。
func cloneCopyableWeatherFormChange(profiles []FormProfile, value *WeatherFormChange) *WeatherFormChange {
	if value == nil || validateWeatherFormChange(value, profiles) != nil {
		return nil
	}
	return cloneWeatherFormChange(value)
}
