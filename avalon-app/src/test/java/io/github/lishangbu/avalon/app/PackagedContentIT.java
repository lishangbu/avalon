package io.github.lishangbu.avalon.app;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

/** 空库只由真实JAR迁移；认证与课程通过真实HTTP，前置事实仅写入本次隔离容器。 */
@EnabledIfEnvironmentVariable(named = "AVALON_PACKAGED_CONTENT", matches = "1")
class PackagedContentIT {

	private final ObjectMapper json = new ObjectMapper();

	private final HttpClient client = HttpClient.newHttpClient();

	private String base;

	private String token;

	@Test
	void emptyDatabaseBootsCurrentJarAndKeepsAcademySuppliesTemporary() throws Exception {
		try (var database = new PostgreSQLContainer("postgres:18.6-trixie")) {
			database.start();
			try (var app = new PackagedGameProcess(database.getJdbcUrl(), database.getUsername(),
					database.getPassword(), Path.of("target/packaged-content.log"))) {
				base = app.baseUrl;
				var home = client.send(HttpRequest.newBuilder(URI.create(base)).build(),
						HttpResponse.BodyHandlers.ofString());
				assertEquals(200, home.statusCode());
				var assets = Pattern.compile("(?:src|href)=\"(/assets/[^\"]+)\"").matcher(home.body());
				int count = 0;
				while (assets.find()) {
					assertEquals(200,
							client
								.send(HttpRequest.newBuilder(URI.create(base + assets.group(1))).build(),
										HttpResponse.BodyHandlers.discarding())
								.statusCode());
					count++;
				}
				assertTrue(count > 0);
				token = request("/api/auth/register",
						Map.of("username", "pkg_" + UUID.randomUUID().toString().substring(0, 8), "password",
								"Packaged-test-1234", "device", "验收"),
						200)
					.path("accessToken")
					.asString();
				long character = request("/api/world/character", Map.of("name", "打包验收", "profession", "MAGE"), 200)
					.path("id")
					.asLong();
				for (String code : List.of("LIFE_HEALTH_POTION_1", "FOOD_0_0", "TOOL_COPPER_HAMMER", "SEAL_I",
						"CRAFT_3_SWORD")) {
					assertFalse(request("/api/content/details/item/" + code, null, 200).path("description")
						.asString()
						.isBlank());
				}
				request("/api/content/details/item/HEALTH_POTION_II", null, 404);
				request("/api/content/details/item/MANA_POTION_II", null, 404);
				var before = request("/api/inventory", null, 200);
				try (var connection = DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(),
						database.getPassword());
						var update = connection
							.prepareStatement("UPDATE player_character SET location='ACADEMY' WHERE id=?");
						var fact = connection.prepareStatement(
								"INSERT INTO character_fact(id,character_id,code,created_at) VALUES(1,?,'QUEST:O012',CURRENT_TIMESTAMP)")) {
					update.setLong(1, character);
					update.executeUpdate();
					fact.setLong(1, character);
					fact.executeUpdate();
				}
				var battle = request("/api/battle/academy", Map.of("course", "ACADEMY_CAPTURE", "partners", List.of(),
						"requestId", UUID.randomUUID().toString()), 200);
				assertEquals(12, battle.path("supplies").path("HEALTH_POTION_II").asInt());
				assertEquals(4, battle.path("supplies").path("MANA_POTION_II").asInt());
				battle = request("/api/battle/" + battle.path("id").asString() + "/turn",
						Map.of("requestId", UUID.randomUUID().toString(), "revision", battle.path("revision").asInt(),
								"round", battle.path("round").asInt(), "orders",
								List.of(Map.of("actor", "trainee", "action", "HEALTH_POTION_II", "target", "trainee"),
										Map.of("actor", "T_CAT", "action", "DEFEND", "target", "T_CAT"),
										Map.of("actor", "T_STONE", "action", "DEFEND", "target", "T_STONE")),
								"surrender", false, "replacements", Map.of()),
						200);
				assertEquals(11, battle.path("supplies").path("HEALTH_POTION_II").asInt());
				assertEquals(before, request("/api/inventory", null, 200));
				var jar = Path.of("target/avalon-app-0.1.0-SNAPSHOT.jar");
				Files.writeString(Path.of("target/packaged-content-result.json"),
						json.writerWithDefaultPrettyPrinter()
							.writeValueAsString(Map.of("sha256", HexFormat.of()
								.formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))),
									"emptyDatabaseBoot", true, "staticAssets", count, "itemDetails", true,
									"academyConsumption", true, "permanentInventoryUnchanged", true)));
			}
		}
	}

	private JsonNode request(String path, Object body, int expected) throws Exception {
		var builder = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(15));
		if (token != null)
			builder.header("Authorization", "Bearer " + token);
		if (body != null)
			builder.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
		var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(expected, response.statusCode(),
				path + ": " + (path.contains("auth") ? "认证状态异常" : response.body()));
		return json.readTree(response.body());
	}

}
