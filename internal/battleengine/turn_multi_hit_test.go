package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnDeterminesMultiHitCount 验证标准 2 至 5 段技能使用公开的非均匀分布，并把段数选择
// 作为可回放随机轨迹的一部分。每个子测试仅保留一名无随机副作用的对手，保证断言不受对方行动干扰。
func TestResolveTurnDeterminesMultiHitCount(t *testing.T) {
	t.Parallel()
	tests := []struct {
		// name 是当前概率区间的稳定子测试名称。
		name string
		// roll 是 0 至 99 的命中段数随机接点。
		roll int32
		// expectedHits 是该接点应产生的实际伤害段数。
		expectedHits int
	}{
		{name: "两段", roll: 0, expectedHits: 2},
		{name: "三段", roll: 35, expectedHits: 3},
		{name: "四段", roll: 70, expectedHits: 4},
		{name: "五段", roll: 85, expectedHits: 5},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			state := multiHitState(t, 2, 5, 0, 1_000)
			trace := []battleengine.RandomTraceEntry{{
				Sequence: 1, Bound: 100, Reason: "multi-hit count for " + testID("multi-hit-skill").String(), Value: test.roll,
			}}
			for hit := 0; hit < test.expectedHits; hit++ {
				trace = append(trace,
					battleengine.RandomTraceEntry{Sequence: int32(2 + hit*2), Bound: 24, Reason: "critical hit for " + testID("multi-hit-skill").String(), Value: 1},
					battleengine.RandomTraceEntry{Sequence: int32(3 + hit*2), Bound: 16, Reason: "damage random for " + testID("multi-hit-skill").String(), Value: 15},
				)
			}
			random, err := battleengine.NewTracedRandom(trace)
			if err != nil {
				t.Fatalf("NewTracedRandom() error = %v", err)
			}
			result, err := battleengine.ResolveTurn(state, multiHitTurn(), random)
			if err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
			damages := multiHitDamageEvents(result.Events)
			if len(damages) != test.expectedHits {
				t.Fatalf("伤害段数 = %d，期望 %d，事件 = %+v", len(damages), test.expectedHits, result.Events)
			}
			if len(result.RandomTrace) == 0 || result.RandomTrace[0].Bound != 100 || result.RandomTrace[0].Value != test.roll {
				t.Fatalf("命中段数随机轨迹 = %+v", result.RandomTrace)
			}
		})
	}
}

// TestResolveTurnResolvesCriticalHitForEachMultiHit 验证多段伤害的每一段独立判定要害和伤害浮动，
// 而不是把首段判定错误复用于剩余段。
func TestResolveTurnResolvesCriticalHitForEachMultiHit(t *testing.T) {
	t.Parallel()
	state := multiHitState(t, 3, 3, 0, 1_000)
	random, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("multi-hit-skill").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("multi-hit-skill").String(), Value: 0},
		{Sequence: 3, Bound: 24, Reason: "critical hit for " + testID("multi-hit-skill").String(), Value: 0},
		{Sequence: 4, Bound: 16, Reason: "damage random for " + testID("multi-hit-skill").String(), Value: 8},
		{Sequence: 5, Bound: 24, Reason: "critical hit for " + testID("multi-hit-skill").String(), Value: 1},
		{Sequence: 6, Bound: 16, Reason: "damage random for " + testID("multi-hit-skill").String(), Value: 15},
	})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, multiHitTurn(), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	damages := multiHitDamageEvents(result.Events)
	if len(damages) != 3 {
		t.Fatalf("伤害段数 = %d，期望 3", len(damages))
	}
	for index, expectedCritical := range []bool{false, true, false} {
		if damages[index].CriticalHit != expectedCritical {
			t.Fatalf("第 %d 段要害 = %t，期望 %t", index+1, damages[index].CriticalHit, expectedCritical)
		}
	}
}

