// Package itemcategory 定义 实时游戏资料 中道具分类的独立命令与查询边界。
package itemcategory

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
	// ErrInvalidItemCategory 表示道具分类或管理写入上下文未通过边界校验。
	ErrInvalidItemCategory = errors.New("道具分类无效")
	// ErrItemCategoryNotFound 表示实时资料中不存在指定道具分类。
	ErrItemCategoryNotFound = errors.New("道具分类不存在")
	// ErrItemCategoryVersionConflict 表示道具分类已被其他管理写入修改。
	ErrItemCategoryVersionConflict = errors.New("道具分类版本冲突")
	// ErrItemCategoryCodeConflict 表示实时资料中已有分类使用相同稳定编码。
	ErrItemCategoryCodeConflict = errors.New("道具分类编码已存在")
	// ErrItemCategoryReferenced 表示其他 实时资料 资料仍然引用目标道具分类。
	ErrItemCategoryReferenced = errors.New("道具分类仍被引用")
)

// Category 是管理端读取和写入的一条 实时资料 道具分类。
type Category struct {
	ID        snowflake.ID
	Code      string
	Name      string
	PocketID  snowflake.ID
	SortOrder int32
	Enabled   bool
	Version   int64
}

// Sort 声明管理端道具分类列表允许使用的稳定排序。
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
	// SortOrderAscending 按分类排序值升序排列，并使用 ID 打破平局。
	SortOrderAscending Sort = "sort_order_asc"
	// SortOrderDescending 按分类排序值降序排列，并使用 ID 打破平局。
	SortOrderDescending Sort = "sort_order_desc"
)

// ListQuery 是实时资料 道具分类列表的显式分页、筛选和排序条件。
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

// Page 是管理端道具分类的有界分页结果。
type Page struct {
	Items    []Category
	Total    int64
	Page     int32
	PageSize int32
}

// CreateCommand 包含创建道具分类所需的业务字段和管理写入上下文。
type CreateCommand struct {
	administration.GameDataWriteContext
	Code      string
	Name      string
	PocketID  snowflake.ID
	SortOrder int32
	Enabled   bool
}

// CreateRecord 是 Repository 原子创建分类、审计和幂等响应所需的完整事实。
type CreateRecord struct {
	administration.GameDataWriteContext
	Category  Category
	CreatedAt time.Time
}

// UpdateCommand 使用完整分类表示和预期版本更新 实时资料中的一条道具分类。
type UpdateCommand struct {
	administration.GameDataWriteContext
	CategoryID      snowflake.ID
	ExpectedVersion int64
	Code            string
	Name            string
	PocketID        snowflake.ID
	SortOrder       int32
	Enabled         bool
}

// UpdateRecord 是 Repository 原子更新分类、审计记录和幂等响应所需的完整事实。
type UpdateRecord struct {
	administration.GameDataWriteContext
	Category        Category
	ExpectedVersion int64
	UpdatedAt       time.Time
}

// DisableCommand 使用预期版本禁用 实时资料中未被引用的一条道具分类。
type DisableCommand struct {
	administration.GameDataWriteContext
	CategoryID      snowflake.ID
	ExpectedVersion int64
}

// DisableRecord 是 Repository 原子禁用分类、审计记录和幂等响应所需的完整事实。
type DisableRecord struct {
	administration.GameDataWriteContext
	CategoryID      snowflake.ID
	ExpectedVersion int64
	DisabledAt      time.Time
}

// Writer 是一次道具分类管理事务内使用的最小写入边界。
type Writer interface {
	Create(context.Context, CreateRecord) (Category, error)
	Update(context.Context, UpdateRecord) (Category, error)
	Disable(context.Context, DisableRecord) error
}

// ItemCategoryReader 返回指定道具分类领域对象。
type ItemCategoryReader interface {
	GetItemCategory(context.Context, snowflake.ID) (Category, error)
}

