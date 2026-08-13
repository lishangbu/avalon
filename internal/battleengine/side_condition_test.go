package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnTailwindChangesLaterTurnOrderAndExpires 验证顺风只影响后续回合的同优先度速度排序，并在
// 约定的完整回合数耗尽后从对应阵营状态清除。该测试通过 Battle Engine 的 ResolveTurn 公共边界观察行为，
// 不依赖任何内部状态写入函数。
func TestResolveTurnTailwindChangesLaterTurnOrderAndExpires(t *testing.T) {
	t.Parallel()

	first := newMember(1, "tailwind-side", 500, 500)
	first.Stats.Speed = 60
	second := newMember(1, "normal-side", 500, 500)
	second.Stats.Speed = 100
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "tailwind", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{
				Side:          battleengine.SideOne,
				ActiveMembers: []battleengine.MemberPosition{1},
				Members:       []battleengine.MemberSnapshot{first},
				Conditions: battleengine.SideConditionSnapshot{
					Tailwind: &battleengine.TailwindEffect{TurnsRemaining: 2},
				},
			},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{second}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 7)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}

	firstTurn, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if firstActor := firstSkillActor(t, firstTurn.Events); firstActor.Side != battleengine.SideOne {
		t.Fatalf("顺风下首个使用技能的阵营 = %d，期望第一方", firstActor.Side)
	}
	firstSide := firstTurn.State.Snapshot().Sides[0]
	if firstSide.Conditions.Tailwind == nil || firstSide.Conditions.Tailwind.TurnsRemaining != 1 {
		t.Fatalf("第一回合后的顺风 = %+v，期望剩余 1 回合", firstSide.Conditions.Tailwind)
	}

	secondTurn, err := battleengine.ResolveTurn(firstTurn.State, fieldSpeedOrderTurn(2,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), firstTurn.RandomSource)
	if err != nil {
		t.Fatalf("ResolveTurn() 第二回合 error = %v", err)
	}
	if secondTurn.State.Snapshot().Sides[0].Conditions.Tailwind != nil {
		t.Fatalf("顺风到期后仍存在 = %+v", secondTurn.State.Snapshot().Sides[0].Conditions.Tailwind)
	}
	if !containsSideConditionEvent(secondTurn.Events, battleengine.EventKindTailwindEnded) {
		t.Fatalf("顺风到期事件缺失: %v", secondTurn.Events)
	}
}

// TestResolveTurnStartsTailwindOnUserSide 验证顺风作为自身范围变化技能的后效，只会写入使用者一方的侧状态，
// 并以建立时长记录开始事件。当前回合末递减属于持续状态的统一生命周期，不会改变开始事件中的原始时长。
func TestResolveTurnStartsTailwindOnUserSide(t *testing.T) {
	t.Parallel()

	first := newMember(1, "tailwind-setter", 500, 500)
	first.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("tailwind"), Name: "顺风", ElementID: testID("wind"), DamageClass: battleengine.DamageClassStatus,
		TargetScope: battleengine.SkillTargetScopeSelf, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
		TailwindApplication: &battleengine.TailwindApplication{
			Effect: battleengine.TailwindEffect{TurnsRemaining: 2}, ChancePercent: 100,
		},
	}
	second := newMember(1, "tailwind-observer", 500, 500)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "tailwind-start", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{second}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 8)
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
	sides := result.State.Snapshot().Sides
	if sides[0].Conditions.Tailwind == nil || sides[0].Conditions.Tailwind.TurnsRemaining != 1 {
		t.Fatalf("使用者一方顺风 = %+v，期望剩余 1 回合", sides[0].Conditions.Tailwind)
	}
	if sides[1].Conditions.Tailwind != nil {
		t.Fatalf("对方不应获得顺风: %+v", sides[1].Conditions.Tailwind)
	}
	if !containsSideConditionEvent(result.Events, battleengine.EventKindTailwindStarted) {
		t.Fatalf("顺风建立事件缺失: %v", result.Events)
	}
}

