package io.github.lishangbu.avalon.catalog.infrastructure.type

import io.github.lishangbu.avalon.catalog.domain.TypeEffectiveness
import io.github.lishangbu.avalon.catalog.domain.TypeEffectivenessDraft
import io.github.lishangbu.avalon.catalog.domain.type.TypeChartRepository
import io.github.lishangbu.avalon.catalog.infrastructure.sql.mapCatalogDatabaseError
import io.github.lishangbu.avalon.catalog.infrastructure.type.sql.TypeSqlGateway
import io.github.lishangbu.avalon.shared.infra.sql.translateSqlErrors
import io.vertx.mutiny.sqlclient.Pool
import jakarta.enterprise.context.ApplicationScoped

/**
 * 属性克制矩阵 SQL 仓储实现。
 */
@ApplicationScoped
class TypeChartSqlRepository(
    pool: Pool,
) : TypeChartRepository {
    private val gateway = TypeSqlGateway(pool)

    override suspend fun listEntries(): List<TypeEffectiveness> =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.listTypeEffectiveness() }

    override suspend fun replaceEntries(entries: List<TypeEffectivenessDraft>) {
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.replaceTypeEffectiveness(entries) }
    }
}
