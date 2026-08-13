package battleengine_test

import (
	"fmt"
	"reflect"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStateCopyOpponentAbilityIncludesFormulaAndAllyRules 验证规则 112—131 的每个独立特性字段都由
// 入场复制取得，并且包含切片的规则不会与调用方传入的资料对象共享底层存储。
func TestInitialStateCopyOpponentAbilityIncludesFormulaAndAllyRules(t *testing.T) {
	t.Parallel()
	receiver := newMember(1, "formula-copy-receiver", 500, 500)
	receiver.AbilityID = testID("copy-opponent-ability")
	receiver.SwitchInCopyOpponentAbility = true
	source := newMember(1, "formula-copy-source", 500, 500)
	source.AbilityID = testID("formula-source-ability")
	setFormulaAbilityLifecycleRules(&source, 1)
	expected := newMember(1, "formula-copy-expected", 500, 500)
	setFormulaAbilityLifecycleRules(&expected, 1)

	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "formula-copy-lifecycle", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{receiver}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{source}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	copied, found := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || copied.AbilityID != source.AbilityID || !formulaAbilityLifecycleRulesEqual(copied, expected) {
		t.Fatalf("入场复制未保留全部公式与伙伴规则: %+v", copied)
	}

	// 调用方继续修改建局前使用的对象，不得反向篡改 State 已冻结的特性规则。
	source.TargetGenderDamageMultiplier.SameGenderNumerator = 99
	source.DamageClassDamageReduction.DamageClasses[0] = battleengine.DamageClassSpecial
	source.ElementSkillDamageReduction.ElementIDs[0] = testID("mutated-element")
	source.AttackingStatMultiplier.RequiredMajorStatuses[0] = battleengine.MajorStatusFreeze
	source.AllySkillDamageBoost.DamageClasses[0] = battleengine.DamageClassSpecial
	copied, found = state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || !formulaAbilityLifecycleRulesEqual(copied, expected) {
		t.Fatalf("复制后的独立规则错误共享了资料切片或指针: %+v", copied)
	}
}

// TestTransformFormulaAndAllyRulesRestoreAfterLeaving 验证变身会按值取得目标当前特性的规则 112—131，
// 同时在 TransformSnapshot 保存自身原规则，并在离场后完整恢复，不能把目标规则泄漏到后备状态。
func TestTransformFormulaAndAllyRulesRestoreAfterLeaving(t *testing.T) {
	t.Parallel()
	first := newMember(1, "formula-transform-first", 500, 500)
	incoming := newMember(2, "formula-transform-incoming", 500, 500)
	incoming.SwitchInTransformIntoOpponent = true
	setFormulaAbilityLifecycleRules(&incoming, 1)
	reserve := newMember(3, "formula-transform-reserve", 500, 500)
	target := newMember(1, "formula-transform-target", 500, 500)
	setFormulaAbilityLifecycleRules(&target, 2)
	target.MajorStatus = battleengine.MajorStatusSleep
	target.SleepTurnsRemaining = 3

	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "formula-transform-lifecycle", ActiveSlotsPerSide: 1, TeamSize: 3},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first, incoming, reserve}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	firstTurn, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
		},
	}, mustRandom(t, 811))
	if err != nil {
		t.Fatalf("ResolveTurn(turn 1) error = %v", err)
	}
	transformed, found := firstTurn.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || transformed.TransformSnapshot == nil ||
		!formulaAbilityLifecycleRulesEqual(transformed, target) ||
		!formulaAbilityLifecycleSnapshotEqual(*transformed.TransformSnapshot, incoming) {
		t.Fatalf("变身后的当前规则或原规则快照不完整: %+v", transformed)
	}

	secondTurn, err := battleengine.ResolveTurn(firstTurn.State, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 2,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 3}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
		},
	}, mustRandom(t, 812))
	if err != nil {
		t.Fatalf("ResolveTurn(turn 2) error = %v", err)
	}
	var restored battleengine.MemberSnapshot
	for _, member := range secondTurn.State.Snapshot().Sides[0].Members {
		if member.Position == incoming.Position {
			restored = member
			break
		}
	}
	if restored.CreatureID != incoming.CreatureID || restored.TransformSnapshot != nil ||
		!formulaAbilityLifecycleRulesEqual(restored, incoming) {
		t.Fatalf("变身者离场后未恢复自身独立公式与伙伴规则: %+v", restored)
	}
}

