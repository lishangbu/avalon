package io.github.lishangbu.avalon.catalog.infrastructure.berry

import io.github.lishangbu.avalon.catalog.domain.berry.*
import io.github.lishangbu.avalon.catalog.infrastructure.berry.sql.BerrySqlGateway
import io.github.lishangbu.avalon.catalog.infrastructure.sql.mapCatalogDatabaseError
import io.github.lishangbu.avalon.shared.infra.sql.translateSqlErrors
import io.vertx.mutiny.sqlclient.Pool
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class BerryDefinitionSqlRepository(
    pool: Pool,
) : BerryDefinitionRepository {
    private val gateway = BerrySqlGateway(pool)

    override suspend fun listBerryDefinitions(): List<BerryDefinition> =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.listBerryDefinitions() }

    override suspend fun findBerryDefinition(id: BerryDefinitionId): BerryDefinition? =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.findBerryDefinition(id) }

    override suspend fun createBerryDefinition(draft: BerryDefinitionDraft): BerryDefinition =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.createBerryDefinition(draft) }

    override suspend fun updateBerryDefinition(
        id: BerryDefinitionId,
        draft: BerryDefinitionDraft,
    ): BerryDefinition =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.updateBerryDefinition(id, draft) }

    override suspend fun deleteBerryDefinition(id: BerryDefinitionId) {
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.deleteBerryDefinition(id) }
    }
}
