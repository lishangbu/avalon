package battleengine

// resolveSwitchInTransformIntoOpponent 结算成员实际换入后复制一名存活上场对手战斗画像的规则。
func resolveSwitchInTransformIntoOpponent(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 || !member.SwitchInTransformIntoOpponent || member.TransformSnapshot != nil {
		return state, nil
	}
	return applySwitchInTransformIntoOpponent(state, MemberRef{Side: slot.Side, Position: member.Position})
}

// initializeSwitchInTransformIntoOpponent 按冻结阵营与槽位顺序结算双方初始入场的变身特性。
//
// 初始入场同样必须产生结构化事件，以便 Battle 在创建事务中持久化公开事实。后续成员的变身只选择当时存活且
// 当前上场的对手，绝不读取后备成员或实时资料。
func initializeSwitchInTransformIntoOpponent(state State) (State, []Event) {
	events := make([]Event, 0, 2)
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || !member.SwitchInTransformIntoOpponent || member.TransformSnapshot != nil {
				continue
			}
			var transformed []Event
			state, transformed = applySwitchInTransformIntoOpponent(state, MemberRef{
				Side: side.Side, Position: member.Position,
			})
			events = append(events, transformed...)
		}
	}
	return state, events
}

// applySwitchInTransformIntoOpponent 选择稳定第一名存活上场对手，并将其可复制战斗画像写入触发者。
//
// 规则只允许尚未变身的成员触发一次。技能的最大与当前 PP 均使用 min(5, target.maxPp)，能力阶级则复制
// 目标当前值；原始画像被单独保存，因此离场不会永久污染成员所属 Team 的冻结资料。
func applySwitchInTransformIntoOpponent(state State, actor MemberRef) (State, []Event) {
	receiver, found := state.member(actor.Side, actor.Position)
	if !found || receiver.CurrentHP == 0 || !receiver.SwitchInTransformIntoOpponent || receiver.TransformSnapshot != nil {
		return state, nil
	}
	targetRef, target, found := transformableOpponent(state, actor.Side)
	if !found {
		return state, nil
	}
	transformed := receiver
	transformed.TransformSnapshot = newMemberTransformSnapshot(receiver)
	transformed.CreatureID = target.CreatureID
	transformed.Stats = target.Stats
	transformed.Weight = target.Weight
	transformed.NaturalElementIDs = append([]Identifier(nil), target.NaturalElementIDs...)
	if len(transformed.NaturalElementIDs) == 0 {
		transformed.NaturalElementIDs = append([]Identifier(nil), target.ElementIDs...)
	}
	transformed.ElementIDs = append([]Identifier(nil), target.ElementIDs...)
	if transformed.Terastallized {
		transformed.ElementIDs = []Identifier{transformed.TeraElementID}
	}
	transformed.Skills = transformedSkills(target.Skills)
	transformed.StatStages = cloneStatStages(target.StatStages)
	transformed = copyAbilityRules(transformed, target)
	state.replaceMember(actor.Side, transformed)
	return state, []Event{ParticipantTransformedEvent{
		Type: EventKindParticipantTransformed, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actor, Target: targetRef, CopiedCreatureID: target.CreatureID,
	}}
}

// transformableOpponent 返回触发者对侧第一名存活且当前上场的成员。
//
// State 保证阵营和槽位有稳定排序，因此此选择与数据库返回顺序、客户端列表顺序无关。
func transformableOpponent(state State, receiverSide Side) (MemberRef, MemberSnapshot, bool) {
	for _, side := range state.sides {
		if side.Side == receiverSide {
			continue
		}
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if found && member.CurrentHP > 0 {
				return MemberRef{Side: side.Side, Position: member.Position}, member, true
			}
		}
	}
	return MemberRef{}, MemberSnapshot{}, false
}

// transformedSkills 复制目标技能，并将每个槽位的最大与当前 PP 限制为变身规则要求的上限。
func transformedSkills(skills []SkillSnapshot) []SkillSnapshot {
	cloned := cloneSkillSnapshots(skills)
	for index := range cloned {
		copiedPP := min(uint8(5), cloned[index].MaxPP)
		cloned[index].MaxPP = copiedPP
		cloned[index].RemainingPP = copiedPP
	}
	return cloned
}

