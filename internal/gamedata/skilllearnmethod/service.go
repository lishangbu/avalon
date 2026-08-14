// Package skilllearnmethod 定义 实时游戏资料 中技能学习方式的独立命令与查询边界。
package skilllearnmethod

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
	// ErrInvalidSkillLearnMethod 表示技能学习方式或管理写入上下文未通过边界校验。
	ErrInvalidSkillLearnMethod = errors.New("技能学习方式无效")
	// ErrSkillLearnMethodNotFound 表示实时资料中不存在指定技能学习方式。
	ErrSkillLearnMethodNotFound = errors.New("技能学习方式不存在")
	// ErrSkillLearnMethodVersionConflict 表示技能学习方式已被其他管理写入修改。
	ErrSkillLearnMethodVersionConflict = errors.New("技能学习方式版本冲突")
	// ErrSkillLearnMethodCodeConflict 表示实时资料中已有资料使用相同稳定编码。
	ErrSkillLearnMethodCodeConflict = errors.New("技能学习方式编码已存在")
	// ErrSkillLearnMethodReferenced 表示技能详情仍然引用该技能学习方式。
	ErrSkillLearnMethodReferenced = errors.New("技能学习方式仍被引用")
)

// Method 是管理端读取和写入的一条 实时资料 技能学习方式资料。
type Method struct {
	ID          snowflake.ID
	Code        string
	Name        string
	Description *string
	Enabled     bool
	Version     int64
}

// Sort 声明管理端技能学习方式列表允许使用的稳定排序。
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

// ListQuery 是实时资料 技能学习方式列表的显式分页、筛选和排序条件。
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

// Page 是管理端技能学习方式资料的有界分页结果。
type Page struct {
	Items    []Method
	Total    int64
	Page     int32
	PageSize int32
}

// DescriptionChange 表示更新请求对可空说明字段的省略、清空或替换意图。
type DescriptionChange struct {
	Specified bool
	Value     *string
}

// CreateCommand 包含创建技能学习方式所需的业务字段和管理写入上下文。
type CreateCommand struct {
	administration.GameDataWriteContext
	Code        string
	Name        string
	Description *string
	Enabled     bool
}

// CreateRecord 是 Repository 原子创建资料、审计和幂等响应所需的完整事实。
type CreateRecord struct {
	administration.GameDataWriteContext
	Method    Method
	CreatedAt time.Time
}

// UpdateCommand 使用说明变更意图和预期版本更新一条技能学习方式。
type UpdateCommand struct {
	administration.GameDataWriteContext
	MethodID        snowflake.ID
	ExpectedVersion int64
	Code            string
	Name            string
	Description     DescriptionChange
	Enabled         bool
}

// UpdateRecord 是 Repository 原子更新资料、审计记录和幂等响应所需的完整事实。
type UpdateRecord struct {
	administration.GameDataWriteContext
	Method          Method
	Description     DescriptionChange
	ExpectedVersion int64
	UpdatedAt       time.Time
}

// DisableCommand 使用预期版本禁用 实时资料中未被引用的一条技能学习方式。
type DisableCommand struct {
	administration.GameDataWriteContext
	MethodID        snowflake.ID
	ExpectedVersion int64
}

// DisableRecord 是 Repository 原子禁用资料、审计记录和幂等响应所需的完整事实。
type DisableRecord struct {
	administration.GameDataWriteContext
	MethodID        snowflake.ID
	ExpectedVersion int64
	DisabledAt      time.Time
}

// Writer 是一次技能学习方式管理事务内使用的最小写入边界。
type Writer interface {
	Create(context.Context, CreateRecord) (Method, error)
	Update(context.Context, UpdateRecord) (Method, error)
	Disable(context.Context, DisableRecord) error
}

// SkillLearnMethodReader 返回指定技能学习方式领域对象。
type SkillLearnMethodReader interface {
	GetSkillLearnMethod(context.Context, snowflake.ID) (Method, error)
}

// SkillLearnMethodQuery 返回技能学习方式分页管理投影。
type SkillLearnMethodQuery interface {
	ListSkillLearnMethods(context.Context, ListQuery) (Page, error)
}

