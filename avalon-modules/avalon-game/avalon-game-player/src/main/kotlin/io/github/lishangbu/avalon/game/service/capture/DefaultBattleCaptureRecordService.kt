package io.github.lishangbu.avalon.game.service.capture

import io.github.lishangbu.avalon.game.battle.engine.capture.CaptureFormulaResult
import io.github.lishangbu.avalon.game.entity.BattleCaptureRecord
import io.github.lishangbu.avalon.game.repository.BattleCaptureRecordRepository
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

@Service
open class DefaultBattleCaptureRecordService(
    private val battleCaptureRecordRepository: BattleCaptureRecordRepository,
) {
    @Transactional(rollbackFor = [Exception::class])
    open fun record(
        context: PreparedCaptureContext,
        result: CaptureFormulaResult,
        capturedCreature: CapturedCreatureSummary?,
    ) {
        battleCaptureRecordRepository.save(
            BattleCaptureRecord {
                sessionId = context.sessionId
                playerId = context.playerId
                targetUnitId = context.targetUnitId
                ballItemId = context.ballItemId
                creatureId = context.targetMetadata.creatureId
                creatureSpeciesId = context.targetMetadata.creatureSpeciesId
                captureRate = context.targetMetadata.captureRate ?: 0
                currentHp = context.targetUnit.currentHp
                maxHp = context.targetUnit.maxHp
                statusId = context.targetUnit.statusId
                shakes = result.shakes
                success = result.success
                reason = result.reason
                finalRate = BigDecimal.valueOf(result.finalRate).setScale(6, RoundingMode.HALF_UP)
                ownedCreatureId = capturedCreature?.ownedCreatureId?.toLongOrNull()
                createdAt = Instant.now()
            },
            SaveMode.INSERT_ONLY,
        )
    }
}
