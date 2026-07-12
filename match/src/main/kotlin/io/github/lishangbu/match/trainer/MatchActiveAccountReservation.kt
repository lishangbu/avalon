package io.github.lishangbu.match.trainer

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/** 每个账户至多一场 Active Match 的数据库保留记录。 */
@Entity
@Table(name = "match_active_account_reservation")
interface MatchActiveAccountReservation {
	@Id
	/** 被限制为只能参与一场 Active Match 的账户。 */
	val accountId: Long
	/** 当前占用该账户 Active Match 容量的 Match。 */
	val matchId: Long
}
