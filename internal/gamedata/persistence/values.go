package persistence

import (
	"bytes"
	"encoding/json"
	"encoding/json/jsontext"
	jsonv2 "encoding/json/v2"
	"io"
	"reflect"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/jackc/pgx/v5/pgtype"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/ability"
	"github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
	"github.com/lishangbu/avalon/internal/gamedata/battlerules"
	"github.com/lishangbu/avalon/internal/gamedata/element"
	"github.com/lishangbu/avalon/internal/gamedata/item"
	"github.com/lishangbu/avalon/internal/gamedata/itemcategory"
	"github.com/lishangbu/avalon/internal/gamedata/skill"
	"github.com/lishangbu/avalon/internal/gamedata/skillailment"
	"github.com/lishangbu/avalon/internal/gamedata/skillcategory"
	"github.com/lishangbu/avalon/internal/gamedata/skilldamageclass"
	"github.com/lishangbu/avalon/internal/gamedata/skilldetail"
	"github.com/lishangbu/avalon/internal/gamedata/skilllearnmethod"
	"github.com/lishangbu/avalon/internal/gamedata/skilltarget"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
)

func databaseIdentifier(id snowflake.ID) pgtype.Int8 {
	return pgtype.Int8{Int64: int64(id), Valid: true}
}

// copyEntDetailFields 将 Ent 详情实体直接映射为领域详情，避免经过旧 SQL 行模型。
func copyEntDetailFields(destination any, source any) {
	copyEntDetailStruct(reflect.ValueOf(destination).Elem(), reflect.ValueOf(source).Elem())
}

func copyEntDetailStruct(destination, source reflect.Value) {
	for index := 0; index < destination.NumField(); index++ {
		field := destination.Type().Field(index)
		target := destination.Field(index)
		if !target.CanSet() {
			continue
		}
		if field.Anonymous && target.Kind() == reflect.Struct {
			copyEntDetailStruct(target, source)
			continue
		}
		origin := source.FieldByName(field.Name)
		if origin.IsValid() {
			setEntDetailValue(target, origin)
		}
	}
}

func setEntDetailValue(target, origin reflect.Value) {
	if origin.Kind() == reflect.Pointer {
		if origin.IsNil() {
			target.SetZero()
			return
		}
		origin = origin.Elem()
	}
	if target.Kind() == reflect.Pointer {
		value := reflect.New(target.Type().Elem())
		setEntDetailValue(value.Elem(), origin)
		target.Set(value)
		return
	}
	if origin.Type() == reflect.TypeOf(snowflake.ID(0)) {
		value := reflect.ValueOf(snowflake.ID(origin.Interface().(snowflake.ID)))
		if value.Type().AssignableTo(target.Type()) {
			target.Set(value)
		}
		return
	}
	if origin.Kind() == reflect.Slice && origin.Type().Elem().Kind() == reflect.Uint8 && (target.Kind() == reflect.Struct || target.Kind() == reflect.Slice || target.Kind() == reflect.Map) {
		_ = jsonv2.Unmarshal(origin.Bytes(), target.Addr().Interface())
		return
	}
	if origin.Type().AssignableTo(target.Type()) {
		target.Set(origin)
		return
	}
	if origin.Type().ConvertibleTo(target.Type()) {
		target.Set(origin.Convert(target.Type()))
	}
}

// applySQLParamsToEntBuilder 将旧参数结构中的 pgtype 值映射到 Ent Builder 的 Set/Clear 方法。
// 该函数只承担字段类型边界转换，不执行 SQL；实际持久化仍由 Ent Builder 完成。
func applySQLParamsToEntBuilder(builder any, params any) error {
	target := reflect.ValueOf(builder)
	source := reflect.ValueOf(params)
	if source.Kind() == reflect.Pointer {
		source = source.Elem()
	}
	for index := 0; index < source.NumField(); index++ {
		field := source.Type().Field(index)
		value := source.Field(index)
		setter := target.MethodByName("Set" + field.Name)
		converted, valid := entBuilderValue(value, setter)
		if valid && setter.IsValid() {
			setter.Call([]reflect.Value{converted})
			continue
		}
		if !valid {
			clear := target.MethodByName("Clear" + field.Name)
			if clear.IsValid() {
				clear.Call(nil)
			}
		}
	}
	return nil
}

// entBuilderValue 把 pgtype 包装值转换为目标 Setter 的参数类型。
func entBuilderValue(value reflect.Value, setter reflect.Value) (reflect.Value, bool) {
	if setter.IsValid() && setter.Type().NumIn() == 1 {
		valueType := setter.Type().In(0)
		switch value.Interface().(type) {
		case pgtype.Text:
			item := value.Interface().(pgtype.Text)
			if !item.Valid {
				return reflect.Value{}, false
			}
			return reflect.ValueOf(item.String).Convert(valueType), true
		case pgtype.Int2:
			item := value.Interface().(pgtype.Int2)
			if !item.Valid {
				return reflect.Value{}, false
			}
			return reflect.ValueOf(item.Int16).Convert(valueType), true
		case pgtype.Int4:
			item := value.Interface().(pgtype.Int4)
			if !item.Valid {
				return reflect.Value{}, false
			}
			return reflect.ValueOf(item.Int32).Convert(valueType), true
		case pgtype.Int8:
			item := value.Interface().(pgtype.Int8)
			if !item.Valid {
				return reflect.Value{}, false
			}
			return reflect.ValueOf(item.Int64).Convert(valueType), true
		case pgtype.Timestamptz:
			item := value.Interface().(pgtype.Timestamptz)
			if !item.Valid {
				return reflect.Value{}, false
			}
			return reflect.ValueOf(item.Time).Convert(valueType), true
		case pgtype.Bool:
			item := value.Interface().(pgtype.Bool)
			if !item.Valid {
				return reflect.Value{}, false
			}
			return reflect.ValueOf(item.Bool).Convert(valueType), true
		}
		if value.Type().AssignableTo(valueType) {
			return value, true
		}
		if value.Type().ConvertibleTo(valueType) {
			return value.Convert(valueType), true
		}
	}
	return reflect.Value{}, false
}

func nullableDatabaseIdentifier(id *snowflake.ID) pgtype.Int8 {
	if id == nil {
		return pgtype.Int8{}
	}
	return databaseIdentifier(*id)
}

func domainIdentifier(id pgtype.Int8) snowflake.ID {
	return snowflake.ID(id.Int64)
}

func nullableDomainIdentifier(id pgtype.Int8) *snowflake.ID {
	if !id.Valid {
		return nil
	}
	value := domainIdentifier(id)
	return &value
}

func databaseTime(value time.Time) pgtype.Timestamptz {
	return pgtype.Timestamptz{Time: value.UTC(), Valid: true}
}

