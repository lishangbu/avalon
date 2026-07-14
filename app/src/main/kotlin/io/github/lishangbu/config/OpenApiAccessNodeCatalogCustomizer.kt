package io.github.lishangbu.config

import io.github.lishangbu.security.entity.SecurityAccessNode
import io.github.lishangbu.security.entity.code
import io.swagger.v3.oas.models.OpenAPI
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springdoc.core.customizers.GlobalOpenApiCustomizer

/**
 * 将稳定权限码目录写入 OpenAPI 扩展，供各客户端在构建阶段校验本地路由权限声明。
 */
class OpenApiAccessNodeCatalogCustomizer(
	private val sqlClient: KSqlClient,
) : GlobalOpenApiCustomizer {
	override fun customise(openApi: OpenAPI) {
		val accessNodeCodes = sqlClient.createQuery(SecurityAccessNode::class) {
			orderBy(table.code)
			select(table.code)
		}.execute()
		openApi.addExtension("x-access-node-codes", accessNodeCodes)
	}
}
