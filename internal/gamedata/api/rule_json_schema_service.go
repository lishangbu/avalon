package api

import (
	"context"
	"encoding/json"
	"fmt"
	"reflect"
	"strings"

	"google.golang.org/protobuf/types/known/structpb"

	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/battlerules"
)

// GetGameDataRuleJSONSchemas 返回当前服务端规则文档的 JSON Schema。
//
// Schema 直接从持久化规则文档及其嵌入规则值的 json 标签生成，避免管理端复制一份容易漂移的字段清单。
// 它只描述编辑器可即时判断的 JSON 形状；跨字段约束仍由保存边界的规则编译器负责。
func (service *KratosService) GetGameDataRuleJSONSchemas(
	ctx context.Context,
	_ *domainv1.GetGameDataRuleJSONSchemasRequest,
) (*domainv1.GetGameDataRuleJSONSchemasResponse, error) {
	if _, err := gameDataPrincipal(ctx); err != nil {
		return nil, err
	}
	skillSchema := ruleJSONSchema(battlerules.Skill{})
	abilitySchema := ruleJSONSchema(battlerules.Ability{})
	skillSchemaObject, err := ruleJSONSchemaStruct(skillSchema)
	if err != nil {
		return nil, fmt.Errorf("编码技能规则结构化 Schema: %w", err)
	}
	abilitySchemaObject, err := ruleJSONSchemaStruct(abilitySchema)
	if err != nil {
		return nil, fmt.Errorf("编码特性规则结构化 Schema: %w", err)
	}
	return &domainv1.GetGameDataRuleJSONSchemasResponse{
		Body: &domainv1.GameDataRuleJSONSchemas{SkillSchema: skillSchemaObject, AbilitySchema: abilitySchemaObject},
	}, nil
}

// ruleJSONSchema 从实际持久化规则类型构造 Draft-07 JSON Schema。
func ruleJSONSchema(value any) map[string]any {
	builder := newRuleSchemaBuilder()
	return builder.rootSchema(reflect.TypeOf(value))
}

// ruleJSONSchemaStruct 通过 JSON 标准化反射生成的嵌套 map，满足 protobuf Struct 的通用对象边界。
func ruleJSONSchemaStruct(schema map[string]any) (*structpb.Struct, error) {
	payload, err := json.Marshal(schema)
	if err != nil {
		return nil, err
	}
	var normalized map[string]any
	if err := json.Unmarshal(payload, &normalized); err != nil {
		return nil, err
	}
	return structpb.NewStruct(normalized)
}

// ruleSchemaBuilder 维护规则类型到 JSON Schema 定义的稳定映射。
type ruleSchemaBuilder struct {
	definitions map[string]map[string]any
	building    map[reflect.Type]string
}

func newRuleSchemaBuilder() *ruleSchemaBuilder {
	return &ruleSchemaBuilder{definitions: map[string]map[string]any{}, building: map[reflect.Type]string{}}
}

func (builder *ruleSchemaBuilder) rootSchema(valueType reflect.Type) map[string]any {
	valueType = dereferenceRuleType(valueType)
	result := builder.objectSchema(valueType)
	result["$schema"] = "http://json-schema.org/draft-07/schema#"
	result["title"] = valueType.Name()
	if len(builder.definitions) != 0 {
		result["definitions"] = builder.definitions
	}
	return result
}

func (builder *ruleSchemaBuilder) schemaForType(valueType reflect.Type) map[string]any {
	valueType = dereferenceRuleType(valueType)
	if isRuleIdentifier(valueType) {
		return map[string]any{"type": "string", "format": "snowflake-identifier"}
	}
	switch valueType.Kind() {
	case reflect.Bool:
		return map[string]any{"type": "boolean"}
	case reflect.String:
		return map[string]any{"type": "string"}
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64,
		reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64:
		return map[string]any{"type": "integer"}
	case reflect.Float32, reflect.Float64:
		return map[string]any{"type": "number"}
	case reflect.Slice, reflect.Array:
		return map[string]any{"type": "array", "items": builder.schemaForType(valueType.Elem())}
	case reflect.Map:
		return map[string]any{"type": "object", "additionalProperties": builder.schemaForType(valueType.Elem())}
	case reflect.Struct:
		name := ruleSchemaDefinitionName(valueType)
		if _, exists := builder.definitions[name]; exists {
			return map[string]any{"$ref": "#/definitions/" + name}
		}
		if _, exists := builder.building[valueType]; exists {
			return map[string]any{"$ref": "#/definitions/" + name}
		}
		builder.building[valueType] = name
		definition := builder.objectSchema(valueType)
		definition["title"] = valueType.Name()
		builder.definitions[name] = definition
		delete(builder.building, valueType)
		return map[string]any{"$ref": "#/definitions/" + name}
	default:
		return map[string]any{}
	}
}

func (builder *ruleSchemaBuilder) objectSchema(valueType reflect.Type) map[string]any {
	properties := map[string]any{}
	for index := 0; index < valueType.NumField(); index++ {
		field := valueType.Field(index)
		if field.PkgPath != "" {
			continue
		}
		name, include := ruleJSONFieldName(field)
		if !include {
			continue
		}
		if field.Anonymous && name == "" {
			builder.addEmbeddedProperties(properties, field.Type)
			continue
		}
		if name == "" || isGameDataDisplayCopy(name) {
			continue
		}
		properties[name] = builder.schemaForType(field.Type)
	}
	return map[string]any{"type": "object", "properties": properties, "additionalProperties": false}
}

func (builder *ruleSchemaBuilder) addEmbeddedProperties(properties map[string]any, valueType reflect.Type) {
	valueType = dereferenceRuleType(valueType)
	if valueType.Kind() != reflect.Struct {
		return
	}
	for index := 0; index < valueType.NumField(); index++ {
		field := valueType.Field(index)
		if field.PkgPath != "" {
			continue
		}
		name, include := ruleJSONFieldName(field)
		if !include {
			continue
		}
		if field.Anonymous && name == "" {
			builder.addEmbeddedProperties(properties, field.Type)
			continue
		}
		if name == "" || isGameDataDisplayCopy(name) {
			continue
		}
		properties[name] = builder.schemaForType(field.Type)
	}
}

func ruleJSONFieldName(field reflect.StructField) (string, bool) {
	tag := field.Tag.Get("json")
	if tag == "-" {
		return "", false
	}
	name := strings.Split(tag, ",")[0]
	if name == "" && !field.Anonymous {
		return field.Name, true
	}
	return name, true
}

func dereferenceRuleType(valueType reflect.Type) reflect.Type {
	for valueType.Kind() == reflect.Pointer {
		valueType = valueType.Elem()
	}
	return valueType
}

func isRuleIdentifier(valueType reflect.Type) bool {
	return valueType.PkgPath() == "github.com/lishangbu/avalon/internal/platform/snowflake" && valueType.Name() == "Identifier"
}

func ruleSchemaDefinitionName(valueType reflect.Type) string {
	packagePath := valueType.PkgPath()
	if slash := strings.LastIndexByte(packagePath, '/'); slash >= 0 {
		packagePath = packagePath[slash+1:]
	}
	return packagePath + "." + valueType.Name()
}

func isGameDataDisplayCopy(name string) bool {
	return name == "effect" || name == "shortEffect" || name == "description" || name == "introduction"
}
