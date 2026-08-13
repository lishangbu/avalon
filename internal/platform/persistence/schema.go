package persistence

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"sort"
	"strings"

	"github.com/lishangbu/avalon/ent/migrate"
)

const schemaAdvisoryLockKey int64 = 0x4156414c4f4e454e

// SchemaMode 定义应用启动时创建或只读校验 Ent Schema。
type SchemaMode uint8

const (
	// SchemaModeCreate 允许开发测试环境同步并删除已废弃字段和索引。
	SchemaModeCreate SchemaMode = iota + 1
	// SchemaModeValidate 只读比较生产实际结构与 Ent目标结构。
	SchemaModeValidate
)

// ApplySchema 执行配置指定的 Schema模式；任何失败都必须阻止进程启动。
func (database *Database) ApplySchema(ctx context.Context, mode SchemaMode) error {
	switch mode {
	case SchemaModeCreate:
		return database.createSchema(ctx)
	case SchemaModeValidate:
		return database.validateSchema(ctx)
	default:
		return fmt.Errorf("不支持的 Ent Schema 模式 %d", mode)
	}
}

func (database *Database) createSchema(ctx context.Context) (result error) {
	connection, err := database.sqlDB.Conn(ctx)
	if err != nil {
		return fmt.Errorf("取得 Ent Schema advisory lock 连接: %w", err)
	}
	defer connection.Close()
	if _, err := connection.ExecContext(ctx, "SELECT pg_advisory_lock($1)", schemaAdvisoryLockKey); err != nil {
		return fmt.Errorf("取得 Ent Schema advisory lock: %w", err)
	}
	defer func() {
		if _, err := connection.ExecContext(context.WithoutCancel(ctx), "SELECT pg_advisory_unlock($1)", schemaAdvisoryLockKey); err != nil {
			result = errorsJoin(result, fmt.Errorf("释放 Ent Schema advisory lock: %w", err))
		}
	}()
	if err := database.client.Schema.Create(ctx, migrate.WithDropColumn(true), migrate.WithDropIndex(true)); err != nil {
		return fmt.Errorf("同步 Ent Schema: %w", err)
	}
	if err := database.createSnowflakeNodeLeases(ctx); err != nil {
		return err
	}
	if err := database.createSchemaExtensions(ctx); err != nil {
		return err
	}
	if err := database.createSchemaTriggers(ctx); err != nil {
		return err
	}
	return nil
}

// createSnowflakeNodeLeases 为全新开发数据库建立协议固定的 1..254 节点租约行。
func (database *Database) createSnowflakeNodeLeases(ctx context.Context) error {
	if _, err := database.sqlDB.ExecContext(ctx, `
INSERT INTO snowflake_node_lease (
    id, node_id, owner_token, fencing_token, lease_expires_at,
    last_renewed_at, created_at, updated_at
)
SELECT ((1::bigint << 20) | (node::bigint << 12)), node::smallint, repeat('0', 64), 1,
       TIMESTAMPTZ '2026-01-01 00:00:00+00', TIMESTAMPTZ '2025-12-31 23:59:30+00',
       clock_timestamp(), clock_timestamp()
FROM generate_series(1, 254) AS node
ON CONFLICT (node_id) DO NOTHING`); err != nil {
		return fmt.Errorf("建立雪花节点租约基线: %w", err)
	}
	return nil
}

func (database *Database) validateSchema(ctx context.Context) error {
	var changes bytes.Buffer
	// PostgreSQL 部分索引、表达式索引和复合外键由下方技术扩展严格校验，
	// 因此 Ent 比较阶段不能把这些已知扩展误判为应删除的额外索引。
	if err := database.client.Schema.WriteTo(ctx, &changes, migrate.WithDropColumn(true)); err != nil {
		return fmt.Errorf("比较 Ent Schema: %w", err)
	}
	unmanagedChanges, err := database.unmanagedEntSchemaChanges(ctx, changes.String())
	if err != nil {
		return err
	}
	if len(unmanagedChanges) > 0 {
		return fmt.Errorf("PostgreSQL Schema 与 Ent 定义不一致:\n%s", strings.Join(unmanagedChanges, ";\n"))
	}
	expected := make(map[string]struct{}, len(migrate.Tables))
	for _, current := range migrate.Tables {
		expected[current.Name] = struct{}{}
	}
	rows, err := database.sqlDB.QueryContext(ctx, `
SELECT tablename
FROM pg_catalog.pg_tables
WHERE schemaname = 'public'
ORDER BY tablename`)
	if err != nil {
		return fmt.Errorf("读取 PostgreSQL 实际表: %w", err)
	}
	defer rows.Close()
	var extra []string
	for rows.Next() {
		var name string
		if err := rows.Scan(&name); err != nil {
			return fmt.Errorf("扫描 PostgreSQL 实际表: %w", err)
		}
		if _, managed := expected[name]; !managed {
			extra = append(extra, name)
		}
	}
	if err := rows.Err(); err != nil {
		return fmt.Errorf("遍历 PostgreSQL 实际表: %w", err)
	}
	if len(extra) > 0 {
		sort.Strings(extra)
		return fmt.Errorf("PostgreSQL 存在 Ent 未管理的表: %s", strings.Join(extra, ", "))
	}
	return database.validateSchemaExtensions(ctx)
}

