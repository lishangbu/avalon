//go:build integration

package persistence

import (
	"context"
	"database/sql"
	"testing"
	"time"

	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

func TestCreatedSchemaHasNoDatabaseGeneratedSnowflakePrimaryKeys(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Minute)
	defer cancel()
	container, err := postgres.Run(
		ctx,
		"postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7",
		postgres.WithDatabase("avalon_test"),
		postgres.WithUsername("avalon"),
		postgres.WithPassword("avalon"),
		postgres.BasicWaitStrategies(),
	)
	if err != nil {
		t.Fatalf("启动 PostgreSQL: %v", err)
	}
	t.Cleanup(func() {
		if err := container.Terminate(context.Background()); err != nil {
			t.Errorf("停止 PostgreSQL: %v", err)
		}
	})
	url, err := container.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		t.Fatal(err)
	}
	database, err := Open(Config{URL: url, MaxOpenConnections: 5, MaxIdleConnections: 2})
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = database.Close() })
	if err := database.ApplySchema(ctx, SchemaModeCreate); err != nil {
		t.Fatalf("创建 Ent Schema: %v", err)
	}
	rows, err := database.RawDB().QueryContext(ctx, `
SELECT columns.table_name, columns.column_default, columns.is_identity
FROM information_schema.table_constraints AS constraints
JOIN information_schema.key_column_usage AS keys
  ON keys.constraint_schema = constraints.constraint_schema
 AND keys.constraint_name = constraints.constraint_name
JOIN information_schema.columns AS columns
  ON columns.table_schema = keys.table_schema
 AND columns.table_name = keys.table_name
 AND columns.column_name = keys.column_name
WHERE constraints.constraint_schema = 'public'
  AND constraints.constraint_type = 'PRIMARY KEY'
  AND columns.data_type = 'bigint'
ORDER BY columns.table_name`)
	if err != nil {
		t.Fatal(err)
	}
	defer rows.Close()
	checked := 0
	for rows.Next() {
		var tableName string
		var defaultValue sql.NullString
		var identity string
		if err := rows.Scan(&tableName, &defaultValue, &identity); err != nil {
			t.Fatal(err)
		}
		checked++
		if defaultValue.Valid {
			t.Errorf("表 %s 的 Snowflake 主键仍有数据库默认值 %s", tableName, defaultValue.String)
		}
		if identity != "NO" {
			t.Errorf("表 %s 的 Snowflake 主键仍是 identity", tableName)
		}
	}
	if err := rows.Err(); err != nil {
		t.Fatal(err)
	}
	if checked == 0 {
		t.Fatal("完整 Schema 中没有找到 BIGINT 主键")
	}
	var invalidForeignKeyColumns int
	if err := database.RawDB().QueryRowContext(ctx, `
SELECT count(*)
FROM pg_constraint AS constraint_name
CROSS JOIN LATERAL unnest(constraint_name.conkey, constraint_name.confkey)
  AS keys(child_attribute_number, parent_attribute_number)
JOIN pg_attribute AS child_column
  ON child_column.attrelid = constraint_name.conrelid
 AND child_column.attnum = keys.child_attribute_number
JOIN pg_attribute AS parent_column
  ON parent_column.attrelid = constraint_name.confrelid
 AND parent_column.attnum = keys.parent_attribute_number
WHERE constraint_name.contype = 'f'
  AND (
    (
      (child_column.attname = 'id' OR child_column.attname LIKE '%\_id' ESCAPE '\')
      AND child_column.atttypid <> 'bigint'::regtype
    )
    OR (
      (parent_column.attname = 'id' OR parent_column.attname LIKE '%\_id' ESCAPE '\')
      AND parent_column.atttypid <> 'bigint'::regtype
    )
  )`).Scan(&invalidForeignKeyColumns); err != nil {
		t.Fatal(err)
	}
	if invalidForeignKeyColumns != 0 {
		t.Fatalf("完整 Schema 中存在 %d 个非 BIGINT Identifier 外键列", invalidForeignKeyColumns)
	}
	var leaseRows int
	if err := database.RawDB().QueryRowContext(ctx, "SELECT count(*) FROM snowflake_node_lease").Scan(&leaseRows); err != nil {
		t.Fatal(err)
	}
	if leaseRows != 254 {
		t.Fatalf("雪花节点租约行数 = %d，期望 254", leaseRows)
	}
	if err := database.ApplySchema(ctx, SchemaModeValidate); err != nil {
		t.Fatalf("校验刚创建的 Ent Schema: %v", err)
	}
}