func databaseInt32(value *int32) pgtype.Int4 {
	if value == nil {
		return pgtype.Int4{}
	}
	return pgtype.Int4{Int32: *value, Valid: true}
}

func databaseBool(value *bool) pgtype.Bool {
	if value == nil {
		return pgtype.Bool{}
	}
	return pgtype.Bool{Bool: *value, Valid: true}
}

func databaseText[T ~string](value *T) pgtype.Text {
	if value == nil {
		return pgtype.Text{}
	}
	return pgtype.Text{String: string(*value), Valid: true}
}

func nullableDomainText(value pgtype.Text) *string {
	if !value.Valid {
		return nil
	}
	result := value.String
	return &result
}

func nullableDomainInt32(value pgtype.Int4) *int32 {
	if !value.Valid {
		return nil
	}
	result := value.Int32
	return &result
}

func elementFromValues(id pgtype.Int8, code, name string, sortOrder int32, enabled bool, version int64) element.Element {
	return element.Element{
		ID: domainIdentifier(id), Code: code, Name: name, SortOrder: sortOrder, Enabled: enabled, Version: version,
	}
}

func abilityFromValues(id pgtype.Int8, code, name string, mainSeries, enabled bool, version int64) ability.Ability {
	return ability.Ability{
		ID: domainIdentifier(id), Code: code, Name: name, MainSeries: mainSeries, Enabled: enabled, Version: version,
	}
}

func abilityFromEnt(row *avalonent.GameAbility) (ability.Ability, error) {
	rules, err := battlerules.ParseAbility(row.Rules)
	if err != nil {
		return ability.Ability{}, err
	}
	return ability.Ability{
		ID: domainIdentifier(pgIdentifier(row.ID)), Code: row.Code, Name: row.Name, MainSeries: row.MainSeries,
		Effect: row.Effect, ShortEffect: row.ShortEffect, Introduction: row.Introduction,
		Rules: rules, Enabled: row.Enabled, Version: row.Version,
	}, nil
}

func weatherAccuracyOverridesFromJSON(value []byte) []skilldetail.WeatherAccuracyOverride {
	value = bytes.TrimSpace(value)
	if string(value) == "[]" {
		return []skilldetail.WeatherAccuracyOverride{}
	}
	invalid := func() []skilldetail.WeatherAccuracyOverride {
		return []skilldetail.WeatherAccuracyOverride{{Weather: "__invalid_database_weather_accuracy_override__"}}
	}
	if len(value) == 0 || value[0] != '[' {
		return invalid()
	}
	var rawValues []jsontext.Value
	if err := jsonv2.Unmarshal(value, &rawValues); err != nil || rawValues == nil {
		return invalid()
	}
	result := make([]skilldetail.WeatherAccuracyOverride, 0, len(rawValues))
	for _, raw := range rawValues {
		override, ok := weatherAccuracyOverrideFromJSON(raw)
		if !ok {
			return invalid()
		}
		result = append(result, override)
	}
	return result
}

// weatherAccuracyOverrideFromJSON 严格读取一条天气命中覆盖对象。
//
// encoding/json/v2 会拒绝重复键；这里额外通过指针字段拒绝缺失必填键，并拒绝未知字段，确保数据库
// 存储与管理 API 的“每种天气恰有一个命中率”契约一致。
func weatherAccuracyOverrideFromJSON(value jsontext.Value) (skilldetail.WeatherAccuracyOverride, bool) {
	var payload struct {
		Weather         *skilldetail.WeatherKind `json:"weather"`
		AccuracyPercent *int32                   `json:"accuracyPercent"`
	}
	if err := jsonv2.Unmarshal(value, &payload, jsonv2.RejectUnknownMembers(true)); err != nil ||
		payload.Weather == nil || payload.AccuracyPercent == nil {
		return skilldetail.WeatherAccuracyOverride{}, false
	}
	return skilldetail.WeatherAccuracyOverride{
		Weather: *payload.Weather, AccuracyPercent: *payload.AccuracyPercent,
	}, true
}

// weatherElementOverridesFromJSON 将数据库 JSONB 数组转换为强类型天气属性覆盖资料。
//
// JSONB 只保证语法而不保证资料契约；对象未知字段、非数组载荷、尾随 JSON 值或空字节都必须留下无效哨兵，
// 交由领域服务与 Battle 编译边界拒绝。重复天气也不能在读取时覆盖或排序，否则同一场对局会依赖 JSON 数组顺序。
func weatherElementOverridesFromJSON(value []byte) []skilldetail.WeatherElementOverride {
	value = bytes.TrimSpace(value)
	if string(value) == "[]" {
		return []skilldetail.WeatherElementOverride{}
	}
	invalid := func() []skilldetail.WeatherElementOverride {
		return []skilldetail.WeatherElementOverride{{Weather: "__invalid_database_weather_element_override__"}}
	}
	if len(value) == 0 || value[0] != '[' {
		return invalid()
	}
	var rawValues []jsontext.Value
	if err := jsonv2.Unmarshal(value, &rawValues); err != nil || rawValues == nil {
		return invalid()
	}
	result := make([]skilldetail.WeatherElementOverride, 0, len(rawValues))
	for _, raw := range rawValues {
		override, ok := weatherElementOverrideFromJSON(raw)
		if !ok {
			return invalid()
		}
		result = append(result, override)
	}
	return result
}

// weatherElementOverrideFromJSON 严格读取一条天气属性覆盖对象。
//
// encoding/json/v2 会拒绝重复键；这里额外通过指针字段拒绝缺失必填键，并拒绝未知字段，确保数据库
// 数据与管理 API 的单一属性覆盖契约一致。
func weatherElementOverrideFromJSON(value jsontext.Value) (skilldetail.WeatherElementOverride, bool) {
	var payload struct {
		Weather   *skilldetail.WeatherKind `json:"weather"`
		ElementID *snowflake.ID            `json:"elementId"`
	}
	if err := jsonv2.Unmarshal(value, &payload, jsonv2.RejectUnknownMembers(true)); err != nil ||
		payload.Weather == nil || payload.ElementID == nil {
		return skilldetail.WeatherElementOverride{}, false
	}
	return skilldetail.WeatherElementOverride{Weather: *payload.Weather, ElementID: *payload.ElementID}, true
}

