package team

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"fmt"
	"regexp"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	// TeamShareSchemaVersion 是当前服务端生成并可导入的 Team 分享快照版本。
	TeamShareSchemaVersion = 1
	defaultShareLifetime   = 7 * 24 * time.Hour
	maximumShareLifetime   = 30 * 24 * time.Hour
	minimumShareLifetime   = time.Minute
)

var (
	// ErrTeamShareNotFound 对不存在、已撤销、已过期和格式非法的分享码返回同一语义。
	ErrTeamShareNotFound = errors.New("Team 分享不存在")
	// ErrTeamShareConflict 表示分享版本、来源 Team 版本或分享码摘要冲突。
	ErrTeamShareConflict = errors.New("Team 分享状态或版本冲突")
	// ErrTeamShareCodeCollision 表示新生成的随机分享码摘要极小概率重复，可安全重新生成。
	ErrTeamShareCodeCollision = errors.New("Team 分享码冲突")
)

var shareCodePattern = regexp.MustCompile(`^[A-Za-z0-9_-]{43}$`)

// ShareSnapshot 是与来源 Team 后续变更隔离的不可变完整阵容事实。
type ShareSnapshot struct {
	// SchemaVersion 是冻结快照的服务端编码版本。
	SchemaVersion int `json:"schemaVersion"`
	// Name 是生成快照时来源 Team 的规范化展示名称。
	Name string `json:"name"`
	// Members 是生成快照时按固定位置冻结的完整阵容。
	Members []Member `json:"members"`
}

// Share 是 Team 分享的生命周期与冻结来源元数据，不包含分享码明文。
type Share struct {
	// ID 是不可变 Team 分享记录的稳定 Identifier。
	ID snowflake.ID
	// SourceTeamID 是生成快照时来源 Team 的稳定 Identifier，不形成导入后的持续关联。
	SourceTeamID snowflake.ID
	// OwnerPlayerCharacterID 是创建并可撤销该分享的 PlayerCharacter 稳定 Identifier。
	OwnerPlayerCharacterID snowflake.ID
	// SourceTeamVersion 是生成分享时来源 Team 的精确乐观版本。
	SourceTeamVersion int64
	// SchemaVersion 是冻结快照使用的 Team 分享结构版本。
	SchemaVersion int
	// Version 是唯一允许撤销生命周期转换使用的乐观版本。
	Version int64
	// ExpiresAt 是分享码不再允许解析或首次导入的 UTC 时间。
	ExpiresAt time.Time
	// RevokedAt 是拥有者永久撤销分享的 UTC 时间；空值表示尚未撤销。
	RevokedAt *time.Time
	// CreatedAt 是不可变分享快照生成的 UTC 时间。
	CreatedAt time.Time
	// UpdatedAt 是唯一允许撤销生命周期转换完成的 UTC 时间。
	UpdatedAt time.Time
}

// CreateShareCommand 冻结调用角色拥有的精确 Team 版本。
type CreateShareCommand struct {
	// AccountID 是发起分享创建的已认证账号稳定 Identifier。
	AccountID snowflake.ID
	// PlayerCharacterID 是拥有来源 Team 的稳定 Identifier。
	PlayerCharacterID snowflake.ID
	// TeamID 是将被冻结为分享快照的来源 Team 稳定 Identifier。
	TeamID snowflake.ID
	// ExpectedVersion 是客户端读取后携带的来源 Team 乐观版本。
	ExpectedVersion int64
	// ExpiresAt 是客户端可选指定的分享过期 UTC 时间。
	ExpiresAt *time.Time
	// IdempotencyKey 是本次分享创建的稳定幂等键。
	IdempotencyKey string
	// RequestID 是贯穿日志和审计的请求关联标识。
	RequestID string
}

// CreateShareRecord 是 Repository 冻结快照、保存码摘要、审计和幂等结果所需的事实。
type CreateShareRecord struct {
	// ShareID 是服务端为新分享预生成的稳定 Identifier。
	ShareID snowflake.ID
	// AccountID 是执行分享创建的账号稳定 Identifier。
	AccountID snowflake.ID
	// PlayerCharacterID 是拥有来源 Team 的稳定 Identifier。
	PlayerCharacterID snowflake.ID
	// TeamID 是将被冻结为分享快照的来源 Team 稳定 Identifier。
	TeamID snowflake.ID
	// ExpectedVersion 是用于锁定来源 Team 的乐观版本。
	ExpectedVersion int64
	// Code 是仅在本次首次创建响应中返回的随机分享码明文，禁止持久化。
	Code string
	// ExpiresAt 是分享码失效的 UTC 时间。
	ExpiresAt time.Time
	// ExpiryDigest 是将可选过期时间纳入幂等请求摘要的规范化文本。
	ExpiryDigest string
	// CreatedAt 是本次分享创建的服务端 UTC 时间。
	CreatedAt time.Time
	// IdempotencyKey 是与请求载荷绑定的稳定幂等键。
	IdempotencyKey string
	// RequestID 是写入审计记录的请求关联标识。
	RequestID string
}