// TestResolveTurnReflectReducesPhysicalDamage 验证反射壁只在防守方一侧存在时减少普通物理伤害。该规则通过
// ResolveTurn 的公开事件观察，避免把测试绑定到伤害公式内部的某个中间数值。
func TestResolveTurnReflectReducesPhysicalDamage(t *testing.T) {
	t.Parallel()

	withoutReflect := resolveSideConditionDamage(t, battleengine.SideConditionSnapshot{}, battleengine.DamageClassPhysical)
	withReflect := resolveSideConditionDamage(t, battleengine.SideConditionSnapshot{
		Reflect: &battleengine.ReflectEffect{TurnsRemaining: 2},
	}, battleengine.DamageClassPhysical)
	if withReflect >= withoutReflect {
		t.Fatalf("反射壁物理伤害 = %d，未设置反射壁 = %d；期望反射壁降低伤害", withReflect, withoutReflect)
	}
}

// TestResolveTurnLightScreenAndAuroraVeilReduceOnlySupportedDamage 验证光墙只减免特殊伤害，而极光幕分别减免物理和
// 特殊伤害。三种屏障共存时单次伤害只读取一项适用规则，不应产生叠乘。
func TestResolveTurnLightScreenAndAuroraVeilReduceOnlySupportedDamage(t *testing.T) {
	t.Parallel()

	physicalBase := resolveSideConditionDamage(t, battleengine.SideConditionSnapshot{}, battleengine.DamageClassPhysical)
	specialBase := resolveSideConditionDamage(t, battleengine.SideConditionSnapshot{}, battleengine.DamageClassSpecial)
	lightScreen := battleengine.SideConditionSnapshot{LightScreen: &battleengine.LightScreenEffect{TurnsRemaining: 2}}
	if actual := resolveSideConditionDamage(t, lightScreen, battleengine.DamageClassPhysical); actual != physicalBase {
		t.Fatalf("光墙物理伤害 = %d，常规物理伤害 = %d；期望光墙不影响物理伤害", actual, physicalBase)
	}
	if actual := resolveSideConditionDamage(t, lightScreen, battleengine.DamageClassSpecial); actual >= specialBase {
		t.Fatalf("光墙特殊伤害 = %d，常规特殊伤害 = %d；期望光墙降低特殊伤害", actual, specialBase)
	}
	auroraVeil := battleengine.SideConditionSnapshot{AuroraVeil: &battleengine.AuroraVeilEffect{TurnsRemaining: 2}}
	if actual := resolveSideConditionDamage(t, auroraVeil, battleengine.DamageClassPhysical); actual >= physicalBase {
		t.Fatalf("极光幕物理伤害 = %d，常规物理伤害 = %d；期望极光幕降低物理伤害", actual, physicalBase)
	}
	if actual := resolveSideConditionDamage(t, auroraVeil, battleengine.DamageClassSpecial); actual >= specialBase {
		t.Fatalf("极光幕特殊伤害 = %d，常规特殊伤害 = %d；期望极光幕降低特殊伤害", actual, specialBase)
	}
}

// TestResolveTurnStartsReflectOnUserSide 验证反射壁作为自身范围变化技能的后效，只会建立在使用者一方，并在
// 建立回合末统一递减持续时间。它不能依赖客户端提交的目标槽位把己方屏障写到对方阵营。
func TestResolveTurnStartsReflectOnUserSide(t *testing.T) {
	t.Parallel()

	first := newMember(1, "reflect-setter", 500, 500)
	first.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("reflect"), Name: "反射壁", ElementID: testID("psychic"), DamageClass: battleengine.DamageClassStatus,
		TargetScope: battleengine.SkillTargetScopeSelf, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
		ReflectApplication: &battleengine.ReflectApplication{
			Effect: battleengine.ReflectEffect{TurnsRemaining: 2}, ChancePercent: 100,
		},
	}
	second := newMember(1, "reflect-observer", 500, 500)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "reflect-start", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{second}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 10)
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
	sides := result.State.Snapshot().Sides
	if sides[0].Conditions.Reflect == nil || sides[0].Conditions.Reflect.TurnsRemaining != 1 {
		t.Fatalf("使用者一方反射壁 = %+v，期望剩余 1 回合", sides[0].Conditions.Reflect)
	}
	if sides[1].Conditions.Reflect != nil {
		t.Fatalf("对方不应获得反射壁: %+v", sides[1].Conditions.Reflect)
	}
	if !containsSideConditionEvent(result.Events, battleengine.EventKindReflectStarted) {
		t.Fatalf("反射壁建立事件缺失: %v", result.Events)
	}
}

