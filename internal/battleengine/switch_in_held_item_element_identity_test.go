package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemElementIdentityAppliesAfterFormAndRestoresOnExit 验证道具属性身份覆盖入场形态的自然属性，
// 并在成员离场后恢复已冻结的形态属性，避免把临时单属性泄漏给后备快照。
func TestHeldItemElementIdentityAppliesAfterFormAndRestoresOnExit(t *testing.T) {
	t.Parallel()
	front := newMember(1, "item-identity-front", 100, 100)
	incoming := newMember(2, "item-identity-base", 100, 100)
	incoming.ItemID = testID("fire-memory")
	incoming.HeldItemElementID = testID("fire")
	incoming.SwitchInHeldItemElementIdentity = true
	// 形态资料和成员本体必须使用同一份有效体重，确保初始状态校验覆盖真实快照。
	incoming.Weight = 1
	incoming.FormProfiles = []battleengine.FormProfile{
		formProfile(incoming),
		{
			CreatureID: testID("item-identity-alternate"), MaxHP: 100, Stats: incoming.Stats, Weight: incoming.Weight,
			ElementIDs: testIDs("water", "ice"),
		},
	}
	incoming.SwitchInFormChange = &battleengine.SwitchInFormChange{
		BaseCreatureID: incoming.CreatureID, AlternateCreatureID: testID("item-identity-alternate"),
	}
	opponent := newMember(1, "item-identity-opponent", 100, 100)
	opponent.Skills[0] = fieldSpeedOrderSkill(1, "无伤害行动", 0)

	state, err := battleengine.NewState(formStateWithReserve("item-element-identity", front, incoming, opponent))
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, formSwitchTurn(1, 2), mustRandom(t, 611))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	active, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || active.CreatureID != testID("item-identity-alternate") || len(active.ElementIDs) != 1 || active.ElementIDs[0] != testID("fire") {
		t.Fatalf("换入后的道具属性身份 = %+v", active)
	}
	if !hasHeldItemElementIdentityApplied(result.Events, battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}, testID("fire-memory"), testID("fire")) {
		t.Fatalf("换入未记录道具属性身份事件: %+v", result.Events)
	}

	// 第二次换入原成员会触发前一个成员离场。读取权威快照而不是场上成员，确保清理的是后备成员状态。
	result, err = battleengine.ResolveTurn(result.State, formSwitchTurn(2, 1), mustRandom(t, 612))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	reserve := result.State.Snapshot().Sides[0].Members[1]
	if reserve.CreatureID != testID("item-identity-alternate") || len(reserve.ElementIDs) != 2 ||
		reserve.ElementIDs[0] != testID("water") || reserve.ElementIDs[1] != testID("ice") || len(reserve.HeldItemElementIdentityBaseElementIDs) != 0 {
		t.Fatalf("离场后的道具属性身份恢复 = %+v", reserve)
	}
}

// hasHeldItemElementIdentityApplied 在结构化事件中查找指定成员的一次道具属性身份替换。
func hasHeldItemElementIdentityApplied(events []battleengine.Event, member battleengine.MemberRef, itemID, elementID Identifier) bool {
	for _, event := range events {
		applied, ok := event.(battleengine.HeldItemElementIdentityAppliedEvent)
		if ok && applied.Member == member && applied.ItemID == itemID && applied.ElementID == elementID {
			return true
		}
	}
	return false
}
