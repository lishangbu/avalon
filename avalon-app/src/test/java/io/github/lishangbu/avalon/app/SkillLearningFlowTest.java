package io.github.lishangbu.avalon.app;

import org.springframework.http.MediaType;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.command.SkillLearningCommandService;
import io.github.lishangbu.avalon.creature.application.command.SkillLearningCommandService.Request;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.creature.application.query.SkillLearningQueryService;
import io.github.lishangbu.avalon.creature.domain.SkillLearningRules;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** 用真实 PostgreSQL 验证消费、技能与凭证同事务；测试资源通过内部发放命令准备，不提供生产调试接口。 */
class SkillLearningFlowTest extends SpringIntegrationTest {

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
	CreatureQueryService creatureQueries;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService inventoryQueries;

	@Autowired
	SkillLearningCommandService commands;

	@Autowired
	SkillLearningQueryService queries;

	@Autowired
	EntityManager em;

	private Player ready() {
		String name = "learn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		var tokens = registrations.register(name, "Learning-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			creatures.grant(owner, "TEST_PET", "SPROUT");
			inventory.grant(owner, "TEST_WATER", "BOOK_WATER", 2, false);
			inventory.grant(owner, "TEST_SMITE", "BOOK_SMITE", 1, false);
			inventory.grant(owner, "TEST_SCREEN", "BOOK_LIGHT_SCREEN", 1, false);
		});
		return new Player(owner, Long.parseLong(creatureQueries.list(owner).getFirst().id()), tokens.accessToken());
	}

	private Request request(Player player, String code) {
		String item = inventoryQueries.list(player.owner())
			.stream()
			.filter(i -> i.code().equals(code))
			.findFirst()
			.orElseThrow()
			.id();
		return new Request(UUID.randomUUID().toString(), item, queries.preview(player.owner(), player.pet()).current());
	}

	private int quantity(Player player, String code) {
		return inventoryQueries.list(player.owner())
			.stream()
			.filter(i -> i.code().equals(code))
			.mapToInt(i -> i.quantity())
			.sum();
	}

	@Test
	void concurrentRetryReturnsOneCommittedDrawAndOneBookConsumption() throws Exception {
		var player = ready();
		var request = request(player, "BOOK_WATER");
		var start = new CountDownLatch(1);
		Callable<SkillLearningRules.Result> action = () -> {
			assertTrue(start.await(10, TimeUnit.SECONDS));
			return commands.learn(player.owner(), player.pet(), request);
		};
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var first = executor.submit(action);
			var second = executor.submit(action);
			start.countDown();
			assertEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
		}
		assertEquals(1, quantity(player, "BOOK_WATER"));
		assertEquals(List.of("藤鞭", "微疗", "水弹"), queries.preview(player.owner(), player.pet()).current());
		// 第四技能有真实遗忘概率；不要求随机结果恰好落在哪个分支，但必须持久化同一结果。
		var screen = request(player, "BOOK_LIGHT_SCREEN");
		var result = commands.learn(player.owner(), player.pet(), screen);
		assertEquals(result, commands.learn(player.owner(), player.pet(), screen));
		assertEquals(result.remaining(), queries.preview(player.owner(), player.pet()).current());
		assertEquals(0, quantity(player, "BOOK_LIGHT_SCREEN"));
		assertTrue(result.remaining().contains("光幕"));
		assertFalse(result.forgotten().contains("光幕"));
		assertThrows(BusinessException.class, () -> commands.learn(player.owner(), player.pet(),
				new Request(screen.requestId(), request.bookId(), screen.expectedSkills())));
	}

	@Test
	void forgottenSkillRequiresNewBookAndOldReceiptCannotRestoreIt() {
		var player = ready();
		var original = request(player, "BOOK_WATER");
		var first = commands.learn(player.owner(), player.pet(), original);
		assertEquals(1, quantity(player, "BOOK_WATER"));
		var stale = request(player, "BOOK_WATER");
		// 固定“后续学习光幕并遗忘水弹”的合法持久化结果，避免依赖随机抽中遗忘而造成偶发测试失败。
		// 随机分布由 SkillLearningRulesTest 验证；本用例专验旧凭证、当前技能及补学消费之间的边界。
		transactions.run(() -> em.createNativeQuery("UPDATE owned_creature SET skills=:skills WHERE id=:id")
			.setParameter("skills", "[\"藤鞭\",\"微疗\",\"光幕\"]")
			.setParameter("id", player.pet())
			.executeUpdate());
		var forgottenState = queries.preview(player.owner(), player.pet()).current();
		assertFalse(forgottenState.contains("水弹"));
		assertEquals(first, commands.learn(player.owner(), player.pet(), original));
		assertEquals(forgottenState, queries.preview(player.owner(), player.pet()).current(),
				"旧请求只能返回旧回执，不能将后来遗忘的技能免费恢复");
		assertEquals(1, quantity(player, "BOOK_WATER"));
		assertThrows(BusinessException.class, () -> commands.learn(player.owner(), player.pet(), stale));
		assertEquals(1, quantity(player, "BOOK_WATER"));

		var relearn = request(player, "BOOK_WATER");
		var result = commands.learn(player.owner(), player.pet(), relearn);
		assertTrue(result.remaining().contains("水弹"));
		assertFalse(result.forgotten().contains("水弹"));
		assertEquals(result.remaining(), queries.preview(player.owner(), player.pet()).current());
		assertEquals(0, quantity(player, "BOOK_WATER"));
		assertEquals(result, commands.learn(player.owner(), player.pet(), relearn));
		assertEquals(0, quantity(player, "BOOK_WATER"));
		assertEquals(result.remaining(), queries.preview(player.owner(), player.pet()).current());
		// 补学之后仍可重放更早回执，但绝不回写更早技能快照。
		assertEquals(first, commands.learn(player.owner(), player.pet(), original));
		assertEquals(result.remaining(), queries.preview(player.owner(), player.pet()).current());
	}

	@Test
	void ownershipPoolDuplicatesAndStaleConfirmationNeverConsumeBooks() {
		var player = ready();
		var outsider = ready();
		var water = request(player, "BOOK_WATER");
		assertThrows(BusinessException.class, () -> queries.preview(outsider.owner(), player.pet()));
		assertThrows(BusinessException.class, () -> commands.learn(outsider.owner(), player.pet(), water));
		assertThrows(BusinessException.class, () -> commands.learn(outsider.owner(), outsider.pet(), water));
		assertThrows(BusinessException.class,
				() -> commands.learn(player.owner(), player.pet(), request(player, "BOOK_SMITE")));
		assertThrows(BusinessException.class, () -> commands.learn(player.owner(), player.pet(),
				new Request(UUID.randomUUID().toString(), water.bookId(), List.of("藤鞭"))));
		assertEquals(2, quantity(player, "BOOK_WATER"));
		transactions.run(() -> characters.setBattle(player.owner(), 123L, false));
		assertThrows(BusinessException.class, () -> commands.learn(player.owner(), player.pet(), water));
		assertEquals(2, quantity(player, "BOOK_WATER"));
		transactions.run(() -> characters.setBattle(player.owner(), null, false));
		commands.learn(player.owner(), player.pet(), water);
		assertThrows(BusinessException.class,
				() -> commands.learn(player.owner(), player.pet(), request(player, "BOOK_WATER")));
		assertEquals(1, quantity(player, "BOOK_WATER"));
		client.get()
			.uri("/api/creatures/" + player.pet() + "/learning")
			.header("Authorization", "Bearer " + player.token())
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.current.length()")
			.isEqualTo(3);
		client.get()
			.uri("/api/creatures/" + player.pet() + "/learning")
			.header("Authorization", "Bearer " + outsider.token())
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		var outsiderRequest = request(outsider, "BOOK_WATER");
		for (int repeat = 0; repeat < 2; repeat++)
			client.post()
				.uri("/api/creatures/" + outsider.pet() + "/learning")
				.header("Authorization", "Bearer " + outsider.token())
				.contentType(MediaType.APPLICATION_JSON)
				.body(outsiderRequest)
				.exchange()
				.expectStatus()
				.isEqualTo(200)
				.expectBody()
				.jsonPath("$.learned")
				.isEqualTo("水弹");
		assertEquals(1, quantity(outsider, "BOOK_WATER"));
	}

	@Test
	void receiptWriteFailureRollsBackBookAndSkillsThenAllowsRetry() {
		var player = ready();
		var request = request(player, "BOOK_WATER");
		long character = Long.parseLong(characterQueries.find(player.owner()).id());
		transactions.run(() -> {
			em.createNativeQuery(
					"CREATE FUNCTION reject_test_learning() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.character_id = "
							+ character + " THEN RAISE EXCEPTION 'test learning failure'; END IF; RETURN NEW; END $$")
				.executeUpdate();
			em.createNativeQuery(
					"CREATE TRIGGER reject_test_learning BEFORE INSERT ON skill_learning_receipt FOR EACH ROW EXECUTE FUNCTION reject_test_learning()")
				.executeUpdate();
		});
		try {
			assertThrows(RuntimeException.class, () -> commands.learn(player.owner(), player.pet(), request));
			assertEquals(2, quantity(player, "BOOK_WATER"));
			assertEquals(request.expectedSkills(), queries.preview(player.owner(), player.pet()).current());
		}
		finally {
			transactions.run(() -> {
				em.createNativeQuery("DROP TRIGGER reject_test_learning ON skill_learning_receipt").executeUpdate();
				em.createNativeQuery("DROP FUNCTION reject_test_learning()").executeUpdate();
			});
		}
		assertEquals("水弹", commands.learn(player.owner(), player.pet(), request).learned());
		assertEquals(1, quantity(player, "BOOK_WATER"));
	}

	private record Player(long owner, long pet, String token) {
	}

}
