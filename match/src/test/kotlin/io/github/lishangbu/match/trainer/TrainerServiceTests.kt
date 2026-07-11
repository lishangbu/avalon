package io.github.lishangbu.match.trainer

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

class TrainerServiceTests {
	@Test
	fun `replays same create command and rejects changed payload`() {
		val store = FakeTrainerStore()
		val service = service(store)
		val commandId = UUID.fromString("00000000-0000-4000-8000-000000000001")
		val first = service.create(1, CreateTrainerCommand(commandId, "Avalon"))
		assertEquals(first, service.create(1, CreateTrainerCommand(commandId, "avalon")))
		assertFailsWith<TrainerCommandPayloadConflictException> {
			service.create(1, CreateTrainerCommand(commandId, "Other"))
		}
	}

	@Test
	fun `enforces active limit and sensitive names`() {
		val store = FakeTrainerStore(rules = listOf(SensitiveNameRule("admin", SensitiveNameMatchType.CONTAINS)))
		val service = service(store)
		assertFailsWith<SensitiveTrainerDisplayNameException> {
			service.create(1, CreateTrainerCommand(uuid("010"), "my_admin"))
		}
		repeat(3) { service.create(1, CreateTrainerCommand(uuid("02$it"), "Trainer-$it")) }
		assertFailsWith<TrainerLimitExceededException> {
			service.create(1, CreateTrainerCommand(uuid("024"), "Trainer-4"))
		}
	}

	@Test
	fun `archives and restores with revision`() {
		val store = FakeTrainerStore()
		val service = service(store)
		val trainer = service.create(1, CreateTrainerCommand(uuid("030"), "Trainer"))
		val archived = service.archive(1, trainer.id, 0)
		assertEquals(1, archived.revision)
		assertEquals(2, service.restore(1, trainer.id, 1).revision)
	}

	@Test
	fun `rejects stale archive revision and blocking activity`() {
		val store = FakeTrainerStore()
		val service = service(store)
		val trainer = service.create(1, CreateTrainerCommand(uuid("040"), "Trainer"))
		assertFailsWith<TrainerRevisionConflictException> { service.archive(1, trainer.id, 1) }
		store.blockingActivity = true
		assertFailsWith<TrainerArchiveBlockedException> { service.archive(1, trainer.id, 0) }
	}

	@Test
	fun `holds archive reservation until transaction commit or rollback completes`() {
		listOf(TransactionSynchronization.STATUS_COMMITTED, TransactionSynchronization.STATUS_ROLLED_BACK).forEach { status ->
			val store = FakeTrainerStore()
			val sessions = TrainerSessionRegistry()
			val service = service(store, sessions)
			val trainer = service.create(1, CreateTrainerCommand(uuid("05$status"), "Trainer-$status"))
			TransactionSynchronizationManager.initSynchronization()
			try {
				service.archive(1, trainer.id, 0)
				assertFailsWith<TrainerSessionEntryBlockedException> {
					sessions.enter(TrainerSelection(1, trainer.id), Instant.EPOCH)
				}
				TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCompletion(status) }
				sessions.enter(TrainerSelection(1, trainer.id), Instant.EPOCH)
			} finally {
				TransactionSynchronizationManager.clearSynchronization()
			}
		}
	}

	private fun service(store: FakeTrainerStore, sessions: TrainerSessionRegistry = TrainerSessionRegistry()) = TrainerService(
		store = store,
		clock = Clock.fixed(Instant.parse("2026-07-12T00:00:00Z"), ZoneOffset.UTC),
		idGenerator = { store.nextId++ },
		sessions = sessions,
	)

	private fun uuid(suffix: String) = UUID.fromString("00000000-0000-4000-8000-${suffix.padStart(12, '0')}")
}

private class FakeTrainerStore(
	private val rules: List<SensitiveNameRule> = emptyList(),
) : TrainerStore {
	override fun lockAccount(accountId: Long) = Unit
	val records = mutableListOf<TrainerRecord>()
	var nextId = 1L
	var blockingActivity = false
	override fun findByCommand(accountId: Long, commandId: String) = records.find { it.accountId == accountId && it.commandId == commandId }
	override fun findById(accountId: Long, trainerId: Long) = records.find { it.accountId == accountId && it.id == trainerId }
	override fun list(accountId: Long) = records.filter { it.accountId == accountId }
	override fun countActive(accountId: Long) = records.count { it.accountId == accountId && it.archivedAt == null }
	override fun hasBlockingActivity(accountId: Long, trainerId: Long) = blockingActivity
	override fun enabledSensitiveNameRules() = rules
	override fun insert(record: TrainerRecord) = record.also(records::add)
	override fun archive(accountId: Long, trainerId: Long, expectedRevision: Long, archivedAt: Instant) = update(accountId, trainerId, expectedRevision) { it.copy(revision = it.revision + 1, archivedAt = archivedAt) }
	override fun restore(accountId: Long, trainerId: Long, expectedRevision: Long) = update(accountId, trainerId, expectedRevision) { it.copy(revision = it.revision + 1, archivedAt = null) }
	private fun update(accountId: Long, trainerId: Long, revision: Long, transform: (TrainerRecord) -> TrainerRecord): TrainerRecord? {
		val index = records.indexOfFirst { it.accountId == accountId && it.id == trainerId && it.revision == revision }
		if (index < 0) return null
		return transform(records[index]).also { records[index] = it }
	}
}
