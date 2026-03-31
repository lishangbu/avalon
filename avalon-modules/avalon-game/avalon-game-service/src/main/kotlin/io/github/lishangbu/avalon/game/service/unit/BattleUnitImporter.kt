package io.github.lishangbu.avalon.game.service.unit

/** 战斗单位导入契约。 */
fun interface BattleUnitImporter {
    fun importUnit(request: BattleUnitImportRequest): BattleUnitImportResult
}
