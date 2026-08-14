// Package skillcategory 定义 实时游戏资料 中技能元分类的独立命令与查询边界。
package skillcategory

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
	// ErrInvalidSkillCategory 表示技能元分类或管理写入上下文未通过边界校验。
	ErrInvalidSkillCategory = errors.New("技能元分类无效")
	// ErrSkillCategoryNotFound 表示实时资料中不存在指定技能元分类。
	ErrSkillCategoryNotFound = errors.New("技能元分类不存在")
	// ErrSkillCategoryVersionConflict 表示技能元分类已被其他管理写入修改。
	ErrSkillCategoryVersionConflict = errors.New("技能元分类版本冲突")
	// ErrSkillCategoryCodeConflict 表示实时资料中已有资料使用相同稳定编码。
	ErrSkillCategoryCodeConflict = errors.New("技能元分类编码已存在")
	// ErrSkillCategoryReferenced 表示技能详情仍然引用目标技能元分类。
	ErrSkillCategoryReferenced = errors.New("技能元分类仍被引用")
)

// Category 是管理端读取和写入的一条 实时资料 技能元分类资料。
type Category struct {
	ID          snowflake.ID
	Code        string
	Name        string
	Description *string
	Enabled     bool
	Version     int64
}

// Sort 声明管理端技能元分类列表允许使用的稳定排序。
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
)

// ListQuery 是实时资料 技能元分类列表的显式分页、筛选和排序条件。
type ListQuery struct {
	Page        int32
	PageSize    int32
	Q           string
	Code        string
	Name        string
	Description string
	Enabled     *bool
	Sort        Sort
}

// Page 是管理端技能元分类资料的有界分页结果。
type Page struct {
	Items    []Category
	Total    int64
	Page     int32
	PageSize int32
}

// DescriptionChange 表示更新请求对可空说明字段的省略、清空或替换意图。
type DescriptionChange struct {
	Specified bool
	Value     *string
}

// CreateCommand 包含创建技能元分类所需的业务字段和管理写入上下文。
type CreateCommand struct {
	administration.GameDataWriteContext
	Code        string
	Name        string
	Description *string
	Enabled     bool
}

// CreateRecord 是存储层原子创建资料、审计和幂等响应所需的完整事实。
type CreateRecord struct {
	administration.GameDataWriteContext
	Category  Category
	CreatedAt time.Time
}

// UpdateCommand 使用说明变更意图和预期版本更新一条技能元分类。
type UpdateCommand struct {
	administration.GameDataWriteContext
	CategoryID      snowflake.ID
	ExpectedVersion int64
	Code            string
	Name            string
	Description     DescriptionChange
	Enabled         bool
}

// UpdateRecord 是存储层原子更新资料、审计记录和幂等响应所需的完整事实。
type UpdateRecord struct {
	administration.GameDataWriteContext
	Category        Category
	Description     DescriptionChange
	ExpectedVersion int64
	UpdatedAt       time.Time
}

// DisableCommand 使用预期版本禁用 实时资料中未被引用的一条技能元分类。
type DisableCommand struct {
	administration.GameDataWriteContext
	CategoryID      snowflake.ID
	ExpectedVersion int64
}

// DisableRecord 是存储层原子禁用资料、审计记录和幂等响应所需的完整事实。
type DisableRecord struct {
	administration.GameDataWriteContext
	CategoryID      snowflake.ID
	ExpectedVersion int64
	DisabledAt      time.Time
}

// Writer 是一次技能元分类管理事务内使用的最小写入边界。
type Writer interface {
	Create(context.Context, CreateRecord) (Category, error)
	Update(context.Context, UpdateRecord) (Category, error)
	Disable(context.Context, DisableRecord) error
}

// SkillCategoryReader 返回指定技能元分类领域对象。
type SkillCategoryReader interface {
	GetSkillCategory(context.Context, snowflake.ID) (Category, error)
}

// SkillCategoryQuery 返回技能元分类分页管理投影。
type SkillCategoryQuery interface {
	ListSkillCategories(context.Context, ListQuery) (Page, error)
}

// SkillCategoryRepository 提供技能元分类资料的事务写入端口。
type SkillCategoryRepository interface {
	WithinSkillCategory(context.Context, func(Writer) error) error
}

