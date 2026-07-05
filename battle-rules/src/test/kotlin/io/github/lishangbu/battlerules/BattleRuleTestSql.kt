package io.github.lishangbu.battlerules

import org.babyfish.jimmer.sql.kt.KSqlClient
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * 战斗规则集成测试使用的极小 SQL 夹具工具。
 *
 * 这些测试经常需要故意把数据库写成“管理端不会允许、但生产库可能因迁移脚本或人工修复而出现”的状态，
 * 以验证运行时装配层能把坏资料转成结构化 API 错误。这里仍保留直接 SQL，是为了绕过 Service 校验；
 * 但连接入口统一走 Jimmer 的 [KSqlClient]，避免测试代码继续依赖 Spring JDBC 模板，也能覆盖和生产代码
 * 一致的事务连接获取路径。
 */
internal fun KSqlClient.executeTestSql(sql: String, vararg parameters: Any?): Int =
	withTestConnection { connection ->
		connection.prepareStatement(sql).use { statement ->
			statement.bindTestParameters(parameters)
			statement.executeUpdate()
		}
	}

/**
 * 执行只读夹具查询并映射为列表。
 *
 * 表名和字段名只在测试源码内硬编码，不接受外部输入；所有动态值仍通过 [parameters] 绑定，避免测试夹具
 * 因字符串拼接值而掩盖真实 SQL 行为。
 */
internal fun <T> KSqlClient.queryTestSql(
	sql: String,
	vararg parameters: Any?,
	mapper: (ResultSet) -> T,
): List<T> =
	withTestConnection { connection ->
		connection.prepareStatement(sql).use { statement ->
			statement.bindTestParameters(parameters)
			statement.executeQuery().use { resultSet ->
				buildList {
					while (resultSet.next()) {
						add(mapper(resultSet))
					}
				}
			}
		}
	}

internal fun <T> KSqlClient.querySingleTestSql(
	sql: String,
	vararg parameters: Any?,
	mapper: (ResultSet) -> T,
): T =
	queryTestSql(sql, *parameters, mapper = mapper).single()

private fun <T> KSqlClient.withTestConnection(block: (Connection) -> T): T =
	javaClient.connectionManager.execute { connection -> block(connection) }

private fun PreparedStatement.bindTestParameters(parameters: Array<out Any?>) {
	parameters.forEachIndexed { index, value ->
		setObject(index + 1, value)
	}
}
