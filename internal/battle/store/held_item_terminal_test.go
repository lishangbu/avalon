package store

import (
	"encoding/json"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// TestHeldItemConsumptionsFromEvents 验证终局只接受引擎显式消费事件，绝不把道具转移当成消费。
func TestHeldItemConsumptionsFromEvents(t *testing.T) {
	t.Parallel()

	itemID := battleengine.Identifier(snowflake.MustParse("1048576001"))
	skillID := battleengine.Identifier(snowflake.MustParse("1048576002"))
	elementID := battleengine.Identifier(snowflake.MustParse("1048576003"))
	events := []any{
		battleengine.HeldItemElementDamageBoostConsumedEvent{Type: battleengine.EventKindHeldItemElementDamageBoostConsumed, SchemaVersion: 1, Actor: battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}, SkillID: skillID, ItemID: itemID, ElementID: elementID},
		battleengine.HeldItemTransferredEvent{Type: battleengine.EventKindHeldItemTransferred, SchemaVersion: 1, From: battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}, To: battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}, ItemID: itemID, SkillID: skillID},
		battleengine.HeldItemTriggeredConsumedEvent{Type: battleengine.EventKindHeldItemTriggeredConsumed, SchemaVersion: 1, Holder: battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}, ItemID: itemID, SkillID: skillID, Trigger: battleengine.HeldItemTriggerAccuracyMiss},
	}
	payload := make([]json.RawMessage, 0, len(events))
	for _, event := range events {
		encoded, err := json.Marshal(event)
		if err != nil {
			t.Fatalf("json.Marshal() error = %v", err)
		}
		payload = append(payload, encoded)
	}

	consumptions, err := heldItemConsumptionsFromEvents(payload)
	if err != nil {
		t.Fatalf("heldItemConsumptionsFromEvents() error = %v", err)
	}
	if len(consumptions) != 2 || consumptions[0].Side != battleengine.SideOne || consumptions[0].Position != 1 || consumptions[0].ItemID != snowflake.ID(itemID) || consumptions[1].Side != battleengine.SideTwo || consumptions[1].Position != 1 {
		t.Fatalf("heldItemConsumptionsFromEvents() = %+v", consumptions)
	}
}

// TestHeldItemConsumptionsFromEventsRejectsMalformedConsumption 验证损坏的权威消费事件不会被静默忽略。
func TestHeldItemConsumptionsFromEventsRejectsMalformedConsumption(t *testing.T) {
	t.Parallel()

	_, err := heldItemConsumptionsFromEvents([]json.RawMessage{json.RawMessage(`{"kind":"heldItemTriggeredConsumed","schemaVersion":1,"holder":{"side":1,"memberPosition":0},"itemId":1048576001}`)})
	if err == nil {
		t.Fatal("heldItemConsumptionsFromEvents() error = nil")
	}
}
