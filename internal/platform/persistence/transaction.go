package persistence

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"math/rand/v2"
	"time"

	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
)

// ErrNestedTransaction 表示应用错误地尝试在已有事务 Context 内再次开启事务。
var ErrNestedTransaction = errors.New("不允许嵌套 PostgreSQL 事务")

// Isolation 定义应用事务允许选择的 PostgreSQL 隔离级别。
type Isolation uint8

const (
	// IsolationReadCommitted 是普通命令默认使用的 PostgreSQL 隔离级别。
	IsolationReadCommitted Isolation = iota + 1
	// IsolationSerializable 仅用于明确声明可安全重放的强竞争命令。
	IsolationSerializable
)

// TransactionOptions 描述事务隔离级别以及序列化冲突时能否安全重放。
type TransactionOptions struct {
	// Isolation 是当前事务明确选择的隔离级别。
	Isolation Isolation
	// Replayable 表示回调不包含网络、对象存储或其它不可逆副作用。
	Replayable bool
}

type transactionContextKey struct{}

type transactionContext struct {
	sqlTx  *sql.Tx
	client *avalonent.Client
}

// WithinTransaction 执行显式事务并把同一个 SQL事务和 Ent Client写入 Context。
func (database *Database) WithinTransaction(
	ctx context.Context,
	options TransactionOptions,
	operation func(context.Context) error,
) error {
	if _, exists := transactionFromContext(ctx); exists {
		return ErrNestedTransaction
	}
	maximumAttempts := 1
	if options.Isolation == IsolationSerializable && options.Replayable {
		maximumAttempts = 3
	}
	for attempt := 1; attempt <= maximumAttempts; attempt++ {
		err := database.withTransactionOnce(ctx, options.Isolation, operation)
		if err == nil || !isSerializationFailure(err) || attempt == maximumAttempts {
			return err
		}
		delay := time.Duration(10+rand.IntN(21)) * time.Millisecond
		timer := time.NewTimer(delay)
		select {
		case <-ctx.Done():
			timer.Stop()
			return context.Cause(ctx)
		case <-timer.C:
		}
	}
	return nil
}

func (database *Database) withTransactionOnce(
	ctx context.Context,
	isolation Isolation,
	operation func(context.Context) error,
) (result error) {
	level, err := sqlIsolationLevel(isolation)
	if err != nil {
		return err
	}
	// 事务生命周期交由 Ent 官方 Tx 管理，业务 Builder 通过 tx.Client() 复用同一连接。
	clientTx, err := database.client.Tx(ctx)
	if err != nil {
		return fmt.Errorf("开始 PostgreSQL 事务: %w", err)
	}
	sqlTx, ok := clientTx.SQLTx()
	if !ok {
		_ = clientTx.Rollback()
		return errors.New("Ent 事务未暴露可供技术 SQL 复用的 PostgreSQL 事务")
	}
	if _, err := sqlTx.ExecContext(ctx, "SET TRANSACTION ISOLATION LEVEL "+postgresIsolationName(level)); err != nil {
		_ = clientTx.Rollback()
		return fmt.Errorf("设置 PostgreSQL 事务隔离级别: %w", err)
	}
	client := clientTx.Client()
	transaction := transactionContext{sqlTx: sqlTx, client: client}
	transactionCtx := context.WithValue(ctx, transactionContextKey{}, transaction)
	defer func() {
		if recovered := recover(); recovered != nil {
			_ = clientTx.Rollback()
			panic(recovered)
		}
	}()
	if err := operation(transactionCtx); err != nil {
		if rollbackErr := clientTx.Rollback(); rollbackErr != nil && !errors.Is(rollbackErr, sql.ErrTxDone) {
			return errors.Join(err, fmt.Errorf("回滚 PostgreSQL 事务: %w", rollbackErr))
		}
		return err
	}
	if err := clientTx.Commit(); err != nil {
		return fmt.Errorf("提交 PostgreSQL 事务: %w", err)
	}
	return nil
}

func transactionFromContext(ctx context.Context) (transactionContext, bool) {
	transaction, ok := ctx.Value(transactionContextKey{}).(transactionContext)
	return transaction, ok
}

// InTransaction 报告 Context 是否已经绑定当前持久化事务。
// 上层组合事务边界可据此复用同一事务，避免把 Ent/SQL 操作错误地嵌套。
func InTransaction(ctx context.Context) bool {
	_, ok := transactionFromContext(ctx)
	return ok
}

func sqlIsolationLevel(value Isolation) (sql.IsolationLevel, error) {
	switch value {
	case IsolationReadCommitted:
		return sql.LevelReadCommitted, nil
	case IsolationSerializable:
		return sql.LevelSerializable, nil
	default:
		return sql.LevelDefault, fmt.Errorf("不支持的 PostgreSQL 事务隔离级别 %d", value)
	}
}

func postgresIsolationName(level sql.IsolationLevel) string {
	if level == sql.LevelSerializable {
		return "SERIALIZABLE"
	}
	return "READ COMMITTED"
}

func isSerializationFailure(err error) bool {
	var postgresError *pgconn.PgError
	return errors.As(err, &postgresError) && postgresError.Code == "40001"
}
