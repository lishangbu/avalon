package io.github.lishangbu.match.trainer

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class JdbcTrainerStore(private val jdbc: JdbcClient) : TrainerStore {
	override fun lockAccount(accountId: Long) {
		jdbc.sql("select pg_advisory_xact_lock(:accountId)").param("accountId", accountId).query { _, _ -> Unit }.single()
	}
	override fun findByCommand(accountId: Long, commandId: String): TrainerRecord? =
		queryOne("select * from match_trainer where account_id = :accountId and command_id = :commandId", mapOf("accountId" to accountId, "commandId" to commandId))

	override fun findById(accountId: Long, trainerId: Long): TrainerRecord? =
		queryOne("select * from match_trainer where account_id = :accountId and id = :trainerId", mapOf("accountId" to accountId, "trainerId" to trainerId))

	override fun list(accountId: Long): List<TrainerRecord> = jdbc.sql(
		"select * from match_trainer where account_id = :accountId order by created_at, id",
	).param("accountId", accountId).query(::map).list()

	override fun countActive(accountId: Long): Int = jdbc.sql(
		"select count(*) from match_trainer where account_id = :accountId and archived_at is null",
	).param("accountId", accountId).query(Int::class.java).single()

	override fun hasBlockingActivity(accountId: Long, trainerId: Long): Boolean = jdbc.sql(
		"""select exists(
			select 1 from match_active_account_reservation r
			join match_participant p on p.match_id = r.match_id
			where r.account_id = :accountId and p.trainer_id = :trainerId
		)""",
	).params(mapOf("accountId" to accountId, "trainerId" to trainerId)).query(Boolean::class.java).single()

	override fun enabledSensitiveNameRules(): List<SensitiveNameRule> = jdbc.sql(
		"select normalized_term, match_type from match_sensitive_name_rule where enabled = true order by id",
	).query { rs, _ -> SensitiveNameRule(rs.getString(1), SensitiveNameMatchType.valueOf(rs.getString(2))) }.list()

	override fun insert(record: TrainerRecord): TrainerRecord {
		jdbc.sql(
			"""insert into match_trainer
			(id, account_id, display_name, display_name_key, command_id, revision)
			values (:id, :accountId, :displayName, :displayNameKey, :commandId, :revision)""",
		).params(
			mapOf("id" to record.id, "accountId" to record.accountId, "displayName" to record.displayName,
				"displayNameKey" to record.displayNameKey, "commandId" to record.commandId, "revision" to record.revision),
		).update()
		return record
	}

	override fun archive(accountId: Long, trainerId: Long, expectedRevision: Long, archivedAt: java.time.Instant): TrainerRecord? {
		jdbc.sql("""update match_challenge set status = 'CANCELLED', cancellation_reason = 'TRAINER_ARCHIVED',
			resolved_at = current_timestamp, revision = revision + 1, updated_at = current_timestamp
			where status = 'PENDING' and (challenger_trainer_id = :trainerId or challenged_trainer_id = :trainerId)""")
			.param("trainerId", trainerId).update()
		val changed = jdbc.sql("""update match_trainer set archived_at = :archivedAt, revision = revision + 1, updated_at = current_timestamp
			where account_id = :accountId and id = :trainerId and revision = :revision and archived_at is null""")
			.params(mapOf("archivedAt" to OffsetDateTime.ofInstant(archivedAt, ZoneOffset.UTC), "accountId" to accountId, "trainerId" to trainerId, "revision" to expectedRevision)).update()
		return if (changed == 1) findById(accountId, trainerId) else null
	}

	override fun restore(accountId: Long, trainerId: Long, expectedRevision: Long): TrainerRecord? {
		val changed = jdbc.sql("""update match_trainer set archived_at = null, revision = revision + 1, updated_at = current_timestamp
			where account_id = :accountId and id = :trainerId and revision = :revision and archived_at is not null""")
			.params(mapOf("accountId" to accountId, "trainerId" to trainerId, "revision" to expectedRevision)).update()
		return if (changed == 1) findById(accountId, trainerId) else null
	}

	private fun queryOne(sql: String, params: Map<String, Any>): TrainerRecord? =
		jdbc.sql(sql).params(params).query(::map).optional().orElse(null)

	private fun map(rs: ResultSet, @Suppress("UNUSED_PARAMETER") row: Int) = TrainerRecord(
		rs.getLong("id"), rs.getLong("account_id"), rs.getString("display_name"), rs.getString("display_name_key"),
		rs.getString("command_id"), rs.getLong("revision"), rs.getObject("archived_at", OffsetDateTime::class.java)?.toInstant(),
	)
}
