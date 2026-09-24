package io.github.lishangbu.avalon.app;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/** 迁移边界测试：请求上限、无状态认证及 SPA 回退不能改变原游戏 API 语义。 */
class SpringHttpContractTest extends SpringIntegrationTest {

	@Test
	void healthUsesDefaultActuatorPath() {
		client.get()
			.uri("/actuator/health")
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.status")
			.isEqualTo("UP");
		client.get().uri("/actuator/not-a-health-endpoint").exchange().expectStatus().isEqualTo(404);
	}

	@Test
	void oversizedAndMalformedBodiesKeepPublicErrorContract() {
		client.post()
			.uri("/api/auth/register")
			.contentType(MediaType.APPLICATION_JSON)
			.body("x".repeat(32769))
			.exchange()
			.expectStatus()
			.isEqualTo(413)
			.expectBody()
			.jsonPath("$.message")
			.value(String.class, actual -> assertThat(actual, not(blankOrNullString())));
		client.post()
			.uri("/api/auth/register")
			.contentType(MediaType.APPLICATION_JSON)
			.body("{")
			.exchange()
			.expectStatus()
			.isEqualTo(400)
			.expectBody()
			.jsonPath("$.message")
			.value(String.class, actual -> assertThat(actual, not(blankOrNullString())));
	}

	@Test
	void bearerLogoutRevokesImmediatelyWithoutServletSession() {
		var response = client.post()
			.uri("/api/auth/register")
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("username", "spring_" + UUID.randomUUID().toString().substring(0, 8), "password",
					"Spring-test-1234"))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.returnResult(JsonNode.class);
		Assertions.assertFalse(response.getResponseCookies().containsKey("JSESSIONID"));
		String token = response.getResponseBody().get("accessToken").asString();
		client.get()
			.uri("/api/not-a-page")
			.header("Authorization", "Bearer " + token)
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		client.post()
			.uri("/api/auth/logout")
			.header("Authorization", "Bearer " + token)
			.exchange()
			.expectStatus()
			.isEqualTo(204);
		client.get()
			.uri("/api/auth/sessions")
			.header("Authorization", "Bearer " + token)
			.exchange()
			.expectStatus()
			.isEqualTo(401);
	}

}
