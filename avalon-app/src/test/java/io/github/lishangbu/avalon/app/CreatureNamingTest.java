package io.github.lishangbu.avalon.app;

import org.springframework.http.MediaType;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureNamingCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureNamingQueryService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** 改名真实事务验收：报价、越权、重试、并发、失败回滚及冒险状态门禁。 */
class CreatureNamingTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	CreatureCommandService pets;

	@Autowired
	CreatureQueryService petQueries;

	@Autowired
	CreatureNamingQueryService queries;

	@Autowired
	CreatureNamingCommandService commands;

	@Autowired
	EntityManager em;

	record Player(long owner, long pet, String token) {
	}

	Player ready() {
		String name = "name_" + UUID.randomUUID().toString().substring(0, 8);
		var token = registrations.register(name, "Naming-test-123", "测试").accessToken();
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			pets.grant(owner, "NAMING", "SPROUT");
			characters.changeGold(owner, 30);
		});
		return new Player(owner, Long.parseLong(petQueries.list(owner).getFirst().id()), token);
	}

	CreatureNamingCommandService.Request request(Player player, String name) {
		var quote = queries.preview(player.owner(), player.pet());
		return new CreatureNamingCommandService.Request(UUID.randomUUID().toString(), name, quote.name(),
				quote.renamed(), quote.price());
	}

	@Test
	void freeThenPaidWithReplayAndStaleConfirmation() throws Exception {
		var player = ready();
		long gold = characterQueries.find(player.owner()).gold();
		var first = request(player, "青芽");
		assertEquals(0, first.expectedPrice());
		commands.rename(player.owner(), player.pet(), first);
		commands.rename(player.owner(), player.pet(), first);
		assertEquals(gold, characterQueries.find(player.owner()).gold());
		assertEquals(1, queries.preview(player.owner(), player.pet()).renamed());
		var second = request(player, "青叶");
		var stale = request(player, "青枝");
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var a = executor.submit(() -> commands.rename(player.owner(), player.pet(), second));
			var b = executor.submit(() -> commands.rename(player.owner(), player.pet(), second));
			a.get(10, TimeUnit.SECONDS);
			b.get(10, TimeUnit.SECONDS);
		}
		assertEquals(gold - 10, characterQueries.find(player.owner()).gold());
		assertEquals(2, queries.preview(player.owner(), player.pet()).renamed());
		assertThrows(BusinessException.class, () -> commands.rename(player.owner(), player.pet(), stale));
		assertThrows(BusinessException.class,
				() -> commands.rename(player.owner(), player.pet(), request(player, "青叶")));
		var changedPayload = new CreatureNamingCommandService.Request(second.requestId(), "青枝", second.expectedName(),
				second.renamed(), second.expectedPrice());
		assertThrows(BusinessException.class, () -> commands.rename(player.owner(), player.pet(), changedPayload));
	}

	@Test
	void rollbackAndInsufficientGoldPreserveIdentityAndFreeEligibility() {
		var player = ready();
		var before = queries.preview(player.owner(), player.pet());
		var request = request(player, "青芽");
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			commands.rename(player.owner(), player.pet(), request);
			throw new IllegalStateException("rollback");
		}));
		assertEquals(before, queries.preview(player.owner(), player.pet()));
		commands.rename(player.owner(), player.pet(), request);
		long gold = characterQueries.find(player.owner()).gold();
		transactions.run(() -> characters.changeGold(player.owner(), -gold));
		var paid = request(player, "新芽");
		assertThrows(BusinessException.class, () -> commands.rename(player.owner(), player.pet(), paid));
		assertEquals("青芽", queries.preview(player.owner(), player.pet()).name());
		assertEquals(1, queries.preview(player.owner(), player.pet()).renamed());
		transactions.run(() -> characters.changeGold(player.owner(), 10));
		commands.rename(player.owner(), player.pet(), paid);
		assertEquals(0, characterQueries.find(player.owner()).gold());
	}

	@Test
	void httpOwnershipValidationAndAdventureRestrictions() {
		var player = ready();
		var other = ready();
		String path = "/api/creatures/" + player.pet() + "/name";
		client.get().uri(path).exchange().expectStatus().isEqualTo(401);
		client.get()
			.uri(path)
			.headers(headers -> headers.setBearerAuth(other.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		var request = request(player, "青芽");
		client.post()
			.uri(path)
			.headers(headers -> headers.setBearerAuth(other.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		for (String invalid : new String[] { "", " ", "<script>", "名字名字名字名字名字名字名" }) {
			assertThrows(BusinessException.class,
					() -> commands.rename(player.owner(), player.pet(), request(player, invalid)));
		}
		// 副本指针受外键保护，先建立属于本角色的有效记录，再验证改名门禁。
		transactions.run(() -> em.createNativeQuery(
				"INSERT INTO dungeon_run(id, leader_id, request_id, active, state) SELECT id, id, 'naming-test', true, '{}' FROM player_character WHERE account_id=:owner")
			.setParameter("owner", player.owner())
			.executeUpdate());
		for (String column : new String[] { "active_battle_id", "active_dungeon_id" }) {
			transactions
				.run(() -> em.createNativeQuery("UPDATE player_character SET " + column + "=id WHERE account_id=:owner")
					.setParameter("owner", player.owner())
					.executeUpdate());
			client.post()
				.uri(path)
				.headers(headers -> headers.setBearerAuth(player.token()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.exchange()
				.expectStatus()
				.isEqualTo(409);
			transactions.run(() -> em
				.createNativeQuery("UPDATE player_character SET " + column + "=NULL WHERE account_id=:owner")
				.setParameter("owner", player.owner())
				.executeUpdate());
		}
		client.post()
			.uri(path)
			.headers(headers -> headers.setBearerAuth(player.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.exchange()
			.expectStatus()
			.isEqualTo(204);
		assertEquals("青芽", petQueries.list(player.owner()).getFirst().name());
	}

}
