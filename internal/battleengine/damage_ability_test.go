package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnIgnoreOpponentDamageStatStages 验证特性只在本次普通伤害公式中屏蔽对手相关能力阶级。
//
// 使用者持有规则时必须忽略目标的防御阶级；目标持有规则时必须忽略使用者的攻击阶级。两种情况都不能把
// StatStages 写回权威状态，否则同回合其它目标与后续回合会观察到被错误清除的强化。
func TestResolveTurnIgnoreOpponentDamageStatStages(t *testing.T) {
	t.Parallel()
	t.Run("目标无视使用者攻击阶级", func(t *testing.T) {
		t.Parallel()
		boosted := resolveDamageStageAbilityTurn(t, 2, 0, false, false, false, nil)
		ignored := resolveDamageStageAbilityTurn(t, 2, 0, false, true, false, nil)
		neutral := resolveDamageStageAbilityTurn(t, 0, 0, false, false, false, nil)
		if ignored.damage != neutral.damage || ignored.damage >= boosted.damage {
			t.Fatalf("目标无视攻击阶级后的伤害 = %d，普通强化伤害 = %d，原始阶级伤害 = %d", ignored.damage, boosted.damage, neutral.damage)
		}
		if got := ignored.attacker.StatStages[battleengine.StatAttack]; got != 2 {
			t.Fatalf("使用者攻击阶级被错误写回为 %d，期望保留 2", got)
		}
	})
	t.Run("使用者无视目标防御阶级", func(t *testing.T) {
		t.Parallel()
		boosted := resolveDamageStageAbilityTurn(t, 0, 2, false, false, false, nil)
		ignored := resolveDamageStageAbilityTurn(t, 0, 2, true, false, false, nil)
		neutral := resolveDamageStageAbilityTurn(t, 0, 0, false, false, false, nil)
		if ignored.damage != neutral.damage || ignored.damage <= boosted.damage {
			t.Fatalf("使用者无视防御阶级后的伤害 = %d，普通防御强化伤害 = %d，原始阶级伤害 = %d", ignored.damage, boosted.damage, neutral.damage)
		}
		if got := ignored.target.StatStages[battleengine.StatDefense]; got != 2 {
			t.Fatalf("目标防御阶级被错误写回为 %d，期望保留 2", got)
		}
	})
	t.Run("使用者无视目标特性时不受目标伤害阶级规则阻止", func(t *testing.T) {
		t.Parallel()
		boosted := resolveDamageStageAbilityTurn(t, 2, 0, false, false, false, nil)
		bypassed := resolveDamageStageAbilityTurn(t, 2, 0, false, true, true, nil)
		if bypassed.damage != boosted.damage {
			t.Fatalf("无视目标特性后的伤害 = %d，期望恢复为攻击强化伤害 %d", bypassed.damage, boosted.damage)
		}
	})
}

// TestResolveTurnReceivedContactDamageHalved 验证特性只降低有效接触伤害，并遵守无视目标特性与接触抑制边界。
func TestResolveTurnReceivedContactDamageHalved(t *testing.T) {
	t.Parallel()
	normal := resolveDamageStageAbilityTurn(t, 0, 0, false, false, false, &contactDamageTestOptions{makesContact: true})
	halved := resolveDamageStageAbilityTurn(t, 0, 0, false, false, false, &contactDamageTestOptions{makesContact: true, targetHalvesContactDamage: true})
	if halved.damage == 0 || halved.damage >= normal.damage || halved.damage*2 != normal.damage {
		t.Fatalf("有效接触减伤 = %d，普通伤害 = %d，期望为精确二分之一", halved.damage, normal.damage)
	}
	nonContact := resolveDamageStageAbilityTurn(t, 0, 0, false, false, false, &contactDamageTestOptions{targetHalvesContactDamage: true})
	suppressed := resolveDamageStageAbilityTurn(t, 0, 0, false, false, false, &contactDamageTestOptions{makesContact: true, attackerSuppressesContact: true, targetHalvesContactDamage: true})
	bypassed := resolveDamageStageAbilityTurn(t, 0, 0, false, false, true, &contactDamageTestOptions{makesContact: true, targetHalvesContactDamage: true})
	if nonContact.damage != normal.damage || suppressed.damage != normal.damage || bypassed.damage != normal.damage {
		t.Fatalf("接触减伤边界错误: normal=%d nonContact=%d suppressed=%d bypassed=%d", normal.damage, nonContact.damage, suppressed.damage, bypassed.damage)
	}
}

