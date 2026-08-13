package team

import (
	"context"
	"errors"
	"slices"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
)

var (
	// ErrTeamCatalogUnavailable 表示当前实时资料暂时无法用于 Team 校验。
	ErrTeamCatalogUnavailable = errors.New("Team 实时资料不可用")
	// ErrTeamReferenceInvalid 表示成员引用在当前实时资料中不可用或不兼容。
	ErrTeamReferenceInvalid = errors.New("Team 资料引用无效")
)

// CompatibilityIssue 精确指出当前实时资料下不兼容的成员位置、字段和稳定引用。
type CompatibilityIssue struct {
	// MemberPosition 是发生不兼容问题的 Team 成员固定位置。
	MemberPosition int32 `json:"memberPosition"`
	// Field 是返回给客户端的稳定输入字段路径。
	Field string `json:"field"`
	// Code 是描述缺失或不兼容原因的稳定机器码。
	Code string `json:"code"`
	// ReferenceID 是当前实时资料中无效或不兼容的稳定 Identifier。
	ReferenceID snowflake.ID `json:"referenceId"`
}

// CompatibilityError 表示 Current Game Data 拒绝 Team 成员引用时的完整结构化问题集。
//
// 它通过 Unwrap 保留 ErrTeamReferenceInvalid，因此既有调用方仍可用 errors.Is 进行稳定分支；
// HTTP 适配器则可以读取 Issues 并把同一批问题以 JSON metadata 返回给客户端。
type CompatibilityError struct {
	// issues 是不可由调用方修改的当前资料兼容问题快照。
	issues []CompatibilityIssue
}

// NewCompatibilityError 创建一个保留问题集副本的当前资料兼容错误。
//
// 复制输入切片使调用方后续复用或修改自己的缓冲区时，已经产生的领域错误仍代表当时的校验事实。
func NewCompatibilityError(issues []CompatibilityIssue) *CompatibilityError {
	return &CompatibilityError{issues: slices.Clone(issues)}
}

// Error 返回与 ErrTeamReferenceInvalid 一致的稳定错误文本，避免把字段细节混入非结构化日志文本。
func (*CompatibilityError) Error() string {
	return ErrTeamReferenceInvalid.Error()
}

// Unwrap 保留现有领域哨兵，兼容 errors.Is(err, ErrTeamReferenceInvalid) 的调用方。
func (*CompatibilityError) Unwrap() error {
	return ErrTeamReferenceInvalid
}

// Issues 返回独立副本，供 HTTP 等边界安全序列化当前校验产生的结构化问题。
func (errorValue *CompatibilityError) Issues() []CompatibilityIssue {
	if errorValue == nil {
		return nil
	}
	return slices.Clone(errorValue.issues)
}

// Reference 是 Team 校验所需的最小实时资料引用状态。
type Reference struct {
	// ID 是跨模块稳定引用的资料 Identifier。
	ID snowflake.ID
	// Code 是限制规则匹配时使用的资料 Stable Code；空字符串只允许缺少机器编码的资料类型。
	Code string
	// Enabled 表示该资料当前允许进入新 Team 和新对战。
	Enabled bool
}

// ReferenceCatalog 按资料类型保存 Team 校验所需的实时只读投影。
type ReferenceCatalog struct {
	// Elements 是当前属性资料引用。
	Elements []Reference
	// Abilities 是当前特性资料引用。
	Abilities []Reference
	// Items 是当前道具资料引用。
	Items []Reference
	// Skills 是当前技能资料引用。
	Skills []Reference
	// Stats 是当前数值项资料引用。
	Stats []Reference
	// Natures 是当前 Nature 资料引用。
	Natures []Reference
	// CreatureMetadata 保留生物、形态、性别、皮肤及可学习关系的独立结构。
	CreatureMetadata creaturemetadata.Data
}

// ReferenceCatalogReader 原子读取同一全局修订下的 Team 实时引用目录。
type ReferenceCatalogReader interface {
	Current(context.Context) (ReferenceCatalog, error)
}

// CurrentMemberValidator 按当前实时资料校验完整 Team 成员。
//
// 它让 Team 保存、分享导入和对战入场都依赖同一条资料校验规则，而不让分享持久层感知资料读取实现。
type CurrentMemberValidator interface {
	// ValidateCurrent 拒绝包含禁用、缺失或与生物不兼容引用的成员集合。
	ValidateCurrent(context.Context, []Member) error
}

// CatalogValidator 按同一全局修订的实时引用目录校验 Team。
type CatalogValidator struct {
	// reader 原子取得同一全局资料修订下的最小 Team 引用目录。
	reader ReferenceCatalogReader
}

// NewCatalogValidator 创建当前实时资料 Team 引用校验器。
func NewCatalogValidator(reader ReferenceCatalogReader) *CatalogValidator {
	return &CatalogValidator{reader: reader}
}

