package creaturemetadata

import (
	"context"
	"errors"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
)

var (
	// ErrCreatureDataNotFound 表示指定的 Species 或 Creature 记录不存在。
	ErrCreatureDataNotFound = errors.New("Creature 资料不存在")
	// ErrCreatureDataConflict 表示编码、记录版本、资料修订或幂等请求发生冲突。
	ErrCreatureDataConflict = errors.New("Creature 资料冲突")
)

// ManagedSpecies 是管理端逐条维护的 Species 记录。
type ManagedSpecies struct {
	// Species 是运行时投影与管理端共同使用的物种字段。
	Species
	// Version 是该 Species 记录的乐观并发版本。
	Version int64
}

// ManagedCreature 是管理端逐条维护的可参战 Creature 记录。
type ManagedCreature struct {
	// Creature 是运行时投影与管理端共同使用的 Creature 字段。
	Creature
	// Version 是该 Creature 记录的乐观并发版本。
	Version int64
}

// SpeciesListQuery 是 Species 的页码分页与文本筛选条件。
type SpeciesListQuery struct {
	// Page 是从一开始的页码。
	Page int32
	// PageSize 是单页最大记录数。
	PageSize int32
	// Q 同时模糊匹配机器编码、名称、分类、图鉴条目与四类参考文案。
	Q string
	// Enabled 可选地筛选启用状态。
	Enabled *bool
}

// SpeciesPage 是 Species 的有界分页结果。
type SpeciesPage struct {
	// Items 是当前页的 Species 记录。
	Items []ManagedSpecies
	// Total 是符合筛选条件的记录总数。
	Total int64
	// Page 是规范化后的当前页码。
	Page int32
	// PageSize 是规范化后的单页条数。
	PageSize int32
}

// CreatureListQuery 是 Creature 的页码分页与归属筛选条件。
type CreatureListQuery struct {
	// Page 是从一开始的页码。
	Page int32
	// PageSize 是单页最大记录数。
	PageSize int32
	// Q 同时模糊匹配机器编码和名称。
	Q string
	// SpeciesID 可选地限定到一个 Species。
	SpeciesID *snowflake.ID
	// Enabled 可选地筛选启用状态。
	Enabled *bool
}

// CreaturePage 是 Creature 的有界分页结果。
type CreaturePage struct {
	// Items 是当前页的 Creature 记录。
	Items []ManagedCreature
	// Total 是符合筛选条件的记录总数。
	Total int64
	// Page 是规范化后的当前页码。
	Page int32
	// PageSize 是规范化后的单页条数。
	PageSize int32
}

// ReferenceOptions 是 Species 管理表单使用的小型独立引用资料集合。
// 它不包含 Creature、形态或技能关系，也不承担运行时资料投影职责。
type ReferenceOptions struct {
	// EggGroups 是可选择的蛋组资料。
	EggGroups []EggGroup
	// GrowthRates 是可选择的成长速率资料。
	GrowthRates []GrowthRate
	// Habitats 是可选择的栖息地资料。
	Habitats []Habitat
	// Colors 是可选择的 Species 颜色资料。
	Colors []SpeciesColor
	// Shapes 是可选择的 Species 外形资料。
	Shapes []SpeciesShape
}

// CreatureRelations 是单个 Creature 拥有的独立关系记录集合。
// 该管理投影始终限定到一个 Creature，不是全局 Creature 聚合或 Catalog。
type CreatureRelations struct {
	// Forms 是该 Creature 的形态记录。
	Forms []Form
	// Stats 是该 Creature 的基础能力关系。
	Stats []StatBinding
	// SkillLearns 是该 Creature 的技能学习关系。
	SkillLearns []SkillLearn
	// Abilities 是该 Creature 的可选特性关系。
	Abilities []AbilityBinding
	// HeldItems 是该 Creature 的可能携带物关系。
	HeldItems []HeldItem
	// Skins 是该 Creature 的展示皮肤记录。
	Skins []Skin
	// Evolutions 是从该 Creature 出发的完整进化关系集合。
	Evolutions []Evolution
}

// ReplaceRelationsCommand 原子替换一个 Creature 的关系记录集合。
type ReplaceRelationsCommand struct {
	// GameDataWriteContext 携带维护窗口、修订、操作者和幂等事实。
	administration.GameDataWriteContext
	// CreatureID 是本次写入唯一允许修改的 Creature。
	CreatureID snowflake.ID
	// Relations 是该 Creature 更新后的完整关系集合。
	Relations CreatureRelations
}