// TestResolveTurnReceivedFireDamageDoubled 验证特性只放大当前有效火属性伤害，并遵守目标特性穿透边界。
func TestResolveTurnReceivedFireDamageDoubled(t *testing.T) {
	t.Parallel()
	fire := resolveDamageStageAbilityTurn(t, 0, 0, false, false, false, &contactDamageTestOptions{usesFire: true})
	doubled := resolveDamageStageAbilityTurn(t, 0, 0, false, false, false, &contactDamageTestOptions{usesFire: true, targetDoublesFireDamage: true})
	if doubled.damage != fire.damage*2 {
		t.Fatalf("火属性伤害翻倍 = %d，基础火属性伤害 = %d", doubled.damage, fire.damage)
	}
	nonFireBase := resolveDamageStageAbilityTurn(t, 0, 0, false, false, false, nil)
	nonFire := resolveDamageStageAbilityTurn(t, 0, 0, false, false, false, &contactDamageTestOptions{targetDoublesFireDamage: true})
	bypassed := resolveDamageStageAbilityTurn(t, 0, 0, false, false, true, &contactDamageTestOptions{usesFire: true, targetDoublesFireDamage: true})
	if nonFire.damage != nonFireBase.damage || bypassed.damage != fire.damage {
		t.Fatalf("火属性弱点边界错误: fire=%d nonFireBase=%d nonFire=%d bypassed=%d", fire.damage, nonFireBase.damage, nonFire.damage, bypassed.damage)
	}
}

// contactDamageTestOptions 收敛接触伤害特性测试的独立开关，避免影响能力阶级测试的固定场景。
type contactDamageTestOptions struct {
	// usesFire 指定攻击技能是否使用规则快照中冻结的火属性 Identifier。
	usesFire bool
	// makesContact 指定攻击技能资料是否带有静态接触标签。
	makesContact bool
	// attackerSuppressesContact 指定攻击方特性是否在运行期取消有效接触事实。
	attackerSuppressesContact bool
	// targetHalvesContactDamage 指定目标特性是否只将有效接触伤害减半。
	targetHalvesContactDamage bool
	// targetDoublesFireDamage 指定目标特性是否只将当前有效火属性伤害翻倍。
	targetDoublesFireDamage bool
}

// damageStageAbilityResult 是伤害能力阶级规则测试需要观察的最小权威结果。
type damageStageAbilityResult struct {
	// damage 是第一方技能本次造成的实际伤害。
	damage uint32
	// attacker 是结算后的使用者状态，用于断言特性不会篡改其真实能力阶级。
	attacker battleengine.MemberSnapshot
	// target 是结算后的目标状态，用于断言特性不会篡改其真实能力阶级。
	target battleengine.MemberSnapshot
}

// resolveDamageStageAbilityTurn 使用固定随机轨迹执行单段物理伤害，避免随机浮动掩盖能力阶级语义。
func resolveDamageStageAbilityTurn(
	t *testing.T,
	attackerStage, defenderStage int8,
	attackerIgnores, defenderIgnores bool,
	attackerIgnoresTargetAbilities bool,
	contactOptions *contactDamageTestOptions,
) damageStageAbilityResult {
	t.Helper()
	attacker := newMember(1, "damage-stage-attacker", 1_000, 1_000)
	attacker.Stats.Speed = 110
	attacker.Skills[0].SkillID = testID("damage-stage-skill")
	attacker.StatStages = map[battleengine.Stat]int8{battleengine.StatAttack: attackerStage}
	attacker.IgnoreOpponentDamageStatStages = attackerIgnores
	attacker.IgnoreTargetAbilityEffects = attackerIgnoresTargetAbilities
	if contactOptions != nil {
		if contactOptions.usesFire {
			attacker.Skills[0].ElementID = testID("fire")
		}
		attacker.Skills[0].MakesContact = contactOptions.makesContact
		attacker.ContactSuppression = contactOptions.attackerSuppressesContact
	}
	target := newMember(1, "damage-stage-target", 1_000, 1_000)
	target.Stats.Speed = 90
	target.StatStages = map[battleengine.Stat]int8{battleengine.StatDefense: defenderStage}
	target.IgnoreOpponentDamageStatStages = defenderIgnores
	if contactOptions != nil {
		target.ReceivedContactDamageHalved = contactOptions.targetHalvesContactDamage
		target.ReceivedFireDamageDoubled = contactOptions.targetDoublesFireDamage
	}
	target.Skills[0].SkillID = testID("damage-stage-pass")
	target.Skills[0].DamageClass = battleengine.DamageClassStatus
	target.Skills[0].Power = 0
	target.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatAttack, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 0,
	}}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "damage-stage-ability", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]battleengine.Identifier{"fire": testID("fire")}},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{attacker}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("damage-stage-skill").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("damage-stage-skill").String(), Value: 15},
	})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
				UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	var damage uint32
	for _, event := range result.Events {
		if applied, ok := event.(battleengine.DamageAppliedEvent); ok && applied.SkillID == testID("damage-stage-skill") {
			damage = applied.Amount
			break
		}
	}
	if damage == 0 {
		t.Fatalf("伤害事件缺失: %+v", result.Events)
	}
	resolvedAttacker, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found {
		t.Fatal("结算后使用者不存在")
	}
	resolvedTarget, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found {
		t.Fatal("结算后目标不存在")
	}
	return damageStageAbilityResult{damage: damage, attacker: resolvedAttacker, target: resolvedTarget}
}
