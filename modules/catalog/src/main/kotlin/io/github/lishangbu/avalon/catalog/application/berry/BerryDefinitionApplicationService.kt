package io.github.lishangbu.avalon.catalog.application.berry

import io.github.lishangbu.avalon.catalog.domain.CatalogNotFound
import io.github.lishangbu.avalon.catalog.domain.berry.BerryDefinition
import io.github.lishangbu.avalon.catalog.domain.berry.BerryDefinitionDraft
import io.github.lishangbu.avalon.catalog.domain.berry.BerryDefinitionId
import io.github.lishangbu.avalon.catalog.domain.berry.BerryDefinitionRepository
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * 树果基础档案应用服务。
 */
@ApplicationScoped
class BerryDefinitionApplicationService(
    private val repository: BerryDefinitionRepository,
) {
    suspend fun listBerryDefinitions(): List<BerryDefinition> = repository.listBerryDefinitions()

    suspend fun getBerryDefinition(id: UUID): BerryDefinition =
        repository.findBerryDefinition(BerryDefinitionId(id))
            ?: throw CatalogNotFound("berry_definition", id.toString())

    suspend fun createBerryDefinition(draft: BerryDefinitionDraft): BerryDefinition =
        repository.createBerryDefinition(draft)

    suspend fun updateBerryDefinition(
        id: UUID,
        draft: BerryDefinitionDraft,
    ): BerryDefinition = repository.updateBerryDefinition(BerryDefinitionId(id), draft)

    suspend fun deleteBerryDefinition(id: UUID) {
        repository.deleteBerryDefinition(BerryDefinitionId(id))
    }
}
