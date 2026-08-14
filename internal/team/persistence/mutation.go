package persistence

import (
	"context"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/activeplayercharacterteam"
	"github.com/lishangbu/avalon/ent/playercharacter"
	"github.com/lishangbu/avalon/ent/playercharacterteam"
	"github.com/lishangbu/avalon/ent/playercharacterteammember"
	"github.com/lishangbu/avalon/ent/playercharacterteammemberskill"
	"github.com/lishangbu/avalon/ent/playercharacterteammemberstat"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/team"
)

const (
	updateOperationID       = "team.update"
	deleteOperationID       = "team.delete"
	switchActiveOperationID = "team.switch-active"
)

// insertTeamEnt 使用 Ent 在当前事务中创建 Team、活动绑定和完整阵容。
func insertTeamEnt(ctx context.Context, client *avalonent.Client, identifiers snowflake.Source, value team.Team) error {
	if _, err := client.PlayerCharacterTeam.Create().SetID(value.ID).
		SetPlayerCharacterID(value.PlayerCharacterID).SetName(value.Name).SetNameKey(value.NameKey).
		SetVersion(value.Version).SetCreatedAt(value.CreatedAt.UTC()).SetUpdatedAt(value.UpdatedAt.UTC()).Save(ctx); err != nil {
		return fmt.Errorf("创建 Team: %w", err)
	}
	if value.Active {
		if _, err := client.ActivePlayerCharacterTeam.Create().SetID(value.PlayerCharacterID).
			SetTeamID(value.ID).SetVersion(1).SetUpdatedAt(value.CreatedAt.UTC()).Save(ctx); err != nil {
			return fmt.Errorf("激活首支 Team: %w", err)
		}
	}
	for _, member := range value.Members {
		memberID, err := identifiers.Next(ctx)
		if err != nil {
			return fmt.Errorf("生成 Team 成员 Identifier: %w", err)
		}
		builder := client.PlayerCharacterTeamMember.Create().SetID(memberID).SetTeamID(value.ID).SetPosition(int16(member.Position)).
			SetCreatureID(member.CreatureID).SetNillableFormID(optionalEntTypeIdentifier(member.FormID)).
			SetNillableGenderID(optionalEntTypeIdentifier(member.GenderID)).SetNillableSkinID(optionalEntTypeIdentifier(member.SkinID)).
			SetAbilityID(member.AbilityID).SetNillableItemID(optionalEntTypeIdentifier(member.ItemID)).
			SetTeraElementID(member.TeraElementID).SetNatureID(member.NatureID).SetLevel(int16(member.Level))
		if _, err := builder.Save(ctx); err != nil {
			return fmt.Errorf("创建 Team 第 %d 位成员: %w", member.Position, err)
		}
		for _, skill := range member.Skills {
			skillID, err := identifiers.Next(ctx)
			if err != nil {
				return fmt.Errorf("生成 Team 成员技能 Identifier: %w", err)
			}
			if _, err := client.PlayerCharacterTeamMemberSkill.Create().SetID(skillID).SetTeamID(value.ID).SetMemberPosition(int16(member.Position)).SetPosition(int16(skill.Position)).SetSkillID(skill.SkillID).Save(ctx); err != nil {
				return fmt.Errorf("创建 Team 第 %d 位成员技能: %w", member.Position, err)
			}
		}
		for _, stat := range member.Stats {
			statID, err := identifiers.Next(ctx)
			if err != nil {
				return fmt.Errorf("生成 Team 成员培养值 Identifier: %w", err)
			}
			if _, err := client.PlayerCharacterTeamMemberStat.Create().SetID(statID).SetTeamID(value.ID).SetMemberPosition(int16(member.Position)).SetStatID(stat.StatID).SetIndividualValue(int16(stat.IndividualValue)).SetEffortValue(int16(stat.EffortValue)).Save(ctx); err != nil {
				return fmt.Errorf("创建 Team 第 %d 位成员培养值: %w", member.Position, err)
			}
		}
	}
	return nil
}

// optionalEntTypeIdentifier 将领域可选 Identifier 转为 Ent 可空 Identifier。
func optionalEntTypeIdentifier(value *snowflake.ID) *snowflake.ID {
	if value == nil {
		return nil
	}
	converted := *value
	return &converted
}

