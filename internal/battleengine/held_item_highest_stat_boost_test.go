package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemHighestStatBoostConsumesMatchingItem 验证初始入场会在特性匹配时消耗道具，并把强化结果写入权威状态、事件与摘要。
func TestHeldItemHighestStatBoostConsumesMatchingItem(t *testing.T) {
	t.Parallel()
	member := newMember(1, "booster-initial", 500, 500)
	member.AbilityID = testID("booster-ability")
	member.ItemID = testID("booster-energy")
	member.HighestStatBoosterAbilityIDs = testIDs("booster-ability")
	member.Stats.Attack = 180
	member.Stats.Defense = 100
	member.Stats.SpecialAttack = 100
	member.Stats.SpecialDefense = 100
	member.Stats.Speed = 100
	opponent := newMember(1, "booster-initial-opponent", 500, 500)
	state := newFieldSpeedOrderState(t, battleengine.EnvironmentSnapshot{}, member, opponent)

	active, found := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || active.ItemID != 0 || active.BoosterEnergyStat != battleengine.StatAttack {
		t.Fatalf("消耗后的成员状态 = %+v，期望道具清空且持续强化攻击", active)
	}
	if !hasHeldItemHighestStatBoostActivated(state.InitialEvents(), battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}, testID("booster-energy"), testID("booster-ability"), battleengine.StatAttack) {
		t.Fatalf("初始事件未记录最高能力强化道具消耗: %+v", state.InitialEvents())
	}
	summary := state.Summary()
	if len(summary.Members) == 0 || summary.Members[0].ItemID != 0 || summary.Members[0].BoosterEnergyStat != battleengine.StatAttack {
		t.Fatalf("消耗后的状态摘要 = %+v", summary)
	}
}

// TestHeldItemHighestStatBoostRespectsEnvironmentAndAbility 验证道具只在匹配当前特性且环境强化未生效时消耗。
//
// 环境失效后不会补消耗：消耗判定是入场一次性效果，不是每回合轮询触发器。
func TestHeldItemHighestStatBoostRespectsEnvironmentAndAbility(t *testing.T) {
	t.Parallel()
	for _, test := range []struct {
		name             string
		abilityID        Identifier
		environment      battleengine.EnvironmentSnapshot
		rule             *battleengine.EnvironmentHighestStatMultiplier
		wantItemRetained bool
	}{
		{
			name: "匹配环境保留道具", abilityID: testID("booster-ability"),
			environment: battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{
				Kind: battleengine.WeatherKindSun, TurnsRemaining: 2,
			}},
			rule:             &battleengine.EnvironmentHighestStatMultiplier{RequiredWeather: battleengine.WeatherKindSun},
			wantItemRetained: true,
		},
		{name: "不匹配特性保留道具", abilityID: testID("another-ability"), wantItemRetained: true},
	} {
		t.Run(test.name, func(t *testing.T) {
			member := newMember(1, "booster-guard-"+test.abilityID.String(), 500, 500)
			member.AbilityID = test.abilityID
			member.ItemID = testID("booster-energy")
			member.HighestStatBoosterAbilityIDs = testIDs("booster-ability")
			member.EnvironmentHighestStatMultiplier = test.rule
			member.Stats.Attack = 180
			opponent := newMember(1, "booster-guard-opponent", 500, 500)
			state := newFieldSpeedOrderState(t, test.environment, member, opponent)
			active, found := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
			if !found || !test.wantItemRetained || active.ItemID != testID("booster-energy") || active.BoosterEnergyStat != "" {
				t.Fatalf("受保护条件下成员状态 = %+v，期望道具未消耗", active)
			}
			if hasHeldItemHighestStatBoostActivated(state.InitialEvents(), battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}, testID("booster-energy"), test.abilityID, battleengine.StatAttack) {
				t.Fatalf("受保护条件不应产生消耗事件: %+v", state.InitialEvents())
			}
		})
	}
}

// TestHeldItemHighestStatBoostChangesSpeedAndActivatesOnSwitchIn 验证道具按最高速度选项改变行动顺序，并覆盖实际换入而非仅初始成员。
func TestHeldItemHighestStatBoostChangesSpeedAndActivatesOnSwitchIn(t *testing.T) {
	t.Parallel()
	front := newMember(1, "booster-switch-front", 500, 500)
	incoming := newMember(2, "booster-switch-incoming", 500, 500)
	incoming.AbilityID = testID("booster-ability")
	incoming.ItemID = testID("booster-energy")
	incoming.HighestStatBoosterAbilityIDs = testIDs("booster-ability")
	incoming.Stats.Attack = 70
	incoming.Stats.Defense = 80
	incoming.Stats.SpecialAttack = 90
	incoming.Stats.SpecialDefense = 95
	incoming.Stats.Speed = 100
	incoming.Skills[0] = ordinaryFieldSpeedOrderSkill(1, "强化速度攻击")
	opponent := newMember(1, "booster-switch-opponent", 500, 500)
	opponent.Stats.Speed = 125
	opponent.Skills[0] = ordinaryFieldSpeedOrderSkill(1, "观察速度攻击")

	state, err := battleengine.NewState(formStateWithReserve("booster-switch", front, incoming, opponent))
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, formSwitchTurn(1, 2), mustRandom(t, 281))
	if err != nil {
		t.Fatalf("ResolveTurn() switch error = %v", err)
	}
	active, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || active.ItemID != 0 || active.BoosterEnergyStat != battleengine.StatSpeed {
		t.Fatalf("换入后的道具强化状态 = %+v", active)
	}
	if !hasHeldItemHighestStatBoostActivated(result.Events, battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}, testID("booster-energy"), testID("booster-ability"), battleengine.StatSpeed) {
		t.Fatalf("换入未记录最高能力强化道具事件: %+v", result.Events)
	}

	second, err := battleengine.ResolveTurn(result.State, fieldSpeedOrderTurn(2,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), mustRandom(t, 283))
	if err != nil {
		t.Fatalf("ResolveTurn() boosted speed error = %v", err)
	}
	firstDamage, found := findFirstDamage(second.Events)
	if !found || firstDamage.Actor.Side != battleengine.SideOne {
		t.Fatalf("最高速度强化后的首个伤害事件 = %+v，期望左方先行动", firstDamage)
	}
}

// hasHeldItemHighestStatBoostActivated 在事件流中查找一条精确匹配的最高原始能力强化道具消耗事件。
func hasHeldItemHighestStatBoostActivated(
	events []battleengine.Event,
	member battleengine.MemberRef,
	itemID, abilityID Identifier,
	stat battleengine.Stat,
) bool {
	for _, event := range events {
		activated, ok := event.(battleengine.HeldItemHighestStatBoostActivatedEvent)
		if ok && activated.Member == member && activated.ItemID == itemID && activated.AbilityID == abilityID && activated.Stat == stat {
			return true
		}
	}
	return false
}
