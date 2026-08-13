package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnPriorityMoveImmunityForSide 验证先制技能侧免疫会在命中和伤害之前阻止对手正优先度技能。
//
// 覆盖目标自身保护、双打伙伴保护、普通优先度不触发及攻击方无视目标特性四条边界。双打断言还要求事件
// 精确记录真正提供特性的伙伴，避免回放把侧范围防守错误归因到被保护的目标。
func TestResolveTurnPriorityMoveImmunityForSide(t *testing.T) {
	t.Parallel()
	t.Run("持有成员保护自身", func(t *testing.T) {
		t.Parallel()
		result := resolvePriorityMoveImmunityForSideTurn(t, true, false, false, false)
		assertPriorityMoveImmunityBlocked(t, result, 1)
	})
	t.Run("持有成员保护同侧伙伴", func(t *testing.T) {
		t.Parallel()
		// 由二号位持有特性并扩展到伙伴，才能验证事件中的 Blocker 指向实际提供侧保护的成员。
		result := resolvePriorityMoveImmunityForSideTurn(t, false, true, false, false)
		assertPriorityMoveImmunityBlocked(t, result, 2)
	})
	t.Run("普通优先度技能不受阻止", func(t *testing.T) {
		t.Parallel()
		result := resolvePriorityMoveImmunityForSideTurn(t, true, true, false, true)
		assertPriorityMoveImmunityPassed(t, result)
	})
	t.Run("无视目标特性可以绕过", func(t *testing.T) {
		t.Parallel()
		result := resolvePriorityMoveImmunityForSideTurn(t, true, true, true, false)
		assertPriorityMoveImmunityPassed(t, result)
	})
}

// priorityMoveImmunityForSideResult 汇总测试所需的目标状态和侧范围阻止事件。
type priorityMoveImmunityForSideResult struct {
	// protected 是本次攻击明确选择的目标成员；它可能由自身或其同侧伙伴保护。
	protected battleengine.MemberSnapshot
	// blocked 是攻击方先制技能被阻止时写入的事件；未阻止时保持 nil。
	blocked *battleengine.SkillBlockedEvent
}

// resolvePriorityMoveImmunityForSideTurn 构造固定双打场景并结算一次先制物理技能。
//
// abilityOnTarget 决定目标自己是否持有规则，protectsAllies 决定伙伴特性是否覆盖目标，
// ignoreTargetAbilities 决定攻击方是否无视目标侧特性，ordinaryPriority 用于构造优先度为零的对照技能。
func resolvePriorityMoveImmunityForSideTurn(
	t *testing.T,
	abilityOnTarget, protectsAllies, ignoreTargetAbilities, ordinaryPriority bool,
) priorityMoveImmunityForSideResult {
	t.Helper()
	attacker := newMember(1, "priority-side-attacker", 1_000, 1_000)
	attacker.Stats.Speed = 300
	attacker.IgnoreTargetAbilityEffects = ignoreTargetAbilities
	attacker.Skills[0].SkillID = testID("priority-side-skill")
	attacker.Skills[0].Priority = 1
	if ordinaryPriority {
		attacker.Skills[0].Priority = 0
	}
	ally := newMember(2, "priority-side-attacker-ally", 1_000, 1_000)
	ally.Stats.Speed = 200
	protected := newMember(1, "priority-side-protected", 1_000, 1_000)
	protected.Stats.Speed = 100
	protected.PriorityMoveImmunityForSideEnabled = abilityOnTarget
	blocker := newMember(2, "priority-side-blocker", 1_000, 1_000)
	blocker.Stats.Speed = 90
	blocker.PriorityMoveImmunityForSideEnabled = !abilityOnTarget
	blocker.PriorityMoveImmunityForSideProtectsAllies = protectsAllies

	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "priority-move-immunity-for-side", ActiveSlotsPerSide: 2, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{attacker, ally}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{protected, blocker}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	resolved, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			priorityMoveImmunityUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
			priorityMoveImmunityUseSkillAction(battleengine.SideOne, 2, battleengine.SideTwo, 2),
			priorityMoveImmunityUseSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
			priorityMoveImmunityUseSkillAction(battleengine.SideTwo, 2, battleengine.SideOne, 2),
		},
	}, mustRandom(t, 947))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	result := priorityMoveImmunityForSideResult{}
	for _, event := range resolved.Events {
		value, ok := event.(battleengine.SkillBlockedEvent)
		if ok && value.SkillID == testID("priority-side-skill") {
			copied := value
			result.blocked = &copied
		}
	}
	member, found := resolved.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found {
		t.Fatal("结算后未找到被保护目标")
	}
	result.protected = member
	return result
}

// priorityMoveImmunityUseSkillAction 创建测试中每个场上成员都必须提交的单体技能行动。
func priorityMoveImmunityUseSkillAction(
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

// assertPriorityMoveImmunityBlocked 断言技能没有造成伤害，并且事件精确指向提供侧保护的成员。
func assertPriorityMoveImmunityBlocked(t *testing.T, result priorityMoveImmunityForSideResult, blockerPosition battleengine.MemberPosition) {
	t.Helper()
	if result.protected.CurrentHP != result.protected.MaxHP || result.blocked == nil ||
		result.blocked.Reason != battleengine.SkillBlockReasonPriorityMoveImmunityForSide || result.blocked.Blocker == nil ||
		result.blocked.Blocker.Side != battleengine.SideTwo || result.blocked.Blocker.Position != blockerPosition {
		t.Fatalf("先制技能侧免疫阻止结果 = protected:%+v, blocked:%+v", result.protected, result.blocked)
	}
}

// assertPriorityMoveImmunityPassed 断言侧范围规则未触发且目标正常承受攻击方技能伤害。
func assertPriorityMoveImmunityPassed(t *testing.T, result priorityMoveImmunityForSideResult) {
	t.Helper()
	if result.protected.CurrentHP >= result.protected.MaxHP || result.blocked != nil {
		t.Fatalf("先制技能侧免疫未阻止结果 = protected:%+v, blocked:%+v", result.protected, result.blocked)
	}
}
