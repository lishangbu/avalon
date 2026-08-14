// Package skilldamageclass 定义 实时游戏资料 中技能伤害分类的独立命令与查询边界。
package skilldamageclass

import (
	"context"
	"errors"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
)

var (
	// ErrInvalidSkillDamageClass 表示技能伤害分类或管理写入上下文未通过边界校验。
	ErrInvalidSkillDamageClass = errors.New("技能伤害分类无效")
	// ErrSkillDamageClassNotFound 表示实时资料中不存在指定技能伤害分类。
	ErrSkillDamageClassNotFound = errors.New("技能伤害分类不存在")
	// ErrSkillDamageClassVersionConflict 表示技能伤害分类已被其他管理写入修改。
	ErrSkillDamageClassVersionConflict = errors.New("技能伤害分类版本冲突")
	// ErrSkillDamageClassCodeConflict 表示实时资料中已有资料使用相同稳定编码。
	ErrSkillDamageClassCodeConflict = errors.New("技能伤害分类编码已存在")
	// ErrSkillDamageClassReferenced 表示其他 实时资料 资料仍然引用目标技能伤害分类。
	ErrSkillDamageClassReferenced = errors.New("技能伤害分类仍被引用")
)

// DamageClass 是管理端读取和写入的一条 实时资料 技能伤害分类。
type DamageClass struct {
	ID          snowflake.ID
	Code        string
	Name        string
	Description *string
	SortOrder   int32
	Enabled     bool
	Version     int64
}

// Sort 声明管理端技能伤害分类列表允许使用的稳定排序。
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
	// SortOrderAscending 按排序值升序排列，并使用 ID 打破平局。
	SortOrderAscending Sort = "sort_order_asc"
	// SortOrderDescending 按排序值降序排列，并使用 ID 打破平局。
	SortOrderDescending Sort = "sort_order_desc"
)

// ListQuery 是实时资料 技能伤害分类列表的显式分页、筛选和排序条件。
type ListQuery struct {
	Page        int32
	PageSize    int32
	Q           string
	Code        string
	Name        string
	Description string
	SortOrder   *int32
	Enabled     *bool
	Sort        Sort
}

// Page 是管理端技能伤害分类的有界分页结果。
type Page struct {
	Items    []DamageClass
	Total    int64
	Page     int32
	PageSize int32
}

// CreateCommand 包含创建技能伤害分类所需的业务字段和管理写入上下文。
type CreateCommand struct {
	administration.GameDataWriteContext
	Code        string
	Name        string
	Description *string
	SortOrder   int32
	Enabled     bool
}

// CreateRecord 是存储层原子创建资料、审计和幂等响应所需的完整事实。
type CreateRecord struct {
	administration.GameDataWriteContext
	DamageClass DamageClass
	CreatedAt   time.Time
}

// DescriptionChange 表示更新请求对可清空说明字段的三态意图。
type DescriptionChange struct {
	Specified bool
	Value     *string
}

// UpdateCommand 使用完整资料表示、说明字段变更意图和预期版本更新一条技能伤害分类。
type UpdateCommand struct {
	administration.GameDataWriteContext
	DamageClassID   snowflake.ID
	ExpectedVersion int64
	Code            string
	Name            string
	Description     DescriptionChange
	SortOrder       int32
	Enabled         bool
}

// UpdateRecord 是存储层原子更新资料、审计记录和幂等响应所需的完整事实。
type UpdateRecord struct {
	administration.GameDataWriteContext
	DamageClass     DamageClass
	Description     DescriptionChange
	ExpectedVersion int64
	UpdatedAt       time.Time
}

// DisableCommand 使用预期版本禁用 实时资料中未被引用的一条技能伤害分类。
type DisableCommand struct {
	administration.GameDataWriteContext
	DamageClassID   snowflake.ID
	ExpectedVersion int64
}

// DisableRecord 是存储层原子禁用资料、审计记录和幂等响应所需的完整事实。
type DisableRecord struct {
	administration.GameDataWriteContext
	DamageClassID   snowflake.ID
	ExpectedVersion int64
	DisabledAt      time.Time
}

// Writer 是一次技能伤害分类管理事务内使用的最小写入边界。
type Writer interface {
	Create(context.Context, CreateRecord) (DamageClass, error)
	Update(context.Context, UpdateRecord) (DamageClass, error)
	Disable(context.Context, DisableRecord) error
}

// SkillDamageClassRepository 提供由应用服务划定范围的技能伤害分类事务执行边界。
type SkillDamageClassRepository interface {
	GetSkillDamageClass(context.Context, snowflake.ID) (DamageClass, error)
	ListSkillDamageClasses(context.Context, ListQuery) (Page, error)
	WithinSkillDamageClass(context.Context, func(Writer) error) error
}

// Service 编排技能伤害分类的独立校验、身份生成和持久化命令。
type Service struct {
	repository SkillDamageClassRepository
	newID      snowflake.Source
	now        func() time.Time
}

