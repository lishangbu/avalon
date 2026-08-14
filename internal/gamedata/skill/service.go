// Package skill 定义 实时游戏资料 中技能主体资料的独立命令与查询边界。
package skill

import (
	"context"
	"errors"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/battlerules"
	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
)

var (
	// ErrInvalidSkill 表示技能资料或管理写入上下文未通过边界校验。
	ErrInvalidSkill = errors.New("技能资料无效")
	// ErrSkillNotFound 表示实时资料中不存在指定技能资料。
	ErrSkillNotFound = errors.New("技能资料不存在")
	// ErrSkillVersionConflict 表示技能资料已被其他管理写入修改。
	ErrSkillVersionConflict = errors.New("技能资料版本冲突")
	// ErrSkillCodeConflict 表示实时资料中已有资料使用相同稳定编码。
	ErrSkillCodeConflict = errors.New("技能资料编码已存在")
	// ErrSkillDependencyNotFound 表示属性或伤害分类没有属于同一实时资料的修订。
	ErrSkillDependencyNotFound = errors.New("技能资料依赖不存在")
	// ErrSkillReferenced 表示其他 实时资料 资料仍然引用目标技能资料。
	ErrSkillReferenced = errors.New("技能资料仍被引用")
)

// OptionalValues 保存技能主体允许为空的资料引用和战斗数值。
type OptionalValues struct {
	ElementID     *snowflake.ID
	DamageClassID *snowflake.ID
	Accuracy      *int32
	Power         *int32
	PP            *int32
	EffectChance  *int32
	// Effect 是技能完整机制说明，不参与结构化战斗规则编译。
	Effect *string
	// ShortEffect 是技能机制的简短说明。
	ShortEffect *string
	// Description 是技能面向玩家展示的简体中文说明。
	Description *string
}

// Skill 是管理端读取和写入的一条 实时资料 技能主体资料。
type Skill struct {
	ID snowflake.ID
	OptionalValues
	Code     string
	Name     string
	Priority int32
	// Rules 是按技能使用时机组织并经过严格校验的战斗规则文档。
	Rules   battlerules.Skill
	Enabled bool
	Version int64
}

// Sort 声明管理端技能列表允许使用的稳定排序。
type Sort string

const (
	// SortCodeAscending 按稳定编码升序排列，并使用 ID 打破平局。
	SortCodeAscending Sort = "code_asc"
	// SortCodeDescending 按稳定编码降序排列，并使用 ID 打破平局。
	SortCodeDescending Sort = "code_desc"
	// SortNameAscending 按简体中文名称升序排列，并使用 ID 打破平局。
	SortNameAscending Sort = "name_asc"
	// SortNameDescending 按简体中文名称降序排列，并使用 ID 打破平局。
	SortNameDescending Sort = "name_desc"
	// SortPriorityAscending 按行动优先级升序排列，并使用 ID 打破平局。
	SortPriorityAscending Sort = "priority_asc"
	// SortPriorityDescending 按行动优先级降序排列，并使用 ID 打破平局。
	SortPriorityDescending Sort = "priority_desc"
)

// ListQuery 是实时资料 技能列表的显式分页、筛选和排序条件。
type ListQuery struct {
	Page          int32
	PageSize      int32
	Q             string
	Code          string
	Name          string
	ElementID     *snowflake.ID
	DamageClassID *snowflake.ID
	Accuracy      *int32
	Power         *int32
	PP            *int32
	Priority      *int32
	EffectChance  *int32
	Enabled       *bool
	Sort          Sort
}

// Page 是管理端技能主体资料的有界分页结果。
type Page struct {
	Items    []Skill
	Total    int64
	Page     int32
	PageSize int32
}

// Change 表示更新请求对单个可空字段的省略、清空或替换意图。
type Change[T any] struct {
	Specified bool
	Value     *T
}

// OptionalChanges 保存技能主体全部可空字段彼此独立的更新意图。
type OptionalChanges struct {
	ElementID     Change[snowflake.ID]
	DamageClassID Change[snowflake.ID]
	Accuracy      Change[int32]
	Power         Change[int32]
	PP            Change[int32]
	EffectChance  Change[int32]
	Effect        Change[string]
	ShortEffect   Change[string]
	Description   Change[string]
}

// CreateCommand 包含创建技能主体资料所需的业务字段和管理写入上下文。
type CreateCommand struct {
	administration.GameDataWriteContext
	OptionalValues
	Code     string
	Name     string
	Priority int32
	Rules    battlerules.Skill
	Enabled  bool
}

// CreateRecord 是存储层原子创建资料、审计和幂等响应所需的完整事实。
type CreateRecord struct {
	administration.GameDataWriteContext
	Skill     Skill
	CreatedAt time.Time
}

