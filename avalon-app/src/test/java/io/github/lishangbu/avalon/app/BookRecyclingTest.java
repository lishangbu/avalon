package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.BookRecyclingCommandService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.model.BookRecyclingRequest;
import io.github.lishangbu.avalon.inventory.application.query.BookRecyclingQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.*;

/** 正式数据库验证回收边界；夹具仅提供指定来源库存和位置，不替代实际回收命令。 */
class BookRecyclingTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registration;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	BookRecyclingQueryService queries;

	@Autowired
	BookRecyclingCommandService commands;

	@Autowired
	EntityManager em;

	record Player(long owner, String token) {
	}

	Player ready() {
		String name = "book_" + UUID.randomUUID().toString().substring(0, 8);
		var token = registration.register(name, "Book-recycle-123", "测试").accessToken();
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		location(owner, "SUPPLY_SHOP");
		return new Player(owner, token);
	}

	void location(long owner, String location) {
		transactions
			.run(() -> em.createNativeQuery("UPDATE player_character SET location=:location WHERE account_id=:owner")
				.setParameter("location", location)
				.setParameter("owner", owner)
				.executeUpdate());
	}

	void grant(long owner, String source, String code, int count, boolean bound) {
		transactions.run(() -> inventory.grantStrict(owner, source, code, count, false, bound));
	}

	BookRecyclingRequest request(long owner, int quantity) {
		return new BookRecyclingRequest(UUID.randomUUID().toString(), quantity,
				queries.view(owner)
					.offers()
					.stream()
					.filter(o -> o.code().equals("BOOK_WATER"))
					.findFirst()
					.orElseThrow());
	}

	long count(long owner, boolean bound) {
		return transactions.call(() -> ((Number) em.createNativeQuery(
				"SELECT COALESCE(SUM(i.quantity),0) FROM owned_item i JOIN player_character c ON c.id=i.character_id WHERE c.account_id=:owner AND i.code='BOOK_WATER' AND i.bound=:bound")
			.setParameter("owner", owner)
			.setParameter("bound", bound)
			.getSingleResult()).longValue());
	}

	@Test
	void boundReserveCrossSourceConcurrentReplayAndStaleQuote() throws Exception {
		var player = ready();
		grant(player.owner(), "task", "BOOK_WATER", 1, true);
		grant(player.owner(), "drop1", "BOOK_WATER", 2, false);
		grant(player.owner(), "drop2", "BOOK_WATER", 1, false);
		long gold = characterQueries.find(player.owner()).gold();
		var request = request(player.owner(), 3);
		assertEquals(6, request.expected().unitPrice());
		assertEquals(3, request.expected().maximum());
		var stale = request(player.owner(), 1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var a = executor.submit(() -> commands.sell(player.owner(), request));
			var b = executor.submit(() -> commands.sell(player.owner(), request));
			a.get(10, TimeUnit.SECONDS);
			b.get(10, TimeUnit.SECONDS);
		}
		assertEquals(gold + 18, characterQueries.find(player.owner()).gold());
		assertEquals(1, count(player.owner(), true));
		assertEquals(0, count(player.owner(), false));
		assertTrue(queries.view(player.owner()).offers().isEmpty());
		assertThrows(BusinessException.class, () -> commands.sell(player.owner(), stale));
		assertThrows(BusinessException.class, () -> commands.sell(player.owner(),
				new BookRecyclingRequest(request.requestId(), 1, request.expected())));
		// 库存耗尽仍保留来源，重复发放不能使已出售的书再次出现。
		grant(player.owner(), "drop1", "BOOK_WATER", 2, false);
		assertEquals(0, count(player.owner(), false));
	}

	@Test
	void unboundReservePendingExclusionAndAtomicRollback() {
		var player = ready();
		grant(player.owner(), "one", "BOOK_WATER", 1, false);
		grant(player.owner(), "pending", "BOOK_WATER", 5, true);
		transactions.run(() -> em.createNativeQuery(
				"UPDATE owned_item SET quantity=0,pending_quantity=5 WHERE source='pending' AND character_id=(SELECT id FROM player_character WHERE account_id=:owner)")
			.setParameter("owner", player.owner())
			.executeUpdate());
		assertTrue(queries.view(player.owner()).offers().isEmpty());
		grant(player.owner(), "two", "BOOK_WATER", 2, false);
		grant(player.owner(), "fragment", "FRAGMENT_WATER", 5, false);
		grant(player.owner(), "unpriced", "BOOK_FLAME_BURST", 2, false);
		var request = request(player.owner(), 2);
		assertEquals(1, queries.view(player.owner()).offers().size());
		assertThrows(BusinessException.class, () -> commands.sell(player.owner(),
				new BookRecyclingRequest(UUID.randomUUID().toString(), 3, request.expected())));
		assertThrows(BusinessException.class, () -> commands.sell(player.owner(),
				new BookRecyclingRequest(UUID.randomUUID().toString(), 0, request.expected())));
		long gold = characterQueries.find(player.owner()).gold();
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			commands.sell(player.owner(), request);
			throw new IllegalStateException("rollback");
		}));
		assertEquals(3, count(player.owner(), false));
		assertEquals(gold, characterQueries.find(player.owner()).gold());
		commands.sell(player.owner(), request);
		assertEquals(1, count(player.owner(), false));
		assertEquals(gold + 12, characterQueries.find(player.owner()).gold());
		assertTrue(queries.view(player.owner()).offers().isEmpty());
	}

	@Test
	void httpAccountLocationAndBattleGates() {
		var player = ready();
		var other = ready();
		grant(player.owner(), "drop", "BOOK_WATER", 2, false);
		var request = request(player.owner(), 1);
		String path = "/api/inventory/books/recycling";
		client.get().uri(path).exchange().expectStatus().isEqualTo(401);
		client.post()
			.uri(path)
			.headers(headers -> headers.setBearerAuth(other.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.exchange()
			.expectStatus()
			.isEqualTo(409);
		location(player.owner(), "SQUARE");
		client.post()
			.uri(path)
			.headers(headers -> headers.setBearerAuth(player.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.exchange()
			.expectStatus()
			.isEqualTo(409);
		location(player.owner(), "SUPPLY_SHOP");
		transactions
			.run(() -> em.createNativeQuery("UPDATE player_character SET active_battle_id=id WHERE account_id=:owner")
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
		transactions
			.run(() -> em.createNativeQuery("UPDATE player_character SET active_battle_id=NULL WHERE account_id=:owner")
				.setParameter("owner", player.owner())
				.executeUpdate());
		client.post()
			.uri(path)
			.headers(headers -> headers.setBearerAuth(player.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.exchange()
			.expectStatus()
			.isEqualTo(204);
	}

}
