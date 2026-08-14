package persistence

import (
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/jackc/pgx/v5/pgtype"
	"github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
)

// TestSkillDetailFromRowPreservesBattleActionFlags 验证数据库行动控制与技能分类布尔列会原样进入领域详情，
// 而不是因零值或其它 JSONB 规则的读取失败被静默丢弃；每个独立标签都必须继续传递到 Battle 冻结快照。
func TestAbilitySwitchInTerrainFromValuesRejectsIncompletePairs(t *testing.T) {
	t.Parallel()
	if absent := abilitySwitchInTerrainFromValues(pgtype.Text{}, 0); absent != nil {
		t.Fatalf("未声明入场普通场地 = %+v，期望 nil", absent)
	}
	valid := abilitySwitchInTerrainFromValues(pgtype.Text{String: "grassy", Valid: true}, 5)
	if valid == nil || valid.Terrain != abilitydetail.TerrainKindGrassy || valid.TurnsRemaining != 5 {
		t.Fatalf("有效入场普通场地 = %+v", valid)
	}
	damaged := abilitySwitchInTerrainFromValues(pgtype.Text{}, 5)
	if damaged == nil || damaged.Terrain != "__invalid_database_ability_switch_in_terrain__" || damaged.TurnsRemaining != 5 {
		t.Fatalf("不完整入场普通场地未生成无效哨兵: %+v", damaged)
	}
}

// TestAbilitySwitchInFormChangeFromValuesRejectsIncompleteColumns 验证入场形态切换的两项 Identifier 和生命补齐开关必须
// 共同构成完整规则；损坏的半列不能被静默解读为没有效果。
func TestAbilitySwitchInFormChangeFromValuesRejectsIncompleteColumns(t *testing.T) {
	t.Parallel()
	if absent := abilitySwitchInFormChangeFromValues(pgtype.Int8{}, pgtype.Int8{}, false); absent != nil {
		t.Fatalf("未声明入场形态切换 = %+v，期望 nil", absent)
	}
	baseCreatureID, alternateCreatureID := snowflake.NewTestID(), snowflake.NewTestID()
	valid := abilitySwitchInFormChangeFromValues(databaseIdentifier(baseCreatureID), databaseIdentifier(alternateCreatureID), true)
	if valid == nil || valid.BaseCreatureID != baseCreatureID || valid.AlternateCreatureID != alternateCreatureID || !valid.AddsMaximumHPDifference {
		t.Fatalf("有效入场形态切换 = %+v", valid)
	}
	damaged := abilitySwitchInFormChangeFromValues(databaseIdentifier(baseCreatureID), pgtype.Int8{}, false)
	if damaged == nil || damaged.BaseCreatureID != baseCreatureID || damaged.AlternateCreatureID != snowflake.ID(0) {
		t.Fatalf("损坏入场形态切换未保留无效哨兵: %+v", damaged)
	}
}

// TestAbilitySwitchOutFormChangeFromValuesRejectsIncompleteColumns 验证离场形态规则的两个 Identifier 只会在同时缺失时
// 被解释为未声明；任一半列损坏都必须传递一个可由领域层拒绝的无效哨兵。
func TestAbilitySwitchOutFormChangeFromValuesRejectsIncompleteColumns(t *testing.T) {
	t.Parallel()
	if absent := abilitySwitchOutFormChangeFromValues(pgtype.Int8{}, pgtype.Int8{}); absent != nil {
		t.Fatalf("未声明离场形态切换 = %+v，期望 nil", absent)
	}
	baseCreatureID, alternateCreatureID := snowflake.NewTestID(), snowflake.NewTestID()
	valid := abilitySwitchOutFormChangeFromValues(databaseIdentifier(baseCreatureID), databaseIdentifier(alternateCreatureID))
	if valid == nil || valid.BaseCreatureID != baseCreatureID || valid.AlternateCreatureID != alternateCreatureID {
		t.Fatalf("有效离场形态切换 = %+v", valid)
	}
	damaged := abilitySwitchOutFormChangeFromValues(databaseIdentifier(baseCreatureID), pgtype.Int8{})
	if damaged == nil || damaged.BaseCreatureID != baseCreatureID || damaged.AlternateCreatureID != snowflake.ID(0) {
		t.Fatalf("损坏离场形态切换未保留无效哨兵: %+v", damaged)
	}
}

