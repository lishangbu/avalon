package persistence

import (
	"testing"

	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/ent/migrate"
)

func TestAllPrimaryKeysRequireExplicitSnowflakeIdentifiers(t *testing.T) {
	t.Parallel()
	checked := 0
	for _, current := range migrate.Tables {
		for _, primaryKey := range current.PrimaryKey {
			if primaryKey.Type != field.TypeInt64 {
				t.Errorf("表 %s 的主键 %s 不是 Snowflake BIGINT", current.Name, primaryKey.Name)
				continue
			}
			checked++
			if primaryKey.Increment {
				t.Errorf("表 %s 的数值主键仍启用了数据库自增", current.Name)
			}
			if primaryKey.Default != nil {
				t.Errorf("表 %s 的数值主键仍保留数据库默认值", current.Name)
			}
		}
	}
	if checked == 0 {
		t.Fatal("Ent 迁移元数据中没有找到 Snowflake 数值主键")
	}
}

func TestAllEntForeignKeysUseSnowflakeBigintColumns(t *testing.T) {
	t.Parallel()
	checked := 0
	for _, current := range migrate.Tables {
		for _, foreignKey := range current.ForeignKeys {
			if len(foreignKey.Columns) != len(foreignKey.RefColumns) {
				t.Errorf("表 %s 的外键 %s 列数不一致", current.Name, foreignKey.Symbol)
				continue
			}
			for index, column := range foreignKey.Columns {
				checked++
				if column.Type != field.TypeInt64 {
					t.Errorf("表 %s 的外键 %s 列 %s 不是 Snowflake BIGINT", current.Name, foreignKey.Symbol, column.Name)
				}
				if foreignKey.RefColumns[index].Type != field.TypeInt64 {
					t.Errorf("表 %s 的外键 %s 引用列 %s 不是 Snowflake BIGINT", current.Name, foreignKey.Symbol, foreignKey.RefColumns[index].Name)
				}
			}
		}
	}
	if checked == 0 {
		t.Fatal("Ent 迁移元数据中没有找到外键")
	}
}
