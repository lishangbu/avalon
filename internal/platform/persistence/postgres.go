// Package persistence 提供 Ent、原生 SQL 和显式事务共享的 PostgreSQL 技术边界。
package persistence

import (
	"context"
	"database/sql"
	"fmt"
	"time"

	entsql "entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/stdlib"
	avalonent "github.com/lishangbu/avalon/ent"
)

// Config 保存单个 PostgreSQL 连接池的全部显式参数。
type Config struct {
	// URL 是包含网络地址和认证信息的 PostgreSQL 连接字符串。
	URL string
	// MaxOpenConnections 是连接池允许同时打开的最大连接数。
	MaxOpenConnections int
	// MaxIdleConnections 是连接池最多保留的空闲连接数。
	MaxIdleConnections int
	// ConnectionMaxLifetime 是连接被轮换前的最长存活时间。
	ConnectionMaxLifetime time.Duration
	// ConnectionMaxIdleTime 是空闲连接被关闭前允许等待的最长时间。
	ConnectionMaxIdleTime time.Duration
	// DebugSQL 控制 Ent 是否输出包含原始参数的 SQL 调试日志。
	DebugSQL bool
}

// Database 统一拥有 pgx stdlib 数据库连接池和根 Ent Client。
type Database struct {
	sqlDB  *sql.DB
	client *avalonent.Client
}

// Open 创建单个 pgx stdlib 连接池；首次连接由后续 Schema 操作自然验证。
func Open(config Config) (*Database, error) {
	parsed, err := pgx.ParseConfig(config.URL)
	if err != nil {
		return nil, fmt.Errorf("解析 PostgreSQL 连接配置: %w", err)
	}
	sqlDB := stdlib.OpenDB(*parsed)
	sqlDB.SetMaxOpenConns(config.MaxOpenConnections)
	sqlDB.SetMaxIdleConns(config.MaxIdleConnections)
	sqlDB.SetConnMaxLifetime(config.ConnectionMaxLifetime)
	sqlDB.SetConnMaxIdleTime(config.ConnectionMaxIdleTime)
	driver := entsql.OpenDB("postgres", sqlDB)
	options := []avalonent.Option{avalonent.Driver(driver)}
	if config.DebugSQL {
		options = append(options, avalonent.Debug())
	}
	client := avalonent.NewClient(options...)
	client.Use(requireExplicitSnowflakeIdentifiers())
	return &Database{sqlDB: sqlDB, client: client}, nil
}

// Client 返回当前事务绑定的 Ent Client；没有事务时返回根 Client。
func (database *Database) Client(ctx context.Context) *avalonent.Client {
	if transaction, ok := transactionFromContext(ctx); ok {
		return transaction.client
	}
	return database.client
}

// SQL 返回当前事务绑定的原生 SQL 执行器；没有事务时返回共享连接池。
func (database *Database) SQL(ctx context.Context) SQLExecutor {
	if transaction, ok := transactionFromContext(ctx); ok {
		return transaction.sqlTx
	}
	return database.sqlDB
}

// RootClient 返回仅供启动 Schema 管理使用的根 Ent Client。
func (database *Database) RootClient() *avalonent.Client {
	return database.client
}

// RawDB 返回仅供 Schema 检查和受控技术设施使用的共享 database/sql 连接池。
func (database *Database) RawDB() *sql.DB {
	return database.sqlDB
}

// Close 关闭 Ent Client及其唯一底层连接池。
func (database *Database) Close() error {
	if database == nil || database.client == nil {
		return nil
	}
	return database.client.Close()
}

// SQLExecutor 是 Ent Store 内复杂只读投影可以共享的最小原生 SQL 边界。
type SQLExecutor interface {
	ExecContext(context.Context, string, ...any) (sql.Result, error)
	QueryContext(context.Context, string, ...any) (*sql.Rows, error)
	QueryRowContext(context.Context, string, ...any) *sql.Row
}