// CreateShareResult 返回分享生命周期元数据；Code 只在首次成功创建时包含明文分享码。
//
// 幂等响应持久化时不会保存 Code，以保证 PostgreSQL 只保存分享码的 SHA-256 摘要；同键重放因此返回空 Code。
type CreateShareResult struct {
	// Share 是创建或重放后返回的冻结分享生命周期元数据。
	Share Share
	// Code 只在首次成功创建时包含明文分享码；幂等重放必须为空。
	Code string
}

// RevokeShareCommand 使用分享版本撤销调用角色拥有的分享。
type RevokeShareCommand struct {
	// AccountID 是发起分享撤销的已认证账号稳定 Identifier。
	AccountID snowflake.ID
	// PlayerCharacterID 是拥有分享的稳定 Identifier。
	PlayerCharacterID snowflake.ID
	// ShareID 是待永久撤销的分享稳定 Identifier。
	ShareID snowflake.ID
	// ExpectedVersion 是客户端读取后携带的分享乐观版本。
	ExpectedVersion int64
	// IdempotencyKey 是本次撤销的稳定幂等键。
	IdempotencyKey string
	// RequestID 是贯穿日志和审计的请求关联标识。
	RequestID string
}

// RevokeShareRecord 是 Repository 撤销分享所需的完整事实。
type RevokeShareRecord struct {
	RevokeShareCommand
	// RevokedAt 是服务端确认永久撤销的 UTC 时间。
	RevokedAt time.Time
}

// ImportShareCommand 使用显式新名称把有效分享导入目标角色为独立 Team。
type ImportShareCommand struct {
	// AccountID 是发起导入的已认证账号稳定 Identifier。
	AccountID snowflake.ID
	// PlayerCharacterID 是将拥有独立导入 Team 的稳定 Identifier。
	PlayerCharacterID snowflake.ID
	// Code 是客户端提交的明文分享码，只用于计算查询摘要。
	Code string
	// Name 是导入后独立 Team 的新展示名称。
	Name string
	// IdempotencyKey 是本次导入的稳定幂等键。
	IdempotencyKey string
	// RequestID 是贯穿日志和审计的请求关联标识。
	RequestID string
}

// ImportShareRecord 是 Repository 在幂等认领后解析快照并创建独立 Team 所需的事实。
type ImportShareRecord struct {
	// Team 是导入后将独立保存的 Team 初始元数据。
	Team Team
	// AccountID 是执行导入操作的账号稳定 Identifier。
	AccountID snowflake.ID
	// CodeDigest 是仅用于定位分享记录的 SHA-256 摘要。
	CodeDigest []byte
	// IdempotencyKey 是与导入请求载荷绑定的稳定幂等键。
	IdempotencyKey string
	// RequestID 是写入审计记录的请求关联标识。
	RequestID string
	// ImportedAt 是服务端首次尝试导入的 UTC 时间。
	ImportedAt time.Time
	// currentMemberValidator 只能由 Team 应用服务在已锁定可用 Current Game Data 的事务内注入；持久化
	// adapter 通过公开方法调用它，外部调用方不能以空操作回调绕过首次导入校验。
	currentMemberValidator CurrentMemberValidator
}

// ValidateCurrentSnapshot 在存储事务确认本次是首次导入后，按当前实时资料校验冻结成员。
//
// 校验器只能由 ShareService 注入；没有受信任校验器的记录会被拒绝，避免导出的 Repository 被任意回调绕过。
func (record ImportShareRecord) ValidateCurrentSnapshot(ctx context.Context, members []Member) error {
	if !record.HasCurrentGameDataValidator() {
		return ErrTeamCatalogUnavailable
	}
	return record.currentMemberValidator.ValidateCurrent(ctx, members)
}