// replaceRosterEnt 在同一 Ent 事务中删除旧阵容并写入新的成员、技能和培养值。
func replaceRosterEnt(ctx context.Context, client *avalonent.Client, identifiers snowflake.Source, value team.Team) error {
	if _, err := client.PlayerCharacterTeamMemberSkill.Delete().Where(playercharacterteammemberskill.TeamIDEQ(value.ID)).Exec(ctx); err != nil {
		return fmt.Errorf("清理 Team 旧成员技能: %w", err)
	}
	if _, err := client.PlayerCharacterTeamMemberStat.Delete().Where(playercharacterteammemberstat.TeamIDEQ(value.ID)).Exec(ctx); err != nil {
		return fmt.Errorf("清理 Team 旧成员培养值: %w", err)
	}
	if _, err := client.PlayerCharacterTeamMember.Delete().Where(playercharacterteammember.TeamIDEQ(value.ID)).Exec(ctx); err != nil {
		return fmt.Errorf("清理 Team 旧阵容: %w", err)
	}
	for _, member := range value.Members {
		memberID, err := identifiers.Next(ctx)
		if err != nil {
			return fmt.Errorf("生成 Team 成员 Identifier: %w", err)
		}
		if _, err := client.PlayerCharacterTeamMember.Create().SetID(memberID).SetTeamID(value.ID).SetPosition(int16(member.Position)).SetCreatureID(member.CreatureID).SetNillableFormID(optionalEntTypeIdentifier(member.FormID)).SetNillableGenderID(optionalEntTypeIdentifier(member.GenderID)).SetNillableSkinID(optionalEntTypeIdentifier(member.SkinID)).SetAbilityID(member.AbilityID).SetNillableItemID(optionalEntTypeIdentifier(member.ItemID)).SetTeraElementID(member.TeraElementID).SetNatureID(member.NatureID).SetLevel(int16(member.Level)).Save(ctx); err != nil {
			return fmt.Errorf("写入 Team 第 %d 位成员: %w", member.Position, err)
		}
		for _, skill := range member.Skills {
			skillID, err := identifiers.Next(ctx)
			if err != nil {
				return fmt.Errorf("生成 Team 成员技能 Identifier: %w", err)
			}
			if _, err := client.PlayerCharacterTeamMemberSkill.Create().SetID(skillID).SetTeamID(value.ID).SetMemberPosition(int16(member.Position)).SetPosition(int16(skill.Position)).SetSkillID(skill.SkillID).Save(ctx); err != nil {
				return fmt.Errorf("写入 Team 第 %d 位成员技能: %w", member.Position, err)
			}
		}
		for _, stat := range member.Stats {
			statID, err := identifiers.Next(ctx)
			if err != nil {
				return fmt.Errorf("生成 Team 成员培养值 Identifier: %w", err)
			}
			if _, err := client.PlayerCharacterTeamMemberStat.Create().SetID(statID).SetTeamID(value.ID).SetMemberPosition(int16(member.Position)).SetStatID(stat.StatID).SetIndividualValue(int16(stat.IndividualValue)).SetEffortValue(int16(stat.EffortValue)).Save(ctx); err != nil {
				return fmt.Errorf("写入 Team 第 %d 位成员培养值: %w", member.Position, err)
			}
		}
	}
	return nil
}

