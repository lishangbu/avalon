// Package authentication 编排账号登录与可撤销 Bearer 会话。
package authentication

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/security/account"
	"github.com/lishangbu/avalon/internal/security/session"
)

// ErrInvalidCredentials 是登录身份不存在、凭据错误或账号不可用的统一外部错误。
var ErrInvalidCredentials = errors.New("登录凭据无效")

// ErrLoginAccountNotFound 是持久化适配器向应用层报告的账号不存在错误。
var ErrLoginAccountNotFound = errors.New("登录账号不存在")

// LoginFailureReason 是内部安全审计使用的稳定登录失败原因码。
type LoginFailureReason string

const (
	// LoginFailureInvalidUsername 表示用户名未通过格式校验。
	LoginFailureInvalidUsername LoginFailureReason = "invalid_username"
	// LoginFailureAccountNotFound 表示规范化用户名不存在。
	LoginFailureAccountNotFound LoginFailureReason = "account_not_found"
	// LoginFailureInvalidPassword 表示密码摘要不匹配。
	LoginFailureInvalidPassword LoginFailureReason = "invalid_password"
	// LoginFailureAccountUnavailable 表示账号状态不允许登录。
	LoginFailureAccountUnavailable LoginFailureReason = "account_unavailable"
	// LoginFailureAccountLocked 表示账号仍处于有时限锁定中。
	LoginFailureAccountLocked LoginFailureReason = "account_locked"
)

// LoginProtectionPolicy 定义连续失败开始锁定及锁定时长的渐进规则。
type LoginProtectionPolicy struct {
	LockThreshold int32
	BaseLock      time.Duration
	MaximumLock   time.Duration
}

// LoginFailureRecord 是 Repository 原子递增失败计数并写审计所需的安全事实。
type LoginFailureRecord struct {
	// LoginAttemptID 是支持持久登录尝试的安全域使用的失败尝试 Identifier。
	LoginAttemptID snowflake.ID
	// AuditID 是登录失败安全审计事实的独立 Identifier。
	AuditID        snowflake.ID
	AccountID      snowflake.ID
	UsernameDigest string
	Reason         LoginFailureReason
	Policy         LoginProtectionPolicy
	RequestID      string
	OccurredAt     time.Time
}

// LoginProtectionState 是账号当前连续失败、锁定时间与会话失效版本的原子快照。
type LoginProtectionState struct {
	Status          account.Status
	SecurityVersion int64
	FailedAttempts  int32
	LockedUntil     *time.Time
}

// AfterFailure 计算一次登录失败后的领域状态；不可登录的账号和未到期锁定保持不变。
func (s LoginProtectionState) AfterFailure(
	policy LoginProtectionPolicy,
	occurredAt time.Time,
) (LoginProtectionState, bool) {
	canAdvance := s.Status == account.StatusActive ||
		(s.Status == account.StatusLocked && s.LockedUntil != nil && !occurredAt.Before(*s.LockedUntil))
	if !canAdvance {
		return s, false
	}
	next := s
	next.FailedAttempts++
	lockDuration := policy.LockDuration(next.FailedAttempts)
	if lockDuration == 0 {
		next.LockedUntil = nil
		return next, true
	}
	if next.Status != account.StatusLocked {
		next.SecurityVersion++
	}
	next.Status = account.StatusLocked
	lockedUntil := occurredAt.Add(lockDuration)
	next.LockedUntil = &lockedUntil
	return next, true
}

// AuthenticationQuery 返回登录凭据校验所需的账号投影。
type AuthenticationQuery interface {
	FindLoginAccount(context.Context, string) (LoginAccount, error)
}

// AuthenticationRepository 提供登录失败与登录成功事实的 PostgreSQL 事务写入。
type AuthenticationRepository interface {
	RecordLoginFailure(context.Context, LoginFailureRecord) error
	CommitLoginSuccess(context.Context, LoginSuccessRecord) (int64, error)
}

// LoginSessionStore 管理登录建立期间的 pending 会话及其激活或撤销。
type LoginSessionStore interface {
	StageSession(context.Context, SessionRecord) error
	ActivateSession(context.Context, []byte, int64) error
	AbortSession(context.Context, []byte) error
}

// LoginSessionStage 标识登录会话建立失败所在的稳定阶段。
type LoginSessionStage string

