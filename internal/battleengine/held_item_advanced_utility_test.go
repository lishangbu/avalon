package battleengine

import (
	"errors"
	"testing"
)

// TestHeldItemChoiceDamageBoosts 验证讲究头带与讲究眼镜只把匹配分类的普通威力提高 50%。
func TestHeldItemChoiceDamageBoosts(t *testing.T) {
	t.Parallel()
	physical := MemberSnapshot{ItemID: testID("choice-band"), HeldItemPhysicalDamagePowerBoost50: true}
	special := MemberSnapshot{ItemID: testID("choice-specs"), HeldItemSpecialDamagePowerBoost50: true}
	if got := heldItemDamageClassPowerBoost(100, physical, DamageClassPhysical); got != 150 || heldItemDamageClassPowerBoost(100, physical, DamageClassSpecial) != 100 {
		t.Fatalf("讲究头带威力 = %d", got)
	}
	if got := heldItemDamageClassPowerBoost(100, special, DamageClassSpecial); got != 150 || heldItemDamageClassPowerBoost(100, special, DamageClassPhysical) != 100 {
		t.Fatalf("讲究眼镜威力 = %d", got)
	}
}

// TestHeldItemChoiceSpeedBoost 验证讲究围巾在其它速度修正之后提供 3/2 倍速度。
func TestHeldItemChoiceSpeedBoost(t *testing.T) {
	t.Parallel()
	member := MemberSnapshot{ItemID: testID("choice-scarf"), HeldItemSpeedBoost50: true, Stats: StatBlock{Speed: 101}}
	if got := effectiveSpeed(member); got != 151 {
		t.Fatalf("讲究围巾有效速度 = %d，期望 151", got)
	}
}

// TestHeldItemZoomLensAccuracyBoost 验证目标已在本回合宣告技能时后手命中道具提供 6/5 倍命中率。
func TestHeldItemZoomLensAccuracyBoost(t *testing.T) {
	t.Parallel()
	actor := MemberSnapshot{ItemID: testID("zoom-lens"), HeldItemAccuracyAfterTargetActedBoost: true, LastSkillActionTurn: 1}
	target := MemberSnapshot{LastSkillActionTurn: 1}
	if got := skillAccuracy(nil, actor, target, SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Accuracy: 80, DamageClass: DamageClassPhysical}); got != 96 {
		t.Fatalf("后手命中率 = %d，期望 96", got)
	}
}

// TestHeldItemTypeImmunitySuppression 验证标靶类道具只把由自身属性提供的零倍伤害改为等倍。
func TestHeldItemTypeImmunitySuppression(t *testing.T) {
	t.Parallel()
	rules := RuleSnapshot{ElementEffectiveness: []ElementEffectiveness{{AttackElementID: testID("ground"), DefenseElementID: testID("flying"), Numerator: 0, Denominator: 1}}}
	attacker := heldItemAdvancedMember(1, "attacker")
	attacker.ElementIDs = testIDs("ground")
	target := heldItemAdvancedMember(1, "target")
	target.ElementIDs = testIDs("flying")
	skill := attacker.Skills[0]
	skill.ElementID = testID("ground")
	plain := calculateDamage(rules, nil, nil, nil, SideConditionSnapshot{}, FormatSnapshot{ActiveSlotsPerSide: 1}, attacker, target, skill, 100, false, false)
	target.ItemID, target.HeldItemTypeImmunitySuppression = testID("ring-target"), true
	withItem := calculateDamage(rules, nil, nil, nil, SideConditionSnapshot{}, FormatSnapshot{ActiveSlotsPerSide: 1}, attacker, target, skill, 100, false, false)
	if plain != 0 || withItem == 0 {
		t.Fatalf("属性免疫抑制伤害 = %d/%d", plain, withItem)
	}
}

