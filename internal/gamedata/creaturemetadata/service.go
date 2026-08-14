// Package creaturemetadata 定义精灵分类字典、种类、形态、数值和技能学习关系的独立实时资料边界。
package creaturemetadata

import (
	"context"
	"errors"
	"fmt"
	"regexp"
	"strings"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

var (
	// ErrInvalidCreatureMetadata 表示 Creature 资料字段、唯一性或本地引用无效。
	ErrInvalidCreatureMetadata = errors.New("Creature 资料无效")
	// ErrCreatureMetadataNotFound 表示无法从关系表组装 Creature Data Projection。
	ErrCreatureMetadataNotFound = errors.New("Creature 资料不存在")
)

// ReferenceCatalog 提供 Creature 关系资料引用的强类型实时资料与 Ready Asset 边界。
type ReferenceCatalog interface {
	// ElementEnabled 判断属性是否存在且启用。
	ElementEnabled(context.Context, snowflake.ID) (bool, error)
	// StatEnabled 判断数值项是否存在且启用。
	StatEnabled(context.Context, snowflake.ID) (bool, error)
	// SkillEnabled 判断技能是否存在且启用。
	SkillEnabled(context.Context, snowflake.ID) (bool, error)
	// SkillLearnMethodEnabled 判断技能学习方式是否存在且启用。
	SkillLearnMethodEnabled(context.Context, snowflake.ID) (bool, error)
	// AbilityEnabled 判断特性是否存在且启用。
	AbilityEnabled(context.Context, snowflake.ID) (bool, error)
	// ItemEnabled 判断道具是否存在且启用。
	ItemEnabled(context.Context, snowflake.ID) (bool, error)
	// AssetReady 判断对象是否已经完成 Ready 生命周期校验。
	AssetReady(context.Context, snowflake.ID) (bool, error)
}

// ErrInvalidCreatureMetadataReference 表示 Creature 资料引用了不可用于实时运行的外部资料。
var ErrInvalidCreatureMetadataReference = errors.New("Creature 资料外部引用无效")
var codePattern = regexp.MustCompile(`^[a-z][a-z0-9-]{1,63}$`)

const (
	// maximumCreatureMetadataDictionaryEntries 限制单类小字典，避免无界管理载荷。
	maximumCreatureMetadataDictionaryEntries = 10_000
	// maximumCreatureMetadataRecords 限制主体和关系记录的单类数量。
	maximumCreatureMetadataRecords = 100_000
	// maximumCreatureSkillLearnRecords 单独限制技能学习关系。首套权威基础资料包含超过十万条
	// 跨世代学习关系；它们仍受固定上限保护，但不能被通用主体上限截断。
	maximumCreatureSkillLearnRecords = 200_000
	// maximumCreatureMetadataReferences 限制单条种类或形态携带的内嵌引用数量。
	maximumCreatureMetadataReferences = 32
)

// EggGroup 是精灵繁育分类使用的蛋组字典。
type EggGroup struct {
	// ID 是蛋组记录的稳定 Identifier。
	ID snowflake.ID `json:"id"`
	// Code 是蛋组的稳定英文机器编码。
	Code string `json:"code"`
	// Name 是蛋组的简体中文展示名称。
	Name string `json:"name"`
	// SortOrder 是字典的稳定展示顺序。
	SortOrder int32 `json:"sortOrder"`
	// Enabled 表示蛋组能否被新的 Species 引用。
	Enabled bool `json:"enabled"`
}

// Gender 是精灵资料可引用的性别字典。
type Gender struct {
	// ID 是性别记录的稳定 Identifier。
	ID snowflake.ID `json:"id"`
	// Code 是性别的稳定英文机器编码。
	Code string `json:"code"`
	// Name 是性别的简体中文展示名称。
	Name string `json:"name"`
	// SortOrder 是字典的稳定展示顺序。
	SortOrder int32 `json:"sortOrder"`
	// Enabled 表示性别能否用于新的个体资料。
	Enabled bool `json:"enabled"`
}

// Habitat 是精灵种类可引用的栖息地字典。
type Habitat struct {
	// ID 是栖息地记录的稳定 Identifier。
	ID snowflake.ID `json:"id"`
	// Code 是栖息地的稳定英文机器编码。
	Code string `json:"code"`
	// Name 是栖息地的简体中文展示名称。
	Name string `json:"name"`
	// SortOrder 是字典的稳定展示顺序。
	SortOrder int32 `json:"sortOrder"`
	// Enabled 表示栖息地能否被新的 Species 引用。
	Enabled bool `json:"enabled"`
}

// SpeciesColor 是精灵种类可引用的颜色字典。
type SpeciesColor struct {
	// ID 是颜色记录的稳定 Identifier。
	ID snowflake.ID `json:"id"`
	// Code 是颜色的稳定英文机器编码。
	Code string `json:"code"`
	// Name 是颜色的简体中文展示名称。
	Name string `json:"name"`
	// SortOrder 是字典的稳定展示顺序。
	SortOrder int32 `json:"sortOrder"`
	// Enabled 表示颜色能否被新的 Species 引用。
	Enabled bool `json:"enabled"`
}

// SpeciesShape 是精灵种类可引用的外形字典。
type SpeciesShape struct {
	// ID 是外形记录的稳定 Identifier。
	ID snowflake.ID `json:"id"`
	// Code 是外形的稳定英文机器编码。
	Code string `json:"code"`
	// Name 是外形的简体中文展示名称。
	Name string `json:"name"`
	// SortOrder 是字典的稳定展示顺序。
	SortOrder int32 `json:"sortOrder"`
	// Enabled 表示外形能否被新的 Species 引用。
	Enabled bool `json:"enabled"`
}

// GrowthRate 是带经验公式和说明的成长速率字典。
type GrowthRate struct {
	// ID 是成长速率记录的稳定 Identifier。
	ID snowflake.ID `json:"id"`
	// Code 是成长速率的稳定英文机器编码。
	Code string `json:"code"`
	// Name 是成长速率的简体中文展示名称。
	Name string `json:"name"`
	// Formula 是可选的经验曲线公式说明。
	Formula *string `json:"formula"`
	// Description 是面向管理者的可选成长曲线说明。
	Description *string `json:"description"`
	// Enabled 表示成长速率能否被新的 Species 引用。
	Enabled bool `json:"enabled"`
}

// GenderRatio 是一个可参战 Creature 的性别出现比率。官方规则以八分份表达；
// 雄性与雌性均为零时则明确表示该 Creature 无性别。
type GenderRatio struct {
	// MaleEighths 是八分份中雄性所占的份数，取值范围为 0 至 8。
	MaleEighths int32 `json:"maleEighths"`
	// FemaleEighths 是八分份中雌性所占的份数，取值范围为 0 至 8。
	FemaleEighths int32 `json:"femaleEighths"`
}

// Species 是精灵种类主体及其分类引用。
type Species struct {
	// ID 是 Species 的稳定 Identifier。
	ID snowflake.ID `json:"id"`
	// NationalDexNumber 是该 Species 唯一的全国图鉴正整数编号。
	NationalDexNumber int32 `json:"nationalDexNumber"`
	// Code 是 Species 的稳定英文机器编码。
	Code string `json:"code"`
	// Name 是 Species 的简体中文名称。
	Name string `json:"name"`
	// GrowthRateID 是可选的成长速率引用。
	GrowthRateID *snowflake.ID `json:"growthRateId"`
	// HabitatID 是可选的栖息地引用。
	HabitatID *snowflake.ID `json:"habitatId"`
	// ColorID 是可选的物种颜色引用。
	ColorID *snowflake.ID `json:"colorId"`
	// ShapeID 是可选的物种外形引用。
	ShapeID *snowflake.ID `json:"shapeId"`
	// EggGroupIDs 是该 Species 关联的蛋组稳定 Identifier 集合。
	EggGroupIDs []snowflake.ID `json:"eggGroupIds"`
	// Genus 是可选的简体中文分类名称。
	Genus *string `json:"genus"`
	// PokedexEntry 是面向玩家展示的物种图鉴条目；它不参与战斗规则或分类检索。
	PokedexEntry *string `json:"pokedexEntry"`
	// Description 是 Species 面向玩家的基础介绍，不参与战斗结算。
	Description *string `json:"description"`
	// Profile 是 Species 的外形与生态说明，不参与结构化分类检索。
	Profile *string `json:"profile"`
	// DesignOrigin 是 Species 的设计原型说明，对应首套权威资料中的 prototype。
	DesignOrigin *string `json:"designOrigin"`
	// Trivia 是 Species 的补充知识，不替代图鉴条目或战斗规则。
	Trivia *string `json:"trivia"`
	// GenderDifferences 表示该物种是否具有可观察的性别外观差异。
	GenderDifferences bool `json:"genderDifferences"`
	// FormsSwitchable 表示该物种的形态能否在既定规则下切换。
	FormsSwitchable bool `json:"formsSwitchable"`
	// Enabled 表示该 Species 能否用于新的 Creature 资料。
	Enabled bool `json:"enabled"`
}

// Creature 是可进入运行时目录的精灵主体。
type Creature struct {
	// ID 是可参战 Creature 的稳定 Identifier。
	ID snowflake.ID `json:"id"`
	// Code 是 Creature 的稳定英文机器编码。
	Code string `json:"code"`
	// Name 是 Creature 的简体中文展示名称。
	Name string `json:"name"`
	// SpeciesID 是该 Creature 所属 Species 的稳定 Identifier。
	SpeciesID snowflake.ID `json:"speciesId"`
	// InheritsFromCreatureID 是可选的资料继承来源 Creature。
	InheritsFromCreatureID *snowflake.ID `json:"inheritsFromCreatureId"`
	// Height 是上游资料使用的可选身高整数刻度。
	Height *int32 `json:"height"`
	// Weight 是上游资料使用的可选体重整数刻度。
	Weight *int32 `json:"weight"`
	// BaseExperience 是击败该 Creature 时使用的可选基础经验值。
	BaseExperience *int32 `json:"baseExperience"`
	// CaptureRate 是该具体 Creature 形态的捕获率整数参数，取值为 0 至 255。
	CaptureRate *int32 `json:"captureRate"`
	// HatchCycles 是该具体 Creature 形态孵化所需的基础周期数。
	HatchCycles *int32 `json:"hatchCycles"`
	// GenderRatio 保存该可参战形态的精确性别比率；它不代替个体可选性别字典。
	GenderRatio *GenderRatio `json:"genderRatio"`
	// DefaultForm 表示该 Creature 是否是所属 Species 的默认可参战记录。
	DefaultForm bool `json:"defaultForm"`
	// Enabled 表示该 Creature 能否进入新的 Team 与 Battle。
	Enabled bool `json:"enabled"`
}

// Form 是精灵的一个可选择形态及其属性集合。
type Form struct {
	// ID 是形态记录的稳定 Identifier。
	ID snowflake.ID `json:"id,omitempty"`
	// Code 是形态的稳定英文机器编码。
	Code string `json:"code"`
	// Name 是形态的简体中文展示名称。
	Name string `json:"name"`
	// CreatureID 是拥有该形态的 Creature 稳定 Identifier。
	CreatureID snowflake.ID `json:"creatureId"`
	// FormName 是上游资料提供的可选形态限定名称。
	FormName *string `json:"formName"`
	// SortOrder 是形态在完整资料中的可选稳定顺序。
	SortOrder *int32 `json:"sortOrder"`
	// FormOrder 是同一 Creature 内的可选形态顺序。
	FormOrder *int32 `json:"formOrder"`
	// BattleOnly 表示该形态只会在对战过程中出现。
	BattleOnly bool `json:"battleOnly"`
	// DefaultForm 表示该形态是否为所属 Creature 的默认形态。
	DefaultForm bool `json:"defaultForm"`
	// EnhancedForm 表示该形态是否属于强化形态。
	EnhancedForm bool `json:"enhancedForm"`
	// Enabled 表示该形态能否进入新的 Team 与 Battle。
	Enabled bool `json:"enabled"`
	// Version 是该形态记录的乐观并发版本；运行时投影不依赖该值。
	Version int64 `json:"version"`
	// ElementIDs 是该形态拥有的属性稳定 Identifier 集合。
	ElementIDs []snowflake.ID `json:"elementIds"`
}

// StatBinding 是精灵与已有数值项之间的基础数值关系。
type StatBinding struct {
	// ID 是能力关系记录的稳定 Identifier。
	ID snowflake.ID `json:"id,omitempty"`
	// CreatureID 是关系所属 Creature 的稳定 Identifier。
	CreatureID snowflake.ID `json:"creatureId"`
	// StatID 是被绑定能力项的稳定 Identifier。
	StatID snowflake.ID `json:"statId"`
	// BaseValue 是该能力项的基础值。
	BaseValue int32 `json:"baseValue"`
	// Effort 是击败该 Creature 时产出的可选努力值。
	Effort *int32 `json:"effort"`
	// Enabled 表示该能力关系是否进入新的 Team 与 Battle。
	Enabled bool `json:"enabled"`
	// Version 是该能力关系的乐观并发版本。
	Version int64 `json:"version"`
}

// SkillLearn 是精灵在指定方式和等级下学习已有技能的关系。
type SkillLearn struct {
	// ID 是技能学习关系的稳定 Identifier。
	ID snowflake.ID `json:"id,omitempty"`
	// CreatureID 是关系所属 Creature 的稳定 Identifier。
	CreatureID snowflake.ID `json:"creatureId"`
	// SkillID 是可学习技能的稳定 Identifier。
	SkillID snowflake.ID `json:"skillId"`
	// LearnMethodID 是学习方式的稳定 Identifier。
	LearnMethodID snowflake.ID `json:"learnMethodId"`
	// LevelLearnedAt 是通过该方式学习技能所需的等级；零由具体学习方式解释。
	LevelLearnedAt int32 `json:"levelLearnedAt"`
	// Enabled 表示该学习关系是否可用于新的 Team 校验。
	Enabled bool `json:"enabled"`
	// Version 是该学习关系的乐观并发版本。
	Version int64 `json:"version"`
}

// AbilityBinding 是精灵可拥有特性及其默认标记。
type AbilityBinding struct {
	// ID 是特性关系记录的稳定 Identifier。
	ID snowflake.ID `json:"id,omitempty"`
	// CreatureID 是关系所属 Creature 的稳定 Identifier。
	CreatureID snowflake.ID `json:"creatureId"`
	// AbilityID 是可拥有特性的稳定 Identifier。
	AbilityID snowflake.ID `json:"abilityId"`
	// Hidden 表示该关系属于隐藏特性。
	Hidden bool `json:"hidden"`
	// Slot 是同一 Creature 内的特性槽位序号。
	Slot int32 `json:"slot"`
	// Enabled 表示该特性关系是否可用于新的 Team。
	Enabled bool `json:"enabled"`
	// Version 是该特性关系的乐观并发版本。
	Version int64 `json:"version"`
}

// HeldItem 是精灵在资料规则下可能持有道具的关系。
type HeldItem struct {
	// ID 是携带物关系记录的稳定 Identifier。
	ID snowflake.ID `json:"id,omitempty"`
	// CreatureID 是关系所属 Creature 的稳定 Identifier。
	CreatureID snowflake.ID `json:"creatureId"`
	// ItemID 是可能携带物品的稳定 Identifier。
	ItemID snowflake.ID `json:"itemId"`
	// Rarity 是来源资料给出的相对稀有度整数。
	Rarity int32 `json:"rarity"`
	// Enabled 表示该携带物关系是否进入新的运行时资料。
	Enabled bool `json:"enabled"`
	// Version 是该携带物关系的乐观并发版本。
	Version int64 `json:"version"`
}

// Skin 是精灵可选择的稳定皮肤标识；实际对象引用由 Asset 生命周期确认后绑定。
type Skin struct {
	// ID 是皮肤记录的稳定 Identifier。
	ID snowflake.ID `json:"id,omitempty"`
	// CreatureID 是皮肤所属 Creature 的稳定 Identifier。
	CreatureID snowflake.ID `json:"creatureId"`
	// Code 是皮肤在所属 Creature 内的稳定英文机器编码。
	Code string `json:"code"`
	// Name 是皮肤的简体中文展示名称。
	Name string `json:"name"`
	// AssetID 是可选的 Ready Asset 稳定 Identifier。
	AssetID *snowflake.ID `json:"assetId"`
	// Enabled 表示该皮肤能否用于新的展示资料。
	Enabled bool `json:"enabled"`
	// Version 是该皮肤记录的乐观并发版本。
	Version int64 `json:"version"`
}

// EvolutionTriggerType 是 Creature Evolution 的封闭触发方式。
type EvolutionTriggerType string

const (
	// EvolutionTriggerLevel 表示通过提升到指定等级触发。
	EvolutionTriggerLevel EvolutionTriggerType = "level"
	// EvolutionTriggerItem 表示通过使用或携带指定物品触发。
	EvolutionTriggerItem EvolutionTriggerType = "item"
	// EvolutionTriggerTrade 表示通过连接交换触发。
	EvolutionTriggerTrade EvolutionTriggerType = "trade"
	// EvolutionTriggerFriendship 表示通过亲密度或友好度触发。
	EvolutionTriggerFriendship EvolutionTriggerType = "friendship"
	// EvolutionTriggerSkill 表示通过学会或使用指定技能触发。
	EvolutionTriggerSkill EvolutionTriggerType = "skill"
	// EvolutionTriggerBreeding 表示上游链路明确记录由生蛋产生。
	EvolutionTriggerBreeding EvolutionTriggerType = "breeding"
	// EvolutionTriggerSpecial 表示尚不能由首版强类型字段完整执行的特殊条件。
	EvolutionTriggerSpecial EvolutionTriggerType = "special"
)

// Evolution 是来源 Creature 满足条件后转换为目标 Creature 的有方向实时资料关系。
type Evolution struct {
	// ID 是进化关系的稳定 Identifier。
	ID snowflake.ID `json:"id,omitempty"`
	// FromCreatureID 是进化前来源 Creature 的稳定 Identifier。
	FromCreatureID snowflake.ID `json:"fromCreatureId"`
	// ToCreatureID 是进化后目标 Creature 的稳定 Identifier。
	ToCreatureID snowflake.ID `json:"toCreatureId"`
	// TriggerType 是进化条件的封闭触发类型。
	TriggerType EvolutionTriggerType `json:"triggerType"`
	// MinimumLevel 是等级触发所需的可选最低等级。
	MinimumLevel *int32 `json:"minimumLevel"`
	// TriggerItemID 是使用或携带物品条件引用的可选物品 Identifier。
	TriggerItemID *snowflake.ID `json:"triggerItemId"`
	// MinimumFriendship 是亲密度或友好度条件要求的可选最低值。
	MinimumFriendship *int32 `json:"minimumFriendship"`
	// TimeOfDay 是可选的 day、night 或 dusk 时间限定。
	TimeOfDay *string `json:"timeOfDay"`
	// Gender 是可选的 male 或 female 性别限定。
	Gender *string `json:"gender"`
	// RequiredSkillID 是学会指定技能条件引用的可选技能 Identifier。
	RequiredSkillID *snowflake.ID `json:"requiredSkillId"`
	// ConditionText 完整保留 Starllow 的简体中文进化条件说明。
	ConditionText string `json:"conditionText"`
	// Enabled 表示该关系是否可用于新的 RPG 进度计算。
	Enabled bool `json:"enabled"`
	// Version 是该关系记录的乐观并发版本。
	Version int64 `json:"version"`
}

// Data 是从独立关系表按需组装的完整 Creature Data Projection。
type Data struct {
	// EggGroups 是 Species 繁育分类引用的小型字典。
	EggGroups []EggGroup `json:"eggGroups"`
	// Genders 是个体资料可选择的性别字典。
	Genders []Gender `json:"genders"`
	// GrowthRates 是 Species 成长曲线字典。
	GrowthRates []GrowthRate `json:"growthRates"`
	// Habitats 是 Species 栖息地字典。
	Habitats []Habitat `json:"habitats"`
	// Colors 是 Species 颜色字典。
	Colors []SpeciesColor `json:"colors"`
	// Shapes 是 Species 外形字典。
	Shapes []SpeciesShape `json:"shapes"`
	// Species 是物种主体记录集合。
	Species []Species `json:"species"`
	// Creatures 是可参战 Creature 主体记录集合。
	Creatures []Creature `json:"creatures"`
	// Forms 是 Creature 形态记录集合。
	Forms []Form `json:"forms"`
	// Stats 是 Creature 基础能力关系集合。
	Stats []StatBinding `json:"stats"`
	// SkillLearns 是 Creature 技能学习关系集合。
	SkillLearns []SkillLearn `json:"skillLearns"`
	// Abilities 是 Creature 特性关系集合。
	Abilities []AbilityBinding `json:"abilities"`
	// HeldItems 是 Creature 可能携带物关系集合。
	HeldItems []HeldItem `json:"heldItems"`
	// Skins 是 Creature 展示皮肤集合。
	Skins []Skin `json:"skins"`
	// Evolutions 是从各来源 Creature 出发的进化关系集合。
	Evolutions []Evolution `json:"evolutions"`
}

// Snapshot 是从当前关系表组装的 Creature Data Projection。
type Snapshot struct {
	// Data 是当前关系表组装出的完整运行时投影。
	Data
}

// CreatureMetadataReader 是从关系表读取 Creature Data Projection 的只读端口。
type CreatureMetadataReader interface {
	GetCreatureMetadata(context.Context) (Snapshot, error)
}

// Service 校验 Creature Data Projection 的领域形状与外部引用。
type Service struct {
	reader CreatureMetadataReader
}

// NewService 使用显式依赖创建 Creature Data Projection 校验服务。
func NewService(reader CreatureMetadataReader) *Service { return &Service{reader: reader} }

// Get 从关系表读取 Team、维护校验和 Battle 冻结所需的完整运行时投影。
func (s *Service) Get(ctx context.Context) (Snapshot, error) {
	return s.reader.GetCreatureMetadata(ctx)
}

// Validate 重新读取并校验当前 Creature Data Projection；关系表为空时视为空资料。
func (s *Service) Validate(ctx context.Context) error {
	snapshot, err := s.Get(ctx)
	if errors.Is(err, ErrCreatureMetadataNotFound) {
		return nil
	}
	if err != nil {
		return err
	}
	if !valid(snapshot.Data) {
		return ErrInvalidCreatureMetadata
	}
	return nil
}

// ValidateReferences 校验形态、数值、技能、特性、持有物和皮肤的全部外部引用。
func (s *Service) ValidateReferences(ctx context.Context, catalog ReferenceCatalog) error {
	if catalog == nil {
		return ErrInvalidCreatureMetadataReference
	}
	snapshot, err := s.Get(ctx)
	if errors.Is(err, ErrCreatureMetadataNotFound) {
		return nil
	}
	if err != nil {
		return err
	}
	// 完整资料包含十万级学习关系，但它们只引用有限的技能和学习方式。按资料种类与稳定 Identifier
	// 去重后查询，既保留逐身份启用校验，也避免维护退出产生重复数据库往返。
	checked := make(map[string]map[snowflake.ID]struct{})
	check := func(kind string, id snowflake.ID, lookup func(context.Context, snowflake.ID) (bool, error)) error {
		seen := checked[kind]
		if seen == nil {
			seen = make(map[snowflake.ID]struct{})
			checked[kind] = seen
		}
		if _, exists := seen[id]; exists {
			return nil
		}
		seen[id] = struct{}{}
		enabled, lookupErr := lookup(ctx, id)
		if lookupErr != nil {
			return lookupErr
		}
		if !enabled {
			return fmt.Errorf("%w: %s %s", ErrInvalidCreatureMetadataReference, kind, id)
		}
		return nil
	}
	for _, form := range snapshot.Forms {
		for _, id := range form.ElementIDs {
			if err := check("属性", id, catalog.ElementEnabled); err != nil {
				return err
			}
		}
	}
	for _, binding := range snapshot.Stats {
		if err := check("数值项", binding.StatID, catalog.StatEnabled); err != nil {
			return err
		}
	}
	for _, learn := range snapshot.SkillLearns {
		if err := check("技能", learn.SkillID, catalog.SkillEnabled); err != nil {
			return err
		}
		if err := check("学习方式", learn.LearnMethodID, catalog.SkillLearnMethodEnabled); err != nil {
			return err
		}
	}
	for _, binding := range snapshot.Abilities {
		if err := check("特性", binding.AbilityID, catalog.AbilityEnabled); err != nil {
			return err
		}
	}
	for _, held := range snapshot.HeldItems {
		if err := check("道具", held.ItemID, catalog.ItemEnabled); err != nil {
			return err
		}
	}
	for _, skin := range snapshot.Skins {
		if skin.AssetID != nil {
			if err := check("Ready Asset", *skin.AssetID, catalog.AssetReady); err != nil {
				return err
			}
		}
	}
	return nil
}

func clean(value *string) *string {
	if value == nil {
		return nil
	}
	v := strings.TrimSpace(*value)
	if v == "" {
		return nil
	}
	return &v
}

func valid(data Data) bool {
	if len(data.EggGroups) > maximumCreatureMetadataDictionaryEntries ||
		len(data.Genders) > maximumCreatureMetadataDictionaryEntries ||
		len(data.GrowthRates) > maximumCreatureMetadataDictionaryEntries ||
		len(data.Habitats) > maximumCreatureMetadataDictionaryEntries ||
		len(data.Colors) > maximumCreatureMetadataDictionaryEntries ||
		len(data.Shapes) > maximumCreatureMetadataDictionaryEntries ||
		len(data.Species) > maximumCreatureMetadataRecords ||
		len(data.Creatures) > maximumCreatureMetadataRecords ||
		len(data.Forms) > maximumCreatureMetadataRecords ||
		len(data.Stats) > maximumCreatureMetadataRecords ||
		len(data.SkillLearns) > maximumCreatureSkillLearnRecords ||
		len(data.Abilities) > maximumCreatureMetadataRecords ||
		len(data.HeldItems) > maximumCreatureMetadataRecords ||
		len(data.Skins) > maximumCreatureMetadataRecords {
		return false
	}
	all := map[snowflake.ID]bool{}
	add := func(id snowflake.ID) bool {
		if id == snowflake.ID(0) || all[id] {
			return false
		}
		all[id] = true
		return true
	}
	validateEntry := func(id snowflake.ID, code, name string, ids map[snowflake.ID]bool, codes map[string]bool) bool {
		if !add(id) || !validCodeName(code, name, 80) || codes[code] {
			return false
		}
		ids[id], codes[code] = true, true
		return true
	}
	eggs, genders, habitats := map[snowflake.ID]bool{}, map[snowflake.ID]bool{}, map[snowflake.ID]bool{}
	colors, shapes := map[snowflake.ID]bool{}, map[snowflake.ID]bool{}
	codes := map[string]bool{}
	for _, v := range data.EggGroups {
		if !validateEntry(v.ID, v.Code, v.Name, eggs, codes) {
			return false
		}
	}
	codes = map[string]bool{}
	for _, v := range data.Genders {
		if !validateEntry(v.ID, v.Code, v.Name, genders, codes) {
			return false
		}
	}
	codes = map[string]bool{}
	for _, v := range data.Habitats {
		if !validateEntry(v.ID, v.Code, v.Name, habitats, codes) {
			return false
		}
	}
	codes = map[string]bool{}
	for _, v := range data.Colors {
		if !validateEntry(v.ID, v.Code, v.Name, colors, codes) {
			return false
		}
	}
	codes = map[string]bool{}
	for _, v := range data.Shapes {
		if !validateEntry(v.ID, v.Code, v.Name, shapes, codes) {
			return false
		}
	}
	growth := map[snowflake.ID]bool{}
	codes = map[string]bool{}
	for _, v := range data.GrowthRates {
		if !add(v.ID) || !validCodeName(v.Code, v.Name, 120) || codes[v.Code] || !optional(v.Formula, 500) || !optional(v.Description, 1000) {
			return false
		}
		growth[v.ID], codes[v.Code] = true, true
	}
	speciesIDs := map[snowflake.ID]bool{}
	codes = map[string]bool{}
	for _, v := range data.Species {
		if len(v.EggGroupIDs) > maximumCreatureMetadataReferences || !add(v.ID) || !validCodeName(v.Code, v.Name, 120) || codes[v.Code] || !ref(v.GrowthRateID, growth) || !ref(v.HabitatID, habitats) || !ref(v.ColorID, colors) || !ref(v.ShapeID, shapes) || !refs(v.EggGroupIDs, eggs) || !optional(v.Genus, 200) || !optional(v.PokedexEntry, 2000) || !optional(v.Description, 4000) || !optional(v.Profile, 4000) || !optional(v.DesignOrigin, 4000) || !optional(v.Trivia, 4000) {
			return false
		}
		speciesIDs[v.ID], codes[v.Code] = true, true
	}
	creatureIDs := map[snowflake.ID]bool{}
	codes = map[string]bool{}
	for _, v := range data.Creatures {
		if !add(v.ID) || !validCodeName(v.Code, v.Name, 120) || codes[v.Code] || !speciesIDs[v.SpeciesID] || negative(v.Height) || negative(v.Weight) || negative(v.BaseExperience) || !validGenderRatio(v.GenderRatio) {
			return false
		}
		creatureIDs[v.ID], codes[v.Code] = true, true
	}
	for _, v := range data.Creatures {
		if v.InheritsFromCreatureID != nil && (!creatureIDs[*v.InheritsFromCreatureID] || *v.InheritsFromCreatureID == v.ID) {
			return false
		}
	}
	formsPerCreature := map[snowflake.ID]int{}
	for _, v := range data.Forms {
		if len(v.ElementIDs) > maximumCreatureMetadataReferences || !add(v.ID) || !validCodeName(v.Code, v.Name, 120) || !creatureIDs[v.CreatureID] || !refsNonNil(v.ElementIDs) || !optional(v.FormName, 120) {
			return false
		}
		if v.DefaultForm {
			formsPerCreature[v.CreatureID]++
			if formsPerCreature[v.CreatureID] > 1 {
				return false
			}
		}
	}
	pairs := map[string]bool{}
	for _, v := range data.Stats {
		key := v.CreatureID.String() + ":" + v.StatID.String()
		if !add(v.ID) || !creatureIDs[v.CreatureID] || v.StatID == snowflake.ID(0) || v.BaseValue < 1 || v.BaseValue > 999 || negative(v.Effort) || pairs[key] {
			return false
		}
		pairs[key] = true
	}
	pairs = map[string]bool{}
	for _, v := range data.SkillLearns {
		key := v.CreatureID.String() + ":" + v.SkillID.String() + ":" + v.LearnMethodID.String() + ":" + string(rune(v.LevelLearnedAt))
		if !add(v.ID) || !creatureIDs[v.CreatureID] || v.SkillID == snowflake.ID(0) || v.LearnMethodID == snowflake.ID(0) || v.LevelLearnedAt < 0 || v.LevelLearnedAt > 100 || pairs[key] {
			return false
		}
		pairs[key] = true
	}
	for _, v := range data.Abilities {
		if !add(v.ID) || !creatureIDs[v.CreatureID] || v.AbilityID == snowflake.ID(0) || v.Slot < 1 || v.Slot > 10 {
			return false
		}
	}
	for _, v := range data.HeldItems {
		if !add(v.ID) || !creatureIDs[v.CreatureID] || v.ItemID == snowflake.ID(0) || v.Rarity < 0 || v.Rarity > 100 {
			return false
		}
	}
	for _, v := range data.Skins {
		if !add(v.ID) || !creatureIDs[v.CreatureID] || !validCodeName(v.Code, v.Name, 120) || (v.AssetID != nil && *v.AssetID == snowflake.ID(0)) {
			return false
		}
	}
	return true
}

// validGenderRatio 只接受合计为八份的性别比率，或代表无性别的 0:0。
func validGenderRatio(value *GenderRatio) bool {
	if value == nil || value.MaleEighths < 0 || value.MaleEighths > 8 || value.FemaleEighths < 0 || value.FemaleEighths > 8 {
		return false
	}
	total := value.MaleEighths + value.FemaleEighths
	return total == 0 || total == 8
}
func validCodeName(code, name string, maximum int) bool {
	return codePattern.MatchString(code) && name != "" && len([]rune(name)) <= maximum
}
func optional(v *string, max int) bool                    { return v == nil || len([]rune(*v)) <= max }
func ref(v *snowflake.ID, ids map[snowflake.ID]bool) bool { return v == nil || ids[*v] }
func refs(values []snowflake.ID, ids map[snowflake.ID]bool) bool {
	seen := map[snowflake.ID]bool{}
	for _, v := range values {
		if !ids[v] || seen[v] {
			return false
		}
		seen[v] = true
	}
	return true
}
func refsNonNil(values []snowflake.ID) bool {
	seen := map[snowflake.ID]bool{}
	for _, v := range values {
		if v == snowflake.ID(0) || seen[v] {
			return false
		}
		seen[v] = true
	}
	return true
}
func negative(v *int32) bool { return v != nil && *v < 0 }