// CreateSpeciesCommand 是创建单条 Species 所需的完整管理命令。
type CreateSpeciesCommand struct {
	// GameDataWriteContext 携带维护窗口、修订、操作者和幂等事实。
	administration.GameDataWriteContext
	// Species 是待创建的业务字段；ID 和版本由服务生成。
	Species Species
}

// UpdateSpeciesCommand 是使用乐观锁更新单条 Species 的完整管理命令。
type UpdateSpeciesCommand struct {
	// GameDataWriteContext 携带维护窗口、修订、操作者和幂等事实。
	administration.GameDataWriteContext
	// Species 是更新后的完整业务字段。
	Species Species
	// ExpectedVersion 是调用方最近读取到的记录版本。
	ExpectedVersion int64
}

// CreateCreatureCommand 是创建单条 Creature 所需的完整管理命令。
type CreateCreatureCommand struct {
	// GameDataWriteContext 携带维护窗口、修订、操作者和幂等事实。
	administration.GameDataWriteContext
	// Code 是稳定英文机器编码。
	Code string
	// Name 是简体中文展示名称。
	Name string
	// SpeciesID 是所属 Species 的稳定 Identifier。
	SpeciesID snowflake.ID
	// InheritsFromCreatureID 是可选的资料继承来源。
	InheritsFromCreatureID *snowflake.ID
	// Height 是可选身高资料。
	Height *int32
	// Weight 是可选体重资料。
	Weight *int32
	// BaseExperience 是可选基础经验值。
	BaseExperience *int32
	// CaptureRate 是该具体形态的捕获率整数参数，取值为 0 至 255。
	CaptureRate *int32
	// HatchCycles 是该具体形态孵化所需的基础周期数。
	HatchCycles *int32
	// GenderRatio 是八分份性别比率。
	GenderRatio GenderRatio
	// DefaultForm 表示该记录是否为 Species 的默认 Creature。
	DefaultForm bool
	// Enabled 表示该记录能否进入新的 Team 与 Battle。
	Enabled bool
}

// UpdateCreatureCommand 是使用乐观锁更新单条 Creature 的完整管理命令。
type UpdateCreatureCommand struct {
	// GameDataWriteContext 携带维护窗口、修订、操作者和幂等事实。
	administration.GameDataWriteContext
	// ID 是待更新 Creature 的稳定 Identifier。
	ID snowflake.ID
	// ExpectedVersion 是调用方最近读取到的记录版本。
	ExpectedVersion int64
	// Code 是更新后的稳定英文机器编码。
	Code string
	// Name 是更新后的简体中文展示名称。
	Name string
	// SpeciesID 是更新后的所属 Species Identifier。
	SpeciesID snowflake.ID
	// InheritsFromCreatureID 是更新后的可选资料继承来源。
	InheritsFromCreatureID *snowflake.ID
	// Height 是更新后的可选身高资料。
	Height *int32
	// Weight 是更新后的可选体重资料。
	Weight *int32
	// BaseExperience 是更新后的可选基础经验值。
	BaseExperience *int32
	// CaptureRate 是更新后的捕获率整数参数，取值为 0 至 255。
	CaptureRate *int32
	// HatchCycles 是更新后的孵化基础周期数。
	HatchCycles *int32
	// GenderRatio 是更新后的八分份性别比率。
	GenderRatio GenderRatio
	// DefaultForm 表示该记录是否为 Species 的默认 Creature。
	DefaultForm bool
	// Enabled 表示该记录能否进入新的 Team 与 Battle。
	Enabled bool
}

// CreateSpeciesRecord 是存储事务使用的已校验 Species 创建事实。
type CreateSpeciesRecord struct {
	administration.GameDataWriteContext
	// Species 是等待写入的完整记录。
	Species ManagedSpecies
	// At 是资料、审计和修订共同使用的 UTC 时间。
	At time.Time
}

// UpdateSpeciesRecord 是存储事务使用的已校验 Species 更新事实。
type UpdateSpeciesRecord struct {
	administration.GameDataWriteContext
	// Species 是等待写入的完整记录。
	Species ManagedSpecies
	// ExpectedVersion 是 SQL 条件使用的乐观版本。
	ExpectedVersion int64
	// At 是资料、审计和修订共同使用的 UTC 时间。
	At time.Time
}

// CreateCreatureRecord 是存储事务使用的已校验 Creature 创建事实。
type CreateCreatureRecord struct {
	administration.GameDataWriteContext
	// Creature 是等待写入的完整记录。
	Creature ManagedCreature
	// At 是资料、审计和修订共同使用的 UTC 时间。
	At time.Time
}

