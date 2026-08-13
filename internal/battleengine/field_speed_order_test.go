package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnReversesSamePrioritySpeedOrderAfterFieldEffectStarted 验证戏法空间在建立回合结束后，
// 只反转后续同一优先度行动的有效速度比较方向，而不会改变优先度规则本身。
func TestResolveTurnReversesSamePrioritySpeedOrderAfterFieldEffectStarted(t *testing.T) {
	t.Parallel()

	fast := newMember(1, "field-speed-fast", 500, 500)
	fast.Stats.Speed = 100
	fast.Skills[0] = fieldSpeedOrderSkill(1, "戏法空间", -7)
	fast.Skills = append(fast.Skills, ordinaryFieldSpeedOrderSkill(2, "高速攻击"))
	slow := newMember(1, "field-speed-slow", 500, 500)
	slow.Stats.Speed = 50
	slow.Skills[0] = ordinaryFieldSpeedOrderSkill(1, "低速攻击")

	state := newFieldSpeedOrderState(t, battleengine.EnvironmentSnapshot{}, fast, slow)
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 7)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	started, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideOne),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() start field effect error = %v", err)
	}
	startEvent, found := findFieldSpeedOrderStarted(started.Events)
	if !found || startEvent.FieldSpeedOrderKind != battleengine.FieldSpeedOrderKindTrickRoom || startEvent.TurnsRemaining != 5 {
		t.Fatalf("field speed order start event = %+v, found=%t", startEvent, found)
	}
	if effect := started.State.Snapshot().Environment.FieldSpeedOrder; effect == nil || effect.TurnsRemaining != 4 {
		t.Fatalf("field effect after activation = %+v, want 4 remaining turns", effect)
	}

	resolved, err := battleengine.ResolveTurn(started.State, fieldSpeedOrderTurn(2,
		fieldSpeedOrderAction(battleengine.SideOne, 2, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), started.RandomSource)
	if err != nil {
		t.Fatalf("ResolveTurn() under field effect error = %v", err)
	}
	firstDamage, found := findFirstDamage(resolved.Events)
	if !found || firstDamage.Actor.Side != battleengine.SideTwo {
		t.Fatalf("first damage event = %+v, found=%t; want lower-speed side two actor", firstDamage, found)
	}
	if effect := resolved.State.Snapshot().Environment.FieldSpeedOrder; effect == nil || effect.TurnsRemaining != 3 {
		t.Fatalf("field effect after following turn = %+v, want 3 remaining turns", effect)
	}
}

// TestResolveTurnCancelsFieldSpeedOrderWhenSameEffectIsUsedAgain 验证同一种全场速度顺序效果再次建立时，
// 按现代规则解除现有效果，而不是错误刷新其持续回合。
func TestResolveTurnCancelsFieldSpeedOrderWhenSameEffectIsUsedAgain(t *testing.T) {
	t.Parallel()

	setter := newMember(1, "field-speed-setter", 500, 500)
	setter.Skills[0] = fieldSpeedOrderSkill(1, "戏法空间", 0)
	observer := newMember(1, "field-speed-observer", 500, 500)
	state := newFieldSpeedOrderState(t, battleengine.EnvironmentSnapshot{
		FieldSpeedOrder: &battleengine.FieldSpeedOrderEffect{
			Kind: battleengine.FieldSpeedOrderKindTrickRoom, TurnsRemaining: 4,
		},
	}, setter, observer)
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 11)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideOne),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	ended, found := findFieldSpeedOrderEnded(result.Events)
	if !found || ended.FieldSpeedOrderKind != battleengine.FieldSpeedOrderKindTrickRoom || ended.Actor.Side != battleengine.SideOne || ended.SkillID != setter.Skills[0].SkillID {
		t.Fatalf("field speed order end event = %+v, found=%t", ended, found)
	}
	if effect := result.State.Snapshot().Environment.FieldSpeedOrder; effect != nil {
		t.Fatalf("field effect = %+v, want cancelled", effect)
	}
}

// TestResolveTurnExpiresFieldSpeedOrderAtEndOfFinalTurn 验证已经进入最后持续回合的全场速度顺序效果会在回合末
// 恢复普通排序，并产生不携带技能来源的自然结束事件。
func TestResolveTurnExpiresFieldSpeedOrderAtEndOfFinalTurn(t *testing.T) {
	t.Parallel()

	first := newMember(1, "field-speed-expire-one", 500, 500)
	second := newMember(1, "field-speed-expire-two", 500, 500)
	state := newFieldSpeedOrderState(t, battleengine.EnvironmentSnapshot{
		FieldSpeedOrder: &battleengine.FieldSpeedOrderEffect{
			Kind: battleengine.FieldSpeedOrderKindTrickRoom, TurnsRemaining: 1,
		},
	}, first, second)
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 13)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	ended, found := findFieldSpeedOrderEnded(result.Events)
	if !found || ended.FieldSpeedOrderKind != battleengine.FieldSpeedOrderKindTrickRoom || ended.Actor != (battleengine.MemberRef{}) || ended.SkillID != 0 {
		t.Fatalf("natural field speed order end event = %+v, found=%t", ended, found)
	}
	if effect := result.State.Snapshot().Environment.FieldSpeedOrder; effect != nil {
		t.Fatalf("field effect = %+v, want expired", effect)
	}
}

