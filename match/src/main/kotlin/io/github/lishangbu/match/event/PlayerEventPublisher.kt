package io.github.lishangbu.match.event

import io.github.lishangbu.match.trainer.MatchChallenge
import io.github.lishangbu.match.trainer.MatchParticipant
import io.github.lishangbu.match.trainer.challengedTrainerId
import io.github.lishangbu.match.trainer.challengerTrainerId
import io.github.lishangbu.match.trainer.id
import io.github.lishangbu.match.trainer.matchId
import io.github.lishangbu.match.trainer.trainerId
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/** 在 REST 命令成功提交后定位资源双方，并向各自连接发送最小失效通知。 */
@Component
class PlayerEventPublisher(private val sqlClient: KSqlClient, private val hub: PlayerEventHub) {
	fun challengeChanged(challengeId: Long, revision: Long) {
		afterCommit {
		 val trainers = sqlClient.createQuery(MatchChallenge::class) {
			where(table.id eq challengeId)
			select(table.challengerTrainerId, table.challengedTrainerId)
		}.execute().singleOrNull() ?: return@afterCommit
		hub.publish(listOf(trainers._1, trainers._2), PlayerEvent("CHALLENGE_CHANGED", challengeId.toString(), revision))
		}
	}

	fun matchChanged(matchId: Long, revision: Long) {
		afterCommit {
		 val trainers = sqlClient.createQuery(MatchParticipant::class) {
			where(table.id.matchId eq matchId)
			select(table.id.trainerId)
		}.execute()
		hub.publish(trainers, PlayerEvent("MATCH_CHANGED", matchId.toString(), revision))
		}
	}

	/** 领域事务内产生的失效通知只在提交后可见；事务外的控制器调用则立即发送。 */
	private fun afterCommit(action: () -> Unit) {
		// 实时通知只是可丢失的缓存失效提示，发送失败不能把已成功的领域命令伪装成失败。
		val bestEffort = { runCatching(action); Unit }
		if (!TransactionSynchronizationManager.isActualTransactionActive()) return bestEffort()
		TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
			override fun afterCommit() = bestEffort()
		})
	}
}
