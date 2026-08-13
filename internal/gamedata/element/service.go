// Package element 定义 实时游戏资料 中属性资料的独立命令与查询边界。
package element

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
	// ErrInvalidElement 表示属性资料或管理写入上下文未通过边界校验。
	ErrInvalidElement = errors.New("属性资料无效")
	// ErrElementNotFound 表示实时资料中不存在指定属性资料。
	ErrElementNotFound = errors.New("属性资料不存在")
	// ErrElementVersionConflict 表示属性资料已被其他管理写入修改。
	ErrElementVersionConflict = errors.New("属性资料版本冲突")
	// ErrElementCodeConflict 表示实时资料中已有资料使用相同稳定编码。
	ErrElementCodeConflict = errors.New("属性资料编码已存在")
	// ErrElementReferenced 表示其他 实时资料 资料仍然引用目标属性资料。
	ErrElementReferenced = errors.New("属性资料仍被引用")
)

// Element 是管理端读取和写入的一条 实时资料 属性资料。
type Element struct {
	ID        snowflake.ID
	Code      string
	Name      string
	SortOrder int32
	Enabled   bool
	Version   int64
}

// Sort 声明管理端属性资料列表允许使用的稳定排序。
type Sort string

const (
	// SortCodeAscending 按稳定编码升序排列，并由存储层使用 ID 打破平局。
	SortCodeAscending Sort = "code_asc"
	// SortCodeDescending 按稳定编码降序排列，并由存储层使用 ID 打破平局。
	SortCodeDescending Sort = "code_desc"
	// SortNameAscending 按简体中文名称升序排列，并由存储层使用 ID 打破平局。
	SortNameAscending Sort = "name_asc"
	// SortNameDescending 按简体中文名称降序排列，并由存储层使用 ID 打破平局。
	SortNameDescending Sort = "name_desc"
	// SortOrderAscending 按资料排序值升序排列，并由存储层使用 ID 打破平局。
	SortOrderAscending Sort = "sort_order_asc"
	// SortOrderDescending 按资料排序值降序排列，并由存储层使用 ID 打破平局。
	SortOrderDescending Sort = "sort_order_desc"
)

// ListQuery 是实时资料 属性资料列表的显式分页、筛选和排序条件。
type ListQuery struct {
	Page      int32
	PageSize  int32
	Q         string
	Code      string
	Name      string
	SortOrder *int32
	Enabled   *bool
	Sort      Sort
}

// Page 是管理端属性资料的有界分页结果。
type Page struct {
	Items    []Element
	Total    int64
	Page     int32
	PageSize int32
}

// CreateCommand 包含创建属性资料所需的业务字段和管理写入上下文。
type CreateCommand struct {
	administration.GameDataWriteContext
	Code      string
	Name      string
	SortOrder int32
	Enabled   bool
}

// CreateRecord 是存储层原子创建资料、审计和幂等响应所需的完整事实。
type CreateRecord struct {
	administration.GameDataWriteContext
	Element   Element
	CreatedAt time.Time
}

// UpdateCommand 使用完整资料表示和预期版本更新 实时资料中的一条属性资料。
type UpdateCommand struct {
	administration.GameDataWriteContext
	ElementID       snowflake.ID
	ExpectedVersion int64
	Code            string
	Name            string
	SortOrder       int32
	Enabled         bool
}

// UpdateRecord 是存储层原子更新资料、审计记录和幂等响应所需的完整事实。
type UpdateRecord struct {
	administration.GameDataWriteContext
	Element         Element
	ExpectedVersion int64
	UpdatedAt       time.Time
}

// DisableCommand 使用预期版本禁用 实时资料中未被引用的一条属性资料。
type DisableCommand struct {
	administration.GameDataWriteContext
	ElementID       snowflake.ID
	ExpectedVersion int64
}

