package playercharacter

import (
	"context"
	"errors"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

var (
	// ErrPlayerCharacterNotFound 表示角色或活动绑定不存在，或不属于当前账号。
	ErrPlayerCharacterNotFound = errors.New("PlayerCharacter 不存在")
	// ErrActiveBindingConflict 表示活动绑定已经被其他设备修改。
	ErrActiveBindingConflict = errors.New("活动 PlayerCharacter 版本冲突")
	// ErrActivePlayerCharacterRequired 表示调用账号尚未选择活动角色。
	ErrActivePlayerCharacterRequired = errors.New("需要活动 PlayerCharacter")
)

// ActiveBinding 是跨会话持久化的账号唯一活动角色绑定。
type ActiveBinding struct {
	AccountID         snowflake.ID
	PlayerCharacterID snowflake.ID
	Version           int64
	UpdatedAt         time.Time
}

// SwitchActiveCommand 使用乐观版本切换账号所有设备共享的活动角色。
// ExpectedVersion 为 0 时只允许创建尚不存在的首个绑定。
type SwitchActiveCommand struct {
	AccountID         snowflake.ID
	PlayerCharacterID snowflake.ID
	ExpectedVersion   int64
	IdempotencyKey    string
	RequestID         string
}

// SwitchActiveRecord 是存储层原子校验角色所有权并替换绑定所需的事实。
type SwitchActiveRecord struct {
	AccountID         snowflake.ID
	PlayerCharacterID snowflake.ID
	ExpectedVersion   int64
	IdempotencyKey    string
	RequestID         string
	UpdatedAt         time.Time
}

// SwitchActiveResult 保存事务实际观察到的前一绑定，以及本次结果是否来自幂等重放。
// 应用服务只对首次提交执行 Presence 清理和同步通知，避免迟到重放撤销较新的状态。
type SwitchActiveResult struct {
	// Binding 是切换提交后生效的活动角色绑定。
	Binding ActiveBinding `json:"binding"`
	// PreviousPlayerCharacterID 是切换前的活动角色；首次建立绑定时为零值并从持久化响应中省略。
	PreviousPlayerCharacterID snowflake.ID `json:"previousPlayerCharacterId,omitempty"`
	// Replayed 表示结果来自幂等响应重放，而不是本次事务首次提交。
	Replayed bool `json:"replayed"`
}

// ActiveStore 提供活动绑定的查询和账号级原子切换。
type ActiveStore interface {
	SwitchActive(context.Context, SwitchActiveRecord) (SwitchActiveResult, error)
}

// PresenceCleaner 清除角色的全部临时在线连接。
type PresenceCleaner interface {
	Clear(snowflake.ID)
}

// ActiveNotifier 向同账号的其他连接广播持久绑定已经变化。
type ActiveNotifier interface {
	ActivePlayerCharacterChanged(context.Context, ActiveBinding)
}

// ActiveService 编排持久活动绑定、Presence 清理和多设备同步通知。
type ActiveService struct {
	store    ActiveStore
	presence PresenceCleaner
	notifier ActiveNotifier
	now      func() time.Time
}

// NewActiveService 使用显式依赖创建活动角色服务。
func NewActiveService(store ActiveStore, presence PresenceCleaner, notifier ActiveNotifier, now func() time.Time) *ActiveService {
	return &ActiveService{store: store, presence: presence, notifier: notifier, now: now}
}

// Switch 以乐观版本替换全账号共享的活动角色，并清除旧角色的临时在线状态。
func (s *ActiveService) Switch(ctx context.Context, command SwitchActiveCommand) (ActiveBinding, error) {
	command.IdempotencyKey = strings.TrimSpace(command.IdempotencyKey)
	command.RequestID = strings.TrimSpace(command.RequestID)
	if command.AccountID == snowflake.ID(0) || command.PlayerCharacterID == snowflake.ID(0) || command.ExpectedVersion < 0 ||
		command.IdempotencyKey == "" || len(command.IdempotencyKey) > 128 || command.RequestID == "" {
		return ActiveBinding{}, ErrInvalidCommand
	}
	result, err := s.store.SwitchActive(ctx, SwitchActiveRecord{
		AccountID: command.AccountID, PlayerCharacterID: command.PlayerCharacterID,
		ExpectedVersion: command.ExpectedVersion,
		IdempotencyKey:  command.IdempotencyKey, RequestID: command.RequestID, UpdatedAt: s.now().UTC(),
	})
	if err != nil {
		return ActiveBinding{}, err
	}
	if result.Replayed {
		return result.Binding, nil
	}
	if result.PreviousPlayerCharacterID != snowflake.ID(0) &&
		result.PreviousPlayerCharacterID != result.Binding.PlayerCharacterID && s.presence != nil {
		s.presence.Clear(result.PreviousPlayerCharacterID)
	}
	if s.notifier != nil {
		s.notifier.ActivePlayerCharacterChanged(ctx, result.Binding)
	}
	return result.Binding, nil
}
