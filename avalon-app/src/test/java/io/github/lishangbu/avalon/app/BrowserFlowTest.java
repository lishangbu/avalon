package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实 PostgreSQL 集成测试：身份隔离、轮换撤销、相邻移动、幂等和事务均经过 HTTP/数据库。
 */
class BrowserFlowTest extends SpringIntegrationTest {

	@Autowired
	EntityManager entityManager;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accountQueries;

	private String unique() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

	private Map<String, String> credentials() {
		return Map.of("username", "u_" + unique(), "password", "Test-password-123", "device", "集成测试");
	}

	private Map<String, String> register(Map<String, String> body) {
		return jsonMapper.convertValue(jsonMapper.readTree(client.post()
			.uri("/api/auth/register")
			.contentType(MediaType.APPLICATION_JSON)
			.body(body)
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.accessToken")
			.value(String.class, actual -> assertThat(actual, not(emptyString())))
			.returnResult()
			.getResponseBody()), new TypeReference<Map<String, String>>() {
			});
	}

	private String bearer(Map<String, String> tokens) {
		return "Bearer " + tokens.get("accessToken");
	}

	/**
	 * 验证密码认证后的创角、移动和归属隔离。非法职业、旧revision和非相邻移动必须被拒绝；接口返回值只来自认证账号，不接受外部角色指定。
	 */
	@Test
	void loginCharacterMapAndOwnership() {
		client.get().uri("/api/world").exchange().expectStatus().isEqualTo(401);
		Map<String, String> credentials = credentials(), tokens = register(credentials);
		client.post()
			.uri("/api/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.body(credentials)
			.exchange()
			.expectStatus()
			.isEqualTo(200);
		client.post()
			.uri("/api/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("username", credentials.get("username"), "password", "incorrect-password"))
			.exchange()
			.expectStatus()
			.isEqualTo(401);
		client.get()
			.uri("/api/world")
			.header("Authorization", bearer(tokens))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.character")
			.isEqualTo(null);
		String name = "旅人" + unique();
		Map<String, String> request = Map.of("name", name, "profession", "WARRIOR");
		String id = jsonMapper
			.readTree(client.post()
				.uri("/api/world/character")
				.header("Authorization", bearer(tokens))
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.exchange()
				.expectStatus()
				.isEqualTo(200)
				.expectBody()
				.jsonPath("$.location")
				.isEqualTo("VILLAGE_HOME")
				.returnResult()
				.getResponseBody())
			.at("/id")
			.asString();
		assertTrue(id.matches("[0-9]+"));
		client.post()
			.uri("/api/world/character")
			.header("Authorization", bearer(tokens))
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.id")
			.isEqualTo(id);
		client.post()
			.uri("/api/world/character")
			.header("Authorization", bearer(tokens))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("name", name, "profession", "MAGE"))
			.exchange()
			.expectStatus()
			.isEqualTo(409);
		client.post()
			.uri("/api/world/move")
			.header("Authorization", bearer(tokens))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("destination", "SQUARE", "expectedRevision", 0))
			.exchange()
			.expectStatus()
			.isEqualTo(400);
		Map<String, Object> move = Map.of("destination", "STONE_PATH", "expectedRevision", 0);
		for (int i = 0; i < 2; i++)
			client.post()
				.uri("/api/world/move")
				.header("Authorization", bearer(tokens))
				.contentType(MediaType.APPLICATION_JSON)
				.body(move)
				.exchange()
				.expectStatus()
				.isEqualTo(200)
				.expectBody()
				.jsonPath("$.revision")
				.isEqualTo(1);
		client.post()
			.uri("/api/world/move")
			.header("Authorization", bearer(tokens))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("destination", "SQUARE", "expectedRevision", 0))
			.exchange()
			.expectStatus()
			.isEqualTo(409);
		Map<String, String> stranger = register(credentials());
		client.get()
			.uri("/api/world")
			.header("Authorization", bearer(stranger))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.character")
			.isEqualTo(null);
		client.post()
			.uri("/api/world/move")
			.header("Authorization", bearer(stranger))
			.contentType(MediaType.APPLICATION_JSON)
			.body(move)
			.exchange()
			.expectStatus()
			.isEqualTo(404);
	}

	/**
	 * 验证一次刷新使旧摘要失效，消费过的刷新令牌重放会撤销该设备。检查设备撤销归属及注销后的访问拒绝，不用客户端清缓存代替服务端安全验证。
	 */
	@Test
	void rotationReplayAndDeviceRevocation() {
		Map<String, String> account = credentials(), first = register(account);
		Map<String, String> rotated = jsonMapper.convertValue(client.post()
			.uri("/api/auth/refresh")
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("refreshToken", first.get("refreshToken")))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.returnResult(JsonNode.class)
			.getResponseBody(), new TypeReference<Map<String, String>>() {
			});
		client.get().uri("/api/world").header("Authorization", bearer(first)).exchange().expectStatus().isEqualTo(401);
		client.get()
			.uri("/api/world")
			.header("Authorization", bearer(rotated))
			.exchange()
			.expectStatus()
			.isEqualTo(200);
		client.post()
			.uri("/api/auth/refresh")
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("refreshToken", first.get("refreshToken")))
			.exchange()
			.expectStatus()
			.isEqualTo(401);
		client.get()
			.uri("/api/world")
			.header("Authorization", bearer(rotated))
			.exchange()
			.expectStatus()
			.isEqualTo(401);
		Map<String, String> device = jsonMapper.convertValue(client.post()
			.uri("/api/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.body(account)
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.returnResult(JsonNode.class)
			.getResponseBody(), new TypeReference<Map<String, String>>() {
			});
		String deviceId = client.get()
			.uri("/api/auth/sessions")
			.header("Authorization", bearer(device))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.returnResult(JsonNode.class)
			.getResponseBody()
			.at("//0/id")
			.asString();
		Map<String, String> other = register(credentials());
		client.delete()
			.uri("/api/auth/sessions/" + deviceId)
			.header("Authorization", bearer(other))
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		client.post()
			.uri("/api/auth/logout")
			.header("Authorization", bearer(device))
			.exchange()
			.expectStatus()
			.isEqualTo(204);
		client.get().uri("/api/world").header("Authorization", bearer(device)).exchange().expectStatus().isEqualTo(401);
		client.post()
			.uri("/api/auth/refresh")
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("refreshToken", device.get("refreshToken")))
			.exchange()
			.expectStatus()
			.isEqualTo(401);
	}

	/**
	 * 两个线程同时消费同一刷新令牌，只能有一次轮换成功，竞争失败按严格重放策略撤销会话。闩锁对齐提交时机，避免串行调用伪装并发测试。
	 */
	@Test
	void concurrentRefreshHasOnlyOneSuccessAndRevokesOnReplay() throws Exception {
		Map<String, String> first = register(credentials());
		CountDownLatch gate = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			Callable<EntityExchangeResult<JsonNode>> refresh = () -> {
				gate.await();
				return client.post()
					.uri("/api/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("refreshToken", first.get("refreshToken")))
					.exchange()
					.returnResult(JsonNode.class);
			};
			Future<EntityExchangeResult<JsonNode>> a = executor.submit(refresh), b = executor.submit(refresh);
			gate.countDown();
			EntityExchangeResult<JsonNode> firstResponse = a.get(15, TimeUnit.SECONDS),
					secondResponse = b.get(15, TimeUnit.SECONDS);
			List<Integer> codes = new ArrayList<>(
					List.of(firstResponse.getStatus().value(), secondResponse.getStatus().value()));
			Collections.sort(codes);
			assertEquals(List.of(200, 401), codes);
			EntityExchangeResult<JsonNode> success = firstResponse.getStatus().value() == 200 ? firstResponse
					: secondResponse;
			client.get()
				.uri("/api/world")
				.header("Authorization", "Bearer " + success.getResponseBody().at("/accessToken").asString())
				.exchange()
				.expectStatus()
				.isEqualTo(401);
		}
		client.get().uri("/api/world").header("Authorization", bearer(first)).exchange().expectStatus().isEqualTo(401);
	}

	/** 两个注册请求同时通过预检查时，唯一约束仍只允许一个账号和一次会话创建。 */
	/**
	 * 并发注册相同用户名，数据库唯一约束决定唯一胜者；另一个请求返回业务冲突，不能创建两份账号或把内部异常暴露给客户端。
	 */
	@Test
	void concurrentRegistrationReturnsConflictWithoutDuplicatingAccounts() throws Exception {
		Map<String, String> body = credentials();
		CountDownLatch gate = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			Callable<Integer> request = () -> {
				gate.await();
				return client.post()
					.uri("/api/auth/register")
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.exchange()
					.returnResult()
					.getStatus()
					.value();
			};
			Future<Integer> a = executor.submit(request), b = executor.submit(request);
			gate.countDown();
			List<Integer> codes = new ArrayList<>(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)));
			Collections.sort(codes);
			assertEquals(List.of(200, 409), codes);
		}
		transactions.run(() -> {
			Number count = (Number) entityManager
				.createNativeQuery("SELECT count(*) FROM device_session s" + " JOIN account a ON"
						+ " s.account_id=a.id WHERE" + " a.username=:username")
				.setParameter("username", body.get("username"))
				.getSingleResult();
			assertEquals(1, count.longValue());
		});
	}

	/**
	 * 使用真实数据库确认失败写事务不残留状态，且会话凭证只保存摘要。
	 */
	@Test
	void failedTransactionRollsBackAndSessionTokensAreHashed() {
		Map<String, String> account = credentials();
		register(account);
		long accountId = accountQueries.accountId(account.get("username"));
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			characters.create(accountId, "回滚" + unique(), "MAGE");
			throw new IllegalStateException("模拟后续步骤失败");
		}));
		assertNull(characterQueries.find(accountId));
		transactions.run(() -> {
			Number plaintext = (Number) entityManager
				.createNativeQuery("SELECT count(*) FROM device_session" + " WHERE length(access_hash)<>64"
						+ " OR length(refresh_hash)<>64")
				.getSingleResult();
			assertEquals(0, plaintext.longValue());
		});
	}

}