// weatherPowerMultipliersFromJSON 将数据库 JSONB 数组转换为强类型天气威力倍率资料。
//
// 倍率的零分子、零分母和未知天气都会在领域校验阶段变为无效资料；这里还使用指针字段拒绝缺失的必填成员，
// 并拒绝非数组、未知字段、重复 JSON 键和语法损坏，绝不把损坏载荷静默降级为没有倍率。
func weatherPowerMultipliersFromJSON(value []byte) []skilldetail.WeatherPowerMultiplier {
	value = bytes.TrimSpace(value)
	if string(value) == "[]" {
		return []skilldetail.WeatherPowerMultiplier{}
	}
	invalid := func() []skilldetail.WeatherPowerMultiplier {
		return []skilldetail.WeatherPowerMultiplier{{Weather: "__invalid_database_weather_power_multiplier__"}}
	}
	if len(value) == 0 || value[0] != '[' {
		return invalid()
	}
	var rawValues []jsontext.Value
	if err := jsonv2.Unmarshal(value, &rawValues); err != nil || rawValues == nil {
		return invalid()
	}
	result := make([]skilldetail.WeatherPowerMultiplier, 0, len(rawValues))
	for _, raw := range rawValues {
		multiplier, ok := weatherPowerMultiplierFromJSON(raw)
		if !ok {
			return invalid()
		}
		result = append(result, multiplier)
	}
	return result
}

// weatherPowerMultiplierFromJSON 严格读取一条天气威力倍率对象。
//
// encoding/json/v2 会拒绝重复键；指针字段让缺失的天气、分子或分母与显式零值一样不能绕过资料契约。范围、
// 重复天气和十倍上限仍由领域及 Battle 编译边界复核，以保持所有入口的失败语义一致。
func weatherPowerMultiplierFromJSON(value jsontext.Value) (skilldetail.WeatherPowerMultiplier, bool) {
	var payload struct {
		Weather     *skilldetail.WeatherKind `json:"weather"`
		Numerator   *int32                   `json:"numerator"`
		Denominator *int32                   `json:"denominator"`
	}
	if err := jsonv2.Unmarshal(value, &payload, jsonv2.RejectUnknownMembers(true)); err != nil ||
		payload.Weather == nil || payload.Numerator == nil || payload.Denominator == nil {
		return skilldetail.WeatherPowerMultiplier{}, false
	}
	return skilldetail.WeatherPowerMultiplier{
		Weather: *payload.Weather, Numerator: *payload.Numerator, Denominator: *payload.Denominator,
	}, true
}

// chargeSkippedWeathersFromJSON 将数据库 JSONB 数组转换为天气跳过蓄力资料。
//
// 数组中的天气代码不在读取时“猜测”或过滤：未知天气、重复天气以及没有蓄力规则的交叉约束会由领域和 Battle
// 编译边界拒绝。此处只负责严格区分空数组与损坏 JSON 结构。
func chargeSkippedWeathersFromJSON(value []byte) []skilldetail.WeatherKind {
	value = bytes.TrimSpace(value)
	if string(value) == "[]" {
		return []skilldetail.WeatherKind{}
	}
	invalid := func() []skilldetail.WeatherKind {
		return []skilldetail.WeatherKind{"__invalid_database_charge_skipped_weather__"}
	}
	if len(value) == 0 || value[0] != '[' {
		return invalid()
	}
	var values []skilldetail.WeatherKind
	if err := jsonv2.Unmarshal(value, &values); err != nil || values == nil {
		return invalid()
	}
	return values
}

// volatileEffectsFromJSON 将数据库 jsonb 数组转回领域强类型值。jsonb 保证载荷语法有效，但仍可能被
// 非应用程序 SQL 写入为未知结构；此时返回一个无效哨兵值，使 Battle 编译边界明确拒绝该资料，绝不
// 静默把错误规则当作空效果执行。
func volatileEffectsFromJSON(value []byte) []skilldetail.VolatileEffect {
	if len(value) == 0 {
		return []skilldetail.VolatileEffect{}
	}
	var effects []skilldetail.VolatileEffect
	if err := json.Unmarshal(value, &effects); err != nil {
		return []skilldetail.VolatileEffect{{}}
	}
	return effects
}

// dynamicPowerFromJSON 将数据库 jsonb 对象转回领域强类型动态威力。未知字段、非对象载荷、语法错误或额外
// JSON 值都返回无效哨兵，让 Battle 初始状态编译明确失败，而不把损坏资料静默解释为“未启用”。
func dynamicPowerFromJSON(value []byte) skilldetail.DynamicPower {
	value = bytes.TrimSpace(value)
	if len(value) == 0 || value[0] != '{' {
		return skilldetail.DynamicPower{Kind: "__invalid_database_dynamic_power__"}
	}
	decoder := json.NewDecoder(bytes.NewReader(value))
	decoder.DisallowUnknownFields()
	var result skilldetail.DynamicPower
	if err := decoder.Decode(&result); err != nil {
		return skilldetail.DynamicPower{Kind: "__invalid_database_dynamic_power__"}
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return skilldetail.DynamicPower{Kind: "__invalid_database_dynamic_power__"}
	}
	return result
}

// fieldSpeedOrderFromJSON 将数据库 jsonb 对象转回可选的全场速度顺序资料。空对象是明确的“未配置”值；
// 非对象、未知字段、语法错误或额外 JSON 值都会变为无效哨兵，让 Battle 启动拒绝损坏资料，而不是把它误当成
// 没有全场效果。
func fieldSpeedOrderFromJSON(value []byte) *skilldetail.FieldSpeedOrder {
	value = bytes.TrimSpace(value)
	if string(value) == "{}" {
		return nil
	}
	invalid := func() *skilldetail.FieldSpeedOrder {
		return &skilldetail.FieldSpeedOrder{Kind: "__invalid_database_field_speed_order__"}
	}
	if len(value) == 0 || value[0] != '{' {
		return invalid()
	}
	decoder := json.NewDecoder(bytes.NewReader(value))
	decoder.DisallowUnknownFields()
	var result skilldetail.FieldSpeedOrder
	if err := decoder.Decode(&result); err != nil {
		return invalid()
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return invalid()
	}
	return &result
}

// leechSeedFromJSON 将数据库 jsonb 对象转回可选寄生种子资料。空对象是明确的未配置值；非对象、未知字段、
// 语法错误或拼接的额外 JSON 值都会转换为无效哨兵，使 Battle 编译边界拒绝损坏资料而不会把它误当成空规则。
func leechSeedFromJSON(value []byte) *skilldetail.LeechSeed {
	value = bytes.TrimSpace(value)
	if string(value) == "{}" {
		return nil
	}
	invalid := func() *skilldetail.LeechSeed {
		return &skilldetail.LeechSeed{}
	}
	if len(value) == 0 || value[0] != '{' {
		return invalid()
	}
	decoder := json.NewDecoder(bytes.NewReader(value))
	decoder.DisallowUnknownFields()
	var result skilldetail.LeechSeed
	if err := decoder.Decode(&result); err != nil {
		return invalid()
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return invalid()
	}
	return &result
}

