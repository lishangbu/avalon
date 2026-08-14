// Package database 提供共享 PostgreSQL 连接、Ent Client、事务上下文和审计所需的
// 最小 SQL 执行边界。业务 Repository 使用 Ent Builder；SQL 接口仅保留给审计哈希链等
// 必须依赖 PostgreSQL 锁和动态表名的技术基础设施。
package database

import (
	"context"
	"database/sql"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/internal/platform/persistence"
)

// Transaction 是审计哈希链等技术基础设施使用的最小事务内 SQL 形状。
type Transaction interface {
	Exec(context.Context, string, ...any) (pgconn.CommandTag, error)
	Query(context.Context, string, ...any) (pgx.Rows, error)
	QueryRow(context.Context, string, ...any) pgx.Row
}

type transactionContextKey struct{}

// Executor 返回当前应用事务绑定的 SQL 执行器；没有事务时回落到调用方提供的连接池。
func Executor(ctx context.Context, fallback Transaction) Transaction {
	if tx, ok := ctx.Value(transactionContextKey{}).(Transaction); ok {
		return tx
	}
	return fallback
}

// Pool 是 persistence.Database 的应用连接边界，不拥有独立连接资源。
type Pool struct {
	database *persistence.Database
}

// Config 保存应用数据库唯一连接池的显式配置。
type Config persistence.Config

// Open 创建由 Ent 和少量技术 SQL 操作共享的唯一连接池。
func Open(config Config) (*Pool, error) {
	database, err := persistence.Open(persistence.Config(config))
	if err != nil {
		return nil, err
	}
	return &Pool{database: database}, nil
}

// Persistence 返回应用启动、Schema 管理和关系型持久化适配器使用的统一边界。
func (pool *Pool) Persistence() *persistence.Database {
	return pool.database
}

// Client 返回当前 Context 绑定的 Ent Client；所有业务持久化都通过该入口访问实体。
func (pool *Pool) Client(ctx context.Context) *avalonent.Client {
	return pool.database.Client(ctx)
}

// Ready 确认共享 PostgreSQL 连接池能够执行查询。
func (pool *Pool) Ready(ctx context.Context) error {
	var value int
	return pool.database.SQL(ctx).QueryRowContext(ctx, "SELECT 1").Scan(&value)
}

// Exec 通过共享连接池或当前事务执行技术基础设施 SQL。
func (pool *Pool) Exec(ctx context.Context, statement string, arguments ...any) (pgconn.CommandTag, error) {
	return exec(ctx, pool.database.SQL(ctx), statement, arguments...)
}

// Query 通过共享连接池或当前事务执行多行查询。
func (pool *Pool) Query(ctx context.Context, statement string, arguments ...any) (pgx.Rows, error) {
	return query(ctx, pool.database.SQL(ctx), statement, arguments...)
}

// QueryRow 通过共享连接池或当前事务执行单行查询。
func (pool *Pool) QueryRow(ctx context.Context, statement string, arguments ...any) pgx.Row {
	return sqlRow{row: pool.database.SQL(ctx).QueryRowContext(ctx, statement, arguments...)}
}

// WithTx 在统一事务中执行技术基础设施回调；新业务代码应使用 WithinTransaction。
func (pool *Pool) WithTx(ctx context.Context, operation func(Transaction) error) error {
	return pool.database.WithinTransaction(ctx, persistence.TransactionOptions{Isolation: persistence.IsolationReadCommitted}, func(transactionCtx context.Context) error {
		return operation(sqlTransaction{database: pool.database, ctx: transactionCtx})
	})
}

// WithinTransaction 通过 Context 向回调传播同一个 SQL 事务和 Ent Client。
func (pool *Pool) WithinTransaction(ctx context.Context, operation func(context.Context) error) error {
	if persistence.InTransaction(ctx) {
		return operation(ctx)
	}
	return pool.database.WithinTransaction(ctx, persistence.TransactionOptions{Isolation: persistence.IsolationReadCommitted}, func(transactionCtx context.Context) error {
		adapter := sqlTransaction{database: pool.database, ctx: transactionCtx}
		return operation(context.WithValue(transactionCtx, transactionContextKey{}, Transaction(adapter)))
	})
}

// InTransaction 报告 Context 是否已经绑定共享 PostgreSQL 事务。
func (pool *Pool) InTransaction(ctx context.Context) bool {
	return persistence.InTransaction(ctx)
}

// Close 关闭唯一 Ent Client 及其底层 database/sql 连接池。
func (pool *Pool) Close() {
	_ = pool.database.Close()
}

type sqlTransaction struct {
	database *persistence.Database
	ctx      context.Context
}

func (transaction sqlTransaction) Exec(_ context.Context, statement string, arguments ...any) (pgconn.CommandTag, error) {
	return exec(transaction.ctx, transaction.database.SQL(transaction.ctx), statement, arguments...)
}

func (transaction sqlTransaction) Query(_ context.Context, statement string, arguments ...any) (pgx.Rows, error) {
	return query(transaction.ctx, transaction.database.SQL(transaction.ctx), statement, arguments...)
}

func (transaction sqlTransaction) QueryRow(_ context.Context, statement string, arguments ...any) pgx.Row {
	return sqlRow{row: transaction.database.SQL(transaction.ctx).QueryRowContext(transaction.ctx, statement, arguments...)}
}

func exec(ctx context.Context, executor persistence.SQLExecutor, statement string, arguments ...any) (pgconn.CommandTag, error) {
	result, err := executor.ExecContext(ctx, statement, arguments...)
	if err != nil {
		return pgconn.CommandTag{}, err
	}
	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return pgconn.CommandTag{}, err
	}
	return pgconn.NewCommandTag(fmt.Sprintf("OK %d", rowsAffected)), nil
}

func query(ctx context.Context, executor persistence.SQLExecutor, statement string, arguments ...any) (pgx.Rows, error) {
	rows, err := executor.QueryContext(ctx, statement, arguments...)
	if err != nil {
		return nil, err
	}
	return &sqlRows{rows: rows}, nil
}

type sqlRow struct {
	row *sql.Row
}

func (row sqlRow) Scan(destinations ...any) error {
	err := row.row.Scan(destinations...)
	if err == sql.ErrNoRows {
		return pgx.ErrNoRows
	}
	return err
}

type sqlRows struct {
	rows   *sql.Rows
	closed bool
}

func (rows *sqlRows) Close() {
	rows.closed = true
	_ = rows.rows.Close()
}

func (rows *sqlRows) Err() error { return rows.rows.Err() }

func (rows *sqlRows) CommandTag() pgconn.CommandTag { return pgconn.CommandTag{} }

func (rows *sqlRows) FieldDescriptions() []pgconn.FieldDescription { return nil }

func (rows *sqlRows) Next() bool {
	next := rows.rows.Next()
	if !next {
		rows.closed = true
	}
	return next
}

func (rows *sqlRows) Scan(destinations ...any) error { return rows.rows.Scan(destinations...) }

func (rows *sqlRows) Values() ([]any, error) {
	columns, err := rows.rows.Columns()
	if err != nil {
		return nil, err
	}
	values := make([]any, len(columns))
	destinations := make([]any, len(columns))
	for index := range values {
		destinations[index] = &values[index]
	}
	if err := rows.rows.Scan(destinations...); err != nil {
		return nil, err
	}
	return values, nil
}

func (rows *sqlRows) RawValues() [][]byte { return nil }

func (rows *sqlRows) Conn() *pgx.Conn { return nil }