// TestAbilityOpponentSwitchRestrictionFromValuesPreservesExistence 验证主动换人限制的规则存在性与条件组合。
func TestAbilityOpponentSwitchRestrictionFromValuesPreservesExistence(t *testing.T) {
	t.Parallel()
	if absent := abilityOpponentSwitchRestrictionFromValues(false, pgtype.Int8{}, false, false); absent != nil {
		t.Fatalf("未声明主动换人限制 = %+v，期望 nil", absent)
	}
	unconditional := abilityOpponentSwitchRestrictionFromValues(true, pgtype.Int8{}, false, false)
	if unconditional == nil || unconditional.RequiredTargetElementID != nil || unconditional.RequiresGroundedTarget || unconditional.SameEffectGrantsImmunity {
		t.Fatalf("无条件主动换人限制 = %+v", unconditional)
	}
	elementID := snowflake.NewTestID()
	valid := abilityOpponentSwitchRestrictionFromValues(true, databaseIdentifier(elementID), true, true)
	if valid == nil || valid.RequiredTargetElementID == nil || *valid.RequiredTargetElementID != elementID ||
		!valid.RequiresGroundedTarget || !valid.SameEffectGrantsImmunity {
		t.Fatalf("有效主动换人限制 = %+v", valid)
	}
	damaged := abilityOpponentSwitchRestrictionFromValues(false, databaseIdentifier(elementID), false, false)
	if damaged == nil || damaged.RequiredTargetElementID == nil || *damaged.RequiredTargetElementID != snowflake.ID(0) {
		t.Fatalf("损坏主动换人限制未生成无效哨兵: %+v", damaged)
	}
}

// TestAbilityWeatherFormChangeFromValuesRejectsDamagedPayloads 验证天气形态的默认 Identifier 和 JSONB 映射严格区分
// 未声明、有效及损坏状态，避免旁路 SQL 写入把半条规则静默降级为没有效果。
func TestAbilityWeatherFormChangeFromValuesRejectsDamagedPayloads(t *testing.T) {
	t.Parallel()
	if absent := abilityWeatherFormChangeFromValues(pgtype.Int8{}, []byte("[]")); absent != nil {
		t.Fatalf("未声明天气形态切换 = %+v，期望 nil", absent)
	}
	defaultCreatureID, targetCreatureID := snowflake.NewTestID(), snowflake.NewTestID()
	valid := abilityWeatherFormChangeFromValues(
		databaseIdentifier(defaultCreatureID), []byte(`[{"weather":"rain","creatureId":"`+targetCreatureID.String()+`"}]`),
	)
	if valid == nil || valid.DefaultCreatureID != defaultCreatureID || len(valid.Targets) != 1 ||
		valid.Targets[0].Weather != abilitydetail.WeatherKindRain || valid.Targets[0].CreatureID != targetCreatureID {
		t.Fatalf("有效天气形态切换 = %+v", valid)
	}
	damaged := abilityWeatherFormChangeFromValues(databaseIdentifier(defaultCreatureID), []byte(`[{"weather":"rain"}]`))
	if damaged == nil || damaged.DefaultCreatureID != defaultCreatureID || len(damaged.Targets) != 1 ||
		damaged.Targets[0].Weather != abilitydetail.WeatherKindRain || damaged.Targets[0].CreatureID != snowflake.ID(0) {
		t.Fatalf("损坏天气形态 JSON 未生成无效哨兵: %+v", damaged)
	}
}

// TestAbilitySwitchInStatStageFromValuesRejectsIncompleteTriples 验证入场能力阶级变化的三个持久化字段必须完整出现。
func TestAbilitySwitchInStatStageFromValuesRejectsIncompleteTriples(t *testing.T) {
	t.Parallel()
	if absent := abilitySwitchInStatStageFromValues(pgtype.Text{}, pgtype.Int8{}, 0); absent != nil {
		t.Fatalf("未声明入场能力阶级变化 = %+v，期望 nil", absent)
	}
	statID := snowflake.NewTestID()
	valid := abilitySwitchInStatStageFromValues(
		pgtype.Text{String: "opponents", Valid: true}, databaseIdentifier(statID), -1,
	)
	if valid == nil || valid.Target != abilitydetail.SwitchInStatStageTargetOpponents || valid.StatID != statID || valid.StageDelta != -1 {
		t.Fatalf("有效入场能力阶级变化 = %+v", valid)
	}
	damaged := abilitySwitchInStatStageFromValues(pgtype.Text{}, databaseIdentifier(statID), -1)
	if damaged == nil || damaged.Target != "__invalid_database_ability_switch_in_stat_stage__" {
		t.Fatalf("不完整入场能力阶级变化未生成无效哨兵: %+v", damaged)
	}
}

