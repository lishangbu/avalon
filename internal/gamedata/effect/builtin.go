package effect

import (
	"sort"
	"strconv"

	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
)

const (
	// KindUniqueHeldItemClause 禁止同一队伍登记重复的持有道具。
	KindUniqueHeldItemClause = "battle.clause.unique-held-item"
	// KindUniqueSpeciesClause 禁止同一队伍登记重复的生物 Stable Code。
	KindUniqueSpeciesClause = "battle.clause.unique-species"
	// KindLevelNormalizationMechanic 在对局快照中把参战成员等级规范化到固定值。
	KindLevelNormalizationMechanic = "battle.mechanic.level-normalization"
	// KindTerastallizationMechanic 允许赛制中的每一方在整局内随一次技能行动使用太晶化。
	// 该机制没有可配置数值；成员的太晶属性来自 Team 成员资料，并在 Battle 启动时冻结到战斗状态。
	KindTerastallizationMechanic = "battle.mechanic.terastallization"
	// KindStableCodeListRestriction 按资料类型允许或禁止一组 Stable Code。
	KindStableCodeListRestriction = "battle.restriction.stable-code-list"
)

// LevelNormalizationParameters 是固定等级规范化机制的版本 1 参数。
type LevelNormalizationParameters struct {
	Level int32 `json:"level"`
}

// StableCodeListParameters 是 Stable Code 名单限制的版本 1 参数。
type StableCodeListParameters struct {
	Mode         string   `json:"mode"`
	ResourceType string   `json:"resourceType"`
	StableCodes  []string `json:"stableCodes"`
}

// NewDefaultRegistry 显式构造当前二进制支持的效果集合。
//
// 新增效果必须在此处注册并同步 Protobuf 参数消息；不得通过 init 或反射让支持集合随运行环境变化。
func NewDefaultRegistry() (*Registry, error) {
	return NewRegistry(
		Register(KindUniqueHeldItemClause, 1, validateEmptyParameters, compileEmptyParameters),
		Register(KindUniqueSpeciesClause, 1, validateEmptyParameters, compileEmptyParameters),
		Register(KindLevelNormalizationMechanic, 1, validateLevelNormalization, compileLevelNormalization),
		Register(KindTerastallizationMechanic, 1, validateEmptyParameters, compileEmptyParameters),
		Register(KindStableCodeListRestriction, 1, validateStableCodeList, compileStableCodeList),
	)
}

func validateEmptyParameters(struct{}) []Issue { return nil }

func compileEmptyParameters(struct{}) (any, error) { return struct{}{}, nil }

func validateLevelNormalization(parameters LevelNormalizationParameters) []Issue {
	if parameters.Level < 1 || parameters.Level > 100 {
		return []Issue{{
			Code: "level_out_of_range", FieldPath: "/parameters/level", Message: "等级必须介于 1 到 100 之间",
		}}
	}
	return nil
}

func compileLevelNormalization(parameters LevelNormalizationParameters) (any, error) {
	return parameters, nil
}

func validateStableCodeList(parameters StableCodeListParameters) []Issue {
	issues := make([]Issue, 0)
	if parameters.Mode != "allow" && parameters.Mode != "deny" {
		issues = append(issues, Issue{
			Code: "restriction_mode_invalid", FieldPath: "/parameters/mode", Message: "名单模式必须是 allow 或 deny",
		})
	}
	switch parameters.ResourceType {
	case "ability", "creature", "item", "skill":
	default:
		issues = append(issues, Issue{
			Code: "restriction_resource_type_invalid", FieldPath: "/parameters/resourceType", Message: "名单资料类型不受支持",
		})
	}
	seen := make(map[string]struct{}, len(parameters.StableCodes))
	if len(parameters.StableCodes) == 0 || len(parameters.StableCodes) > 1_000 {
		issues = append(issues, Issue{
			Code: "restriction_stable_codes_size_invalid", FieldPath: "/parameters/stableCodes", Message: "名单必须包含 1 到 1000 个 Stable Code",
		})
	}
	for index, code := range parameters.StableCodes {
		if !stablecode.Valid(code) {
			issues = append(issues, Issue{
				Code: "stable_code_invalid", FieldPath: "/parameters/stableCodes/" + strconv.Itoa(index), Message: "Stable Code 格式无效",
			})
			continue
		}
		if _, exists := seen[code]; exists {
			issues = append(issues, Issue{
				Code: "stable_code_duplicated", FieldPath: "/parameters/stableCodes/" + strconv.Itoa(index), Message: "Stable Code 不能重复",
			})
		}
		seen[code] = struct{}{}
	}
	return issues
}

func compileStableCodeList(parameters StableCodeListParameters) (any, error) {
	parameters.StableCodes = append([]string(nil), parameters.StableCodes...)
	sort.Strings(parameters.StableCodes)
	return parameters, nil
}
