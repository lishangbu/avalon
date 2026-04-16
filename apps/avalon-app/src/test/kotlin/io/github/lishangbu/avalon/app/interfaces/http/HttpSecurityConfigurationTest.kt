package io.github.lishangbu.avalon.app.interfaces.http

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.junit.jupiter.api.Test

@QuarkusTest
class HttpSecurityConfigurationTest {
    @Test
    fun shouldRequireAuthenticationForProtectedApiRoutes() {
        given()
            .`when`().get("/api/catalog/types")
            .then()
            .statusCode(401)

        given()
            .`when`().get("/api/iam/users")
            .then()
            .statusCode(401)
    }
}