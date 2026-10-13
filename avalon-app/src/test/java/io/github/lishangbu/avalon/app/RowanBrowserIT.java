package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import jakarta.persistence.EntityManager;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.creature.AbilitySlotsCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import java.util.List;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 只准备开篇结束存档；罗温线的道路、现场操作与领奖由手机及桌面浏览器完成。 */
@EnabledIfEnvironmentVariable(named = "AVALON_ROWAN_BROWSER", matches = "1")
class RowanBrowserIT extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	EntityManager em;

	@Autowired
	GrowthRules growth;

	@Autowired
	AbilitySlotsCatalog abilities;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	QuestCatalog questCatalog;

	@Test
	void rowanStoryIsPlayableInDesktopAndMobileBrowsers() throws Exception {
		String prefix = "rowan_ui_" + UUID.randomUUID().toString().substring(0, 8);
		for (String device : new String[] { "desktop", "mobile" }) {
			String username = prefix + "_" + device;
			registrations.register(username, "Rowan-browser-1234", "测试");
			long owner = accounts.accountId(username);
			characters.create(owner, "罗温测试_" + device, "WARRIOR");
			transactions.run(() -> {
				long id = characters.lock(owner).id();
				em.createNativeQuery("UPDATE player_character SET location='GNOME_CITY' WHERE id=:id")
					.setParameter("id", id)
					.executeUpdate();
				// 夹具保留全部开篇回执与事实，避免页面错误地把早期任务显示为尚未完成。
				// 只补前置存档，不伪造本次浏览器负责验证的后续步骤。
				for (var step : questCatalog.definition().steps()) {
					int number = Integer.parseInt(step.id().substring(2));
					if (number > 30)
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
			// 12级队伍只用于真实页面战斗，目标任务事实仍全部由浏览器产生。
			transactions.run(() -> characters.grantExperience(owner, Math.toIntExact(growth.threshold(12))));
			for (int position = 0; position < 2; position++) {
				String species = position == 0 ? "THUNDER_LIZARD" : "STONE_GOLEM";
				long pet = transactions
					.call(() -> creatures.capture(owner, "STORY_BROWSER:" + species, species, 12, "A",
							abilities.ability(species, "A"), List.of(species.equals("THUNDER_LIZARD") ? "雷牙" : "碎岩击")));
				creatures.position(owner, pet, position);
			}
		}
		Path root = Path.of("").toAbsolutePath().getParent();
		Path log = root.resolve("avalon-app/target/rowan-browser.log");
		var builder = new ProcessBuilder("npm", "run", "test:e2e", "--", "rowan-story.spec.ts", "--workers=1")
			.directory(root.resolve("avalon-ui").toFile())
			.redirectErrorStream(true)
			.redirectOutput(log.toFile());
		builder.environment().put("AVALON_BASE_URL", "http://127.0.0.1:" + port);
		builder.environment().put("AVALON_ROWAN_USER", prefix);
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

}
