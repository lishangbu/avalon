package io.github.lishangbu.avalon.app.interfaces.http

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType.JSON
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
class MoveLearnMethodResourceTest : AuthenticatedHttpResourceTest() {
    @Test
    fun shouldManageMoveLearnMethods() {
        val methodId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "level-up-crud",
                        "name" to "Level Up CRUD",
                        "description" to "Learned by leveling up",
                        "sortingOrder" to 10,
                    ),
                ).post("/api/catalog/move-learn-methods")
                .then()
                .statusCode(200)
                .body("code", equalTo("LEVEL-UP-CRUD"))
                .body("name", equalTo("Level Up CRUD"))
                .extract()
                .path<String>("id")
                .toUuid()

        given()
            .`when`()
            .get("/api/catalog/move-learn-methods/$methodId")
            .then()
            .statusCode(200)
            .body("description", equalTo("Learned by leveling up"))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "level-up-crud",
                    "name" to "Level Up Plus",
                    "description" to "Updated method description",
                    "sortingOrder" to 5,
                    "enabled" to false,
                ),
            ).put("/api/catalog/move-learn-methods/$methodId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Level Up Plus"))
            .body("enabled", equalTo(false))

        given()
            .`when`()
            .get("/api/catalog/move-learn-methods")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].code", equalTo("LEVEL-UP-CRUD"))

        given()
            .delete("/api/catalog/move-learn-methods/$methodId")
            .then()
            .statusCode(204)
    }
}