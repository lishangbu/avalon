package io.github.lishangbu.avalon.catalog.domain

/**
 * Catalog 第一阶段手工维护 CRUD 的仓储契约。
 *
 * 当前主要负责 `type_definition`、`type_effectiveness`、`nature`、`item`、`move` 及其参考切片
 * (`move_category` / `move_ailment` / `move_target` / `move_learn_method`)，
 * 还有 `ability`、`species`、`growth_rate`、`species_evolution`、`species_ability`
 * 与 `species_move_learnset` 的 canonical write model 读写边界。
 * 上层应用服务只依赖这里暴露的领域类型，不直接依赖 SQL 行模型或表结构细节。
 */
interface CatalogRepository {
    /**
     * 列出全部性格定义。
     *
     * @return 已持久化的性格列表；若无数据则返回空列表。
     */
    suspend fun listNatures(): List<Nature>

    /**
     * 按标识查询性格定义。
     *
     * @param id 性格定义标识。
     * @return 命中的性格；如果不存在则返回 `null`。
     */
    suspend fun findNature(id: NatureId): Nature?

    /**
     * 创建性格定义。
     *
     * @param draft 性格草稿，包含业务 code、展示名和增减属性配置。
     * @return 已持久化的性格定义。
     */
    suspend fun createNature(draft: NatureDraft): Nature

    /**
     * 更新性格定义。
     *
     * @param id 要更新的性格定义标识。
     * @param draft 更新草稿，承载新的展示属性和增减属性配置。
     * @return 更新后的性格定义。
     */
    suspend fun updateNature(
        id: NatureId,
        draft: NatureDraft,
    ): Nature

    /**
     * 删除性格定义。
     *
     * @param id 要删除的性格定义标识。
     */
    suspend fun deleteNature(id: NatureId)

    /**
     * 列出全部道具定义。
     *
     * @return 已持久化的道具列表；若无数据则返回空列表。
     */
    suspend fun listItems(): List<Item>

    /**
     * 按标识查询道具定义。
     *
     * @param id 道具定义标识。
     * @return 命中的道具；如果不存在则返回 `null`。
     */
    suspend fun findItem(id: ItemId): Item?

    /**
     * 创建道具定义。
     *
     * @param draft 道具草稿，包含业务 code、分类码和展示属性。
     * @return 已持久化的道具定义。
     */
    suspend fun createItem(draft: ItemDraft): Item

    /**
     * 更新道具定义。
     *
     * @param id 要更新的道具定义标识。
     * @param draft 更新草稿，承载新的分类码和展示属性。
     * @return 更新后的道具定义。
     */
    suspend fun updateItem(
        id: ItemId,
        draft: ItemDraft,
    ): Item

    /**
     * 删除道具定义。
     *
     * @param id 要删除的道具定义标识。
     */
    suspend fun deleteItem(id: ItemId)

    /**
     * 列出全部特性定义。
     *
     * @return 已持久化的特性列表；若无数据则返回空列表。
     */
    suspend fun listAbilities(): List<Ability>

    /**
     * 按标识查询特性定义。
     *
     * @param id 特性定义标识。
     * @return 命中的特性；如果不存在则返回 `null`。
     */
    suspend fun findAbility(id: AbilityId): Ability?

    /**
     * 创建特性定义。
     *
     * @param draft 特性草稿，包含业务 code、展示名和说明。
     * @return 已持久化的特性定义。
     */
    suspend fun createAbility(draft: AbilityDraft): Ability

    /**
     * 更新特性定义。
     *
     * @param id 要更新的特性定义标识。
     * @param draft 更新草稿，承载新的展示属性和可见状态。
     * @return 更新后的特性定义。
     */
    suspend fun updateAbility(
        id: AbilityId,
        draft: AbilityDraft,
    ): Ability

    /**
     * 删除特性定义。
     *
     * @param id 要删除的特性定义标识。
     */
    suspend fun deleteAbility(id: AbilityId)

    /**
     * 列出全部技能定义。
     *
     * @return 已持久化的技能列表；若无数据则返回空列表。
     */
    suspend fun listMoves(): List<Move>

    /**
     * 按标识查询技能定义。
     *
     * @param id 技能定义标识。
     * @return 命中的技能；如果不存在则返回 `null`。
     */
    suspend fun findMove(id: MoveId): Move?