// unmanagedEntSchemaChanges 只过滤由项目扩展目录明确拥有的外键；其它差异必须阻止应用启动。
func (database *Database) unmanagedEntSchemaChanges(ctx context.Context, script string) ([]string, error) {
	managedForeignKeys := make(map[string]struct{}, len(schemaForeignKeyDefinitions))
	for _, definition := range schemaForeignKeyDefinitions {
		managedForeignKeys[definition.name] = struct{}{}
	}
	var unmanaged []string
	for _, rawStatement := range strings.Split(script, ";") {
		statement := strings.TrimSpace(rawStatement)
		if statement == "" {
			continue
		}
		const prefix = `ALTER TABLE "`
		if !strings.HasPrefix(statement, prefix) {
			unmanaged = append(unmanaged, statement)
			continue
		}
		tableEnd := strings.Index(statement[len(prefix):], `"`)
		if tableEnd < 0 {
			unmanaged = append(unmanaged, statement)
			continue
		}
		clauses := strings.Split(strings.TrimSpace(statement[len(prefix)+tableEnd+1:]), ", ")
		allManaged := true
		for _, clause := range clauses {
			if strings.HasPrefix(clause, `DROP CONSTRAINT "`) && strings.HasSuffix(clause, `"`) {
				name := strings.TrimSuffix(strings.TrimPrefix(clause, `DROP CONSTRAINT "`), `"`)
				if _, exists := managedForeignKeys[name]; exists {
					continue
				}
			}
			allManaged = false
			break
		}
		if !allManaged {
			unmanaged = append(unmanaged, statement)
		}
	}
	return unmanaged, nil
}

func (database *Database) createSchemaExtensions(ctx context.Context) error {
	for _, current := range schemaIndexDefinitions {
		if _, err := database.sqlDB.ExecContext(ctx, current.statement); err != nil {
			return fmt.Errorf("创建 PostgreSQL 索引 %s: %w", current.name, err)
		}
	}
	for _, current := range schemaForeignKeyExtensions() {
		var exists bool
		if err := database.sqlDB.QueryRowContext(ctx, `
SELECT EXISTS (
    SELECT 1
    FROM pg_constraint AS constraint_name
    JOIN pg_class AS table_name ON table_name.oid = constraint_name.conrelid
    JOIN pg_namespace AS namespace ON namespace.oid = table_name.relnamespace
    WHERE namespace.nspname = 'public'
      AND table_name.relname = $1
      AND constraint_name.conname = $2
)`, current.table, current.name).Scan(&exists); err != nil {
			return fmt.Errorf("检查 PostgreSQL 外键 %s: %w", current.name, err)
		}
		if exists {
			continue
		}
		statement := fmt.Sprintf(
			"ALTER TABLE %s ADD CONSTRAINT %s %s",
			quotePostgreSQLIdentifier(current.table),
			quotePostgreSQLIdentifier(current.name),
			current.definition,
		)
		if _, err := database.sqlDB.ExecContext(ctx, statement); err != nil {
			return fmt.Errorf("创建 PostgreSQL 外键 %s: %w", current.name, err)
		}
	}
	return nil
}

