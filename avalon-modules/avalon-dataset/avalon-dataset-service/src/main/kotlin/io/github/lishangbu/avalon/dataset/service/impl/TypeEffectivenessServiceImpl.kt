package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Type
import io.github.lishangbu.avalon.dataset.entity.TypeEffectivenessEntry
import io.github.lishangbu.avalon.dataset.entity.TypeEffectivenessEntryId
import io.github.lishangbu.avalon.dataset.repository.TypeEffectivenessEntryRepository
import io.github.lishangbu.avalon.dataset.repository.TypeRepository
import io.github.lishangbu.avalon.dataset.service.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

/** 属性相克业务服务实现 */
@Service
class TypeEffectivenessServiceImpl(
    private val typeRepository: TypeRepository,
    private val typeEffectivenessEntryRepository: TypeEffectivenessEntryRepository,
) : TypeEffectivenessService {
    override fun calculate(
        attackingType: String,
        defendingTypes: List<String>,
    ): TypeEffectivenessResult {
        val context = loadContext()
        val attacking = context.requireType(normalizeTypeName(attackingType, "attackingType"), "attackingType")
        val normalizedDefendingTypes = normalizeDefendingTypes(defendingTypes)
        val matchupEvaluations =
            normalizedDefendingTypes.map { defendingType ->
                val defending = context.requireType(defendingType, "defendingTypes")
                MatchupEvaluation(
                    defending = defending,
                    multiplierPercent = context.storedMultipliers[attacking.internalName to defending.internalName],
                )
            }

        val finalMultiplierPercent =
            if (matchupEvaluations.any { it.multiplierPercent == null }) {
                null
            } else {
                matchupEvaluations.fold(TypeEffectivenessMultiplierCodec.ONE_X_PERCENT) { acc, evaluation ->
                    TypeEffectivenessMultiplierCodec.multiplyStoredPercents(acc, requireNotNull(evaluation.multiplierPercent))
                }
            }

        return TypeEffectivenessResult(
            attackingType = attacking.apiView,
            defendingTypes =
                matchupEvaluations.map { evaluation ->
                    TypeEffectivenessMatchup(
                        defendingType = evaluation.defending.apiView,
                        multiplier = TypeEffectivenessMultiplierCodec.decode(evaluation.multiplierPercent),
                        status = cellStatusOf(evaluation.multiplierPercent),
                    )
                },
            finalMultiplier = TypeEffectivenessMultiplierCodec.decode(finalMultiplierPercent),
            status = if (finalMultiplierPercent == null) STATUS_INCOMPLETE else STATUS_COMPLETE,
            effectiveness = effectivenessOf(finalMultiplierPercent),
        )
    }

    override fun getChart(): TypeEffectivenessChart = buildChart(loadContext())

    @Transactional(rollbackFor = [Exception::class])
    override fun upsertMatrix(command: UpsertTypeEffectivenessMatrixCommand): TypeEffectivenessChart {
        val context = loadContext()
        val normalizedCells = normalizeMatrixCells(command.cells)
        normalizedCells.forEach { cell ->
            val attacking = context.requireType(cell.attackingType, "cells.attackingType")
            val defending = context.requireType(cell.defendingType, "cells.defendingType")
            val id =
                TypeEffectivenessEntryId {
                    attackingTypeId = attacking.id
                    defendingTypeId = defending.id
                }
            val multiplierPercent = cell.multiplierPercent
            if (multiplierPercent == null) {
                typeEffectivenessEntryRepository.deleteById(id)
            } else {
                // API 层传入的是自然倍率；真正写入数据库前必须先编码成整数百分比。
                typeEffectivenessEntryRepository.save(
                    TypeEffectivenessEntry {
                        this.id = id
                        this.multiplierPercent = multiplierPercent
                    },
                )
            }
        }
        return buildChart(loadContext())
    }

    private fun buildChart(context: TypeEffectivenessContext): TypeEffectivenessChart {
        val expectedPairs = context.supportedTypes.size * context.supportedTypes.size
        val configuredPairs = context.storedMultipliers.count { (_, multiplierPercent) -> multiplierPercent != null }
        val rows =
            context.supportedTypes.map { attacking ->
                TypeEffectivenessRow(
                    attackingType = attacking.apiView,
                    cells =
                        context.supportedTypes.map { defending ->
                            val multiplierPercent = context.storedMultipliers[attacking.internalName to defending.internalName]
                            TypeEffectivenessCell(
                                defendingType = defending.apiView,
                                multiplier = TypeEffectivenessMultiplierCodec.decode(multiplierPercent),
                                status = cellStatusOf(multiplierPercent),
                            )
                        },
                )
            }
        return TypeEffectivenessChart(
            supportedTypes = context.supportedTypes.map { it.apiView },
            completeness =
                TypeEffectivenessCompleteness(
                    expectedPairs = expectedPairs,
                    configuredPairs = configuredPairs,
                    missingPairs = expectedPairs - configuredPairs,
                ),
            rows = rows,
        )
    }

    private fun loadContext(): TypeEffectivenessContext {
        val supportedTypes =
            typeRepository
                .findAll()
                .map { entity -> entity.toSupportedTypeView() }
                .sortedBy { type -> type.id }

        val supportedTypeIds = supportedTypes.associateBy({ it.id }, { it.internalName })
        val storedMultipliers =
            typeEffectivenessEntryRepository
                .findAll(
                    attackingTypeId = null,
                    defendingTypeId = null,
                    multiplierPercent = null,
                ).mapNotNull { entry ->
                    // 上下文内部始终只缓存数据库中的定点整数，避免后续计算退回到浮点路径。
                    val attackingInternalName = supportedTypeIds[entry.id.attackingTypeId] ?: return@mapNotNull null
                    val defendingInternalName = supportedTypeIds[entry.id.defendingTypeId] ?: return@mapNotNull null
                    (attackingInternalName to defendingInternalName) to entry.multiplierPercent
                }.toMap(LinkedHashMap())

        return TypeEffectivenessContext(
            supportedTypes = supportedTypes,
            typesByInternalName = supportedTypes.associateBy { it.internalName },
            storedMultipliers = storedMultipliers,
        )
    }

    private fun normalizeDefendingTypes(defendingTypes: List<String>): List<String> {
        require(defendingTypes.isNotEmpty()) { "defendingTypes must not be empty" }
        require(defendingTypes.size <= 2) { "defendingTypes supports at most 2 entries" }
        val normalized =
            defendingTypes.map { defendingType ->
                normalizeTypeName(defendingType, "defendingTypes")
            }
        require(normalized.distinct().size == normalized.size) { "defendingTypes must not contain duplicates" }
        return normalized
    }

    private fun normalizeMatrixCells(cells: List<TypeEffectivenessMatrixCellInput>): List<NormalizedMatrixCell> {
        val normalized =
            cells.map { cell ->
                NormalizedMatrixCell(
                    attackingType = normalizeTypeName(cell.attackingType, "cells.attackingType"),
                    defendingType = normalizeTypeName(cell.defendingType, "cells.defendingType"),
                    multiplierPercent = TypeEffectivenessMultiplierCodec.encodeEntryMultiplier(cell.multiplier),
                )
            }
        val keys = normalized.map { it.attackingType to it.defendingType }
        require(keys.distinct().size == keys.size) { "cells must not contain duplicate attackingType/defendingType pairs" }
        return normalized
    }

    private fun normalizeTypeName(
        value: String,
        fieldName: String,
    ): String {
        val normalized = value.trim().lowercase(Locale.ROOT)
        require(normalized.isNotEmpty()) { "$fieldName must not be blank" }
        return normalized
    }

    private fun Type.toSupportedTypeView(): SupportedTypeView {
        val internalName = requireNotNull(internalName) { "Type id=$id is missing internalName" }.trim().lowercase(Locale.ROOT)
        require(internalName.isNotEmpty()) { "Type id=$id has blank internalName" }
        return SupportedTypeView(
            id = id,
            apiView =
                TypeEffectivenessTypeView(
                    internalName = internalName,
                    name = requireNotNull(name) { "Type $internalName is missing name" },
                ),
        )
    }

    private fun cellStatusOf(multiplierPercent: Int?): String =
        if (multiplierPercent == null) {
            STATUS_MISSING
        } else {
            STATUS_CONFIGURED
        }

    private fun effectivenessOf(multiplierPercent: Int?): String =
        when {
            multiplierPercent == null -> EFFECTIVENESS_INCOMPLETE
            multiplierPercent == 0 -> EFFECTIVENESS_IMMUNE
            multiplierPercent < TypeEffectivenessMultiplierCodec.ONE_X_PERCENT -> EFFECTIVENESS_NOT_VERY_EFFECTIVE
            multiplierPercent > TypeEffectivenessMultiplierCodec.ONE_X_PERCENT -> EFFECTIVENESS_SUPER_EFFECTIVE
            else -> EFFECTIVENESS_NORMAL_EFFECTIVE
        }

    private data class MatchupEvaluation(
        val defending: SupportedTypeView,
        val multiplierPercent: Int?,
    )

    private data class SupportedTypeView(
        val id: Long,
        val apiView: TypeEffectivenessTypeView,
    ) {
        val internalName: String
            get() = apiView.internalName
    }

    private data class TypeEffectivenessContext(
        val supportedTypes: List<SupportedTypeView>,
        val typesByInternalName: Map<String, SupportedTypeView>,
        val storedMultipliers: Map<Pair<String, String>, Int?>,
    ) {
        fun requireType(
            internalName: String,
            fieldName: String,
        ): SupportedTypeView = requireNotNull(typesByInternalName[internalName]) { "Unsupported $fieldName: $internalName" }
    }

    private data class NormalizedMatrixCell(
        val attackingType: String,
        val defendingType: String,
        val multiplierPercent: Int?,
    )

    private companion object {
        const val STATUS_COMPLETE: String = "complete"
        const val STATUS_INCOMPLETE: String = "incomplete"
        const val STATUS_CONFIGURED: String = "configured"
        const val STATUS_MISSING: String = "missing"

        const val EFFECTIVENESS_IMMUNE: String = "immune"
        const val EFFECTIVENESS_NOT_VERY_EFFECTIVE: String = "not-very-effective"
        const val EFFECTIVENESS_NORMAL_EFFECTIVE: String = "normal-effective"
        const val EFFECTIVENESS_SUPER_EFFECTIVE: String = "super-effective"
        const val EFFECTIVENESS_INCOMPLETE: String = "incomplete"
    }
}
