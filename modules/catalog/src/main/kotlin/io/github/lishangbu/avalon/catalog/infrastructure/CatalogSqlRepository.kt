package io.github.lishangbu.avalon.catalog.infrastructure

import io.github.lishangbu.avalon.catalog.application.query.SpeciesPageQuery
import io.github.lishangbu.avalon.catalog.application.query.SpeciesQueryRepository
import io.github.lishangbu.avalon.catalog.domain.*
import io.github.lishangbu.avalon.catalog.infrastructure.move.sql.MoveSqlGateway
import io.github.lishangbu.avalon.catalog.infrastructure.reference.sql.ReferenceSqlGateway
import io.github.lishangbu.avalon.catalog.infrastructure.species.sql.SpeciesSqlGateway
import io.github.lishangbu.avalon.catalog.infrastructure.sql.mapCatalogDatabaseError
import io.github.lishangbu.avalon.shared.application.query.Page
import io.github.lishangbu.avalon.shared.infra.sql.translateSqlErrors
import io.vertx.mutiny.sqlclient.Pool
import jakarta.enterprise.context.ApplicationScoped

/**
 * Catalog SQL 仓储对外仍保留一个稳定入口，内部再拆成按能力协作的 SQL gateway。
 *
 * application 层继续只依赖 `CatalogRepository`，但基础设施层不再把 type、
 * reference、move、species 的全部 SQL 逻辑硬塞进一个超长类里。
 */
