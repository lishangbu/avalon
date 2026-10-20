package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.FriendshipCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.chat.application.command.ChatCommandService;
import io.github.lishangbu.avalon.chat.application.model.ChatRead;
import io.github.lishangbu.avalon.chat.application.model.ChatSend;
import io.github.lishangbu.avalon.chat.application.query.ChatQueryService;
import io.github.lishangbu.avalon.chat.domain.ChatRules;
import io.github.lishangbu.avalon.chat.persistence.repository.ChatStore;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.JsonNode;

/**
 * 真实PostgreSQL与认证HTTP共同验证聊天：不使用内存假消息替代持久化、权限、限频或回滚。
 * SQL只用于准备过期历史/时间窗口，以及核对数据库结果；发送、阅读、设置走正式应用入口。
 * 每个用例独立角色，世界频道断言按消息ID过滤，避免把共享测试数据库的其他公开消息当成故障。
 */
class ChatIntegrationTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	FriendshipCommandService friends;

	@Autowired
	ChatCommandService chat;

	@Autowired
	ChatQueryService queries;

	@Autowired
	ChatStore store;

	@Autowired
	JdbcClient jdbc;

	@Autowired
	GrowthRules growth;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	QuestAcceptCommandService acceptance;

	@Autowired
	QuestCommandService quests;

	/** 注册、创角使用真实业务服务；编号与Token只在测试内使用，不打印到日志。 */
	private Player player() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		String name = "聊友0" + suffix;
		var tokens = registrations.register("chat_0" + suffix, "Chat-test-1234", "聊天测试");
		long owner = accounts.accountId("chat_0" + suffix);
		var c = characters.create(owner, name, "WARRIOR");
		return new Player(owner, Long.parseLong(c.id()), name, tokens.accessToken());
	}

	private ChatSend privateMessage(Player peer, String text) {
		return new ChatSend(UUID.randomUUID().toString(), "PRIVATE", peer.name(), text);
	}

	private JsonNode post(Player player, String path, Object body, int status) {
		return jsonMapper.readTree(client.post()
			.uri(path)
			.headers(h -> h.setBearerAuth(player.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(body)
			.exchange()
			.expectStatus()
			.isEqualTo(status)
			.expectBody()
			.returnResult()
			.getResponseBody());
	}

	/** 私聊允许陌生与离线角色，第三人只能查本人参与的消息；读取不会隐式已读。 */
	@Test
	void privateHistoryIsPersistentAuthorizedAndExplicitlyRead() {
		var a = player();
		var b = player();
		var c = player();
		client.get().uri("/api/chat").exchange().expectStatus().isEqualTo(401);
		client.get().uri("/api/chat/events").exchange().expectStatus().isEqualTo(401);
		var request = privateMessage(b, "你好，离线留言");
		var sent = post(a, "/api/chat/messages", request, 200);
		String id = sent.path("id").asString();
		assertEquals(id, post(a, "/api/chat/messages", request, 200).path("id").asString());
		post(a, "/api/chat/messages", new ChatSend(request.requestId(), "PRIVATE", b.name(), "修改正文"), 409);
		assertEquals(1, queries.history(b.owner(), "PRIVATE", a.name(), 0).entries().size());
		assertTrue(queries.history(c.owner(), "PRIVATE", a.name(), 0).entries().isEmpty());
		assertEquals(1, queries.summary(b.owner()).conversations().getFirst().unread());
		assertFalse(queries.history(a.owner(), "PRIVATE", b.name(), 0).entries().getFirst().read());
		client.post()
			.uri("/api/chat/read")
			.headers(h -> h.setBearerAuth(c.token()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(new ChatRead("PRIVATE", a.name(), id))
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		chat.read(b.owner(), new ChatRead("PRIVATE", a.name(), id));
		chat.read(b.owner(), new ChatRead("PRIVATE", a.name(), id));
		assertEquals(0, queries.summary(b.owner()).conversations().getFirst().unread());
		assertTrue(queries.history(a.owner(), "PRIVATE", b.name(), 0).entries().getFirst().read());
		assertEquals(1,
				jdbc.sql("SELECT count(*) FROM chat_seen WHERE character_id=:self AND message_id=:id")
					.param("self", b.character())
					.param("id", Long.parseLong(id))
					.query(Long.class)
					.single());
	}

	/** 不能借SYSTEM、客户端伪造sender、超长文字或请求号绕过服务端规则；被拒绝请求不消耗发送间隔。 */
	@Test
	void textValidationSystemReadOnlyAndSenderIdentityAreServerControlled() {
		var a = player();
		var b = player();
		post(a, "/api/chat/messages", new ChatSend(UUID.randomUUID().toString(), "SYSTEM", null, "公告"), 403);
		post(a, "/api/chat/messages", new ChatSend("bad", "WORLD", null, "正文"), 400);
		post(a, "/api/chat/messages", privateMessage(b, " "), 400);
		post(a, "/api/chat/messages", privateMessage(b, "字".repeat(501)), 400);
		post(a, "/api/chat/messages", privateMessage(b, "官_方_客服"), 400);
		post(a, "/api/chat/messages", privateMessage(a, "自己"), 400);
		var result = post(a, "/api/chat/messages", Map.of("requestId", UUID.randomUUID().toString(), "channel", "WORLD",
				"text", "<script>普通文字</script>", "sender", Long.toString(b.character())), 200);
		assertEquals(Long.toString(a.character()), result.path("sender").asString());
		assertEquals("<script>普通文字</script>", result.path("text").asString());
		// 补充平面字符按码点计数；500个emoji占1000个UTF-16单元但仍符合500字规则。
		assertEquals(500, ChatRules.text("😀".repeat(500)).codePointCount(0, 1000));
	}

	/** 拒收陌生人要求已接受的好友关系，申请未接受仍被拒；双向拉黑禁止私聊，本人公共历史隐藏被拉黑发言。 */
	@Test
	void rejectionFriendshipAndBlocksControlNewMessages() {
		var a = player();
		var b = player();
		chat.settings(b.owner(), true);
		post(a, "/api/chat/messages", privateMessage(b, "陌生人"), 403);
		long friendship = friends.request(a.owner(), b.name());
		post(a, "/api/chat/messages", privateMessage(b, "申请中"), 403);
		friends.accept(b.owner(), friendship);
		var sent = chat.send(a.owner(), privateMessage(b, "好友消息"));
		var world = chat.send(b.owner(), new ChatSend(UUID.randomUUID().toString(), "WORLD", null, "公共发言"));
		chat.block(a.owner(), b.name());
		assertFalse(queries.history(a.owner(), "WORLD", null, 0)
			.entries()
			.stream()
			.anyMatch(m -> m.id().equals(world.id())));
		post(b, "/api/chat/messages", privateMessage(a, "被拉黑"), 403);
		post(a, "/api/chat/messages", privateMessage(b, "已拉黑"), 403);
		assertEquals(b.name(), queries.summary(a.owner()).blocked().getFirst().name());
		chat.unblock(a.owner(), b.character());
		assertTrue(queries.history(a.owner(), "WORLD", null, 0)
			.entries()
			.stream()
			.anyMatch(m -> m.id().equals(world.id())));
		assertEquals(sent.id(), queries.history(b.owner(), "PRIVATE", a.name(), 0).entries().getFirst().id());
	}

	/** 限频跨频道且持久化；已成功同请求号重放不占额度，并发相同请求仍只插入一条。 */
	@Test
	void rateLimitsAndConcurrentRetriesShareOnePersistentSenderState() throws Exception {
		var a = player();
		var b = player();
		var request = privateMessage(b, "并发重试");
		var gate = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var one = executor.submit(() -> {
				gate.await();
				return chat.send(a.owner(), request);
			});
			var two = executor.submit(() -> {
				gate.await();
				return chat.send(a.owner(), request);
			});
			gate.countDown();
			assertEquals(one.get(15, TimeUnit.SECONDS).id(), two.get(15, TimeUnit.SECONDS).id());
		}
		post(a, "/api/chat/messages", new ChatSend(UUID.randomUUID().toString(), "WORLD", null, "立刻切频道"), 429);
		assertEquals(1,
				jdbc.sql("SELECT count(*) FROM chat_message WHERE sender=:self")
					.param("self", a.character())
					.query(Long.class)
					.single());
		// 仅夹具构造一分钟内20条合法既有消息，独立验证分钟限额，避免用长时间sleep拖慢测试。
		transactions.run(() -> {
			for (int index = 0; index < 19; index++)
				store.insert("WORLD", a.character(), a.name(), null, null, "历史" + index, UUID.randomUUID().toString());
			jdbc.sql(
					"UPDATE chat_settings SET last_sent_at=clock_timestamp()-interval '3 seconds' WHERE character_id=:self")
				.param("self", a.character())
				.update();
		});
		post(a, "/api/chat/messages", privateMessage(b, "分钟限额"), 429);
		assertEquals(20,
				jdbc.sql("SELECT count(*) FROM chat_message WHERE sender=:self")
					.param("self", a.character())
					.query(Long.class)
					.single());
	}

	/** 战斗中只能接收无正文SSE，正文、摘要、发送、已读都被拒；战后完整读取且仍未读。 */
	@Test
	void battleBlocksViewingSendingAndReadButRetainsIncomingMessages() throws Exception {
		var a = player();
		var b = player();
		transactions.run(() -> characters.setBattle(b.owner(), 123L, false));
		try (var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
			var response = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/chat/events"))
				.header("Authorization", "Bearer " + b.token())
				.GET()
				.build(), HttpResponse.BodyHandlers.ofInputStream());
			assertEquals(200, response.statusCode());
			try (var reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
				String ready = reader.readLine();
				assertEquals("event:ready", ready);
				assertEquals("data:ready", reader.readLine());
				assertEquals("", reader.readLine());
				var incoming = chat.send(a.owner(), privateMessage(b, "战斗中保留的私聊正文"));
				// 固定常量流提示没有私聊全文、角色名或消息编号；禁止用实时通道查看正文。
				assertEquals("event:changed", reader.readLine());
				assertEquals("data:changed", reader.readLine());
				client.get()
					.uri("/api/chat")
					.headers(h -> h.setBearerAuth(b.token()))
					.exchange()
					.expectStatus()
					.isEqualTo(409);
				client.get()
					.uri("/api/chat/messages?channel=PRIVATE&peer=" + a.name())
					.headers(h -> h.setBearerAuth(b.token()))
					.exchange()
					.expectStatus()
					.isEqualTo(409);
				post(b, "/api/chat/messages", privateMessage(a, "战斗中发送"), 409);
				client.post()
					.uri("/api/chat/read")
					.headers(h -> h.setBearerAuth(b.token()))
					.contentType(MediaType.APPLICATION_JSON)
					.body(new ChatRead("PRIVATE", a.name(), incoming.id()))
					.exchange()
					.expectStatus()
					.isEqualTo(409);
				assertFalse(queries.history(a.owner(), "PRIVATE", b.name(), 0).entries().getFirst().read());
				transactions.run(() -> characters.setBattle(b.owner(), null, false));
				assertEquals(incoming.id(),
						queries.history(b.owner(), "PRIVATE", a.name(), 0).entries().getFirst().id());
				assertEquals(1, queries.summary(b.owner()).conversations().getFirst().unread());
			}
		}
	}

	/** 业务通知与奖励共同回滚；公开公告只由真实升级里程碑事件生成，普通经验金币不全服广播。 */
	@Test
	void rewardsAndAutomaticAnnouncementsAreAtomicAndScoped() {
		var a = player();
		var b = player();
		long original = personalMessages(a.character());
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			characters.changeGold(a.owner(), 10);
			characters.grantExperience(a.owner(), 5);
			throw new IllegalStateException("奖励后置故障");
		}));
		assertEquals(original, personalMessages(a.character()));
		assertEquals(0, characterQueries.find(a.owner()).gold());
		transactions.run(() -> {
			characters.changeGold(a.owner(), 10);
			characters.grantExperience(a.owner(), 5);
			chat.notify(a.owner(), "TEST_PERSONAL:" + a.character(), "个人任务通知");
			chat.notify(a.owner(), "TEST_PERSONAL:" + a.character(), "个人任务通知");
		});
		assertEquals(original + 3, personalMessages(a.character()));
		assertFalse(queries.history(b.owner(), "SYSTEM", null, 0)
			.entries()
			.stream()
			.anyMatch(m -> "个人任务通知".equals(m.text())));
		int amount = Math.toIntExact(growth.threshold(10) - characterQueries.find(a.owner()).experience());
		transactions.run(() -> characters.grantExperience(a.owner(), amount));
		var announcement = queries.history(b.owner(), "SYSTEM", null, 0)
			.entries()
			.stream()
			.filter(m -> m.text().contains(a.name()) && m.recipient() == null)
			.findFirst()
			.orElseThrow();
		assertTrue(announcement.text().contains("10级"));
		assertNull(announcement.sender());
		chat.read(b.owner(), new ChatRead("SYSTEM", null, announcement.id()));
		assertTrue(queries.history(b.owner(), "SYSTEM", null, 0)
			.entries()
			.stream()
			.filter(m -> m.id().equals(announcement.id()))
			.findFirst()
			.orElseThrow()
			.read());
	}

	/** 私聊游标不重复不遗漏；公共消息即时过期并分批物理清理，个人消息即使更早也永久保留。 */
	@Test
	void historyPaginationAndRetentionRespectChannelPolicy() {
		var a = player();
		var b = player();
		var world = chat.send(a.owner(), new ChatSend(UUID.randomUUID().toString(), "WORLD", null, "过期世界消息"));
		transactions.run(() -> {
			for (int index = 0; index < 55; index++)
				store.insert("PRIVATE", a.character(), a.name(), b.character(), b.name(), "分页" + index,
						UUID.randomUUID().toString());
		});
		var page = queries.history(b.owner(), "PRIVATE", a.name(), 0);
		assertEquals(50, page.entries().size());
		assertNotNull(page.next());
		var older = queries.history(b.owner(), "PRIVATE", a.name(), Long.parseLong(page.next()));
		assertEquals(5, older.entries().size());
		assertNull(older.next());
		assertTrue(older.entries().stream().noneMatch(page.entries()::contains));
		transactions.run(() -> {
			chat.notify(null, "TEST_PUBLIC:" + a.character(), "过期公告");
			chat.notify(b.owner(), "TEST_KEEP:" + a.character(), "永久个人通知");
			jdbc.sql(
					"UPDATE chat_message SET created_at=clock_timestamp()-interval '8 days' WHERE sender=:self OR request_id IN (:public,:personal)")
				.param("self", a.character())
				.param("public", "TEST_PUBLIC:" + a.character())
				.param("personal", "TEST_KEEP:" + a.character())
				.update();
		});
		assertFalse(queries.history(b.owner(), "WORLD", null, 0)
			.entries()
			.stream()
			.anyMatch(m -> m.id().equals(world.id())));
		chat.clean();
		assertEquals(0,
				jdbc.sql("SELECT count(*) FROM chat_message WHERE id=:id")
					.param("id", Long.parseLong(world.id()))
					.query(Long.class)
					.single());
		assertEquals(50, queries.history(b.owner(), "PRIVATE", a.name(), 0).entries().size());
		assertTrue(queries.history(b.owner(), "SYSTEM", null, 0)
			.entries()
			.stream()
			.anyMatch(m -> m.text().equals("永久个人通知")));
	}

	/** 查询不会写入默认设置；回滚发送不会留下正文、频率状态或未读。 */
	@Test
	void readOnlyQueriesAndRolledBackSendLeaveNoWriteSideEffects() {
		var a = player();
		var b = player();
		queries.summary(a.owner());
		queries.history(a.owner(), "WORLD", null, 0);
		assertEquals(0,
				jdbc.sql("SELECT count(*) FROM chat_settings WHERE character_id=:self")
					.param("self", a.character())
					.query(Long.class)
					.single());
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			chat.send(a.owner(), privateMessage(b, "回滚消息"));
			throw new IllegalStateException("后置失败");
		}));
		assertTrue(queries.history(b.owner(), "PRIVATE", a.name(), 0).entries().isEmpty());
		assertEquals(0,
				jdbc.sql("SELECT count(*) FROM chat_settings WHERE character_id=:self")
					.param("self", a.character())
					.query(Long.class)
					.single());
		assertNotNull(chat.send(a.owner(), privateMessage(b, "回滚后正常发送")));
	}

	/** 正式任务交付、物品和伙伴首次发放都会生成本人通知，原来源重放保持通知数量不变。 */
	@Test
	void questItemAndCreatureEventsGenerateOnlyFirstSuccessfulNotifications() {
		var a = player();
		for (String location : List.of("STONE_PATH", "SQUARE"))
			characters.move(a.owner(), location, characterQueries.find(a.owner()).revision());
		acceptance.accept(a.owner(), "O001",
				new QuestAcceptRequest(UUID.randomUUID().toString(), characterQueries.find(a.owner()).revision()));
		characters.move(a.owner(), "PET_CENTER", characterQueries.find(a.owner()).revision());
		quests.interact(a.owner(), "O001");
		long afterQuest = personalMessages(a.character());
		assertTrue(queries.history(a.owner(), "SYSTEM", null, 0)
			.entries()
			.stream()
			.anyMatch(m -> m.text().contains("任务交付完成：第一份委托")));
		quests.interact(a.owner(), "O001");
		assertEquals(afterQuest, personalMessages(a.character()));
		transactions.run(() -> {
			inventory.grant(a.owner(), "CHAT_ITEM", "ENHANCEMENT_STONE_I", 1, false);
			creatures.grant(a.owner(), "CHAT_PET", "SPROUT");
		});
		var notices = queries.history(a.owner(), "SYSTEM", null, 0).entries();
		assertTrue(notices.stream().anyMatch(m -> m.text().contains("获得强化石I")));
		assertTrue(notices.stream().anyMatch(m -> m.text().contains("获得伙伴：")));
		long afterRewards = personalMessages(a.character());
		transactions.run(() -> {
			inventory.grant(a.owner(), "CHAT_ITEM", "ENHANCEMENT_STONE_I", 1, false);
			creatures.grant(a.owner(), "CHAT_PET", "SPROUT");
		});
		assertEquals(afterRewards, personalMessages(a.character()));
	}

	/** 较小雪花编号更晚提交时不能被另一公告的已读水位吞掉；逐条凭证只标已提交消息。 */
	@Test
	void lateCommittedAnnouncementRetainsUnreadEvenWithSmallerId() throws Exception {
		var a = player();
		var inserted = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		String source = "LATE:" + a.character();
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var late = executor.submit(() -> transactions.call(() -> {
				var message = store.insert("SYSTEM", null, "系统", null, null, "较晚提交公告", source);
				inserted.countDown();
				try {
					if (!release.await(10, TimeUnit.SECONDS))
						throw new IllegalStateException("等待提交超时");
				}
				catch (InterruptedException failure) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(failure);
				}
				return message;
			}));
			try {
				assertTrue(inserted.await(5, TimeUnit.SECONDS));
				var newer = transactions
					.call(() -> store.insert("SYSTEM", null, "系统", null, null, "先提交公告", "EARLY:" + a.character()));
				chat.read(a.owner(), new ChatRead("SYSTEM", null, newer.id()));
				release.countDown();
				var prior = late.get(10, TimeUnit.SECONDS);
				assertTrue(Long.parseLong(prior.id()) < Long.parseLong(newer.id()));
				assertFalse(queries.history(a.owner(), "SYSTEM", null, 0)
					.entries()
					.stream()
					.filter(m -> m.id().equals(prior.id()))
					.findFirst()
					.orElseThrow()
					.read());
			}
			finally {
				release.countDown();
			}
		}
	}

	/** 战外隔离教学不修改真实资产，但不属于战斗；聊天不能误复用资产门禁把它也禁掉。 */
	@Test
	void nonCombatCourseAllowsCommunicationWithoutChangingGameAssets() {
		var a = player();
		var b = player();
		transactions.run(() -> characters.beginCourse(a.owner(), 321L));
		var before = characterQueries.find(a.owner());
		var sent = chat.send(a.owner(), privateMessage(b, "战外教学期间交流"));
		assertEquals(sent.id(), queries.history(a.owner(), "PRIVATE", b.name(), 0).entries().getFirst().id());
		assertEquals(before, characterQueries.find(a.owner()), "聊天不修改课程占用、经验、金币或角色版本");
		transactions.run(() -> characters.endCourse(a.owner(), 321L));
	}

	private long personalMessages(long character) {
		return jdbc.sql("SELECT count(*) FROM chat_message WHERE channel='SYSTEM' AND recipient=:self")
			.param("self", character)
			.query(Long.class)
			.single();
	}

	private record Player(long owner, long character, String name, String token) {
	}

}