// Service 编排技能元分类的校验、身份生成和持久化命令。
type Service struct {
	// reader 返回指定技能元分类领域对象。
	reader SkillCategoryReader
	// query 返回技能元分类分页管理投影。
	query      SkillCategoryQuery
	repository SkillCategoryRepository
	newID      snowflake.Source
	now        func() time.Time
}

// NewService 使用显式依赖创建技能元分类应用服务。
func NewService(reader SkillCategoryReader, query SkillCategoryQuery, repository SkillCategoryRepository, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{reader: reader, query: query, repository: repository, newID: newID, now: now}
}

// Get 读取当前实时资料中指定稳定身份的技能元分类。
func (s *Service) Get(ctx context.Context, categoryID snowflake.ID) (Category, error) {
	if categoryID == snowflake.ID(0) {
		return Category{}, ErrInvalidSkillCategory
	}
	return s.reader.GetSkillCategory(ctx, categoryID)
}

// List 返回当前实时资料中经过显式筛选和稳定排序的技能元分类页。
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
		len([]rune(query.Q)) > 120 || len([]rune(query.Name)) > 120 || len([]rune(query.Description)) > 500 ||
		(query.Code != "" && !stablecode.Valid(query.Code)) || !validSort(query.Sort) {
		return Page{}, ErrInvalidSkillCategory
	}
	return s.query.ListSkillCategories(ctx, query)
}

func validSort(sort Sort) bool {
	switch sort {
	case SortCodeAscending, SortCodeDescending, SortNameAscending, SortNameDescending:
		return true
	default:
		return false
	}
}

// Create 在当前实时资料中创建版本为 1 的技能元分类。
func (s *Service) Create(ctx context.Context, command CreateCommand) (Category, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.Description = normalizeDescription(command.Description)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || !validFields(command.Code, command.Name, command.Description) {
		return Category{}, ErrInvalidSkillCategory
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Category{}, idErr
	}
	value := Category{
		ID: id, Code: command.Code, Name: command.Name, Description: command.Description,
		Enabled: command.Enabled, Version: 1,
	}
	var created Category
	err := s.repository.WithinSkillCategory(ctx, func(writer Writer) error {
		var createErr error
		created, createErr = writer.Create(ctx, CreateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Category: value, CreatedAt: s.now().UTC(),
		})
		return createErr
	})
	if err != nil {
		return Category{}, err
	}
	return created, nil
}

// Update 使用乐观版本和说明字段意图更新当前实时资料中的技能元分类。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (Category, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	if command.Description.Specified {
		command.Description.Value = normalizeDescription(command.Description.Value)
	}
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.CategoryID == snowflake.ID(0) || command.ExpectedVersion < 1 ||
		!validFields(command.Code, command.Name, command.Description.Value) {
		return Category{}, ErrInvalidSkillCategory
	}
	value := Category{
		ID: command.CategoryID, Code: command.Code, Name: command.Name,
		Enabled: command.Enabled, Version: command.ExpectedVersion + 1,
	}
	var updated Category
	err := s.repository.WithinSkillCategory(ctx, func(writer Writer) error {
		var updateErr error
		updated, updateErr = writer.Update(ctx, UpdateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Category: value, Description: command.Description,
			ExpectedVersion: command.ExpectedVersion, UpdatedAt: s.now().UTC(),
		})
		return updateErr
	})
	if err != nil {
		return Category{}, err
	}
	return updated, nil
}

// Delete 使用乐观版本禁用当前实时资料中未被引用的技能元分类。
func (s *Service) Disable(ctx context.Context, command DisableCommand) error {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.CategoryID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return ErrInvalidSkillCategory
	}
	return s.repository.WithinSkillCategory(ctx, func(writer Writer) error {
		return writer.Disable(ctx, DisableRecord{
			GameDataWriteContext: command.GameDataWriteContext, CategoryID: command.CategoryID,
			ExpectedVersion: command.ExpectedVersion, DisabledAt: s.now().UTC(),
		})
	})
}

func normalizeDescription(value *string) *string {
	if value == nil {
		return nil
	}
	normalized := strings.TrimSpace(*value)
	if normalized == "" {
		return nil
	}
	return &normalized
}

func validFields(code, name string, description *string) bool {
	return stablecode.Valid(code) && name != "" && len([]rune(name)) <= 120 &&
		(description == nil || len([]rune(*description)) <= 500)
}
