package persistence

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/activeplayercharacterteam"
	"github.com/lishangbu/avalon/ent/playercharacter"
	"github.com/lishangbu/avalon/ent/playercharacterteam"
	"github.com/lishangbu/avalon/ent/playercharacterteamshare"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/team"
)

const (
	createShareOperationID = "team.share.create"
	revokeShareOperationID = "team.share.revoke"
	importShareOperationID = "team.share.import"
)

// CreateShare 在幂等认领后锁定来源版本并冻结完整 Team 快照。
func (s *adapters) CreateShare(ctx context.Context, record team.CreateShareRecord) (team.CreateShareResult, error) {
	var result team.CreateShareResult
	err := s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		executor := database.Executor(transactionCtx, s.pool)
		client := s.pool.Client(transactionCtx)
		if err := lockOwnedPlayerCharacter(transactionCtx, client, record.AccountID, record.PlayerCharacterID); err != nil {
			return err
		}
		digest, err := idempotency.Digest(struct {
			TeamID          snowflake.ID
			ExpectedVersion int64
			ExpiryDigest    string
		}{record.TeamID, record.ExpectedVersion, record.ExpiryDigest})
		if err != nil {
			return fmt.Errorf("计算 Team 分享创建幂等摘要: %w", err)
		}
		request := idempotency.Request{
			ActorAccountID: record.AccountID, OperationID: createShareOperationID,
			Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt,
		}
		replayed, err := claimResponse(transactionCtx, idempotency.NewEntRecords(client, s.newID), request, &result)
		if err != nil || replayed {
			return err
		}
		row, err := client.PlayerCharacterTeam.Query().Where(playercharacterteam.IDEQ(record.TeamID), playercharacterteam.PlayerCharacterID(record.PlayerCharacterID)).Only(transactionCtx)
		if avalonent.IsNotFound(err) || (err == nil && row.Version != record.ExpectedVersion) {
			return team.ErrTeamShareConflict
		}
		if err != nil {
			return fmt.Errorf("查询 Team 分享来源: %w", err)
		}
		source := team.Team{ID: row.ID, PlayerCharacterID: row.PlayerCharacterID, Name: row.Name, NameKey: row.NameKey, Version: row.Version, CreatedAt: row.CreatedAt.UTC(), UpdatedAt: row.UpdatedAt.UTC()}
		source.Active, _ = client.ActivePlayerCharacterTeam.Query().Where(activeplayercharacterteam.IDEQ(row.PlayerCharacterID), activeplayercharacterteam.TeamIDEQ(row.ID)).Exist(transactionCtx)
		if err := loadRosterEnt(transactionCtx, client, &source); err != nil {
			return err
		}
		snapshot := team.ShareSnapshot{
			SchemaVersion: team.TeamShareSchemaVersion, Name: source.Name, Members: source.Members,
		}
		snapshotJSON, err := json.Marshal(snapshot)
		if err != nil {
			return fmt.Errorf("编码 Team 分享快照: %w", err)
		}
		codeDigest, valid := team.ShareCodeDigest(record.Code)
		if !valid {
			return team.ErrInvalidTeam
		}
		if _, err := client.PlayerCharacterTeamShare.Create().SetID(record.ShareID).SetSourceTeamID(record.TeamID).SetOwnerPlayerCharacterID(record.PlayerCharacterID).SetSourceTeamVersion(source.Version).SetCodeDigest(codeDigest).SetSchemaVersion(team.TeamShareSchemaVersion).SetSnapshot(snapshotJSON).SetVersion(1).SetExpiresAt(record.ExpiresAt.UTC()).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC()).Save(transactionCtx); err != nil {
			if isUniqueViolation(err) {
				return team.ErrTeamShareCodeCollision
			}
			return fmt.Errorf("创建 Team 分享: %w", err)
		}
		result = team.CreateShareResult{Share: team.Share{
			ID: record.ShareID, SourceTeamID: source.ID, OwnerPlayerCharacterID: source.PlayerCharacterID,
			SourceTeamVersion: source.Version, SchemaVersion: team.TeamShareSchemaVersion, Version: 1,
			ExpiresAt: record.ExpiresAt, CreatedAt: record.CreatedAt, UpdatedAt: record.CreatedAt,
		}, Code: record.Code}
		if err := s.recordAudit(transactionCtx, executor, record.AccountID, "team.share-created", "team_share", record.ShareID,
			result.Share, record.RequestID, record.CreatedAt); err != nil {
			return err
		}
		// 分享码明文只能离开本次创建响应，不能写入 PostgreSQL 的幂等响应、审计或日志。
		// 同键重放仍返回同一分享元数据，但 Code 保持为空，避免把秘密从摘要边界重新带回 Repository。
		if err := completeResponse(transactionCtx, idempotency.NewEntRecords(client, s.newID), request, team.CreateShareResult{Share: result.Share}); err != nil {
			return fmt.Errorf("保存 Team 分享创建幂等结果: %w", err)
		}
		return nil
	})
	return result, err
}

