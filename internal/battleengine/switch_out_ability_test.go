package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnSwitchOutAbilitiesApplyBeforeMemberLeaves 验证主动换人会先按固定顺序结算离场形态、主要异常
// 净化和固定比例回复，再记录成员替换；这些变化必须保留在后备成员的权威快照中。
func TestResolveTurnSwitchOutAbilitiesApplyBeforeMemberLeaves(t *testing.T) {
	t.Parallel()

	front := switchOutAbilityMember(1, "switch-out-base", 1_000, 500)
	reserve := passiveMember(2, "switch-out-reserve", 1_000, 1_000)
	opponent := passiveMember(1, "switch-out-opponent", 1_000, 1_000)
	state, err := battleengine.NewState(formStateWithReserve("switch-out-active", front, reserve, opponent))
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, formSwitchTurn(1, 2), mustRandom(t, 81))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	assertSwitchOutAbilityEventsBeforeSwitch(t, result.Events, battleengine.SideOne, 1)
	previous := memberSnapshotByPosition(t, result.State, battleengine.SideOne, 1)
	if previous.CreatureID != testID("switch-out-base-alternate") || previous.MaxHP != 800 || previous.CurrentHP != 700 || previous.MajorStatus != "" {
		t.Fatalf("成功离场后的后备成员状态 = %+v", previous)
	}
}

// TestResolveTurnSwitchOutAbilitiesAlsoApplyToForcedSwitches 验证技能强制换人和受伤道具强制换人同样复用成功离场
// 生命周期；倒下补位不应伪造任何离场特性事件。
func TestResolveTurnSwitchOutAbilitiesAlsoApplyToForcedSwitches(t *testing.T) {
	t.Parallel()

	for _, test := range []struct {
		name     string
		attacker battleengine.MemberSnapshot
		target   battleengine.MemberSnapshot
		want     bool
	}{
		{
			name:     "技能强制换人",
			attacker: forceTargetSwitchUser(1, "switch-out-force-target-attacker"),
			target:   switchOutAbilityMember(1, "switch-out-force-target", 1_000, 500),
			want:     true,
		},
		{
			name: "受伤道具强制换人",
			attacker: func() battleengine.MemberSnapshot {
				member := fixedDamageUser(1, "switch-out-item-attacker")
				member.Stats.Speed = 200
				return member
			}(),
			target: func() battleengine.MemberSnapshot {
				member := switchOutAbilityMember(1, "switch-out-item", 1_000, 600)
				member.ItemID = testID("switch-out-eject-button")
				member.DamagedForceSelfSwitch = true
				return member
			}(),
			want: true,
		},
		{
			name:     "倒下补位",
			attacker: fixedDamageUser(1, "switch-out-faint-attacker"),
			target:   switchOutAbilityMember(1, "switch-out-faint", 100, 100),
			want:     false,
		},
	} {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			test.attacker.Stats.Speed = max(test.attacker.Stats.Speed, uint32(200))
			test.target.Stats.Speed = 50
			reserve := passiveMember(2, "switch-out-reserve-"+test.name, 1_000, 1_000)
			result := resolveForceTargetSwitchTurn(t, forceTargetSwitchState(
				t, test.attacker, []battleengine.MemberSnapshot{test.target, reserve}, battleengine.SideConditionSnapshot{},
			), 82)
			foundForm, foundCure, foundHeal := switchOutAbilityEventKinds(result.Events, battleengine.SideTwo, 1)
			if (foundForm && foundCure && foundHeal) != test.want {
				t.Fatalf("离场特性事件与预期不符: form=%t cure=%t heal=%t events=%v", foundForm, foundCure, foundHeal, eventKinds(result.Events))
			}
		})
	}
}

