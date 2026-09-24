package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCustodyService;
import io.github.lishangbu.avalon.creature.application.command.CreatureManagementService;
import io.github.lishangbu.avalon.creature.application.model.CreatureView;
import io.github.lishangbu.avalon.creature.application.query.CreatureCustodyQueryService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.creature.application.query.SkillLearningQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;

/** 真实数据库覆盖放生的不可逆边界、完整事务、历史防重与仓库容量。 */
class CreatureManagementTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureManagementService management;

	@Autowired
	CreatureQueryService queries;

	@Autowired
	SkillLearningQueryService learning;

	@Autowired
	EntityManager em;

	@Autowired
	CreatureCustodyService custody;

	@Autowired
	CreatureCustodyQueryService custodyQueries;

	private Player ready() {
		String name = "pets_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		var tokens = registrations.register(name, "Management-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> creatures.grant(owner, "GIFT", "SPROUT"));
		return new Player(owner, tokens.accessToken());
	}

	private long capture(Player player, String source) {
		return transactions
			.call(() -> creatures.capture(player.owner(), source, "STONE_GOLEM", 1, "H", "守护", List.of("碎岩击", "掩护")));
	}

	private CreatureView pet(Player player, long id) {
		return queries.list(player.owner())
			.stream()
			.filter(p -> p.id().equals(Long.toString(id)))
			.findFirst()
			.orElseThrow();
	}

	private CreatureManagementService.Release request(CreatureView... pets) {
		return new CreatureManagementService.Release(UUID.randomUUID().toString(), List.of(pets));
	}

	private void center(Player player) {
		long character = Long.parseLong(characterQueries.find(player.owner()).id());
		transactions
			.run(() -> em.createNativeQuery("UPDATE player_character SET location = 'PET_CENTER' WHERE id = :id")
				.setParameter("id", character)
				.executeUpdate());
	}

	@Test
	void releasingLastUsableCompanionOrInitialGiftBeforeFirstCaptureIsRejected() {
		var player = ready();
		transactions.run(() -> characters.grantExperience(player.owner(), 700));
		var gift = queries.list(player.owner()).getFirst();
		creatures.position(player.owner(), Long.parseLong(gift.id()), 4);
		assertThrows(BusinessException.class,
				() -> management.release(player.owner(), request(pet(player, Long.parseLong(gift.id())))));
		transactions
			.run(() -> creatures.capture(player.owner(), "TOO_HIGH", "STONE_GOLEM", 4, "A", "坚岩", List.of("碎岩击")));
		assertThrows(BusinessException.class,
				() -> management.release(player.owner(), request(pet(player, Long.parseLong(gift.id())))));
		long usable = capture(player, "USABLE");
		management.release(player.owner(), request(pet(player, Long.parseLong(gift.id()))));
		creatures.position(player.owner(), usable, 4);
		assertThrows(BusinessException.class, () -> management.release(player.owner(), request(pet(player, usable))));
		assertTrue(custodyQueries.eligible(player.owner())
			.stream()
			.noneMatch(value -> value.id().equals(Long.toString(usable))));
		assertThrows(BusinessException.class,
				() -> transactions.call(() -> custody.deposit(player.owner(), usable, pet(player, usable))));
		assertEquals(2, queries.list(player.owner()).size(), "高等级伙伴不能使最后一只可用伙伴被放归");

		var starter = ready();
		transactions.run(() -> {
			characters.grantExperience(starter.owner(), 700);
			creatures.grant(starter.owner(), "BQ004:1", "CLAW_CAT");
		});
		var cat = queries.list(starter.owner())
			.stream()
			.filter(value -> value.species().equals("CLAW_CAT"))
			.findFirst()
			.orElseThrow();
		creatures.position(starter.owner(), Long.parseLong(cat.id()), 4);
		assertThrows(BusinessException.class,
				() -> management.release(starter.owner(), request(pet(starter, Long.parseLong(cat.id())))));
	}

	@Test
	void storageHonorsLocationCapacityLevelOwnershipAndBattle() {
		var player = ready();
		var other = ready();
		long id = capture(player, "STORAGE");
		assertThrows(BusinessException.class, () -> management.deposit(player.owner(), id));
		center(player);
		management.deposit(player.owner(), id);
		management.deposit(player.owner(), id);
		assertEquals(100, pet(player, id).partyPosition());
		assertThrows(BusinessException.class, () -> management.deposit(other.owner(), id));
		creatures.position(player.owner(), id, 4);
		assertEquals(4, pet(player, id).partyPosition());
		transactions.run(() -> {
			for (int i = 0; i < 30; i++)
				creatures.capture(player.owner(), "HIGH_" + i, "STONE_GOLEM", 2, "A", "坚岩", List.of("碎岩击"));
		});
		assertThrows(BusinessException.class, () -> management.deposit(player.owner(), id));
		var high = queries.list(player.owner()).stream().filter(p -> p.level() == 2).findFirst().orElseThrow();
		assertThrows(BusinessException.class, () -> creatures.position(player.owner(), Long.parseLong(high.id()), 3));
		transactions.run(() -> characters.setBattle(player.owner(), 123L, false));
		assertThrows(BusinessException.class, () -> management.lock(player.owner(), id, true));
		assertThrows(BusinessException.class, () -> management.deposit(player.owner(), id));
		assertThrows(BusinessException.class, () -> management.release(player.owner(), request(pet(player, id))));
	}

	@Test
	void releaseRejectsProtectedStaleDuplicateAndForeignSelectionsAtomically() {
		var player = ready();
		var other = ready();
		long id = capture(player, "PROTECTED");
		assertThrows(BusinessException.class, () -> management.release(player.owner(), request(pet(player, id))));
		creatures.position(player.owner(), id, 4);
		var stale = request(pet(player, id));
		management.lock(player.owner(), id, true);
		assertThrows(BusinessException.class, () -> management.release(player.owner(), stale));
		assertThrows(BusinessException.class, () -> management.release(player.owner(), request(pet(player, id))));
		management.lock(player.owner(), id, false);
		var current = pet(player, id);
		assertThrows(BusinessException.class, () -> management.release(player.owner(), request(current, current)));
		var foreign = queries.list(other.owner()).getFirst();
		assertThrows(BusinessException.class, () -> management.release(player.owner(), request(current, foreign)));
		assertEquals(current, pet(player, id));
		client.post()
			.uri("/api/creatures/release")
			.header("Authorization", "Bearer " + other.token())
			.contentType(MediaType.APPLICATION_JSON)
			.body(request(current))
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		client.post()
			.uri("/api/creatures/" + id + "/lock")
			.header("Authorization", "Bearer " + player.token())
			.contentType(MediaType.APPLICATION_JSON)
			.body("{}")
			.exchange()
			.expectStatus()
			.isEqualTo(400);
	}

	@Test
	void concurrentReleaseRetainsDiscoveryAndCannotRecreateHistoricalCapture() throws Exception {
		var player = ready();
		long id = capture(player, "PERSISTENT_CAPTURE");
		creatures.position(player.owner(), id, 4);
		var request = request(pet(player, id));
		var start = new CountDownLatch(1);
		Callable<Void> action = () -> {
			assertTrue(start.await(10, TimeUnit.SECONDS));
			management.release(player.owner(), request);
			return null;
		};
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var first = executor.submit(action);
			var second = executor.submit(action);
			start.countDown();
			first.get(15, TimeUnit.SECONDS);
			second.get(15, TimeUnit.SECONDS);
		}
		assertEquals(1, queries.list(player.owner()).size());
		var entry = queries.encyclopedia(player.owner())
			.stream()
			.filter(p -> p.species().equals("STONE_GOLEM"))
			.findFirst()
			.orElseThrow();
		assertTrue(entry.collected());
		assertEquals(0, entry.owned());
		assertEquals("H", entry.abilities().getFirst().slot());
		assertEquals(id, capture(player, "PERSISTENT_CAPTURE"));
		assertEquals(1, queries.list(player.owner()).size());
		assertTrue(queries.hasSourcePrefix(player.owner(), "PERSISTENT_CAPTURE"));
		assertThrows(BusinessException.class, () -> learning.preview(player.owner(), id));
		assertThrows(BusinessException.class, () -> creatures.position(player.owner(), id, 4));
		assertThrows(BusinessException.class, () -> management.lock(player.owner(), id, false));
		assertThrows(BusinessException.class, () -> management.release(player.owner(),
				new CreatureManagementService.Release(request.requestId(), queries.list(player.owner()))));
		client.get()
			.uri("/api/creatures/encyclopedia")
			.header("Authorization", "Bearer " + player.token())
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.size()")
			.isEqualTo(28);
	}

	@Test
	void failedAuditRollsBackEntireBatchAndFreedSlotsCanBeReused() {
		var player = ready();
		long first = capture(player, "BATCH_1");
		long second = capture(player, "BATCH_2");
		center(player);
		management.deposit(player.owner(), first);
		management.deposit(player.owner(), second);
		var request = request(pet(player, first), pet(player, second));
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			management.release(player.owner(), request);
			throw new IllegalStateException("模拟提交前失败");
		}));
		assertEquals(3, queries.list(player.owner()).size());
		management.release(player.owner(), request);
		management.release(player.owner(), request);
		assertEquals(1, queries.list(player.owner()).size());
		long next = capture(player, "BATCH_3");
		management.deposit(player.owner(), next);
		assertEquals(100, pet(player, next).partyPosition());
	}

	private record Player(long owner, String token) {
	}

}
