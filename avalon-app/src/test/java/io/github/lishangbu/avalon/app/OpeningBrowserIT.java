package io.github.lishangbu.avalon.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** 新故事真实浏览器检查：每个视口注册新账号，无进度与库存夹具。 */
@EnabledIfEnvironmentVariable(named = "AVALON_REDESIGN_BROWSER", matches = "1")
class OpeningBrowserIT extends SpringIntegrationTest {

	@Test
	void openingPreparationWorksOnDesktopAndMobile() throws Exception {
		Path root = Path.of("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve("avalon-ui/package.json")))
			root = root.getParent();
		assertNotNull(root);
		var command = new ArrayList<String>();
		if (System.getProperty("os.name").startsWith("Windows"))
			command.addAll(List.of("cmd.exe", "/d", "/c", "npm.cmd"));
		else
			command.add("npm");
		command.addAll(List.of("run", "test:e2e", "--", "story-opening.spec.ts", "--workers=2", "--reporter=list"));
		var log = root.resolve("quest-opening-browser.log");
		var builder = new ProcessBuilder(command).directory(root.resolve("avalon-ui").toFile())
			.redirectErrorStream(true)
			.redirectOutput(log.toFile());
		builder.environment().put("AVALON_BASE_URL", "http://127.0.0.1:" + port);
		var browser = builder.start();
		try {
			assertTrue(browser.waitFor(240, TimeUnit.SECONDS), "浏览器超时，见 " + log);
			assertEquals(0, browser.exitValue(), "浏览器失败，见 " + log);
		}
		finally {
			if (browser.isAlive()) {
				browser.descendants().forEach(ProcessHandle::destroy);
				browser.destroyForcibly();
			}
		}
	}

}
