// Package skillstatchange 定义 实时游戏资料 中技能数值变化的独立命令与查询边界。
package skillstatchange

import (
	"context"
	"errors"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
)

var (
	// ErrInvalidSkillStatChange 表示业务字段或管理写入上下文无效。
	ErrInvalidSkillStatChange = errors.New("技能数值变化无效")
	// ErrSkillStatChangeNotFound 表示实时资料中不存在指定记录。
	ErrSkillStatChangeNotFound = errors.New("技能数值变化不存在")
	// ErrSkillStatChangeVersionConflict 表示记录已被其他管理写入修改。
	ErrSkillStatChangeVersionConflict = errors.New("技能数值变化版本冲突")
	// ErrSkillStatChangeConflict 表示同一技能和数值项已经存在变化记录。
	ErrSkillStatChangeConflict = errors.New("技能数值变化已存在")
	// ErrSkillStatChangeDependencyNotFound 表示技能或数值项不属于实时资料。
	ErrSkillStatChangeDependencyNotFound = errors.New("技能数值变化依赖不存在")
)

// Change 是管理端读取和写入的一条 实时资料 技能数值变化。
type Change struct {
	ID          snowflake.ID
	SkillID     snowflake.ID
	StatID      snowflake.ID
	ChangeValue int32
	Version     int64
}

// Sort 声明列表允许使用的稳定排序。
type Sort string

const (
	// SortSkillAscending 按技能身份升序排列。
	SortSkillAscending Sort = "skill_asc"
	// SortSkillDescending 按技能身份降序排列。
	SortSkillDescending Sort = "skill_desc"
	// SortStatAscending 按数值项身份升序排列。
	SortStatAscending Sort = "stat_asc"
	// SortStatDescending 按数值项身份降序排列。
	SortStatDescending Sort = "stat_desc"
)

// ListQuery 是实时资料 技能数值变化列表的显式分页和筛选条件。
type ListQuery struct {
	Page        int32
	PageSize    int32
	Q           string
	SkillID     *snowflake.ID
	StatID      *snowflake.ID
	ChangeValue *int32
	Sort        Sort
}

// Page 是技能数值变化的有界分页结果。
type Page struct {
	Items    []Change
	Total    int64
	Page     int32
	PageSize int32
}

// CreateCommand 包含创建记录所需的业务字段和管理上下文。
type CreateCommand struct {
	administration.GameDataWriteContext
	SkillID     snowflake.ID
	StatID      snowflake.ID
	ChangeValue int32
}

// CreateRecord 是存储层原子创建、审计和幂等响应所需的事实。
type CreateRecord struct {
	administration.GameDataWriteContext
	Change    Change
	CreatedAt time.Time
}

// UpdateCommand 使用完整业务字段和乐观版本更新记录。
type UpdateCommand struct {
	administration.GameDataWriteContext
	ChangeID        snowflake.ID
	SkillID         snowflake.ID
	StatID          snowflake.ID
	ChangeValue     int32
	ExpectedVersion int64
}

// UpdateRecord 是存储层原子更新、审计和幂等响应所需的事实。
type UpdateRecord struct {
	administration.GameDataWriteContext
	Change          Change
	ExpectedVersion int64
	UpdatedAt       time.Time
}

// DisableCommand 使用乐观版本禁用记录。
type DisableCommand struct {
	administration.GameDataWriteContext
	ChangeID        snowflake.ID
	ExpectedVersion int64
}

// DisableRecord 是存储层原子禁用、审计和幂等响应所需的事实。
type DisableRecord struct {
	administration.GameDataWriteContext
	ChangeID        snowflake.ID
	ExpectedVersion int64
	DisabledAt      time.Time
}

// Writer 是一次管理事务内使用的最小写入边界。
type Writer interface {
	Create(context.Context, CreateRecord) (Change, error)
	Update(context.Context, UpdateRecord) (Change, error)
	Disable(context.Context, DisableRecord) error
}

// Store 提供由应用服务决定范围的事务和查询边界。
type Store interface {
	GetSkillStatChange(context.Context, snowflake.ID) (Change, error)
	ListSkillStatChanges(context.Context, ListQuery) (Page, error)
	WithinSkillStatChange(context.Context, func(Writer) error) error
}

