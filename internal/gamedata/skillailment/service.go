// Package skillailment 定义 实时游戏资料 中技能异常字典的独立命令与查询边界。
package skillailment

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
	// ErrInvalidSkillAilment 表示技能异常资料或管理写入上下文未通过边界校验。
	ErrInvalidSkillAilment = errors.New("技能异常资料无效")
	// ErrSkillAilmentNotFound 表示实时资料中不存在指定技能异常资料。
	ErrSkillAilmentNotFound = errors.New("技能异常资料不存在")
	// ErrSkillAilmentVersionConflict 表示技能异常资料已被其他管理写入修改。
	ErrSkillAilmentVersionConflict = errors.New("技能异常资料版本冲突")
	// ErrSkillAilmentCodeConflict 表示实时资料中已有资料使用相同稳定编码。
	ErrSkillAilmentCodeConflict = errors.New("技能异常资料编码已存在")
	// ErrSkillAilmentReferenced 表示技能详情仍然引用目标技能异常资料。
	ErrSkillAilmentReferenced = errors.New("技能异常资料仍被引用")
)

// Ailment 是管理端读取和写入的一条 实时资料 技能异常资料。
type Ailment struct {
	ID      snowflake.ID
	Code    string
	Name    string
	Enabled bool
	Version int64
}

// Sort 声明管理端技能异常列表允许使用的稳定排序。
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

// ListQuery 是实时资料 技能异常列表的显式分页、筛选和排序条件。
type ListQuery struct {
	Page     int32
	PageSize int32
	Q        string
	Code     string
	Name     string
	Enabled  *bool
	Sort     Sort
}

// Page 是管理端技能异常资料的有界分页结果。
type Page struct {
	Items    []Ailment
	Total    int64
	Page     int32
	PageSize int32
}

// CreateCommand 包含创建技能异常资料所需的业务字段和管理写入上下文。
type CreateCommand struct {
	administration.GameDataWriteContext
	Code    string
	Name    string
	Enabled bool
}

// CreateRecord 是 Repository 原子创建资料、审计和幂等响应所需的完整事实。
type CreateRecord struct {
	administration.GameDataWriteContext
	Ailment   Ailment
	CreatedAt time.Time
}

// UpdateCommand 使用预期版本更新一条技能异常资料。
type UpdateCommand struct {
	administration.GameDataWriteContext
	AilmentID       snowflake.ID
	ExpectedVersion int64
	Code            string
	Name            string
	Enabled         bool
}

// UpdateRecord 是 Repository 原子更新资料、审计记录和幂等响应所需的完整事实。
type UpdateRecord struct {
	administration.GameDataWriteContext
	Ailment         Ailment
	ExpectedVersion int64
	UpdatedAt       time.Time
}

// DisableCommand 使用预期版本禁用 实时资料中未被引用的一条技能异常资料。
type DisableCommand struct {
	administration.GameDataWriteContext
	AilmentID       snowflake.ID
	ExpectedVersion int64
}

// DisableRecord 是 Repository 原子禁用资料、审计记录和幂等响应所需的完整事实。
type DisableRecord struct {
	administration.GameDataWriteContext
	AilmentID       snowflake.ID
	ExpectedVersion int64
	DisabledAt      time.Time
}

// Writer 是一次技能异常资料管理事务内使用的最小写入边界。
type Writer interface {
	Create(context.Context, CreateRecord) (Ailment, error)
	Update(context.Context, UpdateRecord) (Ailment, error)
	Disable(context.Context, DisableRecord) error
}

// SkillAilmentReader 返回指定技能异常领域对象。
type SkillAilmentReader interface {
	GetSkillAilment(context.Context, snowflake.ID) (Ailment, error)
}

// SkillAilmentQuery 返回技能异常分页管理投影。
type SkillAilmentQuery interface {
	ListSkillAilments(context.Context, ListQuery) (Page, error)
}

// SkillAilmentRepository 提供技能异常资料的事务写入端口。
type SkillAilmentRepository interface {
	WithinSkillAilment(context.Context, func(Writer) error) error
}

