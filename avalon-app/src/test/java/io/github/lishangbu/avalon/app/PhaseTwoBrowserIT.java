package io.github.lishangbu.avalon.app;

import tools.jackson.databind.ObjectMapper;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.inventory.domain.EnhancementRoll;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 显式启用的真实浏览器验收：Testcontainers隔离数据库，真实认证/事务/接口，构建后的Vue静态文件。
 * 仅替换人物等级与随机边界，不在生产包添加调试入口，不接触用户原数据库。
 */
@EnabledIfEnvironmentVariable(named = "AVALON_PHASE_TWO_BROWSER", matches = "1")
class PhaseTwoBrowserIT extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	EntityManager em;

	@Autowired
	ObjectMapper json;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureRepository pets;

	@Autowired
	CreatureQueryService creatureQueries;

	private record Player(String username, String password, String itemId, long owner) {
	}

	private record Researcher(Player player, String petId) {
	}

	private record PetMarket(Player seller, Player buyer, String petId) {
	}

	private PetMarket petMarket() {
		Player seller = player(), buyer = player();
		// 双方都保留可用伙伴，允许目标个体退货后再次上架，不绕过最后伙伴保护。
		for (var player : List.of(seller, buyer)) {
			long retained = transactions.call(() -> creatures.capture(player.owner(), "BROWSER:RETAINED", "CLAW_CAT", 3,
					"A", "完好防护", List.of("水弹", "微疗")));
			creatures.position(player.owner(), retained, 0);
		}
		long petId = transactions.call(() -> creatures.capture(seller.owner(), "BROWSER:" + UUID.randomUUID(),
				"STONE_GOLEM", 8, "H", "守护", List.of("碎岩击")));
		// 普通携带位可交易，出战及替补位禁止；不绕过正式编队命令。
		creatures.position(seller.owner(), petId, 4);
		return new PetMarket(seller, buyer, Long.toString(petId));
	}

	/** 仅建立高等级初始个体与凭证；教学、兑换、绑定选择和进化必须经浏览器办理。 */
	private Researcher researcher() {
		Player player = player();
		long petId = transactions.call(() -> {
			long id = creatures.capture(player.owner(), "BROWSER:" + UUID.randomUUID(), "GALE_SPARROW", 8, "H", "乘风",
					List.of("风刃", "逆风"));
			var pet = pets.get(id);
			pet.setLevel(20);
			pet.setExperience(19000);
			inventory.grantStrict(player.owner(), "RESEARCH", "RESEARCH_TOKEN", 12, false, true);
			long character = characters.lock(player.owner()).id();
			em.flush();
			em.clear();
			em.createNativeQuery("UPDATE player_character SET location='CITY_PETS' WHERE id=:id")
				.setParameter("id", character)
				.executeUpdate();
			return id;
		});
		return new Researcher(player, Long.toString(petId));
	}

	private Player player() {
		String username = "browser_" + UUID.randomUUID().toString().substring(0, 8);
		String password = "Browser-phase-two-1234";
		registrations.register(username, password, "测试");
		long owner = accounts.accountId(username);
		characters.create(owner, username, "WARRIOR");
		transactions.run(() -> {
			characters.changeGold(owner, 1000);
			// 城市开放与冒险资格分别记录，不能以资格事实代替地图进入前置。
			facts.record(owner, "QUEST:BQ024");
			facts.record(owner, "QUEST:BQ029");
			inventory.grantStrict(owner, "GEAR", "STARTER_WEAPON", 1, true, false);
			inventory.grantStrict(owner, "STONES", "ENHANCEMENT_STONE_I", 20, false, false);
			long id = characters.lock(owner).id();
			em.flush();
			em.clear();
			em.createNativeQuery("UPDATE player_character SET location='CITY_SHOP' WHERE id=:id")
				.setParameter("id", id)
				.executeUpdate();
		});
		return new Player(username, password,
				items.list(owner).stream().filter(i -> i.slot() != null).findFirst().orElseThrow().id(), owner);
	}

	@Test
	void mobileAndDesktopUseRealEnhancementAndMarket() throws Exception {
		Path root = Path.of("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve("avalon-ui/package.json")))
			root = root.getParent();
		assertNotNull(root);
		assertTrue(Files.isRegularFile(root.resolve("avalon-ui/dist/index.html")), "请先构建前端");
		TestDoubles.install(new GrowthRules() {
			@Override
			public int level(long experience) {
				return 30;
			}

			@Override
			public int carryingCapacity(int level) {
				return 8;
			}
		}, GrowthRules.class);
		TestDoubles.install(new EnhancementRoll() {
			@Override
			public int next() {
				return 0;
			}
		}, EnhancementRoll.class);
		// 真实静态服务回退页面，但不能把缺失脚本或未知健康端点伪装成 HTML。
		client.get()
			.uri("/world")
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectHeader()
			.contentTypeCompatibleWith("text/html");
		client.get()
			.uri("/future-page")
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectHeader()
			.contentTypeCompatibleWith("text/html");
		client.get().uri("/assets/missing.js").exchange().expectStatus().isEqualTo(404);
		client.get().uri("/actuator/not-a-health-endpoint").exchange().expectStatus().isEqualTo(404);
		Process browser = null;
		Path temporary = Files.createTempDirectory("avalon-phase-two-browser-");
		Path log = root.resolve("avalon-app/target/phase-two-browser.log");
		try {
			var desktop = Map.of("seller", player(), "buyer", player());
			var mobile = Map.of("seller", player(), "buyer", player());
			var desktopResearch = researcher();
			var mobileResearch = researcher();
			var desktopPets = petMarket();
			var mobilePets = petMarket();
			Path fixture = temporary.resolve("fixture.json");
			Files.writeString(fixture,
					json.writeValueAsString(Map.of("desktop",
							Map.of("seller", desktop.get("seller"), "buyer", desktop.get("buyer"), "research",
									desktopResearch, "petMarket", desktopPets),
							"mobile", Map.of("seller", mobile.get("seller"), "buyer", mobile.get("buyer"), "research",
									mobileResearch, "petMarket", mobilePets))));
			// 直接验证 Spring Boot 提供的静态产物、SPA 回退和安全响应头，不再绕过应用启动代理。
			var builder = new ProcessBuilder("npm", "run", "test:e2e", "--", "phase-two.spec.ts", "--workers=1")
				.directory(root.resolve("avalon-ui").toFile())
				.redirectErrorStream(true)
				.redirectOutput(log.toFile());
			builder.environment().put("AVALON_BASE_URL", "http://127.0.0.1:" + port);
			builder.environment().put("AVALON_PHASE_TWO_FIXTURE", fixture.toString());
			builder.environment().put("AVALON_LAYOUT_AUDIT", "1");
			browser = builder.start();
			assertTrue(browser.waitFor(240, TimeUnit.SECONDS), "浏览器验收超时，见 " + log);
			assertEquals(0, browser.exitValue(), () -> "浏览器验收失败，见 " + log);
			for (var scenario : List.of(desktop, mobile)) {
				String id = scenario.get("seller").itemId();
				assertFalse(items.list(scenario.get("seller").owner()).stream().anyMatch(i -> i.id().equals(id)));
				assertEquals(1,
						items.list(scenario.get("buyer").owner())
							.stream()
							.filter(i -> i.id().equals(id))
							.findFirst()
							.orElseThrow()
							.enhancementLevel());
			}
			for (var research : List.of(desktopResearch, mobileResearch)) {
				transactions.run(() -> {
					var pet = pets.get(Long.parseLong(research.petId()));
					assertEquals("GALE_FALCON", pet.getSpecies());
					assertTrue(pet.isBound());
					assertEquals("H", pet.getAbilitySlot());
					assertEquals(20, pet.getLevel());
				});
			}
			for (var market : List.of(desktopPets, mobilePets)) {
				assertThrows(BusinessException.class,
						() -> creatureQueries.provenance(market.seller().owner(), Long.parseLong(market.petId())));
				var provenance = creatureQueries.provenance(market.buyer().owner(), Long.parseLong(market.petId()));
				assertFalse(provenance.bound());
				assertEquals(market.seller().username(), provenance.captorName());
				transactions.run(() -> {
					var pet = pets.get(Long.parseLong(market.petId()));
					assertEquals(characters.lock(market.buyer().owner()).id(), pet.getCharacterId());
					assertEquals(characters.lock(market.seller().owner()).id(), pet.getCaptorCharacterId());
					assertEquals("H", pet.getAbilitySlot());
					assertEquals(100, pet.getPartyPosition());
					assertFalse(pet.isBound());
				});
			}
		}
		finally {
			if (browser != null && browser.isAlive()) {
				browser.descendants().forEach(ProcessHandle::destroyForcibly);
				browser.destroyForcibly();
				browser.waitFor(10, TimeUnit.SECONDS);
			}
			TestDoubles.install(new GrowthRules(), GrowthRules.class);
			TestDoubles.install(new EnhancementRoll(), EnhancementRoll.class);
			try (var files = Files.walk(temporary)) {
				for (var file : files.sorted(Comparator.reverseOrder()).toList())
					Files.deleteIfExists(file);
			}
		}
	}

}