// UpdateCreatureRecord 是存储事务使用的已校验 Creature 更新事实。
type UpdateCreatureRecord struct {
	administration.GameDataWriteContext
	// Creature 是等待写入的完整记录。
	Creature ManagedCreature
	// ExpectedVersion 是 SQL 条件使用的乐观版本。
	ExpectedVersion int64
	// At 是资料、审计和修订共同使用的 UTC 时间。
	At time.Time
}

// ReplaceRelationsRecord 是存储事务使用的已校验单 Creature 关系替换事实。
type ReplaceRelationsRecord struct {
	administration.GameDataWriteContext
	// CreatureID 是本次写入唯一允许修改的 Creature。
	CreatureID snowflake.ID
	// Relations 是已补齐身份和版本的完整关系集合。
	Relations CreatureRelations
	// At 是资料、审计和修订共同使用的 UTC 时间。
	At time.Time
}

// ManagementWriter 是单次 Creature 资料管理事务内的最小写入接口。
type ManagementWriter interface {
	CreateSpecies(context.Context, CreateSpeciesRecord) (ManagedSpecies, error)
	UpdateSpecies(context.Context, UpdateSpeciesRecord) (ManagedSpecies, error)
	CreateCreature(context.Context, CreateCreatureRecord) (ManagedCreature, error)
	UpdateCreature(context.Context, UpdateCreatureRecord) (ManagedCreature, error)
	ReplaceCreatureRelations(context.Context, ReplaceRelationsRecord) (CreatureRelations, error)
}

// ManagementStore 是 Species 与 Creature 记录级管理的持久化边界。
type ManagementStore interface {
	GetReferenceOptions(context.Context) (ReferenceOptions, error)
	ListSpecies(context.Context, SpeciesListQuery) (SpeciesPage, error)
	GetSpecies(context.Context, snowflake.ID) (ManagedSpecies, error)
	ListCreatures(context.Context, CreatureListQuery) (CreaturePage, error)
	GetCreature(context.Context, snowflake.ID) (ManagedCreature, error)
	GetCreatureRelations(context.Context, snowflake.ID) (CreatureRelations, error)
	WithinCreatureData(context.Context, func(ManagementWriter) error) error
}

// AdministrationService 编排 Species 与 Creature 的记录级查询和写入。
type AdministrationService struct {
	store ManagementStore
	newID snowflake.Source
	now   func() time.Time
}

// NewAdministrationService 使用显式依赖创建 Creature 资料管理服务。
func NewAdministrationService(store ManagementStore, newID snowflake.Source, now func() time.Time) *AdministrationService {
	return &AdministrationService{store: store, newID: newID, now: now}
}

// GetReferenceOptions 返回 Species 表单所需的小型引用资料，不读取十万级关系表。
func (s *AdministrationService) GetReferenceOptions(ctx context.Context) (ReferenceOptions, error) {
	return s.store.GetReferenceOptions(ctx)
}

// ListSpecies 返回经过规范化的 Species 分页结果。
func (s *AdministrationService) ListSpecies(ctx context.Context, query SpeciesListQuery) (SpeciesPage, error) {
	query.Page, query.PageSize, query.Q = normalizePage(query.Page, query.PageSize, query.Q)
	if !validPage(query.Page, query.PageSize, query.Q) {
		return SpeciesPage{}, ErrInvalidCreatureMetadata
	}
	return s.store.ListSpecies(ctx, query)
}

// GetSpecies 返回指定稳定身份的 Species。
func (s *AdministrationService) GetSpecies(ctx context.Context, id snowflake.ID) (ManagedSpecies, error) {
	if id == snowflake.ID(0) {
		return ManagedSpecies{}, ErrInvalidCreatureMetadata
	}
	return s.store.GetSpecies(ctx, id)
}

// CreateSpecies 创建版本为一的 Species。
func (s *AdministrationService) CreateSpecies(ctx context.Context, command CreateSpeciesCommand) (ManagedSpecies, error) {
	command.GameDataWriteContext = command.Normalize()
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return ManagedSpecies{}, idErr
	}
	command.Species.ID, command.Species.Code, command.Species.Name = id, strings.TrimSpace(command.Species.Code), strings.TrimSpace(command.Species.Name)
	cleanSpeciesCopy(&command.Species)
	value := ManagedSpecies{Species: command.Species, Version: 1}
	if !command.Valid() || !validManagedSpecies(value) {
		return ManagedSpecies{}, ErrInvalidCreatureMetadata
	}
	var created ManagedSpecies
	err := s.store.WithinCreatureData(ctx, func(writer ManagementWriter) error {
		var err error
		created, err = writer.CreateSpecies(ctx, CreateSpeciesRecord{GameDataWriteContext: command.GameDataWriteContext, Species: value, At: s.now().UTC()})
		return err
	})
	return created, err
}

