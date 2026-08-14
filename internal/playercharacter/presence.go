package playercharacter

import (
	"context"
	"errors"
	"sync"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// ActiveBindingReader 是 Presence 心跳解析持久活动角色所需的最小查询边界。
type ActiveBindingReader interface {
	GetActive(context.Context, snowflake.ID) (ActiveBinding, error)
}

// PresenceService 确保临时在线信号只能建立在账号当前持久活动角色上。
type PresenceService struct {
	active   ActiveBindingReader
	registry *PresenceRegistry
	now      func() time.Time
}

// NewPresenceService 使用显式活动绑定、注册表和时钟依赖创建 Presence 服务。
func NewPresenceService(active ActiveBindingReader, registry *PresenceRegistry, now func() time.Time) *PresenceService {
	return &PresenceService{active: active, registry: registry, now: now}
}

// Heartbeat 为当前认证会话刷新账号持久活动角色的在线信号。
func (s *PresenceService) Heartbeat(ctx context.Context, accountID, connectionID snowflake.ID) (ActiveBinding, error) {
	if accountID == snowflake.ID(0) || connectionID == snowflake.ID(0) {
		return ActiveBinding{}, ErrInvalidCommand
	}
	binding, err := s.active.GetActive(ctx, accountID)
	if errors.Is(err, ErrPlayerCharacterNotFound) {
		return ActiveBinding{}, ErrActivePlayerCharacterRequired
	}
	if err != nil {
		return ActiveBinding{}, err
	}
	s.registry.Open(binding.PlayerCharacterID, connectionID, s.now().UTC())
	return binding, nil
}

// PresenceRegistry 保存当前进程内已认证连接的最后活动时间。
// Presence 不是授权事实；进程重启会自然清空，并由后续心跳重新建立。
type PresenceRegistry struct {
	mu          sync.Mutex
	ttl         time.Duration
	connections map[snowflake.ID]map[snowflake.ID]time.Time
}

// NewPresenceRegistry 创建使用给定空闲超时的内存 Presence 注册表。
func NewPresenceRegistry(ttl time.Duration) *PresenceRegistry {
	if ttl <= 0 {
		panic("PlayerCharacter Presence TTL 必须为正数")
	}
	return &PresenceRegistry{ttl: ttl, connections: make(map[snowflake.ID]map[snowflake.ID]time.Time)}
}

// Open 建立或刷新角色的一条已认证连接。
func (r *PresenceRegistry) Open(playerCharacterID, connectionID snowflake.ID, at time.Time) {
	if playerCharacterID == snowflake.ID(0) || connectionID == snowflake.ID(0) {
		return
	}
	r.mu.Lock()
	defer r.mu.Unlock()
	connections := r.connections[playerCharacterID]
	if connections == nil {
		connections = make(map[snowflake.ID]time.Time)
		r.connections[playerCharacterID] = connections
	}
	connections[connectionID] = at.UTC()
}

// Close 移除角色的一条连接；其他设备仍在线时不会清除整个 Presence。
func (r *PresenceRegistry) Close(playerCharacterID, connectionID snowflake.ID) {
	r.mu.Lock()
	defer r.mu.Unlock()
	connections := r.connections[playerCharacterID]
	delete(connections, connectionID)
	if len(connections) == 0 {
		delete(r.connections, playerCharacterID)
	}
}

// Clear 清除角色的全部连接，用于活动角色切换和归档。
func (r *PresenceRegistry) Clear(playerCharacterID snowflake.ID) {
	r.mu.Lock()
	defer r.mu.Unlock()
	delete(r.connections, playerCharacterID)
}

// Online 判断角色是否至少有一条未超时连接，并顺便回收已过期条目。
func (r *PresenceRegistry) Online(playerCharacterID snowflake.ID, now time.Time) bool {
	r.mu.Lock()
	defer r.mu.Unlock()
	connections := r.connections[playerCharacterID]
	threshold := now.UTC().Add(-r.ttl)
	for connectionID, lastSeenAt := range connections {
		if lastSeenAt.Before(threshold) {
			delete(connections, connectionID)
		}
	}
	if len(connections) == 0 {
		delete(r.connections, playerCharacterID)
		return false
	}
	return true
}
