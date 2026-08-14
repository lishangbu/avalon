package playercharacter

import (
	"context"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// Query 是 PlayerCharacter 所有权查询与最小公开查询的只读端口。
type Query interface {
	GetOwned(context.Context, snowflake.ID, snowflake.ID) (PlayerCharacter, error)
	ListOwned(context.Context, snowflake.ID, bool) ([]PlayerCharacter, error)
	GetActive(context.Context, snowflake.ID) (ActiveBinding, error)
	FindActiveByDisplayNameKey(context.Context, string) (PlayerCharacter, error)
}

// PresenceQuery 只公开公开查询需要的粗粒度在线判断。
type PresenceQuery interface {
	Online(snowflake.ID, time.Time) bool
}

// PublicPlayerCharacter 是允许向其他玩家公开的最小角色投影。
type PublicPlayerCharacter struct {
	DisplayName   string
	Online        bool
	Challengeable bool
}

// ChallengeTarget 是仅供 Battle 应用层创建 Challenge 使用的内部目标身份解析结果。
//
// 它不会通过玩家公共查询 API 返回；稳定 Identifier 只在已认证的服务内部用于冻结 Challenge 参与者身份。
type ChallengeTarget struct {
	// AccountID 是目标 PlayerCharacter 所属玩家账号的稳定 Identifier。
	AccountID snowflake.ID
	// PlayerCharacterID 是目标活动 PlayerCharacter 的稳定 Identifier。
	PlayerCharacterID snowflake.ID
	// DisplayName 是创建 Challenge 时冻结的目标公开展示名称。
	DisplayName string
}

// QueryService 提供账号私有查询和不泄露账号、队伍及内部状态的公开精确查询。
type QueryService struct {
	query    Query
	presence PresenceQuery
	now      func() time.Time
}

// NewQueryService 使用显式持久化、Presence 与时钟依赖创建查询服务。
func NewQueryService(query Query, presence PresenceQuery, now func() time.Time) *QueryService {
	return &QueryService{query: query, presence: presence, now: now}
}

// GetOwned 查询调用账号拥有的指定角色。
func (s *QueryService) GetOwned(ctx context.Context, accountID, playerCharacterID snowflake.ID) (PlayerCharacter, error) {
	if accountID == snowflake.ID(0) || playerCharacterID == snowflake.ID(0) {
		return PlayerCharacter{}, ErrInvalidCommand
	}
	return s.query.GetOwned(ctx, accountID, playerCharacterID)
}

// ListOwned 按创建顺序查询账号角色，并由调用方决定是否包含已归档角色。
func (s *QueryService) ListOwned(ctx context.Context, accountID snowflake.ID, includeArchived bool) ([]PlayerCharacter, error) {
	if accountID == snowflake.ID(0) {
		return nil, ErrInvalidCommand
	}
	return s.query.ListOwned(ctx, accountID, includeArchived)
}

// GetActive 返回全设备共享的持久活动绑定。
func (s *QueryService) GetActive(ctx context.Context, accountID snowflake.ID) (ActiveBinding, error) {
	if accountID == snowflake.ID(0) {
		return ActiveBinding{}, ErrInvalidCommand
	}
	return s.query.GetActive(ctx, accountID)
}

// FindPublicByDisplayName 要求调用账号已有活动角色，并仅按完整规范化名称精确查找未归档角色。
func (s *QueryService) FindPublicByDisplayName(ctx context.Context, callerAccountID snowflake.ID, rawDisplayName string) (PublicPlayerCharacter, error) {
	displayName, err := ParseDisplayName(rawDisplayName)
	if err != nil || callerAccountID == snowflake.ID(0) {
		return PublicPlayerCharacter{}, ErrInvalidCommand
	}
	caller, err := s.query.GetActive(ctx, callerAccountID)
	if err != nil {
		return PublicPlayerCharacter{}, ErrActivePlayerCharacterRequired
	}
	target, err := s.query.FindActiveByDisplayNameKey(ctx, displayName.Key())
	if err != nil {
		return PublicPlayerCharacter{}, err
	}
	online := s.presence != nil && s.presence.Online(target.ID, s.now().UTC())
	return PublicPlayerCharacter{
		DisplayName: target.DisplayName, Online: online,
		Challengeable: online && target.ID != caller.PlayerCharacterID,
	}, nil
}

// ResolveChallengeTarget 解析可被当前账号挑战的在线活动 PlayerCharacter。
//
// 该内部用例复用公共精确查找相同的展示名称、活动角色和 Presence 规则，但只将稳定身份交给
// 创建 Challenge 的应用层，不将其作为公共角色检索接口的响应字段暴露。
func (s *QueryService) ResolveChallengeTarget(
	ctx context.Context,
	challengerAccountID snowflake.ID,
	rawDisplayName string,
) (ChallengeTarget, error) {
	displayName, err := ParseDisplayName(rawDisplayName)
	if err != nil || challengerAccountID == snowflake.ID(0) {
		return ChallengeTarget{}, ErrInvalidCommand
	}
	challenger, err := s.query.GetActive(ctx, challengerAccountID)
	if err != nil {
		return ChallengeTarget{}, ErrActivePlayerCharacterRequired
	}
	target, err := s.query.FindActiveByDisplayNameKey(ctx, displayName.Key())
	if err != nil {
		return ChallengeTarget{}, err
	}
	if target.ID == challenger.PlayerCharacterID || s.presence == nil || !s.presence.Online(target.ID, s.now().UTC()) {
		return ChallengeTarget{}, ErrChallengeTargetUnavailable
	}
	return ChallengeTarget{AccountID: target.AccountID, PlayerCharacterID: target.ID, DisplayName: target.DisplayName}, nil
}