// TestHeldItemOpponentStatStageReductionImmunity 验证清净坠饰阻止对手技能降阶。
func TestHeldItemOpponentStatStageReductionImmunity(t *testing.T) {
	t.Parallel()
	state := heldItemAdvancedState(t)
	target, _ := state.member(SideTwo, 1)
	target.ItemID, target.HeldItemOpponentStatStageReductionImmunity = testID("clear-amulet"), true
	state.replaceMember(SideTwo, target)
	skill := SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, SkillID: testID("drop"), StatStageEffects: []StatStageEffect{{Stat: StatAttack, StageDelta: -1, ChancePercent: 100, Target: EffectTargetSelected}}}
	random, _ := NewRandomSource(RandomAlgorithmSplitMix64V1, 1)
	state, _, _, _, err := resolveStatStageEffects(state, MemberRef{Side: SideOne, Position: 1}, MemberRef{Side: SideTwo, Position: 1}, skill, random, true, false)
	final, _ := state.member(SideTwo, 1)
	if err != nil || final.StatStages[StatAttack] != 0 {
		t.Fatalf("清净坠饰降阶结果 = %+v, err=%v", final.StatStages, err)
	}
}

// TestHeldItemNegativeStatStageReset 验证白色香草清零全部负阶级并消费，正阶级保持。
func TestHeldItemNegativeStatStageReset(t *testing.T) {
	t.Parallel()
	state := heldItemAdvancedState(t)
	target, _ := state.member(SideTwo, 1)
	target.ItemID, target.HeldItemNegativeStatStageReset = testID("white-herb"), true
	target.StatStages = map[Stat]int8{StatAttack: -2, StatDefense: 2}
	state.replaceMember(SideTwo, target)
	state, events := applyHeldItemNegativeStatStageReset(state, MemberRef{Side: SideTwo, Position: 1})
	final, _ := state.member(SideTwo, 1)
	if final.ItemID != 0 || final.StatStages[StatAttack] != 0 || final.StatStages[StatDefense] != 2 || len(events) != 2 || events[len(events)-1].Kind() != EventKindHeldItemStatReactionConsumed {
		t.Fatalf("白色香草结果 = %+v events=%d", final, len(events))
	}
}

// TestHeldItemAbilityStatReductionSpeedBoost 验证胆怯球补偿速度强化并消费。
func TestHeldItemAbilityStatReductionSpeedBoost(t *testing.T) {
	t.Parallel()
	state := heldItemAdvancedState(t)
	target, _ := state.member(SideTwo, 1)
	target.ItemID, target.HeldItemAbilityStatReductionSpeedBoost = testID("adrenaline-orb"), true
	state.replaceMember(SideTwo, target)
	state, events := applyHeldItemAbilityStatReductionSpeedBoost(state, MemberRef{Side: SideTwo, Position: 1})
	final, _ := state.member(SideTwo, 1)
	if final.ItemID != 0 || final.StatStages[StatSpeed] != 1 || len(events) != 2 || events[len(events)-1].Kind() != EventKindHeldItemStatReactionConsumed {
		t.Fatalf("胆怯球结果 = %+v events=%d", final, len(events))
	}
}

// TestHeldItemOpponentPositiveStatStageCopy 验证模仿香草覆盖复制对手全部正阶级并消费。
func TestHeldItemOpponentPositiveStatStageCopy(t *testing.T) {
	t.Parallel()
	state := heldItemAdvancedState(t)
	source, _ := state.member(SideOne, 1)
	source.StatStages = map[Stat]int8{StatAttack: 2, StatDefense: -1}
	state.replaceMember(SideOne, source)
	target, _ := state.member(SideTwo, 1)
	target.ItemID, target.HeldItemOpponentPositiveStatStageCopy = testID("mirror-herb"), true
	state.replaceMember(SideTwo, target)
	state, events := applyHeldItemOpponentPositiveStatStageCopy(state, MemberRef{Side: SideOne, Position: 1})
	final, _ := state.member(SideTwo, 1)
	if final.ItemID != 0 || final.StatStages[StatAttack] != 2 || final.StatStages[StatDefense] != 0 || len(events) != 2 || events[len(events)-1].Kind() != EventKindHeldItemStatReactionConsumed {
		t.Fatalf("模仿香草结果 = %+v events=%d", final, len(events))
	}
}

