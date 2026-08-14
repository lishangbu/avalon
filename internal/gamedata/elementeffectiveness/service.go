// Package elementeffectiveness 定义实时游戏资料中属性克制倍率的独立管理边界。
package elementeffectiveness

import (
	"context"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
)

var (
	// ErrInvalidEffectiveness 表示属性引用、倍率或管理写入上下文无效。
	ErrInvalidEffectiveness = errors.New("属性克制资料无效")
	// ErrEffectivenessNotFound 表示指定稳定身份的属性克制资料不存在。
	ErrEffectivenessNotFound = errors.New("属性克制资料不存在")
	// ErrEffectivenessConflict 表示属性组合、乐观版本或全局资料修订发生冲突。
	ErrEffectivenessConflict = errors.New("属性克制资料冲突")
)

// Effectiveness 是一种攻击属性对一种防御属性的非中性规范倍率。
// 实际倍率等于 Numerator / Denominator；没有记录的属性组合由调用方按 1/1 处理。
type Effectiveness struct {
	// ID 是该倍率记录的稳定 Identifier。
	ID snowflake.ID
	// AttackElementID 是技能所用攻击属性的稳定 Identifier。
	AttackElementID snowflake.ID
	// DefenseElementID 是受击成员单个防御属性的稳定 Identifier。
	DefenseElementID snowflake.ID
	// Numerator 是倍率分子；与 Denominator 组成 2/1、1/2 或 0/1，零表示完全免疫。
	Numerator uint16
	// Denominator 是非零倍率分母；实际倍率等于 Numerator / Denominator。
	Denominator uint16
	// Enabled 表示该非中性关系会进入新对战的规则快照。
	Enabled bool
	// Version 是该记录的乐观并发版本。
	Version int64
}

// ListQuery 是属性克制资料的页码分页和显式筛选条件。
type ListQuery struct {
	// Page 是从一开始的页码。
	Page int32
	// PageSize 是单页最大记录数。
	PageSize int32
	// AttackElementID 可选地筛选攻击属性。
	AttackElementID *snowflake.ID
	// DefenseElementID 可选地筛选防御属性。
	DefenseElementID *snowflake.ID
	// Enabled 可选地筛选启用状态。
	Enabled *bool
}

// Page 是属性克制资料的稳定分页结果。
type Page struct {
	// Items 是按攻击属性、防御属性和 ID 排序的当前页。
	Items []Effectiveness
	// Total 是筛选后的总记录数。
	Total int64
	// Page 是实际页码。
	Page int32
	// PageSize 是实际单页大小。
	PageSize int32
}

// CreateCommand 包含创建属性克制资料所需的全部事实。
type CreateCommand struct {
	// GameDataWriteContext 是维护窗口、修订、操作者和幂等事实。
	administration.GameDataWriteContext
	// AttackElementID 是攻击属性 Identifier。
	AttackElementID snowflake.ID
	// DefenseElementID 是防御属性 Identifier。
	DefenseElementID snowflake.ID
	// Numerator 是规范倍率分子；必须与 Denominator 组成 2/1、1/2 或 0/1。
	Numerator uint16
	// Denominator 是规范倍率分母，不能为零；中性 1/1 不应创建关系。
	Denominator uint16
	// Enabled 表示该关系是否进入新对战。
	Enabled bool
}

// UpdateCommand 包含完整替换属性克制资料所需的全部事实。
type UpdateCommand struct {
	// GameDataWriteContext 是维护窗口、修订、操作者和幂等事实。
	administration.GameDataWriteContext
	// ID 是待更新记录的稳定 Identifier。
	ID snowflake.ID
	// ExpectedVersion 是调用方读取到的乐观版本。
	ExpectedVersion int64
	// AttackElementID 是完整替换后的攻击属性 Identifier。
	AttackElementID snowflake.ID
	// DefenseElementID 是完整替换后的防御属性 Identifier。
	DefenseElementID snowflake.ID
	// Numerator 是完整替换后的规范倍率分子；必须与 Denominator 组成 2/1、1/2 或 0/1。
	Numerator uint16
	// Denominator 是完整替换后的规范倍率分母，不能为零；中性 1/1 不应保存。
	Denominator uint16
	// Enabled 是完整替换后的启用状态。
	Enabled bool
}

// CreateRecord 是存储事务使用的已校验创建事实。
type CreateRecord struct {
	// GameDataWriteContext 是已经通过应用边界校验的管理写入事实。
	administration.GameDataWriteContext
	// Effectiveness 是等待原子创建的完整资料。
	Effectiveness Effectiveness
	// At 是创建、审计和全局修订统一使用的 UTC 时间。
	At time.Time
}

// UpdateRecord 是存储事务使用的已校验更新事实。
type UpdateRecord struct {
	// GameDataWriteContext 是已经通过应用边界校验的管理写入事实。
	administration.GameDataWriteContext
	// Effectiveness 是等待原子替换的完整资料。
	Effectiveness Effectiveness
	// ExpectedVersion 是持久化条件使用的乐观版本。
	ExpectedVersion int64
	// At 是更新、审计和全局修订统一使用的 UTC 时间。
	At time.Time
}

