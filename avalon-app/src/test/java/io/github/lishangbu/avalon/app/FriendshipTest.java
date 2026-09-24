package io.github.lishangbu.avalon.app;

import org.assertj.core.api.Assertions;
import java.util.List;
import org.springframework.http.MediaType;
import static org.hamcrest.MatcherAssert.assertThat;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.FriendshipCommandService;
import io.github.lishangbu.avalon.character.application.query.FriendshipQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/** 用真实账号、数据库并发和 HTTP 鉴权验证双向关系，避免把“发出申请”误当作好友。 */
class FriendshipTest extends SpringIntegrationTest {

	private static final String PATH = "/api/friends";

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	FriendshipCommandService commands;

	@Autowired
	FriendshipQueryService queries;

	private Player player() {
		String name = "friend_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		var token = registrations.register(name, "Friend-test-1234", "测试");
		long owner = accounts.accountId(name);
		var character = characters.create(owner, name, "WARRIOR");
		return new Player(owner, Long.parseLong(character.id()), name, token.accessToken());
	}

	private String request(Player source, Player target) {
		return jsonMapper
			.readTree(client.post()
				.uri(PATH)
				.headers(headers -> headers.setBearerAuth(source.token()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("name", target.name()))
				.exchange()
				.expectStatus()
				.isEqualTo(200)
				.expectBody()
				.jsonPath("$.id")
				.value(String.class, actual -> assertThat(actual, instanceOf(String.class)))
				.returnResult()
				.getResponseBody())
			.at("/id")
			.asString();
	}

	@Test
	void friendshipNeedsRecipientConsentAndViewsDoNotLeakPrivateState() {
		var sender = player();
		var recipient = player();
		var outsider = player();
		client.get().uri(PATH).exchange().expectStatus().isEqualTo(401);
		client.post()
			.uri(PATH)
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("name", recipient.name()))
			.exchange()
			.expectStatus()
			.isEqualTo(401);
		client.post()
			.uri(PATH)
			.headers(headers -> headers.setBearerAuth(sender.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("name", sender.name()))
			.exchange()
			.expectStatus()
			.isEqualTo(400);
		client.get()
			.uri(PATH + "?after=-1")
			.headers(headers -> headers.setBearerAuth(sender.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(400);
		String id = request(sender, recipient);
		assertEquals(id, request(sender, recipient));
		assertEquals(id, request(recipient, sender));
		assertThrows(BusinessException.class, () -> queries.requireFriendOwner(sender.owner(), recipient.character()));
		client.get()
			.uri(PATH)
			.headers(headers -> headers.setBearerAuth(sender.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.entries")
			.value(List.class, actual -> Assertions.assertThat(actual).hasSize(1))
			.jsonPath("$.entries[0].status")
			.isEqualTo("OUTGOING");
		var response = jsonMapper.readTree(client.get()
			.uri(PATH)
			.headers(headers -> headers.setBearerAuth(recipient.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.entries[0].status")
			.isEqualTo("INCOMING")
			.jsonPath("$.entries[0].name")
			.isEqualTo(sender.name())
			.returnResult()
			.getResponseBody());
		assertEquals(Set.of("id", "character", "name", "profession", "status"),
				response.at("/entries/0").propertyNames());
		client.get()
			.uri(PATH)
			.headers(headers -> headers.setBearerAuth(outsider.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.entries")
			.value(List.class, actual -> Assertions.assertThat(actual).hasSize(0));
		client.post()
			.uri(PATH + "/" + id + "/accept")
			.headers(headers -> headers.setBearerAuth(outsider.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		client.delete()
			.uri(PATH + "/" + id)
			.headers(headers -> headers.setBearerAuth(outsider.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(204);
		client.post()
			.uri(PATH + "/" + id + "/accept")
			.headers(headers -> headers.setBearerAuth(sender.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(409);
		for (int attempt = 0; attempt < 2; attempt++)
			client.post()
				.uri(PATH + "/" + id + "/accept")
				.headers(headers -> headers.setBearerAuth(recipient.token()))
				.exchange()
				.expectStatus()
				.isEqualTo(204);
		assertEquals(recipient.owner(), queries.requireFriendOwner(sender.owner(), recipient.character()));
		assertEquals(sender.owner(), queries.requireFriendOwner(recipient.owner(), sender.character()));
		client.get()
			.uri(PATH)
			.headers(headers -> headers.setBearerAuth(sender.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.entries[0].status")
			.isEqualTo("ACCEPTED");
		client.delete()
			.uri(PATH + "/" + id)
			.headers(headers -> headers.setBearerAuth(recipient.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(204);
		assertTrue(queries.page(sender.owner(), 0).entries().isEmpty());
		assertThrows(BusinessException.class, () -> queries.requireFriendOwner(sender.owner(), recipient.character()));
	}

	@Test
	void reciprocalConcurrentRequestsCreateOnePendingRelation() throws Exception {
		var first = player();
		var second = player();
		var start = new CountDownLatch(1);
		long id;
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var one = executor.submit(() -> {
				start.await();
				return commands.request(first.owner(), second.name());
			});
			var two = executor.submit(() -> {
				start.await();
				return commands.request(second.owner(), first.name());
			});
			start.countDown();
			id = one.get(15, TimeUnit.SECONDS);
			assertEquals(id, two.get(15, TimeUnit.SECONDS));
		}
		var firstEntry = queries.page(first.owner(), 0).entries().getFirst();
		var secondEntry = queries.page(second.owner(), 0).entries().getFirst();
		assertEquals(Set.of("INCOMING", "OUTGOING"), Set.of(firstEntry.status(), secondEntry.status()));
		assertEquals(Long.toString(id), firstEntry.id());
		assertEquals(firstEntry.id(), secondEntry.id());
		assertThrows(BusinessException.class, () -> queries.requireFriendOwner(first.owner(), second.character()));
		commands.remove(first.owner(), id);
	}

	@Test
	void rejectedRequestCannotBeAcceptedAfterNewRequestAndRollbackRestoresRelation() {
		var first = player();
		var second = player();
		long oldId = commands.request(first.owner(), second.name());
		commands.remove(second.owner(), oldId);
		commands.remove(second.owner(), oldId);
		long currentId = commands.request(first.owner(), second.name());
		assertNotEquals(oldId, currentId);
		assertThrows(BusinessException.class, () -> commands.accept(second.owner(), oldId));
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			commands.accept(second.owner(), currentId);
			throw new IllegalStateException("回滚接受");
		}));
		assertEquals("INCOMING", queries.page(second.owner(), 0).entries().getFirst().status());
		commands.accept(second.owner(), currentId);
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			commands.remove(first.owner(), currentId);
			throw new IllegalStateException("回滚删除");
		}));
		assertEquals(second.owner(), queries.requireFriendOwner(first.owner(), second.character()));
		commands.remove(first.owner(), currentId);
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			commands.request(first.owner(), second.name());
			throw new IllegalStateException("回滚申请");
		}));
		assertTrue(queries.page(first.owner(), 0).entries().isEmpty());
	}

	private record Player(long owner, long character, String name, String token) {
	}

}