// TestResolveTurnUsesCriticalHitStageProbability 验证资料冻结的要害等级使用正确概率分母，并且必定
// 要害不会消耗额外随机数，从而不污染同一回合后续行动的随机轨迹。
func TestResolveTurnUsesCriticalHitStageProbability(t *testing.T) {
	t.Parallel()
	tests := []struct {
		// name 是要害等级概率分支的稳定子测试名称。
		name string
		// stage 是写入技能快照的要害等级。
		stage uint8
		// criticalBound 是本例期望的随机分母；0 表示必定要害无需随机接点。
		criticalBound int32
	}{
		{name: "一级", stage: 1, criticalBound: 8},
		{name: "二级", stage: 2, criticalBound: 2},
		{name: "三级必定要害", stage: 3, criticalBound: 0},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			state := multiHitState(t, 1, 1, test.stage, 1_000)
			trace := make([]battleengine.RandomTraceEntry, 0, 2)
			if test.criticalBound != 0 {
				trace = append(trace, battleengine.RandomTraceEntry{
					Sequence: 1, Bound: test.criticalBound, Reason: "critical hit for " + testID("multi-hit-skill").String(), Value: 0,
				})
			}
			trace = append(trace, battleengine.RandomTraceEntry{
				Sequence: int32(len(trace) + 1), Bound: 16, Reason: "damage random for " + testID("multi-hit-skill").String(), Value: 15,
			})
			random, err := battleengine.NewTracedRandom(trace)
			if err != nil {
				t.Fatalf("NewTracedRandom() error = %v", err)
			}
			result, err := battleengine.ResolveTurn(state, multiHitTurn(), random)
			if err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
			damages := multiHitDamageEvents(result.Events)
			if len(damages) != 1 || !damages[0].CriticalHit {
				t.Fatalf("要害伤害事件 = %+v", damages)
			}
			if test.criticalBound == 0 {
				if len(result.RandomTrace) != 1 || result.RandomTrace[0].Bound != 16 {
					t.Fatalf("必定要害的随机轨迹 = %+v", result.RandomTrace)
				}
			}
		})
	}
}

// TestResolveTurnAppliesAbilityCriticalHitStageBoost 验证使用者特性的固定要害等级会与技能自身等级相加，并且
// 达到必定要害后不会继续消费原本用于要害判定的随机数。
func TestResolveTurnAppliesAbilityCriticalHitStageBoost(t *testing.T) {
	t.Parallel()
	state := multiHitStateWithAttackerAbilityRules(t, 1, 1, 1, 1_000, false, false, 2, false)
	random, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{{
		Sequence: 1, Bound: 16, Reason: "damage random for " + testID("multi-hit-skill").String(), Value: 15,
	}})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, multiHitTurn(), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	damages := multiHitDamageEvents(result.Events)
	if len(damages) != 1 || !damages[0].CriticalHit {
		t.Fatalf("固定要害等级增益后的伤害事件 = %+v", damages)
	}
	if len(result.RandomTrace) != 1 || result.RandomTrace[0].Reason != "damage random for "+testID("multi-hit-skill").String() {
		t.Fatalf("必定要害不应消费要害随机数，轨迹 = %+v", result.RandomTrace)
	}
}

// TestResolveTurnAppliesAbilityMultiHitMaximum 验证可变连续命中最大段数特性直接选择资料声明上限，且不会消费
// 用于段数选择的随机数；每一个实际伤害段仍独立执行要害和伤害随机判定。
func TestResolveTurnAppliesAbilityMultiHitMaximum(t *testing.T) {
	t.Parallel()
	state := multiHitStateWithAttackerAbilityRules(t, 2, 5, 0, 1_000, false, false, 0, true)
	trace := make([]battleengine.RandomTraceEntry, 0, 10)
	for hit := 0; hit < 5; hit++ {
		trace = append(trace,
			battleengine.RandomTraceEntry{Sequence: int32(hit*2 + 1), Bound: 24, Reason: "critical hit for " + testID("multi-hit-skill").String(), Value: 1},
			battleengine.RandomTraceEntry{Sequence: int32(hit*2 + 2), Bound: 16, Reason: "damage random for " + testID("multi-hit-skill").String(), Value: 15},
		)
	}
	random, err := battleengine.NewTracedRandom(trace)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, multiHitTurn(), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if damages := multiHitDamageEvents(result.Events); len(damages) != 5 {
		t.Fatalf("最大连击特性后的伤害段数 = %d，期望 5", len(damages))
	}
	for _, entry := range result.RandomTrace {
		if entry.Reason == "multi-hit count for "+testID("multi-hit-skill").String() {
			t.Fatalf("最大连击特性不应消费段数随机数，轨迹 = %+v", result.RandomTrace)
		}
	}
}