// newMemberTransformSnapshot 捕获成员变身前全部会被变身覆盖、且必须在离场时恢复的字段。
func newMemberTransformSnapshot(member MemberSnapshot) *MemberTransformSnapshot {
	return &MemberTransformSnapshot{
		CreatureID: member.CreatureID, Stats: member.Stats, Weight: member.Weight,
		ElementIDs: append([]Identifier(nil), member.ElementIDs...), NaturalElementIDs: append([]Identifier(nil), member.NaturalElementIDs...),
		Skills: cloneSkillSnapshots(member.Skills), AbilityID: member.AbilityID,
		WeatherDamageImmunities:                              append([]WeatherKind(nil), member.WeatherDamageImmunities...),
		WeatherEffectsSuppressed:                             member.WeatherEffectsSuppressed,
		ReactiveAbilityRules:                                 cloneReactiveAbilityRules(member.ReactiveAbilityRules),
		BasePowerAtMostDamageBoost:                           cloneBasePowerAtMostDamageBoost(member.BasePowerAtMostDamageBoost),
		RecoilSkillDamageBoost:                               cloneRecoilSkillDamageBoost(member.RecoilSkillDamageBoost),
		LowHPElementDamageBoost:                              cloneLowHPElementDamageBoost(member.LowHPElementDamageBoost),
		WeatherElementDamageBoost:                            cloneWeatherElementDamageBoost(member.WeatherElementDamageBoost),
		ElementSkillDamageBoost:                              cloneElementSkillDamageBoost(member.ElementSkillDamageBoost),
		SameElementBonusOverride:                             cloneSameElementBonusOverride(member.SameElementBonusOverride),
		ContactBasedSkillDamageBoost:                         cloneContactBasedSkillDamageBoost(member.ContactBasedSkillDamageBoost),
		CriticalHitDamageBoost:                               cloneCriticalHitDamageBoost(member.CriticalHitDamageBoost),
		SuperEffectiveDamageBoost:                            cloneSuperEffectiveDamageBoost(member.SuperEffectiveDamageBoost),
		NotVeryEffectiveDamageBoost:                          cloneNotVeryEffectiveDamageBoost(member.NotVeryEffectiveDamageBoost),
		TargetGenderDamageMultiplier:                         cloneTargetGenderDamageMultiplier(member.TargetGenderDamageMultiplier),
		PunchBasedSkillDamageBoost:                           clonePunchBasedSkillDamageBoost(member.PunchBasedSkillDamageBoost),
		SlicingBasedSkillDamageBoost:                         cloneSlicingBasedSkillDamageBoost(member.SlicingBasedSkillDamageBoost),
		SoundBasedSkillDamageBoost:                           cloneSoundBasedSkillDamageBoost(member.SoundBasedSkillDamageBoost),
		PulseBasedSkillDamageBoost:                           clonePulseBasedSkillDamageBoost(member.PulseBasedSkillDamageBoost),
		BiteBasedSkillDamageBoost:                            cloneBiteBasedSkillDamageBoost(member.BiteBasedSkillDamageBoost),
		SecondaryEffectsSuppressedDamageBoost:                cloneSecondaryEffectsSuppressedDamageBoost(member.SecondaryEffectsSuppressedDamageBoost),
		SoundBasedSkillDamageReduction:                       cloneSoundBasedSkillDamageReduction(member.SoundBasedSkillDamageReduction),
		SuperEffectiveDamageReduction:                        cloneSuperEffectiveDamageReduction(member.SuperEffectiveDamageReduction),
		FullHPDamageReduction:                                cloneFullHPDamageReduction(member.FullHPDamageReduction),
		DamageClassDamageReduction:                           cloneDamageClassDamageReduction(member.DamageClassDamageReduction),
		ElementSkillDamageReduction:                          cloneElementSkillDamageReduction(member.ElementSkillDamageReduction),
		ContactBasedSkillDamageReduction:                     cloneContactBasedSkillDamageReduction(member.ContactBasedSkillDamageReduction),
		AttackingStatMultiplier:                              cloneAttackingStatMultiplier(member.AttackingStatMultiplier),
		OpponentAttackingStatMultiplier:                      cloneOpponentAttackingStatMultiplier(member.OpponentAttackingStatMultiplier),
		DefendingStatMultiplier:                              cloneDefendingStatMultiplier(member.DefendingStatMultiplier),
		OpponentDefendingStatMultiplier:                      cloneOpponentDefendingStatMultiplier(member.OpponentDefendingStatMultiplier),
		AllySkillDamageBoost:                                 cloneAllySkillDamageBoost(member.AllySkillDamageBoost),
		AllyReceivedDamageReduction:                          cloneAllyReceivedDamageReduction(member.AllyReceivedDamageReduction),
		AllyAbilityGroupCode:                                 member.AllyAbilityGroupCode,
		AllyAbilityPresenceAttackingStatMultiplier:           cloneAllyAbilityPresenceAttackingStatMultiplier(member.AllyAbilityPresenceAttackingStatMultiplier),
		AccuracyMultiplier:                                   cloneAccuracyMultiplier(member.AccuracyMultiplier),
		PhysicalSkillAccuracyMultiplier:                      cloneAccuracyMultiplier(member.PhysicalSkillAccuracyMultiplier),
		OpponentAccuracySandstormMultiplier:                  cloneAccuracyMultiplier(member.OpponentAccuracySandstormMultiplier),
		OpponentAccuracySnowMultiplier:                       cloneAccuracyMultiplier(member.OpponentAccuracySnowMultiplier),
		OpponentAccuracyConfusionMultiplier:                  cloneAccuracyMultiplier(member.OpponentAccuracyConfusionMultiplier),
		AccuracyAlwaysHits:                                   member.AccuracyAlwaysHits,
		StatusSkillAccuracyCap:                               member.StatusSkillAccuracyCap,
		IgnoreOpponentAccuracyStatStages:                     member.IgnoreOpponentAccuracyStatStages,
		CriticalHitImmunity:                                  member.CriticalHitImmunity,
		SkillRecoilDamageImmunity:                            member.SkillRecoilDamageImmunity,
		IndirectDamageImmunity:                               member.IndirectDamageImmunity,
		ContactDamageToAttackerDenominator:                   member.ContactDamageToAttackerDenominator,
		IgnoreOpponentDamageStatStages:                       member.IgnoreOpponentDamageStatStages,
		IgnoreTargetAbilityEffects:                           member.IgnoreTargetAbilityEffects,
		SurviveFatalDamageAtFullHP:                           member.SurviveFatalDamageAtFullHP,
		OpponentStatusSkillImmunity:                          member.OpponentStatusSkillImmunity,
		NonSuperEffectiveDamageImmunity:                      member.NonSuperEffectiveDamageImmunity,
		CriticalHitStageBoost:                                member.CriticalHitStageBoost,
		MultiHitMaximum:                                      member.MultiHitMaximum,
		DamagingSkillSecondaryEffectImmunity:                 member.DamagingSkillSecondaryEffectImmunity,
		PriorityMoveImmunityForSideEnabled:                   member.PriorityMoveImmunityForSideEnabled,
		PriorityMoveImmunityForSideProtectsAllies:            member.PriorityMoveImmunityForSideProtectsAllies,
		StatusSkillMovesLastAndIgnoresTargetAbility:          member.StatusSkillMovesLastAndIgnoresTargetAbility,
		ContactSkillProtectionBypass:                         member.ContactSkillProtectionBypass,
		ContactSkillProtectionBypassDamageMultiplier:         cloneDamageFraction(member.ContactSkillProtectionBypassDamageMultiplier),
		SkillWeatherOverride:                                 member.SkillWeatherOverride,
		SkillElementConversion:                               cloneSkillElementConversion(member.SkillElementConversion),
		ContactSuppression:                                   member.ContactSuppression,
		ReceivedContactDamageHalved:                          member.ReceivedContactDamageHalved,
		ReceivedFireDamageDoubled:                            member.ReceivedFireDamageDoubled,
		ForcedSwitchImmunity:                                 member.ForcedSwitchImmunity,
		OpponentSwitchRestriction:                            cloneOpponentSwitchRestriction(member.OpponentSwitchRestriction),
		DamageCrossedHalfHPForceSelfSwitch:                   member.DamageCrossedHalfHPForceSelfSwitch,
		SwitchOutMajorStatusCure:                             member.SwitchOutMajorStatusCure,
		SwitchOutHealDenominator:                             member.SwitchOutHealDenominator,
		WeatherEndTurnHealing:                                cloneWeatherEndTurnHealing(member.WeatherEndTurnHealing),
		WeatherSpeedMultipliers:                              append([]WeatherSpeedMultiplier(nil), member.WeatherSpeedMultipliers...),
		EnvironmentHighestStatMultiplier:                     cloneEnvironmentHighestStatMultiplier(member.EnvironmentHighestStatMultiplier),
		TerastallizationStatStageChange:                      cloneTerastallizationStatStageChange(member.TerastallizationStatStageChange),
		TerastallizationEnvironmentClear:                     member.TerastallizationEnvironmentClear,
		SwitchInStrongWeather:                                member.SwitchInStrongWeather,
		SwitchInWeather:                                      cloneSwitchInWeather(member.SwitchInWeather),
		SwitchInTerrain:                                      cloneSwitchInTerrain(member.SwitchInTerrain),
		SwitchInStatStageChange:                              cloneSwitchInStatStageChange(member.SwitchInStatStageChange),
		SwitchInAllyHeal:                                     cloneSwitchInAllyHeal(member.SwitchInAllyHeal),
		SwitchInOpponentDefenseComparisonBoost:               member.SwitchInOpponentDefenseComparisonBoost,
		SwitchInAllyStatStageCopy:                            member.SwitchInAllyStatStageCopy,
		SwitchInAllyStatStageReset:                           member.SwitchInAllyStatStageReset,
		SwitchInClearAllSideDamageReductions:                 member.SwitchInClearAllSideDamageReductions,
		SwitchInCopyOpponentAbility:                          member.SwitchInCopyOpponentAbility,
		SwitchInRevealOpponentHeldItems:                      member.SwitchInRevealOpponentHeldItems,
		SwitchInRevealOpponentHighestPowerSkill:              member.SwitchInRevealOpponentHighestPowerSkill,
		SwitchInTransformIntoOpponent:                        member.SwitchInTransformIntoOpponent,
		SwitchInDetectDangerousOpponentSkill:                 member.SwitchInDetectDangerousOpponentSkill,
		SwitchInDisguiseAsLastHealthyAlly:                    member.SwitchInDisguiseAsLastHealthyAlly,
		SwitchInHeldItemElementIdentity:                      member.SwitchInHeldItemElementIdentity,
		SwitchRestrictionImmunity:                            member.SwitchRestrictionImmunity,
		ContactSideEffectImmunity:                            member.ContactSideEffectImmunity,
		HeldItemContactDamageToAttackerDenominator:           member.HeldItemContactDamageToAttackerDenominator,
		HeldItemEndTurnHealDenominator:                       member.HeldItemEndTurnHealDenominator,
		HeldItemEndTurnHealForElementID:                      member.HeldItemEndTurnHealForElementID,
		HeldItemEndTurnHealForElementDenominator:             member.HeldItemEndTurnHealForElementDenominator,
		HeldItemEndTurnDamageDenominator:                     member.HeldItemEndTurnDamageDenominator,
		HeldItemEndTurnDamageWithoutElementID:                member.HeldItemEndTurnDamageWithoutElementID,
		HeldItemEndTurnDamageWithoutElementDenominator:       member.HeldItemEndTurnDamageWithoutElementDenominator,
		HeldItemConsumableElementDamageBoostElementID:        member.HeldItemConsumableElementDamageBoostElementID,
		HeldItemConsumableElementDamageBoostNumerator:        member.HeldItemConsumableElementDamageBoostNumerator,
		HeldItemConsumableElementDamageBoostDenominator:      member.HeldItemConsumableElementDamageBoostDenominator,
		ContactTransferToAttacker:                            member.ContactTransferToAttacker,
		ChargeSkipOnce:                                       member.ChargeSkipOnce,
		HeldItemSurviveFatalDamageAtFullHP:                   member.HeldItemSurviveFatalDamageAtFullHP,
		HeldItemReflectTurnsRemaining:                        member.HeldItemReflectTurnsRemaining,
		HeldItemLightScreenTurnsRemaining:                    member.HeldItemLightScreenTurnsRemaining,
		HeldItemAuroraVeilTurnsRemaining:                     member.HeldItemAuroraVeilTurnsRemaining,
		HeldItemRainTurnsRemaining:                           member.HeldItemRainTurnsRemaining,
		HeldItemSandstormTurnsRemaining:                      member.HeldItemSandstormTurnsRemaining,
		HeldItemSnowTurnsRemaining:                           member.HeldItemSnowTurnsRemaining,
		HeldItemSunTurnsRemaining:                            member.HeldItemSunTurnsRemaining,
		HeldItemTerrainTurnsRemaining:                        member.HeldItemTerrainTurnsRemaining,
		HeldItemSandstormDamageImmunity:                      member.HeldItemSandstormDamageImmunity,
		HeldItemEntryHazardImmunity:                          member.HeldItemEntryHazardImmunity,
		HeldItemWeightHalf:                                   member.HeldItemWeightHalf,
		HeldItemCuresParalysis:                               member.HeldItemCuresParalysis,
		HeldItemCuresSleep:                                   member.HeldItemCuresSleep,
		HeldItemCuresPoison:                                  member.HeldItemCuresPoison,
		HeldItemCuresBurn:                                    member.HeldItemCuresBurn,
		HeldItemCuresFreeze:                                  member.HeldItemCuresFreeze,
		HeldItemCuresAllMajorStatuses:                        member.HeldItemCuresAllMajorStatuses,
		HeldItemCuresConfusion:                               member.HeldItemCuresConfusion,
		HeldItemPunchBasedSkillPowerBoost:                    member.HeldItemPunchBasedSkillPowerBoost,
		HeldItemPhysicalDamagePowerBoost:                     member.HeldItemPhysicalDamagePowerBoost,
		HeldItemSpecialDamagePowerBoost:                      member.HeldItemSpecialDamagePowerBoost,
		HeldItemElementDamageReductionElementID:              member.HeldItemElementDamageReductionElementID,
		HeldItemElementDamageReductionRequiresSuperEffective: member.HeldItemElementDamageReductionRequiresSuperEffective,
		HeldItemSuperEffectiveDamageBoost:                    member.HeldItemSuperEffectiveDamageBoost,
		HeldItemDamageBoostWithRecoil:                        member.HeldItemDamageBoostWithRecoil,
		HeldItemDamageDealtHeal:                              member.HeldItemDamageDealtHeal,
		HeldItemDrainHealingBoost:                            member.HeldItemDrainHealingBoost,
		HeldItemAccuracyBoost:                                member.HeldItemAccuracyBoost,
		HeldItemOpponentAccuracyReduction:                    member.HeldItemOpponentAccuracyReduction,
		HeldItemCriticalHitStageBoost:                        member.HeldItemCriticalHitStageBoost,
		HeldItemAirborneUntilDamaged:                         member.HeldItemAirborneUntilDamaged,
		HeldItemForceGrounded:                                member.HeldItemForceGrounded,
		HeldItemSpeedHalf:                                    member.HeldItemSpeedHalf,
		HeldItemSpecialDefenseBoost:                          member.HeldItemSpecialDefenseBoost,
		HeldItemStatusSkillRestriction:                       member.HeldItemStatusSkillRestriction,
		HeldItemPhysicalDamagePowerBoost50:                   member.HeldItemPhysicalDamagePowerBoost50,
		HeldItemSpecialDamagePowerBoost50:                    member.HeldItemSpecialDamagePowerBoost50,
		HeldItemChoiceSkillLock:                              member.HeldItemChoiceSkillLock,
		HeldItemSpeedBoost50:                                 member.HeldItemSpeedBoost50,
		HeldItemAccuracyAfterTargetActedBoost:                member.HeldItemAccuracyAfterTargetActedBoost,
		HeldItemTypeImmunitySuppression:                      member.HeldItemTypeImmunitySuppression,
		HeldItemOpponentStatStageReductionImmunity:           member.HeldItemOpponentStatStageReductionImmunity,
		HeldItemNegativeStatStageReset:                       member.HeldItemNegativeStatStageReset,
		HeldItemAbilityStatReductionSpeedBoost:               member.HeldItemAbilityStatReductionSpeedBoost,
		HeldItemOpponentPositiveStatStageCopy:                member.HeldItemOpponentPositiveStatStageCopy,
		HeldItemDamagingSkillSecondaryEffectImmunity:         member.HeldItemDamagingSkillSecondaryEffectImmunity,
		HeldItemBindingTurns:                                 member.HeldItemBindingTurns,
		HeldItemBindingDamageDenominator:                     member.HeldItemBindingDamageDenominator,
		HeldItemAccuracyMissStatStageBoostStat:               member.HeldItemAccuracyMissStatStageBoostStat,
		HeldItemAccuracyMissStatStageBoostDelta:              member.HeldItemAccuracyMissStatStageBoostDelta,
		HeldItemWeaknessPolicy:                               member.HeldItemWeaknessPolicy,
		HeldItemWaterDamageSpecialAttackBoostElementID:       member.HeldItemWaterDamageSpecialAttackBoostElementID,
		HeldItemElectricDamageAttackBoostElementID:           member.HeldItemElectricDamageAttackBoostElementID,
		HeldItemWaterDamageSpecialDefenseBoostElementID:      member.HeldItemWaterDamageSpecialDefenseBoostElementID,
		HeldItemIceDamageAttackBoostElementID:                member.HeldItemIceDamageAttackBoostElementID,
		HeldItemAdditionalFlinchChancePercent:                member.HeldItemAdditionalFlinchChancePercent,
		HeldItemRandomActionOrderBoostChancePercent:          member.HeldItemRandomActionOrderBoostChancePercent,
		HeldItemForcedLastActionOrder:                        member.HeldItemForcedLastActionOrder,
		HeldItemLowHPActionOrderBoost:                        member.HeldItemLowHPActionOrderBoost,
		HeldItemFieldSpeedOrderSpeedStageDrop:                member.HeldItemFieldSpeedOrderSpeedStageDrop,
		HeldItemConsecutiveSkillDamageBoost:                  member.HeldItemConsecutiveSkillDamageBoost,
		HeldItemPunchBasedContactSuppression:                 member.HeldItemPunchBasedContactSuppression,
		HeldItemPowderSkillImmunity:                          member.HeldItemPowderSkillImmunity,
		HeldItemMultiHitCountMinimum:                         member.HeldItemMultiHitCountMinimum,
		HeldItemMultiHitCountMaximum:                         member.HeldItemMultiHitCountMaximum,
		HeldItemMultiHitRequiredMinimum:                      member.HeldItemMultiHitRequiredMinimum,
		HeldItemMultiHitRequiredMaximum:                      member.HeldItemMultiHitRequiredMaximum,
		HeldItemElementID:                                    member.HeldItemElementID,
		HeldItemElementIdentityBaseElementIDs:                append([]Identifier(nil), member.HeldItemElementIdentityBaseElementIDs...),
		SwitchInFormChange:                                   cloneSwitchInFormChange(member.SwitchInFormChange),
		SwitchOutFormChange:                                  cloneSwitchOutFormChange(member.SwitchOutFormChange),
		WeatherFormChange:                                    cloneWeatherFormChange(member.WeatherFormChange),
	}
}

