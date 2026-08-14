package team

import (
	"context"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// Reader 返回账号私有 Team 与活动绑定领域对象。
type Reader interface {
	GetOwned(context.Context, snowflake.ID, snowflake.ID, snowflake.ID) (Team, error)
	GetActive(context.Context, snowflake.ID, snowflake.ID) (ActiveBinding, error)
}

// Query 返回账号私有 Team 列表投影。
type Query interface {
	ListOwned(context.Context, snowflake.ID, snowflake.ID) ([]Team, error)
}

// GetActive 返回指定角色当前默认 Team 的持久绑定。
func (s *QueryService) GetActive(
	ctx context.Context,
	accountID, playerCharacterID snowflake.ID,
) (ActiveBinding, error) {
	if accountID == snowflake.ID(0) || playerCharacterID == snowflake.ID(0) {
		return ActiveBinding{}, ErrInvalidTeam
	}
	return s.reader.GetActive(ctx, accountID, playerCharacterID)
}

// QueryService 只通过账号与 PlayerCharacter 双重所有权边界公开 Team 查询。
type QueryService struct {
	// reader 只暴露经账号与 PlayerCharacter 双重所有权约束的 Team 领域对象读取。
	reader Reader
	// query 只暴露经账号与 PlayerCharacter 双重所有权约束的 Team 查询。
	query Query
}

// NewQueryService 创建 Team 私有查询服务。
func NewQueryService(reader Reader, query Query) *QueryService {
	return &QueryService{reader: reader, query: query}
}

// GetOwned 返回调用账号指定角色拥有的完整 Team。
func (s *QueryService) GetOwned(
	ctx context.Context,
	accountID, playerCharacterID, teamID snowflake.ID,
) (Team, error) {
	if accountID == snowflake.ID(0) || playerCharacterID == snowflake.ID(0) || teamID == snowflake.ID(0) {
		return Team{}, ErrInvalidTeam
	}
	return s.reader.GetOwned(ctx, accountID, playerCharacterID, teamID)
}

// ListOwned 按稳定创建顺序返回调用账号指定角色的完整 Team 集合。
func (s *QueryService) ListOwned(
	ctx context.Context,
	accountID, playerCharacterID snowflake.ID,
) ([]Team, error) {
	if accountID == snowflake.ID(0) || playerCharacterID == snowflake.ID(0) {
		return nil, ErrInvalidTeam
	}
	return s.query.ListOwned(ctx, accountID, playerCharacterID)
}
