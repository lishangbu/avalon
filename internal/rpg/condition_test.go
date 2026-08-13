package rpg

import (
	"encoding/json"
	"testing"
)

func TestCompileConditionAndEffect(t *testing.T) {
	condition, err := CompileCondition(json.RawMessage(`{"op":"all","children":[{"op":"level_gte","value":5},{"op":"world_state","key":"gate","value":true}]}`))
	if err != nil {
		t.Fatal(err)
	}
	ctx := ConditionContext{Level: 5, WorldStateSwitch: map[string]bool{"gate": true}}
	if !condition.Evaluate(ctx) {
		t.Fatal("expected condition to pass")
	}
	effect, err := CompileEffect(json.RawMessage(`{"op":"set_world_state","key":"open","value":true}`))
	if err != nil {
		t.Fatal(err)
	}
	effect.Apply(&ctx)
	if !ctx.WorldStateSwitch["open"] {
		t.Fatal("expected effect")
	}
}

func TestCompileConditionFailsClosedOnUnknownOperation(t *testing.T) {
	if _, err := CompileCondition(json.RawMessage(`{"op":"script","code":"x"}`)); err == nil {
		t.Fatal("expected unknown operation error")
	}
}
