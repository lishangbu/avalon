package api

import (
	"context"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/team"
)

// LifecycleService 管理 Team 创建、更新、删除与活动绑定。
type LifecycleService interface {
	Create(context.Context, team.CreateCommand) (team.Team, error)
	Update(context.Context, team.UpdateCommand) (team.Team, error)
	Delete(context.Context, team.DeleteCommand) (team.DeleteResult, error)
	SwitchActive(context.Context, team.SwitchActiveCommand) (team.ActiveBinding, error)
}

// QueryService 提供账号和角色所有权范围内的 Team 查询。
type QueryService interface {
	GetOwned(context.Context, snowflake.ID, snowflake.ID, snowflake.ID) (team.Team, error)
	ListOwned(context.Context, snowflake.ID, snowflake.ID) ([]team.Team, error)
	GetActive(context.Context, snowflake.ID, snowflake.ID) (team.ActiveBinding, error)
}

// ShareService 管理 Team 分享快照的创建、解析、撤销和导入。
type ShareService interface {
	Create(context.Context, team.CreateShareCommand) (team.CreateShareResult, error)
	Resolve(context.Context, string) (team.ShareSnapshot, error)
	Revoke(context.Context, team.RevokeShareCommand) (team.Share, error)
	Import(context.Context, team.ImportShareCommand) (team.Team, error)
}