// ResolveShare 按摘要读取仍有效且 Schema 受支持的冻结快照。
func (s *adapters) ResolveShare(ctx context.Context, codeDigest []byte, now time.Time) (team.ShareSnapshot, error) {
	row, err := s.pool.Client(ctx).PlayerCharacterTeamShare.Query().Where(
		playercharacterteamshare.CodeDigestEQ(codeDigest), playercharacterteamshare.ExpiresAtGT(now.UTC()), playercharacterteamshare.RevokedAtIsNil(),
	).Only(ctx)
	if avalonent.IsNotFound(err) {
		return team.ShareSnapshot{}, team.ErrTeamShareNotFound
	}
	if err != nil {
		return team.ShareSnapshot{}, fmt.Errorf("查询 Team 分享: %w", err)
	}
	return snapshotFromEntShare(row)
}

// snapshotFromEntShare 将 Ent 分享实体解码为领域冻结快照。
func snapshotFromEntShare(row *avalonent.PlayerCharacterTeamShare) (team.ShareSnapshot, error) {
	var snapshot team.ShareSnapshot
	if err := json.Unmarshal(row.Snapshot, &snapshot); err != nil {
		return team.ShareSnapshot{}, fmt.Errorf("解码 Team 分享快照: %w", err)
	}
	if row.SchemaVersion != team.TeamShareSchemaVersion || snapshot.SchemaVersion != team.TeamShareSchemaVersion {
		return team.ShareSnapshot{}, team.ErrTeamShareNotFound
	}
	return snapshot, nil
}

// RevokeShare 原子撤销调用角色拥有的精确分享版本。
func (s *adapters) RevokeShare(ctx context.Context, record team.RevokeShareRecord) (team.Share, error) {
	var revoked team.Share
	err := s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		executor := database.Executor(transactionCtx, s.pool)
		client := s.pool.Client(transactionCtx)
		if err := lockOwnedPlayerCharacter(transactionCtx, client, record.AccountID, record.PlayerCharacterID); err != nil {
			return err
		}
		digest, err := idempotency.Digest(struct {
			ShareID         snowflake.ID
			ExpectedVersion int64
		}{record.ShareID, record.ExpectedVersion})
		if err != nil {
			return fmt.Errorf("计算 Team 分享撤销幂等摘要: %w", err)
		}
		request := idempotency.Request{
			ActorAccountID: record.AccountID, OperationID: revokeShareOperationID,
			Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.RevokedAt,
		}
		replayed, err := claimResponse(transactionCtx, idempotency.NewEntRecords(client, s.newID), request, &revoked)
		if err != nil || replayed {
			return err
		}
		row, err := client.PlayerCharacterTeamShare.Query().Where(playercharacterteamshare.IDEQ(record.ShareID), playercharacterteamshare.OwnerPlayerCharacterIDEQ(record.PlayerCharacterID)).Only(transactionCtx)
		if avalonent.IsNotFound(err) {
			return team.ErrTeamShareNotFound
		} else if err != nil {
			return fmt.Errorf("锁定 Team 分享: %w", err)
		}
		updatedRow, err := client.PlayerCharacterTeamShare.UpdateOne(row).Where(playercharacterteamshare.VersionEQ(record.ExpectedVersion), playercharacterteamshare.RevokedAtIsNil()).SetRevokedAt(record.RevokedAt.UTC()).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.RevokedAt.UTC()).Save(transactionCtx)
		if avalonent.IsNotFound(err) {
			return team.ErrTeamShareConflict
		}
		if err != nil {
			return fmt.Errorf("撤销 Team 分享: %w", err)
		}
		revoked = shareFromEnt(updatedRow)
		if err := s.recordAudit(transactionCtx, executor, record.AccountID, "team.share-revoked", "team_share", record.ShareID,
			revoked, record.RequestID, record.RevokedAt); err != nil {
			return err
		}
		if err := completeResponse(transactionCtx, idempotency.NewEntRecords(client, s.newID), request, revoked); err != nil {
			return fmt.Errorf("保存 Team 分享撤销幂等结果: %w", err)
		}
		return nil
	})
	return revoked, err
}