// Update 以乐观版本完整替换 Team 名称和阵容。
//
// 已提交的同键请求必须在确认角色所有权后、读取 Team 或验证 Current Game Data 前直接重放；因此来源 Team
// 后续删除不会使成功更新的客户端重试退化为不存在错误，同时无效账号会稳定映射为领域所有权错误。
func (s *adapters) Update(ctx context.Context, record team.UpdateRecord) (team.Team, error) {
	if !record.HasCurrentGameDataValidator() {
		return team.Team{}, team.ErrTeamCatalogUnavailable
	}
	var updated team.Team
	err := s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := s.pool.Client(transactionCtx)
		executor := database.Executor(transactionCtx, s.pool)
		if err := lockOwnedPlayerCharacter(ctx, client, record.ActorAccountID, record.Team.PlayerCharacterID); err != nil {
			return err
		}
		// 摘要保留规范化后的展示名称；NameKey 只服务于名称唯一性，不能抹去客户端可见的大小写差异。
		digest, err := idempotency.Digest(struct {
			TeamID          snowflake.ID
			ExpectedVersion int64
			Name            string
			Members         []team.Member
		}{record.Team.ID, record.ExpectedVersion, record.Team.Name, record.Team.Members})
		if err != nil {
			return fmt.Errorf("计算 Team 更新幂等摘要: %w", err)
		}
		request := idempotency.Request{
			ActorAccountID: record.ActorAccountID, OperationID: updateOperationID,
			Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.Team.UpdatedAt,
		}
		replayed, err := claimResponse(ctx, idempotency.NewEntRecords(client, s.newID), request, &updated)
		if err != nil || replayed {
			return err
		}
		currentRow, err := client.PlayerCharacterTeam.Query().Where(playercharacterteam.IDEQ(record.Team.ID), playercharacterteam.PlayerCharacterID(record.Team.PlayerCharacterID)).Only(ctx)
		if avalonent.IsNotFound(err) {
			return team.ErrTeamNotFound
		}
		if err != nil {
			return fmt.Errorf("查询待更新 Team: %w", err)
		}
		current := team.Team{ID: snowflake.ID(currentRow.ID), PlayerCharacterID: snowflake.ID(currentRow.PlayerCharacterID), Name: currentRow.Name, NameKey: currentRow.NameKey, Version: currentRow.Version, CreatedAt: currentRow.CreatedAt.UTC(), UpdatedAt: currentRow.UpdatedAt.UTC()}
		if current.Version != record.ExpectedVersion {
			return team.ErrTeamConflict
		}
		if err := loadRosterEnt(ctx, client, &current); err != nil {
			return err
		}
		if err := record.ValidateCurrentMembers(ctx); err != nil {
			return err
		}
		updated = current
		updated.Name = record.Team.Name
		updated.NameKey = record.Team.NameKey
		updated.Members = record.Team.Members
		updated.UpdatedAt = record.Team.UpdatedAt
		row, err := client.PlayerCharacterTeam.UpdateOne(currentRow).Where(playercharacterteam.VersionEQ(record.ExpectedVersion)).SetName(record.Team.Name).SetNameKey(record.Team.NameKey).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.Team.UpdatedAt.UTC()).Save(ctx)
		if isUniqueViolation(err) {
			return team.ErrTeamConflict
		}
		if err != nil {
			return fmt.Errorf("更新 Team: %w", err)
		}
		updated.Version = row.Version
		updated.Active, _ = client.ActivePlayerCharacterTeam.Query().Where(activeplayercharacterteam.IDEQ(record.Team.PlayerCharacterID), activeplayercharacterteam.TeamIDEQ(updated.ID)).Exist(ctx)
		if err := replaceRosterEnt(ctx, client, s.newID, updated); err != nil {
			return err
		}
		if err := s.recordAudit(ctx, executor, record.ActorAccountID, "team.updated", "team", updated.ID,
			updated, record.RequestID, updated.UpdatedAt); err != nil {
			return err
		}
		if err := completeResponse(ctx, idempotency.NewEntRecords(client, s.newID), request, updated); err != nil {
			return fmt.Errorf("保存 Team 更新幂等结果: %w", err)
		}
		return nil
	})
	return updated, err
}

