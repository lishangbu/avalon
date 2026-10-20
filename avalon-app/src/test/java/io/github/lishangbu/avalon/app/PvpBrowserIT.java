package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.world.application.command.GatheringCommandService;
import io.github.lishangbu.avalon.world.application.command.LifeSkillCommandService;
import io.github.lishangbu.avalon.world.application.model.GatheringStart;
import io.github.lishangbu.avalon.world.application.model.LifeSkillRequest;
import io.github.lishangbu.avalon.world.persistence.repository.GatheringSessionRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** 隔离角色夹具进入洞口；真实页面操作免战、对战和认输，服务器使用正常时钟与数据库。 */
@EnabledIfEnvironmentVariable(named = "AVALON_PVP_BROWSER", matches = "1")
class PvpBrowserIT extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService pets;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	LifeSkillCommandService learning;

	@Autowired
	GatheringCommandService gathering;

	@Autowired
	GatheringSessionRepository sessions;

	@Autowired
	JdbcTemplate jdbc;

	private long player(String name) {
		registrations.register(name, "Pvp-browser-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.grantExperience(owner, 7380);
			creatures.grant(owner, "PVP_BROWSER", "SPROUT");
			inventory.grant(owner, "PVP_BROWSER", "PVP_TRUCE_TOKEN", 1, false);
			facts.record(owner, "QUEST:O006");
			facts.record(owner, "QUEST:O008");
		});
		learning.change(owner, new LifeSkillRequest(UUID.randomUUID().toString(), LifeSkillRulesSkill.MINING, false),
				false);
		jdbc.update("UPDATE player_character SET location='CAVE_ENTRANCE' WHERE id=?", queries.requireId(owner));
		return owner;
	}

	@ParameterizedTest
	@ValueSource(strings = { "desktop", "mobile" })
	void sharedBattleInterruptsGatheringAndRedirectsDefender(String project) throws Exception {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		String attacker = "pvpa_" + suffix, defender = "pvpb_" + suffix;
		player(attacker);
		long owner = player(defender);
		String tool = items.list(owner)
			.stream()
			.filter(item -> item.code().equals("TOOL_WOOD_PICKAXE"))
			.findFirst()
			.orElseThrow()
			.id();
		gathering.begin(owner, new GatheringStart(UUID.randomUUID().toString(), "MINING_1",
				pets.list(owner).getFirst().id(), tool, 1));
		Path root = Path.of("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve("avalon-ui/package.json")))
			root = root.getParent();
		assertNotNull(root);
		var command = new ArrayList<String>();
		if (System.getProperty("os.name").startsWith("Windows"))
			command.addAll(List.of("cmd.exe", "/d", "/c", "npm.cmd"));
		else
			command.add("npm");
		command.addAll(List.of("run", "test:e2e", "--", "pvp.spec.ts", "--project=" + project, "--workers=1",
				"--reporter=list"));
		var log = root.resolve("quest-pvp-browser-" + project + ".log");
		var builder = new ProcessBuilder(command).directory(root.resolve("avalon-ui").toFile())
			.redirectErrorStream(true)
			.redirectOutput(log.toFile());
		builder.environment().put("AVALON_BASE_URL", "http://127.0.0.1:" + port);
		builder.environment().put("AVALON_PVP_ATTACKER", attacker);
		builder.environment().put("AVALON_PVP_DEFENDER", defender);
		var browser = builder.start();
		try {
			assertTrue(browser.waitFor(180, TimeUnit.SECONDS), "浏览器超时，见 " + log);
			assertEquals(0, browser.exitValue(), "浏览器失败，见 " + log);
			assertNull(sessions.find(queries.requireId(owner)));
			assertEquals("SQUARE", queries.find(owner).location());
		}
		finally {
			if (browser.isAlive()) {
				browser.descendants().forEach(ProcessHandle::destroy);
				browser.destroyForcibly();
			}
		}
	}

}
