package io.github.lishangbu.avalon.app;

import org.assertj.core.api.Assertions;
import org.springframework.http.MediaType;
import static org.hamcrest.MatcherAssert.assertThat;

import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.domain.NavigationRules;

import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/** 真实认证与数据库验证配置隔离；导航白名单不能成为任意地址或设施操作入口。 */
class NavigationTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Test
	void preferencesAreAuthenticatedIsolatedValidatedAndPersistent() {
		var first = registrations.register("nav_" + UUID.randomUUID().toString().substring(0, 8), "Navigation-test-123",
				"测试");
		var second = registrations.register("nav_" + UUID.randomUUID().toString().substring(0, 8),
				"Navigation-test-123", "测试");
		client.get().uri("/api/account/navigation").exchange().expectStatus().isEqualTo(401);
		client.get()
			.uri("/api/account/navigation")
			.headers(headers -> headers.setBearerAuth(first.accessToken()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$")
			.value(List.class,
					actual -> Assertions.assertThat(actual).containsExactly("COMPANIONS", "INVENTORY", "CHARACTER"));
		List<String> chosen = List.of("QUESTS", "ENCYCLOPEDIA", "INVENTORY");
		for (int retry = 0; retry < 2; retry++) {
			client.put()
				.uri("/api/account/navigation")
				.headers(headers -> headers.setBearerAuth(first.accessToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(chosen)
				.exchange()
				.expectStatus()
				.isEqualTo(200)
				.expectBody()
				.jsonPath("$")
				.value(List.class, actual -> Assertions.assertThat(actual).containsExactly(chosen.toArray()));
		}
		for (List<String> invalid : List.of(List.of("QUESTS", "QUESTS", "CHARACTER"),
				List.of("WORLD", "SHOP", "CHARACTER"), List.of("CHARACTER"))) {
			client.put()
				.uri("/api/account/navigation")
				.headers(headers -> headers.setBearerAuth(first.accessToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(invalid)
				.exchange()
				.expectStatus()
				.isEqualTo(400);
		}
		client.get()
			.uri("/api/account/navigation")
			.headers(headers -> headers.setBearerAuth(first.accessToken()))
			.exchange()
			.expectBody()
			.jsonPath("$")
			.value(List.class, actual -> Assertions.assertThat(actual).containsExactly(chosen.toArray()));
		client.get()
			.uri("/api/account/navigation")
			.headers(headers -> headers.setBearerAuth(second.accessToken()))
			.exchange()
			.expectBody()
			.jsonPath("$")
			.value(List.class,
					actual -> Assertions.assertThat(actual).containsExactly("COMPANIONS", "INVENTORY", "CHARACTER"));
		assertEquals(List.of("COMPANIONS", "QUESTS", "CHARACTER"), NavigationRules.restore("OLD,QUESTS,CHARACTER"));
		assertEquals(NavigationRules.DEFAULTS, NavigationRules.restore(null));
	}

}
