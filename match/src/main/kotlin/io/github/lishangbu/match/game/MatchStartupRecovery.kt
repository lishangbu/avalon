package io.github.lishangbu.match.game

import io.github.lishangbu.match.trainer.MatchActiveAccountReservation
import io.github.lishangbu.match.trainer.matchId
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.plus
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant

/** 应用启动时收敛无法恢复的内存 Battle Runtime，并释放账户容量。 */
class MatchStartupRecovery(
	private val sqlClient: KSqlClient,
	transactionManager: PlatformTransactionManager,
	private val clock: Clock = Clock.systemUTC(),
) {
	private val transaction = TransactionTemplate(transactionManager)

	fun recover() {
		transaction.execute {
			val games = sqlClient.createQuery(MatchGame::class) {
				where(table.status valueIn listOf(MatchStatus.STARTING, MatchStatus.ACTIVE))
				select(table.id, table.status)
			}.forUpdate().execute()
			if (games.isEmpty()) return@execute
			val now = Instant.now(clock)
			// Runtime 只驻留于当前进程；重启后 ACTIVE 也不能假装仍可继续。
			listOf(
				MatchStatus.STARTING to MatchInterruptionReason.START_FAILED,
				MatchStatus.ACTIVE to MatchInterruptionReason.RUNTIME_LOST,
			).forEach { (status, reason) ->
				sqlClient.createUpdate(MatchGame::class) {
					where(table.id valueIn games.map { it._1 }, table.status eq status)
					set(table.status, MatchStatus.INTERRUPTED)
					set(table.interruptionReason, reason)
					set(table.endedAt, now)
					set(table.revision, table.revision + 1)
					set(table.updatedAt, now)
				}.execute()
			}
			sqlClient.createDelete(MatchActiveAccountReservation::class) {
				where(table.matchId valueIn games.map { it._1 })
			}.execute()
		}
	}
}