// DisableRecord 是存储层原子禁用资料、审计记录和幂等响应所需的完整事实。
type DisableRecord struct {
	administration.GameDataWriteContext
	ElementID       snowflake.ID
	ExpectedVersion int64
	DisabledAt      time.Time
}

// Writer 是一次属性资料管理事务内使用的最小写入边界。
type Writer interface {
	Create(context.Context, CreateRecord) (Element, error)
	Update(context.Context, UpdateRecord) (Element, error)
	Disable(context.Context, DisableRecord) error
}

// Store 提供属性资料查询和由应用服务划定范围的事务执行边界。
type Store interface {
	Get(context.Context, snowflake.ID) (Element, error)
	List(context.Context, ListQuery) (Page, error)
	WithinElement(context.Context, func(Writer) error) error
}

// Service 编排属性资料的独立校验、身份生成和持久化命令。
type Service struct {
	store Store
	newID snowflake.Source
	now   func() time.Time
}

// NewService 使用显式依赖创建属性资料应用服务。
func NewService(store Store, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{store: store, newID: newID, now: now}
}

// Create 在当前实时资料中创建版本为 1 的属性资料。
func (s *Service) Create(ctx context.Context, command CreateCommand) (Element, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || !stablecode.Valid(command.Code) ||
		command.Name == "" || len([]rune(command.Name)) > 80 {
		return Element{}, ErrInvalidElement
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Element{}, idErr
	}
	element := Element{
		ID: id, Code: command.Code, Name: command.Name,
		SortOrder: command.SortOrder, Enabled: command.Enabled, Version: 1,
	}
	var created Element
	err := s.store.WithinElement(ctx, func(writer Writer) error {
		var createErr error
		created, createErr = writer.Create(ctx, CreateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Element: element, CreatedAt: s.now().UTC(),
		})
		return createErr
	})
	if err != nil {
		return Element{}, err
	}
	return created, nil
}

// Update 使用乐观版本替换 实时资料中的完整资料字段并递增版本。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (Element, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.ElementID == snowflake.ID(0) || command.ExpectedVersion < 1 ||
		!stablecode.Valid(command.Code) || command.Name == "" || len([]rune(command.Name)) > 80 {
		return Element{}, ErrInvalidElement
	}
	element := Element{
		ID: command.ElementID, Code: command.Code, Name: command.Name,
		SortOrder: command.SortOrder, Enabled: command.Enabled, Version: command.ExpectedVersion + 1,
	}
	var updated Element
	err := s.store.WithinElement(ctx, func(writer Writer) error {
		var updateErr error
		updated, updateErr = writer.Update(ctx, UpdateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Element: element,
			ExpectedVersion: command.ExpectedVersion, UpdatedAt: s.now().UTC(),
		})
		return updateErr
	})
	if err != nil {
		return Element{}, err
	}
	return updated, nil
}

// Get 读取当前实时资料中指定稳定身份的属性资料。
func (s *Service) Get(ctx context.Context, elementID snowflake.ID) (Element, error) {
	if elementID == snowflake.ID(0) {
		return Element{}, ErrInvalidElement
	}
	return s.store.Get(ctx, elementID)
}

// List 返回当前实时资料中经过显式筛选和稳定排序的属性资料页。
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
		len([]rune(query.Q)) > 80 || len([]rune(query.Name)) > 80 || !validSort(query.Sort) ||
		(query.Code != "" && !stablecode.Valid(query.Code)) {
		return Page{}, ErrInvalidElement
	}
	return s.store.List(ctx, query)
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

// Delete 使用乐观版本禁用当前实时资料中未被引用的属性资料。
func (s *Service) Disable(ctx context.Context, command DisableCommand) error {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.ElementID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return ErrInvalidElement
	}
	return s.store.WithinElement(ctx, func(writer Writer) error {
		return writer.Disable(ctx, DisableRecord{
			GameDataWriteContext: command.GameDataWriteContext, ElementID: command.ElementID,
			ExpectedVersion: command.ExpectedVersion, DisabledAt: s.now().UTC(),
		})
	})
}