// HasCurrentGameDataValidator 表示首次导入携带只能由 ShareService 注入的可信当前资料校验器。
//
// 返回值只供持久化 adapter 在访问数据库前拒绝绕过应用服务的导入事实；它不表示校验已经执行，外部调用方
// 也无法设置其私有依赖。
func (record ImportShareRecord) HasCurrentGameDataValidator() bool {
	return !dependencyIsNil(record.currentMemberValidator)
}

// ShareReader 返回仍有效分享的冻结快照。
type ShareReader interface {
	ResolveShare(context.Context, []byte, time.Time) (ShareSnapshot, error)
}

// ShareRepository 是分享冻结、撤销和独立导入的关系型写入端口。
type ShareRepository interface {
	CreateShare(context.Context, CreateShareRecord) (CreateShareResult, error)
	RevokeShare(context.Context, RevokeShareRecord) (Share, error)
	ImportShare(context.Context, ImportShareRecord) (Team, error)
}

// ShareService 编排不可猜测、可到期、可撤销的 Team 分享。
type ShareService struct {
	// reader 读取仍有效分享的冻结快照。
	reader ShareReader
	// repository 是分享快照、生命周期、导入、审计和幂等结果的唯一关系型持久化端口。
	repository ShareRepository
	// validator 在每次首次导入时把冻结成员重新约束到当前实时资料。
	validator CurrentMemberValidator
	// currentGameData 在首次导入时传播与 Team 写入共用的事务 Context。
	currentGameData CurrentGameDataGate
	// transactions 用于创建和撤销分享时，将快照或生命周期与幂等结果原子提交。
	transactions TransactionRunner
	// newID 为不可变分享和导入后的独立 Team 生成稳定 Identifier。
	newID snowflake.Source
	// newCode 生成仅能在首次成功创建响应中返回的高熵分享码。
	newCode func() (string, error)
	// now 提供可替换的 UTC 时间源，用于到期判断与可复现的领域测试。
	now func() time.Time
}

// NewShareService 使用显式 Repository、当前实时资料校验器、Identifier、随机码和时钟依赖创建分享服务。
func NewShareService(
	reader ShareReader,
	repository ShareRepository, validator CurrentMemberValidator,
	currentGameData CurrentGameDataGate,
	newID snowflake.Source,
	newCode func() (string, error),
	now func() time.Time,
	transactions TransactionRunner,
) *ShareService {
	if dependencyIsNil(validator) {
		panic("team: CurrentMemberValidator 不能为空")
	}
	if dependencyIsNil(currentGameData) {
		panic("team: CurrentGameDataGate 不能为空")
	}
	return &ShareService{
		reader: reader, repository: repository, validator: validator, currentGameData: currentGameData,
		transactions: transactions, newID: newID, newCode: newCode, now: now,
	}
}

// NewShareCode 生成 256 位熵且不含路径保留字符的 URL-safe 分享码。
func NewShareCode() (string, error) {
	bytes := make([]byte, 32)
	if _, err := rand.Read(bytes); err != nil {
		return "", fmt.Errorf("读取 Team 分享随机源: %w", err)
	}
	return base64.RawURLEncoding.EncodeToString(bytes), nil
}

// Create 冻结精确 Team 版本；随机码碰撞时最多重新生成四次。
func (s *ShareService) Create(ctx context.Context, command CreateShareCommand) (CreateShareResult, error) {
	command.IdempotencyKey = strings.TrimSpace(command.IdempotencyKey)
	command.RequestID = strings.TrimSpace(command.RequestID)
	now := s.now().UTC()
	expiresAt := now.Add(defaultShareLifetime)
	expiryDigest := "default"
	if command.ExpiresAt != nil {
		expiresAt = command.ExpiresAt.UTC()
		expiryDigest = expiresAt.Format(time.RFC3339Nano)
	}
	if !validOwnedTeamCommand(
		command.AccountID, command.PlayerCharacterID, command.TeamID,
		command.ExpectedVersion, command.IdempotencyKey, command.RequestID,
	) || expiresAt.Before(now.Add(minimumShareLifetime)) || expiresAt.After(now.Add(maximumShareLifetime)) {
		return CreateShareResult{}, ErrInvalidTeam
	}
	shareID, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return CreateShareResult{}, idErr
	}
	for range 4 {
		code, codeErr := s.newCode()
		if codeErr != nil {
			return CreateShareResult{}, codeErr
		}
		if !shareCodePattern.MatchString(code) {
			return CreateShareResult{}, ErrInvalidTeam
		}
		var result CreateShareResult
		createErr := withinTransaction(ctx, s.transactions, func(transactionContext context.Context) error {
			var operationErr error
			result, operationErr = s.repository.CreateShare(transactionContext, CreateShareRecord{
				ShareID: shareID, AccountID: command.AccountID, PlayerCharacterID: command.PlayerCharacterID,
				TeamID: command.TeamID, ExpectedVersion: command.ExpectedVersion, Code: code,
				ExpiresAt: expiresAt, ExpiryDigest: expiryDigest, CreatedAt: now,
				IdempotencyKey: command.IdempotencyKey, RequestID: command.RequestID,
			})
			return operationErr
		})
		if !errors.Is(createErr, ErrTeamShareCodeCollision) {
			return result, createErr
		}
	}
	return CreateShareResult{}, ErrTeamShareCodeCollision
}

