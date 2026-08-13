package rpg

import (
	"strings"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// TestValidateEquipmentLoadoutAcceptsSlotSwaps 验证整套最终状态允许主副手和饰品交换，不受旧状态更新顺序影响。
func TestValidateEquipmentLoadoutAcceptsSlotSwaps(t *testing.T) {
	entries := []EquipmentLoadoutCandidate{
		{Slot: EquipmentSlotMainHand, InstanceID: snowflake.ID(11), SlotType: EquipmentSlotTypeMainHand, Handedness: EquipmentHandednessOneHanded},
		{Slot: EquipmentSlotOffHand, InstanceID: snowflake.ID(12), SlotType: EquipmentSlotTypeOffHand, Handedness: EquipmentHandednessOffHand},
		{Slot: EquipmentSlotAccessory1, InstanceID: snowflake.ID(13), SlotType: EquipmentSlotTypeAccessory},
		{Slot: EquipmentSlotAccessory2, InstanceID: snowflake.ID(14), SlotType: EquipmentSlotTypeAccessory},
	}
	if err := ValidateEquipmentLoadout(20, nil, entries); err != nil {
		t.Fatalf("ValidateEquipmentLoadout() error = %v", err)
	}
}

// TestEquipmentDiagnosticFiltersUseClosedSets 验证管理诊断只接受数据库约束声明的来源与动作。
func TestEquipmentDiagnosticFiltersUseClosedSets(t *testing.T) {
	for _, value := range []string{"shop", "quest", "loot", "admin"} {
		if !validEquipmentSourceType(value) {
			t.Fatalf("validEquipmentSourceType(%q) = false", value)
		}
	}
	for _, value := range []string{"acquire", "equip", "unequip", "sell"} {
		if !validEquipmentTransactionAction(value) {
			t.Fatalf("validEquipmentTransactionAction(%q) = false", value)
		}
	}
	if validEquipmentSourceType("legacy") || validEquipmentTransactionAction("delete") {
		t.Fatal("装备诊断筛选接受了闭集之外的兼容值")
	}
}

// TestGrantEquipmentReasonLengthUsesCharacters 验证管理授予原因按产品字符上限校验中文文本。
func TestGrantEquipmentReasonLengthUsesCharacters(t *testing.T) {
	base := GrantEquipmentCommand{Write: AdminWriteContext{ActorAccountID: 1, IdempotencyKey: "grant", RequestID: "request"}, PlayerCharacterID: 2, EquipmentID: 3, Quantity: 1}
	base.Reason = strings.Repeat("装", 500)
	if !validGrantEquipmentCommand(base) {
		t.Fatal("validGrantEquipmentCommand(500 个中文字符) = false")
	}
	base.Reason = strings.Repeat("装", 501)
	if validGrantEquipmentCommand(base) {
		t.Fatal("validGrantEquipmentCommand(501 个中文字符) = true")
	}
}

// TestValidateEquipmentLoadoutRejectsTwoHandedConflict 验证双手武器只保存主手条目且要求副手为空。
func TestValidateEquipmentLoadoutRejectsTwoHandedConflict(t *testing.T) {
	entries := []EquipmentLoadoutCandidate{
		{Slot: EquipmentSlotMainHand, InstanceID: snowflake.ID(11), SlotType: EquipmentSlotTypeMainHand, Handedness: EquipmentHandednessTwoHanded},
		{Slot: EquipmentSlotOffHand, InstanceID: snowflake.ID(12), SlotType: EquipmentSlotTypeOffHand, Handedness: EquipmentHandednessOffHand},
	}
	if err := ValidateEquipmentLoadout(20, nil, entries); err != ErrEquipmentTwoHandedConflict {
		t.Fatalf("error = %v, want %v", err, ErrEquipmentTwoHandedConflict)
	}
}

// TestValidateEquipmentLoadoutChecksRequirements 验证最低等级与非空职业白名单共同约束穿戴资格。
func TestValidateEquipmentLoadoutChecksRequirements(t *testing.T) {
	professionID := snowflake.ID(90)
	entry := EquipmentLoadoutCandidate{Slot: EquipmentSlotHead, InstanceID: snowflake.ID(11), SlotType: EquipmentSlotTypeHead, MinimumLevel: 30, ProfessionIDs: []snowflake.ID{professionID}}
	if err := ValidateEquipmentLoadout(20, []snowflake.ID{professionID}, []EquipmentLoadoutCandidate{entry}); err != ErrEquipmentRequirementNotMet {
		t.Fatalf("level error = %v", err)
	}
	entry.MinimumLevel = 10
	if err := ValidateEquipmentLoadout(20, []snowflake.ID{snowflake.ID(91)}, []EquipmentLoadoutCandidate{entry}); err != ErrEquipmentRequirementNotMet {
		t.Fatalf("profession error = %v", err)
	}
}

// TestApplyEquipmentStatModifiersIsOrderIndependent 验证所有平加先汇总、百分比再汇总，且装备遍历顺序不改变结果。
func TestApplyEquipmentStatModifiersIsOrderIndependent(t *testing.T) {
	modifiers := []EquipmentStatModifier{{FlatValue: 20, PercentageBPS: 1000}, {FlatValue: -5, PercentageBPS: -500}}
	got, err := ApplyEquipmentStatModifiers(100, 0, modifiers)
	if err != nil || got != 120 {
		t.Fatalf("ApplyEquipmentStatModifiers() = %d, %v; want 120", got, err)
	}
	reversed := []EquipmentStatModifier{modifiers[1], modifiers[0]}
	gotReversed, err := ApplyEquipmentStatModifiers(100, 0, reversed)
	if err != nil || gotReversed != got {
		t.Fatalf("reversed = %d, %v; want %d", gotReversed, err, got)
	}
}

// TestFreezeEquipmentBattleSnapshot 验证人物 Battle 装备输入按槽位稳定排序且不共享调用方切片。
func TestFreezeEquipmentBattleSnapshot(t *testing.T) {
	rules := []byte(`{}`)
	modifiers := []EquipmentStatModifier{{StatID: snowflake.ID(21), FlatValue: 5}}
	entries := []EquipmentBattleSnapshotEntry{
		{InstanceID: 12, EquipmentID: 22, ItemID: 32, Slot: EquipmentSlotHead, CatalogVersion: 2, StatModifiers: modifiers, CompiledRules: rules},
		{InstanceID: 11, EquipmentID: 21, ItemID: 31, Slot: EquipmentSlotMainHand, CatalogVersion: 1, CompiledRules: rules},
	}

	snapshot, err := FreezeEquipmentBattleSnapshot(7, entries)
	if err != nil {
		t.Fatalf("FreezeEquipmentBattleSnapshot() error = %v", err)
	}
	rules[0] = '['
	modifiers[0].FlatValue = 99
	if snapshot.LoadoutVersion != 7 || snapshot.Entries[0].Slot != EquipmentSlotHead || snapshot.Entries[1].Slot != EquipmentSlotMainHand || string(snapshot.Entries[0].CompiledRules) != "{}" || snapshot.Entries[0].StatModifiers[0].FlatValue != 5 {
		t.Fatalf("FreezeEquipmentBattleSnapshot() = %+v", snapshot)
	}
}

// TestValidAdminEquipmentRelationsRejectsDuplicatesAndInvalidTotals 验证管理关系在写数据库前完成去重和修正范围校验。
func TestValidAdminEquipmentRelationsRejectsDuplicatesAndInvalidTotals(t *testing.T) {
	professionID, statID := snowflake.ID(31), snowflake.ID(41)
	valid := AdminEquipment{ProfessionIDs: []snowflake.ID{professionID}, StatModifiers: []AdminEquipmentStatModifier{{StatID: statID, PercentageBPS: 1000}}}
	if !validAdminEquipmentRelations(valid) {
		t.Fatal("validAdminEquipmentRelations(valid) = false")
	}
	duplicateProfession := valid
	duplicateProfession.ProfessionIDs = []snowflake.ID{professionID, professionID}
	if validAdminEquipmentRelations(duplicateProfession) {
		t.Fatal("validAdminEquipmentRelations(duplicate profession) = true")
	}
	duplicateStat := valid
	duplicateStat.StatModifiers = []AdminEquipmentStatModifier{{StatID: statID, PercentageBPS: 500}, {StatID: statID, PercentageBPS: 500}}
	if validAdminEquipmentRelations(duplicateStat) {
		t.Fatal("validAdminEquipmentRelations(duplicate stat) = true")
	}
	invalidTotal := valid
	invalidTotal.StatModifiers = []AdminEquipmentStatModifier{{StatID: statID, PercentageBPS: 60000}, {StatID: snowflake.ID(42), PercentageBPS: 60000}}
	if validAdminEquipmentRelations(invalidTotal) {
		t.Fatal("validAdminEquipmentRelations(invalid total) = true")
	}
}
