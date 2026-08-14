package battleformat

import (
	"context"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/effect"
	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
)

// Clause 是实时资料中可被 BattleFormat 引用的一条整场条款。
type Clause struct {
	ID          snowflake.ID
	Code        string
	Name        string
	Description string
	Definition  effect.Definition
	Enabled     bool
	Version     int64
}

// ClauseListQuery 是 Clause 管理列表的有界分页条件。
type ClauseListQuery struct {
	Page     int32
	PageSize int32
	Q        string
	Enabled  *bool
}

// ClausePage 是 Clause 管理列表的分页结果。
type ClausePage struct {
	Items    []Clause
	Total    int64
	Page     int32
	PageSize int32
}

// CreateClauseCommand 包含创建 Clause 所需的全部管理输入。
type CreateClauseCommand struct {
	administration.GameDataWriteContext
	Code        string
	Name        string
	Description string
	Definition  effect.Definition
	Enabled     bool
}

// UpdateClauseCommand 使用乐观版本完整替换一条 Clause。
type UpdateClauseCommand struct {
	CreateClauseCommand
	ClauseID        snowflake.ID
	ExpectedVersion int64
}

// DisableClauseCommand 禁用未被 BattleFormat 引用的 Clause。
type DisableClauseCommand struct {
	administration.GameDataWriteContext
	ClauseID        snowflake.ID
	ExpectedVersion int64
}

// CreateClauseRecord 是 Clause 创建事务所需的完整事实。
type CreateClauseRecord struct {
	administration.GameDataWriteContext
	Clause    Clause
	CreatedAt time.Time
}

// UpdateClauseRecord 是 Clause 更新事务所需的完整事实。
type UpdateClauseRecord struct {
	administration.GameDataWriteContext
	Clause          Clause
	ExpectedVersion int64
	UpdatedAt       time.Time
}

// DisableClauseRecord 是 Clause 禁用事务所需的完整事实。
type DisableClauseRecord struct {
	administration.GameDataWriteContext
	ClauseID        snowflake.ID
	ExpectedVersion int64
	DisabledAt      time.Time
}

// Restriction 是实时资料中可被 BattleFormat 引用的一条资料选择限制。
type Restriction struct {
	ID          snowflake.ID
	Code        string
	Name        string
	Description string
	Definition  effect.Definition
	Enabled     bool
	Version     int64
}

// RestrictionListQuery 是 Restriction 管理列表的有界分页条件。
type RestrictionListQuery struct {
	Page     int32
	PageSize int32
	Q        string
	Enabled  *bool
}

// RestrictionPage 是 Restriction 管理列表的分页结果。
type RestrictionPage struct {
	Items    []Restriction
	Total    int64
	Page     int32
	PageSize int32
}

// CreateRestrictionCommand 包含创建 Restriction 所需的全部管理输入。
type CreateRestrictionCommand struct {
	administration.GameDataWriteContext
	Code        string
	Name        string
	Description string
	Definition  effect.Definition
	Enabled     bool
}

// UpdateRestrictionCommand 使用乐观版本完整替换一条 Restriction。
type UpdateRestrictionCommand struct {
	CreateRestrictionCommand
	RestrictionID   snowflake.ID
	ExpectedVersion int64
}

// DisableRestrictionCommand 禁用未被 BattleFormat 引用的 Restriction。
type DisableRestrictionCommand struct {
	administration.GameDataWriteContext
	RestrictionID   snowflake.ID
	ExpectedVersion int64
}

// CreateRestrictionRecord 是 Restriction 创建事务所需的完整事实。
type CreateRestrictionRecord struct {
	administration.GameDataWriteContext
	Restriction Restriction
	CreatedAt   time.Time
}

// UpdateRestrictionRecord 是 Restriction 更新事务所需的完整事实。
type UpdateRestrictionRecord struct {
	administration.GameDataWriteContext
	Restriction     Restriction
	ExpectedVersion int64
	UpdatedAt       time.Time
}

// DisableRestrictionRecord 是 Restriction 禁用事务所需的完整事实。
type DisableRestrictionRecord struct {
	administration.GameDataWriteContext
	RestrictionID   snowflake.ID
	ExpectedVersion int64
	DisabledAt      time.Time
}

// Mechanic 是实时资料中可被 BattleFormat 引用的一条特殊运行时机制。
type Mechanic struct {
	ID          snowflake.ID
	Code        string
	Name        string
	Description string
	Definition  effect.Definition
	Enabled     bool
	Version     int64
}

// MechanicListQuery 是 Mechanic 管理列表的有界分页条件。
type MechanicListQuery struct {
	Page     int32
	PageSize int32
	Q        string
	Enabled  *bool
}