const (
	// LoginSessionStageStage 表示 pending 会话暂存阶段。
	LoginSessionStageStage LoginSessionStage = "stage"
	// LoginSessionStageCommit 表示 PostgreSQL 登录事实提交阶段。
	LoginSessionStageCommit LoginSessionStage = "commit"
	// LoginSessionStageActivate 表示 Valkey 会话激活阶段。
	LoginSessionStageActivate LoginSessionStage = "activate"
	// LoginSessionStageAbort 表示失败会话补偿撤销阶段。
	LoginSessionStageAbort LoginSessionStage = "abort"
)

// LoginSessionTelemetry 记录登录会话建立阶段的结构化故障事件与指标。
type LoginSessionTelemetry interface {
	RecordFailure(context.Context, LoginSessionStage, string, error)
}

// LoginAccount 是验证登录凭据所需的最小账号投影。
type LoginAccount struct {
	ID                  snowflake.ID
	PasswordHash        string
	Status              account.Status
	SecurityVersion     int64
	FailedLoginAttempts int32
	LockedUntil         *time.Time
}

// SessionRecord 是 refresh token 对应 Valkey 会话的不可逆持久化表示。
type SessionRecord struct {
	ID                 snowflake.ID
	FamilyID           snowflake.ID
	AccountID          snowflake.ID
	SessionTokenDigest []byte
	DeviceSummary      string
	SecurityVersion    int64
	ExpiresAt          time.Time
	IdleExpiresAt      time.Time
	LastActivityAt     time.Time
	CreatedAt          time.Time
}

// LoginSuccessRecord 是 Repository 原子提交账号保护状态、安全版本和成功审计所需的事实。
type LoginSuccessRecord struct {
	// LoginAttemptID 是支持持久登录尝试的安全域使用的独立 Identifier。
	LoginAttemptID snowflake.ID
	// AuditID 是成功登录安全审计事实的独立 Identifier。
	AuditID snowflake.ID
	// Session 是成功登录关联的会话身份与有效期事实。
	Session SessionRecord
	// UsernameDigest 是规范化登录名的 SHA-256，不向 Repository 传递登录名明文。
	UsernameDigest []byte
	// RequestID 关联登录请求、成功尝试和安全审计。
	RequestID string
	// ExpectedPasswordHash 防止凭据校验与提交之间的密码变更被忽略。
	ExpectedPasswordHash string
}

// SessionPolicy 定义服务端会话的绝对与空闲有效期。
type SessionPolicy struct {
	AbsoluteTTL time.Duration
	IdleTTL     time.Duration
}

// LoginCommand 包含一次登录尝试的凭据和非敏感设备摘要。
type LoginCommand struct {
	Username      string
	Password      string
	RequestID     string
	DeviceSummary string
}

// LoginResult 只在登录成功传输边界返回一次 refresh 凭证明文。
type LoginResult struct {
	SessionID snowflake.ID
	// SessionFamilyID 是用于撤销全部轮换 refresh token 的稳定设备会话族标识。
	SessionFamilyID snowflake.ID
	// AccountID 是签发短期 access token 使用的管理员账号标识。
	AccountID snowflake.ID
	// SecurityVersion 是登录时冻结的账号安全版本。
	SecurityVersion int64
	SessionToken    string
	ExpiresAt       time.Time
}

// Service 验证登录凭据并签发可撤销 opaque token 会话。
type Service struct {
	// query 读取登录凭据校验所需的账号投影。
	query         AuthenticationQuery
	repository    AuthenticationRepository
	sessions      LoginSessionStore
	telemetry     LoginSessionTelemetry
	passwords     *account.PasswordHasher
	sessionTokens *session.TokenIssuer
	policy        SessionPolicy
	protection    LoginProtectionPolicy
	newID         snowflake.Source
	now           func() time.Time
}

// NewService 使用显式依赖创建登录服务。
func NewService(
	query AuthenticationQuery,
	repository AuthenticationRepository,
	sessions LoginSessionStore,
	telemetry LoginSessionTelemetry,
	passwords *account.PasswordHasher,
	sessionTokens *session.TokenIssuer,
	policy SessionPolicy,
	protection LoginProtectionPolicy,
	newID snowflake.Source,
	now func() time.Time,
) *Service {
	return &Service{
		query:         query,
		repository:    repository,
		sessions:      sessions,
		telemetry:     telemetry,
		passwords:     passwords,
		sessionTokens: sessionTokens,
		policy:        policy,
		protection:    protection,
		newID:         newID,
		now:           now,
	}
}