// TestAbilityTerastallizationStatStageFromValuesPreservesIncompleteValues 验证太晶化能力阶级变化双字段不会把损坏组合静默降级为无效果。
func TestAbilityTerastallizationStatStageFromValuesPreservesIncompleteValues(t *testing.T) {
	t.Parallel()
	if absent := abilityTerastallizationStatStageFromValues(pgtype.Int8{}, 0); absent != nil {
		t.Fatalf("未声明太晶化能力阶级变化 = %+v，期望 nil", absent)
	}
	statID := snowflake.NewTestID()
	valid := abilityTerastallizationStatStageFromValues(databaseIdentifier(statID), 1)
	if valid == nil || valid.StatID != statID || valid.StageDelta != 1 {
		t.Fatalf("有效太晶化能力阶级变化 = %+v", valid)
	}
	damaged := abilityTerastallizationStatStageFromValues(pgtype.Int8{}, 1)
	if damaged == nil || damaged.StatID != snowflake.ID(0) || damaged.StageDelta != 1 {
		t.Fatalf("不完整太晶化能力阶级变化被静默忽略: %+v", damaged)
	}
}

// TestAbilitySwitchInAllyHealFromValuePreservesInvalidDenominator 验证入场同侧回复读取时不会把损坏分母静默改为未声明。
func TestAbilitySwitchInAllyHealFromValuePreservesInvalidDenominator(t *testing.T) {
	t.Parallel()
	if absent := abilitySwitchInAllyHealFromValue(0); absent != nil {
		t.Fatalf("未声明入场同侧回复 = %+v，期望 nil", absent)
	}
	if valid := abilitySwitchInAllyHealFromValue(16); valid == nil || valid.HealDenominator != 16 {
		t.Fatalf("有效入场同侧回复 = %+v", valid)
	}
	if damaged := abilitySwitchInAllyHealFromValue(-1); damaged == nil || damaged.HealDenominator != -1 {
		t.Fatalf("损坏入场同侧回复分母被静默忽略: %+v", damaged)
	}
}

// TestAbilityWeatherDamageImmunitiesFromJSONRejectsDamagedPayloads 验证特性天气伤害免疫 JSON 严格区分空数组和
// 损坏载荷；数据库值异常时必须留下无效哨兵交由领域及 Battle 拒绝，不能静默视为没有特性效果。
func TestAbilityWeatherDamageImmunitiesFromJSONRejectsDamagedPayloads(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name    string
		payload string
		valid   bool
	}{
		{name: "空数组", payload: "[]", valid: true},
		{name: "有效天气", payload: `["sandstorm"]`, valid: true},
		{name: "非数组", payload: `{"weather":"sandstorm"}`},
		{name: "空值", payload: `null`},
		{name: "尾随值", payload: `[] []`},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			values := abilityWeatherDamageImmunitiesFromJSON([]byte(test.payload))
			if test.valid {
				if test.payload == "[]" && len(values) != 0 {
					t.Fatalf("空数组解码 = %+v", values)
				}
				if test.payload != "[]" && (len(values) != 1 || values[0] != abilitydetail.WeatherKindSandstorm) {
					t.Fatalf("有效天气免疫解码 = %+v", values)
				}
				return
			}
			if len(values) != 1 || values[0] != "__invalid_database_ability_weather_damage_immunity__" {
				t.Fatalf("损坏载荷未生成无效哨兵: %+v", values)
			}
		})
	}
}

