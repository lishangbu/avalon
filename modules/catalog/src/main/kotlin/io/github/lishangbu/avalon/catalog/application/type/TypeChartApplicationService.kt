package io.github.lishangbu.avalon.catalog.application.type

import io.github.lishangbu.avalon.catalog.domain.CatalogBadRequest
import io.github.lishangbu.avalon.catalog.domain.TypeDefinition
import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionId
import io.github.lishangbu.avalon.catalog.domain.TypeEffectiveness
import io.github.lishangbu.avalon.catalog.domain.TypeEffectivenessDraft
import io.github.lishangbu.avalon.catalog.domain.type.TypeChart
import io.github.lishangbu.avalon.catalog.domain.type.TypeChartRepository
import io.github.lishangbu.avalon.catalog.domain.type.TypeDefinitionRepository
import jakarta.enterprise.context.ApplicationScoped
import java.math.BigDecimal

/**
 * 属性克制矩阵应用服务。
 *
 * 该服务把属性矩阵视为一个整体能力来读写，负责完整性校验、稳定排序和整表替换。
 */
@ApplicationScoped
class TypeChartApplicationService(
    private val typeDefinitionRepository: TypeDefinitionRepository,
    private val typeChartRepository: TypeChartRepository,
) {
    /**
     * 读取当前属性矩阵快照。
     */
    suspend fun getTypeChart(): TypeChart {
        val types = sortedTypes(typeDefinitionRepository.listTypeDefinitions())
        val entries = sortedEntries(typeChartRepository.listEntries(), types)
        return TypeChart(types = types, entries = entries)
    }

    /**
     * 用一整批关系项替换当前属性矩阵。
     */
    suspend fun replaceTypeChart(entries: List<TypeEffectivenessDraft>): TypeChart {
        val types = sortedTypes(typeDefinitionRepository.listTypeDefinitions())
        validateTypeChart(entries, types)
        typeChartRepository.replaceEntries(entries)
        return getTypeChart()
    }

    private fun validateTypeChart(
        entries: List<TypeEffectivenessDraft>,
        types: List<TypeDefinition>,
    ) {
        if (types.isEmpty()) {
            throw CatalogBadRequest("type chart can only be updated after creating at least one type definition.")
        }

        val typeIds = types.map(TypeDefinition::id).toSet()
        val providedPairs = entries.map { it.attackingTypeId to it.defendingTypeId }

        if (providedPairs.size != providedPairs.toSet().size) {
            throw CatalogBadRequest("type chart contains duplicate attackingTypeId and defendingTypeId combinations.")
        }

        val invalidTypeReferences =
            entries.any { draft ->
                draft.attackingTypeId !in typeIds || draft.defendingTypeId !in typeIds
            }
        if (invalidTypeReferences) {
            throw CatalogBadRequest("type chart entries must reference existing type definitions.")
        }

        val invalidMultiplier =
            entries.any { draft ->
                normalizeMultiplier(draft.multiplier) !in ALLOWED_MULTIPLIERS
            }
        if (invalidMultiplier) {
            throw CatalogBadRequest("type chart multiplier must be one of 0, 0.5, 1 or 2.")
        }

        val expectedPairs =
            typeIds.flatMap { attackingTypeId ->
                typeIds.map { defendingTypeId -> attackingTypeId to defendingTypeId }
            }.toSet()

        if (providedPairs.toSet() != expectedPairs) {
            throw CatalogBadRequest("type chart must provide exactly one multiplier for every attacking and defending type combination.")
        }
    }

    private fun sortedTypes(types: List<TypeDefinition>): List<TypeDefinition> =
        types.sortedWith(compareBy<TypeDefinition>({ it.sortingOrder }, { it.id.value }))

    private fun sortedEntries(
        entries: List<TypeEffectiveness>,
        types: List<TypeDefinition>,
    ): List<TypeEffectiveness> {
        val orderByTypeId =
            types.mapIndexed { index, typeDefinition -> typeDefinition.id to index }.toMap()
        return entries.sortedWith(
            compareBy<TypeEffectiveness>(
                { orderByTypeId[it.attackingType.id] ?: Int.MAX_VALUE },
                { orderByTypeId[it.defendingType.id] ?: Int.MAX_VALUE },
                { it.id.value },
            ),
        )
    }

    private fun normalizeMultiplier(multiplier: BigDecimal): BigDecimal = multiplier.stripTrailingZeros()

    private companion object {
        private val ALLOWED_MULTIPLIERS =
            setOf(
                BigDecimal.ZERO.stripTrailingZeros(),
                BigDecimal("0.5").stripTrailingZeros(),
                BigDecimal.ONE.stripTrailingZeros(),
                BigDecimal("2").stripTrailingZeros(),
            )
    }
}
