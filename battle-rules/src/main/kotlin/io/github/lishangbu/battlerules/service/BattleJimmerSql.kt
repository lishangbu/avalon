package io.github.lishangbu.battlerules.service

import org.babyfish.jimmer.sql.kt.KSqlClient
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * battle-rules 中少量运行时联查的 Jimmer SQL 入口。
 *
 * 大部分管理型 CRUD 已经使用 Jimmer typed query；这里保留给跨资料表的运行时快照装配，例如技能主资料与规则
 * 子表的联查。它通过 Jimmer 的 `ConnectionManager` 进入当前事务，不再注入 Spring JDBC 模板。调用方仍然
 * 必须使用参数化占位符传入外部值，只有固定 SQL 或已由代码白名单校验过的表名才允许拼进 SQL 文本。
 */
internal fun <T> KSqlClient.querySql(sql: String, vararg parameters: Any?, mapper: (ResultSet) -> T): List<T> =
	withJimmerConnection { connection ->
		connection.prepareStatement(sql).use { statement ->
			statement.bind(parameters.asList())
			statement.executeQuery().use { resultSet ->
				buildList {
					while (resultSet.next()) {
						add(mapper(resultSet))
					}
				}
			}
		}
	}

/**
 * 执行没有结果集的 SQL，例如 PostgreSQL 事务级 advisory lock。
 */
internal fun KSqlClient.executeSql(sql: String, vararg parameters: Any?) {
	withJimmerConnection { connection ->
		connection.prepareStatement(sql).use { statement ->
			statement.bind(parameters.asList())
			statement.execute()
		}
	}
}

private fun <T> KSqlClient.withJimmerConnection(block: (Connection) -> T): T =
	javaClient.connectionManager.execute { connection -> block(connection) }

private fun PreparedStatement.bind(parameters: List<Any?>) {
	parameters.forEachIndexed { index, value ->
		setObject(index + 1, value)
	}
}