// TestAbilityWeatherEndTurnHealFromValuesRejectsDamagedPayloads 验证两列天气回复资料必须同时构成一条完整规则；
// 空数组与 0 分母才表示未声明，其余损坏组合必须留下哨兵交由 Battle 拒绝。
func TestAbilityWeatherEndTurnHealFromValuesRejectsDamagedPayloads(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name        string
		weathers    string
		denominator int32
		valid       bool
	}{
		{name: "未声明", weathers: "[]", denominator: 0, valid: true},
		{name: "有效规则", weathers: `["rain"]`, denominator: 16, valid: true},
		{name: "只有天气", weathers: `["rain"]`, denominator: 0},
		{name: "只有分母", weathers: "[]", denominator: 16},
		{name: "非数组", weathers: `{"weather":"rain"}`, denominator: 16},
		{name: "尾随值", weathers: `[] []`, denominator: 16},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			value := abilityWeatherEndTurnHealFromValues([]byte(test.weathers), test.denominator)
			if test.valid {
				if test.denominator == 0 && value != nil {
					t.Fatalf("未声明规则解码 = %+v", value)
				}
				if test.denominator != 0 && (value == nil || len(value.Weathers) != 1 || value.Weathers[0] != abilitydetail.WeatherKindRain || value.HealDenominator != 16) {
					t.Fatalf("有效天气回复解码 = %+v", value)
				}
				return
			}
			if value == nil || len(value.Weathers) != 1 || value.Weathers[0] != "__invalid_database_ability_weather_end_turn_heal__" {
				t.Fatalf("损坏天气回复资料未生成无效哨兵: %+v", value)
			}
		})
	}
}

// TestWeatherAccuracyOverridesFromJSONRejectsDamagedPayloads 验证天气命中覆盖 JSON 严格拒绝未知字段、非数组和
// 语法后仍有尾随值的载荷；空数组与 0 命中率则必须保留各自的明确领域语义。
func TestWeatherAccuracyOverridesFromJSONRejectsDamagedPayloads(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name    string
		payload string
		valid   bool
	}{
		{name: "空数组", payload: "[]", valid: true},
		{name: "必中覆盖", payload: `[{"weather":"rain","accuracyPercent":0}]`, valid: true},
		{name: "未知字段", payload: `[{"weather":"rain","accuracyPercent":0,"unknown":true}]`},
		{name: "重复字段", payload: `[{"weather":"rain","weather":"sun","accuracyPercent":0}]`},
		{name: "缺失命中率", payload: `[{"weather":"rain"}]`},
		{name: "非数组", payload: `{"weather":"rain","accuracyPercent":0}`},
		{name: "尾随值", payload: `[] []`},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			values := weatherAccuracyOverridesFromJSON([]byte(test.payload))
			if test.valid {
				if test.payload == "[]" && len(values) != 0 {
					t.Fatalf("空数组解码 = %+v", values)
				}
				if test.payload != "[]" && (len(values) != 1 || values[0].Weather != "rain" || values[0].AccuracyPercent != 0) {
					t.Fatalf("必中覆盖解码 = %+v", values)
				}
				return
			}
			if len(values) != 1 || values[0].Weather != "__invalid_database_weather_accuracy_override__" {
				t.Fatalf("损坏载荷未生成无效哨兵: %+v", values)
			}
		})
	}
}

// TestWeatherElementOverridesFromJSONRejectsDamagedPayloads 验证天气属性覆盖 JSON 严格拒绝未知字段、重复字段、
// 非数组和尾随载荷；空数组与完整 weather/elementId 对象必须保留为明确的资料语义。
func TestWeatherElementOverridesFromJSONRejectsDamagedPayloads(t *testing.T) {
	t.Parallel()
	elementID := snowflake.NewTestID()
	tests := []struct {
		name    string
		payload string
		valid   bool
	}{
		{name: "空数组", payload: "[]", valid: true},
		{name: "有效覆盖", payload: `[{"weather":"rain","elementId":"` + elementID.String() + `"}]`, valid: true},
		{name: "未知字段", payload: `[{"weather":"rain","elementId":"` + elementID.String() + `","unknown":true}]`},
		{name: "重复字段", payload: `[{"weather":"rain","weather":"sun","elementId":"` + elementID.String() + `"}]`},
		{name: "缺失目标属性", payload: `[{"weather":"rain"}]`},
		{name: "非数组", payload: `{"weather":"rain","elementId":"` + elementID.String() + `"}`},
		{name: "尾随值", payload: `[] []`},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			values := weatherElementOverridesFromJSON([]byte(test.payload))
			if test.valid {
				if test.payload == "[]" && len(values) != 0 {
					t.Fatalf("空数组解码 = %+v", values)
				}
				if test.payload != "[]" && (len(values) != 1 || values[0].Weather != "rain" || values[0].ElementID != elementID) {
					t.Fatalf("天气属性覆盖解码 = %+v", values)
				}
				return
			}
			if len(values) != 1 || values[0].Weather != "__invalid_database_weather_element_override__" {
				t.Fatalf("损坏载荷未生成无效哨兵: %+v", values)
			}
		})
	}
}

