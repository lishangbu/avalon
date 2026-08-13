package store

import (
	"context"
	"errors"
	"fmt"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/activeplayercharacterteam"
	"github.com/lishangbu/avalon/ent/playercharacter"
	"github.com/lishangbu/avalon/ent/playercharacterteam"
	"github.com/lishangbu/avalon/ent/playercharacterteammember"
	"github.com/lishangbu/avalon/ent/playercharacterteammemberskill"
	"github.com/lishangbu/avalon/ent/playercharacterteammemberstat"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/team"
)

// GetOwned 按账号与 PlayerCharacter 双重所有权读取完整 Team。
func (s *Store) GetOwned(ctx context.Context, accountID, playerCharacterID, teamID snowflake.ID) (team.Team, error) {
	client := s.pool.Client(ctx)
	if _, err := client.PlayerCharacter.Query().Where(playercharacter.IDEQ(playerCharacterID), playercharacter.AccountIDEQ(accountID), playercharacter.ArchivedAtIsNil()).Only(ctx); err != nil {
		if avalonent.IsNotFound(err) {
			return team.Team{}, team.ErrTeamNotFound
		}
		return team.Team{}, fmt.Errorf("校验 Team 所属 PlayerCharacter: %w", err)
	}
	row, err := client.PlayerCharacterTeam.Query().Where(playercharacterteam.IDEQ(teamID), playercharacterteam.HasPlayerCharacterWith(playercharacter.IDEQ(playerCharacterID))).Only(ctx)
	if avalonent.IsNotFound(err) {
		return team.Team{}, team.ErrTeamNotFound
	}
	if err != nil {
		return team.Team{}, fmt.Errorf("查询账号 Team: %w", err)
	}
	value := team.Team{ID: snowflake.ID(row.ID), PlayerCharacterID: snowflake.ID(row.PlayerCharacterID), Name: row.Name, NameKey: row.NameKey, Version: row.Version, CreatedAt: row.CreatedAt.UTC(), UpdatedAt: row.UpdatedAt.UTC()}
	active, _ := client.ActivePlayerCharacterTeam.Query().Where(activeplayercharacterteam.IDEQ(row.PlayerCharacterID), activeplayercharacterteam.TeamIDEQ(row.ID)).Exist(ctx)
	value.Active = active
	if err := loadRosterEnt(ctx, client, &value); err != nil {
		return team.Team{}, err
	}
	return value, nil
}

// ListOwned 按稳定创建顺序读取账号指定角色的完整 Team 集合。
func (s *Store) ListOwned(ctx context.Context, accountID, playerCharacterID snowflake.ID) ([]team.Team, error) {
	client := s.pool.Client(ctx)
	if _, err := client.PlayerCharacter.Query().Where(playercharacter.IDEQ(playerCharacterID), playercharacter.AccountIDEQ(accountID), playercharacter.ArchivedAtIsNil()).Only(ctx); err != nil {
		if avalonent.IsNotFound(err) {
			return nil, team.ErrPlayerCharacterUnavailable
		}
		return nil, fmt.Errorf("校验 Team 所属 PlayerCharacter: %w", err)
	}
	rows, err := client.PlayerCharacterTeam.Query().Where(playercharacterteam.HasPlayerCharacterWith(playercharacter.IDEQ(playerCharacterID))).Order(playercharacterteam.ByCreatedAt()).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询账号 Team 列表: %w", err)
	}
	result := make([]team.Team, len(rows))
	for i, row := range rows {
		result[i] = team.Team{ID: snowflake.ID(row.ID), PlayerCharacterID: snowflake.ID(row.PlayerCharacterID), Name: row.Name, NameKey: row.NameKey, Version: row.Version, CreatedAt: row.CreatedAt.UTC(), UpdatedAt: row.UpdatedAt.UTC()}
		result[i].Active, _ = client.ActivePlayerCharacterTeam.Query().Where(activeplayercharacterteam.IDEQ(row.PlayerCharacterID), activeplayercharacterteam.TeamIDEQ(row.ID)).Exist(ctx)
		if err := loadRosterEnt(ctx, client, &result[i]); err != nil {
			return nil, err
		}
	}
	return result, nil
}

