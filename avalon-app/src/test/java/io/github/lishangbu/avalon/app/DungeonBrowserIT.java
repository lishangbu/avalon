package io.github.lishangbu.avalon.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 隔离数据库运行三账号真实浏览器流程；等级夹具不充当自然成长证据。 */
@EnabledIfEnvironmentVariable(named = "AVALON_DUNGEON_BROWSER", matches = "1")
class DungeonBrowserIT extends SpringIntegrationTest {

	@Test
	void multiplayerCompletionAndRecovery() throws Exception {
		Path root = Path.of("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve("avalon-ui/package.json")))
			root = root.getParent();
		assertNotNull(root);
		var builder = new ProcessBuilder("npm", "run", "test:e2e", "--", "dungeon-flow.spec.ts",
				"dungeon-replacement.spec.ts", "--workers=1")
			.directory(root.resolve("avalon-ui").toFile())
			.redirectErrorStream(true)
			.redirectOutput(root.resolve("avalon-app/target/dungeon-browser.log").toFile());
		if ("1".equals(System.getenv("AVALON_DUNGEON_BENCHMARK_ONLY")))
			builder.command().addAll(List.of("--grep", "四场通关"));
		builder.environment().put("AVALON_BASE_URL", "http://127.0.0.1:" + port);
		builder.environment().put("AVALON_LOCAL_FIXTURES", "1");
		builder.environment().put("AVALON_TEST_DB_CONTAINER", DATABASE.getContainerId());
		builder.environment().put("AVALON_TEST_DB_USER", DATABASE.getUsername());
		builder.environment().put("AVALON_TEST_DB_NAME", DATABASE.getDatabaseName());
		var browser = builder.start();
		try {
			assertTrue(browser.waitFor(25, TimeUnit.MINUTES));
			assertEquals(0, browser.exitValue(), "见 target/dungeon-browser.log");
		}
		finally {
			if (browser.isAlive()) {
				browser.descendants().forEach(ProcessHandle::destroy);
				browser.destroyForcibly();
			}
		}
	}

}
