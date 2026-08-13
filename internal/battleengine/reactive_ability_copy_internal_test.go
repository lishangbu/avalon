package battleengine

import "testing"

// TestCopyAbilityRulesDeepCopiesReactiveAbilityRules 验证特性复制获得来源当前规则且完全隔离嵌套切片。
func TestCopyAbilityRulesDeepCopiesReactiveAbilityRules(t *testing.T) {
	t.Parallel()
	source := MemberSnapshot{AbilityID: testID("source"), ReactiveAbilityRules: &ReactiveAbilityRules{
		EndTurnStatStageChanges: []StatStageDelta{{Stat: StatSpeed, Delta: 1}},
		ReceivedDamageStatStageChanges: []ReceivedDamageStatStageChange{{
			Changes: []StatStageDelta{{Stat: StatDefense, Delta: 1}}, ElementIDs: testIDs("electric"),
		}},
	}}
	copied := copyAbilityRules(MemberSnapshot{AbilityID: testID("receiver")}, source)
	source.ReactiveAbilityRules.EndTurnStatStageChanges[0].Delta = 6
	source.ReactiveAbilityRules.ReceivedDamageStatStageChanges[0].Changes[0].Delta = 6
	source.ReactiveAbilityRules.ReceivedDamageStatStageChanges[0].ElementIDs[0] = testID("fire")
	if copied.ReactiveAbilityRules == nil || copied.ReactiveAbilityRules.EndTurnStatStageChanges[0].Delta != 1 ||
		copied.ReactiveAbilityRules.ReceivedDamageStatStageChanges[0].Changes[0].Delta != 1 ||
		copied.ReactiveAbilityRules.ReceivedDamageStatStageChanges[0].ElementIDs[0] != testID("electric") {
		t.Fatalf("复制后的反应型特性规则被来源污染: %+v", copied.ReactiveAbilityRules)
	}
}

// TestTransformSnapshotRestoresReactiveAbilityRules 验证变身快照冻结原规则，离场恢复时不会保留目标规则或共享指针。
func TestTransformSnapshotRestoresReactiveAbilityRules(t *testing.T) {
	t.Parallel()
	original := MemberSnapshot{AbilityID: testID("original"), ReactiveAbilityRules: &ReactiveAbilityRules{
		EndTurnStatStageChanges: []StatStageDelta{{Stat: StatAttack, Delta: 1}},
	}}
	snapshot := newMemberTransformSnapshot(original)
	original.ReactiveAbilityRules.EndTurnStatStageChanges[0].Delta = 6
	transformed := MemberSnapshot{AbilityID: testID("target"), TransformSnapshot: snapshot, ReactiveAbilityRules: &ReactiveAbilityRules{
		EndTurnStatStageChanges: []StatStageDelta{{Stat: StatSpeed, Delta: 2}},
	}}
	restored := restoreTransformSnapshot(transformed)
	snapshot.ReactiveAbilityRules.EndTurnStatStageChanges[0].Delta = 5
	if restored.AbilityID != testID("original") || restored.ReactiveAbilityRules == nil ||
		restored.ReactiveAbilityRules.EndTurnStatStageChanges[0].Stat != StatAttack ||
		restored.ReactiveAbilityRules.EndTurnStatStageChanges[0].Delta != 1 {
		t.Fatalf("离场恢复的反应型特性规则 = %+v", restored.ReactiveAbilityRules)
	}
}
