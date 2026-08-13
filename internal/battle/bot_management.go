package battle

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
)

var (
	// ErrBotStrategyNotFound 表示指定 Code 与版本的 Bot 策略资料不存在。
	ErrBotStrategyNotFound = errors.New("对战机器人策略不存在")
	// ErrBotStrategyCodeConflict 表示新建请求试图重复使用已经存在的 Bot 稳定代码。
	ErrBotStrategyCodeConflict = errors.New("对战机器人策略编码已存在")
	// ErrBotStrategyVersionConflict 表示发布或禁用操作基于过期版本或已改变的启用状态。
	ErrBotStrategyVersionConflict = errors.New("对战机器人策略版本冲突")
	// ErrBotStrategyStoreUnavailable 表示 Bot 策略管理存储尚未装配或当前不可用。
	ErrBotStrategyStoreUnavailable = errors.New("对战机器人策略存储不可用")
)

// ManagedBotStrategy 是管理端可读取的一条不可变版本 Bot 资料。
type ManagedBotStrategy struct {
	// Code 是 Training Battle 请求使用的稳定 Bot 代码。
	Code string
	// Version 是同一 Code 下只增不改的策略版本。
	Version uint32
	// Enabled 表示该版本能否被新的 Training Battle 选择；同一 Code 最多一条启用版本。
	Enabled bool
	// Definition 是经过严格解析与规范化后保存的完整配置 JSON。
	Definition json.RawMessage
	// CreatedAt 是该不可变版本创建时的 UTC 时间。
	CreatedAt time.Time
}

// BotStrategyListQuery 是管理端查询版本化 Bot 资料的有界分页筛选条件。
type BotStrategyListQuery struct {
	// Page 是从 1 开始的当前页码。
	Page int32
	// PageSize 是单页最多返回的记录数。
	PageSize int32
	// Code 是可选的稳定代码精确筛选条件。
	Code string
	// Enabled 是可选的启用状态筛选条件。
	Enabled *bool
}

// BotStrategyPage 是管理端读取版本化 Bot 资料时的统一页码结果。
type BotStrategyPage struct {
	// Items 是按 Code 升序、Version 降序排列的当前页记录。
	Items []ManagedBotStrategy
	// Page 是从 1 开始的当前页码。
	Page int32
	// PageSize 是实际使用的单页上限。
	PageSize int32
	// Total 是筛选条件下的精确总数。
	Total int64
}

// CreateBotStrategyCommand 创建一个此前不存在 Code 的第一个不可变 Bot 版本。
type CreateBotStrategyCommand struct {
	// GameDataWriteContext 携带管理员身份、幂等键和审计请求 ID。
	administration.GameDataWriteContext
	// Code 是新 Bot 的稳定代码。
	Code string
	// Definition 是管理员提交的严格 Bot 配置 JSON。
	Definition json.RawMessage
}

// PublishNextBotStrategyCommand 为已有 Code 发布一个新不可变版本，并自动停用旧启用版本。
type PublishNextBotStrategyCommand struct {
	// GameDataWriteContext 携带管理员身份、幂等键和审计请求 ID。
	administration.GameDataWriteContext
	// Code 是需要发布新版本的稳定 Bot 代码。
	Code string
	// Definition 是新版本的完整严格 Bot 配置 JSON。
	Definition json.RawMessage
}

// DisableBotStrategyCommand 停用指定 Bot 的当前启用版本，保留全部历史版本供已创建 Battle 追溯。
type DisableBotStrategyCommand struct {
	// GameDataWriteContext 携带管理员身份、幂等键和审计请求 ID。
	administration.GameDataWriteContext
	// Code 是要停用的稳定 Bot 代码。
	Code string
	// Version 是要停用的不可变版本。
	Version uint32
}

// BotStrategyAdministrationStore 约束 Bot 资料管理需要的查询和原子写入边界。
type BotStrategyAdministrationStore interface {
	// GetBotStrategy 返回指定 Code 与版本的不可变资料。
	GetBotStrategy(context.Context, string, uint32) (ManagedBotStrategy, error)
	// ListBotStrategies 返回按稳定顺序分页的资料版本。
	ListBotStrategies(context.Context, BotStrategyListQuery) (BotStrategyPage, error)
	// CreateBotStrategy 创建首个启用版本，并同事务保存审计和幂等记录。
	CreateBotStrategy(context.Context, CreateBotStrategyCommand, json.RawMessage, time.Time) (ManagedBotStrategy, error)
	// PublishNextBotStrategy 创建并启用后继版本，同时停用旧启用版本。
	PublishNextBotStrategy(context.Context, PublishNextBotStrategyCommand, json.RawMessage, time.Time) (ManagedBotStrategy, error)
	// DisableBotStrategy 停用指定版本，并同事务保存审计和幂等记录。
	DisableBotStrategy(context.Context, DisableBotStrategyCommand, time.Time) error
}

