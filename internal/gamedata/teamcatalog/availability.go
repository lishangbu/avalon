package teamcatalog

import (
	"context"

	"github.com/lishangbu/avalon/internal/team"
)

// AvailableTransaction 是在同一 PostgreSQL 事务中执行 Team 校验和写入的技术边界。
type AvailableTransaction interface {
	// WithinTransaction 向 work 传播同一个 SQL 与 Ent 事务 Context。
	WithinTransaction(context.Context, func(context.Context) error) error
}

// AvailabilityGate 让 Team 校验和写入共享同一个数据库事务 Context。
type AvailabilityGate struct {
	// transactions 在同一事务内执行 Team 校验和写入。
	transactions AvailableTransaction
}

// NewAvailabilityGate 使用显式事务 adapter 创建 Team 写入边界。
func NewAvailabilityGate(transactions AvailableTransaction) *AvailabilityGate {
	return &AvailabilityGate{transactions: transactions}
}

// WithinAvailable 在同一事务中执行 Team 校验和写入。
func (gate *AvailabilityGate) WithinAvailable(ctx context.Context, work func(context.Context) error) error {
	if gate == nil || gate.transactions == nil || work == nil {
		return team.ErrTeamCatalogUnavailable
	}
	if state, ok := gate.transactions.(interface{ InTransaction(context.Context) bool }); ok && state.InTransaction(ctx) {
		return work(ctx)
	}
	return gate.transactions.WithinTransaction(ctx, work)
}
