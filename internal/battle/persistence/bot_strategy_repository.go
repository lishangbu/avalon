package persistence

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	entsql "entgo.io/ent/dialect/sql"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/battlebotstrategy"
	"github.com/lishangbu/avalon/ent/predicate"
	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createBotStrategyOperationID      = "battle.bot-strategy.create"
	publishNextBotStrategyOperationID = "battle.bot-strategy.publish-next"
	disableBotStrategyOperationID     = "battle.bot-strategy.disable"
)

// GetBotStrategy 返回一个不可变 Bot 策略版本，不要求调用方持有维护窗口。
func (adapter *Adapters) GetBotStrategy(
	ctx context.Context,
	code string,
	version uint32,
) (battle.ManagedBotStrategy, error) {
	if adapter == nil || adapter.pool == nil || version == 0 {
		return battle.ManagedBotStrategy{}, battle.ErrBotStrategyNotFound
	}
	row, err := adapter.pool.Client(ctx).BattleBotStrategy.Query().Where(battlebotstrategy.CodeEQ(code), battlebotstrategy.VersionEQ(int32(version))).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battle.ManagedBotStrategy{}, battle.ErrBotStrategyNotFound
	}
	if err != nil {
		return battle.ManagedBotStrategy{}, fmt.Errorf("读取 Bot 策略版本: %w", err)
	}
	return managedBotStrategyFromEnt(row), nil
}

// ListBotStrategies 按稳定 Code 和版本顺序返回 Bot 策略管理页。
func (adapter *Adapters) ListBotStrategies(
	ctx context.Context,
	query battle.BotStrategyListQuery,
) (battle.BotStrategyPage, error) {
	if adapter == nil || adapter.pool == nil {
		return battle.BotStrategyPage{}, battle.ErrBotStrategyNotFound
	}
	client := adapter.pool.Client(ctx)
	filters := make([]predicate.BattleBotStrategy, 0, 2)
	if query.Code != "" {
		filters = append(filters, battlebotstrategy.CodeEQ(query.Code))
	}
	if query.Enabled != nil {
		filters = append(filters, battlebotstrategy.EnabledEQ(*query.Enabled))
	}
	total, err := client.BattleBotStrategy.Query().Where(filters...).Count(ctx)
	if err != nil {
		return battle.BotStrategyPage{}, fmt.Errorf("统计 Bot 策略版本: %w", err)
	}
	rows, err := client.BattleBotStrategy.Query().Where(filters...).Order(battlebotstrategy.ByCode(), battlebotstrategy.ByVersion()).Offset(int(query.Page-1) * int(query.PageSize)).Limit(int(query.PageSize)).All(ctx)
	if err != nil {
		return battle.BotStrategyPage{}, fmt.Errorf("读取 Bot 策略版本页: %w", err)
	}
	items := make([]battle.ManagedBotStrategy, len(rows))
	for index := range rows {
		items[index] = managedBotStrategyFromEnt(rows[index])
	}
	return battle.BotStrategyPage{Items: items, Page: query.Page, PageSize: query.PageSize, Total: int64(total)}, nil
}

// ListEnabledBotStrategyDefinitions 有界读取所有启用 Bot 的冻结定义，供退出维护窗口前的只读校验使用。
//
// 返回总数使调用方能使用稳定的页码循环，而不需要一次把全部资料加载到内存。
func (adapter *Adapters) ListEnabledBotStrategyDefinitions(
	ctx context.Context,
	page int32,
	pageSize int32,
) ([]battle.BotStrategyRecord, int64, error) {
	if adapter == nil || adapter.pool == nil || page < 1 || pageSize < 1 || pageSize > 100 {
		return nil, 0, battle.ErrBotDefinitionInvalid
	}
	client := adapter.pool.Client(ctx)
	total, err := client.BattleBotStrategy.Query().Where(battlebotstrategy.EnabledEQ(true)).Count(ctx)
	if err != nil {
		return nil, 0, fmt.Errorf("统计启用 Bot 策略: %w", err)
	}
	rows, err := client.BattleBotStrategy.Query().Where(battlebotstrategy.EnabledEQ(true)).Order(battlebotstrategy.ByCode(), battlebotstrategy.ByVersion()).Offset(int(page-1) * int(pageSize)).Limit(int(pageSize)).All(ctx)
	if err != nil {
		return nil, 0, fmt.Errorf("读取启用 Bot 策略: %w", err)
	}
	result := make([]battle.BotStrategyRecord, len(rows))
	for index := range rows {
		result[index] = battle.BotStrategyRecord{
			Code: rows[index].Code, Version: uint32(rows[index].Version),
			Definition: append(json.RawMessage(nil), rows[index].Definition...),
		}
	}
	return result, int64(total), nil
}

