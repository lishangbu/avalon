package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import jakarta.persistence.EntityManager;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 准备港城邀请之前的存档；BQ057～066的移动、选项、战斗和领奖全部由浏览器操作。 */
@EnabledIfEnvironmentVariable(named = "AVALON_PORT_BROWSER", matches = "1")
class PortBrowserIT extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	EntityManager em;

	@Autowired
	QuestCatalog questCatalog;

	@Autowired
	GrowthRules growth;

	@Test
	void portPreparationAndFirstVoyageWorkInBothBrowsers() throws Exception {
		String prefix = "port_ui_" + UUID.randomUUID().toString().substring(0, 8);
		for (String device : new String[] { "desktop", "mobile" }) {
			String username = prefix + "_" + device;
			registrations.register(username, "Port-browser-1234", "测试");
			long owner = accounts.accountId(username);
			characters.create(owner, "港城测试_" + device, "WARRIOR");
			transactions.run(() -> {
				long id = characters.lock(owner).id();
				em.createNativeQuery("UPDATE player_character SET location='GNOME_ROWAN_SHOP' WHERE id=:id")
					.setParameter("id", id)
					.executeUpdate();
				// 完整准备前置回执及事实，页面不能将已经完成的开篇误显示为当前任务。
				for (var step : questCatalog.definition().steps()) {
					int number = Integer.parseInt(step.id().substring(2));
					if (number > 56)
						continue;
					facts.record(owner, "QUEST:" + step.id());
					for (String fact : step.objectives().stream().map(goal -> goal.code()).toList())
						facts.record(owner, fact);
					em.createNativeQuery(
							"INSERT INTO quest_receipt(id, character_id, step, quest_code, completed, response, created_at)"
									+ " VALUES (:receipt, :id, :step, :quest, :complete, '{}', now())")
						.setParameter("receipt", -id - number)
						.setParameter("id", id)
						.setParameter("step", step.id())
						.setParameter("quest", step.quest())
						.setParameter("complete", step.completes())
						.executeUpdate();
				}
			});
			// 页面交互采用明确的15级战斗夹具；自然升级另由NaturalExtendedStoryTest验证。
			transactions.run(() -> characters.grantExperience(owner, Math.toIntExact(growth.threshold(15))));
			for (int position = 0; position < 2; position++) {
				String species = position == 0 ? "THUNDER_LIZARD" : "STONE_GOLEM";
				long pet = transactions.call(() -> creatures.capture(owner, "PORT_BROWSER:" + species, species, 15, "A",
						"坚岩", List.of(positionSkill(species))));
				creatures.position(owner, pet, position);
			}
		}
		Path root = Path.of("").toAbsolutePath().getParent();
		Path log = root.resolve("avalon-app/target/port-browser.log");
		var builder = new ProcessBuilder("npm", "run", "test:e2e", "--", "port-story.spec.ts", "--workers=1")
			.directory(root.resolve("avalon-ui").toFile())
			.redirectErrorStream(true)
			.redirectOutput(log.toFile());
		builder.environment().put("AVALON_BASE_URL", "http://127.0.0.1:" + port);
		builder.environment().put("AVALON_PORT_USER", prefix);
		var process = builder.start();
		try {
			assertTrue(process.waitFor(480, TimeUnit.SECONDS), "浏览器超时，见 " + log);
			assertEquals(0, process.exitValue(), "浏览器失败，见 " + log);
		}
		finally {
			if (process.isAlive()) {
				process.descendants().forEach(ProcessHandle::destroy);
				process.destroyForcibly();
			}
		}
	}

	private String positionSkill(String species) {
		return species.equals("THUNDER_LIZARD") ? "雷牙" : "碎岩击";
	}

}
