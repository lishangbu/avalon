// Package nature 定义实时游戏资料中 Nature 的独立管理边界。
package nature

import (
	"context"
	"errors"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
)

var (
	// ErrInvalidNature 表示 Nature 字段、能力引用或管理写入上下文无效。
	ErrInvalidNature = errors.New("Nature 资料无效")
	// ErrNatureNotFound 表示指定 Nature 不存在。
	ErrNatureNotFound = errors.New("Nature 资料不存在")
	// ErrNatureConflict 表示编码、乐观版本或全局资料修订发生冲突。
	ErrNatureConflict = errors.New("Nature 资料冲突")
	// ErrNatureReferenced 表示 Nature 仍被 Team 引用，不能禁用。
	ErrNatureReferenced = errors.New("Nature 资料仍被引用")
)

// Nature 是成员可选择的能力修正资料；两个能力引用同时为空表示中性 Nature。
type Nature struct {
	// ID 是 Nature 的稳定 Identifier。
	ID snowflake.ID
	// Code 是可修改但全局唯一的英文机器编码。
	Code string
	// Name 是简体中文展示名称。
	Name string
	// IncreasedStatID 是获得 110% 修正的可选能力 Identifier。
	IncreasedStatID *snowflake.ID
	// DecreasedStatID 是获得 90% 修正的可选能力 Identifier。
	DecreasedStatID *snowflake.ID
	// Enabled 表示该 Nature 可进入新 Team 和新对战。
	Enabled bool
	// Version 是该记录的乐观并发版本。
	Version int64
}

// ListQuery 是 Nature 资料的页码分页和筛选条件。
type ListQuery struct {
	// Page 是从一开始的页码。
	Page int32
	// PageSize 是单页最大记录数。
	PageSize int32
	// Q 同时模糊匹配机器编码和名称。
	Q string
	// Code 模糊匹配机器编码。
	Code string
	// Name 模糊匹配中文名称。
	Name string
	// Enabled 可选地筛选启用状态。
	Enabled *bool
}

// Page 是 Nature 资料的有界分页结果。
type Page struct {
	// Items 是当前页 Nature 资料。
	Items []Nature
	// Total 是筛选后的总条数。
	Total int64
	// Page 是实际页码。
	Page int32
	// PageSize 是实际单页大小。
	PageSize int32
}

// CreateCommand 包含创建 Nature 所需的业务字段和管理写入上下文。
type CreateCommand struct {
	// GameDataWriteContext 是维护窗口、修订、操作者和幂等事实。
	administration.GameDataWriteContext
	// Code 是 Nature 的机器编码。
	Code string
	// Name 是 Nature 的中文名称。
	Name string
	// IncreasedStatID 是可选提升能力 Identifier。
	IncreasedStatID *snowflake.ID
	// DecreasedStatID 是可选降低能力 Identifier。
	DecreasedStatID *snowflake.ID
	// Enabled 表示 Nature 是否可进入新 Team。
	Enabled bool
}

// UpdateCommand 包含完整替换 Nature 所需的字段和预期版本。
type UpdateCommand struct {
	// GameDataWriteContext 是维护窗口、修订、操作者和幂等事实。
	administration.GameDataWriteContext
	// ID 是待更新 Nature 的稳定 Identifier。
	ID snowflake.ID
	// ExpectedVersion 是调用方读取到的乐观版本。
	ExpectedVersion int64
	// Code 是完整替换后的机器编码。
	Code string
	// Name 是完整替换后的中文名称。
	Name string
	// IncreasedStatID 是完整替换后的可选提升能力 Identifier。
	IncreasedStatID *snowflake.ID
	// DecreasedStatID 是完整替换后的可选降低能力 Identifier。
	DecreasedStatID *snowflake.ID
	// Enabled 是完整替换后的启用状态。
	Enabled bool
}

// CreateRecord 是存储事务使用的已校验创建事实。
type CreateRecord struct {
	// GameDataWriteContext 是已经通过应用边界校验的管理写入事实。
	administration.GameDataWriteContext
	// Nature 是等待原子创建的完整资料。
	Nature Nature
	// At 是创建、审计和全局修订统一使用的 UTC 时间。
	At time.Time
}

// UpdateRecord 是存储事务使用的已校验更新事实。
type UpdateRecord struct {
	// GameDataWriteContext 是已经通过应用边界校验的管理写入事实。
	administration.GameDataWriteContext
	// Nature 是等待原子替换的完整资料。
	Nature Nature
	// ExpectedVersion 是持久化条件使用的乐观版本。
	ExpectedVersion int64
	// At 是更新、审计和全局修订统一使用的 UTC 时间。
	At time.Time
}

// Writer 是单次 Nature 管理事务内的最小写入接口。
type Writer interface {
	Create(context.Context, CreateRecord) (Nature, error)
	Update(context.Context, UpdateRecord) (Nature, error)
}

// Store 提供 Nature 查询与事务边界。
type Store interface {
	Get(context.Context, snowflake.ID) (Nature, error)
	List(context.Context, ListQuery) (Page, error)
	WithinNature(context.Context, func(Writer) error) error
}

