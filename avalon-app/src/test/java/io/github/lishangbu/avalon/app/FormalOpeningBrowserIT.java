package io.github.lishangbu.avalon.app;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** 手机与桌面浏览器访问同源静态页面及隔离数据库，不注入任务进度或替代玩家操作。 */
@EnabledIfEnvironmentVariable(named = "AVALON_OPENING_BROWSER", matches = "1")
class FormalOpeningBrowserIT extends SpringIntegrationTest {

	@Test
	void formalOpeningWorksInBrowser() throws Exception {
		Path root = Path.of("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve("avalon-ui/package.json")))
			root = root.getParent();
		assertNotNull(root, "未找到前端工程目录");
		Path log = root.resolve("avalon-app/target/formal-opening-browser.log");
		// 两种视口各自注册新账号，进度和库存互不共享，可以并行验证完整旅程。
		var builder = new ProcessBuilder("npm", "run", "test:e2e", "--", "formal-opening.spec.ts", "--workers=2")
			.directory(root.resolve("avalon-ui").toFile())
			.redirectErrorStream(true)
			.redirectOutput(log.toFile());
		builder.environment().put("AVALON_BASE_URL", "http://127.0.0.1:" + port);
		var browser = builder.start();
		try {
			assertTrue(browser.waitFor(1900, TimeUnit.SECONDS), "浏览器操作超时，见 " + log);
			assertEquals(0, browser.exitValue(), "浏览器操作失败，见 " + log);
		}
		finally {
			if (browser.isAlive()) {
				browser.descendants().forEach(ProcessHandle::destroy);
				browser.destroyForcibly();
			}
		}
	}

}