// switchOutAbilityMember 创建同时具有三种成功离场特性的成员，供主动、技能与道具换人路径复用。
func switchOutAbilityMember(position battleengine.MemberPosition, creatureID string, maxHP, currentHP uint32) battleengine.MemberSnapshot {
	member := passiveMember(position, creatureID, maxHP, currentHP)
	member.MajorStatus = battleengine.MajorStatusBurn
	member.SwitchOutMajorStatusCure = true
	member.SwitchOutHealDenominator = 4
	member.FormProfiles = []battleengine.FormProfile{
		formProfile(member),
		{CreatureID: testID(creatureID + "-alternate"), MaxHP: 800, Stats: member.Stats, Weight: 1, ElementIDs: append([]Identifier(nil), member.ElementIDs...)},
	}
	member.SwitchOutFormChange = &battleengine.SwitchOutFormChange{
		BaseCreatureID: testID(creatureID), AlternateCreatureID: testID(creatureID + "-alternate"),
	}
	return member
}

// assertSwitchOutAbilityEventsBeforeSwitch 校验成功离场特性事件严格位于实际成员替换事件之前。
func assertSwitchOutAbilityEventsBeforeSwitch(t *testing.T, events []battleengine.Event, side battleengine.Side, position battleengine.MemberPosition) {
	t.Helper()
	formIndex, cureIndex, healIndex, switchIndex := -1, -1, -1, -1
	for index, event := range events {
		switch value := event.(type) {
		case battleengine.FormChangedEvent:
			if value.Member == (battleengine.MemberRef{Side: side, Position: position}) && value.Reason == battleengine.FormChangeReasonSwitchOutAbility {
				formIndex = index
			}
		case battleengine.MajorStatusClearedEvent:
			if value.Target == (battleengine.MemberRef{Side: side, Position: position}) {
				cureIndex = index
			}
		case battleengine.SwitchOutHealingAppliedEvent:
			if value.Member == (battleengine.MemberRef{Side: side, Position: position}) {
				healIndex = index
			}
		case battleengine.ParticipantSwitchedEvent:
			if value.PreviousMember == (battleengine.MemberRef{Side: side, Position: position}) {
				switchIndex = index
			}
		}
	}
	if formIndex < 0 || cureIndex < 0 || healIndex < 0 || switchIndex < 0 ||
		formIndex >= cureIndex || cureIndex >= healIndex || healIndex >= switchIndex {
		t.Fatalf("成功离场事件顺序错误: form=%d cure=%d heal=%d switch=%d events=%v", formIndex, cureIndex, healIndex, switchIndex, eventKinds(events))
	}
}

// switchOutAbilityEventKinds 报告指定成员的三种成功离场特性事件是否都已产生。
func switchOutAbilityEventKinds(events []battleengine.Event, side battleengine.Side, position battleengine.MemberPosition) (bool, bool, bool) {
	form, cure, heal := false, false, false
	for _, event := range events {
		switch value := event.(type) {
		case battleengine.FormChangedEvent:
			form = form || value.Member == (battleengine.MemberRef{Side: side, Position: position}) && value.Reason == battleengine.FormChangeReasonSwitchOutAbility
		case battleengine.MajorStatusClearedEvent:
			cure = cure || value.Target == (battleengine.MemberRef{Side: side, Position: position})
		case battleengine.SwitchOutHealingAppliedEvent:
			heal = heal || value.Member == (battleengine.MemberRef{Side: side, Position: position})
		}
	}
	return form, cure, heal
}

// memberSnapshotByPosition 从状态快照中读取当前可能已退至后备席的指定成员。
func memberSnapshotByPosition(t *testing.T, state battleengine.State, side battleengine.Side, position battleengine.MemberPosition) battleengine.MemberSnapshot {
	t.Helper()
	for _, snapshot := range state.Snapshot().Sides {
		if snapshot.Side != side {
			continue
		}
		for _, member := range snapshot.Members {
			if member.Position == position {
				return member
			}
		}
	}
	t.Fatalf("��不到成员 %d/%d", side, position)
	return battleengine.MemberSnapshot{}
}