// setFormulaAbilityLifecycleRules 为生命周期测试构造一组彼此独立且可区分的规则 112—131 字段。
// variant 只改变合法字段值，使变身测试能同时辨认“自身原规则”和“目标当前规则”。
func setFormulaAbilityLifecycleRules(member *battleengine.MemberSnapshot, variant uint16) {
	numerator := variant + 1
	member.TargetGenderDamageMultiplier = &battleengine.TargetGenderDamageMultiplier{
		SameGenderNumerator: numerator, SameGenderDenominator: 1,
		OppositeGenderNumerator: numerator + 1, OppositeGenderDenominator: 1,
	}
	member.PunchBasedSkillDamageBoost = &battleengine.PunchBasedSkillDamageBoost{Numerator: numerator, Denominator: 1}
	member.SlicingBasedSkillDamageBoost = &battleengine.SlicingBasedSkillDamageBoost{Numerator: numerator, Denominator: 1}
	member.SoundBasedSkillDamageBoost = &battleengine.SoundBasedSkillDamageBoost{Numerator: numerator, Denominator: 1}
	member.PulseBasedSkillDamageBoost = &battleengine.PulseBasedSkillDamageBoost{Numerator: numerator, Denominator: 1}
	member.BiteBasedSkillDamageBoost = &battleengine.BiteBasedSkillDamageBoost{Numerator: numerator, Denominator: 1}
	member.SecondaryEffectsSuppressedDamageBoost = &battleengine.SecondaryEffectsSuppressedDamageBoost{Numerator: numerator, Denominator: 1}
	member.SoundBasedSkillDamageReduction = &battleengine.SoundBasedSkillDamageReduction{Numerator: numerator, Denominator: 1}
	member.SuperEffectiveDamageReduction = &battleengine.SuperEffectiveDamageReduction{Numerator: numerator, Denominator: 1}
	member.FullHPDamageReduction = &battleengine.FullHPDamageReduction{Numerator: numerator, Denominator: 1}
	damageClass := battleengine.DamageClassPhysical
	attackStat, defenseStat := battleengine.StatAttack, battleengine.StatDefense
	majorStatus := battleengine.MajorStatusBurn
	weather, terrain := battleengine.WeatherKindSun, battleengine.TerrainKindElectric
	if variant%2 == 0 {
		damageClass = battleengine.DamageClassSpecial
		attackStat, defenseStat = battleengine.StatSpecialAttack, battleengine.StatSpecialDefense
		majorStatus = battleengine.MajorStatusPoison
		weather, terrain = battleengine.WeatherKindRain, battleengine.TerrainKindGrassy
	}
	member.DamageClassDamageReduction = &battleengine.DamageClassDamageReduction{
		DamageClasses: []battleengine.DamageClass{damageClass}, Numerator: numerator, Denominator: 1,
	}
	member.ElementSkillDamageReduction = &battleengine.ElementSkillDamageReduction{
		ElementIDs: testIDs(fmt.Sprintf("formula-element-%d", variant)), Numerator: numerator, Denominator: 1,
	}
	member.ContactBasedSkillDamageReduction = &battleengine.ContactBasedSkillDamageReduction{Numerator: numerator, Denominator: 1}
	member.AttackingStatMultiplier = &battleengine.AttackingStatMultiplier{
		Stat: attackStat, Numerator: numerator, Denominator: 1,
		RequiredTerrain: terrain, RequiredWeather: weather, RequiresMajorStatus: true,
		RequiredMajorStatuses: []battleengine.MajorStatus{majorStatus},
		MaximumHPNumerator:    1, MaximumHPDenominator: 2,
		IgnoreBurnAttackReduction: attackStat == battleengine.StatAttack,
	}
	member.OpponentAttackingStatMultiplier = &battleengine.OpponentAttackingStatMultiplier{
		Stat: attackStat, Numerator: numerator, Denominator: 1,
	}
	member.DefendingStatMultiplier = &battleengine.DefendingStatMultiplier{
		Stat: defenseStat, Numerator: numerator, Denominator: 1, RequiredTerrain: terrain, RequiresMajorStatus: true,
	}
	member.OpponentDefendingStatMultiplier = &battleengine.OpponentDefendingStatMultiplier{
		Stat: defenseStat, Numerator: numerator, Denominator: 1,
	}
	member.AllySkillDamageBoost = &battleengine.AllySkillDamageBoost{
		DamageClasses: []battleengine.DamageClass{damageClass}, Numerator: numerator, Denominator: 1,
	}
	member.AllyReceivedDamageReduction = &battleengine.AllyReceivedDamageReduction{Numerator: numerator, Denominator: 1}
	member.AllyAbilityGroupCode = fmt.Sprintf("formula-group-%d", variant)
	member.AllyAbilityPresenceAttackingStatMultiplier = &battleengine.AllyAbilityPresenceAttackingStatMultiplier{
		GroupCode: member.AllyAbilityGroupCode, Stat: attackStat, Numerator: numerator, Denominator: 1,
	}
}

