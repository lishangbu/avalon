// Package persistence 提供 Team 的 PostgreSQL 持久化适配器。
package persistence

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/jackc/pgx/v5/pgconn"
	"github.com/lishangbu/avalon/ent/playercharacterteam"
	platformaudit "github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/team"
)

const createOperationID = "team.create"

// repository 使用 PlayerCharacter 行锁串行化每角色 Team 上限、名称与首次激活写入。
type repository struct {
	// pool 提供 Team 事务、Ent Client 与审计事务所需的 PostgreSQL 连接池。
	pool *database.Pool
	// newID 为 Team 审计事实生成稳定 Identifier。
	newID snowflake.Source
}

// NewRepository 创建 Team PostgreSQL 持久化适配器。
func NewRepository(pool *database.Pool, newID snowflake.Source) *repository {
	return &repository{pool: pool, newID: newID}
}

// Create 在一个事务内先锁定未归档角色、认领幂等键、校验当前资料并保存完整阵容。
//
// 已提交的同键请求必须在确认角色所有权后、访问 Team 或 Current Game Data 前直接返回原始响应；这样首次
// 创建后即使资料状态发生变化，客户端安全重试仍具备确定性，同时不存在的账号不会先触发幂等表外键错误。
func (s *repository) Create(ctx context.Context, record team.CreateRecord) (team.Team, error) {
	if !record.HasCurrentGameDataValidator() {
		return team.Team{}, team.ErrTeamCatalogUnavailable
	}
	var created team.Team
	err := s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := s.pool.Client(transactionCtx)
		if err := lockOwnedPlayerCharacter(ctx, client, record.ActorAccountID, record.Team.PlayerCharacterID); err != nil {
			return err
		}
		request, err := createIdempotencyRequest(record)
		if err != nil {
			return err
		}
		replayed, err := claimResponse(ctx, idempotency.NewEntRecords(client, s.newID), request, &created)
		if err != nil || replayed {
			return err
		}
		if err := record.ValidateCurrentMembers(ctx); err != nil {
			return err
		}

		count, err := client.PlayerCharacterTeam.Query().Where(playercharacterteam.PlayerCharacterIDEQ(record.Team.PlayerCharacterID)).Count(ctx)
		if err != nil {
			return fmt.Errorf("统计 PlayerCharacter Team: %w", err)
		}
		if int64(count) >= team.MaximumPerPlayerCharacter {
			return team.ErrTeamLimitExceeded
		}
		created = record.Team
		created.Active = count == 0
		if err := insertTeamEnt(transactionCtx, client, s.newID, created); err != nil {
			if isUniqueViolation(err) {
				return team.ErrTeamConflict
			}
			return err
		}
		if err := s.recordAudit(
			ctx, database.Executor(transactionCtx, s.pool), record.ActorAccountID, "team.created", "team", created.ID,
			created, record.RequestID, created.CreatedAt,
		); err != nil {
			return err
		}
		if err := completeResponse(ctx, idempotency.NewEntRecords(client, s.newID), request, created); err != nil {
			return fmt.Errorf("保存 Team 创建幂等结果: %w", err)
		}
		return nil
	})
	return created, err
}

func createIdempotencyRequest(record team.CreateRecord) (idempotency.Request, error) {
	// 摘要保留规范化后的展示名称；NameKey 只服务于名称唯一性，不能抹去客户端可见的大小写差异。
	digest, err := idempotency.Digest(struct {
		PlayerCharacterID snowflake.ID
		Name              string
		Members           []team.Member
	}{record.Team.PlayerCharacterID, record.Team.Name, record.Team.Members})
	if err != nil {
		return idempotency.Request{}, fmt.Errorf("计算 Team 创建幂等摘要: %w", err)
	}
	return idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: createOperationID, Key: record.IdempotencyKey,
		RequestDigest: digest, CreatedAt: record.Team.CreatedAt,
	}, nil
}

func (s *repository) recordAudit(
	ctx context.Context,
	executor database.Transaction,
	accountID snowflake.ID,
	action string,
	objectType string,
	objectID snowflake.ID,
	value any,
	requestID string,
	createdAt time.Time,
) error {
	auditID, err := s.newID.Next(ctx)
	if err != nil {
		return fmt.Errorf("生成 Team 审计标识: %w", err)
	}
	changes, err := json.Marshal(value)
	if err != nil {
		return fmt.Errorf("编码 Team 审计变化: %w", err)
	}
	if err := platformaudit.Append(ctx, executor, platformaudit.AdministrationLedger, platformaudit.Entry{
		ID: auditID, ActorAccountID: &accountID, ActorKind: "account",
		ActionCode: action, ObjectType: objectType, ObjectID: stringPointer(objectID.String()), RequestID: requestID,
		Changes: changes, CreatedAt: createdAt.UTC(),
	}); err != nil {
		return fmt.Errorf("记录 Team 审计: %w", err)
	}
	return nil
}

// stringPointer 返回审计条目使用的稳定可选字符串。
func stringPointer(value string) *string { return &value }

func isUniqueViolation(err error) bool {
	var databaseError *pgconn.PgError
	return errors.As(err, &databaseError) && databaseError.Code == "23505"
}