// BotStrategyAdministrationService 编排 Bot 资料的严格定义校验和不可变版本生命周期。
type BotStrategyAdministrationService struct {
	// store 承担版本写入、审计和幂等事务。
	store BotStrategyAdministrationStore
	// now 提供所有版本事实使用的唯一 UTC 时钟。
	now func() time.Time
}

// NewBotStrategyAdministrationService 创建显式 Bot 资料管理应用服务。
func NewBotStrategyAdministrationService(
	store BotStrategyAdministrationStore,
	now func() time.Time,
) *BotStrategyAdministrationService {
	if now == nil {
		now = time.Now
	}
	return &BotStrategyAdministrationService{store: store, now: now}
}

// Create 创建新的 Bot Code 的第一个启用版本。
func (service *BotStrategyAdministrationService) Create(
	ctx context.Context,
	command CreateBotStrategyCommand,
) (ManagedBotStrategy, error) {
	if service == nil || service.store == nil {
		return ManagedBotStrategy{}, ErrBotStrategyStoreUnavailable
	}
	canonical, err := normalizeBotManagementCommand(&command.GameDataWriteContext, command.Code, command.Definition)
	if err != nil {
		return ManagedBotStrategy{}, err
	}
	command.Code = strings.TrimSpace(command.Code)
	return service.store.CreateBotStrategy(ctx, command, canonical, service.now().UTC())
}

// PublishNext 停用当前启用版本并创建同一 Code 的下一个启用版本。
func (service *BotStrategyAdministrationService) PublishNext(
	ctx context.Context,
	command PublishNextBotStrategyCommand,
) (ManagedBotStrategy, error) {
	if service == nil || service.store == nil {
		return ManagedBotStrategy{}, ErrBotStrategyStoreUnavailable
	}
	canonical, err := normalizeBotManagementCommand(&command.GameDataWriteContext, command.Code, command.Definition)
	if err != nil {
		return ManagedBotStrategy{}, err
	}
	command.Code = strings.TrimSpace(command.Code)
	return service.store.PublishNextBotStrategy(ctx, command, canonical, service.now().UTC())
}

// Disable 停用指定版本；已创建 Battle 始终使用其 Participant 中冻结的定义，不受影响。
func (service *BotStrategyAdministrationService) Disable(
	ctx context.Context,
	command DisableBotStrategyCommand,
) error {
	if service == nil || service.store == nil {
		return ErrBotStrategyStoreUnavailable
	}
	command.GameDataWriteContext = command.Normalize()
	command.Code = strings.TrimSpace(command.Code)
	if !command.Valid() || !stablecode.Valid(command.Code) || command.Version == 0 {
		return ErrBotDefinitionInvalid
	}
	return service.store.DisableBotStrategy(ctx, command, service.now().UTC())
}

// Get 返回一条不可变 Bot 版本资料。
func (service *BotStrategyAdministrationService) Get(
	ctx context.Context,
	code string,
	version uint32,
) (ManagedBotStrategy, error) {
	if service == nil || service.store == nil || !stablecode.Valid(strings.TrimSpace(code)) || version == 0 {
		return ManagedBotStrategy{}, ErrBotDefinitionInvalid
	}
	return service.store.GetBotStrategy(ctx, strings.TrimSpace(code), version)
}

// List 返回管理端可用的版本化 Bot 资料统一页码查询结果。
func (service *BotStrategyAdministrationService) List(
	ctx context.Context,
	query BotStrategyListQuery,
) (BotStrategyPage, error) {
	if service == nil || service.store == nil {
		return BotStrategyPage{}, ErrBotStrategyStoreUnavailable
	}
	if query.Page == 0 {
		query.Page = 1
	}
	if query.PageSize == 0 {
		query.PageSize = 20
	}
	query.Code = strings.TrimSpace(query.Code)
	if query.Page < 1 || query.Page > 1_000_000 || query.PageSize < 1 || query.PageSize > 100 ||
		(query.Code != "" && !stablecode.Valid(query.Code)) {
		return BotStrategyPage{}, ErrBotDefinitionInvalid
	}
	return service.store.ListBotStrategies(ctx, query)
}

func normalizeBotManagementCommand(
	context *administration.GameDataWriteContext,
	code string,
	definition json.RawMessage,
) (json.RawMessage, error) {
	*context = context.Normalize()
	if !context.Valid() || !stablecode.Valid(strings.TrimSpace(code)) {
		return nil, ErrBotDefinitionInvalid
	}
	_, canonical, err := DecodeBotStrategyDefinition(definition)
	if err != nil {
		return nil, fmt.Errorf("%w: %v", ErrBotDefinitionInvalid, err)
	}
	return canonical, nil
}
