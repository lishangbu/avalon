// Package stat 定义 实时游戏资料 中数值项资料的独立命令与查询边界。
package stat

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
	// ErrInvalidStat 表示数值项资料或管理写入上下文未通过边界校验。
	ErrInvalidStat = errors.New("数值项资料无效")
	// ErrStatNotFound 表示实时资料中不存在指定数值项资料。
	ErrStatNotFound = errors.New("数值项资料不存在")
	// ErrStatVersionConflict 表示数值项资料已被其他管理写入修改。
	ErrStatVersionConflict = errors.New("数值项资料版本冲突")
	// ErrStatCodeConflict 表示实时资料中已有资料使用相同稳定编码。
	ErrStatCodeConflict = errors.New("数值项资料编码已存在")
	// ErrStatReferenced 表示其他 实时资料 资料仍然引用目标数值项资料。
	ErrStatReferenced = errors.New("数值项资料仍被引用")
)

// Stat 是管理端读取和写入的一条 实时资料 数值项资料。
type Stat struct {
	ID         snowflake.ID
	Code       string
	Name       string
	SortOrder  int32
	BattleOnly bool
	Enabled    bool
	Version    int64
}

// Sort 声明管理端数值项列表允许使用的稳定排序。
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

// ListQuery 是实时资料 数值项列表的显式分页、筛选和排序条件。
type ListQuery struct {
	Page       int32
	PageSize   int32
	Q          string
	Code       string
	Name       string
	SortOrder  *int32
	BattleOnly *bool
	Enabled    *bool
	Sort       Sort
}

// Page 是管理端数值项资料的有界分页结果。
type Page struct {
	Items    []Stat
	Total    int64
	Page     int32
	PageSize int32
}

// CreateCommand 包含创建数值项资料所需的业务字段和管理写入上下文。
type CreateCommand struct {
	administration.GameDataWriteContext
	Code       string
	Name       string
	SortOrder  int32
	BattleOnly bool
	Enabled    bool
}

// CreateRecord 是存储层原子创建资料、审计和幂等响应所需的完整事实。
type CreateRecord struct {
	administration.GameDataWriteContext
	Stat      Stat
	CreatedAt time.Time
}

// UpdateCommand 使用完整资料表示和预期版本更新 实时资料中的一条数值项资料。
type UpdateCommand struct {
	administration.GameDataWriteContext
	StatID          snowflake.ID
	ExpectedVersion int64
	Code            string
	Name            string
	SortOrder       int32
	BattleOnly      bool
	Enabled         bool
}

// UpdateRecord 是存储层原子更新资料、审计记录和幂等响应所需的完整事实。
type UpdateRecord struct {
	administration.GameDataWriteContext
	Stat            Stat
	ExpectedVersion int64
	UpdatedAt       time.Time
}

// DisableCommand 使用预期版本禁用 实时资料中未被引用的一条数值项资料。
type DisableCommand struct {
	administration.GameDataWriteContext
	StatID          snowflake.ID
	ExpectedVersion int64
}

// DisableRecord 是存储层原子禁用资料、审计记录和幂等响应所需的完整事实。
type DisableRecord struct {
	administration.GameDataWriteContext
	StatID          snowflake.ID
	ExpectedVersion int64
	DisabledAt      time.Time
}

// Writer 是一次数值项资料管理事务内使用的最小写入边界。
type Writer interface {
	Create(context.Context, CreateRecord) (Stat, error)
	Update(context.Context, UpdateRecord) (Stat, error)
	Disable(context.Context, DisableRecord) error
}

// Delete 使用乐观版本禁用当前实时资料中未被引用的数值项资料。
func (s *Service) Disable(ctx context.Context, command DisableCommand) error {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.StatID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return ErrInvalidStat
	}
	return s.repository.WithinStat(ctx, func(writer Writer) error {
		return writer.Disable(ctx, DisableRecord{
			GameDataWriteContext: command.GameDataWriteContext, StatID: command.StatID,
			ExpectedVersion: command.ExpectedVersion, DisabledAt: s.now().UTC(),
		})
	})
}

// Update 使用乐观版本替换 实时资料中的完整数值项字段并递增版本。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (Stat, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.StatID == snowflake.ID(0) || command.ExpectedVersion < 1 ||
		!stablecode.Valid(command.Code) || command.Name == "" || len([]rune(command.Name)) > 80 {
		return Stat{}, ErrInvalidStat
	}
	value := Stat{
		ID: command.StatID, Code: command.Code, Name: command.Name, SortOrder: command.SortOrder,
		BattleOnly: command.BattleOnly, Enabled: command.Enabled, Version: command.ExpectedVersion + 1,
	}
	var updated Stat
	err := s.repository.WithinStat(ctx, func(writer Writer) error {
		var updateErr error
		updated, updateErr = writer.Update(ctx, UpdateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Stat: value,
			ExpectedVersion: command.ExpectedVersion, UpdatedAt: s.now().UTC(),
		})
		return updateErr
	})
	if err != nil {
		return Stat{}, err
	}
	return updated, nil
}

// StatReader 返回指定数值项领域对象。
type StatReader interface {
	GetStat(context.Context, snowflake.ID) (Stat, error)
}

// StatQuery 返回数值项分页管理投影。
type StatQuery interface {
	ListStats(context.Context, ListQuery) (Page, error)
}

// StatRepository 提供由应用服务划定范围的数值项资料事务写入边界。
type StatRepository interface {
	WithinStat(context.Context, func(Writer) error) error
}

// List 返回当前实时资料中经过显式筛选和稳定排序的数值项资料页。
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
		return Page{}, ErrInvalidStat
	}
	return s.query.ListStats(ctx, query)
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

// Get 读取当前实时资料中指定稳定身份的数值项资料。
func (s *Service) Get(ctx context.Context, statID snowflake.ID) (Stat, error) {
	if statID == snowflake.ID(0) {
		return Stat{}, ErrInvalidStat
	}
	return s.reader.GetStat(ctx, statID)
}

// Service 编排数值项资料的独立校验、身份生成和持久化命令。
type Service struct {
	reader     StatReader
	query      StatQuery
	repository StatRepository
	newID      snowflake.Source
	now        func() time.Time
}

// NewService 使用显式依赖创建数值项资料应用服务。
func NewService(reader StatReader, query StatQuery, repository StatRepository, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{reader: reader, query: query, repository: repository, newID: newID, now: now}
}

// Create 在当前实时资料中创建版本为 1 的数值项资料。
func (s *Service) Create(ctx context.Context, command CreateCommand) (Stat, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || !stablecode.Valid(command.Code) || command.Name == "" || len([]rune(command.Name)) > 80 {
		return Stat{}, ErrInvalidStat
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Stat{}, idErr
	}
	value := Stat{
		ID: id, Code: command.Code, Name: command.Name, SortOrder: command.SortOrder,
		BattleOnly: command.BattleOnly, Enabled: command.Enabled, Version: 1,
	}
	var created Stat
	err := s.repository.WithinStat(ctx, func(writer Writer) error {
		var createErr error
		created, createErr = writer.Create(ctx, CreateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Stat: value, CreatedAt: s.now().UTC(),
		})
		return createErr
	})
	if err != nil {
		return Stat{}, err
	}
	return created, nil
}