@ApplicationScoped
class CatalogSqlRepository(
    pool: Pool,
) : CatalogRepository, SpeciesQueryRepository {
    private val referenceGateway = ReferenceSqlGateway(pool)
    private val moveGateway = MoveSqlGateway(pool)
    private val speciesGateway = SpeciesSqlGateway(pool)

    override suspend fun listNatures(): List<Nature> =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.listNatures() }

    override suspend fun findNature(id: NatureId): Nature? =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.findNature(id) }

    override suspend fun createNature(draft: NatureDraft): Nature =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.createNature(draft) }

    override suspend fun updateNature(
        id: NatureId,
        draft: NatureDraft,
    ): Nature =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.updateNature(id, draft) }

    override suspend fun deleteNature(id: NatureId) {
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.deleteNature(id) }
    }

    override suspend fun listItems(): List<Item> =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.listItems() }

    override suspend fun findItem(id: ItemId): Item? =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.findItem(id) }

    override suspend fun createItem(draft: ItemDraft): Item =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.createItem(draft) }

    override suspend fun updateItem(
        id: ItemId,
        draft: ItemDraft,
    ): Item =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.updateItem(id, draft) }

    override suspend fun deleteItem(id: ItemId) {
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.deleteItem(id) }
    }

    override suspend fun listAbilities(): List<Ability> =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.listAbilities() }

    override suspend fun findAbility(id: AbilityId): Ability? =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.findAbility(id) }

    override suspend fun createAbility(draft: AbilityDraft): Ability =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.createAbility(draft) }

    override suspend fun updateAbility(
        id: AbilityId,
        draft: AbilityDraft,
    ): Ability =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.updateAbility(id, draft) }

    override suspend fun deleteAbility(id: AbilityId) {
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.deleteAbility(id) }
    }

    override suspend fun listMoves(): List<Move> =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.listMoves() }

    override suspend fun findMove(id: MoveId): Move? =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.findMove(id) }

    override suspend fun createMove(draft: MoveDraft): Move =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.createMove(draft) }

    override suspend fun updateMove(
        id: MoveId,
        draft: MoveDraft,
    ): Move =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.updateMove(id, draft) }

    override suspend fun deleteMove(id: MoveId) {
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.deleteMove(id) }
    }

    override suspend fun listMoveCategories(): List<MoveCategory> =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.listMoveCategories() }

    override suspend fun findMoveCategory(id: MoveCategoryId): MoveCategory? =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.findMoveCategory(id) }

    override suspend fun createMoveCategory(draft: MoveCategoryDraft): MoveCategory =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.createMoveCategory(draft) }

    override suspend fun updateMoveCategory(
        id: MoveCategoryId,
        draft: MoveCategoryDraft,
    ): MoveCategory =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.updateMoveCategory(id, draft) }

    override suspend fun deleteMoveCategory(id: MoveCategoryId) {
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.deleteMoveCategory(id) }
    }

    override suspend fun listMoveAilments(): List<MoveAilment> =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.listMoveAilments() }

    override suspend fun findMoveAilment(id: MoveAilmentId): MoveAilment? =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.findMoveAilment(id) }

    override suspend fun createMoveAilment(draft: MoveAilmentDraft): MoveAilment =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.createMoveAilment(draft) }

    override suspend fun updateMoveAilment(
        id: MoveAilmentId,
        draft: MoveAilmentDraft,
    ): MoveAilment =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.updateMoveAilment(id, draft) }

    override suspend fun deleteMoveAilment(id: MoveAilmentId) {
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.deleteMoveAilment(id) }
    }

    override suspend fun listMoveTargets(): List<MoveTarget> =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.listMoveTargets() }

    override suspend fun findMoveTarget(id: MoveTargetId): MoveTarget? =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.findMoveTarget(id) }

    override suspend fun createMoveTarget(draft: MoveTargetDraft): MoveTarget =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.createMoveTarget(draft) }

    override suspend fun updateMoveTarget(
        id: MoveTargetId,
        draft: MoveTargetDraft,
    ): MoveTarget =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.updateMoveTarget(id, draft) }

    override suspend fun deleteMoveTarget(id: MoveTargetId) {
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.deleteMoveTarget(id) }
    }

    override suspend fun listMoveLearnMethods(): List<MoveLearnMethod> =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.listMoveLearnMethods() }

    override suspend fun findMoveLearnMethod(id: MoveLearnMethodId): MoveLearnMethod? =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.findMoveLearnMethod(id) }

    override suspend fun createMoveLearnMethod(draft: MoveLearnMethodDraft): MoveLearnMethod =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.createMoveLearnMethod(draft) }

    override suspend fun updateMoveLearnMethod(
        id: MoveLearnMethodId,
        draft: MoveLearnMethodDraft,
    ): MoveLearnMethod =
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.updateMoveLearnMethod(id, draft) }

    override suspend fun deleteMoveLearnMethod(id: MoveLearnMethodId) {
        translateSqlErrors(::mapCatalogDatabaseError) { moveGateway.deleteMoveLearnMethod(id) }
    }

    override suspend fun listGrowthRates(): List<GrowthRate> =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.listGrowthRates() }

    override suspend fun findGrowthRate(id: GrowthRateId): GrowthRate? =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.findGrowthRate(id) }

    override suspend fun createGrowthRate(draft: GrowthRateDraft): GrowthRate =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.createGrowthRate(draft) }

    override suspend fun updateGrowthRate(
        id: GrowthRateId,
        draft: GrowthRateDraft,
    ): GrowthRate =
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.updateGrowthRate(id, draft) }

    override suspend fun deleteGrowthRate(id: GrowthRateId) {
        translateSqlErrors(::mapCatalogDatabaseError) { referenceGateway.deleteGrowthRate(id) }
    }

    override suspend fun listSpeciesEvolutions(): List<SpeciesEvolution> =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.listSpeciesEvolutions() }

    override suspend fun findSpeciesEvolution(id: SpeciesEvolutionId): SpeciesEvolution? =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.findSpeciesEvolution(id) }

    override suspend fun createSpeciesEvolution(draft: SpeciesEvolutionDraft): SpeciesEvolution =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.createSpeciesEvolution(draft) }

    override suspend fun updateSpeciesEvolution(
        id: SpeciesEvolutionId,
        draft: SpeciesEvolutionDraft,
    ): SpeciesEvolution =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.updateSpeciesEvolution(id, draft) }

    override suspend fun deleteSpeciesEvolution(id: SpeciesEvolutionId) {
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.deleteSpeciesEvolution(id) }
    }

    override suspend fun listSpeciesAbilities(): List<SpeciesAbility> =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.listSpeciesAbilities() }

    override suspend fun findSpeciesAbility(id: SpeciesAbilityId): SpeciesAbility? =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.findSpeciesAbility(id) }

    override suspend fun createSpeciesAbility(draft: SpeciesAbilityDraft): SpeciesAbility =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.createSpeciesAbility(draft) }

    override suspend fun updateSpeciesAbility(
        id: SpeciesAbilityId,
        draft: SpeciesAbilityDraft,
    ): SpeciesAbility =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.updateSpeciesAbility(id, draft) }

    override suspend fun deleteSpeciesAbility(id: SpeciesAbilityId) {
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.deleteSpeciesAbility(id) }
    }

    override suspend fun listSpeciesMoveLearnsets(): List<SpeciesMoveLearnset> =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.listSpeciesMoveLearnsets() }

    override suspend fun findSpeciesMoveLearnset(id: SpeciesMoveLearnsetId): SpeciesMoveLearnset? =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.findSpeciesMoveLearnset(id) }

    override suspend fun createSpeciesMoveLearnset(draft: SpeciesMoveLearnsetDraft): SpeciesMoveLearnset =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.createSpeciesMoveLearnset(draft) }

    override suspend fun updateSpeciesMoveLearnset(
        id: SpeciesMoveLearnsetId,
        draft: SpeciesMoveLearnsetDraft,
    ): SpeciesMoveLearnset =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.updateSpeciesMoveLearnset(id, draft) }

    override suspend fun deleteSpeciesMoveLearnset(id: SpeciesMoveLearnsetId) {
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.deleteSpeciesMoveLearnset(id) }
    }

    override suspend fun pageSpecies(query: SpeciesPageQuery): Page<Species> =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.pageSpecies(query.pageRequest) }

    override suspend fun findSpecies(id: SpeciesId): Species? =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.findSpecies(id) }

    override suspend fun createSpecies(draft: SpeciesDraft): Species =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.createSpecies(draft) }

    override suspend fun updateSpecies(
        id: SpeciesId,
        draft: SpeciesDraft,
    ): Species =
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.updateSpecies(id, draft) }

    override suspend fun deleteSpecies(id: SpeciesId) {
        translateSqlErrors(::mapCatalogDatabaseError) { speciesGateway.deleteSpecies(id) }
    }
}
