package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnAppliesAllOpponentScope 验证范围变化技能会逐一影响对侧全部仍在场成员，
// 且不会错误读取客户端提交的单体目标作为范围上限。
func TestResolveTurnAppliesAllOpponentScope(t *testing.T) {
	t.Parallel()
	leftLead, leftPartner, rightLead, rightPartner := targetScopeMembers()
	leftLead.Skills[0].DamageClass = battleengine.DamageClassStatus
	leftLead.Skills[0].Power = 0
	leftLead.Skills[0].TargetScope = battleengine.SkillTargetScopeAllAdjacentOpponents
	leftLead.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatAttack, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 100,
	}}

	state := targetScopeDoubleState(t, leftLead, leftPartner, rightLead, rightPartner)
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, targetScopeTurn(
		targetScopeSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		targetScopeSkillAction(battleengine.SideOne, 2, battleengine.SideTwo, 1),
		targetScopeSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
		targetScopeSkillAction(battleengine.SideTwo, 2, battleengine.SideOne, 1),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	for position := battleengine.SlotPosition(1); position <= 2; position++ {
		target, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: position})
		if !exists || target.StatStages[battleengine.StatAttack] != -1 {
			t.Fatalf("对侧槽位 %d 的攻击阶级 = %d，期望 -1", position, target.StatStages[battleengine.StatAttack])
		}
	}
}

// TestResolveTurnKeepsSelectedTargetOnItsCurrentDoubleSlot 验证双打单体技能提交的是目标槽位而不是成员身份。
//
// 目标先主动换人时，技能必须在实际执行阶段重新读取该槽位的当前成员；它既不能继续伤害已经离场的成员，
// 也不能为了寻找旧成员而错误命中同侧的另一名仍在场对手。
func TestResolveTurnKeepsSelectedTargetOnItsCurrentDoubleSlot(t *testing.T) {
	t.Parallel()
	leftLead, leftPartner, targetLeft, targetRight := targetScopeMembers()
	reserve := newMember(3, "scope-target-reserve", 500, 500)
	// 其余三名必须提交合法行动，才能让本用例只观察“目标换人后”的单体攻击行为。将它们配置为
	// 对满生命自身无副作用的变化技能，避免额外伤害污染目标断言。
	for _, member := range []*battleengine.MemberSnapshot{&leftPartner, &targetRight} {
		member.Skills[0].DamageClass = battleengine.DamageClassStatus
		member.Skills[0].Power = 0
		member.Skills[0].TargetScope = battleengine.SkillTargetScopeSelf
		member.Skills[0].HealingPercent = 1
	}

	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "selected-target-slot-double", ActiveSlotsPerSide: 2, TeamSize: 3},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{leftLead, leftPartner}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{targetLeft, targetRight, reserve}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 6)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, targetScopeTurn(
		targetScopeSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		targetScopeSkillAction(battleengine.SideOne, 2, 0, 0),
		battleengine.Action{
			Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
			Switch: &battleengine.SwitchAction{MemberPosition: 3},
		},
		targetScopeSkillAction(battleengine.SideTwo, 2, 0, 0),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	leftSlot, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !exists || leftSlot.Position != 3 || leftSlot.CurrentHP >= leftSlot.MaxHP {
		t.Fatalf("左侧目标槽位 = %+v，期望换入三号成员且受到伤害", leftSlot)
	}
	rightSlot, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 2})
	if !exists || rightSlot.Position != 2 || rightSlot.CurrentHP != rightSlot.MaxHP {
		t.Fatalf("右侧目标槽位 = %+v，期望保持未受伤", rightSlot)
	}
}