// TestStateSnapshotDeepCopiesFieldSpeedOrder 验证环境快照中的指针字段同样通过深复制隔离，避免调用方修改
// 外部快照后篡改权威状态或离线重放输入。
func TestStateSnapshotDeepCopiesFieldSpeedOrder(t *testing.T) {
	t.Parallel()

	first := newMember(1, "field-speed-copy-one", 500, 500)
	second := newMember(1, "field-speed-copy-two", 500, 500)
	state := newFieldSpeedOrderState(t, battleengine.EnvironmentSnapshot{
		FieldSpeedOrder: &battleengine.FieldSpeedOrderEffect{
			Kind: battleengine.FieldSpeedOrderKindTrickRoom, TurnsRemaining: 3,
		},
	}, first, second)
	snapshot := state.Snapshot()
	snapshot.Environment.FieldSpeedOrder.TurnsRemaining = 1
	if effect := state.Snapshot().Environment.FieldSpeedOrder; effect == nil || effect.TurnsRemaining != 3 {
		t.Fatalf("state field effect after snapshot mutation = %+v, want 3 remaining turns", effect)
	}
}

func newFieldSpeedOrderState(
	t *testing.T,
	environment battleengine.EnvironmentSnapshot,
	first battleengine.MemberSnapshot,
	second battleengine.MemberSnapshot,
) battleengine.State {
	t.Helper()
	state, err := battleengine.NewState(battleengine.InitialState{
		Format:      battleengine.FormatSnapshot{Code: "field-speed-order", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:       battleengine.RuleSnapshot{SchemaVersion: 1},
		Environment: environment,
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{second}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	return state
}

func fieldSpeedOrderSkill(position battleengine.SkillPosition, name string, priority int8) battleengine.SkillSnapshot {
	return battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: position, SkillID: testID("field-speed-order-skill"), Name: name, ElementID: testID("field-speed-element"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		Priority: priority, RemainingPP: 5, MaxPP: 5,
		FieldSpeedOrderApplication: &battleengine.FieldSpeedOrderApplication{
			Effect:        battleengine.FieldSpeedOrderEffect{Kind: battleengine.FieldSpeedOrderKindTrickRoom, TurnsRemaining: 5},
			ChancePercent: 100,
		},
	}
}

func ordinaryFieldSpeedOrderSkill(position battleengine.SkillPosition, name string) battleengine.SkillSnapshot {
	return battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: position, SkillID: testID("field-speed-order-attack-" + string(rune('0'+position))), Name: name, ElementID: testID("field-speed-element"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 40, Accuracy: 100, RemainingPP: 10, MaxPP: 10,
	}
}

func fieldSpeedOrderTurn(turnNumber uint32, first, second battleengine.Action) battleengine.TurnCommand {
	return battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: turnNumber, Actions: []battleengine.Action{first, second}}
}

func fieldSpeedOrderAction(side battleengine.Side, skillPosition battleengine.SkillPosition, targetSide battleengine.Side) battleengine.Action {
	return battleengine.Action{
		Kind:  battleengine.ActionKindUseSkill,
		Actor: battleengine.SlotRef{Side: side, Position: 1},
		UseSkill: &battleengine.UseSkillAction{
			SkillPosition: skillPosition, Target: battleengine.SlotRef{Side: targetSide, Position: 1},
		},
	}
}

func findFieldSpeedOrderStarted(events []battleengine.Event) (battleengine.FieldSpeedOrderStartedEvent, bool) {
	for _, event := range events {
		if value, ok := event.(battleengine.FieldSpeedOrderStartedEvent); ok {
			return value, true
		}
	}
	return battleengine.FieldSpeedOrderStartedEvent{}, false
}

func findFieldSpeedOrderEnded(events []battleengine.Event) (battleengine.FieldSpeedOrderEndedEvent, bool) {
	for _, event := range events {
		if value, ok := event.(battleengine.FieldSpeedOrderEndedEvent); ok {
			return value, true
		}
	}
	return battleengine.FieldSpeedOrderEndedEvent{}, false
}

func findFirstDamage(events []battleengine.Event) (battleengine.DamageAppliedEvent, bool) {
	for _, event := range events {
		if value, ok := event.(battleengine.DamageAppliedEvent); ok {
			return value, true
		}
	}
	return battleengine.DamageAppliedEvent{}, false
}
