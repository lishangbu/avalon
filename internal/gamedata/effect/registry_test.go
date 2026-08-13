package effect_test

import (
	"encoding/json"
	"errors"
	"reflect"
	"testing"

	"github.com/lishangbu/avalon/internal/gamedata/effect"
)

func TestRegistryRejectsRegistrationWithoutHandlers(t *testing.T) {
	var validate func(struct{}) []effect.Issue
	var compile func(struct{}) (any, error)
	if _, err := effect.NewRegistry(effect.Register("battle.clause.invalid", 1, validate, compile)); !errors.Is(err, effect.ErrInvalidRegistration) {
		t.Fatalf("缺少处理函数应拒绝注册，实际错误为 %v", err)
	}
}

func TestRegistryRejectsNonObjectParameters(t *testing.T) {
	registry, err := effect.NewDefaultRegistry()
	if err != nil {
		t.Fatalf("创建默认注册表失败: %v", err)
	}
	for _, raw := range []string{"null", "[]", `"value"`, "1"} {
		_, issues := registry.Compile(effect.Definition{
			Kind: effect.KindUniqueHeldItemClause, SchemaVersion: 1, Parameters: json.RawMessage(raw),
		})
		if len(issues) != 1 || issues[0].Code != "effect_parameters_invalid" {
			t.Fatalf("参数 %s 应被稳定拒绝，实际问题为 %+v", raw, issues)
		}
	}
}

type levelNormalizationParameters struct {
	Level int32 `json:"level"`
}

func TestRegistryRejectsUnknownVersionWithoutSilentlyIgnoringDefinition(t *testing.T) {
	registry, err := effect.NewDefaultRegistry()
	if err != nil {
		t.Fatalf("创建默认注册表失败: %v", err)
	}

	_, issues := registry.Compile(effect.Definition{
		Kind: "battle.mechanic.level-normalization", SchemaVersion: 2,
		Parameters: json.RawMessage(`{"level":50}`),
	})
	if len(issues) != 1 || issues[0].Code != "effect_kind_version_unsupported" {
		t.Fatalf("未知版本应返回稳定发布问题，实际为 %+v", issues)
	}
}

func TestDefaultRegistryPublishesStableSupportedVersions(t *testing.T) {
	registry, err := effect.NewDefaultRegistry()
	if err != nil {
		t.Fatalf("创建默认注册表失败: %v", err)
	}
	want := []effect.Support{
		{Kind: "battle.clause.unique-held-item", SchemaVersion: 1},
		{Kind: "battle.clause.unique-species", SchemaVersion: 1},
		{Kind: "battle.mechanic.level-normalization", SchemaVersion: 1},
		{Kind: "battle.mechanic.terastallization", SchemaVersion: 1},
		{Kind: "battle.restriction.stable-code-list", SchemaVersion: 1},
	}
	if got := registry.Supported(); !reflect.DeepEqual(got, want) {
		t.Fatalf("Supported() = %+v, want %+v", got, want)
	}
}

func TestRegistryCompilesKnownEffectDefinition(t *testing.T) {
	registry, err := effect.NewRegistry(effect.Register(
		"battle.mechanic.level-normalization",
		1,
		func(parameters levelNormalizationParameters) []effect.Issue {
			if parameters.Level < 1 || parameters.Level > 100 {
				return []effect.Issue{{
					Code: "level_out_of_range", FieldPath: "/parameters/level", Message: "等级必须介于 1 到 100 之间",
				}}
			}
			return nil
		},
		func(parameters levelNormalizationParameters) (any, error) {
			return struct {
				Level int32 `json:"level"`
			}{Level: parameters.Level}, nil
		},
	))
	if err != nil {
		t.Fatalf("创建注册表失败: %v", err)
	}

	compiled, issues := registry.Compile(effect.Definition{
		Kind:          "battle.mechanic.level-normalization",
		SchemaVersion: 1,
		Parameters:    json.RawMessage(`{"level":50}`),
	})
	if len(issues) != 0 {
		t.Fatalf("合法效果不应产生问题: %+v", issues)
	}
	if compiled.Kind != "battle.mechanic.level-normalization" || compiled.SchemaVersion != 1 {
		t.Fatalf("编译结果没有保留稳定身份: %+v", compiled)
	}
	if string(compiled.Parameters) != `{"level":50}` {
		t.Fatalf("运行时参数应使用确定性 JSON，实际为 %s", compiled.Parameters)
	}
}
