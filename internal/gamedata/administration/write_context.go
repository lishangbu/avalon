// Package administration 提供不同游戏资料类型共享的管理写入上下文。
package administration

import (
	"strings"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// GameDataWriteContext 标识一次实时游戏资料的已认证、可审计且可幂等重放管理写入。
type GameDataWriteContext struct {
	// ActorAccountID 是执行资料变更的管理员账号。
	ActorAccountID snowflake.ID
	// IdempotencyKey 在同一管理员和操作范围内唯一标识本次命令。
	IdempotencyKey string
	// RequestID 把业务审计记录关联到入口请求。
	RequestID string
}

// NewGameDataWriteContext 从 HTTP 或其他命令入口的原始管理事实创建实时资料写入上下文。
func NewGameDataWriteContext(
	actorAccountID snowflake.ID,
	idempotencyKey string,
	requestID string,
) GameDataWriteContext {
	return GameDataWriteContext{
		ActorAccountID: actorAccountID, IdempotencyKey: idempotencyKey, RequestID: requestID,
	}
}

// Normalize 去除只允许作为边界噪声存在的请求标识空白。
func (c GameDataWriteContext) Normalize() GameDataWriteContext {
	c.RequestID = strings.TrimSpace(c.RequestID)
	return c
}

// Valid 判断上下文是否足以安全执行一次实时资料管理写入。
func (c GameDataWriteContext) Valid() bool {
	return c.ActorAccountID != snowflake.ID(0) && idempotency.ValidKey(c.IdempotencyKey) && c.RequestID != ""
}
