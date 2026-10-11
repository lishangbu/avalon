package io.github.lishangbu.avalon.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 违禁文字的独立页面验收：手机、桌面访问本次隔离服务，注册和创角完全走页面。 幻兽夹具只准备伙伴及金币，拒绝、改正、费用确认与刷新均使用正式页面和API。
 * 显式启用后才运行浏览器，默认后端回归不承担浏览器启动成本。
 */
@EnabledIfEnvironmentVariable(named = "AVALON_FORBIDDEN_WORDS_BROWSER", matches = "1")
class ForbiddenWordsBrowserIT extends SpringIntegrationTest {

	/** 日志留在构建输出，便于定位交互失败；不在报告中输出账号令牌或密码。 */
	@Test
	void rejectedTextKeepsInputsAndAllowsCorrection() throws Exception {
		Path root = Path.of("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve("avalon-ui/package.json")))
			root = root.getParent();
		assertNotNull(root, "未找到前端工程目录");
		Path log = root.resolve("avalon-app/target/forbidden-words-browser.log");
		var builder = new ProcessBuilder("npm", "run", "test:e2e", "--", "forbidden-words.spec.ts",
				"creature-naming.spec.ts", "--workers=1")
			.directory(root.resolve("avalon-ui").toFile())
			.redirectErrorStream(true)
			.redirectOutput(log.toFile());
		builder.environment().put("AVALON_BASE_URL", "http://127.0.0.1:" + port);
		builder.environment().put("AVALON_LOCAL_FIXTURES", "1");
		builder.environment().put("AVALON_TEST_DB_CONTAINER", DATABASE.getContainerId());
		builder.environment().put("AVALON_TEST_DB_USER", DATABASE.getUsername());
		builder.environment().put("AVALON_TEST_DB_NAME", DATABASE.getDatabaseName());
		var browser = builder.start();
		try {
			assertTrue(browser.waitFor(240, TimeUnit.SECONDS), "违禁文字页面验收超时，见 " + log);
			assertEquals(0, browser.exitValue(), "违禁文字页面验收失败，见 " + log);
		}
		finally {
			// 超时或断言失败仍回收浏览器进程，防止占用下一次验收的资源。
			if (browser.isAlive()) {
				browser.descendants().forEach(ProcessHandle::destroy);
				browser.destroyForcibly();
			}
		}
	}

}