// Login 验证账号凭据，并创建一个新的服务端可撤销会话。
func (s *Service) Login(ctx context.Context, command LoginCommand) (LoginResult, error) {
	username, err := account.ParseUsername(command.Username)
	if err != nil {
		s.passwords.VerifyUnknownAccount(command.Password)
		return LoginResult{}, s.rejectLogin(ctx, snowflake.ID(0), command.Username, LoginFailureInvalidUsername, command.RequestID)
	}
	loginAccount, err := s.query.FindLoginAccount(ctx, username.String())
	if errors.Is(err, ErrLoginAccountNotFound) {
		s.passwords.VerifyUnknownAccount(command.Password)
		return LoginResult{}, s.rejectLogin(
			ctx, snowflake.ID(0), username.String(), LoginFailureAccountNotFound, command.RequestID,
		)
	}
	if err != nil {
		return LoginResult{}, fmt.Errorf("读取登录账号: %w", err)
	}
	passwordValid, err := s.passwords.Verify(command.Password, loginAccount.PasswordHash)
	if errors.Is(err, account.ErrInvalidPassword) {
		return LoginResult{}, s.rejectLogin(
			ctx, loginAccount.ID, username.String(), LoginFailureInvalidPassword, command.RequestID,
		)
	}
	if err != nil {
		return LoginResult{}, fmt.Errorf("验证密码摘要: %w", err)
	}
	if loginAccount.Status == account.StatusLocked && loginAccount.LockedUntil != nil &&
		s.now().UTC().Before(*loginAccount.LockedUntil) {
		return LoginResult{}, s.rejectLogin(
			ctx, loginAccount.ID, username.String(), LoginFailureAccountLocked, command.RequestID,
		)
	}
	if loginAccount.Status != account.StatusActive && loginAccount.Status != account.StatusLocked {
		return LoginResult{}, s.rejectLogin(
			ctx, loginAccount.ID, username.String(), LoginFailureAccountUnavailable, command.RequestID,
		)
	}
	if !passwordValid {
		return LoginResult{}, s.rejectLogin(
			ctx, loginAccount.ID, username.String(), LoginFailureInvalidPassword, command.RequestID,
		)
	}
	sessionToken, err := s.sessionTokens.Issue()
	if err != nil {
		return LoginResult{}, fmt.Errorf("签发会话凭证: %w", err)
	}
	sessionID, err := s.nextID(ctx, "会话")
	if err != nil {
		return LoginResult{}, err
	}
	familyID, err := s.nextID(ctx, "会话族")
	if err != nil {
		return LoginResult{}, err
	}
	loginAttemptID, err := s.nextID(ctx, "登录成功记录")
	if err != nil {
		return LoginResult{}, err
	}
	auditID, err := s.nextID(ctx, "登录成功审计")
	if err != nil {
		return LoginResult{}, err
	}
	now := s.now().UTC()
	usernameDigest := sha256.Sum256([]byte(username.String()))
	expiresAt := now.Add(s.policy.AbsoluteTTL)
	idleExpiresAt := minTime(now.Add(s.policy.IdleTTL), expiresAt)
	record := SessionRecord{
		ID:                 sessionID,
		FamilyID:           familyID,
		AccountID:          loginAccount.ID,
		SessionTokenDigest: sessionToken.Digest,
		DeviceSummary:      command.DeviceSummary,
		SecurityVersion:    loginAccount.SecurityVersion,
		ExpiresAt:          expiresAt,
		IdleExpiresAt:      idleExpiresAt,
		LastActivityAt:     now,
		CreatedAt:          now,
	}
	if s.sessions == nil {
		return LoginResult{}, errors.New("登录 Session Store 未配置")
	}
	if err := s.sessions.StageSession(ctx, record); err != nil {
		s.recordSessionFailure(ctx, LoginSessionStageStage, command.RequestID, err)
		return LoginResult{}, s.abortStagedSession(ctx, record.SessionTokenDigest, command.RequestID, fmt.Errorf("暂存登录会话: %w", err))
	}
	securityVersion, err := s.repository.CommitLoginSuccess(ctx, LoginSuccessRecord{
		LoginAttemptID: loginAttemptID, AuditID: auditID, Session: record,
		UsernameDigest: usernameDigest[:], RequestID: command.RequestID, ExpectedPasswordHash: loginAccount.PasswordHash,
	})
	if err != nil {
		s.recordSessionFailure(ctx, LoginSessionStageCommit, command.RequestID, err)
		return LoginResult{}, s.abortStagedSession(ctx, record.SessionTokenDigest, command.RequestID, fmt.Errorf("提交登录事实: %w", err))
	}
	if err := s.sessions.ActivateSession(ctx, record.SessionTokenDigest, securityVersion); err != nil {
		s.recordSessionFailure(ctx, LoginSessionStageActivate, command.RequestID, err)
		return LoginResult{}, s.abortStagedSession(ctx, record.SessionTokenDigest, command.RequestID, fmt.Errorf("激活登录会话: %w", err))
	}
	return LoginResult{
		SessionID: sessionID, SessionFamilyID: familyID, AccountID: loginAccount.ID,
		SecurityVersion: securityVersion, SessionToken: sessionToken.Plaintext, ExpiresAt: record.ExpiresAt,
	}, nil
}

