package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.DungeonInvitationCommandService;
import io.github.lishangbu.avalon.battle.application.command.DungeonLobbyCommandService;
import io.github.lishangbu.avalon.battle.application.query.DungeonInvitationQueryService;
import io.github.lishangbu.avalon.battle.application.query.DungeonLobbyQueryService;
import io.github.lishangbu.avalon.battle.application.query.DungeonRunQueryService;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonInvitationRepository;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.command.FriendshipCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/** 真实事务验证邀请不占位、接受才入队，以及最后空位竞争；HTTP 验证不能代替收件人处理。 */
class DungeonInvitationTest extends SpringIntegrationTest {

	@Autowired
	EntityManager entranceFixture;

	@Autowired
	GrowthRules growth;

	private static final String PATH = "/api/dungeon/invitations";

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	FriendshipCommandService friends;

	@Autowired
	DungeonInvitationCommandService invitations;

	@Autowired
	DungeonInvitationQueryService inbox;

	@Autowired
	DungeonInvitationRepository rows;

	@Autowired
	DungeonLobbyCommandService lobbies;

	@Autowired
	DungeonLobbyQueryService lobbyQueries;

	@Autowired
	DungeonRunQueryService runs;

	private Player player(boolean eligible) {
		String name = "invite_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		var tokens = registrations.register(name, "Dungeon-test-1234", "测试");
		long owner = accounts.accountId(name);
		var character = characters.create(owner, name, "WARRIOR");
		if (eligible)
			tx(() -> {
				characters.grantExperience(owner, Math.toIntExact(growth.threshold(15)));
				facts.record(owner, "QUEST:BQ029");
			});
		// 副本专项从集合地点开始；相邻移动由独立入口用例验证。
		transactions.run(() -> entranceFixture
			.createNativeQuery("UPDATE player_character SET location='FOREST_PEN' WHERE account_id=:owner")
			.setParameter("owner", owner)
			.executeUpdate());
		return new Player(owner, Long.parseLong(character.id()), name, tokens.accessToken());
	}

	private long befriend(Player sender, Player recipient) {
		long id = friends.request(sender.owner(), recipient.name());
		friends.accept(recipient.owner(), id);
		return id;
	}

	private long room(Player leader) {
		return lobbies.create(leader.owner(), UUID.randomUUID().toString());
	}

	private long send(Player leader, Player recipient, long room) {
		return invitations.send(leader.owner(), room, recipient.character(), UUID.randomUUID().toString());
	}

	private void tx(Runnable action) {
		transactions.run(action);
	}

