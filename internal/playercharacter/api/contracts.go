package api

import (
	"context"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/playercharacter"
)

// LifecycleService 管理角色创建、改名、归档和恢复生命周期。
type LifecycleService interface {
	Create(context.Context, playercharacter.CreateCommand) (playercharacter.PlayerCharacter, error)
	Rename(context.Context, playercharacter.RenameCommand) (playercharacter.PlayerCharacter, error)
	Archive(context.Context, playercharacter.ArchiveCommand) (playercharacter.PlayerCharacter, error)
	Restore(context.Context, playercharacter.RestoreCommand) (playercharacter.PlayerCharacter, error)
}

// QueryService 提供账号所有权范围内的角色查询和公开名称查找。
type QueryService interface {
	GetOwned(context.Context, snowflake.ID, snowflake.ID) (playercharacter.PlayerCharacter, error)
	ListOwned(context.Context, snowflake.ID, bool) ([]playercharacter.PlayerCharacter, error)
	GetActive(context.Context, snowflake.ID) (playercharacter.ActiveBinding, error)
	FindPublicByDisplayName(context.Context, snowflake.ID, string) (playercharacter.PublicPlayerCharacter, error)
}

// ActiveService 原子切换账号当前使用的唯一角色。
type ActiveService interface {
	Switch(context.Context, playercharacter.SwitchActiveCommand) (playercharacter.ActiveBinding, error)
}

// PresenceService 使用当前会话续约活动角色在线状态。
type PresenceService interface {
	Heartbeat(context.Context, snowflake.ID, snowflake.ID) (playercharacter.ActiveBinding, error)
}
