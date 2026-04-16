package io.github.lishangbu.avalon.catalog.application

import io.github.lishangbu.avalon.catalog.application.query.SpeciesPageQuery
import io.github.lishangbu.avalon.catalog.application.query.SpeciesQueryRepository
import io.github.lishangbu.avalon.catalog.domain.*
import io.github.lishangbu.avalon.shared.application.query.Page
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * Catalog 第一阶段手工维护 CRUD 的应用服务入口。
 *
 * 当前主要协调属性定义、属性克制关系、性格定义、道具定义、技能定义、技能参考切片、
 * 特性定义、成长率定义、物种定义、物种进化定义、物种特性关联和物种招式学习关系的读取与写入，
 * 负责把接口层使用的基础数值标识转换为领域标识，并统一处理 not-found 语义。
 */
@ApplicationScoped
class CatalogService(
    private val repository: CatalogRepository,
    private val speciesQueryRepository: SpeciesQueryRepository,
) {
    /**
     * 列出全部属性定义。
     *
     * @return 属性定义列表；若无数据则返回空列表。
     */
    suspend fun listTypeDefinitions(): List<TypeDefinition> = repository.listTypeDefinitions()

    /**
     * 查询单个属性定义。
     *
     * @param id 属性定义主键值。
     * @return 命中的属性定义。
     * @throws CatalogNotFound 当属性定义不存在时抛出。
     */
    suspend fun getTypeDefinition(id: UUID): TypeDefinition =
        repository.findTypeDefinition(TypeDefinitionId(id))
            ?: throw CatalogNotFound("type_definition", id.toString())

    /**
     * 创建属性定义。
     *
     * @param draft 属性定义草稿。
     * @return 已持久化的属性定义。
     */
    suspend fun createTypeDefinition(draft: TypeDefinitionDraft): TypeDefinition =
        repository.createTypeDefinition(draft)

    /**
     * 更新属性定义。
     *
     * @param id 属性定义主键值。
     * @param draft 更新草稿。
     * @return 更新后的属性定义。
     */
    suspend fun updateTypeDefinition(
        id: UUID,
        draft: TypeDefinitionDraft,
    ): TypeDefinition = repository.updateTypeDefinition(TypeDefinitionId(id), draft)

    /**
     * 删除属性定义。
     *
     * @param id 属性定义主键值。
     */
    suspend fun deleteTypeDefinition(id: UUID) {
        repository.deleteTypeDefinition(TypeDefinitionId(id))
    }

    /**
     * 列出全部属性克制关系。
     *
     * @return 属性克制关系列表；若无数据则返回空列表。
     */
    suspend fun listTypeEffectiveness(): List<TypeEffectiveness> = repository.listTypeEffectiveness()

    /**
     * 查询单个属性克制关系。
     *
     * @param id 属性克制关系主键值。
     * @return 命中的属性克制关系。
     * @throws CatalogNotFound 当克制关系不存在时抛出。
     */
    suspend fun getTypeEffectiveness(id: UUID): TypeEffectiveness =
        repository.findTypeEffectiveness(TypeEffectivenessId(id))
            ?: throw CatalogNotFound("type_effectiveness", id.toString())

    /**
     * 创建属性克制关系。
     *
     * @param draft 属性克制关系草稿。
     * @return 已持久化的属性克制关系。
     */
    suspend fun createTypeEffectiveness(draft: TypeEffectivenessDraft): TypeEffectiveness =
        repository.createTypeEffectiveness(draft)

    /**
     * 更新属性克制关系。
     *
     * @param id 属性克制关系主键值。
     * @param draft 更新草稿。
     * @return 更新后的属性克制关系。
     */
    suspend fun updateTypeEffectiveness(
        id: UUID,
        draft: TypeEffectivenessDraft,
    ): TypeEffectiveness = repository.updateTypeEffectiveness(TypeEffectivenessId(id), draft)

    /**
     * 删除属性克制关系。
     *
     * @param id 属性克制关系主键值。
     */
    suspend fun deleteTypeEffectiveness(id: UUID) {
        repository.deleteTypeEffectiveness(TypeEffectivenessId(id))
    }

    /**
     * 列出全部性格定义。
     *
     * @return 性格定义列表；若无数据则返回空列表。
     */
    suspend fun listNatures(): List<Nature> = repository.listNatures()

    /**
     * 查询单个性格定义。
     *
     * @param id 性格定义主键值。
     * @return 命中的性格定义。
     * @throws CatalogNotFound 当性格定义不存在时抛出。
     */
    suspend fun getNature(id: UUID): Nature =
        repository.findNature(NatureId(id))
            ?: throw CatalogNotFound("nature", id.toString())

    /**
     * 创建性格定义。
     *
     * @param draft 性格定义草稿。
     * @return 已持久化的性格定义。
     */
    suspend fun createNature(draft: NatureDraft): Nature = repository.createNature(draft)

    /**
     * 更新性格定义。
     *
     * @param id 性格定义主键值。
     * @param draft 更新草稿。
     * @return 更新后的性格定义。
     */
    suspend fun updateNature(
        id: UUID,
        draft: NatureDraft,
    ): Nature = repository.updateNature(NatureId(id), draft)

    /**
     * 删除性格定义。
     *
     * @param id 性格定义主键值。
     */
    suspend fun deleteNature(id: UUID) {
        repository.deleteNature(NatureId(id))
    }

    /**
     * 列出全部道具定义。
     *
     * @return 道具定义列表；若无数据则返回空列表。
     */
    suspend fun listItems(): List<Item> = repository.listItems()

    /**
     * 查询单个道具定义。
     *
     * @param id 道具定义主键值。
     * @return 命中的道具定义。
     * @throws CatalogNotFound 当道具定义不存在时抛出。
     */
    suspend fun getItem(id: UUID): Item =
        repository.findItem(ItemId(id))
            ?: throw CatalogNotFound("item", id.toString())

    /**
     * 创建道具定义。
     *
     * @param draft 道具定义草稿。
     * @return 已持久化的道具定义。
     */
    suspend fun createItem(draft: ItemDraft): Item = repository.createItem(draft)

    /**
     * 更新道具定义。
     *
     * @param id 道具定义主键值。
     * @param draft 更新草稿。
     * @return 更新后的道具定义。
     */
    suspend fun updateItem(
        id: UUID,
        draft: ItemDraft,
    ): Item = repository.updateItem(ItemId(id), draft)

    /**
     * 删除道具定义。
     *
     * @param id 道具定义主键值。
     */
    suspend fun deleteItem(id: UUID) {
        repository.deleteItem(ItemId(id))
    }

    /**
     * 列出全部特性定义。
     *
     * @return 特性定义列表；若无数据则返回空列表。
     */
    suspend fun listAbilities(): List<Ability> = repository.listAbilities()

    /**
     * 查询单个特性定义。
     *
     * @param id 特性定义主键值。
     * @return 命中的特性定义。
     * @throws CatalogNotFound 当特性定义不存在时抛出。
     */
    suspend fun getAbility(id: UUID): Ability =
        repository.findAbility(AbilityId(id))
            ?: throw CatalogNotFound("ability", id.toString())

    /**
     * 创建特性定义。
     *
     * @param draft 特性定义草稿。
     * @return 已持久化的特性定义。
     */
    suspend fun createAbility(draft: AbilityDraft): Ability = repository.createAbility(draft)

    /**
     * 更新特性定义。
     *
     * @param id 特性定义主键值。
     * @param draft 更新草稿。
     * @return 更新后的特性定义。
     */
    suspend fun updateAbility(
        id: UUID,
        draft: AbilityDraft,
    ): Ability = repository.updateAbility(AbilityId(id), draft)

    /**
     * 删除特性定义。
     *
     * @param id 特性定义主键值。
     */
    suspend fun deleteAbility(id: UUID) {
        repository.deleteAbility(AbilityId(id))
    }

    /**
     * 列出全部技能定义。
     *
     * @return 技能定义列表；若无数据则返回空列表。
     */
    suspend fun listMoves(): List<Move> = repository.listMoves()

    /**
     * 查询单个技能定义。
     *
     * @param id 技能定义主键值。
     * @return 命中的技能定义。
     * @throws CatalogNotFound 当技能定义不存在时抛出。
     */
    suspend fun getMove(id: UUID): Move =
        repository.findMove(MoveId(id))
            ?: throw CatalogNotFound("move", id.toString())

    /**
     * 创建技能定义。
     *
     * @param draft 技能定义草稿。
     * @return 已持久化的技能定义。
     */
    suspend fun createMove(draft: MoveDraft): Move = repository.createMove(draft)

    /**
     * 更新技能定义。
     *
     * @param id 技能定义主键值。
     * @param draft 更新草稿。
     * @return 更新后的技能定义。
     */
    suspend fun updateMove(
        id: UUID,
        draft: MoveDraft,
    ): Move = repository.updateMove(MoveId(id), draft)

    /**
     * 删除技能定义。
     *
     * @param id 技能定义主键值。
     */
    suspend fun deleteMove(id: UUID) {
        repository.deleteMove(MoveId(id))
    }

    /**
     * 列出全部招式分类定义。
     */
    suspend fun listMoveCategories(): List<MoveCategory> = repository.listMoveCategories()

    /**
     * 查询单个招式分类定义。
     */
    suspend fun getMoveCategory(id: UUID): MoveCategory =
        repository.findMoveCategory(MoveCategoryId(id))
            ?: throw CatalogNotFound("move_category", id.toString())

    /**
     * 创建招式分类定义。
     */
    suspend fun createMoveCategory(draft: MoveCategoryDraft): MoveCategory = repository.createMoveCategory(draft)

    /**
     * 更新招式分类定义。
     */
    suspend fun updateMoveCategory(
        id: UUID,
        draft: MoveCategoryDraft,
    ): MoveCategory = repository.updateMoveCategory(MoveCategoryId(id), draft)

    /**
     * 删除招式分类定义。
     */
    suspend fun deleteMoveCategory(id: UUID) {
        repository.deleteMoveCategory(MoveCategoryId(id))
    }

    /**
     * 列出全部招式异常状态定义。
     */
    suspend fun listMoveAilments(): List<MoveAilment> = repository.listMoveAilments()

    /**
     * 查询单个招式异常状态定义。
     */
    suspend fun getMoveAilment(id: UUID): MoveAilment =
        repository.findMoveAilment(MoveAilmentId(id))
            ?: throw CatalogNotFound("move_ailment", id.toString())

    /**
     * 创建招式异常状态定义。
     */
    suspend fun createMoveAilment(draft: MoveAilmentDraft): MoveAilment = repository.createMoveAilment(draft)

    /**
     * 更新招式异常状态定义。
     */
    suspend fun updateMoveAilment(
        id: UUID,
        draft: MoveAilmentDraft,
    ): MoveAilment = repository.updateMoveAilment(MoveAilmentId(id), draft)

    /**
     * 删除招式异常状态定义。
     */
    suspend fun deleteMoveAilment(id: UUID) {
        repository.deleteMoveAilment(MoveAilmentId(id))
    }

    /**
     * 列出全部招式目标定义。
     *
     * @return 招式目标列表；若无数据则返回空列表。
     */
    suspend fun listMoveTargets(): List<MoveTarget> = repository.listMoveTargets()

    /**
     * 查询单个招式目标定义。
     *
     * @param id 招式目标主键值。
     * @return 命中的招式目标定义。
     * @throws CatalogNotFound 当招式目标不存在时抛出。
     */
    suspend fun getMoveTarget(id: UUID): MoveTarget =
        repository.findMoveTarget(MoveTargetId(id))
            ?: throw CatalogNotFound("move_target", id.toString())

    /**
     * 创建招式目标定义。
     *
     * @param draft 招式目标草稿。
     * @return 已持久化的招式目标定义。
     */
    suspend fun createMoveTarget(draft: MoveTargetDraft): MoveTarget = repository.createMoveTarget(draft)

    /**
     * 更新招式目标定义。
     *
     * @param id 招式目标主键值。
     * @param draft 更新草稿。
     * @return 更新后的招式目标定义。
     */
    suspend fun updateMoveTarget(
        id: UUID,
        draft: MoveTargetDraft,
    ): MoveTarget = repository.updateMoveTarget(MoveTargetId(id), draft)

    /**
     * 删除招式目标定义。
     *
     * @param id 招式目标主键值。
     */
    suspend fun deleteMoveTarget(id: UUID) {
        repository.deleteMoveTarget(MoveTargetId(id))
    }

    /**
     * 列出全部招式学习方法定义。
     *
     * @return 招式学习方法定义列表；若无数据则返回空列表。
     */
    suspend fun listMoveLearnMethods(): List<MoveLearnMethod> = repository.listMoveLearnMethods()

    /**
     * 查询单个招式学习方法定义。
     *
     * @param id 招式学习方法主键值。
     * @return 命中的招式学习方法定义。
     * @throws CatalogNotFound 当招式学习方法不存在时抛出。
     */
    suspend fun getMoveLearnMethod(id: UUID): MoveLearnMethod =
        repository.findMoveLearnMethod(MoveLearnMethodId(id))
            ?: throw CatalogNotFound("move_learn_method", id.toString())

    /**
     * 创建招式学习方法定义。
     *
     * @param draft 招式学习方法草稿。
     * @return 已持久化的招式学习方法定义。
     */
    suspend fun createMoveLearnMethod(draft: MoveLearnMethodDraft): MoveLearnMethod =
        repository.createMoveLearnMethod(draft)

    /**
     * 更新招式学习方法定义。
     *
     * @param id 招式学习方法主键值。
     * @param draft 更新草稿。
     * @return 更新后的招式学习方法定义。
     */
    suspend fun updateMoveLearnMethod(
        id: UUID,
        draft: MoveLearnMethodDraft,
    ): MoveLearnMethod = repository.updateMoveLearnMethod(MoveLearnMethodId(id), draft)

    /**
     * 删除招式学习方法定义。
     *
     * @param id 招式学习方法主键值。
     */
    suspend fun deleteMoveLearnMethod(id: UUID) {
        repository.deleteMoveLearnMethod(MoveLearnMethodId(id))
    }

    /**
     * 列出全部成长率定义。
     *
     * @return 成长率定义列表；若无数据则返回空列表。
     */
    suspend fun listGrowthRates(): List<GrowthRate> = repository.listGrowthRates()

    /**
     * 查询单个成长率定义。
     *
     * @param id 成长率定义主键值。
     * @return 命中的成长率定义。
     * @throws CatalogNotFound 当成长率定义不存在时抛出。
     */
    suspend fun getGrowthRate(id: UUID): GrowthRate =
        repository.findGrowthRate(GrowthRateId(id))
            ?: throw CatalogNotFound("growth_rate", id.toString())

    /**
     * 创建成长率定义。
     *
     * @param draft 成长率定义草稿。
     * @return 已持久化的成长率定义。
     */
    suspend fun createGrowthRate(draft: GrowthRateDraft): GrowthRate = repository.createGrowthRate(draft)

    /**
     * 更新成长率定义。
     *
     * @param id 成长率定义主键值。
     * @param draft 更新草稿。
     * @return 更新后的成长率定义。
     */
    suspend fun updateGrowthRate(
        id: UUID,
        draft: GrowthRateDraft,
    ): GrowthRate = repository.updateGrowthRate(GrowthRateId(id), draft)

    /**
     * 删除成长率定义。
     *
     * @param id 成长率定义主键值。
     */
    suspend fun deleteGrowthRate(id: UUID) {
        repository.deleteGrowthRate(GrowthRateId(id))
    }

    /**
     * 列出全部物种进化定义。
     *
     * @return 物种进化定义列表；若无数据则返回空列表。
     */
    suspend fun listSpeciesEvolutions(): List<SpeciesEvolution> = repository.listSpeciesEvolutions()

    /**
     * 查询单个物种进化定义。
     *
     * @param id 物种进化定义主键值。
     * @return 命中的物种进化定义。
     * @throws CatalogNotFound 当物种进化定义不存在时抛出。
     */
    suspend fun getSpeciesEvolution(id: UUID): SpeciesEvolution =
        repository.findSpeciesEvolution(SpeciesEvolutionId(id))
            ?: throw CatalogNotFound("species_evolution", id.toString())

    /**
     * 创建物种进化定义。
     *
     * @param draft 物种进化定义草稿。
     * @return 已持久化的物种进化定义。
     */
    suspend fun createSpeciesEvolution(draft: SpeciesEvolutionDraft): SpeciesEvolution =
        repository.createSpeciesEvolution(draft)

    /**
     * 更新物种进化定义。
     *
     * @param id 物种进化定义主键值。
     * @param draft 更新草稿。
     * @return 更新后的物种进化定义。
     */
    suspend fun updateSpeciesEvolution(
        id: UUID,
        draft: SpeciesEvolutionDraft,
    ): SpeciesEvolution = repository.updateSpeciesEvolution(SpeciesEvolutionId(id), draft)

    /**
     * 删除物种进化定义。
     *
     * @param id 物种进化定义主键值。
     */
    suspend fun deleteSpeciesEvolution(id: UUID) {
        repository.deleteSpeciesEvolution(SpeciesEvolutionId(id))
    }

    /**
     * 列出全部物种特性关联。
     *
     * @return 物种特性关系列表；若无数据则返回空列表。
     */
    suspend fun listSpeciesAbilities(): List<SpeciesAbility> = repository.listSpeciesAbilities()

    /**
     * 查询单个物种特性关联。
     *
     * @param id 物种特性关联主键值。
     * @return 命中的物种特性关联。
     * @throws CatalogNotFound 当物种特性关联不存在时抛出。
     */
    suspend fun getSpeciesAbility(id: UUID): SpeciesAbility =
        repository.findSpeciesAbility(SpeciesAbilityId(id))
            ?: throw CatalogNotFound("species_ability", id.toString())

    /**
     * 创建物种特性关联。
     *
     * @param draft 物种特性关联草稿。
     * @return 已持久化的物种特性关联。
     */
    suspend fun createSpeciesAbility(draft: SpeciesAbilityDraft): SpeciesAbility =
        repository.createSpeciesAbility(draft)

    /**
     * 更新物种特性关联。
     *
     * @param id 物种特性关联主键值。
     * @param draft 更新草稿。
     * @return 更新后的物种特性关联。
     */
    suspend fun updateSpeciesAbility(
        id: UUID,
        draft: SpeciesAbilityDraft,
    ): SpeciesAbility = repository.updateSpeciesAbility(SpeciesAbilityId(id), draft)

    /**
     * 删除物种特性关联。
     *
     * @param id 物种特性关联主键值。
     */
    suspend fun deleteSpeciesAbility(id: UUID) {
        repository.deleteSpeciesAbility(SpeciesAbilityId(id))
    }

    /**
     * 列出全部物种招式学习关系。
     *
     * @return 物种招式学习关系列表；若无数据则返回空列表。
     */
    suspend fun listSpeciesMoveLearnsets(): List<SpeciesMoveLearnset> = repository.listSpeciesMoveLearnsets()

    /**
     * 查询单个物种招式学习关系。
     *
     * @param id 物种招式学习关系主键值。
     * @return 命中的物种招式学习关系。
     * @throws CatalogNotFound 当关系不存在时抛出。
     */
    suspend fun getSpeciesMoveLearnset(id: UUID): SpeciesMoveLearnset =
        repository.findSpeciesMoveLearnset(SpeciesMoveLearnsetId(id))
            ?: throw CatalogNotFound("species_move_learnset", id.toString())

    /**
     * 创建物种招式学习关系。
     *
     * @param draft 物种招式学习关系草稿；当学习方法代码为 `LEVEL-UP` 时，
     * `level` 必须非空且大于 0，其他方法必须为空。
     * @return 已持久化的物种招式学习关系。
     */
    suspend fun createSpeciesMoveLearnset(draft: SpeciesMoveLearnsetDraft): SpeciesMoveLearnset =
        repository.createSpeciesMoveLearnset(draft)

    /**
     * 更新物种招式学习关系。
     *
     * @param id 物种招式学习关系主键值。
     * @param draft 更新草稿；当学习方法代码为 `LEVEL-UP` 时，
     * `level` 必须非空且大于 0，其他方法必须为空。
     * @return 更新后的物种招式学习关系。
     */
    suspend fun updateSpeciesMoveLearnset(
        id: UUID,
        draft: SpeciesMoveLearnsetDraft,
    ): SpeciesMoveLearnset = repository.updateSpeciesMoveLearnset(SpeciesMoveLearnsetId(id), draft)

    /**
     * 删除物种招式学习关系。
     *
     * @param id 物种招式学习关系主键值。
     */
    suspend fun deleteSpeciesMoveLearnset(id: UUID) {
        repository.deleteSpeciesMoveLearnset(SpeciesMoveLearnsetId(id))
    }

    /**
     * 按固定排序分页列出物种定义。
     *
     * 当前保持按 `sortingOrder, id` 的稳定顺序返回结果，避免页间漂移。
     *
     * @param query 物种分页查询条件。
     * @return 当前页物种定义与分页元数据。
     */
    suspend fun pageSpecies(query: SpeciesPageQuery): Page<Species> = speciesQueryRepository.pageSpecies(query)

    /**
     * 查询单个物种定义。
     *
     * @param id 物种定义主键值。
     * @return 命中的物种定义。
     * @throws CatalogNotFound 当物种定义不存在时抛出。
     */
    suspend fun getSpecies(id: UUID): Species =
        repository.findSpecies(SpeciesId(id))
            ?: throw CatalogNotFound("creature_species", id.toString())

    /**
     * 创建物种定义。
     *
     * @param draft 物种定义草稿。
     * @return 已持久化的物种定义。
     */
    suspend fun createSpecies(draft: SpeciesDraft): Species = repository.createSpecies(draft)

    /**
     * 更新物种定义。
     *
     * @param id 物种定义主键值。
     * @param draft 更新草稿。
     * @return 更新后的物种定义。
     */
    suspend fun updateSpecies(
        id: UUID,
        draft: SpeciesDraft,
    ): Species = repository.updateSpecies(SpeciesId(id), draft)

    /**
     * 删除物种定义。
     *
     * @param id 物种定义主键值。
     */
    suspend fun deleteSpecies(id: UUID) {
        repository.deleteSpecies(SpeciesId(id))
    }
}