// Writer 是单次属性克制资料事务内的最小写入接口。
type Writer interface {
	Create(context.Context, CreateRecord) (Effectiveness, error)
	Update(context.Context, UpdateRecord) (Effectiveness, error)
}

// ElementEffectivenessReader 返回指定属性克制领域对象。
type ElementEffectivenessReader interface {
	Get(context.Context, snowflake.ID) (Effectiveness, error)
}

// ElementEffectivenessQuery 返回属性克制管理投影与 Battle 冻结输入。
type ElementEffectivenessQuery interface {
	List(context.Context, ListQuery) (Page, error)
	ListEnabled(context.Context) ([]Effectiveness, error)
}

// ElementEffectivenessRepository 提供属性克制资料事务写入边界。
type ElementEffectivenessRepository interface {
	WithinElementEffectiveness(context.Context, func(Writer) error) error
}

// Service 校验并编排属性克制资料命令和查询。
type Service struct {
	reader     ElementEffectivenessReader
	query      ElementEffectivenessQuery
	repository ElementEffectivenessRepository
	newID      snowflake.Source
	now        func() time.Time
}

// NewService 使用显式依赖创建属性克制资料服务。
func NewService(reader ElementEffectivenessReader, query ElementEffectivenessQuery, repository ElementEffectivenessRepository, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{reader: reader, query: query, repository: repository, newID: newID, now: now}
}

// Create 创建版本为一的非中性属性克制倍率。
func (s *Service) Create(ctx context.Context, command CreateCommand) (Effectiveness, error) {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || !validValues(command.AttackElementID, command.DefenseElementID, command.Numerator, command.Denominator) {
		return Effectiveness{}, ErrInvalidEffectiveness
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Effectiveness{}, idErr
	}
	value := Effectiveness{ID: id, AttackElementID: command.AttackElementID, DefenseElementID: command.DefenseElementID,
		Numerator: command.Numerator, Denominator: command.Denominator, Enabled: command.Enabled, Version: 1}
	var created Effectiveness
	err := s.repository.WithinElementEffectiveness(ctx, func(writer Writer) error {
		var err error
		created, err = writer.Create(ctx, CreateRecord{GameDataWriteContext: command.GameDataWriteContext, Effectiveness: value, At: s.now().UTC()})
		return err
	})
	return created, err
}

// Update 使用乐观版本完整替换属性组合、倍率与启用状态。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (Effectiveness, error) {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.ID == snowflake.ID(0) || command.ExpectedVersion < 1 ||
		!validValues(command.AttackElementID, command.DefenseElementID, command.Numerator, command.Denominator) {
		return Effectiveness{}, ErrInvalidEffectiveness
	}
	value := Effectiveness{ID: command.ID, AttackElementID: command.AttackElementID, DefenseElementID: command.DefenseElementID,
		Numerator: command.Numerator, Denominator: command.Denominator, Enabled: command.Enabled, Version: command.ExpectedVersion + 1}
	var updated Effectiveness
	err := s.repository.WithinElementEffectiveness(ctx, func(writer Writer) error {
		var err error
		updated, err = writer.Update(ctx, UpdateRecord{GameDataWriteContext: command.GameDataWriteContext, Effectiveness: value,
			ExpectedVersion: command.ExpectedVersion, At: s.now().UTC()})
		return err
	})
	return updated, err
}

// Get 返回指定稳定身份的属性克制资料。
func (s *Service) Get(ctx context.Context, id snowflake.ID) (Effectiveness, error) {
	if id == snowflake.ID(0) {
		return Effectiveness{}, ErrInvalidEffectiveness
	}
	return s.reader.Get(ctx, id)
}

// List 返回属性克制资料分页。
func (s *Service) List(ctx context.Context, query ListQuery) (Page, error) {
	if query.Page == 0 {
		query.Page = 1
	}
	if query.PageSize == 0 {
		query.PageSize = 20
	}
	if query.Page < 1 || query.Page > 1_000_000 || query.PageSize < 1 || query.PageSize > 100 ||
		(query.AttackElementID != nil && *query.AttackElementID == snowflake.ID(0)) ||
		(query.DefenseElementID != nil && *query.DefenseElementID == snowflake.ID(0)) {
		return Page{}, ErrInvalidEffectiveness
	}
	return s.query.List(ctx, query)
}

// ListEnabled 返回会冻结到新对战的全部非中性倍率。
func (s *Service) ListEnabled(ctx context.Context) ([]Effectiveness, error) {
	return s.query.ListEnabled(ctx)
}

// validValues 只接受可持久化的三种非中性现代属性倍率；中性 1/1 由关系缺省表达。
func validValues(attackID, defenseID snowflake.ID, numerator, denominator uint16) bool {
	if attackID == snowflake.ID(0) || defenseID == snowflake.ID(0) {
		return false
	}
	return (numerator == 0 && denominator == 1) || (numerator == 1 && denominator == 2) || (numerator == 2 && denominator == 1)
}