// Delete 删除 Team；若删除当前活动 Team，则确定性绑定最早创建的剩余 Team。
func (s *adapters) Delete(ctx context.Context, record team.DeleteRecord) (team.DeleteResult, error) {
	var result team.DeleteResult
	err := s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := s.pool.Client(transactionCtx)
		executor := database.Executor(transactionCtx, s.pool)
		if err := lockOwnedPlayerCharacter(ctx, client, record.AccountID, record.PlayerCharacterID); err != nil {
			return err
		}
		digest, err := idempotency.Digest(struct {
			TeamID          snowflake.ID
			ExpectedVersion int64
		}{record.TeamID, record.ExpectedVersion})
		if err != nil {
			return fmt.Errorf("计算 Team 删除幂等摘要: %w", err)
		}
		request := idempotency.Request{
			ActorAccountID: record.AccountID, OperationID: deleteOperationID,
			Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DeletedAt,
		}
		replayed, err := claimResponse(ctx, idempotency.NewEntRecords(client, s.newID), request, &result)
		if err != nil || replayed {
			return err
		}
		row, err := client.PlayerCharacterTeam.Query().Where(playercharacterteam.IDEQ(record.TeamID), playercharacterteam.PlayerCharacterID(record.PlayerCharacterID)).Only(ctx)
		if avalonent.IsNotFound(err) {
			return team.ErrTeamConflict
		}
		if err != nil {
			return fmt.Errorf("查询待删除 Team: %w", err)
		}
		if row.Version != record.ExpectedVersion {
			return team.ErrTeamConflict
		}
		active, activeErr := client.ActivePlayerCharacterTeam.Query().Where(activeplayercharacterteam.IDEQ(record.PlayerCharacterID)).Only(ctx)
		if activeErr != nil && !avalonent.IsNotFound(activeErr) {
			return fmt.Errorf("查询待删除 Team 活动绑定: %w", activeErr)
		}
		if activeErr == nil && active.TeamID == row.ID {
			replacement, replacementErr := client.PlayerCharacterTeam.Query().Where(playercharacterteam.PlayerCharacterID(record.PlayerCharacterID), playercharacterteam.IDNEQ(record.TeamID)).Order(playercharacterteam.ByCreatedAt(), playercharacterteam.ByID()).First(ctx)
			if avalonent.IsNotFound(replacementErr) {
				if _, err := client.ActivePlayerCharacterTeam.Delete().Where(activeplayercharacterteam.IDEQ(record.PlayerCharacterID), activeplayercharacterteam.TeamIDEQ(record.TeamID)).Exec(ctx); err != nil {
					return fmt.Errorf("清除最后一支活动 Team: %w", err)
				}
			} else if replacementErr != nil {
				return fmt.Errorf("选择活动 Team 替代项: %w", replacementErr)
			} else {
				binding, err := client.ActivePlayerCharacterTeam.UpdateOne(active).Where(activeplayercharacterteam.VersionEQ(active.Version)).SetTeamID(replacement.ID).SetVersion(active.Version + 1).SetUpdatedAt(record.DeletedAt.UTC()).Save(ctx)
				if avalonent.IsNotFound(err) {
					return team.ErrTeamConflict
				}
				if err != nil {
					return fmt.Errorf("替换被删除的活动 Team: %w", err)
				}
				result.Active = &team.ActiveBinding{PlayerCharacterID: snowflake.ID(binding.ID), TeamID: snowflake.ID(binding.TeamID), Version: binding.Version, UpdatedAt: binding.UpdatedAt.UTC()}
			}
		} else if activeErr == nil {
			result.Active = &team.ActiveBinding{PlayerCharacterID: snowflake.ID(active.ID), TeamID: snowflake.ID(active.TeamID), Version: active.Version, UpdatedAt: active.UpdatedAt.UTC()}
		}
		if _, err := client.PlayerCharacterTeamMemberSkill.Delete().Where(playercharacterteammemberskill.TeamIDEQ(record.TeamID)).Exec(ctx); err != nil {
			return fmt.Errorf("删除 Team 成员技能: %w", err)
		}
		if _, err := client.PlayerCharacterTeamMemberStat.Delete().Where(playercharacterteammemberstat.TeamIDEQ(record.TeamID)).Exec(ctx); err != nil {
			return fmt.Errorf("删除 Team 成员培养值: %w", err)
		}
		if _, err := client.PlayerCharacterTeamMember.Delete().Where(playercharacterteammember.TeamIDEQ(record.TeamID)).Exec(ctx); err != nil {
			return fmt.Errorf("删除 Team 成员: %w", err)
		}
		if _, err := client.PlayerCharacterTeam.Delete().Where(playercharacterteam.IDEQ(record.TeamID), playercharacterteam.VersionEQ(record.ExpectedVersion)).Exec(ctx); err != nil {
			if avalonent.IsNotFound(err) {
				return team.ErrTeamConflict
			}
			return fmt.Errorf("删除 Team: %w", err)
		}
		result.DeletedTeamID = record.TeamID
		if err := s.recordAudit(ctx, executor, record.AccountID, "team.deleted", "team", record.TeamID,
			result, record.RequestID, record.DeletedAt); err != nil {
			return err
		}
		if err := completeResponse(ctx, idempotency.NewEntRecords(client, s.newID), request, result); err != nil {
			return fmt.Errorf("保存 Team 删除幂等结果: %w", err)
		}
		return nil
	})
	return result, err
}

