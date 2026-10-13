package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.world.persistence.entity.QuestJournalState;
import io.github.lishangbu.avalon.world.persistence.entity.QuestJournalStateStatus;
import io.github.lishangbu.avalon.world.persistence.repository.QuestJournalStateRepository;
import java.time.Instant;
import java.util.*;
import io.github.lishangbu.avalon.quest.QuestInstance;
import io.github.lishangbu.avalon.quest.model.QuestStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.jupiter.api.Assertions.*;

/** 实际PostgreSQL验证参与状态可跨事务恢复、角色隔离、唯一性与整体回滚。 */
class QuestJournalStatePersistenceTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registration;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	QuestJournalStateRepository participation;

	@Test
	void persistsTransitionsAndRollsBackFailureWithoutDuplicatingParticipation() {
		String name = "qp" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		registration.register(name, "Quest-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		long[] id = new long[1];
		var first = Instant.parse("2026-09-21T00:00:00Z");
		transactions.run(() -> {
			id[0] = characters.lock(owner).id();
			var projection = QuestJournalState.begin(id[0], "BS01", first);
			projection.synchronize(instance(first, QuestStatus.ACTIVE));
			participation.saveAndFlush(projection);
		});
		transactions.run(() -> {
			characters.lock(owner);
			var state = participation.findByCharacterIdAndQuestCode(id[0], "BS01").orElseThrow();
			assertNotNull(state.getId());
			state.synchronize(instance(first, QuestStatus.PAUSED));
		});
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			characters.lock(owner);
			participation.findByCharacterIdAndQuestCode(id[0], "BS01")
				.orElseThrow()
				.synchronize(instance(first.plusSeconds(30), QuestStatus.ACTIVE));
			participation.flush();
			throw new IllegalStateException("模拟后续效果失败");
		}));
		transactions.run(() -> {
			var state = participation.findByCharacterIdAndQuestCode(id[0], "BS01").orElseThrow();
			assertEquals(QuestJournalStateStatus.ABANDONED, state.getStatus());
			assertEquals(first, state.getActiveSince());
			assertEquals(1, state.getRevision());
			assertTrue(participation.findByCharacterIdAndQuestCode(-1, "BS01").isEmpty());
		});
		assertThrows(DataIntegrityViolationException.class, () -> transactions.run(() -> {
			var duplicate = QuestJournalState.begin(id[0], "BS01", first);
			duplicate.synchronize(instance(first, QuestStatus.ACTIVE));
			participation.saveAndFlush(duplicate);
		}));
		transactions.run(() -> assertEquals(1, participation.findByCharacterId(id[0]).size()));
	}

	private QuestInstance instance(Instant at, QuestStatus status) {
		return new QuestInstance("instance", "BS01", "ONCE", at, at, status, 1, Map.of(), Set.of(), Map.of(), Map.of(),
				Set.of(), Map.of());
	}

}
