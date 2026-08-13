package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnDamagedForceSelfSwitchItem 验证受伤自换道具只在正实际伤害后消耗，并经统一换入生命周期替换
// 持有者。唯一健康后备不消费随机数，选择事件必须先于强制换人事件。
func TestResolveTurnDamagedForceSelfSwitchItem(t *testing.T) {
	t.Parallel()
	attacker := fixedDamageUser(1, "eject-button-attacker")
	target := passiveMember(1, "eject-button-holder", 1_000, 1_000)
	target.ItemID = testID("eject-button")
	target.DamagedForceSelfSwitch = true
	reserve := passiveMember(2, "eject-button-reserve", 1_000, 1_000)

	result := resolveForceTargetSwitchTurn(t, forceTargetSwitchState(t, attacker, []battleengine.MemberSnapshot{target, reserve}, battleengine.SideConditionSnapshot{}), 61)
	selection, found := itemForcedSwitchSelection(result.Events)
	if !found || selection.Source != (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) ||
		selection.Target != selection.Source || selection.ItemID != testID("eject-button") || selection.SelectedMember != (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 2}) {
		t.Fatalf("受伤自换选择事件 = %#v, found=%t", selection, found)
	}
	if containsRandomReason(result.RandomTrace, "item forced switch selection for "+testID("eject-button").String()) ||
		!containsForcedSwitch(result.Events, battleengine.SideTwo, 2) {
		t.Fatalf("受伤自换随机或换人事件错误: trace=%+v events=%v", result.RandomTrace, eventKinds(result.Events))
	}
}

// TestResolveTurnDamagedForceAttackerSwitchItem 验证受伤换攻击者道具由持有者消耗，却替换攻击者；攻击者的
// 强制换人免疫必须阻止整个规则，避免反制道具绕过特性边界。
func TestResolveTurnDamagedForceAttackerSwitchItem(t *testing.T) {
	t.Parallel()
	for _, test := range []struct {
		name         string
		immune       bool
		wantSwitched bool
	}{
		{name: "攻击者没有免疫", wantSwitched: true},
		{name: "攻击者拥有免疫", immune: true},
	} {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			attacker := fixedDamageUser(1, "red-card-attacker-"+test.name)
			attacker.ForcedSwitchImmunity = test.immune
			attackerReserve := passiveMember(2, "red-card-attacker-reserve-"+test.name, 1_000, 1_000)
			holder := passiveMember(1, "red-card-holder-"+test.name, 1_000, 1_000)
			holder.ItemID = testID("red-card")
			holder.DamagedForceAttackerSwitch = true
			state, err := battleengine.NewState(battleengine.InitialState{
				Format: battleengine.FormatSnapshot{Code: "red-card", ActiveSlotsPerSide: 1, TeamSize: 2}, Rules: battleengine.RuleSnapshot{SchemaVersion: 1},
				Sides: []battleengine.SideSnapshot{
					{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{attacker, attackerReserve}},
					{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{holder}},
				},
			})
			if err != nil {
				t.Fatalf("NewState() error = %v", err)
			}
			result := resolveForceTargetSwitchTurn(t, state, 62)
			_, selected := itemForcedSwitchSelection(result.Events)
			if selected != test.wantSwitched || containsForcedSwitch(result.Events, battleengine.SideOne, 2) != test.wantSwitched {
				t.Fatalf("受伤换攻击者结果错误: selected=%t events=%v", selected, eventKinds(result.Events))
			}
		})
	}
}

