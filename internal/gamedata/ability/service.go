// Package ability 定义 实时游戏资料 中特性资料的独立命令与查询边界。
package ability

import (
	"context"
	"errors"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/battlerules"
	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
)

var (
	// ErrInvalidAbility 表示特性资料或管理写入上下文未通过边界校验。
	ErrInvalidAbility = errors.New("特性资料无效")
	// ErrAbilityNotFound 表示实时资料中不存在指定特性资料。
	ErrAbilityNotFound = errors.New("特性资料不存在")
	// ErrAbilityVersionConflict 表示特性资料已被其他管理写入修改。
	ErrAbilityVersionConflict = errors.New("特性资料版本冲突")
	// ErrAbilityCodeConflict 表示实时资料中已有资料使用相同稳定编码。
	ErrAbilityCodeConflict = errors.New("特性资料编码已存在")
	// ErrAbilityReferenced 表示其他 实时资料 资料仍然引用目标特性资料。
	ErrAbilityReferenced = errors.New("特性资料仍被引用")
)

// Ability 是管理端读取和写入的一条 实时资料 特性资料。
type Ability struct {
	ID         snowflake.ID
	Code       string
	Name       string
	MainSeries bool
	// Effect 是面向资料维护者的完整机制说明，不参与规则编译。
	Effect *string
	// ShortEffect 是面向快速查阅的简短机制说明。
	ShortEffect *string
	// Introduction 是面向玩家展示的简体中文特性简介。
	Introduction *string
	// Rules 是按 Battle Engine 执行时机组织并经过严格校验的规则文档。
	Rules   battlerules.Ability
	Enabled bool
	Version int64
}

// Sort 声明管理端特性资料列表允许使用的稳定排序。
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
)

// ListQuery 是实时资料 特性资料列表的显式分页、筛选和排序条件。
type ListQuery struct {
	Page       int32
	PageSize   int32
	Q          string
	Code       string
	Name       string
	MainSeries *bool
	Enabled    *bool
	Sort       Sort
}

// Page 是管理端特性资料的有界分页结果。
type Page struct {
	Items    []Ability
	Total    int64
	Page     int32
	PageSize int32
}

// CreateCommand 包含创建特性资料所需的业务字段和管理写入上下文。
type CreateCommand struct {
	administration.GameDataWriteContext
	Code         string
	Name         string
	MainSeries   bool
	Effect       *string
	ShortEffect  *string
	Introduction *string
	Rules        battlerules.Ability
	Enabled      bool
}

// CreateRecord 是存储层原子创建资料、审计和幂等响应所需的完整事实。
type CreateRecord struct {
	administration.GameDataWriteContext
	Ability   Ability
	CreatedAt time.Time
}

// UpdateCommand 使用完整资料表示和预期版本更新 实时资料中的一条特性资料。
type UpdateCommand struct {
	administration.GameDataWriteContext
	AbilityID       snowflake.ID
	ExpectedVersion int64
	Code            string
	Name            string
	MainSeries      bool
	Effect          *string
	ShortEffect     *string
	Introduction    *string
	Rules           battlerules.Ability
	Enabled         bool
}

// UpdateRecord 是存储层原子更新资料、审计记录和幂等响应所需的完整事实。
type UpdateRecord struct {
	administration.GameDataWriteContext
	Ability         Ability
	ExpectedVersion int64
	UpdatedAt       time.Time
}

// DisableCommand 使用预期版本禁用 实时资料中未被引用的一条特性资料。
type DisableCommand struct {
	administration.GameDataWriteContext
	AbilityID       snowflake.ID
	ExpectedVersion int64
}

// DisableRecord 是存储层原子禁用资料、审计记录和幂等响应所需的完整事实。
type DisableRecord struct {
	administration.GameDataWriteContext
	AbilityID       snowflake.ID
	ExpectedVersion int64
	DisabledAt      time.Time
}

// Writer 是一次特性资料管理事务内使用的最小写入边界。
type Writer interface {
	Create(context.Context, CreateRecord) (Ability, error)
	Update(context.Context, UpdateRecord) (Ability, error)
	Disable(context.Context, DisableRecord) error
}

// AbilityReader 返回指定特性领域对象。
type AbilityReader interface {
	GetAbility(context.Context, snowflake.ID) (Ability, error)
}

// AbilityQuery 返回特性资料分页管理投影。
type AbilityQuery interface {
	ListAbilities(context.Context, ListQuery) (Page, error)
}

// AbilityRepository 提供由应用服务划定范围的特性资料事务写入边界。
type AbilityRepository interface {
	WithinAbility(context.Context, func(Writer) error) error
}

// Service 编排特性资料的独立校验、身份生成和持久化命令。
type Service struct {
	// reader 返回指定特性领域对象。
	reader AbilityReader
	// query 返回特性分页管理投影。
	query      AbilityQuery
	repository AbilityRepository
	newID      snowflake.Source
	now        func() time.Time
}