// SwitchActive 使用持久绑定版本切换默认 Team。
func (s *adapters) SwitchActive(ctx context.Context, record team.SwitchActiveRecord) (team.ActiveBinding, error) {
	var result team.ActiveBinding
	err := s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := s.pool.Client(transactionCtx)
		executor := database.Executor(transactionCtx, s.pool)
		if err := lockOwnedPlayerCharacter(ctx, client, record.AccountID, record.PlayerCharacterID); err != nil {
			return err
		}
		digest, err := idempotency.Digest(struct {
			TeamID          snowflake.ID
			ExpectedVersion int64
		}{record.TeamID, record.ExpectedVersion})
		if err != nil {
			return fmt.Errorf("计算活动 Team 幂等摘要: %w", err)
		}
		request := idempotency.Request{
			ActorAccountID: record.AccountID, OperationID: switchActiveOperationID,
			Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt,
		}
		replayed, err := claimResponse(ctx, idempotency.NewEntRecords(client, s.newID), request, &result)
		if err != nil || replayed {
			return err
		}
		target, err := client.PlayerCharacterTeam.Query().Where(playercharacterteam.IDEQ(record.TeamID), playercharacterteam.PlayerCharacterID(record.PlayerCharacterID)).Only(ctx)
		if avalonent.IsNotFound(err) {
			return team.ErrTeamConflict
		}
		if err != nil {
			return fmt.Errorf("查询待激活 Team: %w", err)
		}
		current, currentErr := client.ActivePlayerCharacterTeam.Query().Where(activeplayercharacterteam.IDEQ(record.PlayerCharacterID)).Only(ctx)
		if currentErr != nil && !avalonent.IsNotFound(currentErr) {
			return fmt.Errorf("查询活动 Team: %w", currentErr)
		}
		var binding *avalonent.ActivePlayerCharacterTeam
		if record.ExpectedVersion == 0 {
			if currentErr == nil {
				return team.ErrTeamConflict
			}
			binding, err = client.ActivePlayerCharacterTeam.Create().SetID(record.PlayerCharacterID).SetTeamID(target.ID).SetVersion(1).SetUpdatedAt(record.UpdatedAt.UTC()).Save(ctx)
		} else {
			if currentErr != nil || current.Version != record.ExpectedVersion {
				return team.ErrTeamConflict
			}
			binding, err = client.ActivePlayerCharacterTeam.UpdateOne(current).Where(activeplayercharacterteam.VersionEQ(record.ExpectedVersion)).SetTeamID(target.ID).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.UpdatedAt.UTC()).Save(ctx)
		}
		if avalonent.IsNotFound(err) {
			return team.ErrTeamConflict
		}
		if err != nil {
			return fmt.Errorf("切换活动 Team: %w", err)
		}
		result = team.ActiveBinding{PlayerCharacterID: snowflake.ID(binding.ID), TeamID: snowflake.ID(binding.TeamID), Version: binding.Version, UpdatedAt: binding.UpdatedAt.UTC()}
		if err := s.recordAudit(ctx, executor, record.AccountID, "team.active-switched", "team", record.TeamID,
			result, record.RequestID, record.UpdatedAt); err != nil {
			return err
		}
		if err := completeResponse(ctx, idempotency.NewEntRecords(client, s.newID), request, result); err != nil {
			return fmt.Errorf("保存活动 Team 幂等结果: %w", err)
		}
		return nil
	})
	return result, err
}

func lockOwnedPlayerCharacter(
	ctx context.Context,
	client *avalonent.Client,
	accountID, playerCharacterID snowflake.ID,
) error {
	// 对角色版本执行加零更新会取得 PostgreSQL 行锁，同时不改变任何业务字段，
	// 以 Ent mutation 保留账号级并发不变量。
	_, err := client.PlayerCharacter.UpdateOneID(playerCharacterID).
		Where(playercharacter.AccountIDEQ(accountID), playercharacter.ArchivedAtIsNil()).
		AddVersion(0).Save(ctx)
	if avalonent.IsNotFound(err) {
		return team.ErrPlayerCharacterUnavailable
	}
	if err != nil {
		return fmt.Errorf("锁定 Team 所属 PlayerCharacter: %w", err)
	}
	return nil
}