// TestResolveTurnStartsLightScreenRejectsDuplicateAndExpires 验证光墙只建立在使用者一方；同种屏障尚未到期时
// 不能被第二次使用刷新，并且到期时必须同时从权威状态移除和写入独立结束事件。
func TestResolveTurnStartsLightScreenRejectsDuplicateAndExpires(t *testing.T) {
	t.Parallel()

	first := newMember(1, "light-screen-setter", 500, 500)
	first.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("light-screen"), Name: "光墙", ElementID: testID("psychic"), DamageClass: battleengine.DamageClassStatus,
		TargetScope: battleengine.SkillTargetScopeSelf, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
		LightScreenApplication: &battleengine.LightScreenApplication{
			Effect: battleengine.LightScreenEffect{TurnsRemaining: 2}, ChancePercent: 100,
		},
	}
	second := newMember(1, "light-screen-observer", 500, 500)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "light-screen-start", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{second}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 12)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}

	started, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() 建立光墙 error = %v", err)
	}
	if effect := started.State.Snapshot().Sides[0].Conditions.LightScreen; effect == nil || effect.TurnsRemaining != 1 {
		t.Fatalf("建立回合后的光墙 = %+v，期望剩余 1 回合", effect)
	}
	if !containsSideConditionEvent(started.Events, battleengine.EventKindLightScreenStarted) {
		t.Fatalf("光墙建立事件缺失: %v", started.Events)
	}

	finished, err := battleengine.ResolveTurn(started.State, fieldSpeedOrderTurn(2,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), started.RandomSource)
	if err != nil {
		t.Fatalf("ResolveTurn() 重复光墙 error = %v", err)
	}
	if effect := finished.State.Snapshot().Sides[0].Conditions.LightScreen; effect != nil {
		t.Fatalf("光墙到期后仍存在 = %+v", effect)
	}
	if !containsSideConditionEvent(finished.Events, battleengine.EventKindLightScreenEnded) {
		t.Fatalf("光墙结束事件缺失: %v", finished.Events)
	}
	if !containsSkillFailure(finished.Events, battleengine.SkillFailureReasonLightScreenAlreadyActive) {
		t.Fatalf("光墙重复建立失败事件缺失: %v", finished.Events)
	}
}

// TestResolveTurnStartsAuroraVeilRejectsDuplicateAndExpires 验证极光幕和光墙一样属于独立侧状态：它不能刷新
// 既有效果，并在完整持续回合结束时产生自己专属的生命周期事件。
func TestResolveTurnStartsAuroraVeilRejectsDuplicateAndExpires(t *testing.T) {
	t.Parallel()

	first := newMember(1, "aurora-veil-setter", 500, 500)
	first.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("aurora-veil"), Name: "极光幕", ElementID: testID("ice"), DamageClass: battleengine.DamageClassStatus,
		TargetScope: battleengine.SkillTargetScopeSelf, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
		AuroraVeilApplication: &battleengine.AuroraVeilApplication{
			Effect: battleengine.AuroraVeilEffect{TurnsRemaining: 2}, ChancePercent: 100,
		},
	}
	second := newMember(1, "aurora-veil-observer", 500, 500)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "aurora-veil-start", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{second}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 13)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}

	started, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() 建立极光幕 error = %v", err)
	}
	if effect := started.State.Snapshot().Sides[0].Conditions.AuroraVeil; effect == nil || effect.TurnsRemaining != 1 {
		t.Fatalf("建立回合后的极光幕 = %+v，期望剩余 1 回合", effect)
	}
	if !containsSideConditionEvent(started.Events, battleengine.EventKindAuroraVeilStarted) {
		t.Fatalf("极光幕建立事件缺失: %v", started.Events)
	}

	finished, err := battleengine.ResolveTurn(started.State, fieldSpeedOrderTurn(2,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), started.RandomSource)
	if err != nil {
		t.Fatalf("ResolveTurn() 重复极光幕 error = %v", err)
	}
	if effect := finished.State.Snapshot().Sides[0].Conditions.AuroraVeil; effect != nil {
		t.Fatalf("极光幕到期后仍存在 = %+v", effect)
	}
	if !containsSideConditionEvent(finished.Events, battleengine.EventKindAuroraVeilEnded) {
		t.Fatalf("极光幕结束事件缺失: %v", finished.Events)
	}
	if !containsSkillFailure(finished.Events, battleengine.SkillFailureReasonAuroraVeilAlreadyActive) {
		t.Fatalf("极光幕重复建立失败事件缺失: %v", finished.Events)
	}
}

