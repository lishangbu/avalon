package persistence

import (
	"context"
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
	rpg "github.com/lishangbu/avalon/internal/rpg"
)

func validProgressionWrite() rpg.AdminWriteContext {
	return rpg.AdminWriteContext{ActorAccountID: snowflake.ID(1), IdempotencyKey: "progression-test-key", RequestID: "progression-test-request"}
}

func TestSaveRecipeRejectsInvalidAggregateBeforePersistence(t *testing.T) {
	level := int32(2)
	tests := []struct {
		name  string
		value rpg.AdminRecipe
	}{
		{name: "职业条件必须成对", value: rpg.AdminRecipe{Code: "potion", Name: "药水", RequiredProfessionCode: "alchemy", Enabled: true, Outputs: []rpg.AdminRecipeItem{{ItemID: 2, Quantity: 1}}}},
		{name: "必须声明产物", value: rpg.AdminRecipe{Code: "potion", Name: "药水", Enabled: true}},
		{name: "同侧道具不可重复", value: rpg.AdminRecipe{Code: "potion", Name: "药水", RequiredProfessionCode: "alchemy", RequiredProfessionLevel: &level, Enabled: true, Outputs: []rpg.AdminRecipeItem{{ItemID: 2, Quantity: 1}, {ItemID: 2, Quantity: 2}}}},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			_, err := (&Adapters{}).SaveRecipe(context.Background(), rpg.SaveRecipeCommand{Write: validProgressionWrite(), Value: test.value})
			if !errors.Is(err, rpg.ErrInvalidAdminWorld) {
				t.Fatalf("SaveRecipe() error = %v, want %v", err, rpg.ErrInvalidAdminWorld)
			}
		})
	}
}

func TestSaveProfessionRejectsInvalidSkillsBeforePersistence(t *testing.T) {
	tests := []struct {
		name  string
		value rpg.AdminProfession
	}{
		{name: "等级上限必须为正", value: rpg.AdminProfession{Code: "alchemy", Name: "炼金", MaximumLevel: 0, Enabled: true}},
		{name: "技能等级不可超过上限", value: rpg.AdminProfession{Code: "alchemy", Name: "炼金", MaximumLevel: 10, Enabled: true, Skills: []rpg.AdminProfessionSkill{{Code: "mastery", Name: "精通", RequiredLevel: 11, Enabled: true}}}},
		{name: "技能编码不可重复", value: rpg.AdminProfession{Code: "alchemy", Name: "炼金", MaximumLevel: 10, Enabled: true, Skills: []rpg.AdminProfessionSkill{{Code: "mastery", Name: "精通一", RequiredLevel: 1}, {Code: "mastery", Name: "精通二", RequiredLevel: 2}}}},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			_, err := (&Adapters{}).SaveProfession(context.Background(), rpg.SaveProfessionCommand{Write: validProgressionWrite(), Value: test.value})
			if !errors.Is(err, rpg.ErrInvalidAdminWorld) {
				t.Fatalf("SaveProfession() error = %v, want %v", err, rpg.ErrInvalidAdminWorld)
			}
		})
	}
}
