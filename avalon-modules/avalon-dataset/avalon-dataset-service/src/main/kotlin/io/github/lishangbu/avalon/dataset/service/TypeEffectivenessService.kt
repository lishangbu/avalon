package io.github.lishangbu.avalon.dataset.service

/** 属性相克业务服务 */
interface TypeEffectivenessService {
    /** 计算攻击属性对一个或两个防守属性的最终倍率 */
    fun calculate(
        attackingType: String,
        defendingTypes: List<String>,
    ): TypeEffectivenessResult

    /** 获取前端可直接渲染的完整属性相克矩阵 */
    fun getChart(): TypeEffectivenessChart

    /** 批量补录、更新或清空属性相克单元格 */
    fun upsertMatrix(command: UpsertTypeEffectivenessMatrixCommand): TypeEffectivenessChart
}