// TestResolveTurnSpikesDamageGroundedSwitchIn 验证撒菱保存在被布置阵营的侧状态中，并在该方接地成员实际换入后
// 按层数造成最大生命比例伤害。换出成员不会承受入场危害，事件中的目标必须是新上场成员。
func TestResolveTurnSpikesDamageGroundedSwitchIn(t *testing.T) {
	t.Parallel()

	previous := newMember(1, "spikes-previous", 1_000, 1_000)
	incoming := newMember(2, "spikes-incoming", 1_000, 1_000)
	opponent := newMember(1, "spikes-opponent", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "spikes", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{
				Side:          battleengine.SideOne,
				ActiveMembers: []battleengine.MemberPosition{1},
				Members:       []battleengine.MemberSnapshot{previous, incoming},
				Conditions:    battleengine.SideConditionSnapshot{SpikesLayers: 3},
			},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 11)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			{
				Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				Switch: &battleengine.SwitchAction{MemberPosition: 2},
			},
			fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	active, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !exists || active.Position != 2 {
		t.Fatalf("撒菱换入后的场上成员 = %+v, exists=%t；期望位置 2", active, exists)
	}
	for _, event := range result.Events {
		if value, ok := event.(battleengine.SpikesDamageAppliedEvent); ok {
			if value.Target != (battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) || value.Layers != 3 || value.Amount != 250 || value.CurrentHP != 750 {
				t.Fatalf("撒菱入场伤害事件 = %+v，期望第三层对换入成员造成 250 伤害", value)
			}
			return
		}
	}
	t.Fatalf("撒菱入场伤害事件缺失: %v", result.Events)
}

// TestResolveTurnStealthRockUsesFrozenRockEffectiveness 验证隐形岩按战斗开始时冻结的岩石属性倍率伤害换入成员，
// 而不是把飞行成员当作不接地对象跳过。该倍率由 RuleSnapshot 固化，资料在对战中变化不会影响本次结算。
func TestResolveTurnStealthRockUsesFrozenRockEffectiveness(t *testing.T) {
	t.Parallel()

	previous := newMember(1, "stealth-rock-previous", 1_000, 1_000)
	incoming := newMember(2, "stealth-rock-incoming", 1_000, 1_000)
	incoming.ElementIDs = testIDs("flying-element")
	opponent := newMember(1, "stealth-rock-opponent", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "stealth-rock", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules: battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]Identifier{"rock": testID("rock-element")}, ElementEffectiveness: []battleengine.ElementEffectiveness{{
			AttackElementID: testID("rock-element"), DefenseElementID: testID("flying-element"), Numerator: 2, Denominator: 1,
		}}},
		Sides: []battleengine.SideSnapshot{
			{
				Side:          battleengine.SideOne,
				ActiveMembers: []battleengine.MemberPosition{1},
				Members:       []battleengine.MemberSnapshot{previous, incoming},
				Conditions:    battleengine.SideConditionSnapshot{StealthRock: true},
			},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 14)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{
		{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	}}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	for _, event := range result.Events {
		if value, ok := event.(battleengine.StealthRockDamageAppliedEvent); ok {
			if value.Target != (battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) || value.Amount != 250 || value.CurrentHP != 750 || value.EffectivenessNumerator != 2 || value.EffectivenessDenominator != 1 {
				t.Fatalf("隐形岩入场伤害事件 = %+v，期望岩石两倍倍率造成 250 伤害", value)
			}
			return
		}
	}
	t.Fatalf("隐形岩入场伤害事件缺失: %v", result.Events)
}