	@Test
	void authenticatedRecipientAloneCanAcceptWithoutAutomaticConfirmationOrQuotaCost() {
		var leader = player(true);
		var recipient = player(true);
		var outsider = player(true);
		befriend(leader, recipient);
		long room = room(leader);
		String request = UUID.randomUUID().toString();
		var body = Map.of("lobby", Long.toString(room), "recipient", Long.toString(recipient.character()), "requestId",
				request);
		client.get().uri(PATH).exchange().expectStatus().isEqualTo(401);
		client.post()
			.uri(PATH)
			.contentType(MediaType.APPLICATION_JSON)
			.body(body)
			.exchange()
			.expectStatus()
			.isEqualTo(401);
		String id = jsonMapper
			.readTree(client.post()
				.uri(PATH)
				.headers(headers -> headers.setBearerAuth(leader.token()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
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
		client.post()
			.uri(PATH)
			.headers(headers -> headers.setBearerAuth(leader.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(body)
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.id")
			.isEqualTo(id);
		assertNull(characterQueries.lobby(recipient.owner()));
		assertEquals(1, lobbyQueries.restore(room, leader.owner()).getMembers().size());
		var response = jsonMapper.readTree(client.get()
			.uri(PATH)
			.headers(headers -> headers.setBearerAuth(recipient.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.invitations")
			.value(List.class, actual -> Assertions.assertThat(actual).hasSize(1))
			.jsonPath("$.invitations[0].sender")
			.isEqualTo(leader.name())
			.returnResult()
			.getResponseBody());
		assertEquals(Set.of("id", "lobby", "sender", "dungeon", "members", "capacity", "expiresAt"),
				response.at("/invitations/0").propertyNames());
		client.get()
			.uri(PATH)
			.headers(headers -> headers.setBearerAuth(outsider.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.invitations")
			.value(List.class, actual -> Assertions.assertThat(actual).hasSize(0));
		client.post()
			.uri(PATH + "/" + id + "/accept")
			.headers(headers -> headers.setBearerAuth(outsider.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		client.post()
			.uri(PATH + "/" + id + "/decline")
			.headers(headers -> headers.setBearerAuth(leader.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		for (int attempt = 0; attempt < 2; attempt++)
			client.post()
				.uri(PATH + "/" + id + "/accept")
				.headers(headers -> headers.setBearerAuth(recipient.token()))
				.exchange()
				.expectStatus()
				.isEqualTo(200)
				.expectBody()
				.jsonPath("$.lobby")
				.isEqualTo(Long.toString(room));
		var member = lobbyQueries.restore(room, recipient.owner()).getMembers().get(recipient.owner());
		assertFalse(member.isConfirmed());
		assertFalse(member.isReady());
		assertNull(member.getDeadline());
		assertEquals(3, runs.remainingStarts(leader.owner()));
		assertEquals(3, runs.remainingStarts(recipient.owner()));
		lobbies.leave(room, recipient.owner());
		assertEquals(room, invitations.accept(recipient.owner(), Long.parseLong(id)));
		assertNull(characterQueries.lobby(recipient.owner()));
		lobbies.leave(room, leader.owner());
	}

	@Test
	void expiredDeclinedDeletedFriendAndUnqualifiedInvitationsCannotBypassAdmission() {
		var leader = player(true);
		var recipient = player(true);
		var unqualified = player(false);
		long friendship = befriend(leader, recipient);
		befriend(leader, unqualified);
		long room = room(leader);
		long id = send(leader, recipient, room);
		tx(() -> rows.get(id).setExpiresAt(Instant.now().minusSeconds(1)));
		assertTrue(inbox.inbox(recipient.owner(), 0).invitations().isEmpty());
		assertThrows(BusinessException.class, () -> invitations.accept(recipient.owner(), id));
		long fresh = send(leader, recipient, room);
		assertNotEquals(id, fresh);
		invitations.decline(recipient.owner(), fresh);
		invitations.decline(recipient.owner(), fresh);
		assertThrows(BusinessException.class, () -> invitations.accept(recipient.owner(), fresh));
		long revoked = send(leader, recipient, room);
		friends.remove(recipient.owner(), friendship);
		assertTrue(inbox.inbox(recipient.owner(), 0).invitations().isEmpty());
		assertThrows(BusinessException.class, () -> invitations.accept(recipient.owner(), revoked));
		long gated = send(leader, unqualified, room);
		assertThrows(BusinessException.class, () -> invitations.accept(unqualified.owner(), gated));
		assertNull(characterQueries.lobby(unqualified.owner()));
		assertEquals("PENDING", transactions.call(() -> rows.get(gated).getStatus()));
		lobbies.leave(room, leader.owner());
		assertTrue(inbox.inbox(unqualified.owner(), 0).invitations().isEmpty());
	}

	@Test
	void concurrentAcceptsShareOneLastPositionAndLosingInvitationIsNotConsumed() throws Exception {
		var leader = player(true);
		var member = player(true);
		var first = player(true);
		var second = player(true);
		befriend(leader, first);
		befriend(leader, second);
		long room = room(leader);
		lobbies.join(room, member.owner());
		long one = send(leader, first, room), two = send(leader, second, room);
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var attempts = List.of(first, second).stream().map(player -> executor.submit(() -> {
				start.await();
				try {
					invitations.accept(player.owner(), player == first ? one : two);
					return true;
				}
				catch (BusinessException exception) {
					assertEquals(409, exception.status());
					return false;
				}
			})).toList();
			start.countDown();
			int accepted = 0;
			for (var attempt : attempts)
				if (attempt.get(15, TimeUnit.SECONDS))
					accepted++;
			assertEquals(1, accepted);
		}
		assertEquals(3, lobbyQueries.restore(room, leader.owner()).getMembers().size());
		tx(() -> {
			var statuses = List.of(rows.get(one).getStatus(), rows.get(two).getStatus());
			assertTrue(statuses.contains("PENDING"));
			assertTrue(statuses.contains("ACCEPTED"));
		});
		for (var player : List.of(first, second, member, leader))
			if (characterQueries.lobby(player.owner()) != null)
				lobbies.leave(room, player.owner());
	}

	@Test
	void rollbackRestoresInvitationAndPositionAndSenderMustRemainLeader() {
		var leader = player(true);
		var recipient = player(true);
		var member = player(true);
		befriend(leader, recipient);
		befriend(member, recipient);
		long room = room(leader);
		lobbies.join(room, member.owner());
		assertThrows(BusinessException.class, () -> send(member, recipient, room));
		String request = UUID.randomUUID().toString();
		assertThrows(IllegalStateException.class, () -> tx(() -> {
			invitations.send(leader.owner(), room, recipient.character(), request);
			throw new IllegalStateException("回滚发送");
		}));
		assertTrue(inbox.inbox(recipient.owner(), 0).invitations().isEmpty());
		long id = invitations.send(leader.owner(), room, recipient.character(), request);
		assertThrows(IllegalStateException.class, () -> tx(() -> {
			invitations.accept(recipient.owner(), id);
			throw new IllegalStateException("回滚接受");
		}));
		assertNull(characterQueries.lobby(recipient.owner()));
		assertEquals(1, inbox.inbox(recipient.owner(), 0).invitations().size());
		assertThrows(BusinessException.class,
				() -> invitations.send(leader.owner(), room, member.character(), request));
		lobbies.leave(room, leader.owner());
		lobbies.lead(room, member.owner());
		assertThrows(BusinessException.class, () -> invitations.accept(recipient.owner(), id));
		long renewed = send(member, recipient, room);
		assertNotEquals(id, renewed);
		assertEquals(room, invitations.accept(recipient.owner(), renewed));
		lobbies.leave(room, recipient.owner());
		lobbies.leave(room, member.owner());
	}

	private record Player(long owner, long character, String name, String token) {
	}

}
