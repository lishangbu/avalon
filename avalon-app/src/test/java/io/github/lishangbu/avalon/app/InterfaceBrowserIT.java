package io.github.lishangbu.avalon.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 现有界面专项的隔离验收入口。测试只向本次容器写入边界夹具，实际交互经过浏览器； 从零自然成长和第二阶段培养交易由各自专用入口验证，避免把夹具测试误报为自然成长。
 */
@EnabledIfEnvironmentVariable(named = "AVALON_INTERFACE_BROWSER", matches = "1")
class InterfaceBrowserIT extends SpringIntegrationTest {

	@Test
	void existingPagesWorkAgainstIsolatedDatabase() throws Exception {
		Path root = Path.of("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve("avalon-ui/package.json")))
			root = root.getParent();
		assertNotNull(root, "未找到前端工程目录");
		Path log = root.resolve("avalon-app/target/interface-browser.log");
		var command = new ArrayList<>(List.of("npm", "run", "test:e2e", "--", "--workers=1"));
		var independent = Set.of("story-opening.spec.ts", "story-branches.spec.ts", "phase-two.spec.ts");
		try (var files = Files.list(root.resolve("avalon-ui/tests"))) {
			files.map(path -> path.getFileName().toString())
				.filter(name -> name.endsWith(".spec.ts") && !independent.contains(name))
				.sorted()
				.forEach(command::add);
		}
		assertTrue(command.size() > 5, "未发现页面专项用例");
		var builder = new ProcessBuilder(command).directory(root.resolve("avalon-ui").toFile())
			.redirectErrorStream(true)
			.redirectOutput(log.toFile());
		builder.environment().put("AVALON_BASE_URL", "http://127.0.0.1:" + port);
		builder.environment().put("AVALON_LOCAL_FIXTURES", "1");
		builder.environment().put("AVALON_LAYOUT_AUDIT", "1");
		builder.environment().put("AVALON_TEST_DB_CONTAINER", DATABASE.getContainerId());
		builder.environment().put("AVALON_TEST_DB_USER", DATABASE.getUsername());
		builder.environment().put("AVALON_TEST_DB_NAME", DATABASE.getDatabaseName());
		var browser = builder.start();
		try {
			assertTrue(browser.waitFor(2, TimeUnit.HOURS), "页面专项操作超时，见 " + log);
			assertEquals(0, browser.exitValue(), "页面专项操作失败，见 " + log);
		}
		finally {
			if (browser.isAlive()) {
				browser.descendants().forEach(ProcessHandle::destroy);
				browser.destroyForcibly();
			}
		}
	}

}