// CreateBotStrategy 创建此前不存在稳定 Code 的第一个启用 Bot 策略版本。
func (adapter *Adapters) CreateBotStrategy(
	ctx context.Context,
	command battle.CreateBotStrategyCommand,
	definition json.RawMessage,
	createdAt time.Time,
) (battle.ManagedBotStrategy, error) {
	request, err := botStrategyIdempotencyRequest(command.GameDataWriteContext, createBotStrategyOperationID, struct {
		Code       string          `json:"code"`
		Definition json.RawMessage `json:"definition"`
	}{command.Code, definition}, createdAt)
	if err != nil {
		return battle.ManagedBotStrategy{}, err
	}
	var created battle.ManagedBotStrategy
	err = adapter.withBotStrategyAdministration(ctx, request, &created, func(client *avalonent.Client, executor database.Transaction) error {
		id, idErr := adapter.newID.Next(ctx)
		if idErr != nil {
			return fmt.Errorf("生成 Bot 策略 Identifier: %w", idErr)
		}
		row, createErr := client.BattleBotStrategy.Create().SetID(id).SetCode(command.Code).SetVersion(1).SetEnabled(true).SetDefinition(definition).SetCreatedAt(createdAt.UTC()).Save(ctx)
		if createErr != nil {
			if isUniqueViolation(createErr) {
				return battle.ErrBotStrategyCodeConflict
			}
			return fmt.Errorf("创建 Bot 策略版本: %w", createErr)
		}
		created = managedBotStrategyFromEnt(row)
		return adapter.recordBotStrategyAudit(ctx, executor, command.ActorAccountID, "battle.bot-strategy.created", created,
			command.RequestID, createdAt, nil, created)
	})
	if err != nil {
		return battle.ManagedBotStrategy{}, err
	}
	return created, nil
}

// PublishNextBotStrategy 将同一 Code 的新不可变版本设为唯一可用于新 Training Battle 的版本。
func (adapter *Adapters) PublishNextBotStrategy(
	ctx context.Context,
	command battle.PublishNextBotStrategyCommand,
	definition json.RawMessage,
	createdAt time.Time,
) (battle.ManagedBotStrategy, error) {
	request, err := botStrategyIdempotencyRequest(command.GameDataWriteContext, publishNextBotStrategyOperationID, struct {
		Code       string          `json:"code"`
		Definition json.RawMessage `json:"definition"`
	}{command.Code, definition}, createdAt)
	if err != nil {
		return battle.ManagedBotStrategy{}, err
	}
	var published battle.ManagedBotStrategy
	err = adapter.withBotStrategyAdministration(ctx, request, &published, func(client *avalonent.Client, executor database.Transaction) error {
		currentRow, lockErr := client.BattleBotStrategy.Query().Where(battlebotstrategy.CodeEQ(command.Code)).Order(battlebotstrategy.ByVersion(entsql.OrderDesc())).First(ctx)
		if avalonent.IsNotFound(lockErr) {
			return battle.ErrBotStrategyNotFound
		}
		if lockErr != nil {
			return fmt.Errorf("锁定 Bot 策略最新版本: %w", lockErr)
		}
		current := managedBotStrategyFromEnt(currentRow)
		if current.Version >= uint32(1<<31-1) {
			return battle.ErrBotStrategyVersionConflict
		}
		if current.Enabled {
			rows, disableErr := client.BattleBotStrategy.Update().Where(battlebotstrategy.CodeEQ(current.Code), battlebotstrategy.VersionEQ(int32(current.Version)), battlebotstrategy.EnabledEQ(true)).SetEnabled(false).Save(ctx)
			if disableErr != nil {
				return fmt.Errorf("停用当前 Bot 策略版本: %w", disableErr)
			}
			if rows != 1 {
				return battle.ErrBotStrategyVersionConflict
			}
		}
		id, idErr := adapter.newID.Next(ctx)
		if idErr != nil {
			return fmt.Errorf("生成 Bot 策略版本 Identifier: %w", idErr)
		}
		row, createErr := client.BattleBotStrategy.Create().SetID(id).SetCode(command.Code).SetVersion(int32(current.Version + 1)).SetEnabled(true).SetDefinition(definition).SetCreatedAt(createdAt.UTC()).Save(ctx)
		if createErr != nil {
			if isUniqueViolation(createErr) {
				return battle.ErrBotStrategyVersionConflict
			}
			return fmt.Errorf("发布 Bot 策略新版本: %w", createErr)
		}
		published = managedBotStrategyFromEnt(row)
		return adapter.recordBotStrategyAudit(ctx, executor, command.ActorAccountID, "battle.bot-strategy.published", published,
			command.RequestID, createdAt, current, published)
	})
	if err != nil {
		return battle.ManagedBotStrategy{}, err
	}
	return published, nil
}