// TestResolveTurnAppliesHeldItemMultiHitRangeOverride 验证指定道具只把原始 2–5 段随机技能收窄为随机 4–5 段。
//
// 道具不会把段数固定为四段或五段：两种边界均保留一条 Bound=2 的段数随机轨迹；原始区间不匹配或失去道具后
// 必须继续使用既有规则，避免把道具扩大为所有多段技能的通用最大段数效果。
func TestResolveTurnAppliesHeldItemMultiHitRangeOverride(t *testing.T) {
	t.Parallel()
	for _, test := range []struct {
		name           string
		roll           int32
		wantHitCount   int
		minHits        uint8
		maxHits        uint8
		holdsItem      bool
		wantCountBound int32
	}{
		{name: "随机下界为四段", roll: 0, wantHitCount: 4, minHits: 2, maxHits: 5, holdsItem: true, wantCountBound: 2},
		{name: "随机上界为五段", roll: 1, wantHitCount: 5, minHits: 2, maxHits: 5, holdsItem: true, wantCountBound: 2},
		{name: "不匹配原始区间时保持原规则", roll: 0, wantHitCount: 3, minHits: 3, maxHits: 5, holdsItem: true, wantCountBound: 3},
		{name: "失去道具后保持原规则", roll: 0, wantHitCount: 2, minHits: 2, maxHits: 5, holdsItem: false, wantCountBound: 100},
	} {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			state := multiHitStateWithHeldItemRangeOverride(t, test.minHits, test.maxHits, test.holdsItem)
			trace := []battleengine.RandomTraceEntry{{Sequence: 1, Bound: test.wantCountBound, Reason: "multi-hit count for " + testID("multi-hit-skill").String(), Value: test.roll}}
			for hit := 0; hit < test.wantHitCount; hit++ {
				trace = append(trace,
					battleengine.RandomTraceEntry{Sequence: int32(hit*2 + 2), Bound: 24, Reason: "critical hit for " + testID("multi-hit-skill").String(), Value: 1},
					battleengine.RandomTraceEntry{Sequence: int32(hit*2 + 3), Bound: 16, Reason: "damage random for " + testID("multi-hit-skill").String(), Value: 15},
				)
			}
			random, err := battleengine.NewTracedRandom(trace)
			if err != nil {
				t.Fatalf("NewTracedRandom() error = %v", err)
			}
			result, err := battleengine.ResolveTurn(state, multiHitTurn(), random)
			if err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
			if got := len(multiHitDamageEvents(result.Events)); got != test.wantHitCount || len(result.RandomTrace) == 0 || result.RandomTrace[0].Bound != test.wantCountBound {
				t.Fatalf("道具多段随机结果 = hits:%d trace:%+v，期望 hits:%d bound:%d", got, result.RandomTrace, test.wantHitCount, test.wantCountBound)
			}
		})
	}
}

