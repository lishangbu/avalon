package api

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"net/http"
	"strconv"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/effect"
)

const (
	// playerCatalogPageSize 限制每次从 PostgreSQL 读取的实时战斗资料数量。
	playerCatalogPageSize int32 = 500
	// playerCatalogSchemaVersion 标识当前玩家资料目录载荷结构。
	playerCatalogSchemaVersion int32 = 1
)

// PlayerBattleCatalogQuery 按各自领域结构读取启用的实时战斗资料。
type PlayerBattleCatalogQuery interface {
	ListClauses(context.Context, battleformat.ClauseListQuery) (battleformat.ClausePage, error)
	ListRestrictions(context.Context, battleformat.RestrictionListQuery) (battleformat.RestrictionPage, error)
	ListMechanics(context.Context, battleformat.MechanicListQuery) (battleformat.MechanicPage, error)
	ListFormats(context.Context, battleformat.FormatListQuery) (battleformat.FormatPage, error)
}

// PlayerCatalogService 从实时资料表构建玩家只读资料目录。
type PlayerCatalogService struct {
	// battleRules 读取启用的 BattleFormat 及独立规则资料。
	battleRules PlayerBattleCatalogQuery
	// logger 记录数据库或资料解码故障，不向调用方泄漏内部细节。
	logger *slog.Logger
}

// NewPlayerCatalogService 使用实时资料查询边界创建玩家目录服务。
func NewPlayerCatalogService(
	battleRules PlayerBattleCatalogQuery,
	logger *slog.Logger,
) *PlayerCatalogService {
	return &PlayerCatalogService{battleRules: battleRules, logger: logger}
}

// GetPlayerCatalog 构建当前启用的玩家资料目录。
func (service *PlayerCatalogService) GetPlayerCatalog(
	ctx context.Context,
	_ *domainv1.GetPlayerCatalogRequest,
) (*domainv1.GetPlayerCatalogResponse, error) {
	payload, err := service.loadPayload(ctx)
	if err != nil {
		service.logError(ctx, "构建 Player Catalog 失败", err)
		return nil, kratoserrors.InternalServer("PLAYER_CATALOG_QUERY_FAILED", "服务端无法读取玩家资料目录")
	}
	return &domainv1.GetPlayerCatalogResponse{HttpStatusCode: http.StatusOK, Body: &domainv1.PlayerCatalog{
		SchemaVersion: playerCatalogSchemaVersion,
		Payload:       payload,
	}}, nil
}

// loadPayload 分页读取所有启用的玩家战斗资料并保持各资料结构独立。
func (service *PlayerCatalogService) loadPayload(ctx context.Context) (*domainv1.PlayerCatalogPayload, error) {
	enabled := true
	clauses, err := loadClauseMessages(ctx, service.battleRules, &enabled)
	if err != nil {
		return nil, err
	}
	restrictions, err := loadRestrictionMessages(ctx, service.battleRules, &enabled)
	if err != nil {
		return nil, err
	}
	mechanics, err := loadMechanicMessages(ctx, service.battleRules, &enabled)
	if err != nil {
		return nil, err
	}
	formats, err := loadFormatMessages(ctx, service.battleRules, &enabled)
	if err != nil {
		return nil, err
	}
	return &domainv1.PlayerCatalogPayload{
		BattleClauses: clauses, BattleRestrictions: restrictions,
		BattleMechanics: mechanics, BattleFormats: formats,
	}, nil
}

func loadClauseMessages(ctx context.Context, query PlayerBattleCatalogQuery, enabled *bool) ([]*domainv1.GameBattleClause, error) {
	items := make([]*domainv1.GameBattleClause, 0)
	for page := int32(1); ; page++ {
		result, err := query.ListClauses(ctx, battleformat.ClauseListQuery{Page: page, PageSize: playerCatalogPageSize, Enabled: enabled})
		if err != nil {
			return nil, fmt.Errorf("查询玩家 Battle Clause: %w", err)
		}
		for _, value := range result.Items {
			items = append(items, battleClauseCatalogMessage(value))
		}
		if int64(len(items)) >= result.Total {
			return items, nil
		}
	}
}

func loadRestrictionMessages(ctx context.Context, query PlayerBattleCatalogQuery, enabled *bool) ([]*domainv1.GameBattleRestriction, error) {
	items := make([]*domainv1.GameBattleRestriction, 0)
	for page := int32(1); ; page++ {
		result, err := query.ListRestrictions(ctx, battleformat.RestrictionListQuery{Page: page, PageSize: playerCatalogPageSize, Enabled: enabled})
		if err != nil {
			return nil, fmt.Errorf("查询玩家 Battle Restriction: %w", err)
		}
		for _, value := range result.Items {
			message, err := battleRestrictionCatalogMessage(value)
			if err != nil {
				return nil, err
			}
			items = append(items, message)
		}
		if int64(len(items)) >= result.Total {
			return items, nil
		}
	}
}