// TestResolveTurnAppliesUserSideScope 验证同侧范围不会校验无意义的占位 target，且会分别向
// 使用者和同侧伙伴应用 selectedTarget 形式的资料效果。
func TestResolveTurnAppliesUserSideScope(t *testing.T) {
	t.Parallel()
	leftLead, leftPartner, rightLead, rightPartner := targetScopeMembers()
	leftLead.Skills[0].DamageClass = battleengine.DamageClassStatus
	leftLead.Skills[0].Power = 0
	leftLead.Skills[0].TargetScope = battleengine.SkillTargetScopeUserSideActive
	leftLead.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatDefense, Target: battleengine.EffectTargetSelected, StageDelta: 1, ChancePercent: 100,
	}}

	state := targetScopeDoubleState(t, leftLead, leftPartner, rightLead, rightPartner)
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 2)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, targetScopeTurn(
		// 同侧范围的 target 仅保留为统一命令形状，不参与范围目标解析。
		targetScopeSkillAction(battleengine.SideOne, 1, 0, 0),
		targetScopeSkillAction(battleengine.SideOne, 2, battleengine.SideTwo, 1),
		targetScopeSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
		targetScopeSkillAction(battleengine.SideTwo, 2, battleengine.SideOne, 1),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	for position := battleengine.SlotPosition(1); position <= 2; position++ {
		member, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: position})
		if !exists || member.StatStages[battleengine.StatDefense] != 1 {
			t.Fatalf("同侧槽位 %d 的防御阶级 = %d，期望 1", position, member.StatStages[battleengine.StatDefense])
		}
	}
}

// TestResolveTurnSelectsOneRandomOpponent 验证随机对手范围在双打中恰好选择一个目标，并把这次选择
// 写入可重放的随机轨迹，而不是退化为客户端提交的单体目标。
func TestResolveTurnSelectsOneRandomOpponent(t *testing.T) {
	t.Parallel()
	leftLead, leftPartner, rightLead, rightPartner := targetScopeMembers()
	leftLead.Skills[0].DamageClass = battleengine.DamageClassStatus
	leftLead.Skills[0].Power = 0
	leftLead.Skills[0].SkillID = testID("scope-random")
	leftLead.Skills[0].TargetScope = battleengine.SkillTargetScopeRandomAdjacentOpponent
	leftLead.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatSpeed, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 100,
	}}

	state := targetScopeDoubleState(t, leftLead, leftPartner, rightLead, rightPartner)
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 3)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, targetScopeTurn(
		targetScopeSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		targetScopeSkillAction(battleengine.SideOne, 2, battleengine.SideTwo, 1),
		targetScopeSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
		targetScopeSkillAction(battleengine.SideTwo, 2, battleengine.SideOne, 1),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if len(result.RandomTrace) == 0 || result.RandomTrace[0].Bound != 2 ||
		result.RandomTrace[0].Reason != "random adjacent opponent target for "+testID("scope-random").String() {
		t.Fatalf("随机目标轨迹 = %+v", result.RandomTrace)
	}
	changed := 0
	for position := battleengine.SlotPosition(1); position <= 2; position++ {
		member, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: position})
		if member.StatStages[battleengine.StatSpeed] == -1 {
			changed++
		}
	}
	if changed != 1 {
		t.Fatalf("随机目标数 = %d，期望 1", changed)
	}
}