// ValidateCurrent 使用当前实时资料投影校验完整阵容。
func (v *CatalogValidator) ValidateCurrent(ctx context.Context, members []Member) error {
	issues, err := v.CheckCurrent(ctx, members)
	if err != nil {
		return err
	}
	if len(issues) > 0 {
		return NewCompatibilityError(issues)
	}
	return nil
}

// ValidateMembersAgainstCatalog 使用调用方已经读取并固定的实时资料目录校验完整阵容。
//
// 该函数供全局维护窗口的退出校验复用：退出过程必须在维护仍开启时验证资料，不能调用会主动拒绝维护
// 状态的 ValidateCurrent。调用方负责保证 catalog 来自同一全局资料修订。
func ValidateMembersAgainstCatalog(members []Member, catalog ReferenceCatalog) error {
	index := newCatalogIndex(catalog)
	for _, member := range members {
		if len(index.issues(member)) != 0 {
			return ErrTeamReferenceInvalid
		}
	}
	return nil
}

// CheckCurrent 返回当前实时资料下逐成员、逐字段的全部兼容性问题。
func (v *CatalogValidator) CheckCurrent(ctx context.Context, members []Member) ([]CompatibilityIssue, error) {
	catalog, err := v.reader.Current(ctx)
	if err != nil {
		// 只有 teamcatalog adapter 已明确标记的维护或修订竞争才会成为
		// ErrTeamCatalogUnavailable。数据库、解码和 Context 故障必须保留，以便 HTTP
		// 边界记录并返回 5xx，而不是误导客户端将服务端故障当作可重试的 409 冲突。
		return nil, err
	}
	index := newCatalogIndex(catalog)
	issues := make([]CompatibilityIssue, 0)
	for _, member := range members {
		issues = append(issues, index.issues(member)...)
	}
	return issues, nil
}

type catalogIndex struct {
	// elements 保存当前启用的太晶属性资料稳定 Identifier。
	elements map[snowflake.ID]struct{}
	// abilities 保存当前启用的特性资料稳定 Identifier。
	abilities map[snowflake.ID]struct{}
	// items 保存当前启用的道具资料稳定 Identifier。
	items map[snowflake.ID]struct{}
	// skills 保存当前启用的技能资料稳定 Identifier。
	skills map[snowflake.ID]struct{}
	// stats 保存当前启用的培养数值资料稳定 Identifier。
	stats map[snowflake.ID]struct{}
	// natures 保存当前启用的 Nature 资料稳定 Identifier。
	natures map[snowflake.ID]struct{}
	// creatures 保存当前启用的生物资料稳定 Identifier。
	creatures map[snowflake.ID]struct{}
	// forms 把当前启用形态映射到其所属生物稳定 Identifier。
	forms map[snowflake.ID]snowflake.ID
	// genders 保存当前启用的性别资料稳定 Identifier。
	genders map[snowflake.ID]struct{}
	// skins 把当前启用皮肤映射到其所属生物稳定 Identifier。
	skins map[snowflake.ID]snowflake.ID
	// creatureSkills 保存生物与可学习技能之间的实时关联。
	creatureSkills map[[2]snowflake.ID]struct{}
	// creatureAbilities 保存生物与可选特性之间的实时关联。
	creatureAbilities map[[2]snowflake.ID]struct{}
	// creatureStats 保存生物与可配置培养数值之间的实时关联。
	creatureStats map[[2]snowflake.ID]struct{}
	// creatureGenders 把生物映射到该 Creature 性别比率中占比非零的可选性别。
	creatureGenders map[snowflake.ID]map[snowflake.ID]struct{}
}

