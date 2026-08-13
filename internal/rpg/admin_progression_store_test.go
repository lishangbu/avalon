package rpg

import (
	"context"
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

func validProgressionWrite() AdminWriteContext {
	return AdminWriteContext{ActorAccountID: snowflake.ID(1), IdempotencyKey: "progression-test-key", RequestID: "progression-test-request"}
}

func TestSaveRecipeRejectsInvalidAggregateBeforePersistence(t *testing.T) {
	level := int32(2)
	tests := []struct {
		name  string
		value AdminRecipe
	}{
		{name: "职业条件必须成对", value: AdminRecipe{Code: "potion", Name: "药水", RequiredProfessionCode: "alchemy", Enabled: true, Outputs: []AdminRecipeItem{{ItemID: 2, Quantity: 1}}}},
		{name: "必须声明产物", value: AdminRecipe{Code: "potion", Name: "药水", Enabled: true}},
		{name: "同侧道具不可重复", value: AdminRecipe{Code: "potion", Name: "药水", RequiredProfessionCode: "alchemy", RequiredProfessionLevel: &level, Enabled: true, Outputs: []AdminRecipeItem{{ItemID: 2, Quantity: 1}, {ItemID: 2, Quantity: 2}}}},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			_, err := (&EntWorldStore{}).SaveRecipe(context.Background(), SaveRecipeCommand{Write: validProgressionWrite(), Value: test.value})
			if !errors.Is(err, ErrInvalidAdminWorld) {
				t.Fatalf("SaveRecipe() error = %v, want %v", err, ErrInvalidAdminWorld)
			}
		})
	}
}

func TestSaveProfessionRejectsInvalidSkillsBeforePersistence(t *testing.T) {
	tests := []struct {
		name  string
		value AdminProfession
	}{
		{name: "等级上限必须为正", value: AdminProfession{Code: "alchemy", Name: "炼金", MaximumLevel: 0, Enabled: true}},
		{name: "技能等级不可超过上限", value: AdminProfession{Code: "alchemy", Name: "炼金", MaximumLevel: 10, Enabled: true, Skills: []AdminProfessionSkill{{Code: "mastery", Name: "精通", RequiredLevel: 11, Enabled: true}}}},
		{name: "技能编码不可重复", value: AdminProfession{Code: "alchemy", Name: "炼金", MaximumLevel: 10, Enabled: true, Skills: []AdminProfessionSkill{{Code: "mastery", Name: "精通一", RequiredLevel: 1}, {Code: "mastery", Name: "精通二", RequiredLevel: 2}}}},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			_, err := (&EntWorldStore{}).SaveProfession(context.Background(), SaveProfessionCommand{Write: validProgressionWrite(), Value: test.value})
			if !errors.Is(err, ErrInvalidAdminWorld) {
				t.Fatalf("SaveProfession() error = %v, want %v", err, ErrInvalidAdminWorld)
			}
		})
	}
}