func loadMechanicMessages(ctx context.Context, query PlayerBattleCatalogQuery, enabled *bool) ([]*domainv1.GameBattleMechanic, error) {
	items := make([]*domainv1.GameBattleMechanic, 0)
	for page := int32(1); ; page++ {
		result, err := query.ListMechanics(ctx, battleformat.MechanicListQuery{Page: page, PageSize: playerCatalogPageSize, Enabled: enabled})
		if err != nil {
			return nil, fmt.Errorf("查询玩家 Battle Mechanic: %w", err)
		}
		for _, value := range result.Items {
			message, err := battleMechanicCatalogMessage(value)
			if err != nil {
				return nil, err
			}
			items = append(items, message)
		}
		if int64(len(items)) >= result.Total {
			return items, nil
		}
	}
}

func loadFormatMessages(ctx context.Context, query PlayerBattleCatalogQuery, enabled *bool) ([]*domainv1.GameBattleFormat, error) {
	items := make([]*domainv1.GameBattleFormat, 0)
	for page := int32(1); ; page++ {
		result, err := query.ListFormats(ctx, battleformat.FormatListQuery{Page: page, PageSize: playerCatalogPageSize, Enabled: enabled})
		if err != nil {
			return nil, fmt.Errorf("查询玩家 BattleFormat: %w", err)
		}
		for _, value := range result.Items {
			items = append(items, battleFormatCatalogMessage(value))
		}
		if int64(len(items)) >= result.Total {
			return items, nil
		}
	}
}

func battleClauseCatalogMessage(value battleformat.Clause) *domainv1.GameBattleClause {
	return &domainv1.GameBattleClause{
		Id: value.ID.String(), Code: value.Code, Name: value.Name, Description: value.Description,
		Definition: &domainv1.BattleClauseEffectDefinition{
			Kind: value.Definition.Kind, SchemaVersion: value.Definition.SchemaVersion,
			Parameters: &domainv1.EmptyEffectParameters{},
		},
		Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10),
	}
}

func battleRestrictionCatalogMessage(value battleformat.Restriction) (*domainv1.GameBattleRestriction, error) {
	parameters := effect.StableCodeListParameters{}
	if err := json.Unmarshal(value.Definition.Parameters, &parameters); err != nil {
		return nil, fmt.Errorf("解析 Battle Restriction %s 参数: %w", value.ID, err)
	}
	return &domainv1.GameBattleRestriction{
		Id: value.ID.String(), Code: value.Code, Name: value.Name, Description: value.Description,
		Definition: &domainv1.BattleRestrictionEffectDefinition{
			Kind: value.Definition.Kind, SchemaVersion: value.Definition.SchemaVersion,
			Parameters: &domainv1.StableCodeListParameters{
				Mode: parameters.Mode, ResourceType: parameters.ResourceType,
				StableCodes: append([]string(nil), parameters.StableCodes...),
			},
		},
		Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10),
	}, nil
}

func battleMechanicCatalogMessage(value battleformat.Mechanic) (*domainv1.GameBattleMechanic, error) {
	parameters := effect.LevelNormalizationParameters{}
	if err := json.Unmarshal(value.Definition.Parameters, &parameters); err != nil {
		return nil, fmt.Errorf("解析 Battle Mechanic %s 参数: %w", value.ID, err)
	}
	return &domainv1.GameBattleMechanic{
		Id: value.ID.String(), Code: value.Code, Name: value.Name, Description: value.Description,
		Definition: &domainv1.BattleMechanicEffectDefinition{
			Kind: value.Definition.Kind, SchemaVersion: value.Definition.SchemaVersion,
			Parameters: &domainv1.LevelNormalizationParameters{Level: parameters.Level},
		},
		Enabled: value.Enabled, Version: strconv.FormatInt(value.Version, 10),
	}, nil
}

func battleFormatCatalogMessage(value battleformat.Format) *domainv1.GameBattleFormat {
	return &domainv1.GameBattleFormat{
		Id: value.ID.String(), Code: value.Code, Name: value.Name, Description: value.Description,
		Mode: string(value.Mode), RosterCount: value.RosterCount, SelectCount: value.SelectCount,
		ActiveParticipantsPerSide: value.ActiveParticipantsPerSide,
		LevelRule:                 &domainv1.BattleLevelRule{Mode: string(value.LevelRule.Mode), Level: value.LevelRule.Level},
		Deadlines: &domainv1.BattleDeadlines{
			PreviewSeconds: value.Deadlines.PreviewSeconds, TurnSeconds: value.Deadlines.TurnSeconds,
			BattleSeconds: value.Deadlines.BattleSeconds,
		},
		Availability: &domainv1.BattleFormatAvailability{
			Challenge: value.Availability.Challenge, Training: value.Availability.Training, Encounter: value.Availability.Encounter,
			AdminPreview: value.Availability.AdminPreview,
		},
		ClauseIds: identifierStrings(value.ClauseIDs), RestrictionIds: identifierStrings(value.RestrictionIDs),
		MechanicIds: identifierStrings(value.MechanicIDs), Default: value.Default, Enabled: value.Enabled,
		Version: strconv.FormatInt(value.Version, 10),
	}
}

// identifierStrings 把领域 Identifier 列表复制为 Protobuf 使用的稳定字符串列表。
func identifierStrings(values []snowflake.ID) []string {
	result := make([]string, len(values))
	for index, value := range values {
		result[index] = value.String()
	}
	return result
}

func (service *PlayerCatalogService) logError(ctx context.Context, message string, err error) {
	if service.logger != nil {
		service.logger.ErrorContext(ctx, message, "error", err.Error())
	}
}