// Service 编排技能异常资料的校验、身份生成和持久化命令。
type Service struct {
	// reader 返回指定技能异常领域对象。
	reader SkillAilmentReader
	// query 返回技能异常分页管理投影。
	query      SkillAilmentQuery
	repository SkillAilmentRepository
	newID      snowflake.Source
	now        func() time.Time
}

// NewService 使用显式依赖创建技能异常资料应用服务。
func NewService(reader SkillAilmentReader, query SkillAilmentQuery, repository SkillAilmentRepository, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{reader: reader, query: query, repository: repository, newID: newID, now: now}
}

// Get 读取当前实时资料中指定稳定身份的技能异常资料。
func (s *Service) Get(ctx context.Context, ailmentID snowflake.ID) (Ailment, error) {
	if ailmentID == snowflake.ID(0) {
		return Ailment{}, ErrInvalidSkillAilment
	}
	return s.reader.GetSkillAilment(ctx, ailmentID)
}

// List 返回当前实时资料中经过显式筛选和稳定排序的技能异常资料页。
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
		len([]rune(query.Q)) > 120 || len([]rune(query.Name)) > 120 ||
		(query.Code != "" && !stablecode.Valid(query.Code)) || !validSort(query.Sort) {
		return Page{}, ErrInvalidSkillAilment
	}
	return s.query.ListSkillAilments(ctx, query)
}

func validSort(sort Sort) bool {
	switch sort {
	case SortCodeAscending, SortCodeDescending, SortNameAscending, SortNameDescending:
		return true
	default:
		return false
	}
}

// Create 在当前实时资料中创建版本为 1 的技能异常资料。
func (s *Service) Create(ctx context.Context, command CreateCommand) (Ailment, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || !validFields(command.Code, command.Name) {
		return Ailment{}, ErrInvalidSkillAilment
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Ailment{}, idErr
	}
	value := Ailment{
		ID: id, Code: command.Code, Name: command.Name, Enabled: command.Enabled, Version: 1,
	}
	var created Ailment
	err := s.repository.WithinSkillAilment(ctx, func(writer Writer) error {
		var createErr error
		created, createErr = writer.Create(ctx, CreateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Ailment: value, CreatedAt: s.now().UTC(),
		})
		return createErr
	})
	if err != nil {
		return Ailment{}, err
	}
	return created, nil
}

// Update 使用乐观版本更新当前实时资料中的技能异常资料。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (Ailment, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.AilmentID == snowflake.ID(0) || command.ExpectedVersion < 1 ||
		!validFields(command.Code, command.Name) {
		return Ailment{}, ErrInvalidSkillAilment
	}
	value := Ailment{
		ID: command.AilmentID, Code: command.Code, Name: command.Name,
		Enabled: command.Enabled, Version: command.ExpectedVersion + 1,
	}
	var updated Ailment
	err := s.repository.WithinSkillAilment(ctx, func(writer Writer) error {
		var updateErr error
		updated, updateErr = writer.Update(ctx, UpdateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Ailment: value,
			ExpectedVersion: command.ExpectedVersion, UpdatedAt: s.now().UTC(),
		})
		return updateErr
	})
	if err != nil {
		return Ailment{}, err
	}
	return updated, nil
}

// Delete 使用乐观版本禁用当前实时资料中未被引用的技能异常资料。
func (s *Service) Disable(ctx context.Context, command DisableCommand) error {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.AilmentID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return ErrInvalidSkillAilment
	}
	return s.repository.WithinSkillAilment(ctx, func(writer Writer) error {
		return writer.Disable(ctx, DisableRecord{
			GameDataWriteContext: command.GameDataWriteContext, AilmentID: command.AilmentID,
			ExpectedVersion: command.ExpectedVersion, DisabledAt: s.now().UTC(),
		})
	})
}

func validFields(code, name string) bool {
	return stablecode.Valid(code) && name != "" && len([]rune(name)) <= 120
}