// formulaAbilityLifecycleRulesEqual 逐字段比较成员上的独立规则，避免测试只检查某个聚合对象存在而漏掉字段。
func formulaAbilityLifecycleRulesEqual(actual, expected battleengine.MemberSnapshot) bool {
	return reflect.DeepEqual(actual.TargetGenderDamageMultiplier, expected.TargetGenderDamageMultiplier) &&
		reflect.DeepEqual(actual.PunchBasedSkillDamageBoost, expected.PunchBasedSkillDamageBoost) &&
		reflect.DeepEqual(actual.SlicingBasedSkillDamageBoost, expected.SlicingBasedSkillDamageBoost) &&
		reflect.DeepEqual(actual.SoundBasedSkillDamageBoost, expected.SoundBasedSkillDamageBoost) &&
		reflect.DeepEqual(actual.PulseBasedSkillDamageBoost, expected.PulseBasedSkillDamageBoost) &&
		reflect.DeepEqual(actual.BiteBasedSkillDamageBoost, expected.BiteBasedSkillDamageBoost) &&
		reflect.DeepEqual(actual.SecondaryEffectsSuppressedDamageBoost, expected.SecondaryEffectsSuppressedDamageBoost) &&
		reflect.DeepEqual(actual.SoundBasedSkillDamageReduction, expected.SoundBasedSkillDamageReduction) &&
		reflect.DeepEqual(actual.SuperEffectiveDamageReduction, expected.SuperEffectiveDamageReduction) &&
		reflect.DeepEqual(actual.FullHPDamageReduction, expected.FullHPDamageReduction) &&
		reflect.DeepEqual(actual.DamageClassDamageReduction, expected.DamageClassDamageReduction) &&
		reflect.DeepEqual(actual.ElementSkillDamageReduction, expected.ElementSkillDamageReduction) &&
		reflect.DeepEqual(actual.ContactBasedSkillDamageReduction, expected.ContactBasedSkillDamageReduction) &&
		reflect.DeepEqual(actual.AttackingStatMultiplier, expected.AttackingStatMultiplier) &&
		reflect.DeepEqual(actual.OpponentAttackingStatMultiplier, expected.OpponentAttackingStatMultiplier) &&
		reflect.DeepEqual(actual.DefendingStatMultiplier, expected.DefendingStatMultiplier) &&
		reflect.DeepEqual(actual.OpponentDefendingStatMultiplier, expected.OpponentDefendingStatMultiplier) &&
		reflect.DeepEqual(actual.AllySkillDamageBoost, expected.AllySkillDamageBoost) &&
		reflect.DeepEqual(actual.AllyReceivedDamageReduction, expected.AllyReceivedDamageReduction) &&
		actual.AllyAbilityGroupCode == expected.AllyAbilityGroupCode &&
		reflect.DeepEqual(actual.AllyAbilityPresenceAttackingStatMultiplier, expected.AllyAbilityPresenceAttackingStatMultiplier)
}

// formulaAbilityLifecycleSnapshotEqual 逐字段比较变身前快照与成员原规则，确保离场恢复拥有完整输入。
func formulaAbilityLifecycleSnapshotEqual(actual battleengine.MemberTransformSnapshot, expected battleengine.MemberSnapshot) bool {
	return reflect.DeepEqual(actual.TargetGenderDamageMultiplier, expected.TargetGenderDamageMultiplier) &&
		reflect.DeepEqual(actual.PunchBasedSkillDamageBoost, expected.PunchBasedSkillDamageBoost) &&
		reflect.DeepEqual(actual.SlicingBasedSkillDamageBoost, expected.SlicingBasedSkillDamageBoost) &&
		reflect.DeepEqual(actual.SoundBasedSkillDamageBoost, expected.SoundBasedSkillDamageBoost) &&
		reflect.DeepEqual(actual.PulseBasedSkillDamageBoost, expected.PulseBasedSkillDamageBoost) &&
		reflect.DeepEqual(actual.BiteBasedSkillDamageBoost, expected.BiteBasedSkillDamageBoost) &&
		reflect.DeepEqual(actual.SecondaryEffectsSuppressedDamageBoost, expected.SecondaryEffectsSuppressedDamageBoost) &&
		reflect.DeepEqual(actual.SoundBasedSkillDamageReduction, expected.SoundBasedSkillDamageReduction) &&
		reflect.DeepEqual(actual.SuperEffectiveDamageReduction, expected.SuperEffectiveDamageReduction) &&
		reflect.DeepEqual(actual.FullHPDamageReduction, expected.FullHPDamageReduction) &&
		reflect.DeepEqual(actual.DamageClassDamageReduction, expected.DamageClassDamageReduction) &&
		reflect.DeepEqual(actual.ElementSkillDamageReduction, expected.ElementSkillDamageReduction) &&
		reflect.DeepEqual(actual.ContactBasedSkillDamageReduction, expected.ContactBasedSkillDamageReduction) &&
		reflect.DeepEqual(actual.AttackingStatMultiplier, expected.AttackingStatMultiplier) &&
		reflect.DeepEqual(actual.OpponentAttackingStatMultiplier, expected.OpponentAttackingStatMultiplier) &&
		reflect.DeepEqual(actual.DefendingStatMultiplier, expected.DefendingStatMultiplier) &&
		reflect.DeepEqual(actual.OpponentDefendingStatMultiplier, expected.OpponentDefendingStatMultiplier) &&
		reflect.DeepEqual(actual.AllySkillDamageBoost, expected.AllySkillDamageBoost) &&
		reflect.DeepEqual(actual.AllyReceivedDamageReduction, expected.AllyReceivedDamageReduction) &&
		actual.AllyAbilityGroupCode == expected.AllyAbilityGroupCode &&
		reflect.DeepEqual(actual.AllyAbilityPresenceAttackingStatMultiplier, expected.AllyAbilityPresenceAttackingStatMultiplier)
}