// StatQuery 读取 Nature 可修正能力的稳定语义。
type StatQuery interface {
	Get(context.Context, snowflake.ID) (stat.Stat, error)
}

// Service 校验并编排 Nature 资料命令和查询。
type Service struct {
	store Store
	stats StatQuery
	newID snowflake.Source
	now   func() time.Time
}

// NewService 使用显式依赖创建 Nature 资料服务。
func NewService(store Store, stats StatQuery, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{store: store, stats: stats, newID: newID, now: now}
}

// Create 创建版本为一的 Nature。
func (s *Service) Create(ctx context.Context, command CreateCommand) (Nature, error) {
	command.Code, command.Name = strings.TrimSpace(command.Code), strings.TrimSpace(command.Name)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || !validNature(command.Code, command.Name, command.IncreasedStatID, command.DecreasedStatID) {
		return Nature{}, ErrInvalidNature
	}
	if err := s.validateStats(ctx, command.IncreasedStatID, command.DecreasedStatID); err != nil {
		return Nature{}, err
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Nature{}, idErr
	}
	value := Nature{ID: id, Code: command.Code, Name: command.Name, IncreasedStatID: cloneID(command.IncreasedStatID), DecreasedStatID: cloneID(command.DecreasedStatID), Enabled: command.Enabled, Version: 1}
	var created Nature
	err := s.store.WithinNature(ctx, func(writer Writer) error {
		var err error
		created, err = writer.Create(ctx, CreateRecord{GameDataWriteContext: command.GameDataWriteContext, Nature: value, At: s.now().UTC()})
		return err
	})
	return created, err
}

// Update 使用乐观版本完整替换 Nature 字段。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (Nature, error) {
	command.Code, command.Name = strings.TrimSpace(command.Code), strings.TrimSpace(command.Name)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.ID == snowflake.ID(0) || command.ExpectedVersion < 1 || !validNature(command.Code, command.Name, command.IncreasedStatID, command.DecreasedStatID) {
		return Nature{}, ErrInvalidNature
	}
	if err := s.validateStats(ctx, command.IncreasedStatID, command.DecreasedStatID); err != nil {
		return Nature{}, err
	}
	value := Nature{ID: command.ID, Code: command.Code, Name: command.Name, IncreasedStatID: cloneID(command.IncreasedStatID), DecreasedStatID: cloneID(command.DecreasedStatID), Enabled: command.Enabled, Version: command.ExpectedVersion + 1}
	var updated Nature
	err := s.store.WithinNature(ctx, func(writer Writer) error {
		var err error
		updated, err = writer.Update(ctx, UpdateRecord{GameDataWriteContext: command.GameDataWriteContext, Nature: value, ExpectedVersion: command.ExpectedVersion, At: s.now().UTC()})
		return err
	})
	return updated, err
}

// Get 返回指定稳定身份的 Nature。
func (s *Service) Get(ctx context.Context, id snowflake.ID) (Nature, error) {
	if id == snowflake.ID(0) {
		return Nature{}, ErrInvalidNature
	}
	return s.store.Get(ctx, id)
}

// List 返回经过规范化筛选的 Nature 资料页。
func (s *Service) List(ctx context.Context, query ListQuery) (Page, error) {
	if query.Page == 0 {
		query.Page = 1
	}
	if query.PageSize == 0 {
		query.PageSize = 20
	}
	query.Q, query.Code, query.Name = strings.TrimSpace(query.Q), strings.TrimSpace(query.Code), strings.TrimSpace(query.Name)
	if query.Page < 1 || query.Page > 1_000_000 || query.PageSize < 1 || query.PageSize > 100 || len([]rune(query.Q)) > 80 || len([]rune(query.Name)) > 80 || (query.Code != "" && !stablecode.Valid(query.Code)) {
		return Page{}, ErrInvalidNature
	}
	return s.store.List(ctx, query)
}

func validNature(code, name string, increased, decreased *snowflake.ID) bool {
	if !stablecode.Valid(code) || name == "" || len([]rune(name)) > 80 {
		return false
	}
	if (increased == nil) != (decreased == nil) {
		return false
	}
	if increased == nil {
		return true
	}
	return *increased != snowflake.ID(0) && *decreased != snowflake.ID(0) && *increased != *decreased
}

func cloneID(value *snowflake.ID) *snowflake.ID {
	if value == nil {
		return nil
	}
	result := *value
	return &result
}

func (s *Service) validateStats(ctx context.Context, increased, decreased *snowflake.ID) error {
	if increased == nil {
		return nil
	}
	if s.stats == nil {
		return ErrInvalidNature
	}
	for _, id := range []snowflake.ID{*increased, *decreased} {
		value, err := s.stats.Get(ctx, id)
		if errors.Is(err, stat.ErrStatNotFound) {
			return ErrInvalidNature
		}
		if err != nil {
			return err
		}
		if !value.Enabled || !natureStatCode(value.Code) {
			return ErrInvalidNature
		}
	}
	return nil
}

func natureStatCode(code string) bool {
	return code == "attack" || code == "defense" || code == "special-attack" || code == "special-defense" || code == "speed"
}
