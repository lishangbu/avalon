package api

import (
	"testing"

	rpgv1 "github.com/lishangbu/avalon/api/gen/go/avalon/rpg/v1"
)

func TestConditionJSONFromMessageBuildsNestedClosedRule(t *testing.T) {
	t.Parallel()
	message := &rpgv1.ExitCondition{Node: &rpgv1.ExitCondition_All{All: &rpgv1.ConditionAll{Children: []*rpgv1.ExitCondition{
		{Node: &rpgv1.ExitCondition_LevelAtLeast{LevelAtLeast: &rpgv1.LevelAtLeast{Value: 5}}},
		{Node: &rpgv1.ExitCondition_WorldStateEquals{WorldStateEquals: &rpgv1.WorldStateEquals{Key: "gate-open", Value: true}}},
	}}}}
	raw, err := conditionJSONFromMessage(message)
	if err != nil {
		t.Fatalf("转换嵌套出口条件: %v", err)
	}
	want := `{"op":"all","children":[{"op":"level_gte","value":5},{"op":"world_state","key":"gate-open","value":true}]}`
	if string(raw) != want {
		t.Fatalf("规范条件 = %s, want %s", raw, want)
	}
}

func TestConditionJSONFromMessageRejectsEmptyNode(t *testing.T) {
	t.Parallel()
	if _, err := conditionJSONFromMessage(&rpgv1.ExitCondition{}); err == nil {
		t.Fatal("空出口条件节点应被拒绝")
	}
}

func TestEffectJSONFromMessageAllowsNoEffect(t *testing.T) {
	t.Parallel()
	raw, err := effectJSONFromMessage(nil)
	if err != nil {
		t.Fatalf("转换无副作用: %v", err)
	}
	if raw != nil {
		t.Fatalf("无副作用 = %s, want nil", raw)
	}
}