// NewService 使用显式依赖创建特性资料应用服务。
func NewService(reader AbilityReader, query AbilityQuery, repository AbilityRepository, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{reader: reader, query: query, repository: repository, newID: newID, now: now}
}

// Create 在当前实时资料中创建版本为 1 的特性资料。
func (s *Service) Create(ctx context.Context, command CreateCommand) (Ability, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.Effect = normalizeText(command.Effect)
	command.ShortEffect = normalizeText(command.ShortEffect)
	command.Introduction = normalizeText(command.Introduction)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || !stablecode.Valid(command.Code) ||
		command.Name == "" || len([]rune(command.Name)) > 120 || !validText(command.Effect, 20_000) ||
		!validText(command.ShortEffect, 500) || !validText(command.Introduction, 500) || !validRules(command.Rules) {
		return Ability{}, ErrInvalidAbility
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Ability{}, idErr
	}
	ability := Ability{
		ID: id, Code: command.Code, Name: command.Name,
		MainSeries: command.MainSeries, Effect: command.Effect, ShortEffect: command.ShortEffect,
		Introduction: command.Introduction, Rules: command.Rules, Enabled: command.Enabled, Version: 1,
	}
	var created Ability
	err := s.repository.WithinAbility(ctx, func(writer Writer) error {
		var createErr error
		created, createErr = writer.Create(ctx, CreateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Ability: ability, CreatedAt: s.now().UTC(),
		})
		return createErr
	})
	if err != nil {
		return Ability{}, err
	}
	return created, nil
}

// Update 使用乐观版本替换 实时资料中的完整资料字段并递增版本。
func (s *Service) Update(ctx context.Context, command UpdateCommand) (Ability, error) {
	command.Code = strings.TrimSpace(command.Code)
	command.Name = strings.TrimSpace(command.Name)
	command.Effect = normalizeText(command.Effect)
	command.ShortEffect = normalizeText(command.ShortEffect)
	command.Introduction = normalizeText(command.Introduction)
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.AbilityID == snowflake.ID(0) || command.ExpectedVersion < 1 ||
		!stablecode.Valid(command.Code) || command.Name == "" || len([]rune(command.Name)) > 120 ||
		!validText(command.Effect, 20_000) || !validText(command.ShortEffect, 500) ||
		!validText(command.Introduction, 500) || !validRules(command.Rules) {
		return Ability{}, ErrInvalidAbility
	}
	ability := Ability{
		ID: command.AbilityID, Code: command.Code, Name: command.Name,
		MainSeries: command.MainSeries, Effect: command.Effect, ShortEffect: command.ShortEffect,
		Introduction: command.Introduction, Rules: command.Rules, Enabled: command.Enabled, Version: command.ExpectedVersion + 1,
	}
	var updated Ability
	err := s.repository.WithinAbility(ctx, func(writer Writer) error {
		var updateErr error
		updated, updateErr = writer.Update(ctx, UpdateRecord{
			GameDataWriteContext: command.GameDataWriteContext, Ability: ability,
			ExpectedVersion: command.ExpectedVersion, UpdatedAt: s.now().UTC(),
		})
		return updateErr
	})
	if err != nil {
		return Ability{}, err
	}
	return updated, nil
}

func normalizeText(value *string) *string {
	if value == nil {
		return nil
	}
	trimmed := strings.TrimSpace(*value)
	return &trimmed
}

func validText(value *string, maximum int) bool {
	return value == nil || *value != "" && len([]rune(*value)) <= maximum
}

func validRules(rules battlerules.Ability) bool {
	_, valid := rules.Values()
	return valid
}

// Get 读取当前实时资料中指定稳定身份的特性资料。
func (s *Service) Get(ctx context.Context, abilityID snowflake.ID) (Ability, error) {
	if abilityID == snowflake.ID(0) {
		return Ability{}, ErrInvalidAbility
	}
	return s.reader.GetAbility(ctx, abilityID)
}

// List 返回当前实时资料中经过显式筛选和稳定排序的特性资料页。
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
		return Page{}, ErrInvalidAbility
	}
	return s.query.ListAbilities(ctx, query)
}

func validSort(sort Sort) bool {
	switch sort {
	case SortCodeAscending, SortCodeDescending, SortNameAscending, SortNameDescending:
		return true
	default:
		return false
	}
}

// Delete 使用乐观版本禁用当前实时资料中未被引用的特性资料。
func (s *Service) Disable(ctx context.Context, command DisableCommand) error {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.AbilityID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return ErrInvalidAbility
	}
	return s.repository.WithinAbility(ctx, func(writer Writer) error {
		return writer.Disable(ctx, DisableRecord{
			GameDataWriteContext: command.GameDataWriteContext, AbilityID: command.AbilityID,
			ExpectedVersion: command.ExpectedVersion, DisabledAt: s.now().UTC(),
		})
	})
}
