package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/** 经过真实认证和 HTTP 序列化验证招募边界，不能用内部命令测试代替接口权限检查。 */
class DungeonRecruitmentTest extends SpringIntegrationTest {

	@Autowired
	EntityManager entranceFixture;

	@Autowired
	GrowthRules growth;

	private static final String PATH = "/api/dungeon/lobbies";

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
	CreatureCommandService creatures;

	@Autowired
	CombatVitalsStore combatVitalsStore;

	@Autowired
	CombatVitalsRepository combatVitalsRepository;

	private Player player(boolean eligible) {
		String name = "recruit_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		var tokens = registrations.register(name, "Dungeon-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		if (eligible)
			transactions.run(() -> {
				characters.grantExperience(owner, Math.toIntExact(growth.threshold(15)));
				facts.record(owner, "QUEST:O014");
			});
		// 副本专项从集合地点开始；相邻移动由独立入口用例验证。
		transactions.run(() -> entranceFixture
			.createNativeQuery("UPDATE player_character SET location='FOREST_PEN' WHERE account_id=:owner")
			.setParameter("owner", owner)
			.executeUpdate());
		return new Player(owner, tokens.accessToken());
	}

	private String create(Player player, String request) {
		return jsonMapper
			.readTree(client.post()
				.uri(PATH)
				.headers(headers -> headers.setBearerAuth(player.token()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("requestId", request))
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

	private void command(Player player, String lobby, String action) {
		client.post()
			.uri(PATH + "/" + lobby + "/" + action)
			.headers(headers -> headers.setBearerAuth(player.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(204);
	}

	@Test
	void privateViewsRequireMembershipAndRecruitmentDoesNotExposeInternalState() {
		var leader = player(true);
		var outsider = player(false);
		String request = UUID.randomUUID().toString();
		String lobby = create(leader, request);
		assertEquals(lobby, create(leader, request));
		client.get().uri(PATH).exchange().expectStatus().isEqualTo(401);
		client.get()
			.uri(PATH + "/" + lobby)
			.headers(headers -> headers.setBearerAuth(outsider.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		client.post()
			.uri(PATH + "/" + lobby + "/join")
			.headers(headers -> headers.setBearerAuth(outsider.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(409);
		client.get()
			.uri(PATH + "?after=-1")
			.headers(headers -> headers.setBearerAuth(leader.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(400);
		// 从目标之前开始翻页，避免测试库已有队伍影响有界扫描结果。
		var publicView = client.get()
			.uri(builder -> builder.path(PATH).queryParam("after", Long.parseLong(lobby) - 1).build())
			.headers(headers -> headers.setBearerAuth(outsider.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.returnResult(JsonNode.class)
			.getResponseBody();
		assertEquals(lobby, publicView.at("/offers/0/id").asString());
		var offer = publicView.at("/offers/0");
		assertEquals(Set.of("id", "name", "leader", "members", "capacity", "professions"), offer.propertyNames());
		var view = jsonMapper.readTree(client.get()
			.uri(PATH + "/current")
			.headers(headers -> headers.setBearerAuth(leader.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.lobby.id")
			.isEqualTo(lobby)
			.jsonPath("$.lobby.canLaunch")
			.isEqualTo(false)
			.returnResult()
			.getResponseBody());
		assertEquals(characterQueries.find(leader.owner()).id(), view.at("/lobby/members/0/id").asString());
		assertTrue(view.at("/lobby/members/0/self").asBoolean());
		String json = view.toPrettyString();
		for (String internal : List.of("preparedRevision", "randomSeed", "accountId", "owner")) {
			assertFalse(json.contains("\"" + internal + "\""));
		}
		command(leader, lobby, "leave");
		client.get()
			.uri(PATH + "/current")
			.headers(headers -> headers.setBearerAuth(leader.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.lobby")
			.isEqualTo(null);
	}

	@Test
	void matchingHttpRequiresAuthenticationAndNoticeOnlyExposesOwnConfirmation() {
		client.post().uri(PATH + "/match").exchange().expectStatus().isEqualTo(401);
		client.get().uri(PATH + "/notice").exchange().expectStatus().isEqualTo(401);
		var players = List.of(player(true), player(true), player(true));
		String lobby = null;
		for (var player : players) {
			String allocated = client.post()
				.uri(PATH + "/match")
				.headers(headers -> headers.setBearerAuth(player.token()))
				.exchange()
				.expectStatus()
				.isEqualTo(200)
				.returnResult(JsonNode.class)
				.getResponseBody()
				.at("/id")
				.asString();
			if (lobby == null)
				lobby = allocated;
			assertEquals(lobby, allocated);
		}
		var leader = players.getFirst();
		client.post()
			.uri(PATH + "/match")
			.headers(headers -> headers.setBearerAuth(leader.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.id")
			.isEqualTo(lobby);
		var notice = jsonMapper.readTree(client.get()
			.uri(PATH + "/notice")
			.headers(headers -> headers.setBearerAuth(leader.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.id")
			.isEqualTo(lobby)
			.jsonPath("$.confirmed")
			.isEqualTo(false)
			.jsonPath("$.members")
			.isEqualTo(3)
			.jsonPath("$.deadline")
			.exists()
			.returnResult()
			.getResponseBody());
		assertEquals(Set.of("id", "confirmed", "deadline", "inBattle", "members"), notice.at("").propertyNames());
		command(leader, lobby, "confirm");
		client.get()
			.uri(PATH + "/notice")
			.headers(headers -> headers.setBearerAuth(leader.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.confirmed")
			.isEqualTo(true);
		client.get()
			.uri(PATH + "/notice")
			.headers(headers -> headers.setBearerAuth(players.get(1).token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.confirmed")
			.isEqualTo(false);
		for (var player : players)
			command(player, lobby, "leave");
		client.get()
			.uri(PATH + "/notice")
			.headers(headers -> headers.setBearerAuth(leader.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.id")
			.isEqualTo(null);
	}

	@Test
	void replacementRequiresPublicationAndExplicitCandidatePreparation() {
		var players = List.of(player(true), player(true), player(true));
		var leader = players.getFirst();
		var candidate = player(true);
		String lobby = create(leader, UUID.randomUUID().toString());
		for (var member : players.subList(1, 3))
			command(member, lobby, "join");
		for (var member : players) {
			command(member, lobby, "confirm");
			command(member, lobby, "ready");
		}
		String run = client.post()
			.uri(PATH + "/" + lobby + "/launch")
			.headers(headers -> headers.setBearerAuth(leader.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.returnResult(JsonNode.class)
			.getResponseBody()
			.at("/run")
			.asString();
		String runPath = "/api/dungeon/runs/" + run;
		client.post()
			.uri(runPath + "/leave")
			.headers(headers -> headers.setBearerAuth(players.getLast().token()))
			.exchange()
			.expectStatus()
			.isEqualTo(204);
		client.get().uri(PATH + "/replacements").exchange().expectStatus().isEqualTo(401);
		client.post()
			.uri(runPath + "/recruit")
			.headers(headers -> headers.setBearerAuth(candidate.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("enabled", true))
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		client.post()
			.uri(runPath + "/recruit")
			.headers(headers -> headers.setBearerAuth(leader.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("enabled", true))
			.exchange()
			.expectStatus()
			.isEqualTo(204);
		var offer = client.get()
			.uri(builder -> builder.path(PATH + "/replacements").queryParam("after", Long.parseLong(run) - 1).build())
			.headers(headers -> headers.setBearerAuth(candidate.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.returnResult(JsonNode.class)
			.getResponseBody();
		assertEquals(run, offer.at("/offers/0/id").asString());
		assertEquals(Set.of("id", "name", "cleared", "stages", "members", "capacity", "professions"),
				offer.at("/offers/0").propertyNames());
		String confirmation = client.post()
			.uri(PATH + "/replacements/" + run)
			.headers(headers -> headers.setBearerAuth(candidate.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("requestId", UUID.randomUUID().toString()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.returnResult(JsonNode.class)
			.getResponseBody()
			.at("/id")
			.asString();
		client.get()
			.uri(PATH + "/" + confirmation)
			.headers(headers -> headers.setBearerAuth(candidate.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.targetRun")
			.isEqualTo(run)
			.jsonPath("$.canLaunch")
			.isEqualTo(false);
		command(candidate, confirmation, "confirm");
		command(candidate, confirmation, "ready");
		client.get()
			.uri(PATH + "/" + confirmation)
			.headers(headers -> headers.setBearerAuth(candidate.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.canLaunch")
			.isEqualTo(true);
		client.post()
			.uri(PATH + "/" + confirmation + "/launch")
			.headers(headers -> headers.setBearerAuth(candidate.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.run")
			.isEqualTo(run);
		client.get()
			.uri(builder -> builder.path(PATH + "/replacements").queryParam("after", Long.parseLong(run) - 1).build())
			.headers(headers -> headers.setBearerAuth(candidate.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.offers[?(@.id == '" + run + "')]")
			.value(List.class, actual -> Assertions.assertThat(actual).hasSize(0));
		for (var member : List.of(leader, players.get(1), candidate))
			client.post()
				.uri(runPath + "/leave")
				.headers(headers -> headers.setBearerAuth(member.token()))
				.exchange()
				.expectStatus()
				.isEqualTo(204);
	}

	@Test
	void preparationShowsSavedInjuryAndOnlyFourBattlePetsWithoutHealingOnRead() {
		var leader = player(true);
		long character = Long.parseLong(characterQueries.find(leader.owner()).id());
		transactions.run(() -> {
			for (int i = 0; i < 5; i++)
				creatures.grant(leader.owner(), "RECRUIT_PET_" + i, "SPROUT");
			combatVitalsStore.saveVitals(character, character, 7, 3);
		});
		String lobby = create(leader, UUID.randomUUID().toString());
		for (int read = 0; read < 2; read++) {
			client.get()
				.uri(PATH + "/" + lobby)
				.headers(headers -> headers.setBearerAuth(leader.token()))
				.exchange()
				.expectStatus()
				.isEqualTo(200)
				.expectBody()
				.jsonPath("$.members[0].party")
				.value(List.class, actual -> Assertions.assertThat(actual).hasSize(5))
				.jsonPath("$.members[0].party[?(@.hero == true)].health")
				.isEqualTo(List.of(7))
				.jsonPath("$.members[0].party[?(@.hero == true)].mana")
				.isEqualTo(List.of(3));
		}
		transactions.run(() -> {
			assertEquals(7, combatVitalsRepository.get(character).getHp());
			assertEquals(3, combatVitalsRepository.get(character).getMp());
		});
		command(leader, lobby, "leave");
	}

	@Test
	void preparationProjectionInvalidatesReadinessWithoutMutatingTheTeam() {
		var players = List.of(player(true), player(true), player(true));
		var leader = players.getFirst();
		String lobby = create(leader, UUID.randomUUID().toString());
		for (var player : players.subList(1, 3))
			command(player, lobby, "join");
		for (var player : players) {
			command(player, lobby, "confirm");
			command(player, lobby, "ready");
		}
		client.get()
			.uri(PATH + "/" + lobby)
			.headers(headers -> headers.setBearerAuth(leader.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.canLaunch")
			.isEqualTo(true)
			.jsonPath("$.members")
			.value(List.class, actual -> Assertions.assertThat(actual).hasSize(3))
			.jsonPath("$.members[?(@.self == true)]")
			.value(List.class, actual -> Assertions.assertThat(actual).hasSize(1));
		transactions.run(() -> characters.invalidatePreparation(leader.owner()));
		client.get()
			.uri(PATH + "/" + lobby)
			.headers(headers -> headers.setBearerAuth(leader.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.canLaunch")
			.isEqualTo(false)
			.jsonPath("$.members[?(@.self == true)].ready")
			.isEqualTo(List.of(false));
		command(leader, lobby, "ready");
		client.get()
			.uri(PATH + "/" + lobby)
			.headers(headers -> headers.setBearerAuth(leader.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.canLaunch")
			.isEqualTo(true);
		for (var player : players)
			command(player, lobby, "leave");
	}

	private record Player(long owner, String token) {
	}

}