// Service 编排校验、身份生成和持久化命令。
type Service struct {
	store Store
	newID snowflake.Source
	now   func() time.Time
}

// NewService 使用显式依赖创建应用服务。
func NewService(store Store, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{store: store, newID: newID, now: now}
}

// Get 读取实时资料中指定记录。
func (s *Service) Get(ctx context.Context, changeID snowflake.ID) (Change, error) {
	if changeID == snowflake.ID(0) {
		return Change{}, ErrInvalidSkillStatChange
	}
	return s.store.GetSkillStatChange(ctx, changeID)
}

// List 返回显式筛选和稳定排序后的记录页。
func (s *Service) List(ctx context.Context, query ListQuery) (Page, error) {
	if query.Page == 0 {
		query.Page = 1
	}
	if query.PageSize == 0 {
		query.PageSize = 20
	}
	if query.Sort == "" {
		query.Sort = SortSkillAscending
	}
	query.Q = strings.TrimSpace(query.Q)
	if query.Page < 1 || query.Page > 1_000_000 || query.PageSize < 1 || query.PageSize > 100 ||
		len([]rune(query.Q)) > 120 || invalidOptionalIdentifier(query.SkillID) || invalidOptionalIdentifier(query.StatID) ||
		!validOptionalChangeValue(query.ChangeValue) || !validSort(query.Sort) {
		return Page{}, ErrInvalidSkillStatChange
	}
	return s.store.ListSkillStatChanges(ctx, query)
}

// Create 在实时资料中创建版本为 1 的记录。
func (s *Service) Create(ctx context.Context, command CreateCommand) (Change, error) {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || !validFields(command.SkillID, command.StatID, command.ChangeValue) {
		return Change{}, ErrInvalidSkillStatChange
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Change{}, idErr
	}
	value := Change{ID: id, SkillID: command.SkillID, StatID: command.StatID, ChangeValue: command.ChangeValue, Version: 1}
	var created Change
	err := s.store.WithinSkillStatChange(ctx, func(writer Writer) error {
		var createErr error
		created, createErr = writer.Create(ctx, CreateRecord{GameDataWriteContext: command.GameDataWriteContext, Change: value, CreatedAt: s.now().UTC()})
		return createErr
	})
	return created, err
}

// Update 使用完整业务字段和乐观版本更新记录。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (Change, error) {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.ChangeID == snowflake.ID(0) || command.ExpectedVersion < 1 ||
		!validFields(command.SkillID, command.StatID, command.ChangeValue) {
		return Change{}, ErrInvalidSkillStatChange
	}
	value := Change{ID: command.ChangeID, SkillID: command.SkillID, StatID: command.StatID,
		ChangeValue: command.ChangeValue, Version: command.ExpectedVersion + 1}
	var updated Change
	err := s.store.WithinSkillStatChange(ctx, func(writer Writer) error {
		var updateErr error
		updated, updateErr = writer.Update(ctx, UpdateRecord{GameDataWriteContext: command.GameDataWriteContext,
			Change: value, ExpectedVersion: command.ExpectedVersion, UpdatedAt: s.now().UTC()})
		return updateErr
	})
	return updated, err
}

// Disable 使用乐观版本禁用记录。
func (s *Service) Disable(ctx context.Context, command DisableCommand) error {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.ChangeID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return ErrInvalidSkillStatChange
	}
	return s.store.WithinSkillStatChange(ctx, func(writer Writer) error {
		return writer.Disable(ctx, DisableRecord{GameDataWriteContext: command.GameDataWriteContext,
			ChangeID: command.ChangeID, ExpectedVersion: command.ExpectedVersion, DisabledAt: s.now().UTC()})
	})
}

func validFields(skillID, statID snowflake.ID, value int32) bool {
	return skillID != snowflake.ID(0) && statID != snowflake.ID(0) && validChangeValue(value)
}

func validChangeValue(value int32) bool { return value >= -6 && value <= 6 && value != 0 }

func validOptionalChangeValue(value *int32) bool { return value == nil || validChangeValue(*value) }

func invalidOptionalIdentifier(value *snowflake.ID) bool {
	return value != nil && *value == snowflake.ID(0)
}

func validSort(sort Sort) bool {
	return sort == SortSkillAscending || sort == SortSkillDescending ||
		sort == SortStatAscending || sort == SortStatDescending
}
