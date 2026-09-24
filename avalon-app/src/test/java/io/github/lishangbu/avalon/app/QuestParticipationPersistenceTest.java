package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.world.persistence.entity.QuestParticipation;
import io.github.lishangbu.avalon.world.persistence.repository.QuestParticipationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** 实际PostgreSQL验证参与状态可跨事务恢复、角色隔离、唯一性与整体回滚。 */
class QuestParticipationPersistenceTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registration;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	QuestParticipationRepository participation;

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
			participation.saveAndFlush(QuestParticipation.begin(id[0], "BS01", first));
		});
		transactions.run(() -> {
			characters.lock(owner);
			var state = participation.findByCharacterIdAndQuestCode(id[0], "BS01").orElseThrow();
			assertNotNull(state.getId());
			state.abandon(true);
		});
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			characters.lock(owner);
			participation.findByCharacterIdAndQuestCode(id[0], "BS01").orElseThrow().resume(first.plusSeconds(30));
			participation.flush();
			throw new IllegalStateException("模拟后续效果失败");
		}));
		transactions.run(() -> {
			var state = participation.findByCharacterIdAndQuestCode(id[0], "BS01").orElseThrow();
			assertEquals(QuestParticipation.Status.ABANDONED, state.getStatus());
			assertEquals(first, state.getActiveSince());
			assertEquals(1, state.getRevision());
			assertTrue(participation.findByCharacterIdAndQuestCode(-1, "BS01").isEmpty());
		});
		assertThrows(DataIntegrityViolationException.class, () -> transactions
			.run(() -> participation.saveAndFlush(QuestParticipation.begin(id[0], "BS01", first))));
		transactions.run(() -> assertEquals(1, participation.findByCharacterId(id[0]).size()));
	}

}