// UpdateCommand 使用字段变更意图和预期版本更新一条技能主体资料。
type UpdateCommand struct {
	administration.GameDataWriteContext
	SkillID         snowflake.ID
	ExpectedVersion int64
	Code            string
	Name            string
	Changes         OptionalChanges
	Priority        int32
	Rules           battlerules.Skill
	Enabled         bool
}

// UpdateRecord 是存储层原子更新资料、审计记录和幂等响应所需的完整事实。
type UpdateRecord struct {
	administration.GameDataWriteContext
	Skill           Skill
	Changes         OptionalChanges
	ExpectedVersion int64
	UpdatedAt       time.Time
}

// DisableCommand 使用预期版本禁用 实时资料中未被引用的一条技能主体资料。
type DisableCommand struct {
	administration.GameDataWriteContext
	SkillID         snowflake.ID
	ExpectedVersion int64
}

// DisableRecord 是存储层原子禁用资料、审计记录和幂等响应所需的完整事实。
type DisableRecord struct {
	administration.GameDataWriteContext
	SkillID         snowflake.ID
	ExpectedVersion int64
	DisabledAt      time.Time
}

// Writer 是一次技能主体资料管理事务内使用的最小写入边界。
type Writer interface {
	Create(context.Context, CreateRecord) (Skill, error)
	Update(context.Context, UpdateRecord) (Skill, error)
	Disable(context.Context, DisableRecord) error
}

// SkillReader 返回指定技能领域对象。
type SkillReader interface {
	GetSkill(context.Context, snowflake.ID) (Skill, error)
}

// SkillQuery 返回技能资料分页管理投影。
type SkillQuery interface {
	ListSkills(context.Context, ListQuery) (Page, error)
}

// SkillRepository 提供由应用服务划定范围的技能资料事务写入边界。
type SkillRepository interface {
	WithinSkill(context.Context, func(Writer) error) error
}

// Service 编排技能主体资料的独立校验、身份生成和持久化命令。
type Service struct {
	// reader 返回指定技能领域对象。
	reader SkillReader
	// query 返回技能分页管理投影。
	query      SkillQuery
	repository SkillRepository
	newID      snowflake.Source
	now        func() time.Time
}

// NewService 使用显式依赖创建技能主体资料应用服务。
func NewService(reader SkillReader, query SkillQuery, repository SkillRepository, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{reader: reader, query: query, repository: repository, newID: newID, now: now}
}

// Get 读取当前实时资料中指定稳定身份的技能主体资料。
func (s *Service) Get(ctx context.Context, skillID snowflake.ID) (Skill, error) {
	if skillID == snowflake.ID(0) {
		return Skill{}, ErrInvalidSkill
	}
	return s.reader.GetSkill(ctx, skillID)
}

// List 返回当前实时资料中经过显式筛选和稳定排序的技能主体资料页。
func (s *Service) List(ctx context.Context, query ListQuery) (Page, error) {
	if query.Page == 0 {
		query.Page = 1
	}
	if query.PageSize == 0 {
		query.PageSize = 20
	}
	if query.Sort == "" {
		query.Sort = SortCodeAscending
	}
	query.Q = strings.TrimSpace(query.Q)
	query.Code = strings.TrimSpace(query.Code)
	query.Name = strings.TrimSpace(query.Name)
	if query.Page < 1 || query.Page > 1_000_000 || query.PageSize < 1 || query.PageSize > 100 ||
		len([]rune(query.Q)) > 120 || len([]rune(query.Name)) > 120 || !validSort(query.Sort) ||
		(query.Code != "" && !stablecode.Valid(query.Code)) || !validIdentifier(query.ElementID) ||
		!validIdentifier(query.DamageClassID) || !validAccuracy(query.Accuracy) ||
		!validNonNegative(query.Power) || !validPositive(query.PP) || !validPercentage(query.EffectChance) {
		return Page{}, ErrInvalidSkill
	}
	return s.query.ListSkills(ctx, query)
}

func validSort(sort Sort) bool {
	switch sort {
	case SortCodeAscending, SortCodeDescending, SortNameAscending, SortNameDescending,
		SortPriorityAscending, SortPriorityDescending:
		return true
	default:
		return false
	}
}

// Create 在当前实时资料中创建版本为 1 的技能主体资料。
func (s *Service) Create(ctx context.Context, command CreateCommand) (Skill, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.OptionalValues = normalizeOptionalValues(command.OptionalValues)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || !validRequiredFields(command.Code, command.Name) ||
		!validOptionalValues(command.OptionalValues) || !validRules(command.Rules) {
		return Skill{}, ErrInvalidSkill
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Skill{}, idErr
	}
	value := Skill{
		ID: id, OptionalValues: command.OptionalValues, Code: command.Code, Name: command.Name,
		Priority: command.Priority, Rules: command.Rules, Enabled: command.Enabled, Version: 1,
	}
	var created Skill
	err := s.repository.WithinSkill(ctx, func(writer Writer) error {
		var createErr error
		created, createErr = writer.Create(ctx, CreateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Skill: value, CreatedAt: s.now().UTC(),
		})
		return createErr
	})
	if err != nil {
		return Skill{}, err
	}
	return created, nil
}