// TestResolveTurnToxicSpikesPoisonMemberAbsorbsLayers 验证接地毒属性成员换入时会吸收己方场地的全部毒菱层数，
// 而不是获得中毒或剧毒；吸收后的侧状态必须为零，确保后续换入成员不会再次触发旧危害。
func TestResolveTurnToxicSpikesPoisonMemberAbsorbsLayers(t *testing.T) {
	t.Parallel()

	previous := newMember(1, "toxic-spikes-previous", 1_000, 1_000)
	incoming := newMember(2, "toxic-spikes-poison", 1_000, 1_000)
	incoming.ElementIDs = testIDs("poison-element")
	opponent := newMember(1, "toxic-spikes-opponent", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "toxic-spikes-absorb", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]Identifier{"poison": testID("poison-element")}},
		Sides: []battleengine.SideSnapshot{
			{
				Side:          battleengine.SideOne,
				ActiveMembers: []battleengine.MemberPosition{1},
				Members:       []battleengine.MemberSnapshot{previous, incoming},
				Conditions:    battleengine.SideConditionSnapshot{ToxicSpikesLayers: 2},
			},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 15)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{
		{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	}}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if layers := result.State.Snapshot().Sides[0].Conditions.ToxicSpikesLayers; layers != 0 {
		t.Fatalf("毒属性成员换入后的毒菱层数 = %d，期望 0", layers)
	}
	if !containsSideConditionEvent(result.Events, battleengine.EventKindToxicSpikesAbsorbed) {
		t.Fatalf("毒菱吸收事件缺失: %v", result.Events)
	}
	active, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !exists || active.MajorStatus != "" {
		t.Fatalf("毒属性换入成员 = %+v, exists=%t；期望未获得主要异常", active, exists)
	}
}

// TestResolveTurnToxicSpikesAndStickyWebApplyDistinctEntryEffects 验证双层毒菱为接地成员施加剧毒，而黏黏网仅降低
// 速度能力阶级。二者共享“接地成员换入”触发时机，但异常和能力变化必须写入不同状态和事件。
func TestResolveTurnToxicSpikesAndStickyWebApplyDistinctEntryEffects(t *testing.T) {
	t.Parallel()

	previous := newMember(1, "entry-effects-previous", 1_000, 1_000)
	incoming := newMember(2, "entry-effects-incoming", 1_000, 1_000)
	opponent := newMember(1, "entry-effects-opponent", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "entry-effects", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{
				Side:          battleengine.SideOne,
				ActiveMembers: []battleengine.MemberPosition{1},
				Members:       []battleengine.MemberSnapshot{previous, incoming},
				Conditions:    battleengine.SideConditionSnapshot{ToxicSpikesLayers: 2, StickyWeb: true},
			},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 16)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{
		{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	}}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	active, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !exists || active.MajorStatus != battleengine.MajorStatusBadPoison || active.StatStages[battleengine.StatSpeed] != -1 {
		t.Fatalf("毒菱和黏黏网换入成员 = %+v, exists=%t；期望剧毒且速度 -1", active, exists)
	}
	if !containsSideConditionEvent(result.Events, battleengine.EventKindToxicSpikesStatusApplied) || !containsSideConditionEvent(result.Events, battleengine.EventKindStickyWebSpeedLowered) {
		t.Fatalf("毒菱或黏黏网事件缺失: %v", result.Events)
	}
}

