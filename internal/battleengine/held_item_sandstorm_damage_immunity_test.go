package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnHeldItemSandstormDamageImmunity 验证沙暴伤害免疫道具只阻止持有者的回合末沙暴扣血。
// 它不会移除环境、不会保护其它成员，也不能在道具已失去时继续生效。
func TestResolveTurnHeldItemSandstormDamageImmunity(t *testing.T) {
	t.Parallel()
	immune := newMember(1, "item-sandstorm-immune", 160, 160)
	immune.CurrentHP = 120
	immune.ItemID = testID("sandstorm-immunity-item")
	immune.HeldItemSandstormDamageImmunity = true
	immune.Skills[0] = fieldSpeedOrderSkill(1, "免疫测试等待", 0)
	normal := newMember(1, "item-sandstorm-normal", 160, 160)
	normal.CurrentHP = 120
	normal.Skills[0] = fieldSpeedOrderSkill(1, "普通测试等待", 0)

	result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSandstorm, TurnsRemaining: 2}}, battleengine.RuleSnapshot{SchemaVersion: 1}, immune, normal), volatileTurn(1, 1, 1), mustRandom(t, 539))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	immuneAfter, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	normalAfter, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if immuneAfter.CurrentHP != 120 || normalAfter.CurrentHP != 110 {
		t.Fatalf("沙暴道具免疫生命 = immune:%d normal:%d", immuneAfter.CurrentHP, normalAfter.CurrentHP)
	}

	immune.ItemID = 0
	lostItem, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSandstorm, TurnsRemaining: 2}}, battleengine.RuleSnapshot{SchemaVersion: 1}, immune, normal), volatileTurn(1, 1, 1), mustRandom(t, 540))
	if err != nil {
		t.Fatalf("失去道具 ResolveTurn() error = %v", err)
	}
	immuneAfter, _ = lostItem.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if immuneAfter.CurrentHP != 110 {
		t.Fatalf("失去道具后仍错误免疫，生命 = %d", immuneAfter.CurrentHP)
	}
}