// GetActive 返回账号指定角色当前默认 Team 的乐观版本绑定。
func (s *Store) GetActive(ctx context.Context, accountID, playerCharacterID snowflake.ID) (team.ActiveBinding, error) {
	if _, err := s.pool.Client(ctx).PlayerCharacter.Query().Where(playercharacter.IDEQ(playerCharacterID), playercharacter.AccountIDEQ(accountID), playercharacter.ArchivedAtIsNil()).Only(ctx); err != nil {
		if avalonent.IsNotFound(err) {
			return team.ActiveBinding{}, team.ErrTeamNotFound
		}
		return team.ActiveBinding{}, err
	}
	row, err := s.pool.Client(ctx).ActivePlayerCharacterTeam.Query().Where(activeplayercharacterteam.IDEQ(playerCharacterID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return team.ActiveBinding{}, team.ErrTeamNotFound
	}
	if err != nil {
		return team.ActiveBinding{}, fmt.Errorf("查询活动 Team: %w", err)
	}
	return team.ActiveBinding{PlayerCharacterID: snowflake.ID(row.ID), TeamID: snowflake.ID(row.TeamID), Version: row.Version, UpdatedAt: row.UpdatedAt.UTC()}, nil
}

// loadRosterEnt 使用 Ent 查询加载 Team 成员、技能和培养值，保持各位置的稳定顺序。
func loadRosterEnt(ctx context.Context, client *avalonent.Client, value *team.Team) error {
	members, err := client.PlayerCharacterTeamMember.Query().Where(playercharacterteammember.TeamIDEQ(value.ID)).Order(playercharacterteammember.ByPosition()).All(ctx)
	if err != nil {
		return fmt.Errorf("查询 Team 成员: %w", err)
	}
	value.Members = make([]team.Member, len(members))
	byPos := make(map[int16]int, len(members))
	for i, row := range members {
		value.Members[i] = team.Member{Position: int32(row.Position), CreatureID: snowflake.ID(row.CreatureID), FormID: optionalEntIdentifier(row.FormID), GenderID: optionalEntIdentifier(row.GenderID), SkinID: optionalEntIdentifier(row.SkinID), AbilityID: snowflake.ID(row.AbilityID), ItemID: optionalEntIdentifier(row.ItemID), TeraElementID: snowflake.ID(row.TeraElementID), NatureID: snowflake.ID(row.NatureID), Level: int32(row.Level), Skills: []team.MemberSkill{}, Stats: []team.MemberStat{}}
		byPos[row.Position] = i
	}
	skills, err := client.PlayerCharacterTeamMemberSkill.Query().Where(playercharacterteammemberskill.TeamIDEQ(value.ID)).Order(playercharacterteammemberskill.ByMemberPosition(), playercharacterteammemberskill.ByPosition()).All(ctx)
	if err != nil {
		return fmt.Errorf("查询 Team 成员技能: %w", err)
	}
	for _, row := range skills {
		i, ok := byPos[row.MemberPosition]
		if !ok {
			return errors.New("team 技能缺少所属成员")
		}
		value.Members[i].Skills = append(value.Members[i].Skills, team.MemberSkill{Position: int32(row.Position), SkillID: snowflake.ID(row.SkillID)})
	}
	stats, err := client.PlayerCharacterTeamMemberStat.Query().Where(playercharacterteammemberstat.TeamIDEQ(value.ID)).Order(playercharacterteammemberstat.ByMemberPosition()).All(ctx)
	if err != nil {
		return fmt.Errorf("查询 Team 成员培养值: %w", err)
	}
	for _, row := range stats {
		i, ok := byPos[row.MemberPosition]
		if !ok {
			return errors.New("team 培养值缺少所属成员")
		}
		value.Members[i].Stats = append(value.Members[i].Stats, team.MemberStat{StatID: snowflake.ID(row.StatID), IndividualValue: int32(row.IndividualValue), EffortValue: int32(row.EffortValue)})
	}
	return nil
}

func optionalEntIdentifier(value *snowflake.ID) *snowflake.ID {
	if value == nil {
		return nil
	}
	result := snowflake.ID(*value)
	return &result
}
