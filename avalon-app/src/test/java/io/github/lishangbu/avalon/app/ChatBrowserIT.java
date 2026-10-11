package io.github.lishangbu.avalon.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 显式启用的聊天浏览器验收，访问当前Spring测试服务的真实静态产物、认证和SSE。
 * 只把本次Testcontainers数据库信息交给夹具，不连接local或生产数据库；失败保留Playwright trace。
 */
@EnabledIfEnvironmentVariable(named = "AVALON_CHAT_BROWSER", matches = "1")
class ChatBrowserIT extends SpringIntegrationTest {

	@Test
	void desktopAndMobileChatUseRealAuthenticatedService() throws Exception {
		Path root = Path.of("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve("avalon-ui/package.json")))
			root = root.getParent();
		assertNotNull(root, "未找到前端工程");
		Path log = root.resolve("avalon-app/target/chat-browser.log");
		var builder = new ProcessBuilder("npm", "run", "test:e2e", "--", "chat.spec.ts", "--workers=1")
			.directory(root.resolve("avalon-ui").toFile())
			.redirectErrorStream(true)
			.redirectOutput(log.toFile());
		builder.environment().put("AVALON_BASE_URL", "http://127.0.0.1:" + port);
		builder.environment().put("AVALON_LOCAL_FIXTURES", "1");
		builder.environment().put("AVALON_TEST_DB_CONTAINER", DATABASE.getContainerId());
		builder.environment().put("AVALON_TEST_DB_USER", DATABASE.getUsername());
		builder.environment().put("AVALON_TEST_DB_NAME", DATABASE.getDatabaseName());
		var process = builder.start();
		try {
			assertTrue(process.waitFor(240, TimeUnit.SECONDS), "聊天浏览器验收超时，见 " + log);
			assertEquals(0, process.exitValue(), "聊天浏览器验收失败，见 " + log);
		}
		finally {
			// Maven超时或断言失败仍回收浏览器，避免遗留进程继续请求已关闭的隔离服务。
			if (process.isAlive()) {
				process.descendants().forEach(ProcessHandle::destroy);
				process.destroyForcibly();
			}
		}
	}

}