// TestWeatherPowerMultipliersFromJSONRejectsDamagedPayloads 验证天气威力倍率 JSON 严格拒绝未知字段、重复字段、
// 非数组、缺少必要分数或尾随载荷；损坏资料必须转换成无效哨兵，交由 Battle 编译边界显式阻止。
func TestWeatherPowerMultipliersFromJSONRejectsDamagedPayloads(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name    string
		payload string
		valid   bool
	}{
		{name: "空数组", payload: "[]", valid: true},
		{name: "有效倍率", payload: `[{"weather":"rain","numerator":3,"denominator":2}]`, valid: true},
		{name: "未知字段", payload: `[{"weather":"rain","numerator":3,"denominator":2,"unknown":true}]`},
		{name: "重复字段", payload: `[{"weather":"rain","weather":"sun","numerator":3,"denominator":2}]`},
		{name: "缺失分母", payload: `[{"weather":"rain","numerator":3}]`},
		{name: "非数组", payload: `{"weather":"rain","numerator":3,"denominator":2}`},
		{name: "尾随值", payload: `[] []`},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			values := weatherPowerMultipliersFromJSON([]byte(test.payload))
			if test.valid {
				if test.payload == "[]" && len(values) != 0 {
					t.Fatalf("空数组解码 = %+v", values)
				}
				if test.payload != "[]" && (len(values) != 1 || values[0].Weather != "rain" || values[0].Numerator != 3 || values[0].Denominator != 2) {
					t.Fatalf("天气威力倍率解码 = %+v", values)
				}
				return
			}
			if len(values) != 1 || values[0].Weather != "__invalid_database_weather_power_multiplier__" {
				t.Fatalf("损坏载荷未生成无效哨兵: %+v", values)
			}
		})
	}
}

// TestAbilityWeatherSpeedMultipliersFromJSONRejectsDamagedPayloads 验证特性天气速度倍率 JSON
// 严格拒绝未知字段、重复字段、非数组、缺少必要分数或尾随载荷。损坏资料必须保留无效哨兵，
// 由领域校验和 Battle 编译边界明确阻止，不能被静默降级为未配置的速度倍率。
func TestAbilityWeatherSpeedMultipliersFromJSONRejectsDamagedPayloads(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name    string
		payload string
		valid   bool
	}{
		{name: "空数组", payload: "[]", valid: true},
		{name: "有效倍率", payload: `[{"weather":"rain","numerator":2,"denominator":1}]`, valid: true},
		{name: "未知字段", payload: `[{"weather":"rain","numerator":2,"denominator":1,"unknown":true}]`},
		{name: "重复字段", payload: `[{"weather":"rain","weather":"sun","numerator":2,"denominator":1}]`},
		{name: "缺少分母", payload: `[{"weather":"rain","numerator":2}]`},
		{name: "非数组", payload: `{"weather":"rain","numerator":2,"denominator":1}`},
		{name: "尾随载荷", payload: `[] []`},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			values := abilityWeatherSpeedMultipliersFromJSON([]byte(test.payload))
			if test.valid {
				if test.payload == "[]" && len(values) != 0 {
					t.Fatalf("空数组解码 = %+v", values)
				}
				if test.payload != "[]" && (len(values) != 1 || values[0].Weather != "rain" || values[0].Numerator != 2 || values[0].Denominator != 1) {
					t.Fatalf("特性天气速度倍率解码 = %+v", values)
				}
				return
			}
			if len(values) != 1 || values[0].Weather != "__invalid_database_ability_weather_speed_multiplier__" {
				t.Fatalf("损坏载荷未生成无效哨兵: %+v", values)
			}
		})
	}
}

