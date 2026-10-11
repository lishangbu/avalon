package io.github.lishangbu.avalon.app;

import org.springframework.http.MediaType;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.UUID;

/** 验证真实认证入口，未登录不能提交，生产开篇不能通过直接请求绕过11级门槛。 */
class MarketHttpTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Test
	void unauthenticatedWritesAreRejectedAndOpeningCharacterCannotUnlockMarket() {
		for (String endpoint : new String[] { "teaching", "items", "creatures", "purchase", "cancel",
				"returns/claim" }) {
			client.post()
				.uri("/api/market/" + endpoint)
				.contentType(MediaType.APPLICATION_JSON)
				.body("{}")
				.exchange()
				.expectStatus()
				.isEqualTo(401);
		}
		// HTTP是待测接口类型，不能把被禁止的导流词http用作新注册测试账号的前缀。
		String name = "market0_" + UUID.randomUUID().toString().substring(0, 5);
		var token = registrations.register(name, "Market-http-1234", "测试").accessToken();
		characters.create(accounts.accountId(name), name, "WARRIOR");
		client.post()
			.uri("/api/market/teaching")
			.header("Authorization", "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("accepted", true))
			.exchange()
			.expectStatus()
			.isEqualTo(409);
		client.post()
			.uri("/api/market/items")
			.header("Authorization", "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.body("null")
			.exchange()
			.expectStatus()
			.isEqualTo(400);
	}

}
