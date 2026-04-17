package io.github.lishangbu.avalon.catalog.infrastructure.type

import io.github.lishangbu.avalon.catalog.domain.TypeDefinition
import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionDraft
import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionId
import io.github.lishangbu.avalon.catalog.domain.type.TypeDefinitionRepository
import io.github.lishangbu.avalon.catalog.infrastructure.sql.mapCatalogDatabaseError
import io.github.lishangbu.avalon.catalog.infrastructure.type.sql.TypeSqlGateway
import io.github.lishangbu.avalon.shared.infra.sql.translateSqlErrors
import io.vertx.mutiny.sqlclient.Pool
import jakarta.enterprise.context.ApplicationScoped

/**
 * 属性定义 SQL 仓储实现。
 */
@ApplicationScoped
class TypeDefinitionSqlRepository(
    pool: Pool,
) : TypeDefinitionRepository {
    private val gateway = TypeSqlGateway(pool)

    override suspend fun listTypeDefinitions(): List<TypeDefinition> =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.listTypeDefinitions() }

    override suspend fun findTypeDefinition(id: TypeDefinitionId): TypeDefinition? =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.findTypeDefinition(id) }

    override suspend fun createTypeDefinition(draft: TypeDefinitionDraft): TypeDefinition =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.createTypeDefinition(draft) }

    override suspend fun updateTypeDefinition(
        id: TypeDefinitionId,
        draft: TypeDefinitionDraft,
    ): TypeDefinition =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.updateTypeDefinition(id, draft) }

    override suspend fun deleteTypeDefinition(id: TypeDefinitionId) {
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.deleteTypeDefinition(id) }
    }
}
