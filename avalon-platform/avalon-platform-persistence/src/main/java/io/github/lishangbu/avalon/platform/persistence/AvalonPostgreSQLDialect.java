package io.github.lishangbu.avalon.platform.persistence;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.TableMigrator;

/** 在 Hibernate 增量更新结构时，同步实体声明的 PostgreSQL 表和字段注释。 */
public class AvalonPostgreSQLDialect extends PostgreSQLDialect {

	public AvalonPostgreSQLDialect(DialectResolutionInfo info) {
		super(info);
	}

	/** 标准 update 不更新已有对象的注释；每次启动按实体元数据同步，不重建表。 */
	@Override
	public TableMigrator getTableMigrator() {
		var delegate = super.getTableMigrator();
		return (table, metadata, information, context) -> {
			var statements = new ArrayList<>(
					Arrays.asList(delegate.getSqlAlterStrings(table, metadata, information, context)));
			appendComments(table, context.format(table.getQualifiedTableName()), statements);
			return statements.toArray(String[]::new);
		};
	}

	private void appendComments(Table table, String name, List<String> statements) {
		if (table.getComment() != null) {
			statements.add("comment on " + "table " + name + " is " + literal(table.getComment()));
		}
		for (var column : table.getColumns()) {
			if (column.getComment() != null) {
				statements.add("comment on column " + name + "." + column.getQuotedName(this) + " is "
						+ literal(column.getComment()));
			}
		}
	}

	private static String literal(String value) {
		return "'" + value.replace("'", "''") + "'";
	}

}
