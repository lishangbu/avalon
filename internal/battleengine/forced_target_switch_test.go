package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnForcesTargetSwitchThroughCompleteSwitchInLifecycle 验证技能强制换人不会走一条简化的成员替换路径：
// 选择事件必须先于强制换人事件，换入成员仍要承受己方场地危害，而仅有一个健康后备时不应额外消耗随机数。
func TestResolveTurnForcesTargetSwitchThroughCompleteSwitchInLifecycle(t *testing.T) {
	t.Parallel()

	attacker := forceTargetSwitchUser(1, "force-switch-attacker")
	attacker.Stats.Speed = 200
	target := passiveMember(1, "force-switch-target", 1_000, 1_000)
	target.Stats.Speed = 50
	reserve := passiveMember(2, "force-switch-reserve", 1_000, 1_000)
	state := forceTargetSwitchState(t, attacker, []battleengine.MemberSnapshot{target, reserve}, battleengine.SideConditionSnapshot{SpikesLayers: 1})

	result := resolveForceTargetSwitchTurn(t, state, 11)
	selection, found := forcedTargetSwitchSelection(result.Events)
	if !found {
		t.Fatalf("事件流未记录强制换人选择: %v", eventKinds(result.Events))
	}
	if selection.Target != (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) ||
		selection.SelectedMember != (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 2}) ||
		len(selection.Candidates) != 1 || selection.Candidates[0] != selection.SelectedMember {
		t.Fatalf("强制换人选择事件 = %#v", selection)
	}
	if containsRandomReason(result.RandomTrace, "force target switch selection for "+testID("force-target-switch").String()) {
		t.Fatalf("唯一后备不应消耗强制换人随机数: %+v", result.RandomTrace)
	}
	if !containsForcedSwitch(result.Events, battleengine.SideTwo, 2) {
		t.Fatalf("事件流未记录进入成员 2 的强制换人: %v", eventKinds(result.Events))
	}
	if !containsSpikesDamage(result.Events, battleengine.SideTwo, 2, 125) {
		t.Fatalf("强制换入没有结算撒菱: %v", eventKinds(result.Events))
	}
	active, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !exists || active.Position != 2 || active.CurrentHP != 885 {
		t.Fatalf("强制换入后的活动成员 = %+v, %t，期望成员 2 且生命 885", active, exists)
	}
}

// TestResolveTurnSkipsForcedTargetSwitchWhenBlocked 验证强制换人严格要求目标没有替身、没有特性免疫且存在健康后备。三种阻止
// 情况都不能产生选择随机、选择事件或成员替换，避免把普通技能命中误记录为一次未完成的换人。
func TestResolveTurnSkipsForcedTargetSwitchWhenBlocked(t *testing.T) {
	t.Parallel()

	for _, test := range []struct {
		name       string
		members    []battleengine.MemberSnapshot
		configure  func(*battleengine.MemberSnapshot)
		wantActive battleengine.MemberPosition
	}{
		{
			name: "目标拥有替身",
			members: []battleengine.MemberSnapshot{
				passiveMember(1, "force-switch-substitute-target", 1_000, 1_000),
				passiveMember(2, "force-switch-substitute-reserve", 1_000, 1_000),
			},
			configure: func(target *battleengine.MemberSnapshot) {
				target.Stats.Speed = 300
				target.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
					Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser,
					ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
				}}
			},
			wantActive: 1,
		},
		{
			name: "目标特性免疫强制换人",
			members: []battleengine.MemberSnapshot{
				passiveMember(1, "force-switch-immune-target", 1_000, 1_000),
				passiveMember(2, "force-switch-immune-reserve", 1_000, 1_000),
			},
			configure: func(target *battleengine.MemberSnapshot) {
				target.ForcedSwitchImmunity = true
			},
			wantActive: 1,
		},
		{
			name: "没有健康后备",
			members: []battleengine.MemberSnapshot{
				passiveMember(1, "force-switch-no-reserve-target", 1_000, 1_000),
				passiveMember(2, "force-switch-fainted-reserve", 1_000, 0),
			},
			configure:  func(*battleengine.MemberSnapshot) {},
			wantActive: 1,
		},
	} {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			members := append([]battleengine.MemberSnapshot(nil), test.members...)
			test.configure(&members[0])
			attacker := forceTargetSwitchUser(1, "force-switch-blocked-attacker-"+test.name)
			attacker.Stats.Speed = 200
			state := forceTargetSwitchState(t, attacker, members, battleengine.SideConditionSnapshot{})

			result := resolveForceTargetSwitchTurn(t, state, 12)
			if _, found := forcedTargetSwitchSelection(result.Events); found {
				t.Fatalf("被阻止时不应存在强制换人选择: %v", eventKinds(result.Events))
			}
			if containsForcedSwitch(result.Events, battleengine.SideTwo, 2) {
				t.Fatalf("被阻止时不应发生强制换人: %v", eventKinds(result.Events))
			}
			if containsRandomReason(result.RandomTrace, "force target switch selection for "+testID("force-target-switch").String()) {
				t.Fatalf("被阻止时不应消耗强制换人随机数: %+v", result.RandomTrace)
			}
			active, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
			if !exists || active.Position != test.wantActive {
				t.Fatalf("被阻止后的活动成员 = %+v, %t，期望位置 %d", active, exists, test.wantActive)
			}
		})
	}
}