// ItemCategoryQuery 返回道具分类分页管理投影。
type ItemCategoryQuery interface {
	ListItemCategories(context.Context, ListQuery) (Page, error)
}

// ItemCategoryRepository 提供由应用服务划定范围的道具分类事务写入边界。
type ItemCategoryRepository interface {
	WithinItemCategory(context.Context, func(Writer) error) error
}

// Service 编排道具分类的独立校验、身份生成和持久化命令。
type Service struct {
	// reader 返回指定道具分类领域对象。
	reader ItemCategoryReader
	// query 返回道具分类分页管理投影。
	query      ItemCategoryQuery
	repository ItemCategoryRepository
	newID      snowflake.Source
	now        func() time.Time
}

// NewService 使用显式依赖创建道具分类应用服务。
func NewService(reader ItemCategoryReader, query ItemCategoryQuery, repository ItemCategoryRepository, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{reader: reader, query: query, repository: repository, newID: newID, now: now}
}

// Create 在当前实时资料中创建版本为 1 的道具分类。
func (s *Service) Create(ctx context.Context, command CreateCommand) (Category, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || !stablecode.Valid(command.Code) || command.Name == "" || len([]rune(command.Name)) > 120 {
		return Category{}, ErrInvalidItemCategory
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Category{}, idErr
	}
	category := Category{
		ID: id, Code: command.Code, Name: command.Name, PocketID: command.PocketID,
		SortOrder: command.SortOrder, Enabled: command.Enabled, Version: 1,
	}
	var created Category
	err := s.repository.WithinItemCategory(ctx, func(writer Writer) error {
		var createErr error
		created, createErr = writer.Create(ctx, CreateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Category: category, CreatedAt: s.now().UTC(),
		})
		return createErr
	})
	if err != nil {
		return Category{}, err
	}
	return created, nil
}

// Update 使用乐观版本替换 实时资料中的完整分类字段并递增版本。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (Category, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.CategoryID == snowflake.ID(0) || command.ExpectedVersion < 1 ||
		!stablecode.Valid(command.Code) || command.Name == "" || len([]rune(command.Name)) > 120 {
		return Category{}, ErrInvalidItemCategory
	}
	category := Category{
		ID: command.CategoryID, Code: command.Code, Name: command.Name, PocketID: command.PocketID, SortOrder: command.SortOrder,
		Enabled: command.Enabled, Version: command.ExpectedVersion + 1,
	}
	var updated Category
	err := s.repository.WithinItemCategory(ctx, func(writer Writer) error {
		var updateErr error
		updated, updateErr = writer.Update(ctx, UpdateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Category: category,
			ExpectedVersion: command.ExpectedVersion, UpdatedAt: s.now().UTC(),
		})
		return updateErr
	})
	if err != nil {
		return Category{}, err
	}
	return updated, nil
}

// Get 读取当前实时资料中指定稳定身份的道具分类。
func (s *Service) Get(ctx context.Context, categoryID snowflake.ID) (Category, error) {
	if categoryID == snowflake.ID(0) {
		return Category{}, ErrInvalidItemCategory
	}
	return s.reader.GetItemCategory(ctx, categoryID)
}

// List 返回当前实时资料中经过显式筛选和稳定排序的道具分类页。
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
		(query.Code != "" && !stablecode.Valid(query.Code)) {
		return Page{}, ErrInvalidItemCategory
	}
	return s.query.ListItemCategories(ctx, query)
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

// Delete 使用乐观版本禁用当前实时资料中未被引用的道具分类。
func (s *Service) Disable(ctx context.Context, command DisableCommand) error {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.CategoryID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return ErrInvalidItemCategory
	}
	return s.repository.WithinItemCategory(ctx, func(writer Writer) error {
		return writer.Disable(ctx, DisableRecord{
			GameDataWriteContext: command.GameDataWriteContext, CategoryID: command.CategoryID,
			ExpectedVersion: command.ExpectedVersion, DisabledAt: s.now().UTC(),
		})
	})
}