// TestLeechSeedFromJSONRejectsUnknownOrMalformedValues 验证数据库被应用外 SQL 写入的损坏寄生种子 JSON
// 会转为无效哨兵，由资料编译边界拒绝，而不会被静默视为未配置。
func TestLeechSeedFromJSONRejectsUnknownOrMalformedValues(t *testing.T) {
	t.Parallel()

	valid := leechSeedFromJSON([]byte(`{"chancePercent":100}`))
	if valid == nil || valid.ChancePercent != 100 {
		t.Fatalf("有效寄生种子 JSON = %+v", valid)
	}
	if absent := leechSeedFromJSON([]byte(`{}`)); absent != nil {
		t.Fatalf("空寄生种子 JSON = %+v，期望 nil", absent)
	}
	for _, value := range [][]byte{
		[]byte(`{"chancePercent":100,"unknown":true}`),
		[]byte(`{"chancePercent":100}{}`),
		[]byte(`[]`),
		[]byte(`not-json`),
	} {
		decoded := leechSeedFromJSON(value)
		if decoded == nil || decoded.ChancePercent != 0 {
			t.Fatalf("损坏寄生种子 JSON %s = %+v，期望无效哨兵", value, decoded)
		}
	}
}

// TestWeatherFromJSONRejectsUnknownOrMalformedValues 验证损坏普通天气 JSON 会转为无效哨兵，由资料编译边界拒绝，
// 而不会被静默当作未配置天气。
func TestWeatherFromJSONRejectsUnknownOrMalformedValues(t *testing.T) {
	t.Parallel()

	valid := weatherFromJSON([]byte(`{"kind":"sandstorm","turnsRemaining":5,"chancePercent":100}`))
	if valid == nil || valid.Kind != "sandstorm" || valid.TurnsRemaining != 5 || valid.ChancePercent != 100 {
		t.Fatalf("有效普通天气 JSON = %+v", valid)
	}
	if absent := weatherFromJSON([]byte(`{}`)); absent != nil {
		t.Fatalf("空普通天气 JSON = %+v，期望 nil", absent)
	}
	for _, value := range [][]byte{
		[]byte(`{"kind":"sandstorm","turnsRemaining":5,"chancePercent":100,"unknown":true}`),
		[]byte(`{"kind":"sandstorm","turnsRemaining":5,"chancePercent":100}{}`),
		[]byte(`[]`),
		[]byte(`not-json`),
	} {
		decoded := weatherFromJSON(value)
		if decoded == nil || decoded.Kind != "__invalid_database_weather__" {
			t.Fatalf("损坏普通天气 JSON %s = %+v，期望无效哨兵", value, decoded)
		}
	}
}

// TestTerrainFromJSONRejectsUnknownOrMalformedValues 验证损坏普通场地 JSON 会转为无效哨兵，由资料编译边界拒绝，
// 而不会被静默当作未配置场地。
func TestTerrainFromJSONRejectsUnknownOrMalformedValues(t *testing.T) {
	t.Parallel()

	valid := terrainFromJSON([]byte(`{"kind":"grassy","turnsRemaining":5,"chancePercent":100}`))
	if valid == nil || valid.Kind != "grassy" || valid.TurnsRemaining != 5 || valid.ChancePercent != 100 {
		t.Fatalf("有效普通场地 JSON = %+v", valid)
	}
	if absent := terrainFromJSON([]byte(`{}`)); absent != nil {
		t.Fatalf("空普通场地 JSON = %+v，期望 nil", absent)
	}
	for _, value := range [][]byte{
		[]byte(`{"kind":"grassy","turnsRemaining":5,"chancePercent":100,"unknown":true}`),
		[]byte(`{"kind":"grassy","turnsRemaining":5,"chancePercent":100}{}`),
		[]byte(`[]`),
		[]byte(`not-json`),
	} {
		decoded := terrainFromJSON(value)
		if decoded == nil || decoded.Kind != "__invalid_database_terrain__" {
			t.Fatalf("损坏普通场地 JSON %s = %+v，期望无效哨兵", value, decoded)
		}
	}
}

