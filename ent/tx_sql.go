package ent

import (
	"database/sql"

	entsql "entgo.io/ent/dialect/sql"
)

// SQLTx 返回 Ent 官方 Tx 使用的底层 database/sql 事务，供已登记的 PostgreSQL 技术边界复用。
// 业务查询和写入仍必须使用 Tx.Client()；该方法仅用于审计哈希链等无法由 Ent 表达的 SQL。
func (tx *Tx) SQLTx() (*sql.Tx, bool) {
	if tx == nil {
		return nil, false
	}
	driver, ok := tx.config.driver.(*txDriver)
	if !ok {
		return nil, false
	}
	sqlDriver, ok := driver.tx.(*entsql.Tx)
	if !ok {
		return nil, false
	}
	sqlTx, ok := sqlDriver.Tx.(*sql.Tx)
	return sqlTx, ok
}