// weatherFromJSON 将数据库 jsonb 对象转回可选普通天气资料。空对象表示未配置；非对象、未知字段、语法错误或
// 拼接的额外 JSON 值都会返回无效哨兵，确保 Battle 编译边界拒绝损坏资料，而不会将其静默解释为没有天气。
func weatherFromJSON(value []byte) *skilldetail.Weather {
	value = bytes.TrimSpace(value)
	if string(value) == "{}" {
		return nil
	}
	invalid := func() *skilldetail.Weather {
		return &skilldetail.Weather{Kind: "__invalid_database_weather__"}
	}
	if len(value) == 0 || value[0] != '{' {
		return invalid()
	}
	decoder := json.NewDecoder(bytes.NewReader(value))
	decoder.DisallowUnknownFields()
	var result skilldetail.Weather
	if err := decoder.Decode(&result); err != nil {
		return invalid()
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return invalid()
	}
	return &result
}

// terrainFromJSON 将数据库 jsonb 对象转回可选普通场地资料。空对象表示未配置；非对象、未知字段、语法错误或
// 拼接的额外 JSON 值都会返回无效哨兵，确保 Battle 编译边界拒绝损坏资料，而不会将其静默解释为没有场地。
func terrainFromJSON(value []byte) *skilldetail.Terrain {
	value = bytes.TrimSpace(value)
	if string(value) == "{}" {
		return nil
	}
	invalid := func() *skilldetail.Terrain {
		return &skilldetail.Terrain{Kind: "__invalid_database_terrain__"}
	}
	if len(value) == 0 || value[0] != '{' {
		return invalid()
	}
	decoder := json.NewDecoder(bytes.NewReader(value))
	decoder.DisallowUnknownFields()
	var result skilldetail.Terrain
	if err := decoder.Decode(&result); err != nil {
		return invalid()
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return invalid()
	}
	return &result
}

// tailwindFromJSON 将数据库 jsonb 对象转回可选顺风资料。空对象表示未配置；非对象、未知字段、语法错误或拼接的
// 额外 JSON 值都会返回无效哨兵，使 Battle 编译边界拒绝损坏资料，而不会把它错误解释为没有顺风。
func tailwindFromJSON(value []byte) *skilldetail.Tailwind {
	value = bytes.TrimSpace(value)
	if string(value) == "{}" {
		return nil
	}
	invalid := func() *skilldetail.Tailwind {
		return &skilldetail.Tailwind{TurnsRemaining: -1}
	}
	if len(value) == 0 || value[0] != '{' {
		return invalid()
	}
	decoder := json.NewDecoder(bytes.NewReader(value))
	decoder.DisallowUnknownFields()
	var result skilldetail.Tailwind
	if err := decoder.Decode(&result); err != nil {
		return invalid()
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return invalid()
	}
	return &result
}

// reflectFromJSON 将数据库 jsonb 对象转回可选反射壁资料。空对象表示未配置；非对象、未知字段、语法错误或拼接的
// 额外 JSON 值都会返回无效哨兵，使 Battle 编译边界拒绝损坏资料，而不会把它错误解释为没有反射壁。
func reflectFromJSON(value []byte) *skilldetail.Reflect {
	value = bytes.TrimSpace(value)
	if string(value) == "{}" {
		return nil
	}
	invalid := func() *skilldetail.Reflect {
		return &skilldetail.Reflect{TurnsRemaining: -1}
	}
	if len(value) == 0 || value[0] != '{' {
		return invalid()
	}
	decoder := json.NewDecoder(bytes.NewReader(value))
	decoder.DisallowUnknownFields()
	var result skilldetail.Reflect
	if err := decoder.Decode(&result); err != nil {
		return invalid()
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return invalid()
	}
	return &result
}

// lightScreenFromJSON 将数据库 jsonb 对象转回可选光墙资料。空对象表示未配置；非对象、未知字段、语法错误或
// 拼接的额外 JSON 值都会返回无效哨兵，使 Battle 编译边界拒绝损坏资料，而不会把它错误解释为没有光墙。
func lightScreenFromJSON(value []byte) *skilldetail.LightScreen {
	value = bytes.TrimSpace(value)
	if string(value) == "{}" {
		return nil
	}
	invalid := func() *skilldetail.LightScreen {
		return &skilldetail.LightScreen{TurnsRemaining: -1}
	}
	if len(value) == 0 || value[0] != '{' {
		return invalid()
	}
	decoder := json.NewDecoder(bytes.NewReader(value))
	decoder.DisallowUnknownFields()
	var result skilldetail.LightScreen
	if err := decoder.Decode(&result); err != nil {
		return invalid()
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return invalid()
	}
	return &result
}

// auroraVeilFromJSON 将数据库 jsonb 对象转回可选极光幕资料。空对象表示未配置；非对象、未知字段、语法错误或
// 拼接的额外 JSON 值都会返回无效哨兵，使 Battle 编译边界拒绝损坏资料，而不会把它错误解释为没有极光幕。
func auroraVeilFromJSON(value []byte) *skilldetail.AuroraVeil {
	value = bytes.TrimSpace(value)
	if string(value) == "{}" {
		return nil
	}
	invalid := func() *skilldetail.AuroraVeil {
		return &skilldetail.AuroraVeil{TurnsRemaining: -1}
	}
	if len(value) == 0 || value[0] != '{' {
		return invalid()
	}
	decoder := json.NewDecoder(bytes.NewReader(value))
	decoder.DisallowUnknownFields()
	var result skilldetail.AuroraVeil
	if err := decoder.Decode(&result); err != nil {
		return invalid()
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return invalid()
	}
	return &result
}

// spikesFromJSON 将数据库 jsonb 对象转换为可选撒菱资料。空对象表示未配置；非对象、未知字段、语法错误或
// 拼接的额外 JSON 值都会返回无效哨兵，阻止 Battle 把损坏资料静默解释成没有入场危害。
func spikesFromJSON(value []byte) *skilldetail.Spikes {
	value = bytes.TrimSpace(value)
	if string(value) == "{}" {
		return nil
	}
	invalid := func() *skilldetail.Spikes {
		return &skilldetail.Spikes{ChancePercent: -1}
	}
	if len(value) == 0 || value[0] != '{' {
		return invalid()
	}
	decoder := json.NewDecoder(bytes.NewReader(value))
	decoder.DisallowUnknownFields()
	var result skilldetail.Spikes
	if err := decoder.Decode(&result); err != nil {
		return invalid()
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return invalid()
	}
	return &result
}

// stealthRockFromJSON 将数据库 jsonb 对象转换为可选隐形岩资料。任何损坏或多余字段都返回无效哨兵，避免
// 损坏资料被误当作未配置的隐形岩规则。
func stealthRockFromJSON(value []byte) *skilldetail.StealthRock {
	value = bytes.TrimSpace(value)
	if string(value) == "{}" {
		return nil
	}
	invalid := func() *skilldetail.StealthRock {
		return &skilldetail.StealthRock{ChancePercent: -1}
	}
	if len(value) == 0 || value[0] != '{' {
		return invalid()
	}
	decoder := json.NewDecoder(bytes.NewReader(value))
	decoder.DisallowUnknownFields()
	var result skilldetail.StealthRock
	if err := decoder.Decode(&result); err != nil {
		return invalid()
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return invalid()
	}
	return &result
}

