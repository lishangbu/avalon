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

// IdentityService 为当前玩家账号身份读取提供固定超时边界。
type IdentityService struct {
	// reader 是由使用方定义的最小持久化读取接口。
	reader IdentityReader
}

// NewIdentityService 使用显式 Reader 创建玩家身份服务。
func NewIdentityService(reader IdentityReader) *IdentityService {
	return &IdentityService{reader: reader}
}

// Get 返回仍然有效的玩家账号身份快照。
func (s *IdentityService) Get(ctx context.Context, accountID snowflake.ID) (Identity, error) {
	if accountID == snowflake.ID(0) {
		return Identity{}, ErrIdentityNotFound
	}
	readContext, cancel := context.WithTimeout(ctx, 2*time.Second)
	defer cancel()
	return s.reader.FindIdentity(readContext, accountID)
}
