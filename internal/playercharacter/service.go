package playercharacter

import (
	"context"
	"errors"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// MaximumUnarchivedPerAccount 是单个账号可以同时拥有的未归档 PlayerCharacter 数量上限。
// 存储适配器在幂等请求认领后使用该策略值，保证并发写入和请求重放都遵守同一不变量。
const MaximumUnarchivedPerAccount int64 = 3

var (
	// ErrInvalidCommand 表示 PlayerCharacter 命令缺少账号、幂等键或合法业务字段。
	ErrInvalidCommand = errors.New("PlayerCharacter 命令无效")
	// ErrActiveLimitExceeded 表示账号已经拥有三名未归档 PlayerCharacter。
	ErrActiveLimitExceeded = errors.New("PlayerCharacter 数量已达上限")
	// ErrSensitiveDisplayName 表示展示名称命中了当前启用的敏感名称规则。
	ErrSensitiveDisplayName = errors.New("PlayerCharacter 展示名称不可用")
	// ErrDisplayNameUnavailable 表示展示名称正被其他角色使用或已进入其他角色的历史。
	ErrDisplayNameUnavailable = errors.New("PlayerCharacter 展示名称已被占用")
	// ErrVersionConflict 表示角色不存在、不属于调用账号、状态不允许或版本已经变化。
	ErrVersionConflict = errors.New("PlayerCharacter 版本或状态冲突")
	// ErrChallengeTargetUnavailable 表示目标不是另一名在线活动 PlayerCharacter，不能创建 Challenge。
	ErrChallengeTargetUnavailable = errors.New("挑战目标不可用")
)

// PlayerCharacter 是 Account 拥有的持久游戏角色。
type PlayerCharacter struct {
	ID             snowflake.ID
	AccountID      snowflake.ID
	DisplayName    string
	DisplayNameKey string
	Version        int64
	ArchivedAt     *time.Time
	CreatedAt      time.Time
	UpdatedAt      time.Time
}

// CreateCommand 包含创建 PlayerCharacter 所需的认证账号与幂等请求上下文。
type CreateCommand struct {
	AccountID      snowflake.ID
	DisplayName    string
	IdempotencyKey string
	RequestID      string
}

// CreateRecord 是存储层原子创建角色、名称历史和幂等响应所需的完整事实。
type CreateRecord struct {
	PlayerCharacter PlayerCharacter
	ModerationKey   string
	IdempotencyKey  string
	RequestID       string
}

// RenameCommand 使用乐观版本为账号拥有的 PlayerCharacter 设置新的全局展示名称。
type RenameCommand struct {
	AccountID         snowflake.ID
	PlayerCharacterID snowflake.ID
	ExpectedVersion   int64
	DisplayName       string
	IdempotencyKey    string
	RequestID         string
}

// RenameRecord 是存储层原子保留历史名称、更新当前名称和保存幂等响应所需的事实。
type RenameRecord struct {
	AccountID         snowflake.ID
	PlayerCharacterID snowflake.ID
	ExpectedVersion   int64
	DisplayName       string
	DisplayNameKey    string
	ModerationKey     string
	IdempotencyKey    string
	RequestID         string
	UpdatedAt         time.Time
}

// ArchiveCommand 使用乐观版本归档账号拥有的 PlayerCharacter。
type ArchiveCommand struct {
	AccountID         snowflake.ID
	PlayerCharacterID snowflake.ID
	ExpectedVersion   int64
	IdempotencyKey    string
	RequestID         string
}

// ArchiveRecord 是存储层阻断活跃 Battle 并原子清理活动绑定与 Pending Challenge 所需的事实。
type ArchiveRecord struct {
	AccountID         snowflake.ID
	PlayerCharacterID snowflake.ID
	ExpectedVersion   int64
	IdempotencyKey    string
	RequestID         string
	ArchivedAt        time.Time
}

// RestoreCommand 使用乐观版本恢复账号拥有的已归档 PlayerCharacter。
type RestoreCommand struct {
	AccountID         snowflake.ID
	PlayerCharacterID snowflake.ID
	ExpectedVersion   int64
	IdempotencyKey    string
	RequestID         string
}

// RestoreRecord 是存储层重新检查账号上限并清除归档状态所需的完整事实。
type RestoreRecord struct {
	AccountID         snowflake.ID
	PlayerCharacterID snowflake.ID
	ExpectedVersion   int64
	IdempotencyKey    string
	RequestID         string
	RestoredAt        time.Time
}

// Writer 是按 Account 串行化的一次 PlayerCharacter 写事务边界。
type Writer interface {
	Create(context.Context, CreateRecord) (PlayerCharacter, error)
	Rename(context.Context, RenameRecord) (PlayerCharacter, error)
	Archive(context.Context, ArchiveRecord) (PlayerCharacter, error)
	Restore(context.Context, RestoreRecord) (PlayerCharacter, error)
}

// Repository 提供 PlayerCharacter 事务执行边界，并负责锁定目标 Account。
type Repository interface {
	WithinAccount(context.Context, snowflake.ID, func(Writer) error) error
}

// Service 编排 PlayerCharacter 生命周期命令。
type Service struct {
	repository Repository
	newID      snowflake.Source
	now        func() time.Time
	presence   PresenceCleaner
}

// NewService 使用显式 Repository、Identifier 和时钟依赖创建 PlayerCharacter 服务。
func NewService(repository Repository, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{repository: repository, newID: newID, now: now}
}

// NewServiceWithPresence 创建在角色归档后同步清理临时 Presence 的生命周期服务。
func NewServiceWithPresence(
	repository Repository, presence PresenceCleaner,
	newID snowflake.Source,
	now func() time.Time,
) *Service {
	return &Service{repository: repository, presence: presence, newID: newID, now: now}
}

// Create 在账号级事务内创建版本为 1 的未归档 PlayerCharacter。
func (s *Service) Create(ctx context.Context, command CreateCommand) (PlayerCharacter, error) {
	command.IdempotencyKey = strings.TrimSpace(command.IdempotencyKey)
	command.RequestID = strings.TrimSpace(command.RequestID)
	displayName, err := ParseDisplayName(command.DisplayName)
	if err != nil || command.AccountID == snowflake.ID(0) || command.IdempotencyKey == "" ||
		len(command.IdempotencyKey) > 128 || command.RequestID == "" {
		return PlayerCharacter{}, ErrInvalidCommand
	}
	id, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return PlayerCharacter{}, idErr
	}
	now := s.now().UTC()
	character := PlayerCharacter{
		ID: id, AccountID: command.AccountID, DisplayName: displayName.String(),
		DisplayNameKey: displayName.Key(), Version: 1, CreatedAt: now, UpdatedAt: now,
	}
	var created PlayerCharacter
	err = s.repository.WithinAccount(ctx, command.AccountID, func(writer Writer) error {
		created, err = writer.Create(ctx, CreateRecord{
			PlayerCharacter: character,
			ModerationKey:   displayName.ModerationKey(),
			IdempotencyKey:  command.IdempotencyKey,
			RequestID:       command.RequestID,
		})
		return err
	})
	if err != nil {
		return PlayerCharacter{}, err
	}
	return created, nil
}