// TestResolveTurnCreatesDistinctEntryHazardsOnTargetSide 验证四种入场危害都由各自的单体目标变化技能写入目标阵营。
//
// 虽然这些技能都通过被选中目标确定“对方场地”，但它们必须分别保持撒菱层数、隐形岩布尔值、毒菱层数和
// 黏黏网布尔值；任何统一危害载荷都会丢失换入时的不同规则。
func TestResolveTurnCreatesDistinctEntryHazardsOnTargetSide(t *testing.T) {
	t.Parallel()

	setter := newMember(1, "hazard-setter", 1_000, 1_000)
	setter.Stats.Speed = 200
	setter.Skills = []battleengine.SkillSnapshot{
		{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("spikes"), Name: "撒菱", ElementID: testID("ground"), DamageClass: battleengine.DamageClassStatus,
			TargetScope: battleengine.SkillTargetScopeSelectedTarget, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
			SpikesApplication: &battleengine.SpikesApplication{ChancePercent: 100},
		},
		{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 2, SkillID: testID("stealth-rock"), Name: "隐形岩", ElementID: testID("rock"), DamageClass: battleengine.DamageClassStatus,
			TargetScope: battleengine.SkillTargetScopeSelectedTarget, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
			StealthRockApplication: &battleengine.StealthRockApplication{ChancePercent: 100},
		},
		{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 3, SkillID: testID("toxic-spikes"), Name: "毒菱", ElementID: testID("poison"), DamageClass: battleengine.DamageClassStatus,
			TargetScope: battleengine.SkillTargetScopeSelectedTarget, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
			ToxicSpikesApplication: &battleengine.ToxicSpikesApplication{ChancePercent: 100},
		},
		{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 4, SkillID: testID("sticky-web"), Name: "黏黏网", ElementID: testID("bug"), DamageClass: battleengine.DamageClassStatus,
			TargetScope: battleengine.SkillTargetScopeSelectedTarget, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
			StickyWebApplication: &battleengine.StickyWebApplication{ChancePercent: 100},
		},
	}
	opponent := newMember(1, "hazard-target", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "hazard-creation", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{setter}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 17)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	seen := make(map[battleengine.EventKind]bool, 4)
	for turnNumber, skillPosition := uint32(1), battleengine.SkillPosition(1); skillPosition <= 4; turnNumber, skillPosition = turnNumber+1, skillPosition+1 {
		result, resolveErr := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(turnNumber,
			fieldSpeedOrderAction(battleengine.SideOne, skillPosition, battleengine.SideTwo),
			fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
		), random)
		if resolveErr != nil {
			t.Fatalf("第 %d 回合 ResolveTurn() error = %v", turnNumber, resolveErr)
		}
		for _, event := range result.Events {
			seen[event.Kind()] = true
		}
		state, random = result.State, result.RandomSource
	}
	conditions := state.Snapshot().Sides[1].Conditions
	if conditions.SpikesLayers != 1 || !conditions.StealthRock || conditions.ToxicSpikesLayers != 1 || !conditions.StickyWeb {
		t.Fatalf("目标方入场危害 = %+v，期望四种独立状态均已建立", conditions)
	}
	for _, kind := range []battleengine.EventKind{
		battleengine.EventKindSpikesLayerAdded,
		battleengine.EventKindStealthRockStarted,
		battleengine.EventKindToxicSpikesLayerAdded,
		battleengine.EventKindStickyWebStarted,
	} {
		if !seen[kind] {
			t.Fatalf("入场危害建立事件 %s 缺失", kind)
		}
	}
}

// TestResolveTurnRapidSpinClearsOnlyUserSideHazards 验证快速旋转只清除使用者一方的四种入场危害，
// 不移除本方屏障，也不会清除目标一方的任意侧状态。
func TestResolveTurnRapidSpinClearsOnlyUserSideHazards(t *testing.T) {
	t.Parallel()

	spinner := newMember(1, "rapid-spin-user", 1_000, 1_000)
	spinner.Skills[0].SkillID = testID("rapid-spin")
	spinner.Skills[0].Name = "快速旋转"
	spinner.Skills[0].RapidSpinApplication = &battleengine.RapidSpinApplication{}
	target := newMember(1, "rapid-spin-target", 1_000, 1_000)
	userConditions := battleengine.SideConditionSnapshot{
		Reflect: &battleengine.ReflectEffect{TurnsRemaining: 2}, SpikesLayers: 2, StealthRock: true,
		ToxicSpikesLayers: 1, StickyWeb: true,
	}
	targetConditions := battleengine.SideConditionSnapshot{
		SpikesLayers: 3, StealthRock: true, ToxicSpikesLayers: 2, StickyWeb: true,
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "rapid-spin", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]Identifier{"rock": testID("rock-element")}},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{spinner}, Conditions: userConditions},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}, Conditions: targetConditions},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 18)
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
	sides := result.State.Snapshot().Sides
	if conditions := sides[0].Conditions; conditions.SpikesLayers != 0 || conditions.StealthRock || conditions.ToxicSpikesLayers != 0 || conditions.StickyWeb || conditions.Reflect == nil {
		t.Fatalf("快速旋转后的使用者侧状态 = %+v，期望仅清除入场危害", conditions)
	}
	if conditions := sides[1].Conditions; conditions.SpikesLayers != 3 || !conditions.StealthRock || conditions.ToxicSpikesLayers != 2 || !conditions.StickyWeb {
		t.Fatalf("快速旋转后的目标侧状态 = %+v，期望目标方危害不变", conditions)
	}
	if !containsSideConditionEvent(result.Events, battleengine.EventKindRapidSpinHazardsCleared) {
		t.Fatalf("快速旋转清场事件缺失: %v", result.Events)
	}
}