    /**
     * 创建技能定义。
     *
     * @param draft 技能草稿，包含业务 code、属性、分类和基础战斗参数。
     * @return 已持久化的技能定义。
     */
    suspend fun createMove(draft: MoveDraft): Move

    /**
     * 更新技能定义。
     *
     * @param id 要更新的技能定义标识。
     * @param draft 更新草稿，承载新的属性、分类和基础战斗参数。
     * @return 更新后的技能定义。
     */
    suspend fun updateMove(
        id: MoveId,
        draft: MoveDraft,
    ): Move

    /**
     * 删除技能定义。
     *
     * @param id 要删除的技能定义标识。
     */
    suspend fun deleteMove(id: MoveId)

    /**
     * 列出全部招式分类定义。
     *
     * @return 已持久化的招式分类列表；若无数据则返回空列表。
     */
    suspend fun listMoveCategories(): List<MoveCategory>

    /**
     * 按标识查询招式分类定义。
     *
     * @param id 招式分类标识。
     * @return 命中的招式分类；如果不存在则返回 `null`。
     */
    suspend fun findMoveCategory(id: MoveCategoryId): MoveCategory?

    /**
     * 创建招式分类定义。
     *
     * @param draft 招式分类草稿，包含业务码、名称和展示顺序。
     * @return 已持久化的招式分类定义。
     */
    suspend fun createMoveCategory(draft: MoveCategoryDraft): MoveCategory

    /**
     * 更新招式分类定义。
     *
     * @param id 要更新的招式分类标识。
     * @param draft 更新草稿，承载新的展示名称和可见状态。
     * @return 更新后的招式分类定义。
     */
    suspend fun updateMoveCategory(
        id: MoveCategoryId,
        draft: MoveCategoryDraft,
    ): MoveCategory

    /**
     * 删除招式分类定义。
     *
     * @param id 要删除的招式分类标识。
     */
    suspend fun deleteMoveCategory(id: MoveCategoryId)

    /**
     * 列出全部招式异常状态定义。
     *
     * @return 已持久化的招式异常状态列表；若无数据则返回空列表。
     */
    suspend fun listMoveAilments(): List<MoveAilment>

    /**
     * 按标识查询招式异常状态定义。
     *
     * @param id 招式异常状态标识。
     * @return 命中的招式异常状态；如果不存在则返回 `null`。
     */
    suspend fun findMoveAilment(id: MoveAilmentId): MoveAilment?

    /**
     * 创建招式异常状态定义。
     *
     * @param draft 招式异常状态草稿，包含业务码、名称和展示顺序。
     * @return 已持久化的招式异常状态定义。
     */
    suspend fun createMoveAilment(draft: MoveAilmentDraft): MoveAilment

    /**
     * 更新招式异常状态定义。
     *
     * @param id 要更新的招式异常状态标识。
     * @param draft 更新草稿，承载新的展示名称和可见状态。
     * @return 更新后的招式异常状态定义。
     */
    suspend fun updateMoveAilment(
        id: MoveAilmentId,
        draft: MoveAilmentDraft,
    ): MoveAilment

    /**
     * 删除招式异常状态定义。
     *
     * @param id 要删除的招式异常状态标识。
     */
    suspend fun deleteMoveAilment(id: MoveAilmentId)

    /**
     * 列出全部招式目标定义。
     *
     * @return 已持久化的招式目标列表；若无数据则返回空列表。
     */
    suspend fun listMoveTargets(): List<MoveTarget>

    /**
     * 按标识查询招式目标定义。
     *
     * @param id 招式目标标识。
     * @return 命中的招式目标；如果不存在则返回 `null`。
     */
    suspend fun findMoveTarget(id: MoveTargetId): MoveTarget?

    /**
     * 创建招式目标定义。
     *
     * @param draft 招式目标草稿，包含业务 code、名称和展示顺序。
     * @return 已持久化的招式目标定义。
     */
    suspend fun createMoveTarget(draft: MoveTargetDraft): MoveTarget