// MechanicPage 是 Mechanic 管理列表的分页结果。
type MechanicPage struct {
	Items    []Mechanic
	Total    int64
	Page     int32
	PageSize int32
}

// CreateMechanicCommand 包含创建 Mechanic 所需的全部管理输入。
type CreateMechanicCommand struct {
	administration.GameDataWriteContext
	Code        string
	Name        string
	Description string
	Definition  effect.Definition
	Enabled     bool
}

// UpdateMechanicCommand 使用乐观版本完整替换一条 Mechanic。
type UpdateMechanicCommand struct {
	CreateMechanicCommand
	MechanicID      snowflake.ID
	ExpectedVersion int64
}

// DisableMechanicCommand 禁用未被 BattleFormat 引用的 Mechanic。
type DisableMechanicCommand struct {
	administration.GameDataWriteContext
	MechanicID      snowflake.ID
	ExpectedVersion int64
}

// CreateMechanicRecord 是 Mechanic 创建事务所需的完整事实。
type CreateMechanicRecord struct {
	administration.GameDataWriteContext
	Mechanic  Mechanic
	CreatedAt time.Time
}

// UpdateMechanicRecord 是 Mechanic 更新事务所需的完整事实。
type UpdateMechanicRecord struct {
	administration.GameDataWriteContext
	Mechanic        Mechanic
	ExpectedVersion int64
	UpdatedAt       time.Time
}

// DisableMechanicRecord 是 Mechanic 禁用事务所需的完整事实。
type DisableMechanicRecord struct {
	administration.GameDataWriteContext
	MechanicID      snowflake.ID
	ExpectedVersion int64
	DisabledAt      time.Time
}

// CreateClause 创建一条独立 Clause 管理资源。
func (s *Service) CreateClause(ctx context.Context, command CreateClauseCommand) (Clause, error) {
	clause, valid := s.normalizeClause(command, 1)
	command.GameDataWriteContext = command.Normalize()
	if !valid || !command.Valid() {
		return Clause{}, ErrInvalidClause
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Clause{}, idErr
	}
	clause.ID = id
	var created Clause
	err := s.repository.WithinBattleRules(ctx, func(writer Writer) error {
		var createErr error
		created, createErr = writer.CreateClause(ctx, CreateClauseRecord{
			GameDataWriteContext: command.GameDataWriteContext, Clause: clause, CreatedAt: s.now().UTC(),
		})
		return createErr
	})
	return created, err
}

// UpdateClause 更新一条独立 Clause 管理资源。
func (s *Service) UpdateClause(ctx context.Context, command UpdateClauseCommand) (Clause, error) {
	clause, valid := s.normalizeClause(command.CreateClauseCommand, command.ExpectedVersion+1)
	command.GameDataWriteContext = command.Normalize()
	if !valid || !command.Valid() || command.ClauseID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return Clause{}, ErrInvalidClause
	}
	clause.ID = command.ClauseID
	var updated Clause
	err := s.repository.WithinBattleRules(ctx, func(writer Writer) error {
		var updateErr error
		updated, updateErr = writer.UpdateClause(ctx, UpdateClauseRecord{
			GameDataWriteContext: command.GameDataWriteContext, Clause: clause,
			ExpectedVersion: command.ExpectedVersion, UpdatedAt: s.now().UTC(),
		})
		return updateErr
	})
	return updated, err
}

// DisableClause 禁用一条独立 Clause 管理资源。
func (s *Service) DisableClause(ctx context.Context, command DisableClauseCommand) error {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.ClauseID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return ErrInvalidClause
	}
	return s.repository.WithinBattleRules(ctx, func(writer Writer) error {
		return writer.DisableClause(ctx, DisableClauseRecord{
			GameDataWriteContext: command.GameDataWriteContext, ClauseID: command.ClauseID,
			ExpectedVersion: command.ExpectedVersion, DisabledAt: s.now().UTC(),
		})
	})
}

// GetClause 读取一条独立 Clause 管理资源。
func (s *Service) GetClause(ctx context.Context, id snowflake.ID) (Clause, error) {
	if id == snowflake.ID(0) {
		return Clause{}, ErrInvalidClause
	}
	return s.repository.GetClause(ctx, id)
}

// ListClauses 返回 Clause 的稳定分页结果。
func (s *Service) ListClauses(ctx context.Context, query ClauseListQuery) (ClausePage, error) {
	query.Q = strings.TrimSpace(query.Q)
	query.Page, query.PageSize = normalizePage(query.Page, query.PageSize)
	if !validPage(query.Page, query.PageSize, query.Q) {
		return ClausePage{}, ErrInvalidClause
	}
	return s.repository.ListClauses(ctx, query)
}

