package io.github.lishangbu.avalon.app.interfaces.http

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType.JSON
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
class MoveTargetResourceTest : AuthenticatedHttpResourceTest() {
    @Test
    fun shouldManageMoveTargets() {
        val targetId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "selected-pokemon",
                        "name" to "Selected Pokemon",
                        "description" to "Targets the selected opponent",
                        "sortingOrder" to 10,
                    ),
                ).post("/catalog/move-targets")
                .then()
                .statusCode(200)
                .body("code", equalTo("SELECTED-POKEMON"))
                .body("name", equalTo("Selected Pokemon"))
                .extract()
                .path<String>("id")
                .toUuid()

        given()
            .`when`()
            .get("/catalog/move-targets/$targetId")
            .then()
            .statusCode(200)
            .body("description", equalTo("Targets the selected opponent"))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "selected-pokemon",
                    "name" to "Selected Pokemon Plus",
                    "description" to "Updated target description",
                    "sortingOrder" to 5,
                    "enabled" to false,
                ),
            ).put("/catalog/move-targets/$targetId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Selected Pokemon Plus"))
            .body("enabled", equalTo(false))

        given()
            .`when`()
            .get("/catalog/move-targets")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].code", equalTo("SELECTED-POKEMON"))

        given()
            .delete("/catalog/move-targets/$targetId")
            .then()
            .statusCode(204)
    }
}