// TestResolveTurnDefogClearsTargetSideScreensHazardsAndTerrain 验证清除浓雾清除目标一方的三种屏障与四种危害，
// 并清除当前普通场地；顺风不是屏障或危害，必须保持在目标方侧状态中。
func TestResolveTurnDefogClearsTargetSideScreensHazardsAndTerrain(t *testing.T) {
	t.Parallel()

	user := newMember(1, "defog-user", 1_000, 1_000)
	user.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("defog"), Name: "清除浓雾", ElementID: testID("flying"), DamageClass: battleengine.DamageClassStatus,
		TargetScope: battleengine.SkillTargetScopeSelectedTarget, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
		DefogApplication: &battleengine.DefogApplication{},
	}
	target := newMember(1, "defog-target", 1_000, 1_000)
	targetConditions := battleengine.SideConditionSnapshot{
		Reflect: &battleengine.ReflectEffect{TurnsRemaining: 2}, LightScreen: &battleengine.LightScreenEffect{TurnsRemaining: 2},
		AuroraVeil: &battleengine.AuroraVeilEffect{TurnsRemaining: 2}, Tailwind: &battleengine.TailwindEffect{TurnsRemaining: 2},
		SpikesLayers: 3, StealthRock: true, ToxicSpikesLayers: 2, StickyWeb: true,
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format:      battleengine.FormatSnapshot{Code: "defog", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:       battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]Identifier{"rock": testID("rock-element")}},
		Environment: battleengine.EnvironmentSnapshot{Terrain: &battleengine.TerrainEffect{Kind: battleengine.TerrainKindGrassy, TurnsRemaining: 2}},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{user}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}, Conditions: targetConditions},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 19)
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
	conditions := result.State.Snapshot().Sides[1].Conditions
	if conditions.Reflect != nil || conditions.LightScreen != nil || conditions.AuroraVeil != nil || conditions.SpikesLayers != 0 || conditions.StealthRock || conditions.ToxicSpikesLayers != 0 || conditions.StickyWeb || conditions.Tailwind == nil {
		t.Fatalf("清除浓雾后的目标侧状态 = %+v，期望保留顺风并清除屏障和危害", conditions)
	}
	if terrain := result.State.Snapshot().Environment.Terrain; terrain != nil {
		t.Fatalf("清除浓雾后的普通场地 = %+v，期望 nil", terrain)
	}
	if !containsSideConditionEvent(result.Events, battleengine.EventKindDefogSideConditionsCleared) || !containsSideConditionEvent(result.Events, battleengine.EventKindDefogTerrainCleared) {
		t.Fatalf("清除浓雾事件缺失: %v", result.Events)
	}
}

// TestResolveTurnForcedReplacement 验证倒下成员的补位属于强制换人；换人事件必须明确标记 Forced，供
// Battle 阶段和重放正确区分。
func TestResolveTurnForcedReplacement(t *testing.T) {
	t.Parallel()

	fainted := newMember(1, "forced-switch-fainted", 1_000, 0)
	reserve := newMember(2, "forced-switch-reserve", 1_000, 1_000)
	opponent := newMember(1, "forced-switch-opponent", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "forced-switch", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{fainted, reserve}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 20)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{
		{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	}}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	for _, event := range result.Events {
		if switched, ok := event.(battleengine.ParticipantSwitchedEvent); ok && switched.Slot == (battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}) {
			if !switched.Forced || switched.NextMember != (battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) {
				t.Fatalf("强制补位事件 = %+v", switched)
			}
			return
		}
	}
	t.Fatalf("强制补位事件缺失: %v", result.Events)
}

// TestResolveTurnDoubleVacantSlotDoesNotRequireImpossibleAction 验证双打一侧有倒下空槽且没有后备成员时，
// 引擎只要求仍可行动的三名成员提交行动，不会要求空槽伪造技能或无效换人。
func TestResolveTurnDoubleVacantSlotDoesNotRequireImpossibleAction(t *testing.T) {
	t.Parallel()

	fainted := newMember(1, "vacant-fainted", 1_000, 0)
	live := newMember(2, "vacant-live", 1_000, 1_000)
	opponentFirst := newMember(1, "vacant-opponent-first", 1_000, 1_000)
	opponentSecond := newMember(2, "vacant-opponent-second", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "double-vacant", ActiveSlotsPerSide: 2, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{fainted, live}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{opponentFirst, opponentSecond}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 21)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{
		{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 2}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
		{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 2}}},
		{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 2}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 2}}},
	}}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if result.State.Snapshot().Result != nil {
		t.Fatalf("仍有一名成员在场时不应终局: %+v", result.State.Snapshot().Result)
	}
}

