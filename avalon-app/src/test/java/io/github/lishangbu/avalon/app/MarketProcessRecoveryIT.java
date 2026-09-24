package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.market.application.command.MarketAccess;
import io.github.lishangbu.avalon.market.application.command.MarketListingCommandService;
import io.github.lishangbu.avalon.market.application.command.MarketReturnCommandService;
import io.github.lishangbu.avalon.market.persistence.repository.MarketListingRepository;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 实际终止并重启打包进程，验证内存调度器重新扫描持久化截止时间，而非用重新new服务替代重启。 数据库使用本次 Spring 测试的隔离 PostgreSQL
 * 容器；子进程使用独立内存调度器和雪花机器号。
 */
@EnabledIfEnvironmentVariable(named = "AVALON_PROCESS_RECOVERY", matches = "1")
class MarketProcessRecoveryIT extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	MarketAccess access;

	@Autowired
	MarketListingCommandService market;

	@Autowired
	MarketListingRepository listings;

	@Autowired
	MarketReturnCommandService returns;

	@Autowired
	EntityManager em;

	@Test
	void stoppedProcessResumesExpiryAndDoesNotDuplicateReturnedAssets() throws Exception {
		Path root = Path.of("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve("avalon-ui/package.json")))
			root = root.getParent();
		assertNotNull(root);
		Path jar = root.resolve("avalon-app/target/avalon-app-0.1.0-SNAPSHOT.jar");
		assertTrue(Files.exists(jar), "先执行 Maven package");
		TestDoubles.install(new GrowthRules() {
			@Override
			public int level(long experience) {
				return 16;
			}

			@Override
			public int carryingCapacity(int level) {
				return 6;
			}
		}, GrowthRules.class);
		Process child = null;
		try {
			String name = "recover_" + UUID.randomUUID().toString().substring(0, 8);
			registrations.register(name, "Recovery-test-1234", "测试");
			long owner = accounts.accountId(name);
			characters.create(owner, name, "WARRIOR");
			transactions.run(() -> {
				long id = characters.lock(owner).id();
				inventory.grantStrict(owner, "RECOVERY", "WIND_FEATHER", 3, false, false);
				em.flush();
				em.clear();
				em.createNativeQuery("UPDATE player_character SET location='CITY_SHOP' WHERE id=:id")
					.setParameter("id", id)
					.executeUpdate();
			});
			access.teach(owner, true);
			var item = items.list(owner).getFirst();
			var listing = market.listItem(owner, new MarketListingCommandService.Request(UUID.randomUUID().toString(),
					Long.parseLong(item.id()), 3, 10, item));
			long listingId = Long.parseLong(listing.listingId());
			int port;
			try (var socket = new ServerSocket(0)) {
				port = socket.getLocalPort();
			}
			child = start(root, jar, port, "first");
			ready(child, port);
			assertEquals("ACTIVE", state(listingId));
			child.destroyForcibly();
			assertTrue(child.waitFor(10, TimeUnit.SECONDS));
			child = null;
			// 仅加速离线期间截止时刻，不调用到期命令、不改挂单状态、不交付资产。
			Instant expired = Instant.now().minusSeconds(60);
			transactions.run(() -> {
				var persisted = listings.get(listingId);
				persisted.setCreatedAt(expired.minus(Duration.ofHours(48)));
				persisted.setExpiresAt(expired);
			});
			assertEquals("ACTIVE", state(listingId));
			child = start(root, jar, port, "restart");
			ready(child, port);
			for (int i = 0; i < 100 && !"RETURN_PENDING".equals(state(listingId)); i++)
				Thread.sleep(200);
			assertEquals("RETURN_PENDING", state(listingId));
			assertTrue(items.list(owner).isEmpty(), "后台只转待领，不擅自入包");
			var request = new MarketReturnCommandService.Claim(UUID.randomUUID().toString(), listingId, 3);
			var result = returns.claim(owner, request);
			assertEquals(result, returns.claim(owner, request));
			assertEquals("RETURNED", state(listingId));
			assertEquals(3, items.list(owner).stream().mapToInt(i -> i.quantity()).sum());
			assertEquals(item.id(), items.list(owner).getFirst().id());
			transactions.run(() -> assertEquals(expired.toEpochMilli(),
					listings.get(listingId).getExpiresAt().toEpochMilli(), "重启不能延长出售期限"));
		}
		finally {
			if (child != null) {
				child.destroy();
				if (!child.waitFor(10, TimeUnit.SECONDS)) {
					child.destroyForcibly();
					child.waitFor(10, TimeUnit.SECONDS);
				}
			}
			TestDoubles.install(new GrowthRules(), GrowthRules.class);
		}
	}

	private String state(long id) {
		return transactions.call(() -> listings.get(id).getState());
	}

	private Process start(Path root, Path jar, int port, String phase) throws Exception {

		String url = DATABASE.getJdbcUrl();
		assertFalse(url.contains(":54329/"), "禁止连接用户原数据库");
		var builder = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-jar",
				jar.toString())
			.directory(root.toFile())
			.redirectErrorStream(true)
			.redirectOutput(root.resolve("avalon-app/target/market-recovery-" + phase + ".log").toFile());
		var env = builder.environment();
		env.put("SPRING_PROFILES_ACTIVE", "prod");
		env.put("SPRING_DATASOURCE_URL", url);
		env.put("SPRING_DATASOURCE_USERNAME", DATABASE.getUsername());
		env.put("SPRING_DATASOURCE_PASSWORD", DATABASE.getPassword());
		env.put("COSID_MACHINE_DISTRIBUTOR_MANUAL_MACHINEID", "2");
		env.put("SERVER_ADDRESS", "127.0.0.1");
		env.put("SERVER_PORT", Integer.toString(port));
		return builder.start();
	}

	private void ready(Process child, int port) throws Exception {
		try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
			var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/actuator/health"))
				.timeout(Duration.ofSeconds(1))
				.build();
			for (int i = 0; i < 100 && child.isAlive(); i++) {
				try {
					if (client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200)
						return;
				}
				catch (IOException ignored) {
					/* 进程尚未监听，继续等待同一个进程。 */ }
				Thread.sleep(200);
			}
		}
		fail("恢复测试应用未就绪，请检查market-recovery日志");
	}

}