// toxicSpikesFromJSON 将数据库 jsonb 对象转换为可选毒菱资料。任何损坏或多余字段都返回无效哨兵，让启动
// 编译边界明确拒绝资料，而不错误移除毒菱建立规则。
func toxicSpikesFromJSON(value []byte) *skilldetail.ToxicSpikes {
	value = bytes.TrimSpace(value)
	if string(value) == "{}" {
		return nil
	}
	invalid := func() *skilldetail.ToxicSpikes {
		return &skilldetail.ToxicSpikes{ChancePercent: -1}
	}
	if len(value) == 0 || value[0] != '{' {
		return invalid()
	}
	decoder := json.NewDecoder(bytes.NewReader(value))
	decoder.DisallowUnknownFields()
	var result skilldetail.ToxicSpikes
	if err := decoder.Decode(&result); err != nil {
		return invalid()
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return invalid()
	}
	return &result
}

// stickyWebFromJSON 将数据库 jsonb 对象转换为可选黏黏网资料。任何损坏或多余字段都返回无效哨兵，避免
// 将未知规则静默降级为未配置。
func stickyWebFromJSON(value []byte) *skilldetail.StickyWeb {
	value = bytes.TrimSpace(value)
	if string(value) == "{}" {
		return nil
	}
	invalid := func() *skilldetail.StickyWeb {
		return &skilldetail.StickyWeb{ChancePercent: -1}
	}
	if len(value) == 0 || value[0] != '{' {
		return invalid()
	}
	decoder := json.NewDecoder(bytes.NewReader(value))
	decoder.DisallowUnknownFields()
	var result skilldetail.StickyWeb
	if err := decoder.Decode(&result); err != nil {
		return invalid()
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return invalid()
	}
	return &result
}

// rapidSpinFromJSON 将数据库 jsonb 对象转换为可选快速旋转资料。启用规则必须携带 enabled=true；无效哨兵会
// 在资料服务与 Battle 编译边界被拒绝，避免把损坏对象当成无规则。
func rapidSpinFromJSON(value []byte) *skilldetail.RapidSpin {
	value = bytes.TrimSpace(value)
	if string(value) == "{}" {
		return nil
	}
	invalid := func() *skilldetail.RapidSpin {
		return &skilldetail.RapidSpin{}
	}
	if len(value) == 0 || value[0] != '{' {
		return invalid()
	}
	decoder := json.NewDecoder(bytes.NewReader(value))
	decoder.DisallowUnknownFields()
	var result skilldetail.RapidSpin
	if err := decoder.Decode(&result); err != nil {
		return invalid()
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return invalid()
	}
	return &result
}

// defogFromJSON 将数据库 jsonb 对象转换为可选清除浓雾资料。启用规则必须携带 enabled=true；无效哨兵会被
// 资料服务与 Battle 编译边界拒绝，不会静默失去固定清场后效。
func defogFromJSON(value []byte) *skilldetail.Defog {
	value = bytes.TrimSpace(value)
	if string(value) == "{}" {
		return nil
	}
	invalid := func() *skilldetail.Defog {
		return &skilldetail.Defog{}
	}
	if len(value) == 0 || value[0] != '{' {
		return invalid()
	}
	decoder := json.NewDecoder(bytes.NewReader(value))
	decoder.DisallowUnknownFields()
	var result skilldetail.Defog
	if err := decoder.Decode(&result); err != nil {
		return invalid()
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return invalid()
	}
	return &result
}

func abilityReactiveAbilityRulesFromJSON(encoded []byte) *battleengine.ReactiveAbilityRules {
	var value *battleengine.ReactiveAbilityRules
	if err := jsonv2.Unmarshal(encoded, &value, jsonv2.RejectUnknownMembers(true), jsontext.AllowDuplicateNames(false)); err != nil {
		return &battleengine.ReactiveAbilityRules{ReceivedDamageCharge: &battleengine.ReceivedDamageCharge{}}
	}
	return value
}

// abilityDamageMultiplierFromJSON 从独立 JSONB 列恢复强类型伤害倍率规则。
func abilityDamageMultiplierFromJSON[T any](encoded []byte) *T {
	var value *T
	if err := jsonv2.Unmarshal(encoded, &value, jsonv2.RejectUnknownMembers(true), jsontext.AllowDuplicateNames(false)); err != nil {
		return new(T)
	}
	return value
}

// abilityDamageMultiplierFromJSON 从一条独立 JSONB 列严格恢复对应的攻击型特性领域规则。
//
// JSON null 唯一表示没有规则；未知字段、重复字段和损坏 JSON 会返回非 nil 零值，使后续领域或 Battle 校验明确
// 拒绝旁路写入的坏数据，不能悄悄降级为“没有特性效果”。

func abilityAccuracyMultiplierFromValues(numerator, denominator int32) *abilitydetail.AccuracyMultiplier {
	if numerator == 1 && denominator == 1 {
		return nil
	}
	return &abilitydetail.AccuracyMultiplier{Numerator: numerator, Denominator: denominator}
}

func abilityOpponentSwitchRestrictionFromValues(enabled bool, requiredTargetElementID pgtype.Int8, requiresGroundedTarget, sameEffectGrantsImmunity bool) *abilitydetail.OpponentSwitchRestriction {
	if !enabled {
		if !requiredTargetElementID.Valid && !requiresGroundedTarget && !sameEffectGrantsImmunity {
			return nil
		}
		invalidTargetElementID := snowflake.ID(0)
		return &abilitydetail.OpponentSwitchRestriction{
			RequiredTargetElementID:  &invalidTargetElementID,
			RequiresGroundedTarget:   requiresGroundedTarget,
			SameEffectGrantsImmunity: sameEffectGrantsImmunity,
		}
	}
	value := &abilitydetail.OpponentSwitchRestriction{
		RequiresGroundedTarget:   requiresGroundedTarget,
		SameEffectGrantsImmunity: sameEffectGrantsImmunity,
	}
	if requiredTargetElementID.Valid {
		targetElementID := domainIdentifier(requiredTargetElementID)
		value.RequiredTargetElementID = &targetElementID
	}
	return value
}

// abilityEnvironmentHighestStatMultiplierFromValues 将数据库的互斥环境条件重组成独立领域规则。
//
// 两列均为 NULL 表示未声明；同时出现或空文本等绕过数据库约束的组合会保留为无效哨兵，随后由领域校验和
// Battle 编译边界拒绝，绝不悄悄降级为无强化规则。