// Rename 原子保留历史名称并以乐观版本更新 PlayerCharacter 的当前展示名称。
func (s *Service) Rename(ctx context.Context, command RenameCommand) (PlayerCharacter, error) {
	command.IdempotencyKey = strings.TrimSpace(command.IdempotencyKey)
	command.RequestID = strings.TrimSpace(command.RequestID)
	displayName, err := ParseDisplayName(command.DisplayName)
	if err != nil || command.AccountID == snowflake.ID(0) || command.PlayerCharacterID == snowflake.ID(0) ||
		command.ExpectedVersion < 1 || command.IdempotencyKey == "" || len(command.IdempotencyKey) > 128 ||
		command.RequestID == "" {
		return PlayerCharacter{}, ErrInvalidCommand
	}
	var renamed PlayerCharacter
	err = s.repository.WithinAccount(ctx, command.AccountID, func(writer Writer) error {
		renamed, err = writer.Rename(ctx, RenameRecord{
			AccountID: command.AccountID, PlayerCharacterID: command.PlayerCharacterID,
			ExpectedVersion: command.ExpectedVersion, DisplayName: displayName.String(),
			DisplayNameKey: displayName.Key(), ModerationKey: displayName.ModerationKey(),
			IdempotencyKey: command.IdempotencyKey, RequestID: command.RequestID, UpdatedAt: s.now().UTC(),
		})
		return err
	})
	if err != nil {
		return PlayerCharacter{}, err
	}
	return renamed, nil
}

