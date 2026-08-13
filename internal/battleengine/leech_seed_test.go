package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnPlantsLeechSeedAndTransfersTheoreticalDrain 验证种子在命中当回合末扣除目标生命，
// 并按目标最大生命固定份额而非实际损失量回复来源。
func TestResolveTurnPlantsLeechSeedAndTransfersTheoreticalDrain(t *testing.T) {
	t.Parallel()

	source := newMember(1, "leech-seed-source", 800, 600)
	source.Skills[0] = leechSeedSkill(1)
	target := newMember(1, "leech-seed-target", 800, 50)
	target.Skills[0].Power = 1
	state := newLeechSeedState(t, battleengine.RuleSnapshot{SchemaVersion: 1}, source, []battleengine.MemberSnapshot{target})

	result := resolveLeechSeedTurn(t, state, 1,
		leechSeedUseSkill(battleengine.SideOne, 1, battleengine.SideTwo),
		leechSeedUseSkill(battleengine.SideTwo, 1, battleengine.SideOne),
	)
	seeded, found := findLeechSeedPlanted(result.Events)
	if !found || seeded.SourceSlot != (battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}) {
		t.Fatalf("LeechSeedPlantedEvent = %+v, found=%t", seeded, found)
	}
	damage, found := findLeechSeedDamage(result.Events)
	if !found || damage.Amount != 50 || damage.CurrentHP != 0 {
		t.Fatalf("LeechSeedDamageAppliedEvent = %+v, found=%t; want actual damage clipped to 50", damage, found)
	}
	healing, found := findLeechSeedHealing(result.Events)
	if !found || healing.Recipient != (battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}) || healing.Amount != 100 {
		t.Fatalf("LeechSeedHealingAppliedEvent = %+v, found=%t; want 100 theoretical-drain healing", healing, found)
	}
}

// TestResolveTurnRejectsLeechSeedAgainstGrassOrSubstitute 验证草属性和已存在的替身都会形成可审计的规则失败，
// 不会把种子写入目标本体。
func TestResolveTurnRejectsLeechSeedAgainstGrassOrSubstitute(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name          string
		target        battleengine.MemberSnapshot
		rules         battleengine.RuleSnapshot
		failureReason battleengine.SkillFailureReason
	}{
		{
			name: "草属性免疫",
			target: func() battleengine.MemberSnapshot {
				member := newMember(1, "leech-seed-grass-target", 500, 500)
				member.ElementIDs = testIDs("grass-element")
				member.Skills[0].Power = 1
				return member
			}(),
			rules:         battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]Identifier{"grass": testID("grass-element")}},
			failureReason: battleengine.SkillFailureReasonLeechSeedGrassTarget,
		},
		{
			name: "替身阻止",
			target: func() battleengine.MemberSnapshot {
				member := newMember(1, "leech-seed-substitute-target", 500, 500)
				member.Stats.Speed = 200
				member.Skills[0] = leechSeedSubstituteSkill(1)
				return member
			}(),
			rules:         battleengine.RuleSnapshot{SchemaVersion: 1},
			failureReason: battleengine.SkillFailureReasonLeechSeedTargetBehindSubstitute,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()

			source := newMember(1, "leech-seed-source-"+test.name, 500, 500)
			source.Skills[0] = leechSeedSkill(1)
			state := newLeechSeedState(t, test.rules, source, []battleengine.MemberSnapshot{test.target})
			targetAction := leechSeedUseSkill(battleengine.SideTwo, 1, battleengine.SideOne)
			result := resolveLeechSeedTurn(t, state, 1,
				leechSeedUseSkill(battleengine.SideOne, 1, battleengine.SideTwo),
				targetAction,
			)
			failure, found := findLeechSeedFailure(result.Events)
			if !found || failure.Reason != test.failureReason {
				t.Fatalf("SkillFailedEvent = %+v, found=%t", failure, found)
			}
			target, targetFound := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
			if !targetFound || target.LeechSeedSourceSlot != nil {
				t.Fatalf("target after rejected seed = %+v, found=%t", target, targetFound)
			}
		})
	}
}