func abilityEnvironmentHighestStatMultiplierFromValues(weather, terrain pgtype.Text) *abilitydetail.EnvironmentHighestStatMultiplier {
	if !weather.Valid && !terrain.Valid {
		return nil
	}
	if weather.Valid && !terrain.Valid {
		value := abilitydetail.WeatherKind(weather.String)
		return &abilitydetail.EnvironmentHighestStatMultiplier{RequiredWeather: &value}
	}
	if !weather.Valid && terrain.Valid {
		value := abilitydetail.TerrainKind(terrain.String)
		return &abilitydetail.EnvironmentHighestStatMultiplier{RequiredTerrain: &value}
	}
	invalidWeather := abilitydetail.WeatherKind("__invalid_database_environment_highest_stat_multiplier_weather__")
	invalidTerrain := abilitydetail.TerrainKind("__invalid_database_environment_highest_stat_multiplier_terrain__")
	return &abilitydetail.EnvironmentHighestStatMultiplier{RequiredWeather: &invalidWeather, RequiredTerrain: &invalidTerrain}
}

// abilitySwitchInFormChangeFromValues 将数据库的三列入场形态切换规则重组成领域值。
//
// 只有两个 Identifier 均为 NULL 且开关为 false 才表示未声明。其它残缺组合保留无效 Identifier，由领域服务与 Battle
// 编译边界拒绝，而不是静默把损坏资料降级为没有形态规则。

func abilitySwitchInFormChangeFromValues(baseCreatureID, alternateCreatureID pgtype.Int8, addsMaximumHPDifference bool) *abilitydetail.SwitchInFormChange {
	if !baseCreatureID.Valid && !alternateCreatureID.Valid && !addsMaximumHPDifference {
		return nil
	}
	return &abilitydetail.SwitchInFormChange{
		BaseCreatureID: domainIdentifier(baseCreatureID), AlternateCreatureID: domainIdentifier(alternateCreatureID),
		AddsMaximumHPDifference: addsMaximumHPDifference,
	}
}

// abilitySwitchOutFormChangeFromValues 将数据库的两个离场形态 Identifier 重组成独立领域规则。
//
// 两列同时为 NULL 才表示未声明；任何半条资料都会以无效 Identifier 保留到领域校验和 Battle 编译边界，避免数据库外
// 写入把本应失败的形态切换悄悄降级为没有规则。

func abilitySwitchOutFormChangeFromValues(baseCreatureID, alternateCreatureID pgtype.Int8) *abilitydetail.SwitchOutFormChange {
	if !baseCreatureID.Valid && !alternateCreatureID.Valid {
		return nil
	}
	return &abilitydetail.SwitchOutFormChange{
		BaseCreatureID: domainIdentifier(baseCreatureID), AlternateCreatureID: domainIdentifier(alternateCreatureID),
	}
}

// abilityWeatherFormChangeFromValues 将数据库的默认形态与 JSONB 天气映射重组成领域值。
//
// 空默认 Identifier 与空数组共同表示未声明；任何 JSON 解析失败或不完整组合都保留为领域校验可拒绝的无效值，
// 避免旁路 SQL 写入悄然丢失天气形态效果。

func abilityWeatherFormChangeFromValues(defaultCreatureID pgtype.Int8, targets []byte) *abilitydetail.WeatherFormChange {
	decoded, err := abilityWeatherFormTargetsFromJSON(targets)
	if !defaultCreatureID.Valid && err == nil && len(decoded) == 0 {
		return nil
	}
	if err != nil {
		return &abilitydetail.WeatherFormChange{}
	}
	return &abilitydetail.WeatherFormChange{DefaultCreatureID: domainIdentifier(defaultCreatureID), Targets: decoded}
}

// abilityWeatherFormTargetJSON 是 JSONB 存储层的稳定天气形态映射载荷。
//
// 它独立于 API/Proto 字段名，确保数据库编码在 Go 结构字段重命名后仍可严格读取；所有用户可见映射均由
// abilitydetail.WeatherFormChange 公开，不能直接依赖此持久化细节。
type abilityWeatherFormTargetJSON struct {
	// Weather 是目标形态适用的封闭普通天气。
	Weather abilitydetail.WeatherKind `json:"weather"`
	// CreatureID 是目标形态稳定 Identifier。
	CreatureID snowflake.ID `json:"creatureId"`
}

// abilityWeatherFormTargetsJSON 将领域天气形态映射编码为严格 JSONB 数组。

func abilityWeatherFormTargetsJSON(values []abilitydetail.WeatherFormTarget) ([]byte, error) {
	encoded := make([]abilityWeatherFormTargetJSON, len(values))
	for index, value := range values {
		encoded[index] = abilityWeatherFormTargetJSON{Weather: value.Weather, CreatureID: value.CreatureID}
	}
	return jsonv2.Marshal(encoded)
}

// abilityWeatherFormTargetsFromJSON 严格读取 JSONB 天气形态映射。

func abilityWeatherFormTargetsFromJSON(raw []byte) ([]abilitydetail.WeatherFormTarget, error) {
	if len(raw) == 0 {
		return nil, io.ErrUnexpectedEOF
	}
	decoder := json.NewDecoder(bytes.NewReader(raw))
	decoder.DisallowUnknownFields()
	var encoded []abilityWeatherFormTargetJSON
	if err := decoder.Decode(&encoded); err != nil {
		return nil, err
	}
	var trailing any
	if err := decoder.Decode(&trailing); err != io.EOF {
		return nil, io.ErrUnexpectedEOF
	}
	values := make([]abilitydetail.WeatherFormTarget, len(encoded))
	for index, value := range encoded {
		values[index] = abilitydetail.WeatherFormTarget{Weather: value.Weather, CreatureID: value.CreatureID}
	}
	return values, nil
}

// abilitySwitchInStrongWeatherFromValue 将可空数据库枚举转换为独立入场强天气资料。
//
// 未声明列保持 nil；数据库中绕过约束写入的未知文本转换为封闭枚举外的值，随后由领域校验和 Battle
// 编译边界明确拒绝，绝不静默忽略强天气规则。

func abilitySwitchInStrongWeatherFromValue(value pgtype.Text) *abilitydetail.SwitchInStrongWeather {
	if !value.Valid {
		return nil
	}
	return &abilitydetail.SwitchInStrongWeather{Weather: abilitydetail.StrongWeatherKind(value.String)}
}

// abilitySwitchInWeatherFromValues 将数据库的可空普通天气与持续回合双字段重组成独立资料规则。
//
// 只有 NULL 与 0 共同表示没有规则；其它不完整或绕过约束的组合都会留下封闭枚举外的哨兵，由领域校验和
// Battle 编译边界阻止新对局，绝不静默降级为没有入场天气。

