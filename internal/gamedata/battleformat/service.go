package battleformat

import (
	"context"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/effect"
	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
)

// Service 编排 BattleFormat 与三类规则组件的 实时资料 CRUD。
type Service struct {
	repository BattleRuleRepository
	registry   *effect.Registry
	newID      snowflake.Source
	now        func() time.Time
}

// NewService 使用显式依赖创建战斗规则应用服务。
func NewService(repository BattleRuleRepository, registry *effect.Registry, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{repository: repository, registry: registry, newID: newID, now: now}
}

// CreateFormat 在实时资料中创建版本为 1 的 BattleFormat。
func (s *Service) CreateFormat(ctx context.Context, command CreateFormatCommand) (Format, error) {
	format, valid := normalizeFormat(command, 1)
	command.GameDataWriteContext = command.Normalize()
	if !valid || !command.Valid() {
		return Format{}, ErrInvalidFormat
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Format{}, idErr
	}
	format.ID = id
	var created Format
	err := s.repository.WithinBattleRules(ctx, func(writer Writer) error {
		var createErr error
		created, createErr = writer.CreateFormat(ctx, CreateFormatRecord{
			GameDataWriteContext: command.GameDataWriteContext, Format: format, CreatedAt: s.now().UTC(),
		})
		return createErr
	})
	return created, err
}

// UpdateFormat 使用格式版本与 实时资料 版本双重乐观锁完整替换 BattleFormat。
func (s *Service) UpdateFormat(ctx context.Context, command UpdateFormatCommand) (Format, error) {
	format, valid := normalizeFormat(command.CreateFormatCommand, command.ExpectedVersion+1)
	command.GameDataWriteContext = command.Normalize()
	if !valid || !command.Valid() || command.FormatID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return Format{}, ErrInvalidFormat
	}
	format.ID = command.FormatID
	var updated Format
	err := s.repository.WithinBattleRules(ctx, func(writer Writer) error {
		var updateErr error
		updated, updateErr = writer.UpdateFormat(ctx, UpdateFormatRecord{
			GameDataWriteContext: command.GameDataWriteContext, Format: format,
			ExpectedVersion: command.ExpectedVersion, UpdatedAt: s.now().UTC(),
		})
		return updateErr
	})
	return updated, err
}

// DisableFormat 使用乐观版本禁用实时资料中的一条 BattleFormat。
func (s *Service) DisableFormat(ctx context.Context, command DisableFormatCommand) error {
	command.GameDataWriteContext = command.Normalize()
	if !command.Valid() || command.FormatID == snowflake.ID(0) || command.ExpectedVersion < 1 {
		return ErrInvalidFormat
	}
	return s.repository.WithinBattleRules(ctx, func(writer Writer) error {
		return writer.DisableFormat(ctx, DisableFormatRecord{
			GameDataWriteContext: command.GameDataWriteContext, FormatID: command.FormatID,
			ExpectedVersion: command.ExpectedVersion, DisabledAt: s.now().UTC(),
		})
	})
}

// GetFormat 读取实时资料中指定稳定身份的 BattleFormat。
func (s *Service) GetFormat(ctx context.Context, id snowflake.ID) (Format, error) {
	if id == snowflake.ID(0) {
		return Format{}, ErrInvalidFormat
	}
	return s.repository.GetFormat(ctx, id)
}

// ListFormats 返回实时资料中符合条件的 BattleFormat 页。
func (s *Service) ListFormats(ctx context.Context, query FormatListQuery) (FormatPage, error) {
	query.Q = strings.TrimSpace(query.Q)
	if query.Page == 0 {
		query.Page = 1
	}
	if query.PageSize == 0 {
		query.PageSize = 20
	}
	if query.Page < 1 || query.Page > 1_000_000 || query.PageSize < 1 || query.PageSize > 100 ||
		len([]rune(query.Q)) > 80 || (query.Mode != "" && query.Mode != ModeSingle && query.Mode != ModeDouble) {
		return FormatPage{}, ErrInvalidFormat
	}
	return s.repository.ListFormats(ctx, query)
}

func normalizeFormat(command CreateFormatCommand, version int64) (Format, bool) {
	command.Code, command.Name, command.Description = strings.TrimSpace(command.Code), strings.TrimSpace(command.Name), strings.TrimSpace(command.Description)
	validCounts := command.RosterCount >= 1 && command.RosterCount <= 6 && command.SelectCount >= 1 &&
		command.SelectCount <= command.RosterCount && command.ActiveParticipantsPerSide >= 1 &&
		command.ActiveParticipantsPerSide <= command.SelectCount
	validMode := command.Mode == ModeSingle && command.ActiveParticipantsPerSide == 1 ||
		command.Mode == ModeDouble && command.ActiveParticipantsPerSide == 2
	validLevel := command.LevelRule.Mode == LevelRulePreserve && command.LevelRule.Level == nil ||
		command.LevelRule.Mode == LevelRuleNormalize && command.LevelRule.Level != nil &&
			*command.LevelRule.Level >= 1 && *command.LevelRule.Level <= 100
	validDeadlines := command.Deadlines.PreviewSeconds >= 10 && command.Deadlines.PreviewSeconds <= 600 &&
		command.Deadlines.TurnSeconds >= 10 && command.Deadlines.TurnSeconds <= 600 &&
		command.Deadlines.BattleSeconds >= 60 && command.Deadlines.BattleSeconds <= 14_400
	validAvailability := command.Availability.Challenge || command.Availability.Training || command.Availability.Encounter || command.Availability.AdminPreview
	validDefault := command.Default == (command.Code == "standard-single")
	if !stablecode.Valid(command.Code) || command.Name == "" || len([]rune(command.Name)) > 80 ||
		len([]rune(command.Description)) > 500 || !validCounts || !validMode || !validLevel || !validDeadlines ||
		!validAvailability || !validDefault || !validIdentifierSet(command.ClauseIDs, 50) ||
		!validIdentifierSet(command.RestrictionIDs, 50) || !validIdentifierSet(command.MechanicIDs, 50) {
		return Format{}, false
	}
	return Format{Code: command.Code, Name: command.Name, Description: command.Description, Mode: command.Mode,
		RosterCount: command.RosterCount, SelectCount: command.SelectCount,
		ActiveParticipantsPerSide: command.ActiveParticipantsPerSide, LevelRule: command.LevelRule,
		Deadlines: command.Deadlines, Availability: command.Availability,
		ClauseIDs: append([]snowflake.ID(nil), command.ClauseIDs...), RestrictionIDs: append([]snowflake.ID(nil), command.RestrictionIDs...),
		MechanicIDs: append([]snowflake.ID(nil), command.MechanicIDs...), Default: command.Default, Enabled: command.Enabled, Version: version}, true
}

func validIdentifierSet(ids []snowflake.ID, maximum int) bool {
	if len(ids) > maximum {
		return false
	}
	seen := make(map[snowflake.ID]struct{}, len(ids))
	for _, id := range ids {
		if id == snowflake.ID(0) {
			return false
		}
		if _, exists := seen[id]; exists {
			return false
		}
		seen[id] = struct{}{}
	}
	return true
}
