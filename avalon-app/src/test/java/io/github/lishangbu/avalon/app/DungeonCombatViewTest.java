package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.DungeonRunCommandService;
import io.github.lishangbu.avalon.battle.application.query.DungeonCombatQueryService;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundStateSubmission;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.JsonNode;

/** 三账号战局读取测试：操作菜单归属、提交保密和物资额度由真实进度验证。 */
class DungeonCombatViewTest extends SpringIntegrationTest {

	@Autowired
	EntityManager entranceFixture;

	@Autowired
	GrowthRules growth;

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
	InventoryCommandService inventory;

	@Autowired
	DungeonRunCommandService commands;

	@Autowired
	DungeonCombatQueryService queries;

	private Player player() {
		String name = "combat_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		var tokens = registrations.register(name, "Dungeon-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.grantExperience(owner, Math.toIntExact(growth.threshold(15)));
			facts.record(owner, "QUEST:BQ029");
			inventory.grant(owner, "ENTRY", "HEALTH_POTION", 2, false);
			creatures.grant(owner, "PARTNER", "SPROUT");
		});
		// 副本专项从集合地点开始；相邻移动由独立入口用例验证。
		transactions.run(() -> entranceFixture
			.createNativeQuery("UPDATE player_character SET location='FOREST_PEN' WHERE account_id=:owner")
			.setParameter("owner", owner)
			.executeUpdate());
		return new Player(owner, characterQueries.find(owner).id(), tokens.accessToken());
	}

	private long run(List<Player> players) {
		long run = transactions.call(() -> commands.create(players.getFirst().owner(),
				players.stream().map(Player::owner).toList(), UUID.randomUUID().toString()));
		players.forEach(player -> commands.ready(run, player.owner()));
		return run;
	}

	@Test
	void privateCommandsAndExternalSuppliesAreNeverExposedAsUsable() {
		var players = List.of(player(), player(), player());
		var self = players.getFirst();
		long run = run(players);
		long battle = commands.begin(run, self.owner());
		transactions.run(() -> inventory.grant(self.owner(), "EXTERNAL", "HEALTH_POTION", 5, false));
		var view = queries.view(run, battle, self.owner());
		assertEquals(2, view.supplies().get("HEALTH_POTION"));
		assertTrue(view.canSubmit());
		assertEquals(2, view.units().stream().filter(unit -> unit.controllable()).count());
		assertTrue(view.units()
			.stream()
			.filter(unit -> !self.hero().equals(unit.owner()))
			.allMatch(unit -> unit.detail().actions().isEmpty()));
		assertTrue(view.units()
			.stream()
			.filter(unit -> unit.detail().enemy())
			.allMatch(unit -> unit.detail().ability() == null && unit.detail().captures().isEmpty()));
		var orders = view.units()
			.stream()
			.filter(unit -> unit.controllable())
			.map(unit -> new BattleStateOrder(unit.detail().id(), "DEFEND", unit.detail().id()))
			.toList();
		String path = "/api/dungeon/runs/" + run + "/battles/" + battle;
		var request = Map.of("requestId", UUID.randomUUID().toString(), "revision", view.revision(), "round",
				view.round(), "orders", orders, "replacements", Map.of());
		// 不能伪造队友行动；失败也不能提前锁住自己本轮。
		client.post()
			.uri(path + "/submit")
			.headers(headers -> headers.setBearerAuth(players.get(1).token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.exchange()
			.expectStatus()
			.isEqualTo(400);
		assertTrue(queries.view(run, battle, players.get(1).owner()).canSubmit());
		client.post()
			.uri(path + "/submit")
			.headers(headers -> headers.setBearerAuth(self.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("requestId", UUID.randomUUID(), "revision", view.revision(), "round", view.round(), "orders",
					orders))
			.exchange()
			.expectStatus()
			.isEqualTo(400);
		for (int retry = 0; retry < 2; retry++)
			client.post()
				.uri(path + "/submit")
				.headers(headers -> headers.setBearerAuth(self.token()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.exchange()
				.expectStatus()
				.isEqualTo(204);
		var locked = queries.view(run, battle, self.owner());
		assertFalse(locked.canSubmit());
		assertTrue(locked.units().stream().allMatch(unit -> unit.detail().actions().isEmpty()));
		var other = players.get(1);
		var response = jsonMapper.readTree(client.get()
			.uri(path)
			.headers(headers -> headers.setBearerAuth(other.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.self")
			.isEqualTo(other.hero())
			.jsonPath("$.id")
			.isEqualTo(Long.toString(battle))
			.returnResult()
			.getResponseBody());
		assertTrue(response.get("members")
			.valueStream()
			.filter(member -> member.get("id").asString().equals(self.hero()))
			.findFirst()
			.orElseThrow()
			.get("submitted")
			.asBoolean());
		String json = response.toPrettyString();
		for (String field : List.of("seed", "enemyOrders", "submissions", "orders", "abilitySlot", "withdrawn"))
			assertFalse(json.contains("\"" + field + "\""));
		client.get().uri(path).exchange().expectStatus().isEqualTo(401);
		client.get()
			.uri(path)
			.headers(headers -> headers.setBearerAuth(player().token()))
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		client.get()
			.uri("/api/dungeon/runs/" + run + "/battles/1")
			.headers(headers -> headers.setBearerAuth(other.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(404);
	}

	@Test
	void progressHttpRestReadyBeginAndReconnectRespectTheSameState() {
		var players = List.of(player(), player(), player());
		var self = players.getFirst();
		client.get()
			.uri("/api/dungeon/runs/current")
			.headers(headers -> headers.setBearerAuth(self.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.run")
			.isEqualTo(null)
			.jsonPath("$.remainingStarts")
			.isEqualTo(3);
		long run = run(players);
		String path = "/api/dungeon/runs/" + run;
		client.get()
			.uri("/api/dungeon/runs/current")
			.headers(headers -> headers.setBearerAuth(self.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.run.id")
			.isEqualTo(Long.toString(run))
			.jsonPath("$.remainingStarts")
			.isEqualTo(2)
			.jsonPath("$.run.canBegin")
			.isEqualTo(true)
			.jsonPath("$.run.canRest")
			.isEqualTo(true);
		client.post()
			.uri(path + "/move")
			.headers(headers -> headers.setBearerAuth(self.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of())
			.exchange()
			.expectStatus()
			.isEqualTo(400);
		client.post()
			.uri(path + "/move")
			.headers(headers -> headers.setBearerAuth(self.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("location", 1))
			.exchange()
			.expectStatus()
			.isEqualTo(409);
		client.post()
			.uri(path + "/rest")
			.headers(headers -> headers.setBearerAuth(self.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(204);
		client.get()
			.uri(path)
			.headers(headers -> headers.setBearerAuth(self.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.canBegin")
			.isEqualTo(false)
			.jsonPath("$.canReady")
			.isEqualTo(true);
		client.post()
			.uri(path + "/begin")
			.headers(headers -> headers.setBearerAuth(self.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(409);
		client.post()
			.uri(path + "/ready")
			.headers(headers -> headers.setBearerAuth(self.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(204);
		String battle = client.post()
			.uri(path + "/begin")
			.headers(headers -> headers.setBearerAuth(self.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.returnResult(JsonNode.class)
			.getResponseBody()
			.at("/battle")
			.asString();
		client.post()
			.uri(path + "/begin")
			.headers(headers -> headers.setBearerAuth(players.get(1).token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.battle")
			.isEqualTo(battle);
		client.get()
			.uri("/api/dungeon/runs/current")
			.headers(headers -> headers.setBearerAuth(self.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.run.battle")
			.isEqualTo(battle)
			.jsonPath("$.run.canBegin")
			.isEqualTo(false)
			.jsonPath("$.run.canReady")
			.isEqualTo(false)
			.jsonPath("$.run.canRest")
			.isEqualTo(false);
		client.post()
			.uri(path + "/rest")
			.headers(headers -> headers.setBearerAuth(self.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(409);
		client.post()
			.uri(path + "/move")
			.headers(headers -> headers.setBearerAuth(self.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("location", 0))
			.exchange()
			.expectStatus()
			.isEqualTo(409);
		var outsider = player();
		client.get()
			.uri(path)
			.headers(headers -> headers.setBearerAuth(outsider.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		client.post()
			.uri(path + "/ready")
			.headers(headers -> headers.setBearerAuth(outsider.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		client.get().uri(path).exchange().expectStatus().isEqualTo(401);
	}

	@Test
	void exitedMemberLosesReadAccessAfterTheirFinalRound() {
		var players = List.of(player(), player(), player());
		var self = players.getFirst();
		long run = run(players);
		long battle = commands.begin(run, self.owner());
		commands.leave(run, self.owner());
		assertFalse(queries.view(run, battle, self.owner()).canSubmit());
		for (var player : players.subList(1, 3)) {
			var view = queries.view(run, battle, player.owner());
			var orders = view.units()
				.stream()
				.filter(unit -> unit.controllable())
				.map(unit -> new BattleStateOrder(unit.detail().id(), "DEFEND", unit.detail().id()))
				.toList();
			DungeonTestCommands.submit(commands, run, player.owner(), battle, view.round(),
					new DungeonRoundStateSubmission(orders, Map.of()));
		}
		client.get()
			.uri("/api/dungeon/runs/" + run + "/battles/" + battle)
			.headers(headers -> headers.setBearerAuth(self.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		assertNull(characterQueries.adventure(self.owner()).dungeon());
	}

	private record Player(long owner, String hero, String token) {
	}

}
