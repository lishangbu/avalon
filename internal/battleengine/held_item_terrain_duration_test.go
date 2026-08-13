package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnHeldItemExtendsAllTerrainDuration 验证场地延长道具对四种普通场地共用的一条规则。
// 该规则是一个明确的“全部普通场地”资料事实，不会影响天气、强天气或已建立的场地，也不会缩短技能来源值。
func TestResolveTurnHeldItemExtendsAllTerrainDuration(t *testing.T) {
	t.Parallel()
	caster := newMember(1, "terrain-duration-skill-user", 500, 500)
	caster.Stats.Speed = 200
	caster.ItemID = testID("terrain-duration-item")
	caster.HeldItemTerrainTurnsRemaining = 8
	caster.Skills[0] = terrainSkill(1, battleengine.TerrainKindElectric, 5)
	target := newMember(1, "terrain-duration-skill-target", 500, 500)
	target.Stats.Speed = 10

	result, err := battleengine.ResolveTurn(newTerrainState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, caster, target), volatileTurn(1, 1, 1), mustRandom(t, 537))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	terrain := result.State.Snapshot().Environment.Terrain
	if terrain == nil || *terrain != (battleengine.TerrainEffect{Kind: battleengine.TerrainKindElectric, TurnsRemaining: 7}) || !terrainStartedWithDuration(result.Events, battleengine.TerrainKindElectric, 8) {
		t.Fatalf("电气场地延长结果 = terrain:%+v events:%+v", terrain, result.Events)
	}

	caster.Skills[0] = terrainSkill(1, battleengine.TerrainKindGrassy, 9)
	longer, err := battleengine.ResolveTurn(newTerrainState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, caster, target), volatileTurn(1, 1, 1), mustRandom(t, 538))
	if err != nil {
		t.Fatalf("更长场地 ResolveTurn() error = %v", err)
	}
	terrain = longer.State.Snapshot().Environment.Terrain
	if terrain == nil || *terrain != (battleengine.TerrainEffect{Kind: battleengine.TerrainKindGrassy, TurnsRemaining: 8}) || !terrainStartedWithDuration(longer.Events, battleengine.TerrainKindGrassy, 9) {
		t.Fatalf("更长草场结果 = terrain:%+v events:%+v", terrain, longer.Events)
	}
}

// terrainStartedWithDuration 报告事件流是否含有指定普通场地的完整建立持续回合。
func terrainStartedWithDuration(events []battleengine.Event, terrain battleengine.TerrainKind, turns uint8) bool {
	for _, event := range events {
		value, ok := event.(battleengine.TerrainStartedEvent)
		if ok && value.Terrain == terrain && value.TurnsRemaining == turns {
			return true
		}
	}
	return false
}