// abortStagedSession 确保失败登录不会留下 pending 或已经激活但未返回客户端的会话。
func (s *Service) abortStagedSession(ctx context.Context, digest []byte, requestID string, cause error) error {
	compensationCtx, cancel := context.WithTimeout(context.WithoutCancel(ctx), 2*time.Second)
	defer cancel()
	if abortErr := s.sessions.AbortSession(compensationCtx, digest); abortErr != nil {
		s.recordSessionFailure(compensationCtx, LoginSessionStageAbort, requestID, abortErr)
		return errors.Join(cause, fmt.Errorf("撤销未完成登录会话: %w", abortErr))
	}
	return cause
}

func (s *Service) recordSessionFailure(ctx context.Context, stage LoginSessionStage, requestID string, cause error) {
	if s.telemetry != nil {
		s.telemetry.RecordFailure(ctx, stage, requestID, cause)
	}
}

func (s *Service) rejectLogin(
	ctx context.Context,
	accountID snowflake.ID,
	username string,
	reason LoginFailureReason,
	requestID string,
) error {
	loginAttemptID, err := s.nextID(ctx, "登录失败记录")
	if err != nil {
		return err
	}
	auditID, err := s.nextID(ctx, "登录失败审计")
	if err != nil {
		return err
	}
	digest := sha256.Sum256([]byte(username))
	if err := s.repository.RecordLoginFailure(ctx, LoginFailureRecord{
		LoginAttemptID: loginAttemptID, AuditID: auditID, AccountID: accountID, UsernameDigest: hex.EncodeToString(digest[:]),
		Reason: reason, Policy: s.protection, RequestID: requestID, OccurredAt: s.now().UTC(),
	}); err != nil {
		return fmt.Errorf("记录登录失败: %w", err)
	}
	return ErrInvalidCredentials
}

func (s *Service) nextID(ctx context.Context, purpose string) (snowflake.ID, error) {
	id, err := s.newID.Next(ctx)
	if err != nil {
		return 0, fmt.Errorf("生成%s标识: %w", purpose, err)
	}
	return id, nil
}

func minTime(left time.Time, right time.Time) time.Time {
	if left.Before(right) {
		return left
	}
	return right
}

// LockDuration 返回指定连续失败次数应施加的锁定时长。
// 达到阈值后按二倍渐进增长，并在配置上限处封顶。
func (p LoginProtectionPolicy) LockDuration(failedAttempts int32) time.Duration {
	if failedAttempts < p.LockThreshold || p.LockThreshold < 1 || p.BaseLock <= 0 || p.MaximumLock < p.BaseLock {
		return 0
	}
	duration := p.BaseLock
	for remaining := failedAttempts - p.LockThreshold; remaining > 0 && duration < p.MaximumLock; remaining-- {
		if duration > p.MaximumLock/2 {
			return p.MaximumLock
		}
		duration *= 2
	}
	return min(duration, p.MaximumLock)
}