// NewService 使用显式依赖创建技能伤害分类应用服务。
func NewService(repository SkillDamageClassRepository, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{repository: repository, newID: newID, now: now}
}

// Get 读取当前实时资料中指定稳定身份的技能伤害分类。
func (s *Service) Get(ctx context.Context, damageClassID snowflake.ID) (DamageClass, error) {
	if damageClassID == snowflake.ID(0) {
		return DamageClass{}, ErrInvalidSkillDamageClass
	}
	return s.repository.GetSkillDamageClass(ctx, damageClassID)
}

// List 返回当前实时资料中经过显式筛选和稳定排序的技能伤害分类页。
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
	query.Description = strings.TrimSpace(query.Description)
	if query.Page < 1 || query.Page > 1_000_000 || query.PageSize < 1 || query.PageSize > 100 ||
		len([]rune(query.Q)) > 80 || len([]rune(query.Name)) > 80 ||
		len([]rune(query.Description)) > 500 || !validSort(query.Sort) ||
		(query.Code != "" && !stablecode.Valid(query.Code)) {
		return Page{}, ErrInvalidSkillDamageClass
	}
	return s.repository.ListSkillDamageClasses(ctx, query)
}

func validSort(sort Sort) bool {
	switch sort {
	case SortCodeAscending, SortCodeDescending, SortNameAscending, SortNameDescending,
		SortOrderAscending, SortOrderDescending:
		return true
	default:
		return false
	}
}

// Create 在当前实时资料中创建版本为 1 的技能伤害分类。
func (s *Service) Create(ctx context.Context, command CreateCommand) (DamageClass, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	description, validDescription := normalizeDescription(command.Description)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || !stablecode.Valid(command.Code) || command.Name == "" ||
		len([]rune(command.Name)) > 80 || !validDescription {
		return DamageClass{}, ErrInvalidSkillDamageClass
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return DamageClass{}, idErr
	}
	value := DamageClass{
		ID: id, Code: command.Code, Name: command.Name, Description: description,
		SortOrder: command.SortOrder, Enabled: command.Enabled, Version: 1,
	}
	var created DamageClass
	err := s.repository.WithinSkillDamageClass(ctx, func(writer Writer) error {
		var createErr error
		created, createErr = writer.Create(ctx, CreateRecord{
			GameDataWriteContext: command.GameDataWriteContext,
			DamageClass:          value,
			CreatedAt:            s.now().UTC(),
		})
		return createErr
	})
	if err != nil {
		return DamageClass{}, err
	}
	return created, nil
}

// Update 使用乐观版本更新 实时资料中的技能伤害分类，并保留未提供的可清空说明字段。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (DamageClass, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	description, validDescription := normalizeDescriptionChange(command.Description)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.DamageClassID == snowflake.ID(0) || command.ExpectedVersion < 1 ||
		!stablecode.Valid(command.Code) || command.Name == "" || len([]rune(command.Name)) > 80 ||
		!validDescription {
		return DamageClass{}, ErrInvalidSkillDamageClass
	}
	value := DamageClass{
		ID: command.DamageClassID, Code: command.Code, Name: command.Name,
		SortOrder: command.SortOrder, Enabled: command.Enabled, Version: command.ExpectedVersion + 1,
	}
	var updated DamageClass
	err := s.repository.WithinSkillDamageClass(ctx, func(writer Writer) error {
		var updateErr error
		updated, updateErr = writer.Update(ctx, UpdateRecord{
			GameDataWriteContext: command.GameDataWriteContext,
			DamageClass:          value,
			Description:          description,
			ExpectedVersion:      command.ExpectedVersion,
			UpdatedAt:            s.now().UTC(),
		})
		return updateErr
	})
	if err != nil {
		return DamageClass{}, err
	}
	return updated, nil
}

// Delete 使用乐观版本禁用当前实时资料中未被引用的技能伤害分类。
func (s *Service) Disable(ctx context.Context, command DisableCommand) error {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.DamageClassID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return ErrInvalidSkillDamageClass
	}
	return s.repository.WithinSkillDamageClass(ctx, func(writer Writer) error {
		return writer.Disable(ctx, DisableRecord{
			GameDataWriteContext: command.GameDataWriteContext,
			DamageClassID:        command.DamageClassID,
			ExpectedVersion:      command.ExpectedVersion,
			DisabledAt:           s.now().UTC(),
		})
	})
}

func normalizeDescription(value *string) (*string, bool) {
	if value == nil {
		return nil, true
	}
	normalized := strings.TrimSpace(*value)
	if normalized == "" {
		return nil, true
	}
	if len([]rune(normalized)) > 500 {
		return nil, false
	}
	return &normalized, true
}

func normalizeDescriptionChange(change DescriptionChange) (DescriptionChange, bool) {
	if !change.Specified {
		return DescriptionChange{}, true
	}
	normalized, valid := normalizeDescription(change.Value)
	return DescriptionChange{Specified: true, Value: normalized}, valid
}
