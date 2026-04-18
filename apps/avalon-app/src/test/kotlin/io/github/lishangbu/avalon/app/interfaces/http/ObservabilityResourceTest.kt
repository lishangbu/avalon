package io.github.lishangbu.avalon.app.interfaces.http

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Test

@QuarkusTest
class ObservabilityResourceTest : AuthenticatedHttpResourceTest() {
    @ConfigProperty(name = "quarkus.http.test-port")
    var testPort: Int = 0

    @Test
    fun shouldGenerateRequestIdHeaderForApplicationRequests() {
        given()
            .`when`().get("/catalog/types")
            .then()
            .statusCode(200)
            .header("X-Request-ID", matchesPattern("^[0-9a-fA-F-]{36}$"))
    }

    @Test
    fun shouldPreserveIncomingRequestIdHeader() {
        given()
            .header("X-Request-ID", "req-avalon-observability")
            .`when`().get("/catalog/types")
            .then()
            .statusCode(200)
            .header("X-Request-ID", equalTo("req-avalon-observability"))
    }

    @Test
    fun shouldExposePrometheusMetricsEndpoint() {
        given()
            .`when`().get("/catalog/types")
            .then()
            .statusCode(200)

        given()
            .`when`().get("http://localhost:$testPort/q/metrics")
            .then()
            .statusCode(200)
            .body(containsString("http_server_requests"))
    }
}
