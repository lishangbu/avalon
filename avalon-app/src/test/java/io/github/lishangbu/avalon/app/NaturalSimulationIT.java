package io.github.lishangbu.avalon.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 独立隔离数据库的正式HTTP长链；不启动浏览器、不写成长夹具。 */
@EnabledIfEnvironmentVariable(named = "AVALON_API_SIMULATION", matches = "1")
class NaturalSimulationIT extends SpringIntegrationTest {

	@Test
	void naturalJourneyUsesPublicHttpCommands() throws Exception {
		Path root = Path.of("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve("avalon-ui/package.json")))
			root = root.getParent();
		assertNotNull(root, "未找到模拟工程目录");
		Path log = root.resolve("avalon-app/target/natural-simulation.log");
		var builder = new ProcessBuilder("npm", "run", "test:simulation").directory(root.resolve("avalon-ui").toFile())
			.redirectErrorStream(true)
			.redirectOutput(log.toFile());
		builder.environment().put("AVALON_BASE_URL", "http://127.0.0.1:" + port);
		var simulation = builder.start();
		try {
			assertTrue(simulation.waitFor(650, TimeUnit.SECONDS), "接口模拟超时，见 " + log);
			assertEquals(0, simulation.exitValue(), "接口模拟失败，见 " + log);
		}
		finally {
			if (simulation.isAlive()) {
				simulation.descendants().forEach(ProcessHandle::destroy);
				simulation.destroyForcibly();
			}
		}
	}

}