// CreateRestriction 创建一条独立 Restriction 管理资源。
func (s *Service) CreateRestriction(ctx context.Context, command CreateRestrictionCommand) (Restriction, error) {
	restriction, valid := s.normalizeRestriction(command, 1)
	command.GameDataWriteContext = command.Normalize()
	if !valid || !command.Valid() {
		return Restriction{}, ErrInvalidRestriction
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Restriction{}, idErr
	}
	restriction.ID = id
	var created Restriction
	err := s.repository.WithinBattleRules(ctx, func(writer Writer) error {
		var createErr error
		created, createErr = writer.CreateRestriction(ctx, CreateRestrictionRecord{
			GameDataWriteContext: command.GameDataWriteContext, Restriction: restriction, CreatedAt: s.now().UTC(),
		})
		return createErr
	})
	return created, err
}

// UpdateRestriction 更新一条独立 Restriction 管理资源。
func (s *Service) UpdateRestriction(ctx context.Context, command UpdateRestrictionCommand) (Restriction, error) {
	restriction, valid := s.normalizeRestriction(command.CreateRestrictionCommand, command.ExpectedVersion+1)
	command.GameDataWriteContext = command.Normalize()
	if !valid || !command.Valid() || command.RestrictionID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return Restriction{}, ErrInvalidRestriction
	}
	restriction.ID = command.RestrictionID
	var updated Restriction
	err := s.repository.WithinBattleRules(ctx, func(writer Writer) error {
		var updateErr error
		updated, updateErr = writer.UpdateRestriction(ctx, UpdateRestrictionRecord{
			GameDataWriteContext: command.GameDataWriteContext, Restriction: restriction,
			ExpectedVersion: command.ExpectedVersion, UpdatedAt: s.now().UTC(),
		})
		return updateErr
	})
	return updated, err
}

// DisableRestriction 禁用一条独立 Restriction 管理资源。
func (s *Service) DisableRestriction(ctx context.Context, command DisableRestrictionCommand) error {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.RestrictionID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return ErrInvalidRestriction
	}
	return s.repository.WithinBattleRules(ctx, func(writer Writer) error {
		return writer.DisableRestriction(ctx, DisableRestrictionRecord{
			GameDataWriteContext: command.GameDataWriteContext, RestrictionID: command.RestrictionID,
			ExpectedVersion: command.ExpectedVersion, DisabledAt: s.now().UTC(),
		})
	})
}

// GetRestriction 读取一条独立 Restriction 管理资源。
func (s *Service) GetRestriction(ctx context.Context, id snowflake.ID) (Restriction, error) {
	if id == snowflake.ID(0) {
		return Restriction{}, ErrInvalidRestriction
	}
	return s.repository.GetRestriction(ctx, id)
}

// ListRestrictions 返回 Restriction 的稳定分页结果。
func (s *Service) ListRestrictions(ctx context.Context, query RestrictionListQuery) (RestrictionPage, error) {
	query.Q = strings.TrimSpace(query.Q)
	query.Page, query.PageSize = normalizePage(query.Page, query.PageSize)
	if !validPage(query.Page, query.PageSize, query.Q) {
		return RestrictionPage{}, ErrInvalidRestriction
	}
	return s.repository.ListRestrictions(ctx, query)
}

// CreateMechanic 创建一条独立 Mechanic 管理资源。
func (s *Service) CreateMechanic(ctx context.Context, command CreateMechanicCommand) (Mechanic, error) {
	mechanic, valid := s.normalizeMechanic(command, 1)
	command.GameDataWriteContext = command.Normalize()
	if !valid || !command.Valid() {
		return Mechanic{}, ErrInvalidMechanic
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Mechanic{}, idErr
	}
	mechanic.ID = id
	var created Mechanic
	err := s.repository.WithinBattleRules(ctx, func(writer Writer) error {
		var createErr error
		created, createErr = writer.CreateMechanic(ctx, CreateMechanicRecord{
			GameDataWriteContext: command.GameDataWriteContext, Mechanic: mechanic, CreatedAt: s.now().UTC(),
		})
		return createErr
	})
	return created, err
}

// UpdateMechanic 更新一条独立 Mechanic 管理资源。
func (s *Service) UpdateMechanic(ctx context.Context, command UpdateMechanicCommand) (Mechanic, error) {
	mechanic, valid := s.normalizeMechanic(command.CreateMechanicCommand, command.ExpectedVersion+1)
	command.GameDataWriteContext = command.Normalize()
	if !valid || !command.Valid() || command.MechanicID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return Mechanic{}, ErrInvalidMechanic
	}
	mechanic.ID = command.MechanicID
	var updated Mechanic
	err := s.repository.WithinBattleRules(ctx, func(writer Writer) error {
		var updateErr error
		updated, updateErr = writer.UpdateMechanic(ctx, UpdateMechanicRecord{
			GameDataWriteContext: command.GameDataWriteContext, Mechanic: mechanic,
			ExpectedVersion: command.ExpectedVersion, UpdatedAt: s.now().UTC(),
		})
		return updateErr
	})
	return updated, err
}

