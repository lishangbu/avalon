package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 门禁持久化与实际移动/编队入口共用角色锁；旧会话释放不得影响新派遣。 */
class LifeActivityTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService pets;

	private long owner() {
		String name = "act_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Activity-test-12345", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			creatures.grant(owner, "FIRST", "SPROUT");
			creatures.grant(owner, "SECOND", "STONE_GOLEM");
		});
		return owner;
	}

	@Test
	void dispatchBlocksMovementBattleAndSwappingThroughAnotherPet() {
		long owner = owner();
		var owned = pets.list(owner);
		long first = Long.parseLong(owned.getFirst().id()), second = Long.parseLong(owned.getLast().id());
		String session = UUID.randomUUID().toString();
		transactions.run(() -> characters.beginLifeActivity(owner, session, first));
		assertThrows(BusinessException.class,
				() -> characters.move(owner, "STONE_PATH", queries.find(owner).revision()));
		assertThrows(BusinessException.class, () -> transactions.run(() -> characters.setBattle(owner, 123L, false)));
		assertThrows(BusinessException.class,
				() -> creatures.position(owner, second, owned.getFirst().partyPosition()));
		assertThrows(BusinessException.class, () -> transactions
			.run(() -> characters.beginLifeActivity(owner, UUID.randomUUID().toString(), second)));
		assertDoesNotThrow(() -> pets.list(owner));
		assertDoesNotThrow(() -> transactions.run(() -> characters.requireCreatureNotDispatched(owner, second)));
		transactions.run(() -> characters.endLifeActivity(owner, session));
		characters.move(owner, "STONE_PATH", queries.find(owner).revision());
		assertEquals("STONE_PATH", queries.find(owner).location());
	}

	@Test
	void onlyOneConcurrentActivityCanReserveTheCharacter() throws Exception {
		long owner = owner();
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var attempts = List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString())
				.stream()
				.map(session -> executor.submit(() -> {
					assertTrue(start.await(10, TimeUnit.SECONDS));
					try {
						transactions.run(() -> characters.beginLifeActivity(owner, session, null));
						return true;
					}
					catch (BusinessException error) {
						return false;
					}
				}))
				.toList();
			start.countDown();
			assertNotEquals(attempts.getFirst().get(15, TimeUnit.SECONDS),
					attempts.getLast().get(15, TimeUnit.SECONDS));
		}
	}

	@Test
	void staleStopCannotReleaseNewSessionAndFailedBeginRollsBack() {
		long owner = owner();
		String first = UUID.randomUUID().toString(), second = UUID.randomUUID().toString();
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			characters.beginLifeActivity(owner, first, null);
			throw new IllegalStateException("模拟会话持久化失败");
		}));
		transactions.run(() -> characters.beginLifeActivity(owner, second, null));
		assertThrows(BusinessException.class, () -> transactions.run(() -> characters.endLifeActivity(owner, first)));
		assertThrows(BusinessException.class, () -> transactions.run(() -> characters.requireNoLifeActivity(owner)));
		transactions.run(() -> characters.endLifeActivity(owner, second));
		assertDoesNotThrow(() -> transactions.run(() -> characters.requireNoLifeActivity(owner)));
	}

}
