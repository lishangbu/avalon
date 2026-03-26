package io.github.lishangbu.avalon.dataset.service

import java.math.BigDecimal

/** 前端使用的属性基础信息 */
data class TypeEffectivenessTypeView(
    val internalName: String,
    val name: String,
)

/** 属性倍率查询中的防守方明细 */
data class TypeEffectivenessMatchup(
    val defendingType: TypeEffectivenessTypeView,
    /** 使用精确十进制返回倍率，避免浮点序列化与比较误差。 */
    val multiplier: BigDecimal?,
    val status: String,
)

/** 属性倍率查询结果 */
data class TypeEffectivenessResult(
    val attackingType: TypeEffectivenessTypeView,
    val defendingTypes: List<TypeEffectivenessMatchup>,
    /** 双属性最终倍率同样使用精确十进制返回。 */
    val finalMultiplier: BigDecimal?,
    val status: String,
    val effectiveness: String,
)

/** 矩阵完整度统计 */
data class TypeEffectivenessCompleteness(
    val expectedPairs: Int,
    val configuredPairs: Int,
    val missingPairs: Int,
)

/** 属性相克矩阵中的单元格 */
data class TypeEffectivenessCell(
    val defendingType: TypeEffectivenessTypeView,
    /** 单元格倍率使用精确十进制返回。 */
    val multiplier: BigDecimal?,
    val status: String,
)

/** 属性相克矩阵中的单行 */
data class TypeEffectivenessRow(
    val attackingType: TypeEffectivenessTypeView,
    val cells: List<TypeEffectivenessCell>,
)

/** 完整属性相克矩阵 */
data class TypeEffectivenessChart(
    val supportedTypes: List<TypeEffectivenessTypeView>,
    val completeness: TypeEffectivenessCompleteness,
    val rows: List<TypeEffectivenessRow>,
)

/** 批量修改矩阵时的单元格输入 */
data class TypeEffectivenessMatrixCellInput(
    val attackingType: String,
    val defendingType: String,
    /**
     * 外部 API 仍然使用自然倍率语义，例如 0.5、1、2。
     * 服务层会在入库前将其编码为定点整数。
     */
    val multiplier: BigDecimal?,
)

/** 批量修改矩阵命令 */
data class UpsertTypeEffectivenessMatrixCommand(
    val cells: List<TypeEffectivenessMatrixCellInput> = emptyList(),
)