// TestHeldItemChoiceSkillLock 验证讲究类道具在第一次实际宣告后拒绝其它技能槽。
func TestHeldItemChoiceSkillLock(t *testing.T) {
	state := heldItemAdvancedState(t)
	actor, _ := state.member(SideOne, 1)
	actor.ItemID, actor.HeldItemChoiceSkillLock = testID("choice-item"), true
	actor.Skills = append(actor.Skills, actor.Skills[0])
	actor.Skills[1].Position, actor.Skills[1].SkillID = 2, testID("second")
	state.replaceMember(SideOne, actor)
	random, _ := NewRandomSource(RandomAlgorithmSplitMix64V1, 7)
	result, err := ResolveTurn(state, heldItemAdvancedCommand(1, 1), random)
	if err != nil {
		t.Fatalf("首次宣告 error=%v", err)
	}
	_, err = ResolveTurn(result.State, heldItemAdvancedCommand(2, 2), result.RandomSource)
	if !errors.Is(err, ErrInvalidTurnCommand) {
		t.Fatalf("改选技能 error=%v", err)
	}
}

// TestHeldItemAdvancedUtilityLifecycle 验证高级道具投影在转移时整体迁移、连续上场锁定不迁移，并可由变身快照还原。
func TestHeldItemAdvancedUtilityLifecycle(t *testing.T) {
	t.Parallel()
	source := heldItemAdvancedMember(1, "source")
	source.ItemID = testID("advanced-item")
	source.HeldItemPhysicalDamagePowerBoost50 = true
	source.HeldItemSpecialDamagePowerBoost50 = true
	source.HeldItemChoiceSkillLock = true
	source.HeldItemChoiceLockedSkillPosition = 1
	source.HeldItemSpeedBoost50 = true
	source.HeldItemAccuracyAfterTargetActedBoost = true
	source.HeldItemTypeImmunitySuppression = true
	source.HeldItemOpponentStatStageReductionImmunity = true
	source.HeldItemNegativeStatStageReset = true
	source.HeldItemAbilityStatReductionSpeedBoost = true
	source.HeldItemOpponentPositiveStatStageCopy = true
	source.HeldItemDamagingSkillSecondaryEffectImmunity = true
	source.HeldItemBindingTurns = 7
	source.HeldItemBindingDamageDenominator = 6
	source.HeldItemAccuracyMissStatStageBoostStat, source.HeldItemAccuracyMissStatStageBoostDelta = StatSpeed, 2
	source.HeldItemWeaknessPolicy = true
	source.HeldItemWaterDamageSpecialAttackBoostElementID = testID("water")
	source.HeldItemElectricDamageAttackBoostElementID = testID("electric")
	source.HeldItemWaterDamageSpecialDefenseBoostElementID = testID("water")
	source.HeldItemIceDamageAttackBoostElementID = testID("ice")
	source.HeldItemAdditionalFlinchChancePercent = 10
	source.HeldItemRandomActionOrderBoostChancePercent = 20
	source.HeldItemForcedLastActionOrder = true
	source.HeldItemLowHPActionOrderBoost = true
	source.HeldItemFieldSpeedOrderSpeedStageDrop = true
	source.HeldItemConsecutiveSkillDamageBoost = true
	received := receiveTransferredHeldItem(heldItemAdvancedMember(1, "receiver"), source.ItemID, source)
	if !heldItemAdvancedFlags(received) || received.HeldItemChoiceLockedSkillPosition != 0 {
		t.Fatalf("转移后的高级道具投影 = %+v", received)
	}
	snapshot := newMemberTransformSnapshot(source)
	cleared := clearHeldItemRuntimeState(source)
	cleared.TransformSnapshot = snapshot
	restored := restoreTransformSnapshot(cleared)
	if !heldItemAdvancedFlags(restored) {
		t.Fatalf("变身还原后的高级道具投影 = %+v", restored)
	}
}