// Archive 在保留稳定身份和历史的前提下归档 PlayerCharacter。
func (s *Service) Archive(ctx context.Context, command ArchiveCommand) (PlayerCharacter, error) {
	command.IdempotencyKey = strings.TrimSpace(command.IdempotencyKey)
	command.RequestID = strings.TrimSpace(command.RequestID)
	if !validLifecycleCommand(
		command.AccountID,
		command.PlayerCharacterID,
		command.ExpectedVersion,
		command.IdempotencyKey,
		command.RequestID,
	) {
		return PlayerCharacter{}, ErrInvalidCommand
	}
	var archived PlayerCharacter
	err := s.repository.WithinAccount(ctx, command.AccountID, func(writer Writer) error {
		var archiveErr error
		archived, archiveErr = writer.Archive(ctx, ArchiveRecord{
			AccountID: command.AccountID, PlayerCharacterID: command.PlayerCharacterID,
			ExpectedVersion: command.ExpectedVersion, IdempotencyKey: command.IdempotencyKey,
			RequestID: command.RequestID, ArchivedAt: s.now().UTC(),
		})
		return archiveErr
	})
	if err != nil {
		return PlayerCharacter{}, err
	}
	if s.presence != nil {
		s.presence.Clear(archived.ID)
	}
	return archived, nil
}

// Restore 在账号仍有角色容量时清除 PlayerCharacter 的归档状态。
func (s *Service) Restore(ctx context.Context, command RestoreCommand) (PlayerCharacter, error) {
	command.IdempotencyKey = strings.TrimSpace(command.IdempotencyKey)
	command.RequestID = strings.TrimSpace(command.RequestID)
	if !validLifecycleCommand(
		command.AccountID,
		command.PlayerCharacterID,
		command.ExpectedVersion,
		command.IdempotencyKey,
		command.RequestID,
	) {
		return PlayerCharacter{}, ErrInvalidCommand
	}
	var restored PlayerCharacter
	err := s.repository.WithinAccount(ctx, command.AccountID, func(writer Writer) error {
		var restoreErr error
		restored, restoreErr = writer.Restore(ctx, RestoreRecord{
			AccountID: command.AccountID, PlayerCharacterID: command.PlayerCharacterID,
			ExpectedVersion: command.ExpectedVersion, IdempotencyKey: command.IdempotencyKey,
			RequestID: command.RequestID, RestoredAt: s.now().UTC(),
		})
		return restoreErr
	})
	if err != nil {
		return PlayerCharacter{}, err
	}
	return restored, nil
}

func validLifecycleCommand(
	accountID snowflake.ID,
	playerCharacterID snowflake.ID,
	expectedVersion int64,
	idempotencyKey string,
	requestID string,
) bool {
	return accountID != snowflake.ID(0) && playerCharacterID != snowflake.ID(0) && expectedVersion >= 1 &&
		idempotencyKey != "" && len(idempotencyKey) <= 128 && requestID != ""
}
