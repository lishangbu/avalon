package rpg

import (
	"encoding/json"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// TestAdminEncounterTableJSONRoundTrip 验证未选择形态和掉落表的遭遇候选可稳定进入幂等响应。
func TestAdminEncounterTableJSONRoundTrip(t *testing.T) {
	t.Parallel()
	want := AdminEncounterTable{ID: snowflake.NewTestID(), LocationID: snowflake.NewTestID(), Code: "plain-encounter", Name: "普通遭遇", TriggerProbabilityBPS: 1000, Enabled: true, Version: 1, Entries: []AdminEncounterEntry{{ID: snowflake.NewTestID(), CreatureID: snowflake.NewTestID(), MinimumLevel: 2, MaximumLevel: 4, Weight: 1, Enabled: true}}}
	raw, err := json.Marshal(want)
	if err != nil {
		t.Fatalf("Marshal(AdminEncounterTable) error = %v", err)
	}
	var got AdminEncounterTable
	if err = json.Unmarshal(raw, &got); err != nil {
		t.Fatalf("Unmarshal(AdminEncounterTable) error = %v", err)
	}
	if got.ID != want.ID || got.LocationID != want.LocationID || len(got.Entries) != 1 || got.Entries[0].ID != want.Entries[0].ID || got.Entries[0].FormID.IsValid() || got.Entries[0].LootTableID.IsValid() {
		t.Fatalf("AdminEncounterTable round trip = %+v", got)
	}
}

// TestAdminQuestJSONRoundTrip 验证未设置 NPC、前置任务和目标引用的任务可稳定进入幂等响应。
func TestAdminQuestJSONRoundTrip(t *testing.T) {
	t.Parallel()
	want := AdminQuest{ID: snowflake.NewTestID(), Code: "plain-quest", Name: "普通任务", QuestType: "side", Description: "无可选引用", Enabled: true, Version: 1, Objectives: []AdminQuestObjective{{ID: snowflake.NewTestID(), Code: "plain-objective", Position: 1, ObjectiveType: "battle", RequiredCount: 1, Description: "完成战斗"}}}
	raw, err := json.Marshal(want)
	if err != nil {
		t.Fatalf("Marshal(AdminQuest) error = %v", err)
	}
	var got AdminQuest
	if err = json.Unmarshal(raw, &got); err != nil {
		t.Fatalf("Unmarshal(AdminQuest) error = %v", err)
	}
	if got.ID != want.ID || got.StartNPCID.IsValid() || got.PrerequisiteQuestID.IsValid() || len(got.Objectives) != 1 || got.Objectives[0].ID != want.Objectives[0].ID || got.Objectives[0].TargetItemID.IsValid() {
		t.Fatalf("AdminQuest round trip = %+v", got)
	}
}