    /**
     * 更新招式目标定义。
     *
     * @param id 要更新的招式目标标识。
     * @param draft 更新草稿，承载新的展示名称、说明和可见状态。
     * @return 更新后的招式目标定义。
     */
    suspend fun updateMoveTarget(
        id: MoveTargetId,
        draft: MoveTargetDraft,
    ): MoveTarget

    /**
     * 删除招式目标定义。
     *
     * @param id 要删除的招式目标标识。
     */
    suspend fun deleteMoveTarget(id: MoveTargetId)

    /**
     * 列出全部招式学习方法定义。
     *
     * @return 已持久化的招式学习方法列表；若无数据则返回空列表。
     */
    suspend fun listMoveLearnMethods(): List<MoveLearnMethod>

    /**
     * 按标识查询招式学习方法定义。
     *
     * @param id 招式学习方法标识。
     * @return 命中的招式学习方法；如果不存在则返回 `null`。
     */
    suspend fun findMoveLearnMethod(id: MoveLearnMethodId): MoveLearnMethod?

    /**
     * 创建招式学习方法定义。
     *
     * @param draft 招式学习方法草稿，包含业务码、名称和展示顺序。
     * @return 已持久化的招式学习方法定义。
     */
    suspend fun createMoveLearnMethod(draft: MoveLearnMethodDraft): MoveLearnMethod

    /**
     * 更新招式学习方法定义。
     *
     * @param id 要更新的招式学习方法标识。
     * @param draft 更新草稿，承载新的展示名称和可见状态。
     * @return 更新后的招式学习方法定义。
     */
    suspend fun updateMoveLearnMethod(
        id: MoveLearnMethodId,
        draft: MoveLearnMethodDraft,
    ): MoveLearnMethod

    /**
     * 删除招式学习方法定义。
     *
     * @param id 要删除的招式学习方法标识。
     */
    suspend fun deleteMoveLearnMethod(id: MoveLearnMethodId)

    /**
     * 列出全部成长率定义。
     *
     * @return 已持久化的成长率列表；若无数据则返回空列表。
     */
    suspend fun listGrowthRates(): List<GrowthRate>

    /**
     * 按标识查询成长率定义。
     *
     * @param id 成长率定义标识。
     * @return 命中的成长率；如果不存在则返回 `null`。
     */
    suspend fun findGrowthRate(id: GrowthRateId): GrowthRate?

    /**
     * 创建成长率定义。
     *
     * @param draft 成长率草稿，包含业务 code、展示属性和公式码。
     * @return 已持久化的成长率定义。
     */
    suspend fun createGrowthRate(draft: GrowthRateDraft): GrowthRate

    /**
     * 更新成长率定义。
     *
     * @param id 要更新的成长率定义标识。
     * @param draft 更新草稿，承载新的展示属性和公式码。
     * @return 更新后的成长率定义。
     */
    suspend fun updateGrowthRate(
        id: GrowthRateId,
        draft: GrowthRateDraft,
    ): GrowthRate

    /**
     * 删除成长率定义。
     *
     * @param id 要删除的成长率定义标识。
     */
    suspend fun deleteGrowthRate(id: GrowthRateId)

    /**
     * 列出全部物种进化定义。
     *
     * @return 已持久化的进化定义列表；若无数据则返回空列表。
     */
    suspend fun listSpeciesEvolutions(): List<SpeciesEvolution>

    /**
     * 按标识查询物种进化定义。
     *
     * @param id 物种进化定义标识。
     * @return 命中的进化定义；如果不存在则返回 `null`。
     */
    suspend fun findSpeciesEvolution(id: SpeciesEvolutionId): SpeciesEvolution?

    /**
     * 创建物种进化定义。
     *
     * @param draft 进化草稿，包含起点、终点和触发方式。
     * @return 已持久化的进化定义。
     */
    suspend fun createSpeciesEvolution(draft: SpeciesEvolutionDraft): SpeciesEvolution

    /**
     * 更新物种进化定义。
     *
     * @param id 要更新的进化定义标识。
     * @param draft 更新草稿，承载新的起点、终点和触发方式。
     * @return 更新后的进化定义。
     */
    suspend fun updateSpeciesEvolution(
        id: SpeciesEvolutionId,
        draft: SpeciesEvolutionDraft,
    ): SpeciesEvolution

    /**
     * 删除物种进化定义。
     *
     * @param id 要删除的进化定义标识。
     */
    suspend fun deleteSpeciesEvolution(id: SpeciesEvolutionId)