// UpdateSpecies 使用乐观版本完整更新一条 Species。
func (s *AdministrationService) UpdateSpecies(ctx context.Context, command UpdateSpeciesCommand) (ManagedSpecies, error) {
	command.GameDataWriteContext = command.Normalize()
	command.Species.Code, command.Species.Name = strings.TrimSpace(command.Species.Code), strings.TrimSpace(command.Species.Name)
	cleanSpeciesCopy(&command.Species)
	value := ManagedSpecies{Species: command.Species, Version: command.ExpectedVersion + 1}
	if !command.Valid() || command.ExpectedVersion < 1 || !validManagedSpecies(value) {
		return ManagedSpecies{}, ErrInvalidCreatureMetadata
	}
	var updated ManagedSpecies
	err := s.store.WithinCreatureData(ctx, func(writer ManagementWriter) error {
		var err error
		updated, err = writer.UpdateSpecies(ctx, UpdateSpeciesRecord{GameDataWriteContext: command.GameDataWriteContext, Species: value, ExpectedVersion: command.ExpectedVersion, At: s.now().UTC()})
		return err
	})
	return updated, err
}

// ListCreatures 返回经过规范化的 Creature 分页结果。
func (s *AdministrationService) ListCreatures(ctx context.Context, query CreatureListQuery) (CreaturePage, error) {
	query.Page, query.PageSize, query.Q = normalizePage(query.Page, query.PageSize, query.Q)
	if !validPage(query.Page, query.PageSize, query.Q) || (query.SpeciesID != nil && *query.SpeciesID == snowflake.ID(0)) {
		return CreaturePage{}, ErrInvalidCreatureMetadata
	}
	return s.store.ListCreatures(ctx, query)
}

// GetCreature 返回指定稳定身份的 Creature。
func (s *AdministrationService) GetCreature(ctx context.Context, id snowflake.ID) (ManagedCreature, error) {
	if id == snowflake.ID(0) {
		return ManagedCreature{}, ErrInvalidCreatureMetadata
	}
	return s.store.GetCreature(ctx, id)
}

// GetCreatureRelations 返回一个 Creature 的形态与全部可管理关系记录。
func (s *AdministrationService) GetCreatureRelations(ctx context.Context, id snowflake.ID) (CreatureRelations, error) {
	if id == snowflake.ID(0) {
		return CreatureRelations{}, ErrInvalidCreatureMetadata
	}
	return s.store.GetCreatureRelations(ctx, id)
}

// CreateCreature 创建版本为一的 Creature。
func (s *AdministrationService) CreateCreature(ctx context.Context, command CreateCreatureCommand) (ManagedCreature, error) {
	command.GameDataWriteContext = command.Normalize()
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return ManagedCreature{}, idErr
	}
	value := managedCreatureFromCreate(id, command)
	if !command.Valid() || !validManagedCreature(value) {
		return ManagedCreature{}, ErrInvalidCreatureMetadata
	}
	var created ManagedCreature
	err := s.store.WithinCreatureData(ctx, func(writer ManagementWriter) error {
		var err error
		created, err = writer.CreateCreature(ctx, CreateCreatureRecord{GameDataWriteContext: command.GameDataWriteContext, Creature: value, At: s.now().UTC()})
		return err
	})
	return created, err
}

// UpdateCreature 使用乐观版本完整更新一条 Creature。
func (s *AdministrationService) UpdateCreature(ctx context.Context, command UpdateCreatureCommand) (ManagedCreature, error) {
	command.GameDataWriteContext = command.Normalize()
	value := managedCreatureFromUpdate(command)
	if !command.Valid() || command.ExpectedVersion < 1 || !validManagedCreature(value) {
		return ManagedCreature{}, ErrInvalidCreatureMetadata
	}
	var updated ManagedCreature
	err := s.store.WithinCreatureData(ctx, func(writer ManagementWriter) error {
		var err error
		updated, err = writer.UpdateCreature(ctx, UpdateCreatureRecord{GameDataWriteContext: command.GameDataWriteContext, Creature: value, ExpectedVersion: command.ExpectedVersion, At: s.now().UTC()})
		return err
	})
	return updated, err
}