// createSchemaTriggers 创建 Ent Schema 无法表达的不可变快照保护触发器。
func (database *Database) createSchemaTriggers(ctx context.Context) error {
	if _, err := database.sqlDB.ExecContext(ctx, `
CREATE OR REPLACE FUNCTION public.reject_frozen_team_share_mutation()
RETURNS trigger LANGUAGE plpgsql AS $function$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'player_character_team_share 快照不可删除';
    END IF;
    IF NEW.source_team_id IS DISTINCT FROM OLD.source_team_id
       OR NEW.owner_player_character_id IS DISTINCT FROM OLD.owner_player_character_id
       OR NEW.source_team_version IS DISTINCT FROM OLD.source_team_version
       OR NEW.code_digest IS DISTINCT FROM OLD.code_digest
       OR NEW.schema_version IS DISTINCT FROM OLD.schema_version
       OR NEW.snapshot IS DISTINCT FROM OLD.snapshot
       OR NEW.expires_at IS DISTINCT FROM OLD.expires_at
       OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'player_character_team_share 冻结字段不可修改';
    END IF;
    IF NEW.revoked_at IS DISTINCT FROM OLD.revoked_at THEN
        IF OLD.revoked_at IS NOT NULL OR NEW.revoked_at IS NULL
           OR NEW.version <> OLD.version + 1
           OR NEW.updated_at < OLD.updated_at THEN
            RAISE EXCEPTION 'player_character_team_share 只能执行一次撤销转换';
        END IF;
    ELSIF NEW.version IS DISTINCT FROM OLD.version
       OR NEW.updated_at IS DISTINCT FROM OLD.updated_at THEN
        RAISE EXCEPTION 'player_character_team_share 生命周期字段不可单独修改';
    END IF;
    RETURN NEW;
END
$function$`); err != nil {
		return fmt.Errorf("创建 Team 分享快照保护函数: %w", err)
	}
	if _, err := database.sqlDB.ExecContext(ctx, `
DROP TRIGGER IF EXISTS player_character_team_share_immutable_trigger ON public.player_character_team_share`); err != nil {
		return fmt.Errorf("重建 Team 分享快照保护触发器: %w", err)
	}
	if _, err := database.sqlDB.ExecContext(ctx, `
CREATE TRIGGER player_character_team_share_immutable_trigger
BEFORE DELETE OR UPDATE ON public.player_character_team_share
FOR EACH ROW EXECUTE FUNCTION public.reject_frozen_team_share_mutation()`); err != nil {
		return fmt.Errorf("创建 Team 分享快照保护触发器: %w", err)
	}
	return nil
}

func (database *Database) validateSchemaExtensions(ctx context.Context) error {
	var validSnowflakeLeaseRows int
	if err := database.sqlDB.QueryRowContext(ctx, `
SELECT count(*)
FROM snowflake_node_lease
WHERE node_id BETWEEN 1 AND 254
  AND id = ((1::bigint << 20) | (node_id::bigint << 12))`).Scan(&validSnowflakeLeaseRows); err != nil {
		return fmt.Errorf("校验雪花节点租约基线: %w", err)
	}
	if validSnowflakeLeaseRows != 254 {
		return fmt.Errorf("雪花节点租约基线不完整: 有效行 %d，期望 254", validSnowflakeLeaseRows)
	}
	for _, current := range schemaIndexDefinitions {
		var actual string
		if err := database.sqlDB.QueryRowContext(ctx, `
SELECT pg_get_indexdef(index_class.oid)
FROM pg_class AS index_class
JOIN pg_namespace AS namespace ON namespace.oid = index_class.relnamespace
WHERE namespace.nspname = 'public' AND index_class.relname = $1`, current.name).Scan(&actual); err != nil {
			return fmt.Errorf("校验 PostgreSQL 索引 %s: %w", current.name, err)
		}
		expected := strings.Replace(current.statement, " IF NOT EXISTS", "", 1)
		if normalizeSchemaSQL(actual) != normalizeSchemaSQL(expected) {
			return fmt.Errorf("PostgreSQL 索引 %s 与权威定义不一致", current.name)
		}
	}
	for _, current := range schemaForeignKeyExtensions() {
		var actual string
		if err := database.sqlDB.QueryRowContext(ctx, `
SELECT pg_get_constraintdef(constraint_name.oid, true)
FROM pg_constraint AS constraint_name
JOIN pg_class AS table_name ON table_name.oid = constraint_name.conrelid
JOIN pg_namespace AS namespace ON namespace.oid = table_name.relnamespace
WHERE namespace.nspname = 'public'
  AND table_name.relname = $1
  AND constraint_name.conname = $2`, current.table, current.name).Scan(&actual); err != nil {
			return fmt.Errorf("校验 PostgreSQL 外键 %s: %w", current.name, err)
		}
		if normalizeSchemaSQL(actual) != normalizeSchemaSQL(current.definition) {
			return fmt.Errorf("PostgreSQL 外键 %s 与权威定义不一致", current.name)
		}
	}
	var triggerExists bool
	if err := database.sqlDB.QueryRowContext(ctx, `
SELECT EXISTS (
    SELECT 1 FROM pg_trigger AS trigger_name
    JOIN pg_class AS table_name ON table_name.oid = trigger_name.tgrelid
    WHERE table_name.relname = 'player_character_team_share'
      AND trigger_name.tgname = 'player_character_team_share_immutable_trigger'
      AND NOT trigger_name.tgisinternal
)`).Scan(&triggerExists); err != nil {
		return fmt.Errorf("校验 Team 分享快照保护触发器: %w", err)
	}
	if !triggerExists {
		return errors.New("PostgreSQL 缺少 Team 分享快照保护触发器")
	}
	return nil
}

func normalizeSchemaSQL(value string) string {
	return strings.Join(strings.Fields(strings.TrimSpace(value)), " ")
}

func quotePostgreSQLIdentifier(value string) string {
	return `"` + strings.ReplaceAll(value, `"`, `""`) + `"`
}

func errorsJoin(left error, right error) error {
	if left == nil {
		return right
	}
	return fmt.Errorf("%v; %w", left, right)
}
