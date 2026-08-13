package authentication

import (
	"context"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// ErrIdentityNotFound 表示会话引用的玩家账号已经不可用。
var ErrIdentityNotFound = errors.New("玩家账号身份不存在")

// Identity 是玩家客户端恢复登录态所需的最小非敏感账号快照。
type Identity struct {
	// ID 是玩家安全域内稳定且不可修改的账号 Identifier。
	ID snowflake.ID
	// Username 是登录使用的规范化 ASCII 名称。
	Username string
	// DisplayName 是玩家账号的展示名称，不等同于 PlayerCharacter 名称。
	DisplayName string
}

// IdentityReader 从权威玩家账号存储读取身份。
type IdentityReader interface {
	FindIdentity(context.Context, snowflake.ID) (Identity, error)
}

// IdentityQuery 为当前玩家账号查询提供固定超时边界。
type IdentityQuery struct {
	// reader 是由查询方定义的最小持久化读取接口。
	reader IdentityReader
}

// NewIdentityQuery 使用显式读取边界创建玩家身份查询。
func NewIdentityQuery(reader IdentityReader) *IdentityQuery {
	return &IdentityQuery{reader: reader}
}

// Get 返回仍然有效的玩家账号身份快照。
func (q *IdentityQuery) Get(ctx context.Context, accountID snowflake.ID) (Identity, error) {
	if accountID == snowflake.ID(0) {
		return Identity{}, ErrIdentityNotFound
	}
	queryContext, cancel := context.WithTimeout(ctx, 2*time.Second)
	defer cancel()
	return q.reader.FindIdentity(queryContext, accountID)
}