// TestResolveTurnNegativeStatStageForceSelfSwitchItem 验证能力下降自换道具只响应实际负能力变化，并在技能目标
// 强制换人之前完成自身替换。此规则不依赖技能伤害，因此变化技能也能正确触发。
func TestResolveTurnNegativeStatStageForceSelfSwitchItem(t *testing.T) {
	t.Parallel()
	attacker := newMember(1, "eject-pack-attacker", 1_000, 1_000)
	attacker.Skills[0].DamageClass = battleengine.DamageClassStatus
	attacker.Skills[0].Power = 0
	attacker.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat:          battleengine.StatDefense,
		Target:        battleengine.EffectTargetSelected,
		StageDelta:    -1,
		ChancePercent: 100,
	}}
	target := passiveMember(1, "eject-pack-holder", 1_000, 1_000)
	target.ItemID = testID("eject-pack")
	target.NegativeStatStageForceSelfSwitch = true
	reserve := passiveMember(2, "eject-pack-reserve", 1_000, 1_000)

	result := resolveForceTargetSwitchTurn(t, forceTargetSwitchState(t, attacker, []battleengine.MemberSnapshot{target, reserve}, battleengine.SideConditionSnapshot{}), 63)
	selection, found := itemForcedSwitchSelection(result.Events)
	if !found || selection.ItemID != testID("eject-pack") || selection.Target != (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) ||
		!containsForcedSwitch(result.Events, battleengine.SideTwo, 2) {
		t.Fatalf("能力下降自换结果 = %#v, found=%t events=%v", selection, found, eventKinds(result.Events))
	}
}

// TestResolveTurnDamagedForceSelfSwitchItemUsesOneRandomChoice 验证多个健康后备时，道具规则只消费一次专用随机数，
// 且选择事件、随机轨迹和实际换入成员始终指向同一成员。
func TestResolveTurnDamagedForceSelfSwitchItemUsesOneRandomChoice(t *testing.T) {
	t.Parallel()
	attacker := fixedDamageUser(1, "eject-button-random-attacker")
	target := passiveMember(1, "eject-button-random-holder", 1_000, 1_000)
	target.ItemID = testID("eject-button-random")
	target.DamagedForceSelfSwitch = true
	reserveThree := passiveMember(3, "eject-button-random-reserve-three", 1_000, 1_000)
	reserveTwo := passiveMember(2, "eject-button-random-reserve-two", 1_000, 1_000)

	result := resolveForceTargetSwitchTurn(t, forceTargetSwitchState(t, attacker, []battleengine.MemberSnapshot{target, reserveThree, reserveTwo}, battleengine.SideConditionSnapshot{}), 64)
	selection, found := itemForcedSwitchSelection(result.Events)
	trace, traced := randomTraceByReason(result.RandomTrace, "item forced switch selection for "+testID("eject-button-random").String())
	if !found || !traced || trace.Bound != 2 || selection.SelectedMember != selection.Candidates[trace.Value] ||
		!containsForcedSwitch(result.Events, battleengine.SideTwo, selection.SelectedMember.Position) {
		t.Fatalf("多后备道具换人结果: selection=%#v trace=%+v found=%t traced=%t", selection, trace, found, traced)
	}
}

// fixedDamageUser 创建使用固定直接伤害的单体技能成员，避免测试依赖要害、伤害浮动与属性相性随机数。
func fixedDamageUser(position battleengine.MemberPosition, creatureID string) battleengine.MemberSnapshot {
	member := newMember(position, creatureID, 1_000, 1_000)
	member.Skills[0].SkillID = testID("fixed-damage-" + creatureID)
	member.Skills[0].DamageClass = battleengine.DamageClassPhysical
	member.Skills[0].Power = 0
	member.Skills[0].DamageMode = battleengine.SkillDamageModeFixedAmount
	member.Skills[0].DamageAmount = 100
	return member
}

// itemForcedSwitchSelection 从事件流中读取一次性道具已确定换入成员的事件。
func itemForcedSwitchSelection(events []battleengine.Event) (battleengine.ItemForcedSwitchSelectedEvent, bool) {
	for _, event := range events {
		if selection, ok := event.(battleengine.ItemForcedSwitchSelectedEvent); ok {
			return selection, true
		}
	}
	return battleengine.ItemForcedSwitchSelectedEvent{}, false
}
