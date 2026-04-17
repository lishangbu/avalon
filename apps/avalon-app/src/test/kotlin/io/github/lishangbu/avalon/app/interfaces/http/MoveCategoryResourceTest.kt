package io.github.lishangbu.avalon.app.interfaces.http

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType.JSON
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
class MoveCategoryResourceTest : AuthenticatedHttpResourceTest() {
    @Test
    fun shouldManageMoveCategories() {
        val categoryId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "damage+ailment-crud",
                        "name" to "Damage + Ailment CRUD",
                        "description" to "Inflicts damage and a status ailment",
                        "sortingOrder" to 10,
                    ),
                ).post("/catalog/move-categories")
                .then()
                .statusCode(200)
                .body("code", equalTo("DAMAGE+AILMENT-CRUD"))
                .body("name", equalTo("Damage + Ailment CRUD"))
                .extract()
                .path<String>("id")
                .toUuid()

        given()
            .`when`()
            .get("/catalog/move-categories/$categoryId")
            .then()
            .statusCode(200)
            .body("description", equalTo("Inflicts damage and a status ailment"))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "damage+ailment-crud",
                    "name" to "Damage + Ailment Plus",
                    "description" to "Updated category description",
                    "sortingOrder" to 5,
                    "enabled" to false,
                ),
            ).put("/catalog/move-categories/$categoryId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Damage + Ailment Plus"))
            .body("enabled", equalTo(false))

        given()
            .`when`()
            .get("/catalog/move-categories")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].code", equalTo("DAMAGE+AILMENT-CRUD"))

        given()
            .delete("/catalog/move-categories/$categoryId")
            .then()
            .statusCode(204)
    }
}