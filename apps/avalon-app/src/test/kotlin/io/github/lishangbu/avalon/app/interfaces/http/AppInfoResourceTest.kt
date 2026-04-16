package io.github.lishangbu.avalon.app.interfaces.http

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.Test

@QuarkusTest
class AppInfoResourceTest {
    @Test
    fun shouldExposeApplicationMetadata() {
        given()
            .`when`().get("/api/app-info")
            .then()
            .statusCode(200)
            .body("name", equalTo("avalon"))
            .body("architecture", equalTo("modular-monolith"))
            .body("persistence", equalTo("reactive-sql-client-coroutines"))
            .body("contexts", hasItems("identity-access", "catalog", "player", "battle"))
    }
}