// ReplaceRelations 原子替换一个 Creature 的关系集合，不读取或覆盖其它 Creature。
func (s *AdministrationService) ReplaceRelations(ctx context.Context, command ReplaceRelationsCommand) (CreatureRelations, error) {
	command.GameDataWriteContext = command.Normalize()
	prepared, prepareErr := s.prepareRelations(ctx, command.CreatureID, &command.Relations)
	if !command.Valid() || command.CreatureID == snowflake.ID(0) || !prepared {
		if prepareErr != nil {
			return CreatureRelations{}, prepareErr
		}
		return CreatureRelations{}, ErrInvalidCreatureMetadata
	}
	var result CreatureRelations
	err := s.store.WithinCreatureData(ctx, func(writer ManagementWriter) error {
		var err error
		result, err = writer.ReplaceCreatureRelations(ctx, ReplaceRelationsRecord{GameDataWriteContext: command.GameDataWriteContext, CreatureID: command.CreatureID, Relations: command.Relations, At: s.now().UTC()})
		return err
	})
	return result, err
}

func (s *AdministrationService) prepareRelations(ctx context.Context, creatureID snowflake.ID, relations *CreatureRelations) (bool, error) {
	if len(relations.Forms) > 100 || len(relations.Stats) > 100 || len(relations.SkillLearns) > 10_000 || len(relations.Abilities) > 100 || len(relations.HeldItems) > 1_000 || len(relations.Skins) > 1_000 || len(relations.Evolutions) > 100 {
		return false, nil
	}
	var sourceErr error
	prepare := func(id, relationCreatureID *snowflake.ID, version *int64) bool {
		if *relationCreatureID != snowflake.ID(0) && *relationCreatureID != creatureID {
			return false
		}
		*relationCreatureID = creatureID
		if *id == snowflake.ID(0) {
			generated, err := s.newID.Next(ctx)
			if err != nil {
				sourceErr = err
				return false
			}
			*id, *version = generated, 0
			return true
		}
		return *version >= 1
	}
	for index := range relations.Forms {
		value := &relations.Forms[index]
		value.Code, value.Name, value.FormName = strings.TrimSpace(value.Code), strings.TrimSpace(value.Name), clean(value.FormName)
		if !prepare(&value.ID, &value.CreatureID, &value.Version) || !validCodeName(value.Code, value.Name, 120) || len(value.ElementIDs) == 0 || !refsNonNil(value.ElementIDs) {
			return false, sourceErr
		}
	}
	for index := range relations.Stats {
		value := &relations.Stats[index]
		if !prepare(&value.ID, &value.CreatureID, &value.Version) || value.StatID == snowflake.ID(0) || value.BaseValue <= 0 || value.Effort == nil || *value.Effort < 0 || *value.Effort > 3 {
			return false, sourceErr
		}
	}
	for index := range relations.SkillLearns {
		value := &relations.SkillLearns[index]
		if !prepare(&value.ID, &value.CreatureID, &value.Version) || value.SkillID == snowflake.ID(0) || value.LearnMethodID == snowflake.ID(0) || value.LevelLearnedAt < 0 {
			return false, sourceErr
		}
	}
	for index := range relations.Abilities {
		value := &relations.Abilities[index]
		if !prepare(&value.ID, &value.CreatureID, &value.Version) || value.AbilityID == snowflake.ID(0) || value.Slot < 1 {
			return false, sourceErr
		}
	}
	for index := range relations.HeldItems {
		value := &relations.HeldItems[index]
		if !prepare(&value.ID, &value.CreatureID, &value.Version) || value.ItemID == snowflake.ID(0) || value.Rarity < 0 {
			return false, sourceErr
		}
	}
	for index := range relations.Skins {
		value := &relations.Skins[index]
		value.Code, value.Name = strings.TrimSpace(value.Code), strings.TrimSpace(value.Name)
		if !prepare(&value.ID, &value.CreatureID, &value.Version) || !validCodeName(value.Code, value.Name, 120) {
			return false, sourceErr
		}
	}
	for index := range relations.Evolutions {
		value := &relations.Evolutions[index]
		value.ConditionText = strings.TrimSpace(value.ConditionText)
		if !prepare(&value.ID, &value.FromCreatureID, &value.Version) || !validEvolution(*value) {
			return false, sourceErr
		}
	}
	return true, nil
}

