package io.github.lishangbu.avalon.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** 当前故事的浏览器自然开篇；使用本次隔离数据库，不写入本地开发存档。 */
@EnabledIfEnvironmentVariable(named = "AVALON_STORY_BROWSER", matches = "1")
class StoryBrowserIT extends SpringIntegrationTest {

	@org.springframework.beans.factory.annotation.Autowired
	io.github.lishangbu.avalon.identity.application.command.RegistrationService registrations;

	@org.springframework.beans.factory.annotation.Autowired
	io.github.lishangbu.avalon.identity.application.query.AccountQueryService accounts;

	@org.springframework.beans.factory.annotation.Autowired
	io.github.lishangbu.avalon.character.application.command.CharacterCommandService characters;

	@org.springframework.beans.factory.annotation.Autowired
	io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService facts;

	@org.springframework.beans.factory.annotation.Autowired
	io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService inventory;

	@org.springframework.beans.factory.annotation.Autowired
	io.github.lishangbu.avalon.character.application.query.CharacterQueryService queries;

	@org.springframework.beans.factory.annotation.Autowired
	io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService acceptance;

	@org.springframework.beans.factory.annotation.Autowired
	org.springframework.jdbc.core.simple.JdbcClient jdbc;

	@org.springframework.beans.factory.annotation.Autowired
	io.github.lishangbu.avalon.world.persistence.repository.QuestRepository completionReceipts;

	@Test
	void storyWorksOnDesktopAndMobile() throws Exception {
		Path root = Path.of("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve("avalon-ui/package.json")))
			root = root.getParent();
		assertNotNull(root, "未找到前端工程目录");
		Path log = root.resolve("avalon-app/target/story-browser.log");
		String prefix = "branch_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		for (String project : java.util.List.of("desktop", "mobile"))
			prepareBranch(prefix + "_" + project);
		var command = new java.util.ArrayList<>(java.util.List.of("node", "node_modules/@playwright/test/cli.js",
				"test", "story-branches.spec.ts", "--workers=1"));
		if (!"1".equals(System.getenv("AVALON_STORY_BROWSER_BRANCHES_ONLY")))
			command.add("story-opening.spec.ts");
		var builder = new ProcessBuilder(command).directory(root.resolve("avalon-ui").toFile())
			.redirectErrorStream(true)
			.redirectOutput(log.toFile());
		builder.environment().put("AVALON_BASE_URL", "http://127.0.0.1:" + port);
		builder.environment().put("AVALON_STORY_BRANCH_USER", prefix);
		var browser = builder.start();
		try {
			assertTrue(browser.waitFor(10, TimeUnit.MINUTES), "故事浏览器操作超时，见 " + log);
			assertEquals(0, browser.exitValue(), "故事浏览器操作失败，见 " + log);
		}
		finally {
			if (browser.isAlive()) {
				browser.descendants().forEach(ProcessHandle::destroy);
				browser.destroyForcibly();
			}
		}
	}

	/** 仅为页面分支边界准备前置与四份木材，不作为自然成长证据。 */
	private void prepareBranch(String username) {
		registrations.register(username, "Story-branch-1234", "分支页面测试");
		long owner = accounts.accountId(username);
		characters.create(owner, "分支" + username.substring(7, 15) + username.substring(username.length() - 3),
				"WARRIOR");
		transactions.run(() -> {
			characters.grantExperience(owner, 30000);
			facts.record(owner, "QUEST:O014");
			for (String step : java.util.List.of("N002", "N003")) {
				var receipt = new io.github.lishangbu.avalon.world.persistence.entity.QuestReceipt();
				receipt.setCharacterId(queries.requireId(owner));
				receipt.setStep(step);
				receipt.setQuestCode(step);
				receipt.setCompleted(true);
				receipt.setResponse("{}");
				receipt.setCreatedAt(java.time.Instant.now());
				completionReceipts.save(receipt);
				facts.record(owner, "QUEST:" + step);
			}
			inventory.grantStrict(owner, "BRANCH_WOOD", "LIFE_WOOD_0", 4, false, false);
		});
		jdbc.sql("UPDATE player_character SET location='CITY_SHOP' WHERE id=:id")
			.param("id", queries.requireId(owner))
			.update();
		acceptance.accept(owner, "N004_FIGHT",
				new io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest(
						java.util.UUID.randomUUID().toString(), queries.find(owner).revision()));
	}

}
