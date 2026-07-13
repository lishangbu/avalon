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

/** 在 REST 命令成功提交后定位资源双方，并向各自连接发送最小失效通知。 */
@Component
class PlayerEventPublisher(private val sqlClient: KSqlClient, private val hub: PlayerEventHub) {
	fun challengeChanged(challengeId: Long, revision: Long) {
		val trainers = sqlClient.createQuery(MatchChallenge::class) {
			where(table.id eq challengeId)
			select(table.challengerTrainerId, table.challengedTrainerId)
		}.execute().singleOrNull() ?: return
		hub.publish(listOf(trainers._1, trainers._2), PlayerEvent("CHALLENGE_CHANGED", challengeId.toString(), revision))
	}

	fun matchChanged(matchId: Long, revision: Long) {
		val trainers = sqlClient.createQuery(MatchParticipant::class) {
			where(table.id.matchId eq matchId)
			select(table.id.trainerId)
		}.execute()
		hub.publish(trainers, PlayerEvent("MATCH_CHANGED", matchId.toString(), revision))
	}
}