// SkillLearnMethodRepository 提供技能学习方式事务写入边界。
type SkillLearnMethodRepository interface {
	WithinSkillLearnMethod(context.Context, func(Writer) error) error
}

// Service 编排技能学习方式的校验、身份生成和持久化命令。
type Service struct {
	// reader 返回指定技能学习方式领域对象。
	reader SkillLearnMethodReader
	// query 返回技能学习方式分页管理投影。
	query      SkillLearnMethodQuery
	repository SkillLearnMethodRepository
	newID      snowflake.Source
	now        func() time.Time
}

// NewService 使用显式依赖创建技能学习方式应用服务。
func NewService(reader SkillLearnMethodReader, query SkillLearnMethodQuery, repository SkillLearnMethodRepository, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{reader: reader, query: query, repository: repository, newID: newID, now: now}
}

// Get 读取当前实时资料中指定稳定身份的技能学习方式。
func (s *Service) Get(ctx context.Context, methodID snowflake.ID) (Method, error) {
	if methodID == snowflake.ID(0) {
		return Method{}, ErrInvalidSkillLearnMethod
	}
	return s.reader.GetSkillLearnMethod(ctx, methodID)
}

// List 返回当前实时资料中经过显式筛选和稳定排序的技能学习方式页。
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
		return Page{}, ErrInvalidSkillLearnMethod
	}
	return s.query.ListSkillLearnMethods(ctx, query)
}

func validSort(sort Sort) bool {
	switch sort {
	case SortCodeAscending, SortCodeDescending, SortNameAscending, SortNameDescending:
		return true
	default:
		return false
	}
}

// Create 在当前实时资料中创建版本为 1 的技能学习方式。
func (s *Service) Create(ctx context.Context, command CreateCommand) (Method, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.Description = normalizeDescription(command.Description)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || !validFields(command.Code, command.Name, command.Description) {
		return Method{}, ErrInvalidSkillLearnMethod
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Method{}, idErr
	}
	value := Method{
		ID: id, Code: command.Code, Name: command.Name, Description: command.Description,
		Enabled: command.Enabled, Version: 1,
	}
	var created Method
	err := s.repository.WithinSkillLearnMethod(ctx, func(writer Writer) error {
		var createErr error
		created, createErr = writer.Create(ctx, CreateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Method: value, CreatedAt: s.now().UTC(),
		})
		return createErr
	})
	if err != nil {
		return Method{}, err
	}
	return created, nil
}

// Update 使用乐观版本和说明字段意图更新当前实时资料中的技能学习方式。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (Method, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	if command.Description.Specified {
		command.Description.Value = normalizeDescription(command.Description.Value)
	}
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.MethodID == snowflake.ID(0) || command.ExpectedVersion < 1 ||
		!validFields(command.Code, command.Name, command.Description.Value) {
		return Method{}, ErrInvalidSkillLearnMethod
	}
	value := Method{
		ID: command.MethodID, Code: command.Code, Name: command.Name,
		Enabled: command.Enabled, Version: command.ExpectedVersion + 1,
	}
	var updated Method
	err := s.repository.WithinSkillLearnMethod(ctx, func(writer Writer) error {
		var updateErr error
		updated, updateErr = writer.Update(ctx, UpdateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Method: value, Description: command.Description,
			ExpectedVersion: command.ExpectedVersion, UpdatedAt: s.now().UTC(),
		})
		return updateErr
	})
	if err != nil {
		return Method{}, err
	}
	return updated, nil
}

// Delete 使用乐观版本禁用当前实时资料中未被引用的技能学习方式。
func (s *Service) Disable(ctx context.Context, command DisableCommand) error {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.MethodID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return ErrInvalidSkillLearnMethod
	}
	return s.repository.WithinSkillLearnMethod(ctx, func(writer Writer) error {
		return writer.Disable(ctx, DisableRecord{
			GameDataWriteContext: command.GameDataWriteContext, MethodID: command.MethodID,
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