// TestTailwindFromJSONRejectsUnknownOrMalformedValues 验证损坏顺风 JSON 会转为无效哨兵，由 Battle 编译边界拒绝，
// 而不会被静默当作未配置顺风。
func TestTailwindFromJSONRejectsUnknownOrMalformedValues(t *testing.T) {
	t.Parallel()

	valid := tailwindFromJSON([]byte(`{"turnsRemaining":5,"chancePercent":100}`))
	if valid == nil || valid.TurnsRemaining != 5 || valid.ChancePercent != 100 {
		t.Fatalf("有效顺风 JSON = %+v", valid)
	}
	if absent := tailwindFromJSON([]byte(`{}`)); absent != nil {
		t.Fatalf("空顺风 JSON = %+v，期望 nil", absent)
	}
	for _, value := range [][]byte{
		[]byte(`{"turnsRemaining":5,"chancePercent":100,"unknown":true}`),
		[]byte(`{"turnsRemaining":5,"chancePercent":100}{}`),
		[]byte(`[]`),
		[]byte(`not-json`),
	} {
		decoded := tailwindFromJSON(value)
		if decoded == nil || decoded.TurnsRemaining != -1 {
			t.Fatalf("损坏顺风 JSON %s = %+v，期望无效哨兵", value, decoded)
		}
	}
}

// TestReflectFromJSONRejectsUnknownOrMalformedValues 验证损坏反射壁 JSON 会转为无效哨兵，而不会静默失去物理屏障。
func TestReflectFromJSONRejectsUnknownOrMalformedValues(t *testing.T) {
	t.Parallel()

	valid := reflectFromJSON([]byte(`{"turnsRemaining":5,"chancePercent":100}`))
	if valid == nil || valid.TurnsRemaining != 5 || valid.ChancePercent != 100 {
		t.Fatalf("有效反射壁 JSON = %+v", valid)
	}
	if absent := reflectFromJSON([]byte(`{}`)); absent != nil {
		t.Fatalf("空反射壁 JSON = %+v，期望 nil", absent)
	}
	for _, value := range [][]byte{
		[]byte(`{"turnsRemaining":5,"chancePercent":100,"unknown":true}`),
		[]byte(`{"turnsRemaining":5,"chancePercent":100}{}`),
		[]byte(`[]`),
		[]byte(`not-json`),
	} {
		decoded := reflectFromJSON(value)
		if decoded == nil || decoded.TurnsRemaining != -1 {
			t.Fatalf("损坏反射壁 JSON %s = %+v，期望无效哨兵", value, decoded)
		}
	}
}

// TestLightScreenFromJSONRejectsUnknownOrMalformedValues 验证损坏光墙 JSON 会转为无效哨兵，而不会静默失去特殊屏障。
func TestLightScreenFromJSONRejectsUnknownOrMalformedValues(t *testing.T) {
	t.Parallel()

	valid := lightScreenFromJSON([]byte(`{"turnsRemaining":5,"chancePercent":100}`))
	if valid == nil || valid.TurnsRemaining != 5 || valid.ChancePercent != 100 {
		t.Fatalf("有效光墙 JSON = %+v", valid)
	}
	if absent := lightScreenFromJSON([]byte(`{}`)); absent != nil {
		t.Fatalf("空光墙 JSON = %+v，期望 nil", absent)
	}
	for _, value := range [][]byte{
		[]byte(`{"turnsRemaining":5,"chancePercent":100,"unknown":true}`),
		[]byte(`{"turnsRemaining":5,"chancePercent":100}{}`),
		[]byte(`[]`),
		[]byte(`not-json`),
	} {
		decoded := lightScreenFromJSON(value)
		if decoded == nil || decoded.TurnsRemaining != -1 {
			t.Fatalf("损坏光墙 JSON %s = %+v，期望无效哨兵", value, decoded)
		}
	}
}

// TestAuroraVeilFromJSONRejectsUnknownOrMalformedValues 验证损坏极光幕 JSON 会转为无效哨兵，而不会静默失去双防屏障。
func TestAuroraVeilFromJSONRejectsUnknownOrMalformedValues(t *testing.T) {
	t.Parallel()

	valid := auroraVeilFromJSON([]byte(`{"turnsRemaining":5,"chancePercent":100}`))
	if valid == nil || valid.TurnsRemaining != 5 || valid.ChancePercent != 100 {
		t.Fatalf("有效极光幕 JSON = %+v", valid)
	}
	if absent := auroraVeilFromJSON([]byte(`{}`)); absent != nil {
		t.Fatalf("空极光幕 JSON = %+v，期望 nil", absent)
	}
	for _, value := range [][]byte{
		[]byte(`{"turnsRemaining":5,"chancePercent":100,"unknown":true}`),
		[]byte(`{"turnsRemaining":5,"chancePercent":100}{}`),
		[]byte(`[]`),
		[]byte(`not-json`),
	} {
		decoded := auroraVeilFromJSON(value)
		if decoded == nil || decoded.TurnsRemaining != -1 {
			t.Fatalf("损坏极光幕 JSON %s = %+v，期望无效哨兵", value, decoded)
		}
	}
}