    /**
     * 列出全部物种特性关联。
     *
     * @return 已持久化的物种特性关系列表；若无数据则返回空列表。
     */
    suspend fun listSpeciesAbilities(): List<SpeciesAbility>

    /**
     * 按标识查询物种特性关联。
     *
     * @param id 物种特性关联标识。
     * @return 命中的物种特性关联；如果不存在则返回 `null`。
     */
    suspend fun findSpeciesAbility(id: SpeciesAbilityId): SpeciesAbility?

    /**
     * 创建物种特性关联。
     *
     * @param draft 物种特性关联草稿，包含物种、特性和槽位类型。
     * @return 已持久化的物种特性关联。
     */
    suspend fun createSpeciesAbility(draft: SpeciesAbilityDraft): SpeciesAbility

    /**
     * 更新物种特性关联。
     *
     * @param id 要更新的物种特性关联标识。
     * @param draft 更新草稿，承载新的特性引用或槽位类型。
     * @return 更新后的物种特性关联。
     */
    suspend fun updateSpeciesAbility(
        id: SpeciesAbilityId,
        draft: SpeciesAbilityDraft,
    ): SpeciesAbility

    /**
     * 删除物种特性关联。
     *
     * @param id 要删除的物种特性关联标识。
     */
    suspend fun deleteSpeciesAbility(id: SpeciesAbilityId)

    /**
     * 列出全部物种招式学习关系。
     *
     * @return 已持久化的物种招式学习关系列表；若无数据则返回空列表。
     */
    suspend fun listSpeciesMoveLearnsets(): List<SpeciesMoveLearnset>

    /**
     * 按标识查询物种招式学习关系。
     *
     * @param id 物种招式学习关系标识。
     * @return 命中的物种招式学习关系；如果不存在则返回 `null`。
     */
    suspend fun findSpeciesMoveLearnset(id: SpeciesMoveLearnsetId): SpeciesMoveLearnset?

    /**
     * 创建物种招式学习关系。
     *
     * @param draft 物种招式学习关系草稿，包含物种、招式、学习方法和可选等级；
     * 当学习方法代码为 `LEVEL-UP` 时，`level` 必须非空且大于 0，其他方法必须为空。
     * @return 已持久化的物种招式学习关系。
     */
    suspend fun createSpeciesMoveLearnset(draft: SpeciesMoveLearnsetDraft): SpeciesMoveLearnset

    /**
     * 更新物种招式学习关系。
     *
     * @param id 要更新的物种招式学习关系标识。
     * @param draft 更新草稿，承载新的招式、学习方法或等级门槛；
     * 当学习方法代码为 `LEVEL-UP` 时，`level` 必须非空且大于 0，其他方法必须为空。
     * @return 更新后的物种招式学习关系。
     */
    suspend fun updateSpeciesMoveLearnset(
        id: SpeciesMoveLearnsetId,
        draft: SpeciesMoveLearnsetDraft,
    ): SpeciesMoveLearnset

    /**
     * 删除物种招式学习关系。
     *
     * @param id 要删除的物种招式学习关系标识。
     */
    suspend fun deleteSpeciesMoveLearnset(id: SpeciesMoveLearnsetId)

    /**
     * 按标识查询物种定义。
     *
     * @param id 物种定义标识。
     * @return 命中的物种；如果不存在则返回 `null`。
     */
    suspend fun findSpecies(id: SpeciesId): Species?

    /**
     * 创建物种定义。
     *
     * @param draft 物种草稿，包含图鉴号、主副属性和基础种族值。
     * @return 已持久化的物种定义。
     */
    suspend fun createSpecies(draft: SpeciesDraft): Species

    /**
     * 更新物种定义。
     *
     * @param id 要更新的物种定义标识。
     * @param draft 更新草稿，承载新的图鉴号、主副属性和基础种族值。
     * @return 更新后的物种定义。
     */
    suspend fun updateSpecies(
        id: SpeciesId,
        draft: SpeciesDraft,
    ): Species

    /**
     * 删除物种定义。
     *
     * @param id 要删除的物种定义标识。
     */
    suspend fun deleteSpecies(id: SpeciesId)
}