// ImportShare 在目标角色与分享行锁内解析有效快照，并创建不持续同步的独立 Team。
//
// 首次导入锁定分享记录直至独立 Team 提交，因而并发撤销必须先后串行：已经提交的撤销会使导入读取不到
// 分享，而先完成的导入仍可由其幂等记录在分享随后撤销或过期后确定性重放。
func (s *adapters) ImportShare(ctx context.Context, record team.ImportShareRecord) (team.Team, error) {
	if !record.HasCurrentGameDataValidator() {
		return team.Team{}, team.ErrTeamCatalogUnavailable
	}
	var imported team.Team
	err := s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		executor := database.Executor(transactionCtx, s.pool)
		client := s.pool.Client(transactionCtx)
		if err := lockOwnedPlayerCharacter(transactionCtx, client, record.AccountID, record.Team.PlayerCharacterID); err != nil {
			return err
		}
		// 摘要保留规范化后的展示名称；NameKey 只服务于名称唯一性，不能抹去客户端可见的大小写差异。
		digest, err := idempotency.Digest(struct {
			PlayerCharacterID snowflake.ID
			CodeDigest        []byte
			Name              string
		}{record.Team.PlayerCharacterID, record.CodeDigest, record.Team.Name})
		if err != nil {
			return fmt.Errorf("计算 Team 分享导入幂等摘要: %w", err)
		}
		request := idempotency.Request{
			ActorAccountID: record.AccountID, OperationID: importShareOperationID,
			Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.ImportedAt,
		}
		replayed, err := claimResponse(transactionCtx, idempotency.NewEntRecords(client, s.newID), request, &imported)
		if err != nil || replayed {
			return err
		}
		// ResolveShare 保持无锁读取；只有会写入独立 Team 的首次导入需要锁住分享，防止撤销提交后仍使用旧快照。
		shareRow, err := client.PlayerCharacterTeamShare.Query().Where(playercharacterteamshare.CodeDigestEQ(record.CodeDigest), playercharacterteamshare.ExpiresAtGT(record.ImportedAt.UTC()), playercharacterteamshare.RevokedAtIsNil()).ForUpdate().Only(transactionCtx)
		if avalonent.IsNotFound(err) {
			return team.ErrTeamShareNotFound
		}
		if err != nil {
			return fmt.Errorf("查询待导入 Team 分享: %w", err)
		}
		snapshot, err := snapshotFromEntShare(shareRow)
		if err != nil {
			return err
		}
		// 仅在幂等认领确认本次是首次导入后校验，既阻止已失效资料写入，
		// 也保证已成功响应即使分享随后撤销或过期仍能确定性重放。
		if err := record.ValidateCurrentSnapshot(transactionCtx, snapshot.Members); err != nil {
			return err
		}
		count, err := client.PlayerCharacterTeam.Query().Where(playercharacterteam.HasPlayerCharacterWith(playercharacter.IDEQ(record.Team.PlayerCharacterID))).Count(transactionCtx)
		if err != nil {
			return fmt.Errorf("统计 Team 分享导入目标容量: %w", err)
		}
		if int64(count) >= team.MaximumPerPlayerCharacter {
			return team.ErrTeamLimitExceeded
		}
		imported = record.Team
		imported.Members = snapshot.Members
		imported.Active = count == 0
		if err := insertTeamEnt(transactionCtx, client, s.newID, imported); err != nil {
			if isUniqueViolation(err) {
				return team.ErrTeamConflict
			}
			return err
		}
		if err := s.recordAudit(transactionCtx, executor, record.AccountID, "team.share-imported", "team", imported.ID,
			imported, record.RequestID, record.ImportedAt); err != nil {
			return err
		}
		if err := completeResponse(transactionCtx, idempotency.NewEntRecords(client, s.newID), request, imported); err != nil {
			return fmt.Errorf("保存 Team 分享导入幂等结果: %w", err)
		}
		return nil
	})
	return imported, err
}

// shareFromEnt 将 Ent 分享实体转换为领域分享元数据，保留撤销和版本信息。
func shareFromEnt(row *avalonent.PlayerCharacterTeamShare) team.Share {
	value := team.Share{
		ID: row.ID, SourceTeamID: row.SourceTeamID, OwnerPlayerCharacterID: row.OwnerPlayerCharacterID,
		SourceTeamVersion: row.SourceTeamVersion, SchemaVersion: int(row.SchemaVersion), Version: row.Version,
		ExpiresAt: row.ExpiresAt.UTC(), CreatedAt: row.CreatedAt.UTC(), UpdatedAt: row.UpdatedAt.UTC(),
	}
	if row.RevokedAt != nil {
		revokedAt := row.RevokedAt.UTC()
		value.RevokedAt = &revokedAt
	}
	return value
}