func abilitySwitchInWeatherFromValues(weather pgtype.Text, turnsRemaining int32) *abilitydetail.SwitchInWeather {
	if !weather.Valid && turnsRemaining == 0 {
		return nil
	}
	if !weather.Valid {
		return &abilitydetail.SwitchInWeather{Weather: "__invalid_database_ability_switch_in_weather__", TurnsRemaining: turnsRemaining}
	}
	return &abilitydetail.SwitchInWeather{Weather: abilitydetail.WeatherKind(weather.String), TurnsRemaining: turnsRemaining}
}

// abilitySwitchInTerrainFromValues 将数据库的可空普通场地与持续回合双字段重组成独立资料规则。
//
// 只有 NULL 与 0 共同表示没有规则；其它不完整或绕过约束的组合都会留下封闭枚举外的哨兵，由领域校验和
// Battle 编译边界阻止新对局，绝不静默降级为没有入场场地。

func abilitySwitchInTerrainFromValues(terrain pgtype.Text, turnsRemaining int32) *abilitydetail.SwitchInTerrain {
	if !terrain.Valid && turnsRemaining == 0 {
		return nil
	}
	if !terrain.Valid {
		return &abilitydetail.SwitchInTerrain{Terrain: "__invalid_database_ability_switch_in_terrain__", TurnsRemaining: turnsRemaining}
	}
	return &abilitydetail.SwitchInTerrain{Terrain: abilitydetail.TerrainKind(terrain.String), TurnsRemaining: turnsRemaining}
}

// abilitySwitchInStatStageFromValues 将数据库的入场能力阶级变化三字段重组成独立资料规则。
//
// 只有 NULL、NULL 与 0 共同表示没有规则；其它不完整组合都会留下封闭枚举外的目标哨兵，由领域校验和
// Battle 编译边界阻止新对局，绝不静默降级为没有入场能力阶级变化。

func abilitySwitchInStatStageFromValues(target pgtype.Text, statID pgtype.Int8, stageDelta int32) *abilitydetail.SwitchInStatStageChange {
	if !target.Valid && !statID.Valid && stageDelta == 0 {
		return nil
	}
	if !target.Valid || !statID.Valid {
		return &abilitydetail.SwitchInStatStageChange{
			Target: "__invalid_database_ability_switch_in_stat_stage__", StageDelta: stageDelta,
		}
	}
	return &abilitydetail.SwitchInStatStageChange{
		Target: abilitydetail.SwitchInStatStageTarget(target.String), StatID: domainIdentifier(statID), StageDelta: stageDelta,
	}
}

// abilityTerastallizationStatStageFromValues 将数据库的太晶化能力阶级变化双字段重组成独立资料规则。
//
// 只有 NULL 与 0 共同表示没有规则；其它不完整组合都会保留无效 Identifier，由领域校验和 Battle 编译边界阻止新对局，
// 绝不静默降级为没有太晶化能力阶级变化。

func abilityTerastallizationStatStageFromValues(statID pgtype.Int8, stageDelta int32) *abilitydetail.TerastallizationStatStageChange {
	if !statID.Valid && stageDelta == 0 {
		return nil
	}
	return &abilitydetail.TerastallizationStatStageChange{StatID: domainIdentifier(statID), StageDelta: stageDelta}
}

// abilitySwitchInAllyHealFromValue 将数据库的入场同侧回复分母转换为独立资料规则。
//
// 0 是未声明规则；负数或超过允许范围的数据库值会保留为无效哨兵，由领域校验和 Battle 编译边界明确阻止，
// 不能静默转为不回复。

func abilitySwitchInAllyHealFromValue(denominator int32) *abilitydetail.SwitchInAllyHeal {
	if denominator == 0 {
		return nil
	}
	return &abilitydetail.SwitchInAllyHeal{HealDenominator: denominator}
}

// abilityWeatherDamageImmunitiesFromJSON 将特性详情的 JSONB 天气免疫数组转换为强类型资料。
//
// 读侧绝不静默丢弃损坏数据库值：非数组、未知 JSON 结构、重复键或 null 都转换为封闭枚举之外的哨兵，
// 之后由领域服务和 Battle 编译边界明确拒绝，避免一条被应用外 SQL 损坏的特性悄然失去战斗效果。

func abilityWeatherDamageImmunitiesFromJSON(value []byte) []abilitydetail.WeatherKind {
	value = bytes.TrimSpace(value)
	if string(value) == "[]" {
		return []abilitydetail.WeatherKind{}
	}
	invalid := func() []abilitydetail.WeatherKind {
		return []abilitydetail.WeatherKind{"__invalid_database_ability_weather_damage_immunity__"}
	}
	if len(value) == 0 || value[0] != '[' {
		return invalid()
	}
	var values []abilitydetail.WeatherKind
	if err := jsonv2.Unmarshal(value, &values); err != nil || values == nil {
		return invalid()
	}
	return values
}

// abilityWeatherEndTurnHealFromValues 将特性天气回合末回复的两个持久化字段重组为强类型资料。
//
// 只有 [] 与 0 的组合表示没有规则。其它任何不完整、非数组、null 或语法损坏的组合都会产生无效天气哨兵，
// 交由领域校验和 Battle 编译明确拒绝，不能降级成“没有效果”。

func abilityWeatherEndTurnHealFromValues(weathers []byte, denominator int32) *abilitydetail.WeatherEndTurnHeal {
	weathers = bytes.TrimSpace(weathers)
	if string(weathers) == "[]" && denominator == 0 {
		return nil
	}
	invalid := func() *abilitydetail.WeatherEndTurnHeal {
		return &abilitydetail.WeatherEndTurnHeal{
			Weathers:        []abilitydetail.WeatherKind{"__invalid_database_ability_weather_end_turn_heal__"},
			HealDenominator: denominator,
		}
	}
	if denominator < 1 || len(weathers) == 0 || weathers[0] != '[' {
		return invalid()
	}
	var values []abilitydetail.WeatherKind
	if err := jsonv2.Unmarshal(weathers, &values); err != nil || len(values) == 0 {
		return invalid()
	}
	return &abilitydetail.WeatherEndTurnHeal{Weathers: values, HealDenominator: denominator}
}

// abilityWeatherSpeedMultipliersFromJSON 将特性天气速度倍率 JSONB 数组转换为强类型资料。
//
// 非数组、未知字段、重复键、缺失分数或尾随载荷都会留下无效哨兵，交由领域和 Battle 拒绝；读取时绝不以浮点
// 默认值、补充分母或过滤重复天气悄然改变行动顺序。

