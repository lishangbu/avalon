// Package effect 提供游戏资料效果定义的显式版本注册、校验和运行时编译能力。
package effect

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"sort"
	"strings"
)

var (
	// ErrInvalidRegistration 表示注册项缺少稳定身份或处理函数。
	ErrInvalidRegistration = errors.New("效果注册项无效")
	// ErrDuplicateRegistration 表示相同 kind/schemaVersion 被显式注册了多次。
	ErrDuplicateRegistration = errors.New("效果注册项重复")
)

// Definition 是游戏资料中由代码实现、由资料参数化的效果定义。
type Definition struct {
	// Kind 是代码注册表中效果实现的稳定标识。
	Kind string `json:"kind"`
	// SchemaVersion 是参数结构与运行语义的显式版本。
	SchemaVersion int32 `json:"schemaVersion"`
	// Parameters 是进入领域校验前的原始效果参数对象。
	Parameters json.RawMessage `json:"parameters"`
}

// CompiledDefinition 是由代码注册表规范化后可供战斗运行时直接消费的确定性效果定义。
type CompiledDefinition struct {
	// Kind 是代码注册表中效果实现的稳定标识。
	Kind string `json:"kind"`
	// SchemaVersion 是已完成编译的效果语义版本。
	SchemaVersion int32 `json:"schemaVersion"`
	// Parameters 是经过结构和领域校验后的规范化参数对象。
	Parameters json.RawMessage `json:"parameters"`
}

// Issue 是阻止效果定义用于当前实时游戏资料的稳定结构化问题。
type Issue struct {
	// Code 是客户端可稳定判断的问题类型。
	Code string `json:"code"`
	// FieldPath 是相对于效果定义的 JSON Pointer 字段路径。
	FieldPath string `json:"fieldPath"`
	// Message 是面向简体中文管理端的诊断说明。
	Message string `json:"message"`
}

// Registration 封装一个 kind/schemaVersion 的参数解码、领域校验和运行时编译实现。
//
// Registration 只能通过 Register 创建；应用启动时应在构造函数中显式传入全部注册项，
// 禁止依赖 init、反射扫描或运行时插件改变支持集合。
type Registration struct {
	// kind 是代码实现的稳定效果标识。
	kind string
	// schemaVersion 是该实现接受的参数结构与语义版本。
	schemaVersion int32
	// compile 完成严格解码、领域校验和运行时参数规范化。
	compile func(json.RawMessage) (json.RawMessage, []Issue)
}

// Register 为具体参数类型创建一个显式注册项。
//
// JSON 解码会拒绝未知字段、类型不匹配、空参数和尾随值；validate 负责范围与跨字段等
// 领域约束，compile 负责生成不包含内部索引的运行时参数。
func Register[T any](
	kind string,
	schemaVersion int32,
	validate func(T) []Issue,
	compile func(T) (any, error),
) Registration {
	if validate == nil || compile == nil {
		return Registration{kind: strings.TrimSpace(kind), schemaVersion: schemaVersion}
	}
	return Registration{
		kind: strings.TrimSpace(kind), schemaVersion: schemaVersion,
		compile: func(raw json.RawMessage) (json.RawMessage, []Issue) {
			trimmed := bytes.TrimSpace(raw)
			if len(trimmed) == 0 {
				return nil, invalidParametersIssue("参数不能为空")
			}
			// 所有效果参数都以对象承载，明确拒绝 null、数组和标量，避免零值被误当成合法配置。
			if trimmed[0] != '{' {
				return nil, invalidParametersIssue("参数必须是 JSON 对象")
			}
			var parameters T
			decoder := json.NewDecoder(bytes.NewReader(raw))
			decoder.DisallowUnknownFields()
			if err := decoder.Decode(&parameters); err != nil {
				return nil, invalidParametersIssue("参数结构无效")
			}
			var trailing any
			if err := decoder.Decode(&trailing); !errors.Is(err, io.EOF) {
				return nil, invalidParametersIssue("参数只能包含一个 JSON 值")
			}
			if issues := validate(parameters); len(issues) > 0 {
				return nil, issues
			}
			compiled, err := compile(parameters)
			if err != nil {
				return nil, []Issue{{
					Code: "effect_compile_failed", FieldPath: "/parameters", Message: "效果参数无法编译为运行时定义",
				}}
			}
			payload, err := json.Marshal(compiled)
			if err != nil {
				return nil, []Issue{{
					Code: "effect_compile_failed", FieldPath: "/parameters", Message: "效果参数无法编译为运行时定义",
				}}
			}
			return payload, nil
		},
	}
}

type registrationKey struct {
	// kind 是效果实现的稳定标识。
	kind string
	// schemaVersion 区分同一效果标识的不同参数与运行语义。
	schemaVersion int32
}

// Registry 保存进程在构建时明确支持的全部效果版本。
type Registry struct {
	registrations map[registrationKey]Registration
}

// Support 是代码注册表对外公开的稳定 kind/schemaVersion 组合。
type Support struct {
	Kind          string `json:"kind"`
	SchemaVersion int32  `json:"schemaVersion"`
}

// NewRegistry 创建不可变使用的显式效果注册表，并拒绝无效或重复注册项。
func NewRegistry(registrations ...Registration) (*Registry, error) {
	registry := &Registry{registrations: make(map[registrationKey]Registration, len(registrations))}
	for _, registration := range registrations {
		if registration.kind == "" || registration.schemaVersion <= 0 || registration.compile == nil {
			return nil, ErrInvalidRegistration
		}
		key := registrationKey{kind: registration.kind, schemaVersion: registration.schemaVersion}
		if _, exists := registry.registrations[key]; exists {
			return nil, fmt.Errorf("%w: %s/%d", ErrDuplicateRegistration, key.kind, key.schemaVersion)
		}
		registry.registrations[key] = registration
	}
	return registry, nil
}

// Compile 解码、校验并编译一个效果定义；返回的问题使用可写入 Validation Report 的稳定代码。
func (r *Registry) Compile(definition Definition) (CompiledDefinition, []Issue) {
	kind := strings.TrimSpace(definition.Kind)
	registration, exists := r.registrations[registrationKey{kind: kind, schemaVersion: definition.SchemaVersion}]
	if !exists {
		return CompiledDefinition{}, []Issue{{
			Code:      "effect_kind_version_unsupported",
			FieldPath: "/kind",
			Message:   "效果种类或 Schema 版本未被当前服务支持",
		}}
	}
	parameters, issues := registration.compile(definition.Parameters)
	if len(issues) > 0 {
		return CompiledDefinition{}, issues
	}
	return CompiledDefinition{Kind: kind, SchemaVersion: definition.SchemaVersion, Parameters: parameters}, nil
}

// Supported 按 kind 和版本稳定排序返回当前二进制支持的全部效果定义。
func (r *Registry) Supported() []Support {
	result := make([]Support, 0, len(r.registrations))
	for key := range r.registrations {
		result = append(result, Support{Kind: key.kind, SchemaVersion: key.schemaVersion})
	}
	sort.Slice(result, func(i, j int) bool {
		if result[i].Kind != result[j].Kind {
			return result[i].Kind < result[j].Kind
		}
		return result[i].SchemaVersion < result[j].SchemaVersion
	})
	return result
}

func invalidParametersIssue(message string) []Issue {
	return []Issue{{Code: "effect_parameters_invalid", FieldPath: "/parameters", Message: message}}
}
