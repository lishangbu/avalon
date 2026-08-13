package team

import (
	"context"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// QueryStore 是账号私有 Team 详情与列表的持久化边界。
type QueryStore interface {
	GetOwned(context.Context, snowflake.ID, snowflake.ID, snowflake.ID) (Team, error)
	ListOwned(context.Context, snowflake.ID, snowflake.ID) ([]Team, error)
	GetActive(context.Context, snowflake.ID, snowflake.ID) (ActiveBinding, error)
}

// GetActive 返回指定角色当前默认 Team 的持久绑定。
func (s *QueryService) GetActive(
	ctx context.Context,
	accountID, playerCharacterID snowflake.ID,
) (ActiveBinding, error) {
	if accountID == snowflake.ID(0) || playerCharacterID == snowflake.ID(0) {
		return ActiveBinding{}, ErrInvalidTeam
	}
	return s.store.GetActive(ctx, accountID, playerCharacterID)
}

// QueryService 只通过账号与 PlayerCharacter 双重所有权边界公开 Team 查询。
type QueryService struct {
	// store 只暴露经账号与 PlayerCharacter 双重所有权约束的 Team 查询。
	store QueryStore
}

// NewQueryService 创建 Team 私有查询服务。
func NewQueryService(store QueryStore) *QueryService {
	return &QueryService{store: store}
}

// GetOwned 返回调用账号指定角色拥有的完整 Team。
func (s *QueryService) GetOwned(
	ctx context.Context,
	accountID, playerCharacterID, teamID snowflake.ID,
) (Team, error) {
	if accountID == snowflake.ID(0) || playerCharacterID == snowflake.ID(0) || teamID == snowflake.ID(0) {
		return Team{}, ErrInvalidTeam
	}
	return s.store.GetOwned(ctx, accountID, playerCharacterID, teamID)
}

// ListOwned 按稳定创建顺序返回调用账号指定角色的完整 Team 集合。
func (s *QueryService) ListOwned(
	ctx context.Context,
	accountID, playerCharacterID snowflake.ID,
) ([]Team, error) {
	if accountID == snowflake.ID(0) || playerCharacterID == snowflake.ID(0) {
		return nil, ErrInvalidTeam
	}
	return s.store.ListOwned(ctx, accountID, playerCharacterID)
}