func newCatalogIndex(catalog ReferenceCatalog) catalogIndex {
	index := catalogIndex{
		elements: enabledIDs(catalog.Elements), abilities: enabledIDs(catalog.Abilities),
		items: enabledIDs(catalog.Items), skills: enabledIDs(catalog.Skills), stats: enabledIDs(catalog.Stats), natures: enabledIDs(catalog.Natures),
		creatures: make(map[snowflake.ID]struct{}), forms: make(map[snowflake.ID]snowflake.ID),
		genders: make(map[snowflake.ID]struct{}), skins: make(map[snowflake.ID]snowflake.ID),
		creatureSkills: make(map[[2]snowflake.ID]struct{}), creatureAbilities: make(map[[2]snowflake.ID]struct{}),
		creatureStats: make(map[[2]snowflake.ID]struct{}), creatureGenders: make(map[snowflake.ID]map[snowflake.ID]struct{}),
	}
	genderIDsByCode := make(map[string]snowflake.ID, len(catalog.CreatureMetadata.Genders))
	for _, value := range catalog.CreatureMetadata.Genders {
		if value.Enabled {
			index.genders[value.ID] = struct{}{}
			genderIDsByCode[value.Code] = value.ID
		}
	}
	for _, value := range catalog.CreatureMetadata.Creatures {
		if value.Enabled {
			index.creatures[value.ID] = struct{}{}
			if value.GenderRatio != nil {
				allowed := make(map[snowflake.ID]struct{}, 2)
				if value.GenderRatio.MaleEighths > 0 && genderIDsByCode["male"] != snowflake.ID(0) {
					allowed[genderIDsByCode["male"]] = struct{}{}
				}
				if value.GenderRatio.FemaleEighths > 0 && genderIDsByCode["female"] != snowflake.ID(0) {
					allowed[genderIDsByCode["female"]] = struct{}{}
				}
				if value.GenderRatio.MaleEighths == 0 && value.GenderRatio.FemaleEighths == 0 && genderIDsByCode["genderless"] != snowflake.ID(0) {
					allowed[genderIDsByCode["genderless"]] = struct{}{}
				}
				index.creatureGenders[value.ID] = allowed
			}
		}
	}
	for _, value := range catalog.CreatureMetadata.Forms {
		if value.Enabled {
			index.forms[value.ID] = value.CreatureID
		}
	}
	for _, value := range catalog.CreatureMetadata.Skins {
		if value.Enabled {
			index.skins[value.ID] = value.CreatureID
		}
	}
	for _, value := range catalog.CreatureMetadata.SkillLearns {
		index.creatureSkills[[2]snowflake.ID{value.CreatureID, value.SkillID}] = struct{}{}
	}
	for _, value := range catalog.CreatureMetadata.Abilities {
		index.creatureAbilities[[2]snowflake.ID{value.CreatureID, value.AbilityID}] = struct{}{}
	}
	for _, value := range catalog.CreatureMetadata.Stats {
		index.creatureStats[[2]snowflake.ID{value.CreatureID, value.StatID}] = struct{}{}
	}
	return index
}

func enabledIDs(values []Reference) map[snowflake.ID]struct{} {
	result := make(map[snowflake.ID]struct{}, len(values))
	for _, value := range values {
		if value.Enabled && value.ID != snowflake.ID(0) {
			result[value.ID] = struct{}{}
		}
	}
	return result
}

func (i catalogIndex) issues(member Member) []CompatibilityIssue {
	result := make([]CompatibilityIssue, 0)
	add := func(field, code string, referenceID snowflake.ID) {
		result = append(result, CompatibilityIssue{
			MemberPosition: member.Position, Field: field, Code: code, ReferenceID: referenceID,
		})
	}
	if _, ok := i.creatures[member.CreatureID]; !ok {
		add("creatureId", "reference_unavailable", member.CreatureID)
	}
	if member.FormID != nil && i.forms[*member.FormID] != member.CreatureID {
		add("formId", "reference_incompatible", *member.FormID)
	}
	if member.GenderID != nil {
		if _, ok := i.genders[*member.GenderID]; !ok {
			add("genderId", "reference_unavailable", *member.GenderID)
		} else if allowedGenderIDs, constrained := i.creatureGenders[member.CreatureID]; constrained {
			if _, allowed := allowedGenderIDs[*member.GenderID]; !allowed {
				add("genderId", "reference_incompatible", *member.GenderID)
			}
		}
	}
	if member.SkinID != nil && i.skins[*member.SkinID] != member.CreatureID {
		add("skinId", "reference_incompatible", *member.SkinID)
	}
	if _, ok := i.abilities[member.AbilityID]; !ok {
		add("abilityId", "reference_unavailable", member.AbilityID)
	} else if _, ok := i.creatureAbilities[[2]snowflake.ID{member.CreatureID, member.AbilityID}]; !ok {
		add("abilityId", "reference_incompatible", member.AbilityID)
	}
	if member.ItemID != nil {
		if _, ok := i.items[*member.ItemID]; !ok {
			add("itemId", "reference_unavailable", *member.ItemID)
		}
	}
	if _, ok := i.elements[member.TeraElementID]; !ok {
		add("teraElementId", "reference_unavailable", member.TeraElementID)
	}
	if _, ok := i.natures[member.NatureID]; !ok {
		add("natureId", "reference_unavailable", member.NatureID)
	}
	for _, skill := range member.Skills {
		if _, ok := i.skills[skill.SkillID]; !ok {
			add("skillIds", "reference_unavailable", skill.SkillID)
		} else if _, ok := i.creatureSkills[[2]snowflake.ID{member.CreatureID, skill.SkillID}]; !ok {
			add("skillIds", "reference_incompatible", skill.SkillID)
		}
	}
	for _, stat := range member.Stats {
		if _, ok := i.stats[stat.StatID]; !ok {
			add("stats.statId", "reference_unavailable", stat.StatID)
		} else if _, ok := i.creatureStats[[2]snowflake.ID{member.CreatureID, stat.StatID}]; !ok {
			add("stats.statId", "reference_incompatible", stat.StatID)
		}
	}
	return result
}