func abilityWeatherSpeedMultipliersFromJSON(value []byte) []abilitydetail.WeatherSpeedMultiplier {
	value = bytes.TrimSpace(value)
	if string(value) == "[]" {
		return []abilitydetail.WeatherSpeedMultiplier{}
	}
	invalid := func() []abilitydetail.WeatherSpeedMultiplier {
		return []abilitydetail.WeatherSpeedMultiplier{{Weather: "__invalid_database_ability_weather_speed_multiplier__"}}
	}
	if len(value) == 0 || value[0] != '[' {
		return invalid()
	}
	var rawValues []jsontext.Value
	if err := jsonv2.Unmarshal(value, &rawValues); err != nil || rawValues == nil {
		return invalid()
	}
	result := make([]abilitydetail.WeatherSpeedMultiplier, 0, len(rawValues))
	for _, raw := range rawValues {
		var payload struct {
			Weather     *abilitydetail.WeatherKind `json:"weather"`
			Numerator   *int32                     `json:"numerator"`
			Denominator *int32                     `json:"denominator"`
		}
		if err := jsonv2.Unmarshal(raw, &payload, jsonv2.RejectUnknownMembers(true), jsontext.AllowDuplicateNames(false)); err != nil ||
			payload.Weather == nil || payload.Numerator == nil || payload.Denominator == nil {
			return invalid()
		}
		result = append(result, abilitydetail.WeatherSpeedMultiplier{
			Weather: *payload.Weather, Numerator: *payload.Numerator, Denominator: *payload.Denominator,
		})
	}
	return result
}

func itemCategoryFromValues(id pgtype.Int8, code, name string, sortOrder int32, enabled bool, version int64) itemcategory.Category {
	return itemcategory.Category{
		ID: domainIdentifier(id), Code: code, Name: name, SortOrder: sortOrder, Enabled: enabled, Version: version,
	}
}

func itemFromValues(
	id pgtype.Int8,
	code string,
	name string,
	usageType string,
	categoryID pgtype.Int8,
	cost int32,
	flingPower pgtype.Int4,
	enabled bool,
	version int64,
	assetID pgtype.Int8,
	description *string,
	effect *string,
	shortEffect *string,
	flingEffectID pgtype.Int8,
) item.Item {
	return item.Item{
		ID: domainIdentifier(id), Code: code, Name: name, UsageType: item.UsageType(usageType),
		CategoryID: nullableDomainIdentifier(categoryID), Cost: cost, FlingPower: nullableDomainInt32(flingPower),
		Enabled: enabled, Version: version, AssetID: nullableDomainIdentifier(assetID), Description: description, Effect: effect, ShortEffect: shortEffect, FlingEffectID: nullableDomainIdentifier(flingEffectID),
	}
}

func nullableText(value *string) *string {
	if value == nil {
		return nil
	}
	copy := *value
	return &copy
}

func statFromValues(
	id pgtype.Int8,
	code string,
	name string,
	sortOrder int32,
	battleOnly bool,
	enabled bool,
	version int64,
) stat.Stat {
	return stat.Stat{
		ID: domainIdentifier(id), Code: code, Name: name, SortOrder: sortOrder,
		BattleOnly: battleOnly, Enabled: enabled, Version: version,
	}
}

func skillDamageClassFromValues(
	id pgtype.Int8,
	code string,
	name string,
	description pgtype.Text,
	sortOrder int32,
	enabled bool,
	version int64,
) skilldamageclass.DamageClass {
	return skilldamageclass.DamageClass{
		ID: domainIdentifier(id), Code: code, Name: name, Description: nullableDomainText(description),
		SortOrder: sortOrder, Enabled: enabled, Version: version,
	}
}

func skillFromValues(
	id pgtype.Int8,
	code string,
	name string,
	elementID pgtype.Int8,
	damageClassID pgtype.Int8,
	accuracy pgtype.Int4,
	power pgtype.Int4,
	pp pgtype.Int4,
	priority int32,
	effectChance pgtype.Int4,
	enabled bool,
	version int64,
) skill.Skill {
	return skill.Skill{
		ID: domainIdentifier(id), Code: code, Name: name, Priority: priority, Enabled: enabled, Version: version,
		OptionalValues: skill.OptionalValues{
			ElementID: nullableDomainIdentifier(elementID), DamageClassID: nullableDomainIdentifier(damageClassID),
			Accuracy: nullableDomainInt32(accuracy), Power: nullableDomainInt32(power),
			PP: nullableDomainInt32(pp), EffectChance: nullableDomainInt32(effectChance),
		},
	}
}

func skillFromEnt(row *avalonent.GameSkill) (skill.Skill, error) {
	rules, err := battlerules.ParseSkill(row.Rules)
	if err != nil {
		return skill.Skill{}, err
	}
	return skill.Skill{
		ID: domainIdentifier(pgIdentifier(row.ID)), Code: row.Code, Name: row.Name, Priority: row.Priority,
		Rules: rules, Enabled: row.Enabled, Version: row.Version,
		OptionalValues: skill.OptionalValues{
			ElementID:     nullableDomainIdentifier(pgIdentifierPointer(row.ElementID)),
			DamageClassID: nullableDomainIdentifier(pgIdentifierPointer(row.DamageClassID)),
			Accuracy:      nullableDomainInt32(databaseInt32(row.Accuracy)), Power: nullableDomainInt32(databaseInt32(row.Power)),
			PP: nullableDomainInt32(databaseInt32(row.Pp)), EffectChance: nullableDomainInt32(databaseInt32(row.EffectChance)),
			Effect: row.Effect, ShortEffect: row.ShortEffect, Description: row.Description,
		},
	}, nil
}

func skillAilmentFromValues(
	id pgtype.Int8,
	code string,
	name string,
	enabled bool,
	version int64,
) skillailment.Ailment {
	return skillailment.Ailment{
		ID: domainIdentifier(id), Code: code, Name: name, Enabled: enabled, Version: version,
	}
}

func skillCategoryFromValues(
	id pgtype.Int8,
	code string,
	name string,
	description pgtype.Text,
	enabled bool,
	version int64,
) skillcategory.Category {
	return skillcategory.Category{
		ID: domainIdentifier(id), Code: code, Name: name, Description: nullableDomainText(description),
		Enabled: enabled, Version: version,
	}
}

func skillTargetFromValues(
	id pgtype.Int8,
	code string,
	name string,
	description pgtype.Text,
	enabled bool,
	version int64,
) skilltarget.Target {
	return skilltarget.Target{
		ID: domainIdentifier(id), Code: code, Name: name, Description: nullableDomainText(description),
		Enabled: enabled, Version: version,
	}
}

func skillLearnMethodFromValues(
	id pgtype.Int8,
	code string,
	name string,
	description pgtype.Text,
	enabled bool,
	version int64,
) skilllearnmethod.Method {
	return skilllearnmethod.Method{
		ID: domainIdentifier(id), Code: code, Name: name, Description: nullableDomainText(description),
		Enabled: enabled, Version: version,
	}
}