// TestResolveTurnIgnoreTargetAbilityEffectsBypassesForcedSwitchImmunity 验证无视目标防守特性的使用者可以越过
// 目标的技能强制换人免疫；替身、无后备等非特性阻止条件仍由各自规则处理。
func TestResolveTurnIgnoreTargetAbilityEffectsBypassesForcedSwitchImmunity(t *testing.T) {
	t.Parallel()
	attacker := forceTargetSwitchUser(1, "force-switch-ability-ignore-attacker")
	attacker.Stats.Speed = 200
	attacker.IgnoreTargetAbilityEffects = true
	target := passiveMember(1, "force-switch-ability-ignore-target", 1_000, 1_000)
	target.ForcedSwitchImmunity = true
	reserve := passiveMember(2, "force-switch-ability-ignore-reserve", 1_000, 1_000)
	result := resolveForceTargetSwitchTurn(t, forceTargetSwitchState(
		t, attacker, []battleengine.MemberSnapshot{target, reserve}, battleengine.SideConditionSnapshot{},
	), 26)
	if _, found := forcedTargetSwitchSelection(result.Events); !found || !containsForcedSwitch(result.Events, battleengine.SideTwo, 2) {
		t.Fatalf("无视目标特性后未执行强制换人: events=%v", eventKinds(result.Events))
	}
}

// TestResolveTurnUsesOneStableRandomChoiceForMultipleForcedSwitchReserves 验证多个健康后备按成员位置排序，并只
// 消耗一次专用随机值；选择事件、随机轨迹和实际强制换人必须指向同一个候选成员。
func TestResolveTurnUsesOneStableRandomChoiceForMultipleForcedSwitchReserves(t *testing.T) {
	t.Parallel()

	attacker := forceTargetSwitchUser(1, "force-switch-multiple-attacker")
	attacker.Stats.Speed = 200
	target := passiveMember(1, "force-switch-multiple-target", 1_000, 1_000)
	reserveThree := passiveMember(3, "force-switch-multiple-reserve-three", 1_000, 1_000)
	reserveTwo := passiveMember(2, "force-switch-multiple-reserve-two", 1_000, 1_000)
	state := forceTargetSwitchState(
		t, attacker, []battleengine.MemberSnapshot{target, reserveThree, reserveTwo}, battleengine.SideConditionSnapshot{},
	)

	result := resolveForceTargetSwitchTurn(t, state, 13)
	selection, found := forcedTargetSwitchSelection(result.Events)
	if !found {
		t.Fatalf("事件流未记录多后备强制换人选择: %v", eventKinds(result.Events))
	}
	if len(selection.Candidates) != 2 || selection.Candidates[0].Position != 2 || selection.Candidates[1].Position != 3 {
		t.Fatalf("候选顺序 = %+v，期望按成员位置升序", selection.Candidates)
	}
	trace, found := randomTraceByReason(result.RandomTrace, "force target switch selection for "+testID("force-target-switch").String())
	if !found || trace.Bound != 2 || trace.Value < 0 || trace.Value >= 2 {
		t.Fatalf("强制换人随机轨迹 = %+v, %t", trace, found)
	}
	if selection.SelectedMember != selection.Candidates[trace.Value] {
		t.Fatalf("随机选择 = %+v，候选 = %+v，轨迹 = %+v", selection.SelectedMember, selection.Candidates, trace)
	}
	if !containsForcedSwitch(result.Events, battleengine.SideTwo, selection.SelectedMember.Position) {
		t.Fatalf("实际强制换人没有使用随机选中成员: %v", eventKinds(result.Events))
	}
}