// restoreTransformSnapshot 以变身前画像恢复成员，并清除一次性变身运行态。
func restoreTransformSnapshot(member MemberSnapshot) MemberSnapshot {
	snapshot := member.TransformSnapshot
	if snapshot == nil {
		return member
	}
	member.CreatureID = snapshot.CreatureID
	member.Stats = snapshot.Stats
	member.Weight = snapshot.Weight
	member.NaturalElementIDs = append([]Identifier(nil), snapshot.NaturalElementIDs...)
	member.ElementIDs = append([]Identifier(nil), snapshot.ElementIDs...)
	if member.Terastallized {
		member.ElementIDs = []Identifier{member.TeraElementID}
	}
	member.Skills = cloneSkillSnapshots(snapshot.Skills)
	member.AbilityID = snapshot.AbilityID
	member.WeatherDamageImmunities = append([]WeatherKind(nil), snapshot.WeatherDamageImmunities...)
	member.WeatherEffectsSuppressed = snapshot.WeatherEffectsSuppressed
	member.ReactiveAbilityRules = cloneReactiveAbilityRules(snapshot.ReactiveAbilityRules)
	member.BasePowerAtMostDamageBoost = cloneBasePowerAtMostDamageBoost(snapshot.BasePowerAtMostDamageBoost)
	member.RecoilSkillDamageBoost = cloneRecoilSkillDamageBoost(snapshot.RecoilSkillDamageBoost)
	member.LowHPElementDamageBoost = cloneLowHPElementDamageBoost(snapshot.LowHPElementDamageBoost)
	member.WeatherElementDamageBoost = cloneWeatherElementDamageBoost(snapshot.WeatherElementDamageBoost)
	member.ElementSkillDamageBoost = cloneElementSkillDamageBoost(snapshot.ElementSkillDamageBoost)
	member.SameElementBonusOverride = cloneSameElementBonusOverride(snapshot.SameElementBonusOverride)
	member.ContactBasedSkillDamageBoost = cloneContactBasedSkillDamageBoost(snapshot.ContactBasedSkillDamageBoost)
	member.CriticalHitDamageBoost = cloneCriticalHitDamageBoost(snapshot.CriticalHitDamageBoost)
	member.SuperEffectiveDamageBoost = cloneSuperEffectiveDamageBoost(snapshot.SuperEffectiveDamageBoost)
	member.NotVeryEffectiveDamageBoost = cloneNotVeryEffectiveDamageBoost(snapshot.NotVeryEffectiveDamageBoost)
	member.TargetGenderDamageMultiplier = cloneTargetGenderDamageMultiplier(snapshot.TargetGenderDamageMultiplier)
	member.PunchBasedSkillDamageBoost = clonePunchBasedSkillDamageBoost(snapshot.PunchBasedSkillDamageBoost)
	member.SlicingBasedSkillDamageBoost = cloneSlicingBasedSkillDamageBoost(snapshot.SlicingBasedSkillDamageBoost)
	member.SoundBasedSkillDamageBoost = cloneSoundBasedSkillDamageBoost(snapshot.SoundBasedSkillDamageBoost)
	member.PulseBasedSkillDamageBoost = clonePulseBasedSkillDamageBoost(snapshot.PulseBasedSkillDamageBoost)
	member.BiteBasedSkillDamageBoost = cloneBiteBasedSkillDamageBoost(snapshot.BiteBasedSkillDamageBoost)
	member.SecondaryEffectsSuppressedDamageBoost = cloneSecondaryEffectsSuppressedDamageBoost(snapshot.SecondaryEffectsSuppressedDamageBoost)
	member.SoundBasedSkillDamageReduction = cloneSoundBasedSkillDamageReduction(snapshot.SoundBasedSkillDamageReduction)
	member.SuperEffectiveDamageReduction = cloneSuperEffectiveDamageReduction(snapshot.SuperEffectiveDamageReduction)
	member.FullHPDamageReduction = cloneFullHPDamageReduction(snapshot.FullHPDamageReduction)
	member.DamageClassDamageReduction = cloneDamageClassDamageReduction(snapshot.DamageClassDamageReduction)
	member.ElementSkillDamageReduction = cloneElementSkillDamageReduction(snapshot.ElementSkillDamageReduction)
	member.ContactBasedSkillDamageReduction = cloneContactBasedSkillDamageReduction(snapshot.ContactBasedSkillDamageReduction)
	member.AttackingStatMultiplier = cloneAttackingStatMultiplier(snapshot.AttackingStatMultiplier)
	member.OpponentAttackingStatMultiplier = cloneOpponentAttackingStatMultiplier(snapshot.OpponentAttackingStatMultiplier)
	member.DefendingStatMultiplier = cloneDefendingStatMultiplier(snapshot.DefendingStatMultiplier)
	member.OpponentDefendingStatMultiplier = cloneOpponentDefendingStatMultiplier(snapshot.OpponentDefendingStatMultiplier)
	member.AllySkillDamageBoost = cloneAllySkillDamageBoost(snapshot.AllySkillDamageBoost)
	member.AllyReceivedDamageReduction = cloneAllyReceivedDamageReduction(snapshot.AllyReceivedDamageReduction)
	member.AllyAbilityGroupCode = snapshot.AllyAbilityGroupCode
	member.AllyAbilityPresenceAttackingStatMultiplier = cloneAllyAbilityPresenceAttackingStatMultiplier(snapshot.AllyAbilityPresenceAttackingStatMultiplier)
	member.AccuracyMultiplier = cloneAccuracyMultiplier(snapshot.AccuracyMultiplier)
	member.PhysicalSkillAccuracyMultiplier = cloneAccuracyMultiplier(snapshot.PhysicalSkillAccuracyMultiplier)
	member.OpponentAccuracySandstormMultiplier = cloneAccuracyMultiplier(snapshot.OpponentAccuracySandstormMultiplier)
	member.OpponentAccuracySnowMultiplier = cloneAccuracyMultiplier(snapshot.OpponentAccuracySnowMultiplier)
	member.OpponentAccuracyConfusionMultiplier = cloneAccuracyMultiplier(snapshot.OpponentAccuracyConfusionMultiplier)
	member.AccuracyAlwaysHits = snapshot.AccuracyAlwaysHits
	member.StatusSkillAccuracyCap = snapshot.StatusSkillAccuracyCap
	member.IgnoreOpponentAccuracyStatStages = snapshot.IgnoreOpponentAccuracyStatStages
	member.CriticalHitImmunity = snapshot.CriticalHitImmunity
	member.SkillRecoilDamageImmunity = snapshot.SkillRecoilDamageImmunity
	member.IndirectDamageImmunity = snapshot.IndirectDamageImmunity
	member.ContactDamageToAttackerDenominator = snapshot.ContactDamageToAttackerDenominator
	member.IgnoreOpponentDamageStatStages = snapshot.IgnoreOpponentDamageStatStages
	member.IgnoreTargetAbilityEffects = snapshot.IgnoreTargetAbilityEffects
	member.SurviveFatalDamageAtFullHP = snapshot.SurviveFatalDamageAtFullHP
	member.OpponentStatusSkillImmunity = snapshot.OpponentStatusSkillImmunity
	member.NonSuperEffectiveDamageImmunity = snapshot.NonSuperEffectiveDamageImmunity
	member.CriticalHitStageBoost = snapshot.CriticalHitStageBoost
	member.MultiHitMaximum = snapshot.MultiHitMaximum
	member.DamagingSkillSecondaryEffectImmunity = snapshot.DamagingSkillSecondaryEffectImmunity
	member.PriorityMoveImmunityForSideEnabled = snapshot.PriorityMoveImmunityForSideEnabled
	member.PriorityMoveImmunityForSideProtectsAllies = snapshot.PriorityMoveImmunityForSideProtectsAllies
	member.StatusSkillMovesLastAndIgnoresTargetAbility = snapshot.StatusSkillMovesLastAndIgnoresTargetAbility
	member.ContactSkillProtectionBypass = snapshot.ContactSkillProtectionBypass
	member.ContactSkillProtectionBypassDamageMultiplier = cloneDamageFraction(snapshot.ContactSkillProtectionBypassDamageMultiplier)
	member.SkillWeatherOverride = snapshot.SkillWeatherOverride
	member.SkillElementConversion = cloneSkillElementConversion(snapshot.SkillElementConversion)
	member.ContactSuppression = snapshot.ContactSuppression
	member.ReceivedContactDamageHalved = snapshot.ReceivedContactDamageHalved
	member.ReceivedFireDamageDoubled = snapshot.ReceivedFireDamageDoubled
	member.ForcedSwitchImmunity = snapshot.ForcedSwitchImmunity
	member.OpponentSwitchRestriction = cloneOpponentSwitchRestriction(snapshot.OpponentSwitchRestriction)
	member.DamageCrossedHalfHPForceSelfSwitch = snapshot.DamageCrossedHalfHPForceSelfSwitch
	member.SwitchOutMajorStatusCure = snapshot.SwitchOutMajorStatusCure
	member.SwitchOutHealDenominator = snapshot.SwitchOutHealDenominator
	member.WeatherEndTurnHealing = cloneWeatherEndTurnHealing(snapshot.WeatherEndTurnHealing)
	member.WeatherSpeedMultipliers = append([]WeatherSpeedMultiplier(nil), snapshot.WeatherSpeedMultipliers...)
	member.EnvironmentHighestStatMultiplier = cloneEnvironmentHighestStatMultiplier(snapshot.EnvironmentHighestStatMultiplier)
	member.TerastallizationStatStageChange = cloneTerastallizationStatStageChange(snapshot.TerastallizationStatStageChange)
	member.TerastallizationEnvironmentClear = snapshot.TerastallizationEnvironmentClear
	member.SwitchInStrongWeather = snapshot.SwitchInStrongWeather
	member.SwitchInWeather = cloneSwitchInWeather(snapshot.SwitchInWeather)
	member.SwitchInTerrain = cloneSwitchInTerrain(snapshot.SwitchInTerrain)
	member.SwitchInStatStageChange = cloneSwitchInStatStageChange(snapshot.SwitchInStatStageChange)
	member.SwitchInAllyHeal = cloneSwitchInAllyHeal(snapshot.SwitchInAllyHeal)
	member.SwitchInOpponentDefenseComparisonBoost = snapshot.SwitchInOpponentDefenseComparisonBoost
	member.SwitchInAllyStatStageCopy = snapshot.SwitchInAllyStatStageCopy
	member.SwitchInAllyStatStageReset = snapshot.SwitchInAllyStatStageReset
	member.SwitchInClearAllSideDamageReductions = snapshot.SwitchInClearAllSideDamageReductions
	member.SwitchInCopyOpponentAbility = snapshot.SwitchInCopyOpponentAbility
	member.SwitchInRevealOpponentHeldItems = snapshot.SwitchInRevealOpponentHeldItems
	member.SwitchInRevealOpponentHighestPowerSkill = snapshot.SwitchInRevealOpponentHighestPowerSkill
	member.SwitchInTransformIntoOpponent = snapshot.SwitchInTransformIntoOpponent
	member.SwitchInDetectDangerousOpponentSkill = snapshot.SwitchInDetectDangerousOpponentSkill
	member.SwitchInDisguiseAsLastHealthyAlly = snapshot.SwitchInDisguiseAsLastHealthyAlly
	member.SwitchInHeldItemElementIdentity = snapshot.SwitchInHeldItemElementIdentity
	member.SwitchRestrictionImmunity = snapshot.SwitchRestrictionImmunity
	member.ContactSideEffectImmunity = snapshot.ContactSideEffectImmunity
	member.HeldItemContactDamageToAttackerDenominator = snapshot.HeldItemContactDamageToAttackerDenominator
	member.HeldItemEndTurnHealDenominator = snapshot.HeldItemEndTurnHealDenominator
	member.HeldItemEndTurnHealForElementID = snapshot.HeldItemEndTurnHealForElementID
	member.HeldItemEndTurnHealForElementDenominator = snapshot.HeldItemEndTurnHealForElementDenominator
	member.HeldItemEndTurnDamageDenominator = snapshot.HeldItemEndTurnDamageDenominator
	member.HeldItemEndTurnDamageWithoutElementID = snapshot.HeldItemEndTurnDamageWithoutElementID
	member.HeldItemEndTurnDamageWithoutElementDenominator = snapshot.HeldItemEndTurnDamageWithoutElementDenominator
	member.HeldItemConsumableElementDamageBoostElementID = snapshot.HeldItemConsumableElementDamageBoostElementID
	member.HeldItemConsumableElementDamageBoostNumerator = snapshot.HeldItemConsumableElementDamageBoostNumerator
	member.HeldItemConsumableElementDamageBoostDenominator = snapshot.HeldItemConsumableElementDamageBoostDenominator
	member.ContactTransferToAttacker = snapshot.ContactTransferToAttacker
	member.ChargeSkipOnce = snapshot.ChargeSkipOnce
	member.HeldItemSurviveFatalDamageAtFullHP = snapshot.HeldItemSurviveFatalDamageAtFullHP
	member.HeldItemReflectTurnsRemaining = snapshot.HeldItemReflectTurnsRemaining
	member.HeldItemLightScreenTurnsRemaining = snapshot.HeldItemLightScreenTurnsRemaining
	member.HeldItemAuroraVeilTurnsRemaining = snapshot.HeldItemAuroraVeilTurnsRemaining
	member.HeldItemRainTurnsRemaining = snapshot.HeldItemRainTurnsRemaining
	member.HeldItemSandstormTurnsRemaining = snapshot.HeldItemSandstormTurnsRemaining
	member.HeldItemSnowTurnsRemaining = snapshot.HeldItemSnowTurnsRemaining
	member.HeldItemSunTurnsRemaining = snapshot.HeldItemSunTurnsRemaining
	member.HeldItemTerrainTurnsRemaining = snapshot.HeldItemTerrainTurnsRemaining
	member.HeldItemSandstormDamageImmunity = snapshot.HeldItemSandstormDamageImmunity
	member.HeldItemEntryHazardImmunity = snapshot.HeldItemEntryHazardImmunity
	member.HeldItemWeightHalf = snapshot.HeldItemWeightHalf
	member.HeldItemCuresParalysis = snapshot.HeldItemCuresParalysis
	member.HeldItemCuresSleep = snapshot.HeldItemCuresSleep
	member.HeldItemCuresPoison = snapshot.HeldItemCuresPoison
	member.HeldItemCuresBurn = snapshot.HeldItemCuresBurn
	member.HeldItemCuresFreeze = snapshot.HeldItemCuresFreeze
	member.HeldItemCuresAllMajorStatuses = snapshot.HeldItemCuresAllMajorStatuses
	member.HeldItemCuresConfusion = snapshot.HeldItemCuresConfusion
	member.HeldItemPunchBasedSkillPowerBoost = snapshot.HeldItemPunchBasedSkillPowerBoost
	member.HeldItemPhysicalDamagePowerBoost = snapshot.HeldItemPhysicalDamagePowerBoost
	member.HeldItemSpecialDamagePowerBoost = snapshot.HeldItemSpecialDamagePowerBoost
	member.HeldItemElementDamageReductionElementID = snapshot.HeldItemElementDamageReductionElementID
	member.HeldItemElementDamageReductionRequiresSuperEffective = snapshot.HeldItemElementDamageReductionRequiresSuperEffective
	member.HeldItemSuperEffectiveDamageBoost = snapshot.HeldItemSuperEffectiveDamageBoost
	member.HeldItemDamageBoostWithRecoil = snapshot.HeldItemDamageBoostWithRecoil
	member.HeldItemDamageDealtHeal = snapshot.HeldItemDamageDealtHeal
	member.HeldItemDrainHealingBoost = snapshot.HeldItemDrainHealingBoost
	member.HeldItemAccuracyBoost = snapshot.HeldItemAccuracyBoost
	member.HeldItemOpponentAccuracyReduction = snapshot.HeldItemOpponentAccuracyReduction
	member.HeldItemCriticalHitStageBoost = snapshot.HeldItemCriticalHitStageBoost
	member.HeldItemAirborneUntilDamaged = snapshot.HeldItemAirborneUntilDamaged
	member.HeldItemForceGrounded = snapshot.HeldItemForceGrounded
	member.HeldItemSpeedHalf = snapshot.HeldItemSpeedHalf
	member.HeldItemSpecialDefenseBoost = snapshot.HeldItemSpecialDefenseBoost
	member.HeldItemStatusSkillRestriction = snapshot.HeldItemStatusSkillRestriction
	member.HeldItemPhysicalDamagePowerBoost50 = snapshot.HeldItemPhysicalDamagePowerBoost50
	member.HeldItemSpecialDamagePowerBoost50 = snapshot.HeldItemSpecialDamagePowerBoost50
	member.HeldItemChoiceSkillLock = snapshot.HeldItemChoiceSkillLock
	member.HeldItemSpeedBoost50 = snapshot.HeldItemSpeedBoost50
	member.HeldItemAccuracyAfterTargetActedBoost = snapshot.HeldItemAccuracyAfterTargetActedBoost
	member.HeldItemTypeImmunitySuppression = snapshot.HeldItemTypeImmunitySuppression
	member.HeldItemOpponentStatStageReductionImmunity = snapshot.HeldItemOpponentStatStageReductionImmunity
	member.HeldItemNegativeStatStageReset = snapshot.HeldItemNegativeStatStageReset
	member.HeldItemAbilityStatReductionSpeedBoost = snapshot.HeldItemAbilityStatReductionSpeedBoost
	member.HeldItemOpponentPositiveStatStageCopy = snapshot.HeldItemOpponentPositiveStatStageCopy
	member.HeldItemDamagingSkillSecondaryEffectImmunity = snapshot.HeldItemDamagingSkillSecondaryEffectImmunity
	member.HeldItemBindingTurns = snapshot.HeldItemBindingTurns
	member.HeldItemBindingDamageDenominator = snapshot.HeldItemBindingDamageDenominator
	member.HeldItemAccuracyMissStatStageBoostStat = snapshot.HeldItemAccuracyMissStatStageBoostStat
	member.HeldItemAccuracyMissStatStageBoostDelta = snapshot.HeldItemAccuracyMissStatStageBoostDelta
	member.HeldItemWeaknessPolicy = snapshot.HeldItemWeaknessPolicy
	member.HeldItemWaterDamageSpecialAttackBoostElementID = snapshot.HeldItemWaterDamageSpecialAttackBoostElementID
	member.HeldItemElectricDamageAttackBoostElementID = snapshot.HeldItemElectricDamageAttackBoostElementID
	member.HeldItemWaterDamageSpecialDefenseBoostElementID = snapshot.HeldItemWaterDamageSpecialDefenseBoostElementID
	member.HeldItemIceDamageAttackBoostElementID = snapshot.HeldItemIceDamageAttackBoostElementID
	member.HeldItemAdditionalFlinchChancePercent = snapshot.HeldItemAdditionalFlinchChancePercent
	member.HeldItemRandomActionOrderBoostChancePercent = snapshot.HeldItemRandomActionOrderBoostChancePercent
	member.HeldItemForcedLastActionOrder = snapshot.HeldItemForcedLastActionOrder
	member.HeldItemLowHPActionOrderBoost = snapshot.HeldItemLowHPActionOrderBoost
	member.HeldItemFieldSpeedOrderSpeedStageDrop = snapshot.HeldItemFieldSpeedOrderSpeedStageDrop
	member.HeldItemConsecutiveSkillDamageBoost = snapshot.HeldItemConsecutiveSkillDamageBoost
	member.HeldItemPunchBasedContactSuppression = snapshot.HeldItemPunchBasedContactSuppression
	member.HeldItemPowderSkillImmunity = snapshot.HeldItemPowderSkillImmunity
	member.HeldItemMultiHitCountMinimum = snapshot.HeldItemMultiHitCountMinimum
	member.HeldItemMultiHitCountMaximum = snapshot.HeldItemMultiHitCountMaximum
	member.HeldItemMultiHitRequiredMinimum = snapshot.HeldItemMultiHitRequiredMinimum
	member.HeldItemMultiHitRequiredMaximum = snapshot.HeldItemMultiHitRequiredMaximum
	member.HeldItemElementID = snapshot.HeldItemElementID
	member.HeldItemElementIdentityBaseElementIDs = append([]Identifier(nil), snapshot.HeldItemElementIdentityBaseElementIDs...)
	member.SwitchInFormChange = cloneSwitchInFormChange(snapshot.SwitchInFormChange)
	member.SwitchOutFormChange = cloneSwitchOutFormChange(snapshot.SwitchOutFormChange)
	member.WeatherFormChange = cloneWeatherFormChange(snapshot.WeatherFormChange)
	member.TransformSnapshot = nil
	return member
}