// TestHazardFromJSONRejectsUnknownOrMalformedValues 验证四种入场危害各自的 JSON 解码都拒绝未知字段、拼接载荷和
// 非对象值，避免损坏资料被静默解释为未配置规则。
func TestHazardFromJSONRejectsUnknownOrMalformedValues(t *testing.T) {
	t.Parallel()

	testCases := []struct {
		name    string
		decode  func([]byte) int32
		valid   []byte
		invalid []byte
	}{
		{
			name: "撒菱", decode: func(value []byte) int32 {
				result := spikesFromJSON(value)
				if result == nil {
					return 0
				}
				return result.ChancePercent
			}, valid: []byte(`{"chancePercent":80}`), invalid: []byte(`{"chancePercent":80,"unknown":true}`),
		},
		{
			name: "隐形岩", decode: func(value []byte) int32 {
				result := stealthRockFromJSON(value)
				if result == nil {
					return 0
				}
				return result.ChancePercent
			}, valid: []byte(`{"chancePercent":81}`), invalid: []byte(`{"chancePercent":81}{}`),
		},
		{
			name: "毒菱", decode: func(value []byte) int32 {
				result := toxicSpikesFromJSON(value)
				if result == nil {
					return 0
				}
				return result.ChancePercent
			}, valid: []byte(`{"chancePercent":82}`), invalid: []byte(`[]`),
		},
		{
			name: "黏黏网", decode: func(value []byte) int32 {
				result := stickyWebFromJSON(value)
				if result == nil {
					return 0
				}
				return result.ChancePercent
			}, valid: []byte(`{"chancePercent":83}`), invalid: []byte(`not-json`),
		},
	}
	for _, testCase := range testCases {
		testCase := testCase
		t.Run(testCase.name, func(t *testing.T) {
			t.Parallel()
			if actual := testCase.decode(testCase.valid); actual < 1 {
				t.Fatalf("有效%s JSON 解码 = %d", testCase.name, actual)
			}
			if actual := testCase.decode(testCase.invalid); actual != -1 {
				t.Fatalf("损坏%s JSON 解码 = %d，期望无效哨兵", testCase.name, actual)
			}
		})
	}
}

// TestFixedClearRuleFromJSONRequiresEnabled 验证快速旋转与清除浓雾以 enabled=true 区分固定规则和未配置空对象，
// 同时拒绝未知字段与未显式启用的损坏值。
func TestFixedClearRuleFromJSONRequiresEnabled(t *testing.T) {
	t.Parallel()

	if result := rapidSpinFromJSON([]byte(`{"enabled":true}`)); result == nil || !result.Enabled {
		t.Fatalf("有效快速旋转 JSON = %+v", result)
	}
	if result := defogFromJSON([]byte(`{"enabled":true}`)); result == nil || !result.Enabled {
		t.Fatalf("有效清除浓雾 JSON = %+v", result)
	}
	if result := rapidSpinFromJSON([]byte(`{}`)); result != nil {
		t.Fatalf("空快速旋转 JSON = %+v，期望 nil", result)
	}
	if result := defogFromJSON([]byte(`{}`)); result != nil {
		t.Fatalf("空清除浓雾 JSON = %+v，期望 nil", result)
	}
	if result := rapidSpinFromJSON([]byte(`{"enabled":false}`)); result == nil || result.Enabled {
		t.Fatalf("未启用快速旋转 JSON = %+v，期望无效哨兵", result)
	}
	if result := defogFromJSON([]byte(`{"enabled":true,"unknown":true}`)); result == nil || result.Enabled {
		t.Fatalf("损坏清除浓雾 JSON = %+v，期望无效哨兵", result)
	}
}