// forceTargetSwitchUser 创建仅包含强制目标换人规则的单体变化技能使用者，避免普通伤害、命中和暴击随机数掩盖
// 强制换人自身的随机消费边界。
func forceTargetSwitchUser(position battleengine.MemberPosition, creatureID string) battleengine.MemberSnapshot {
	member := newMember(position, creatureID, 1_000, 1_000)
	member.Skills[0].SkillID = testID("force-target-switch")
	member.Skills[0].DamageClass = battleengine.DamageClassStatus
	member.Skills[0].Power = 0
	member.Skills[0].ForceTargetSwitch = true
	return member
}

// passiveMember 创建一名使用自我回复变化技能的成员。该技能在被换入后仍可完成对方已提交的行动，却不消耗命中、
// 暴击或伤害随机数，从而让测试只观察强制换人的随机轨迹。
func passiveMember(position battleengine.MemberPosition, creatureID string, maxHP, currentHP uint32) battleengine.MemberSnapshot {
	member := newMember(position, creatureID, maxHP, currentHP)
	member.Skills[0].DamageClass = battleengine.DamageClassStatus
	member.Skills[0].Power = 0
	member.Skills[0].TargetScope = battleengine.SkillTargetScopeSelf
	member.Skills[0].HealingPercent = 1
	return member
}

// forceTargetSwitchState 创建单打强制换人测试的权威状态。第二方成员位置 1 固定占据唯一场上槽位，其余成员
// 由测试分别作为健康、倒下或乱序后备提供。
func forceTargetSwitchState(
	t *testing.T,
	attacker battleengine.MemberSnapshot,
	targetMembers []battleengine.MemberSnapshot,
	conditions battleengine.SideConditionSnapshot,
) battleengine.State {
	t.Helper()
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "force-target-switch", ActiveSlotsPerSide: 1, TeamSize: uint8(len(targetMembers))},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{attacker}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: targetMembers, Conditions: conditions},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	return state
}

// resolveForceTargetSwitchTurn 执行一回合双方已提交的行动。第二方的行动会在强制换人后由实际换入成员继续
// 执行，以覆盖槽位语义而不是把已提交行动错误绑定到旧成员身份。
func resolveForceTargetSwitchTurn(t *testing.T, state battleengine.State, seed uint64) battleengine.TurnResult {
	t.Helper()
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, seed)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    state.TurnNumber() + 1,
		Actions: []battleengine.Action{
			{
				Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}},
			},
			{
				Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
				UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}},
			},
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	return result
}

// forcedTargetSwitchSelection 从事件流中查找强制换人选择事件。
func forcedTargetSwitchSelection(events []battleengine.Event) (battleengine.ForcedTargetSwitchSelectedEvent, bool) {
	for _, event := range events {
		if selection, ok := event.(battleengine.ForcedTargetSwitchSelectedEvent); ok {
			return selection, true
		}
	}
	return battleengine.ForcedTargetSwitchSelectedEvent{}, false
}

// containsForcedSwitch 报告事件流是否包含指定阵营与目标成员的强制替换。
func containsForcedSwitch(events []battleengine.Event, side battleengine.Side, position battleengine.MemberPosition) bool {
	for _, event := range events {
		if switched, ok := event.(battleengine.ParticipantSwitchedEvent); ok && switched.Forced &&
			switched.Slot.Side == side && switched.NextMember.Position == position {
			return true
		}
	}
	return false
}

// containsSpikesDamage 报告事件流是否包含指定成员承受的撒菱伤害。
func containsSpikesDamage(events []battleengine.Event, side battleengine.Side, position battleengine.MemberPosition, amount uint32) bool {
	for _, event := range events {
		if damage, ok := event.(battleengine.SpikesDamageAppliedEvent); ok && damage.Target.Side == side &&
			damage.Target.Position == position && damage.Amount == amount {
			return true
		}
	}
	return false
}

// containsRandomReason 报告本回合随机轨迹是否包含指定用途。
func containsRandomReason(trace []battleengine.RandomTraceEntry, reason string) bool {
	_, found := randomTraceByReason(trace, reason)
	return found
}

// randomTraceByReason 返回指定用途的首个随机轨迹项。强制换人每次结算至多消耗一次，因此首项就是完整事实。
func randomTraceByReason(trace []battleengine.RandomTraceEntry, reason string) (battleengine.RandomTraceEntry, bool) {
	for _, entry := range trace {
		if entry.Reason == reason {
			return entry, true
		}
	}
	return battleengine.RandomTraceEntry{}, false
}
