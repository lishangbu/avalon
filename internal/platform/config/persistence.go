package config

import (
	"fmt"
	"time"

	configv1 "github.com/lishangbu/avalon/api/gen/go/avalon/config/v1"
	"github.com/lishangbu/avalon/internal/platform/persistence"
)

// PersistenceConfig 将完成校验的 Protobuf 配置转换为统一 PostgreSQL/Ent 运行参数。
func PersistenceConfig(value *configv1.DatabaseConfig) persistence.Config {
	pool := value.GetPool()
	return persistence.Config{
		URL:                   value.GetUrl(),
		MaxOpenConnections:    int(pool.GetMaxOpenConnections()),
		MaxIdleConnections:    int(pool.GetMaxIdleConnections()),
		ConnectionMaxLifetime: time.Duration(pool.GetConnectionMaxLifetimeSeconds()) * time.Second,
		ConnectionMaxIdleTime: time.Duration(pool.GetConnectionMaxIdleSeconds()) * time.Second,
		DebugSQL:              value.GetDebugSql(),
	}
}

// PersistenceSchemaMode 将对外配置枚举转换为持久化层的封闭 Schema 模式。
func PersistenceSchemaMode(value configv1.DatabaseSchemaMode) (persistence.SchemaMode, error) {
	switch value {
	case configv1.DatabaseSchemaMode_DATABASE_SCHEMA_MODE_CREATE:
		return persistence.SchemaModeCreate, nil
	case configv1.DatabaseSchemaMode_DATABASE_SCHEMA_MODE_VALIDATE:
		return persistence.SchemaModeValidate, nil
	default:
		return 0, fmt.Errorf("不支持的数据库 Schema 模式 %s", value.String())
	}
}