// cloneStatStages 深复制可变能力阶级，避免变身者与来源成员共享同一张运行态映射。
func cloneStatStages(stages map[Stat]int8) map[Stat]int8 {
	cloned := make(map[Stat]int8, len(stages))
	for stat, stage := range stages {
		cloned[stat] = stage
	}
	return cloned
}

// cloneSkillSnapshots 深复制技能数组及其中的效果集合，保证变身前后技能快照彼此隔离。
func cloneSkillSnapshots(skills []SkillSnapshot) []SkillSnapshot {
	cloned := append([]SkillSnapshot(nil), skills...)
	for index := range cloned {
		cloned[index].StatusApplications = append([]MajorStatusApplication(nil), cloned[index].StatusApplications...)
		cloned[index].StatStageEffects = append([]StatStageEffect(nil), cloned[index].StatStageEffects...)
		cloned[index].VolatileStatusApplications = append([]VolatileStatusApplication(nil), cloned[index].VolatileStatusApplications...)
		cloned[index].WeatherAccuracyOverrides = append([]WeatherAccuracyOverride(nil), cloned[index].WeatherAccuracyOverrides...)
		cloned[index].WeatherElementOverrides = append([]WeatherElementOverride(nil), cloned[index].WeatherElementOverrides...)
		cloned[index].WeatherPowerMultipliers = append([]WeatherPowerMultiplier(nil), cloned[index].WeatherPowerMultipliers...)
		cloned[index].ChargeSkippedWeathers = append([]WeatherKind(nil), cloned[index].ChargeSkippedWeathers...)
		cloned[index].DynamicPower = cloneDynamicPowerRule(cloned[index].DynamicPower)
		cloned[index].LeechSeedApplication = cloneLeechSeedApplication(cloned[index].LeechSeedApplication)
		cloned[index].FieldSpeedOrderApplication = cloneFieldSpeedOrderApplication(cloned[index].FieldSpeedOrderApplication)
		cloned[index].WeatherApplication = cloneWeatherApplication(cloned[index].WeatherApplication)
		cloned[index].TerrainApplication = cloneTerrainApplication(cloned[index].TerrainApplication)
		cloned[index].TailwindApplication = cloneTailwindApplication(cloned[index].TailwindApplication)
		cloned[index].ReflectApplication = cloneReflectApplication(cloned[index].ReflectApplication)
		cloned[index].LightScreenApplication = cloneLightScreenApplication(cloned[index].LightScreenApplication)
		cloned[index].AuroraVeilApplication = cloneAuroraVeilApplication(cloned[index].AuroraVeilApplication)
		cloned[index].SpikesApplication = cloneSpikesApplication(cloned[index].SpikesApplication)
		cloned[index].StealthRockApplication = cloneStealthRockApplication(cloned[index].StealthRockApplication)
		cloned[index].ToxicSpikesApplication = cloneToxicSpikesApplication(cloned[index].ToxicSpikesApplication)
		cloned[index].StickyWebApplication = cloneStickyWebApplication(cloned[index].StickyWebApplication)
		cloned[index].RapidSpinApplication = cloneRapidSpinApplication(cloned[index].RapidSpinApplication)
		cloned[index].DefogApplication = cloneDefogApplication(cloned[index].DefogApplication)
	}
	return cloned
}

