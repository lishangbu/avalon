// Package admin 定义独立管理员身份域的应用边界。
package admin

import (
	"context"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// ErrIdentityNotFound 表示会话引用的管理员已经不可用。
var ErrIdentityNotFound = errors.New("管理员身份不存在")

// Identity 是管理端恢复登录态所需的最小非敏感身份快照。
type Identity struct {
	// ID 是管理员安全域内稳定且不可修改的 Identifier。
	ID snowflake.ID
	// Username 是登录使用的规范化 ASCII 名称。
	Username string
	// DisplayName 是管理页面和审计展示使用的全局唯一名称。
	DisplayName string
}

// IdentityReader 从权威存储读取一个仍然有效的管理员身份。
type IdentityReader interface {
	FindIdentity(context.Context, snowflake.ID) (Identity, error)
}

// IdentityQuery 为认证后的当前管理员查询提供固定超时边界。
type IdentityQuery struct {
	// reader 是由使用方定义的最小持久化读取接口。
	reader IdentityReader
}

// NewIdentityQuery 使用显式持久化边界创建管理员身份查询。
func NewIdentityQuery(reader IdentityReader) *IdentityQuery {
	return &IdentityQuery{reader: reader}
}

// Get 返回指定管理员的当前权威身份快照。
func (q *IdentityQuery) Get(ctx context.Context, accountID snowflake.ID) (Identity, error) {
	if accountID == snowflake.ID(0) {
		return Identity{}, ErrIdentityNotFound
	}
	queryContext, cancel := context.WithTimeout(ctx, 2*time.Second)
	defer cancel()
	return q.reader.FindIdentity(queryContext, accountID)
}