// TestResolveTurnResolvesSelfAndAllParticipantScopes 验证自身范围只命中使用者，而全体相邻范围
// 会命中除使用者外的全部场上成员，二者都不依赖客户端提交的占位目标。
func TestResolveTurnResolvesSelfAndAllParticipantScopes(t *testing.T) {
	t.Parallel()
	tests := []struct {
		// name 是 Go 子测试的稳定说明名称。
		name string
		// scope 是当前测试交给技能快照的冻结目标范围。
		scope battleengine.SkillTargetScope
		// expected 保存应当获得速度下降的“阵营/槽位”键集合。
		expected map[battleengine.SlotRef]bool
	}{
		{
			name: "自身", scope: battleengine.SkillTargetScopeSelf,
			expected: map[battleengine.SlotRef]bool{{Side: battleengine.SideOne, Position: 1}: true},
		},
		{
			name: "全体相邻", scope: battleengine.SkillTargetScopeAllAdjacentParticipants,
			expected: map[battleengine.SlotRef]bool{
				{Side: battleengine.SideOne, Position: 2}: true,
				{Side: battleengine.SideTwo, Position: 1}: true,
				{Side: battleengine.SideTwo, Position: 2}: true,
			},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			leftLead, leftPartner, rightLead, rightPartner := targetScopeMembers()
			leftLead.Skills[0].DamageClass = battleengine.DamageClassStatus
			leftLead.Skills[0].Power = 0
			leftLead.Skills[0].TargetScope = test.scope
			leftLead.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
				Stat: battleengine.StatSpeed, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 100,
			}}
			state := targetScopeDoubleState(t, leftLead, leftPartner, rightLead, rightPartner)
			random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 4)
			if err != nil {
				t.Fatalf("NewRandomSource() error = %v", err)
			}
			result, err := battleengine.ResolveTurn(state, targetScopeTurn(
				targetScopeSkillAction(battleengine.SideOne, 1, 0, 0),
				targetScopeSkillAction(battleengine.SideOne, 2, battleengine.SideTwo, 1),
				targetScopeSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
				targetScopeSkillAction(battleengine.SideTwo, 2, battleengine.SideOne, 1),
			), random)
			if err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
			for _, slot := range []battleengine.SlotRef{
				{Side: battleengine.SideOne, Position: 1}, {Side: battleengine.SideOne, Position: 2},
				{Side: battleengine.SideTwo, Position: 1}, {Side: battleengine.SideTwo, Position: 2},
			} {
				member, exists := result.State.ActiveMember(slot)
				if !exists {
					t.Fatalf("槽位 %+v 不存在", slot)
				}
				want := int8(0)
				if test.expected[slot] {
					want = -1
				}
				if member.StatStages[battleengine.StatSpeed] != want {
					t.Fatalf("槽位 %+v 的速度阶级 = %d，期望 %d", slot, member.StatStages[battleengine.StatSpeed], want)
				}
			}
		})
	}
}

// TestResolveTurnAppliesMultiTargetDamageModifier 验证同时命中两名对手时，每个目标都使用
// 比同随机轨迹下单体技能更低的现代范围伤害修正。
func TestResolveTurnAppliesMultiTargetDamageModifier(t *testing.T) {
	t.Parallel()
	leftLead, leftPartner, rightLead, rightPartner := targetScopeMembers()
	leftLead.Skills[0].SkillID = testID("spread-damage")
	leftLead.Skills[0].TargetScope = battleengine.SkillTargetScopeAllAdjacentOpponents
	spreadState := targetScopeDoubleState(t, leftLead, leftPartner, rightLead, rightPartner)

	singleLead, singlePartner, singleRightLead, singleRightPartner := targetScopeMembers()
	singleLead.Skills[0].SkillID = testID("spread-damage")
	singleState := targetScopeDoubleState(t, singleLead, singlePartner, singleRightLead, singleRightPartner)

	spreadRandom, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 5)
	if err != nil {
		t.Fatalf("NewRandomSource(spread) error = %v", err)
	}
	spreadResult, err := battleengine.ResolveTurn(spreadState, targetScopeTurn(
		targetScopeSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		targetScopeSkillAction(battleengine.SideOne, 2, battleengine.SideTwo, 1),
		targetScopeSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
		targetScopeSkillAction(battleengine.SideTwo, 2, battleengine.SideOne, 1),
	), spreadRandom)
	if err != nil {
		t.Fatalf("ResolveTurn(spread) error = %v", err)
	}

	singleRandom, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 5)
	if err != nil {
		t.Fatalf("NewRandomSource(single) error = %v", err)
	}
	singleResult, err := battleengine.ResolveTurn(singleState, targetScopeTurn(
		targetScopeSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		targetScopeSkillAction(battleengine.SideOne, 2, battleengine.SideTwo, 1),
		targetScopeSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
		targetScopeSkillAction(battleengine.SideTwo, 2, battleengine.SideOne, 1),
	), singleRandom)
	if err != nil {
		t.Fatalf("ResolveTurn(single) error = %v", err)
	}

	spreadDamage := targetScopeDamageEvents(spreadResult.Events, battleengine.SideOne, testID("spread-damage"))
	singleDamage := targetScopeDamageEvents(singleResult.Events, battleengine.SideOne, testID("spread-damage"))
	if len(spreadDamage) != 2 || len(singleDamage) != 1 {
		t.Fatalf("范围/单体伤害事件数量 = %d/%d，期望 2/1", len(spreadDamage), len(singleDamage))
	}
	if spreadDamage[0].Target.Position != 1 || spreadDamage[1].Target.Position != 2 {
		t.Fatalf("范围伤害目标 = %+v", spreadDamage)
	}
	if spreadDamage[0].Amount*4 > singleDamage[0].Amount*3 {
		t.Fatalf("范围伤害 %d 未使用 0.75 修正，单体伤害 = %d", spreadDamage[0].Amount, singleDamage[0].Amount)
	}
}