// Resolve 返回仍有效分享的冻结快照，并对非法码保持与不存在相同的响应。
func (s *ShareService) Resolve(ctx context.Context, code string) (ShareSnapshot, error) {
	digest, valid := ShareCodeDigest(code)
	if !valid {
		return ShareSnapshot{}, ErrTeamShareNotFound
	}
	return s.reader.ResolveShare(ctx, digest, s.now().UTC())
}

// Revoke 以乐观版本永久撤销分享。
func (s *ShareService) Revoke(ctx context.Context, command RevokeShareCommand) (Share, error) {
	command.IdempotencyKey = strings.TrimSpace(command.IdempotencyKey)
	command.RequestID = strings.TrimSpace(command.RequestID)
	if command.ShareID == snowflake.ID(0) || command.AccountID == snowflake.ID(0) || command.PlayerCharacterID == snowflake.ID(0) ||
		command.ExpectedVersion < 1 || !idempotency.ValidKey(command.IdempotencyKey) || command.RequestID == "" {
		return Share{}, ErrInvalidTeam
	}
	var revoked Share
	err := withinTransaction(ctx, s.transactions, func(transactionContext context.Context) error {
		var operationErr error
		revoked, operationErr = s.repository.RevokeShare(
			transactionContext, RevokeShareRecord{RevokeShareCommand: command, RevokedAt: s.now().UTC()},
		)
		return operationErr
	})
	return revoked, err
}

// Import 在目标角色上创建与来源后续变化完全隔离的新 Team。
func (s *ShareService) Import(ctx context.Context, command ImportShareCommand) (Team, error) {
	name, nameKey, validName := normalizeName(command.Name)
	digest, validCode := ShareCodeDigest(command.Code)
	command.IdempotencyKey = strings.TrimSpace(command.IdempotencyKey)
	command.RequestID = strings.TrimSpace(command.RequestID)
	if !validName || !validCode || command.AccountID == snowflake.ID(0) || command.PlayerCharacterID == snowflake.ID(0) ||
		!idempotency.ValidKey(command.IdempotencyKey) || command.RequestID == "" {
		return Team{}, ErrInvalidTeam
	}
	teamID, idErr := s.newID.Next(ctx)
	if idErr != nil {
		return Team{}, idErr
	}
	var imported Team
	err := s.currentGameData.WithinAvailable(ctx, func(transactionContext context.Context) error {
		// 导入时刻必须在取得 Current Game Data 可用行锁后读取；等待维护窗口释放期间，分享可能已经到期。
		now := s.now().UTC()
		var operationErr error
		imported, operationErr = s.repository.ImportShare(transactionContext, ImportShareRecord{
			Team: Team{
				ID: teamID, PlayerCharacterID: command.PlayerCharacterID, Name: name, NameKey: nameKey,
				Version: 1, CreatedAt: now, UpdatedAt: now,
			},
			AccountID: command.AccountID, CodeDigest: digest,
			IdempotencyKey: command.IdempotencyKey, RequestID: command.RequestID, ImportedAt: now,
			currentMemberValidator: s.validator,
		})
		return operationErr
	})
	return imported, err
}

// ShareCodeDigest 校验分享码形状并返回只用于数据库查找的 SHA-256 摘要。
func ShareCodeDigest(code string) ([]byte, bool) {
	if !shareCodePattern.MatchString(code) {
		return nil, false
	}
	digest := sha256.Sum256([]byte(code))
	return digest[:], true
}