// TestResolveTurnCriticalHitImmunityRetainsRandomTrace 验证目标特性会在要害判定完成后否决结果。必定要害
// 本来就不能消耗要害随机数；随机要害即使被免疫，也必须保留已消费的随机轨迹，避免后续随机结果发生偏移。
func TestResolveTurnCriticalHitImmunityRetainsRandomTrace(t *testing.T) {
	t.Parallel()
	tests := []struct {
		// name 是要害来源与随机轨迹组合的稳定名称。
		name string
		// stage 是技能冻结的要害等级；三级及以上为必定要害。
		stage uint8
		// trace 是本例结算所需的精确随机输入。
		trace []battleengine.RandomTraceEntry
		// expectedTraceLength 是要害免疫后仍应保留的随机轨迹长度。
		expectedTraceLength int
	}{
		{
			name:  "必定要害不消费要害随机数",
			stage: 3,
			trace: []battleengine.RandomTraceEntry{
				{Sequence: 1, Bound: 16, Reason: "damage random for " + testID("multi-hit-skill").String(), Value: 15},
			},
			expectedTraceLength: 1,
		},
		{
			name:  "随机要害被免疫后仍消费随机数",
			stage: 0,
			trace: []battleengine.RandomTraceEntry{
				{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("multi-hit-skill").String(), Value: 0},
				{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("multi-hit-skill").String(), Value: 15},
			},
			expectedTraceLength: 2,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			random, err := battleengine.NewTracedRandom(test.trace)
			if err != nil {
				t.Fatalf("NewTracedRandom() error = %v", err)
			}
			result, err := battleengine.ResolveTurn(multiHitStateWithCriticalHitImmunity(t, 1, 1, test.stage, 1_000, true), multiHitTurn(), random)
			if err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
			damages := multiHitDamageEvents(result.Events)
			if len(damages) != 1 || damages[0].CriticalHit {
				t.Fatalf("要害免疫后的伤害事件 = %+v", damages)
			}
			if len(result.RandomTrace) != test.expectedTraceLength {
				t.Fatalf("随机轨迹长度 = %d，期望 %d；轨迹 = %+v", len(result.RandomTrace), test.expectedTraceLength, result.RandomTrace)
			}
			if test.stage == 0 && result.RandomTrace[0].Reason != "critical hit for "+testID("multi-hit-skill").String() {
				t.Fatalf("随机要害轨迹未保留: %+v", result.RandomTrace)
			}
		})
	}
}

// TestResolveTurnIgnoreTargetAbilityEffectsBypassesCriticalHitImmunity 验证使用者的无视目标特性规则会让必定
// 要害继续作为要害伤害结算；必定要害本身仍不应凭空消费要害随机数。
func TestResolveTurnIgnoreTargetAbilityEffectsBypassesCriticalHitImmunity(t *testing.T) {
	t.Parallel()
	random, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 16, Reason: "damage random for " + testID("multi-hit-skill").String(), Value: 15},
	})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	state := multiHitStateWithTargetAbilityIgnore(t, 1, 1, 3, 1_000, true, true)
	result, err := battleengine.ResolveTurn(state, multiHitTurn(), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	damages := multiHitDamageEvents(result.Events)
	if len(damages) != 1 || !damages[0].CriticalHit || len(result.RandomTrace) != 1 {
		t.Fatalf("无视目标要害免疫后的结果: damages=%+v trace=%+v", damages, result.RandomTrace)
	}
}

// TestResolveTurnStopsMultiHitAfterTargetFaints 验证目标在前一段倒下时立即停止后续段数，并且不再
// 读取不存在的要害或伤害随机接点。
func TestResolveTurnStopsMultiHitAfterTargetFaints(t *testing.T) {
	t.Parallel()
	state := multiHitState(t, 3, 3, 0, 1)
	random, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("multi-hit-skill").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("multi-hit-skill").String(), Value: 15},
	})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, multiHitTurn(), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if damages := multiHitDamageEvents(result.Events); len(damages) != 1 {
		t.Fatalf("目标倒下后的伤害段数 = %d，期望 1", len(damages))
	}
	if len(result.RandomTrace) != 2 {
		t.Fatalf("目标倒下后的随机轨迹 = %+v", result.RandomTrace)
	}
}

// multiHitState 创建一方使用伤害多段技能、另一方使用无随机状态技能的最小有效单打状态。
func multiHitState(t *testing.T, minimum, maximum, criticalStage uint8, targetHP uint32) battleengine.State {
	return multiHitStateWithCriticalHitImmunity(t, minimum, maximum, criticalStage, targetHP, false)
}

// multiHitStateWithCriticalHitImmunity 创建可选要害免疫目标的最小有效单打状态。
func multiHitStateWithCriticalHitImmunity(
	t *testing.T,
	minimum, maximum, criticalStage uint8,
	targetHP uint32,
	criticalHitImmunity bool,
) battleengine.State {
	return multiHitStateWithTargetAbilityIgnore(t, minimum, maximum, criticalStage, targetHP, criticalHitImmunity, false)
}