// targetScopeDamageEvents 从完整事件流中提取指定使用者与技能产生的直接伤害，避免其它槽位行动干扰断言。
func targetScopeDamageEvents(events []battleengine.Event, side battleengine.Side, skillID Identifier) []battleengine.DamageAppliedEvent {
	result := make([]battleengine.DamageAppliedEvent, 0, 2)
	for _, event := range events {
		damage, isDamage := event.(battleengine.DamageAppliedEvent)
		if !isDamage || damage.Actor.Side != side || damage.Actor.Position != 1 || damage.SkillID != skillID {
			continue
		}
		result = append(result, damage)
	}
	return result
}

// targetScopeMembers 创建所有目标范围测试共用的四名双打成员，并用速度确保首个行动稳定执行。
func targetScopeMembers() (
	battleengine.MemberSnapshot,
	battleengine.MemberSnapshot,
	battleengine.MemberSnapshot,
	battleengine.MemberSnapshot,
) {
	leftLead := newMember(1, "scope-lead", 500, 500)
	leftLead.Stats.Speed = 200
	leftPartner := newMember(2, "scope-partner", 500, 500)
	leftPartner.Stats.Speed = 150
	rightLead := newMember(1, "scope-target-one", 500, 500)
	rightLead.Stats.Speed = 60
	rightPartner := newMember(2, "scope-target-two", 500, 500)
	rightPartner.Stats.Speed = 50
	return leftLead, leftPartner, rightLead, rightPartner
}

// targetScopeDoubleState 创建具有两名上场成员的最小有效双打状态。
func targetScopeDoubleState(
	t *testing.T,
	leftLead battleengine.MemberSnapshot,
	leftPartner battleengine.MemberSnapshot,
	rightLead battleengine.MemberSnapshot,
	rightPartner battleengine.MemberSnapshot,
) battleengine.State {
	t.Helper()
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "target-scope-double", ActiveSlotsPerSide: 2, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{leftLead, leftPartner}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{rightLead, rightPartner}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	return state
}

// targetScopeTurn 为双打四个场上槽位构造同一回合的完整动作集合。
func targetScopeTurn(actions ...battleengine.Action) battleengine.TurnCommand {
	return battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: actions}
}

// targetScopeSkillAction 构造一个携带显式槽位位置的技能行动，便于范围测试验证 target 是否被忽略。
func targetScopeSkillAction(
	actorSide battleengine.Side,
	actorPosition battleengine.SlotPosition,
	targetSide battleengine.Side,
	targetPosition battleengine.SlotPosition,
) battleengine.Action {
	return battleengine.Action{
		Kind:  battleengine.ActionKindUseSkill,
		Actor: battleengine.SlotRef{Side: actorSide, Position: actorPosition},
		UseSkill: &battleengine.UseSkillAction{
			SkillPosition: 1,
			Target:        battleengine.SlotRef{Side: targetSide, Position: targetPosition},
		},
	}
}
