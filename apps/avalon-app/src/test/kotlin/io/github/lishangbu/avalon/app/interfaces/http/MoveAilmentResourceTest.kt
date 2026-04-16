package io.github.lishangbu.avalon.app.interfaces.http

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType.JSON
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
class MoveAilmentResourceTest : AuthenticatedHttpResourceTest() {
    @Test
    fun shouldManageMoveAilments() {
        val ailmentId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "burn-crud",
                        "name" to "Burn CRUD",
                        "description" to "Inflicts burn",
                        "sortingOrder" to 10,
                    ),
                ).post("/api/catalog/move-ailments")
                .then()
                .statusCode(200)
                .body("code", equalTo("BURN-CRUD"))
                .body("name", equalTo("Burn CRUD"))
                .extract()
                .path<String>("id")
                .toUuid()

        given()
            .`when`()
            .get("/api/catalog/move-ailments/$ailmentId")
            .then()
            .statusCode(200)
            .body("description", equalTo("Inflicts burn"))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "burn-crud",
                    "name" to "Burn Plus",
                    "description" to "Updated ailment description",
                    "sortingOrder" to 5,
                    "enabled" to false,
                ),
            ).put("/api/catalog/move-ailments/$ailmentId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Burn Plus"))
            .body("enabled", equalTo(false))

        given()
            .`when`()
            .get("/api/catalog/move-ailments")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].code", equalTo("BURN-CRUD"))

        given()
            .delete("/api/catalog/move-ailments/$ailmentId")
            .then()
            .statusCode(204)
    }
}