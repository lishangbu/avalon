package persistence

import (
	"github.com/lishangbu/avalon/internal/platform/snowflake"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// newEntAdministrationRecords 创建使用共享租约 Source 的资料管理幂等记录适配器。
func newEntAdministrationRecords(client *avalonent.Client, newID snowflake.Source) *idempotency.AdminEntRecords {
	return idempotency.NewAdminEntRecords(client, newID)
}

// newEntID 将领域 Identifier 转换为 Ent Identifier 类型，集中收敛适配器内部的类型转换。
func newEntID(value snowflake.ID) snowflake.ID { return value }