// multiHitStateWithTargetAbilityIgnore 创建可选要害免疫目标及无视目标特性使用者的最小有效单打状态。
func multiHitStateWithTargetAbilityIgnore(
	t *testing.T,
	minimum, maximum, criticalStage uint8,
	targetHP uint32,
	criticalHitImmunity bool,
	ignoreTargetAbilityEffects bool,
) battleengine.State {
	return multiHitStateWithAttackerAbilityRules(
		t, minimum, maximum, criticalStage, targetHP, criticalHitImmunity, ignoreTargetAbilityEffects, 0, false,
	)
}

// multiHitStateWithAttackerAbilityRules 创建包含攻击方固定要害等级或最大连击特性的最小有效单打状态。
// 这两个字段都放在使用者上，避免测试意外把目标防守特性或技能资料变更误当成特性结算。
func multiHitStateWithAttackerAbilityRules(
	t *testing.T,
	minimum, maximum, criticalStage uint8,
	targetHP uint32,
	criticalHitImmunity bool,
	ignoreTargetAbilityEffects bool,
	criticalHitStageBoost uint8,
	multiHitMaximum bool,
) battleengine.State {
	t.Helper()
	left := newMember(1, "multi-hit-user", 1_000, 1_000)
	left.Stats.Speed = 200
	left.IgnoreTargetAbilityEffects = ignoreTargetAbilityEffects
	left.CriticalHitStageBoost = criticalHitStageBoost
	left.MultiHitMaximum = multiHitMaximum
	left.Skills[0].SkillID = testID("multi-hit-skill")
	left.Skills[0].MinHits = minimum
	left.Skills[0].MaxHits = maximum
	left.Skills[0].CriticalHitStage = criticalStage
	right := newMember(1, "multi-hit-target", 1_000, targetHP)
	right.CriticalHitImmunity = criticalHitImmunity
	right.Stats.Speed = 10
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatAttack, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 0,
	}}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "multi-hit", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	return state
}

// multiHitStateWithHeldItemRangeOverride 创建使用可选多段范围道具的最小有效单打状态。
//
// 道具投影放在成员快照而非技能快照中，确保后续失去或转移道具时同一技能会立刻回到其冻结的原始段数范围。
func multiHitStateWithHeldItemRangeOverride(
	t *testing.T,
	minimum, maximum uint8,
	holdsItem bool,
) battleengine.State {
	t.Helper()
	left := newMember(1, "multi-hit-user", 1_000, 1_000)
	left.Stats.Speed = 200
	left.Skills[0].SkillID = testID("multi-hit-skill")
	left.Skills[0].MinHits = minimum
	left.Skills[0].MaxHits = maximum
	if holdsItem {
		left.ItemID = testID("multi-hit-range-item")
		left.HeldItemMultiHitCountMinimum = 4
		left.HeldItemMultiHitCountMaximum = 5
		left.HeldItemMultiHitRequiredMinimum = 2
		left.HeldItemMultiHitRequiredMaximum = 5
	}
	right := newMember(1, "multi-hit-target", 1_000, 1_000)
	// 让目标先执行无副作用的等待技能，避免多段攻击后的濒死状态干扰同一回合的第二个行动。
	right.Stats.Speed = 300
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	// 最小回复使等待动作通过命令层的可执行效果校验，不消费多段命中的随机轨迹。
	right.Skills[0].HealingPercent = 1
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "multi-hit-held-item", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	return state
}

// multiHitTurn 返回多段技能测试所需的完整第一回合命令集合。
func multiHitTurn() battleengine.TurnCommand {
	return battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			{
				Kind:  battleengine.ActionKindUseSkill,
				Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				UseSkill: &battleengine.UseSkillAction{
					SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
				},
			},
			{
				Kind:  battleengine.ActionKindUseSkill,
				Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
				UseSkill: &battleengine.UseSkillAction{
					SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				},
			},
		},
	}
}

// multiHitDamageEvents 仅提取测试技能产生的伤害事件，忽略对方状态技能和回合末事件。
func multiHitDamageEvents(events []battleengine.Event) []battleengine.DamageAppliedEvent {
	result := make([]battleengine.DamageAppliedEvent, 0, 5)
	for _, event := range events {
		damage, ok := event.(battleengine.DamageAppliedEvent)
		if ok && damage.SkillID == testID("multi-hit-skill") {
			result = append(result, damage)
		}
	}
	return result
}