// cloneLeechSeedApplication 等一组小型复制函数保持变身技能快照的指针成员隔离。
func cloneLeechSeedApplication(value *LeechSeedApplication) *LeechSeedApplication {
	return cloneValue(value)
}

func cloneFieldSpeedOrderApplication(value *FieldSpeedOrderApplication) *FieldSpeedOrderApplication {
	return cloneValue(value)
}

func cloneWeatherApplication(value *WeatherApplication) *WeatherApplication { return cloneValue(value) }

func cloneTerrainApplication(value *TerrainApplication) *TerrainApplication { return cloneValue(value) }

func cloneTailwindApplication(value *TailwindApplication) *TailwindApplication {
	return cloneValue(value)
}

func cloneReflectApplication(value *ReflectApplication) *ReflectApplication { return cloneValue(value) }

func cloneLightScreenApplication(value *LightScreenApplication) *LightScreenApplication {
	return cloneValue(value)
}

func cloneAuroraVeilApplication(value *AuroraVeilApplication) *AuroraVeilApplication {
	return cloneValue(value)
}

func cloneSpikesApplication(value *SpikesApplication) *SpikesApplication { return cloneValue(value) }

func cloneStealthRockApplication(value *StealthRockApplication) *StealthRockApplication {
	return cloneValue(value)
}

func cloneToxicSpikesApplication(value *ToxicSpikesApplication) *ToxicSpikesApplication {
	return cloneValue(value)
}

func cloneStickyWebApplication(value *StickyWebApplication) *StickyWebApplication {
	return cloneValue(value)
}

func cloneRapidSpinApplication(value *RapidSpinApplication) *RapidSpinApplication {
	return cloneValue(value)
}

func cloneDefogApplication(value *DefogApplication) *DefogApplication { return cloneValue(value) }

// cloneValue 复制不包含引用字段的规则对象；所有当前规则对象均为值结构。
func cloneValue[T any](value *T) *T {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