// heldItemAdvancedFlags 判断成员是否携带本批全部高级道具投影。
func heldItemAdvancedFlags(member MemberSnapshot) bool {
	return member.HeldItemPhysicalDamagePowerBoost50 && member.HeldItemSpecialDamagePowerBoost50 && member.HeldItemChoiceSkillLock && member.HeldItemSpeedBoost50 &&
		member.HeldItemAccuracyAfterTargetActedBoost && member.HeldItemTypeImmunitySuppression && member.HeldItemOpponentStatStageReductionImmunity &&
		member.HeldItemNegativeStatStageReset && member.HeldItemAbilityStatReductionSpeedBoost && member.HeldItemOpponentPositiveStatStageCopy &&
		member.HeldItemDamagingSkillSecondaryEffectImmunity && member.HeldItemBindingTurns == 7 && member.HeldItemBindingDamageDenominator == 6 &&
		member.HeldItemAccuracyMissStatStageBoostStat == StatSpeed && member.HeldItemAccuracyMissStatStageBoostDelta == 2 && member.HeldItemWeaknessPolicy && member.HeldItemWaterDamageSpecialAttackBoostElementID == testID("water") &&
		member.HeldItemElectricDamageAttackBoostElementID == testID("electric") && member.HeldItemAdditionalFlinchChancePercent == 10 &&
		member.HeldItemWaterDamageSpecialDefenseBoostElementID == testID("water") && member.HeldItemIceDamageAttackBoostElementID == testID("ice") &&
		member.HeldItemRandomActionOrderBoostChancePercent == 20 && member.HeldItemForcedLastActionOrder && member.HeldItemLowHPActionOrderBoost &&
		member.HeldItemFieldSpeedOrderSpeedStageDrop && member.HeldItemConsecutiveSkillDamageBoost
}

// heldItemAdvancedState 构造双方各一名成员的确定性引擎状态。
func heldItemAdvancedState(t *testing.T) State {
	t.Helper()
	state, err := NewState(InitialState{Format: FormatSnapshot{Code: "held-item-advanced", ActiveSlotsPerSide: 1, TeamSize: 1}, Rules: RuleSnapshot{SchemaVersion: 1}, Sides: []SideSnapshot{{Side: SideOne, ActiveMembers: []MemberPosition{1}, Members: []MemberSnapshot{heldItemAdvancedMember(1, "one")}}, {Side: SideTwo, ActiveMembers: []MemberPosition{1}, Members: []MemberSnapshot{heldItemAdvancedMember(1, "two")}}}})
	if err != nil {
		t.Fatalf("NewState() error=%v", err)
	}
	return state
}

// heldItemAdvancedMember 构造带一个普通伤害技能的有效成员快照。
func heldItemAdvancedMember(position MemberPosition, id string) MemberSnapshot {
	return MemberSnapshot{Position: position, CreatureID: testID(id), Level: 50, MaxHP: 500, CurrentHP: 500, Stats: StatBlock{Attack: 100, Defense: 100, SpecialAttack: 100, SpecialDefense: 100, Speed: 100}, ElementIDs: testIDs("normal"), Skills: []SkillSnapshot{{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("first"), Name: "攻击", ElementID: testID("normal"), DamageClass: DamageClassPhysical, Power: 40, Accuracy: 100, RemainingPP: 20, MaxPP: 20}}}
}

// heldItemAdvancedCommand 构造双方均使用技能的完整回合命令。
func heldItemAdvancedCommand(turn uint32, actorSkill SkillPosition) TurnCommand {
	return TurnCommand{SchemaVersion: 1, TurnNumber: turn, Actions: []Action{{Kind: ActionKindUseSkill, Actor: SlotRef{Side: SideOne, Position: 1}, UseSkill: &UseSkillAction{SkillPosition: actorSkill, Target: SlotRef{Side: SideTwo, Position: 1}}}, {Kind: ActionKindUseSkill, Actor: SlotRef{Side: SideTwo, Position: 1}, UseSkill: &UseSkillAction{SkillPosition: 1, Target: SlotRef{Side: SideOne, Position: 1}}}}}
}
