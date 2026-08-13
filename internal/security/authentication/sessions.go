package authentication

import (
	"context"
	"fmt"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

const ownSessionQueryTimeout = 2 * time.Second

// SessionFamily 是账号可以查看和撤销的一个持久登录设备会话。
type SessionFamily struct {
	FamilyID       snowflake.ID
	Current        bool
	DeviceSummary  string
	CreatedAt      time.Time
	LastActivityAt time.Time
	ExpiresAt      time.Time
	IdleExpiresAt  time.Time
}

// SessionQueryStore 读取账号当前仍然有效的会话族。
type SessionQueryStore interface {
	ListActiveSessionFamilies(context.Context, snowflake.ID, time.Time) ([]SessionFamily, error)
}

// SessionRevocationAudit 是成功撤销自有会话时同事务保存的安全审计事实。
type SessionRevocationAudit struct {
	ID         snowflake.ID
	AccountID  snowflake.ID
	FamilyID   snowflake.ID
	RequestID  string
	OccurredAt time.Time
}

// SessionRevocationWriter 是一次自有会话撤销事务内使用的写入边界。
type SessionRevocationWriter interface {
	RevokeOwnedSessionFamily(context.Context, snowflake.ID, snowflake.ID, time.Time) (bool, error)
	RecordSessionRevocation(context.Context, SessionRevocationAudit) error
}

// SessionRevocationTransactions 为应用服务提供显式的会话撤销事务。
type SessionRevocationTransactions interface {
	WithinSessionRevocation(context.Context, func(SessionRevocationWriter) error) error
}

// SessionManager 查询和撤销已认证账号自己的持久会话。
type SessionManager struct {
	query        SessionQueryStore
	transactions SessionRevocationTransactions
	newID        snowflake.Source
	now          func() time.Time
}

// NewSessionManager 使用显式查询、事务、Snowflake Identifier 生成器和时钟创建会话管理服务。
func NewSessionManager(
	query SessionQueryStore,
	transactions SessionRevocationTransactions,
	newID snowflake.Source,
	now func() time.Time,
) *SessionManager {
	return &SessionManager{query: query, transactions: transactions, newID: newID, now: now}
}

// List 返回已认证账号当前仍有效的会话族，并标识发起请求的当前会话族。
func (m *SessionManager) List(ctx context.Context, principal Principal) ([]SessionFamily, error) {
	if principal.AccountID == snowflake.ID(0) || principal.SessionFamilyID == snowflake.ID(0) {
		return nil, ErrInvalidSession
	}
	queryContext, cancel := context.WithTimeout(ctx, ownSessionQueryTimeout)
	defer cancel()
	families, err := m.query.ListActiveSessionFamilies(queryContext, principal.AccountID, m.now().UTC())
	if err != nil {
		return nil, fmt.Errorf("读取自有会话: %w", err)
	}
	for index := range families {
		families[index].Current = families[index].FamilyID == principal.SessionFamilyID
	}
	return families, nil
}

// Revoke 幂等撤销已认证账号拥有的指定会话族；不存在或不属于账号时不泄露对象存在性。
func (m *SessionManager) Revoke(
	ctx context.Context,
	principal Principal,
	familyID snowflake.ID,
	requestID string,
) error {
	if principal.AccountID == snowflake.ID(0) || familyID == snowflake.ID(0) {
		return ErrInvalidSession
	}
	now := m.now().UTC()
	transactionContext, cancel := context.WithTimeout(ctx, ownSessionQueryTimeout)
	defer cancel()
	if err := m.transactions.WithinSessionRevocation(transactionContext, func(writer SessionRevocationWriter) error {
		revoked, err := writer.RevokeOwnedSessionFamily(transactionContext, principal.AccountID, familyID, now)
		if err != nil {
			return fmt.Errorf("撤销自有会话族: %w", err)
		}
		if !revoked {
			return nil
		}
		auditID, nextErr := m.newID.Next(transactionContext)
		if nextErr != nil {
			return fmt.Errorf("生成会话撤销审计标识: %w", nextErr)
		}
		if err := writer.RecordSessionRevocation(transactionContext, SessionRevocationAudit{
			ID: auditID, AccountID: principal.AccountID, FamilyID: familyID,
			RequestID: requestID, OccurredAt: now,
		}); err != nil {
			return fmt.Errorf("记录自有会话撤销审计: %w", err)
		}
		return nil
	}); err != nil {
		return fmt.Errorf("提交自有会话撤销: %w", err)
	}
	return nil
}