// Update 使用乐观版本更新 实时资料中的技能主体，并保留各个未提供的可空字段。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (Skill, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.Changes = normalizeOptionalChanges(command.Changes)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.SkillID == snowflake.ID(0) || command.ExpectedVersion < 1 ||
		!validRequiredFields(command.Code, command.Name) || !validOptionalChanges(command.Changes) || !validRules(command.Rules) {
		return Skill{}, ErrInvalidSkill
	}
	value := Skill{
		ID: command.SkillID, Code: command.Code, Name: command.Name,
		Priority: command.Priority, Rules: command.Rules, Enabled: command.Enabled, Version: command.ExpectedVersion + 1,
	}
	var updated Skill
	err := s.repository.WithinSkill(ctx, func(writer Writer) error {
		var updateErr error
		updated, updateErr = writer.Update(ctx, UpdateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Skill: value, Changes: command.Changes,
			ExpectedVersion: command.ExpectedVersion, UpdatedAt: s.now().UTC(),
		})
		return updateErr
	})
	if err != nil {
		return Skill{}, err
	}
	return updated, nil
}

// Delete 使用乐观版本禁用当前实时资料中未被引用的技能主体资料。
func (s *Service) Disable(ctx context.Context, command DisableCommand) error {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.SkillID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return ErrInvalidSkill
	}
	return s.repository.WithinSkill(ctx, func(writer Writer) error {
		return writer.Disable(ctx, DisableRecord{
			GameDataWriteContext: command.GameDataWriteContext, SkillID: command.SkillID,
			ExpectedVersion: command.ExpectedVersion, DisabledAt: s.now().UTC(),
		})
	})
}

func validRequiredFields(code, name string) bool {
	return stablecode.Valid(code) && name != "" && len([]rune(name)) <= 120
}

func validOptionalValues(values OptionalValues) bool {
	return validIdentifier(values.ElementID) && validIdentifier(values.DamageClassID) &&
		validAccuracy(values.Accuracy) && validNonNegative(values.Power) && validPositive(values.PP) &&
		validPercentage(values.EffectChance) && validText(values.Effect, 20_000) &&
		validText(values.ShortEffect, 500) && validText(values.Description, 500)
}

func validOptionalChanges(changes OptionalChanges) bool {
	return validChange(changes.ElementID, validIdentifier) && validChange(changes.DamageClassID, validIdentifier) &&
		validChange(changes.Accuracy, validAccuracy) && validChange(changes.Power, validNonNegative) &&
		validChange(changes.PP, validPositive) && validChange(changes.EffectChance, validPercentage) &&
		validChange(changes.Effect, func(value *string) bool { return validText(value, 20_000) }) &&
		validChange(changes.ShortEffect, func(value *string) bool { return validText(value, 500) }) &&
		validChange(changes.Description, func(value *string) bool { return validText(value, 500) })
}

func normalizeOptionalValues(values OptionalValues) OptionalValues {
	values.Effect = normalizeText(values.Effect)
	values.ShortEffect = normalizeText(values.ShortEffect)
	values.Description = normalizeText(values.Description)
	return values
}

func normalizeOptionalChanges(changes OptionalChanges) OptionalChanges {
	if changes.Effect.Specified {
		changes.Effect.Value = normalizeText(changes.Effect.Value)
	}
	if changes.ShortEffect.Specified {
		changes.ShortEffect.Value = normalizeText(changes.ShortEffect.Value)
	}
	if changes.Description.Specified {
		changes.Description.Value = normalizeText(changes.Description.Value)
	}
	return changes
}

func normalizeText(value *string) *string {
	if value == nil {
		return nil
	}
	trimmed := strings.TrimSpace(*value)
	return &trimmed
}

func validText(value *string, maximum int) bool {
	return value == nil || *value != "" && len([]rune(*value)) <= maximum
}

func validRules(rules battlerules.Skill) bool {
	_, valid := rules.Values()
	return valid
}

func validChange[T any](change Change[T], validate func(*T) bool) bool {
	return !change.Specified || validate(change.Value)
}

func validIdentifier(value *snowflake.ID) bool {
	return value == nil || *value != snowflake.ID(0)
}

func validAccuracy(value *int32) bool {
	return value == nil || *value >= 1 && *value <= 100
}

func validNonNegative(value *int32) bool {
	return value == nil || *value >= 0
}

func validPositive(value *int32) bool {
	return value == nil || *value > 0
}

func validPercentage(value *int32) bool {
	return value == nil || *value >= 0 && *value <= 100
}
