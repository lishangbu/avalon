package api_test

import (
	"context"
	"io"
	"log/slog"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	gameapi "github.com/lishangbu/avalon/internal/gamedata/api"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// TestKratosServiceReturnsRuleJSONSchemas 验证管理端取得的 Schema 与当前持久化规则文档层级一致。
func TestKratosServiceReturnsRuleJSONSchemas(t *testing.T) {
	t.Parallel()

	service := gameapi.NewKratosService(gameapi.NativeServices{}, slog.New(slog.NewTextHandler(io.Discard, nil)))
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{
		AccountID: snowflake.MustParse("1048576125"),
	})
	response, err := service.GetGameDataRuleJSONSchemas(ctx, &domainv1.GetGameDataRuleJSONSchemasRequest{})
	if err != nil {
		t.Fatalf("GetGameDataRuleJSONSchemas() error = %v", err)
	}
	if response.GetBody().GetSkillSchema() == nil || response.GetBody().GetAbilitySchema() == nil {
		t.Fatal("规则 Schema 必须同时提供结构化字段")
	}
	if response.GetBody().GetSkillSchema().GetFields()["properties"] == nil ||
		response.GetBody().GetAbilitySchema().GetFields()["properties"] == nil {
		t.Fatal("结构化规则 Schema 缺少 properties")
	}
	assertRuleSchemaProperty(t, response.GetBody().GetSkillSchema().AsMap(), "onUse")
	assertRuleSchemaProperty(t, response.GetBody().GetAbilitySchema().AsMap(), "passive")
	assertSchemaExcludesDisplayCopy(t, response.GetBody().GetSkillSchema().AsMap())
}

func assertRuleSchemaProperty(t *testing.T, schema map[string]any, property string) {
	t.Helper()
	if schema["$schema"] != "http://json-schema.org/draft-07/schema#" {
		t.Fatalf("Schema 版本 = %#v", schema["$schema"])
	}
	if schema["$defs"] != nil || schema["definitions"] == nil {
		t.Fatalf("Draft-07 Schema 必须使用 definitions: %#v", schema)
	}
	properties, ok := schema["properties"].(map[string]any)
	if !ok || properties[property] == nil {
		t.Fatalf("Schema 未提供 %q 属性: %#v", property, schema["properties"])
	}
}

func assertSchemaExcludesDisplayCopy(t *testing.T, payload map[string]any) {
	t.Helper()
	if containsJSONKey(payload, "effect") || containsJSONKey(payload, "shortEffect") ||
		containsJSONKey(payload, "description") || containsJSONKey(payload, "introduction") {
		t.Fatalf("Schema 不应提示主表展示字段: %#v", payload)
	}
}

func containsJSONKey(value any, key string) bool {
	switch typed := value.(type) {
	case map[string]any:
		for currentKey, currentValue := range typed {
			if currentKey == key || containsJSONKey(currentValue, key) {
				return true
			}
		}
	case []any:
		for _, item := range typed {
			if containsJSONKey(item, key) {
				return true
			}
		}
	}
	return false
}