// DisableBotStrategy 停用指定版本，不删除冻结给历史 Battle 的定义。
func (adapter *Adapters) DisableBotStrategy(
	ctx context.Context,
	command battle.DisableBotStrategyCommand,
	disabledAt time.Time,
) error {
	request, err := botStrategyIdempotencyRequest(command.GameDataWriteContext, disableBotStrategyOperationID, struct {
		Code    string `json:"code"`
		Version uint32 `json:"version"`
	}{command.Code, command.Version}, disabledAt)
	if err != nil {
		return err
	}
	response := struct {
		// Disabled 是幂等重放时返回的停用成功事实。
		Disabled bool `json:"disabled"`
	}{Disabled: true}
	return adapter.withBotStrategyAdministration(ctx, request, &response, func(client *avalonent.Client, executor database.Transaction) error {
		row, lockErr := client.BattleBotStrategy.Query().Where(battlebotstrategy.CodeEQ(command.Code), battlebotstrategy.VersionEQ(int32(command.Version))).Only(ctx)
		if avalonent.IsNotFound(lockErr) {
			return battle.ErrBotStrategyNotFound
		}
		if lockErr != nil {
			return fmt.Errorf("锁定待停用 Bot 策略版本: %w", lockErr)
		}
		current := managedBotStrategyFromEnt(row)
		if !current.Enabled {
			return battle.ErrBotStrategyVersionConflict
		}
		rows, disableErr := client.BattleBotStrategy.UpdateOne(row).Where(battlebotstrategy.EnabledEQ(true)).SetEnabled(false).Save(ctx)
		if disableErr != nil {
			return fmt.Errorf("停用 Bot 策略版本: %w", disableErr)
		}
		if rows == nil {
			return battle.ErrBotStrategyVersionConflict
		}
		return adapter.recordBotStrategyAudit(ctx, executor, command.ActorAccountID, "battle.bot-strategy.disabled", current,
			command.RequestID, disabledAt, current, nil)
	})
}

// withBotStrategyAdministration 在单个数据库事务中执行 Bot 写入，并原子保存审计与幂等结果。
func (adapter *Adapters) withBotStrategyAdministration(
	ctx context.Context,
	request idempotency.Request,
	response any,
	work func(*avalonent.Client, database.Transaction) error,
) error {
	if adapter == nil || adapter.pool == nil || adapter.newID == nil || work == nil {
		return battle.ErrBotStrategyRepositoryUnavailable
	}
	return adapter.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := adapter.pool.Client(transactionCtx)
		executor := database.Executor(transactionCtx, adapter.pool)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, adapter.newID))
		claim, err := writer.ClaimIdempotency(transactionCtx, request)
		if err != nil {
			return fmt.Errorf("认领 Bot 策略幂等键: %w", err)
		}
		if claim.Replay {
			if err := json.Unmarshal(claim.Response, response); err != nil {
				return fmt.Errorf("解码 Bot 策略幂等响应: %w", err)
			}
			return nil
		}
		if err := work(client, executor); err != nil {
			return err
		}
		encodedResponse, err := json.Marshal(response)
		if err != nil {
			return fmt.Errorf("编码 Bot 策略幂等响应: %w", err)
		}
		if err := writer.CompleteIdempotency(transactionCtx, request, encodedResponse); err != nil {
			return fmt.Errorf("保存 Bot 策略幂等响应: %w", err)
		}
		return nil
	})
}

// recordBotStrategyAudit 将不可变版本的前后事实写入统一管理员审计日志。
func (adapter *Adapters) recordBotStrategyAudit(
	ctx context.Context,
	executor database.Transaction,
	actorID snowflake.ID,
	actionCode string,
	strategy battle.ManagedBotStrategy,
	requestID string,
	occurredAt time.Time,
	before any,
	after any,
) error {
	changes, err := json.Marshal(struct {
		Before any `json:"before,omitempty"`
		After  any `json:"after,omitempty"`
	}{Before: before, After: after})
	if err != nil {
		return fmt.Errorf("编码 Bot 策略审计摘要: %w", err)
	}
	auditIdentifier, err := adapter.newID.Next(ctx)
	if err != nil {
		return fmt.Errorf("生成 Bot 策略审计标识: %w", err)
	}
	objectID := fmt.Sprintf("%s:%d", strategy.Code, strategy.Version)
	return audit.Append(ctx, executor, audit.AdminLedger, audit.Entry{ID: auditIdentifier, ActorAccountID: &actorID, ActorKind: "admin", ActionCode: actionCode, ObjectType: "battle_bot_strategy", ObjectID: &objectID, RequestID: requestID, Changes: changes, CreatedAt: occurredAt})
}

// botStrategyIdempotencyRequest 创建一条仅与规范化命令事实绑定的持久幂等请求。
func botStrategyIdempotencyRequest(
	context administration.GameDataWriteContext,
	operationID string,
	payload any,
	occurredAt time.Time,
) (idempotency.Request, error) {
	digest, err := idempotency.Digest(payload)
	if err != nil {
		return idempotency.Request{}, fmt.Errorf("计算 Bot 策略幂等摘要: %w", err)
	}
	return idempotency.Request{
		ActorAccountID: context.ActorAccountID, OperationID: operationID, Key: context.IdempotencyKey,
		RequestDigest: digest, CreatedAt: occurredAt.UTC(),
	}, nil
}

// managedBotStrategyFromEnt 将 Ent Bot 策略实体转换为领域只读模型。
func managedBotStrategyFromEnt(row *avalonent.BattleBotStrategy) battle.ManagedBotStrategy {
	return battle.ManagedBotStrategy{Code: row.Code, Version: uint32(row.Version), Enabled: row.Enabled, Definition: append(json.RawMessage(nil), row.Definition...), CreatedAt: row.CreatedAt.UTC()}
}
