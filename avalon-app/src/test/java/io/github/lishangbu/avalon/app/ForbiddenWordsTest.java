package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureNamingCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureNamingQueryService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 四个自由文字写入口经真实认证HTTP和隔离PostgreSQL验证，不能由页面按钮或纯匹配单测替代。
 * SQL只用于构造词表上线前已存在的旧存档，以及核对失败事务没有残留账号、会话或扣费。
 */
class ForbiddenWordsTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	CreatureCommandService pets;

	@Autowired
	CreatureQueryService petQueries;

	@Autowired
	CreatureNamingQueryService naming;

	@Autowired
	JdbcClient jdbc;

	/** 密码故意包含违禁词，成功注册也证明密码没有被送入文字检测。 */
	private static final String PASSWORD = "admin-Fuck-password-123";

	/** 每个用例独立新号，后缀只含十六进制字符，不随机撞中严格英文短词。 */
	private String unique() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
	}

	/** 正常账号准备只调用正式注册服务；无需直接插入密码摘要或令牌。 */
	private Player register() {
		String username = "text_" + unique();
		var tokens = registrations.register(username, PASSWORD, "文字测试");
		return new Player(username, accounts.accountId(username), tokens.accessToken(), tokens.refreshToken());
	}

	/** 原始客户端直接发HTTP，检验服务端约束而不是前端输入校验。 */
	private JsonNode post(String path, String token, Object body, int status) {
		var request = client.post().uri(path).contentType(MediaType.APPLICATION_JSON);
		if (token != null)
			request.headers(headers -> headers.setBearerAuth(token));
		return request.body(body)
			.exchange()
			.expectStatus()
			.isEqualTo(status)
			.returnResult(JsonNode.class)
			.getResponseBody();
	}

	/** 所有账号计数均按本用例用户名筛选，不依赖共用数据库其他测试的全表数量。 */
	private long accountCount(String username) {
		return jdbc.sql("select count(*) from account where username=:name")
			.param("name", username)
			.query(Long.class)
			.single();
	}

	private long sessionCount(long owner) {
		return jdbc.sql("select count(*) from device_session where account_id=:owner")
			.param("owner", owner)
			.query(Long.class)
			.single();
	}

	/** 准备版本不在角色展示视图中，单独核对拒绝没有触发战斗准备失效。 */
	private long preparationRevision(long owner) {
		return jdbc.sql("select preparation_revision from player_character where account_id=:owner")
			.param("owner", owner)
			.query(Long.class)
			.single();
	}

	/** 名称包含匹配和下划线逃避都必须拒绝；拒绝响应不带令牌，也不写账号。 */
	@Test
	void registrationRejectsNewForbiddenUsernamesAndRollsBackForbiddenDevices() {
		for (String prefix : new String[] { "aDmIn_", "f_u_c_k_", "wx_" }) {
			String username = prefix + unique();
			var error = post("/api/auth/register", null,
					Map.of("username", username, "password", PASSWORD, "device", "正常设备"), 400);
			assertEquals("账号包含不允许使用的内容，请修改后重试", error.path("message").asString());
			assertFalse(error.has("accessToken"));
			assertEquals(0, accountCount(username));
		}
		for (String label : new String[] { "官 方_客 服", "ｆ\u200bu_c-k", "桌面".repeat(45) + "微信" }) {
			String username = "text_" + unique();
			post("/api/auth/register", null, Map.of("username", username, "password", PASSWORD, "device", label), 400);
			assertEquals(0, accountCount(username), "首次会话被拒绝时必须回滚已写入的账号");
			// 同名重试成功证明失败没有占用唯一用户名，也没有留下无法登录的半成品账号。
			post("/api/auth/register", null, Map.of("username", username, "password", PASSWORD, "device", "正常设备"), 200);
			assertEquals(1, sessionCount(accounts.accountId(username)));
		}
	}

	/** 登录新设备标签也检查；旧有效会话不会因另一次登录被拒绝而撤销。 */
	@Test
	void rejectedLoginDeviceDoesNotCreateOrRevokeSessions() {
		var player = register();
		long before = sessionCount(player.owner());
		var error = post("/api/auth/login", null,
				Map.of("username", player.username(), "password", PASSWORD, "device", "WX_广告"), 400);
		assertEquals("设备名称包含不允许使用的内容，请修改后重试", error.path("message").asString());
		assertFalse(error.has("refreshToken"));
		assertEquals(before, sessionCount(player.owner()));
		client.get()
			.uri("/api/world")
			.headers(headers -> headers.setBearerAuth(player.token()))
			.exchange()
			.expectStatus()
			.isEqualTo(200);
	}

	/** 默认标签保持原规则，正常全角原文持久化不被检测副本替换。 */
	@Test
	void healthyInputIsStoredVerbatimAndEmptyDeviceStillUsesDefault() {
		String username = "text_" + unique();
		var tokens = post("/api/auth/register", null, Map.of("username", username, "password", PASSWORD), 200);
		long owner = accounts.accountId(username);
		String label = "桌面　ＡＢＣ_记录";
		post("/api/auth/login", null, Map.of("username", username, "password", PASSWORD, "device", label), 200);
		post("/api/auth/login", null, Map.of("username", username, "password", PASSWORD, "device", " \t"), 200);
		var labels = jdbc.sql("select label from device_session where account_id=:owner")
			.param("owner", owner)
			.query(String.class)
			.list();
		assertEquals(2, labels.stream().filter("浏览器"::equals).count());
		assertTrue(labels.contains(label), "正常输入不能因检测规范化变成半角或丢失空格");
		String name = "旅人Ａ_" + unique();
		post("/api/world/character", tokens.path("accessToken").asString(),
				Map.of("name", name, "profession", "WARRIOR"), 200);
		assertEquals(name, characterQueries.find(owner).name());
	}

	/** 角色名格式合法也可能命中，拒绝后仍可在同一账号创建正常角色。 */
	@Test
	void characterCreationBlocksForbiddenNamesBeforePersisting() {
		var player = register();
		for (String name : new String[] { "官方旅人", "官_方_客服", "ＡＤＭＩＮ", "微信小号" }) {
			post("/api/world/character", player.token(), Map.of("name", name, "profession", "WARRIOR"), 400);
			assertNull(characterQueries.find(player.owner()));
		}
		String name = "旅人" + unique();
		post("/api/world/character", player.token(), Map.of("name", name, "profession", "WARRIOR"), 200);
		assertEquals(name, characterQueries.find(player.owner()).name());
	}

	/**
	 * 免费和付费两种阶段都检验金币、名字、改名次数、准备版本及回执不变； 修正原失败请求的名称后复用请求号仍可成功，证明拒绝没有消耗请求号。
	 */
	@Test
	void forbiddenPetNamesPreserveFreeEligibilityGoldRevisionAndReceipts() {
		var player = register();
		characters.create(player.owner(), "旅人" + unique(), "WARRIOR");
		transactions.run(() -> {
			pets.grant(player.owner(), "FORBIDDEN_TEXT", "SPROUT");
			characters.changeGold(player.owner(), 30);
		});
		long pet = Long.parseLong(petQueries.list(player.owner()).getFirst().id());
		for (int stage = 0; stage < 2; stage++) {
			var beforeCharacter = characterQueries.find(player.owner());
			long beforeRevision = preparationRevision(player.owner());
			var quote = naming.preview(player.owner(), pet);
			String requestId = UUID.randomUUID().toString();
			var request = new CreatureNamingCommandService.Request(requestId, "官ＦＵＣＫ方", quote.name(), quote.renamed(),
					quote.price());
			var error = post("/api/creatures/" + pet + "/name", player.token(), request, 400);
			assertEquals("名称包含不允许使用的内容，请修改后重试", error.path("message").asString());
			assertEquals(beforeCharacter, characterQueries.find(player.owner()));
			assertEquals(beforeRevision, preparationRevision(player.owner()));
			assertEquals(quote, naming.preview(player.owner(), pet));
			assertEquals(0,
					jdbc.sql("select count(*) from creature_rename_receipt where request_id=:request")
						.param("request", requestId)
						.query(Long.class)
						.single());
			String corrected = stage == 0 ? "青芽" : "青叶";
			var accepted = new CreatureNamingCommandService.Request(requestId, corrected, quote.name(), quote.renamed(),
					quote.price());
			post("/api/creatures/" + pet + "/name", player.token(), accepted, 204);
			post("/api/creatures/" + pet + "/name", player.token(), accepted, 204);
			assertEquals(quote.renamed() + 1, naming.preview(player.owner(), pet).renamed());
			assertEquals(beforeCharacter.gold() - quote.price(), characterQueries.find(player.owner()).gold());
			assertEquals(corrected, petQueries.list(player.owner()).getFirst().name());
		}
	}

	/**
	 * 仅夹具模拟词表上线前的旧违规账号、角色和标签；登录、读取、刷新及创角重放仍可用。 这与允许新提交违规文字不同，不为历史读取新增扫描、处罚或自动改名。
	 */
	@Test
	void legacyForbiddenAccountCharacterAndDeviceStillAllowLoginReadAndRefresh() {
		var player = register();
		characters.create(player.owner(), "旅人" + unique(), "WARRIOR");
		String legacyUsername = "old_admin_" + unique();
		String legacyName = "官方" + unique();
		transactions.run(() -> {
			jdbc.sql("update account set username=:name where id=:owner")
				.param("name", legacyUsername)
				.param("owner", player.owner())
				.update();
			jdbc.sql("update player_character set name=:name where account_id=:owner")
				.param("name", legacyName)
				.param("owner", player.owner())
				.update();
			jdbc.sql("update device_session set label='官方客服' where account_id=:owner")
				.param("owner", player.owner())
				.update();
		});
		var tokens = post("/api/auth/login", null,
				Map.of("username", legacyUsername, "password", PASSWORD, "device", "正常设备"), 200);
		String token = tokens.path("accessToken").asString();
		post("/api/world/character", token, Map.of("name", legacyName, "profession", "WARRIOR"), 200);
		assertEquals(legacyName, characterQueries.find(player.owner()).name());
		client.get()
			.uri("/api/auth/sessions")
			.headers(headers -> headers.setBearerAuth(token))
			.exchange()
			.expectStatus()
			.isEqualTo(200);
		post("/api/auth/refresh", null, Map.of("refreshToken", player.refresh()), 200);
		assertEquals(1,
				jdbc.sql("select count(*) from device_session where account_id=:owner and label='官方客服'")
					.param("owner", player.owner())
					.query(Long.class)
					.single());
	}

	/** 旧违规宠名与已成功回执均可读取和重放，随后通过expectedName确认后改正。 */
	@Test
	void legacyPetNameCanBeChangedToHealthyName() {
		var player = register();
		characters.create(player.owner(), "旅人" + unique(), "WARRIOR");
		transactions.run(() -> {
			pets.grant(player.owner(), "LEGACY_TEXT", "SPROUT");
			characters.changeGold(player.owner(), 30);
		});
		long pet = Long.parseLong(petQueries.list(player.owner()).getFirst().id());
		var initial = naming.preview(player.owner(), pet);
		String priorRequestId = UUID.randomUUID().toString();
		post("/api/creatures/" + pet + "/name", player.token(), new CreatureNamingCommandService.Request(priorRequestId,
				"青芽", initial.name(), initial.renamed(), initial.price()), 204);
		// 仅夹具把名字和已提交回执一起改成上线前状态；正式请求仍不允许新写违规名字。
		transactions.run(() -> {
			jdbc.sql("update owned_creature set name='官方客服' where id=:pet").param("pet", pet).update();
			jdbc.sql(
					"update creature_rename_receipt set payload=replace(payload, '青芽', '官方客服') where request_id=:request")
				.param("request", priorRequestId)
				.update();
		});
		long revision = preparationRevision(player.owner());
		long balance = characterQueries.find(player.owner()).gold();
		post("/api/creatures/" + pet + "/name", player.token(), new CreatureNamingCommandService.Request(priorRequestId,
				"官方客服", initial.name(), initial.renamed(), initial.price()), 204);
		assertEquals(revision, preparationRevision(player.owner()), "已提交的旧回执重放不能再次执行改名");
		assertEquals(balance, characterQueries.find(player.owner()).gold());
		var quote = naming.preview(player.owner(), pet);
		assertEquals("官方客服", quote.name());
		assertEquals(1, quote.renamed());
		post("/api/creatures/" + pet + "/name", player.token(), new CreatureNamingCommandService.Request(
				UUID.randomUUID().toString(), "青芽", quote.name(), quote.renamed(), quote.price()), 204);
		assertEquals("青芽", petQueries.list(player.owner()).getFirst().name());
		assertEquals(balance - quote.price(), characterQueries.find(player.owner()).gold());
	}

	/** 测试身份与令牌不作为日志输出，账号归属用于按行核对事务结果。 */
	private record Player(String username, long owner, String token, String refresh) {
	}

}
