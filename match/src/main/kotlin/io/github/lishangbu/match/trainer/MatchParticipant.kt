package io.github.lishangbu.match.trainer

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/** Active Match 查询中连接账户、Match 与 Trainer 的参与者记录。 */
@Entity
@Table(name = "match_participant")
interface MatchParticipant {
	@Id
	/** Match 与 Trainer 组成的复合主键。 */
	val id: MatchParticipantId
	/** 参与 Trainer 所属的 OAuth 账户，用于账户级 Active Match 约束。 */
	val accountId: Long
	/** 参与方不可变 Team Snapshot。 */
	val snapshotId: Long
	/** Battle Session 中稳定映射到 side-1/side-2 的一基 side。 */
	val side: Int
	/** Match 创建时冻结的公开 Trainer 名称。 */
	val displayName: String
}