// TestResolveTurnLeechSeedHealsCurrentMemberInSourceSlot 验证来源成员换下后，种子会回复相同场上槽位的替换成员。
func TestResolveTurnLeechSeedHealsCurrentMemberInSourceSlot(t *testing.T) {
	t.Parallel()

	source := newMember(1, "leech-seed-original-source", 800, 800)
	source.Stats.Speed = 200
	source.Skills[0] = leechSeedSkill(1)
	replacement := newMember(2, "leech-seed-replacement-source", 800, 600)
	replacement.Skills[0].Power = 1
	target := newMember(1, "leech-seed-target", 800, 800)
	target.Skills[0].Power = 1
	sourceSlot := battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "leech-seed-switch-source", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{source, replacement}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	started := resolveLeechSeedTurn(t, state, 1,
		leechSeedUseSkill(battleengine.SideOne, 1, battleengine.SideTwo),
		leechSeedUseSkill(battleengine.SideTwo, 1, battleengine.SideOne),
	)
	result := resolveLeechSeedTurnWithRandom(t, started.State, 2, started.RandomSource,
		battleengine.Action{Kind: battleengine.ActionKindSwitch, Actor: sourceSlot, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
		leechSeedUseSkill(battleengine.SideTwo, 1, battleengine.SideOne),
	)
	healing, found := findLeechSeedHealing(result.Events)
	if !found || healing.Recipient != (battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) || healing.Amount != 100 {
		t.Fatalf("LeechSeedHealingAppliedEvent = %+v, found=%t", healing, found)
	}
}

// TestResolveTurnSwitchingSeededTargetClearsLeechSeed 验证目标离场时种子被清除，后备成员和离场成员均不再保留该状态。
func TestResolveTurnSwitchingSeededTargetClearsLeechSeed(t *testing.T) {
	t.Parallel()

	source := newMember(1, "leech-seed-source", 800, 800)
	source.Stats.Speed = 200
	source.Skills[0] = leechSeedSkill(1)
	ordinarySkill := newMember(1, "leech-seed-ordinary-skill", 800, 800).Skills[0]
	ordinarySkill.Position = 2
	ordinarySkill.Power = 1
	source.Skills = append(source.Skills, ordinarySkill)
	seeded := newMember(1, "leech-seed-seeded-target", 800, 800)
	seeded.Skills[0].Power = 1
	reserve := newMember(2, "leech-seed-reserve-target", 800, 800)
	reserve.Skills[0].Power = 1
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "leech-seed-switch-target", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{source}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{seeded, reserve}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	started := resolveLeechSeedTurn(t, state, 1,
		leechSeedUseSkill(battleengine.SideOne, 1, battleengine.SideTwo),
		leechSeedUseSkill(battleengine.SideTwo, 1, battleengine.SideOne),
	)
	result := resolveLeechSeedTurnWithRandom(t, started.State, 2, started.RandomSource,
		leechSeedUseSkill(battleengine.SideOne, 2, battleengine.SideTwo),
		battleengine.Action{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
	)
	if _, found := findLeechSeedDamage(result.Events); found {
		t.Fatalf("leech seed damage after target switch must not occur: %v", result.Events)
	}
	for _, position := range []battleengine.MemberPosition{1, 2} {
		member, found := result.State.Snapshot().Sides[1].Members[position-1], position <= 2
		if !found || member.LeechSeedSourceSlot != nil {
			t.Fatalf("side two member %d after switch = %+v, found=%t", position, member, found)
		}
	}
}

func newLeechSeedState(
	t *testing.T,
	rules battleengine.RuleSnapshot,
	source battleengine.MemberSnapshot,
	targets []battleengine.MemberSnapshot,
) battleengine.State {
	t.Helper()
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "leech-seed", ActiveSlotsPerSide: 1, TeamSize: uint8(len(targets))},
		Rules:  rules,
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{source}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: targets},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	return state
}

func leechSeedSkill(position battleengine.SkillPosition) battleengine.SkillSnapshot {
	return battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: position, SkillID: testID("leech-seed-skill"), Name: "寄生种子", ElementID: testID("leech-seed-element"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10,
		LeechSeedApplication: &battleengine.LeechSeedApplication{ChancePercent: 100},
	}
}

func leechSeedSubstituteSkill(position battleengine.SkillPosition) battleengine.SkillSnapshot {
	return battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: position, SkillID: testID("leech-seed-substitute"), Name: "替身", ElementID: testID("leech-seed-element"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		RemainingPP: 10, MaxPP: 10,
		VolatileStatusApplications: []battleengine.VolatileStatusApplication{{
			Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser,
			ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
		}},
	}
}

func leechSeedUseSkill(side battleengine.Side, position battleengine.SkillPosition, targetSide battleengine.Side) battleengine.Action {
	return battleengine.Action{
		Kind:  battleengine.ActionKindUseSkill,
		Actor: battleengine.SlotRef{Side: side, Position: 1},
		UseSkill: &battleengine.UseSkillAction{
			SkillPosition: position,
			Target:        battleengine.SlotRef{Side: targetSide, Position: 1},
		},
	}
}

func resolveLeechSeedTurn(t *testing.T, state battleengine.State, turnNumber uint32, first, second battleengine.Action) battleengine.TurnResult {
	t.Helper()
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, uint64(turnNumber))
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	return resolveLeechSeedTurnWithRandom(t, state, turnNumber, random, first, second)
}

func resolveLeechSeedTurnWithRandom(
	t *testing.T,
	state battleengine.State,
	turnNumber uint32,
	random battleengine.RandomSource,
	first, second battleengine.Action,
) battleengine.TurnResult {
	t.Helper()
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: turnNumber, Actions: []battleengine.Action{first, second},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	return result
}

func findLeechSeedPlanted(events []battleengine.Event) (battleengine.LeechSeedPlantedEvent, bool) {
	for _, event := range events {
		if value, ok := event.(battleengine.LeechSeedPlantedEvent); ok {
			return value, true
		}
	}
	return battleengine.LeechSeedPlantedEvent{}, false
}

func findLeechSeedDamage(events []battleengine.Event) (battleengine.LeechSeedDamageAppliedEvent, bool) {
	for _, event := range events {
		if value, ok := event.(battleengine.LeechSeedDamageAppliedEvent); ok {
			return value, true
		}
	}
	return battleengine.LeechSeedDamageAppliedEvent{}, false
}

func findLeechSeedHealing(events []battleengine.Event) (battleengine.LeechSeedHealingAppliedEvent, bool) {
	for _, event := range events {
		if value, ok := event.(battleengine.LeechSeedHealingAppliedEvent); ok {
			return value, true
		}
	}
	return battleengine.LeechSeedHealingAppliedEvent{}, false
}

func findLeechSeedFailure(events []battleengine.Event) (battleengine.SkillFailedEvent, bool) {
	for _, event := range events {
		if value, ok := event.(battleengine.SkillFailedEvent); ok &&
			(value.Reason == battleengine.SkillFailureReasonLeechSeedGrassTarget || value.Reason == battleengine.SkillFailureReasonLeechSeedTargetBehindSubstitute) {
			return value, true
		}
	}
	return battleengine.SkillFailedEvent{}, false
}