// TestResolveTurnDoubleFaintedSlotsCanChooseEitherReplacement 验证双打一方两个槽位同时倒下但仅有一个后备成员时，
// 客户端可选择任一槽位完成唯一必须的强制补位；引擎不得把后备成员强行绑定到某个固定槽位。
func TestResolveTurnDoubleFaintedSlotsCanChooseEitherReplacement(t *testing.T) {
	t.Parallel()

	firstFainted := newMember(1, "double-choice-first-fainted", 1_000, 0)
	secondFainted := newMember(2, "double-choice-second-fainted", 1_000, 0)
	reserve := newMember(3, "double-choice-reserve", 1_000, 1_000)
	opponentFirst := newMember(1, "double-choice-opponent-first", 1_000, 1_000)
	opponentSecond := newMember(2, "double-choice-opponent-second", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "double-choice", ActiveSlotsPerSide: 2, TeamSize: 3},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{firstFainted, secondFainted, reserve}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{opponentFirst, opponentSecond}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 22)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{
		{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 2}, Switch: &battleengine.SwitchAction{MemberPosition: 3}},
		{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 2}}},
		{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 2}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 2}}},
	}}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	snapshot := result.State.Snapshot()
	if snapshot.Sides[0].ActiveMembers[0] != 1 || snapshot.Sides[0].ActiveMembers[1] != 3 {
		t.Fatalf("双打强制补位后的场上成员 = %+v，期望后备进入客户端选择的第二槽", snapshot.Sides[0].ActiveMembers)
	}
	for _, event := range result.Events {
		if switched, ok := event.(battleengine.ParticipantSwitchedEvent); ok && switched.Slot == (battleengine.SlotRef{Side: battleengine.SideOne, Position: 2}) {
			if !switched.Forced || switched.NextMember != (battleengine.MemberRef{Side: battleengine.SideOne, Position: 3}) {
				t.Fatalf("第二槽强制补位事件 = %+v", switched)
			}
			return
		}
	}
	t.Fatalf("第二槽强制补位事件缺失: %v", result.Events)
}

// firstSkillActor 返回回合中第一个实际使用技能的成员引用；测试场景应至少产生一个技能使用事件。
func firstSkillActor(t *testing.T, events []battleengine.Event) battleengine.MemberRef {
	t.Helper()
	for _, event := range events {
		if value, ok := event.(battleengine.SkillUsedEvent); ok {
			return value.Actor
		}
	}
	t.Fatalf("未产生技能使用事件: %v", events)
	return battleengine.MemberRef{}
}

// containsSideConditionEvent 报告事件序列是否包含指定的侧状态生命周期事件种类。
func containsSideConditionEvent(events []battleengine.Event, kind battleengine.EventKind) bool {
	for _, event := range events {
		if event.Kind() == kind {
			return true
		}
	}
	return false
}

// containsSkillFailure 报告事件序列是否包含指定原因的技能规则失败事件。
func containsSkillFailure(events []battleengine.Event, reason battleengine.SkillFailureReason) bool {
	for _, event := range events {
		if value, ok := event.(battleengine.SkillFailedEvent); ok && value.Reason == reason {
			return true
		}
	}
	return false
}

// resolveSideConditionDamage 返回第一方固定物理技能对第二方造成的实际伤害，使各侧状态测试在相同随机轨迹下
// 比较唯一变化的防守方状态。
func resolveSideConditionDamage(
	t *testing.T,
	defenderConditions battleengine.SideConditionSnapshot,
	damageClass battleengine.DamageClass,
) uint32 {
	t.Helper()
	attacker := newMember(1, "screen-attacker", 1_000, 1_000)
	attacker.Stats.Speed = 200
	attacker.Skills[0].Power = 80
	attacker.Skills[0].DamageClass = damageClass
	defender := newMember(1, "screen-defender", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "screen", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{attacker}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{defender}, Conditions: defenderConditions},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 9)
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
	for _, event := range result.Events {
		if value, ok := event.(battleengine.DamageAppliedEvent); ok && value.Actor.Side == battleengine.SideOne {
			return value.Amount
		}
	}
	t.Fatalf("未产生第一方伤害事件: %v", result.Events)
	return 0
}