// DisableMechanic 禁用一条独立 Mechanic 管理资源。
func (s *Service) DisableMechanic(ctx context.Context, command DisableMechanicCommand) error {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.MechanicID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return ErrInvalidMechanic
	}
	return s.repository.WithinBattleRules(ctx, func(writer Writer) error {
		return writer.DisableMechanic(ctx, DisableMechanicRecord{
			GameDataWriteContext: command.GameDataWriteContext, MechanicID: command.MechanicID,
			ExpectedVersion: command.ExpectedVersion, DisabledAt: s.now().UTC(),
		})
	})
}

// GetMechanic 读取一条独立 Mechanic 管理资源。
func (s *Service) GetMechanic(ctx context.Context, id snowflake.ID) (Mechanic, error) {
	if id == snowflake.ID(0) {
		return Mechanic{}, ErrInvalidMechanic
	}
	return s.repository.GetMechanic(ctx, id)
}

// ListMechanics 返回 Mechanic 的稳定分页结果。
func (s *Service) ListMechanics(ctx context.Context, query MechanicListQuery) (MechanicPage, error) {
	query.Q = strings.TrimSpace(query.Q)
	query.Page, query.PageSize = normalizePage(query.Page, query.PageSize)
	if !validPage(query.Page, query.PageSize, query.Q) {
		return MechanicPage{}, ErrInvalidMechanic
	}
	return s.repository.ListMechanics(ctx, query)
}

func (s *Service) normalizeClause(command CreateClauseCommand, version int64) (Clause, bool) {
	code, name, description := strings.TrimSpace(command.Code), strings.TrimSpace(command.Name), strings.TrimSpace(command.Description)
	if s.registry == nil || !strings.HasPrefix(command.Definition.Kind, "battle.clause.") || !stablecode.Valid(code) ||
		name == "" || len([]rune(name)) > 80 || len([]rune(description)) > 500 {
		return Clause{}, false
	}
	compiled, issues := s.registry.Compile(command.Definition)
	if len(issues) > 0 {
		return Clause{}, false
	}
	definition := effect.Definition(compiled)
	return Clause{Code: code, Name: name, Description: description, Definition: definition, Enabled: command.Enabled, Version: version}, true
}

func (s *Service) normalizeRestriction(command CreateRestrictionCommand, version int64) (Restriction, bool) {
	code, name, description := strings.TrimSpace(command.Code), strings.TrimSpace(command.Name), strings.TrimSpace(command.Description)
	if s.registry == nil || !strings.HasPrefix(command.Definition.Kind, "battle.restriction.") || !stablecode.Valid(code) ||
		name == "" || len([]rune(name)) > 80 || len([]rune(description)) > 500 {
		return Restriction{}, false
	}
	compiled, issues := s.registry.Compile(command.Definition)
	if len(issues) > 0 {
		return Restriction{}, false
	}
	definition := effect.Definition(compiled)
	return Restriction{Code: code, Name: name, Description: description, Definition: definition, Enabled: command.Enabled, Version: version}, true
}

func (s *Service) normalizeMechanic(command CreateMechanicCommand, version int64) (Mechanic, bool) {
	code, name, description := strings.TrimSpace(command.Code), strings.TrimSpace(command.Name), strings.TrimSpace(command.Description)
	if s.registry == nil || !strings.HasPrefix(command.Definition.Kind, "battle.mechanic.") || !stablecode.Valid(code) ||
		name == "" || len([]rune(name)) > 80 || len([]rune(description)) > 500 {
		return Mechanic{}, false
	}
	compiled, issues := s.registry.Compile(command.Definition)
	if len(issues) > 0 {
		return Mechanic{}, false
	}
	definition := effect.Definition(compiled)
	return Mechanic{Code: code, Name: name, Description: description, Definition: definition, Enabled: command.Enabled, Version: version}, true
}

func normalizePage(page, pageSize int32) (int32, int32) {
	if page == 0 {
		page = 1
	}
	if pageSize == 0 {
		pageSize = 20
	}
	return page, pageSize
}

func validPage(page, pageSize int32, q string) bool {
	return page >= 1 && page <= 1_000_000 && pageSize >= 1 && pageSize <= 100 && len([]rune(q)) <= 80
}
