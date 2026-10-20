package io.github.lishangbu.avalon.app;

import java.net.ServerSocket;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** 验收真实JAR；数据库地址必须由隔离测试容器提供。 */
final class PackagedGameProcess implements AutoCloseable {

	private final Process process;

	final String baseUrl;

	PackagedGameProcess(String jdbc, String username, String password, Path log) throws Exception {
		int port;
		try (var socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		}
		baseUrl = "http://127.0.0.1:" + port;
		var builder = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-jar",
				Path.of("target/avalon-app-0.1.0-SNAPSHOT.jar").toAbsolutePath().toString(),
				"--server.address=127.0.0.1", "--server.port=" + port, "--spring.liquibase.enabled=true",
				"--avalon.battle.logs.directory="
						+ log.toAbsolutePath().getParent().resolve("packaged-battles-" + port),
				"--cosid.machine.distributor.manual.machine-id=996")
			.redirectErrorStream(true)
			.redirectOutput(log.toFile());
		builder.environment().put("SPRING_DATASOURCE_URL", jdbc);
		builder.environment().put("SPRING_DATASOURCE_USERNAME", username);
		builder.environment().put("SPRING_DATASOURCE_PASSWORD", password);
		builder.environment().put("SPRING_PROFILES_ACTIVE", "prod");
		process = builder.start();
		try {
			var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
			for (int attempt = 0; attempt < 120 && process.isAlive(); attempt++) {
				try {
					var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/actuator/health"))
						.timeout(Duration.ofSeconds(2))
						.build(), HttpResponse.BodyHandlers.ofString());
					if (response.statusCode() == 200 && response.body().contains("UP"))
						return;
				}
				catch (IOException ignored) {
				}
				Thread.sleep(500);
			}
			throw new IllegalStateException("打包进程未就绪，见 " + log);
		}
		catch (Exception failure) {
			close();
			throw failure;
		}
	}

	@Override
	public void close() throws InterruptedException {
		process.destroy();
		if (!process.waitFor(10, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			process.waitFor(10, TimeUnit.SECONDS);
		}
	}

}
