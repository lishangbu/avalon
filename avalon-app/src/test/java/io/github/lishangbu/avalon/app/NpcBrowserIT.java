package io.github.lishangbu.avalon.app;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** 新账号从页面走人物接待、任务标记与详情，不预置任务或替代浏览器命令。 */
@EnabledIfEnvironmentVariable(named = "AVALON_NPC_BROWSER", matches = "1")
class NpcBrowserIT extends SpringIntegrationTest {

	@Test
	void npcAndDetailsWorkInBothViewports() throws Exception {
		Path root = Path.of("").toAbsolutePath().getParent();
		Path log = root.resolve("avalon-app/target/npc-browser.log");
		var builder = new ProcessBuilder("npm", "run", "test:e2e", "--", "npc-details.spec.ts", "--workers=2")
			.directory(root.resolve("avalon-ui").toFile())
			.redirectErrorStream(true)
			.redirectOutput(log.toFile());
		builder.environment().put("AVALON_BASE_URL", "http://127.0.0.1:" + port);
		var process = builder.start();
		try {
			assertTrue(process.waitFor(360, TimeUnit.SECONDS), "人物界面浏览器超时，见" + log);
			assertEquals(0, process.exitValue(), "人物界面浏览器失败，见" + log);
		}
		finally {
			if (process.isAlive()) {
				process.descendants().forEach(ProcessHandle::destroy);
				process.destroyForcibly();
			}
		}
	}

}