func validEvolution(value Evolution) bool {
	if value.ToCreatureID == snowflake.ID(0) || value.ToCreatureID == value.FromCreatureID || value.ConditionText == "" || len([]rune(value.ConditionText)) > 2000 || negative(value.MinimumLevel) || negative(value.MinimumFriendship) {
		return false
	}
	switch value.TriggerType {
	case EvolutionTriggerLevel, EvolutionTriggerItem, EvolutionTriggerTrade, EvolutionTriggerFriendship, EvolutionTriggerSkill, EvolutionTriggerBreeding, EvolutionTriggerSpecial:
	default:
		return false
	}
	if value.MinimumLevel != nil && *value.MinimumLevel == 0 {
		return false
	}
	if value.TimeOfDay != nil && *value.TimeOfDay != "day" && *value.TimeOfDay != "night" && *value.TimeOfDay != "dusk" {
		return false
	}
	return value.Gender == nil || *value.Gender == "male" || *value.Gender == "female"
}

func normalizePage(page, pageSize int32, q string) (int32, int32, string) {
	if page == 0 {
		page = 1
	}
	if pageSize == 0 {
		pageSize = 20
	}
	return page, pageSize, strings.TrimSpace(q)
}

func validPage(page, pageSize int32, q string) bool {
	return page >= 1 && page <= 1_000_000 && pageSize >= 1 && pageSize <= 100 && len([]rune(q)) <= 120
}

func validManagedSpecies(value ManagedSpecies) bool {
	return value.ID != snowflake.ID(0) && value.Version > 0 && value.NationalDexNumber > 0 && validCodeName(value.Code, value.Name, 120) && len(value.EggGroupIDs) <= maximumCreatureMetadataReferences && refsNonNil(value.EggGroupIDs) && optional(value.Genus, 200) && optional(value.PokedexEntry, 2000) && optional(value.Description, 4000) && optional(value.Profile, 4000) && optional(value.DesignOrigin, 4000) && optional(value.Trivia, 4000)
}

// cleanSpeciesCopy 统一清理 Species 的分类、图鉴条目和四类独立参考文案。
// 空白文本转换为 nil，使领域对象、SQL NULL 与 API 空字符串保持同一缺失语义。
func cleanSpeciesCopy(value *Species) {
	value.Genus = clean(value.Genus)
	value.PokedexEntry = clean(value.PokedexEntry)
	value.Description = clean(value.Description)
	value.Profile = clean(value.Profile)
	value.DesignOrigin = clean(value.DesignOrigin)
	value.Trivia = clean(value.Trivia)
}

func validManagedCreature(value ManagedCreature) bool {
	return value.ID != snowflake.ID(0) && value.SpeciesID != snowflake.ID(0) && value.Version > 0 && stablecode.Valid(value.Code) && value.Name != "" && len([]rune(value.Name)) <= 120 && validGenderRatio(value.GenderRatio) && !negative(value.Height) && !negative(value.Weight) && !negative(value.BaseExperience) && !negative(value.CaptureRate) && (value.CaptureRate == nil || *value.CaptureRate <= 255) && !negative(value.HatchCycles) && (value.InheritsFromCreatureID == nil || *value.InheritsFromCreatureID != value.ID)
}

func managedCreatureFromCreate(id snowflake.ID, command CreateCreatureCommand) ManagedCreature {
	return ManagedCreature{Creature: Creature{ID: id, Code: strings.TrimSpace(command.Code), Name: strings.TrimSpace(command.Name), SpeciesID: command.SpeciesID, InheritsFromCreatureID: command.InheritsFromCreatureID, Height: command.Height, Weight: command.Weight, BaseExperience: command.BaseExperience, CaptureRate: command.CaptureRate, HatchCycles: command.HatchCycles, GenderRatio: &command.GenderRatio, DefaultForm: command.DefaultForm, Enabled: command.Enabled}, Version: 1}
}

func managedCreatureFromUpdate(command UpdateCreatureCommand) ManagedCreature {
	return ManagedCreature{Creature: Creature{ID: command.ID, Code: strings.TrimSpace(command.Code), Name: strings.TrimSpace(command.Name), SpeciesID: command.SpeciesID, InheritsFromCreatureID: command.InheritsFromCreatureID, Height: command.Height, Weight: command.Weight, BaseExperience: command.BaseExperience, CaptureRate: command.CaptureRate, HatchCycles: command.HatchCycles, GenderRatio: &command.GenderRatio, DefaultForm: command.DefaultForm, Enabled: command.Enabled}, Version: command.ExpectedVersion + 1}
}
