package io.github.lishangbu.avalon.app.interfaces.http

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType.JSON
import io.restassured.RestAssured.given
import java.util.UUID
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
class SpeciesMoveLearnsetResourceTest : AuthenticatedHttpResourceTest() {
    @Test
    fun shouldManageSpeciesMoveLearnsetsAndRejectInvalidLevels() {
        var typeId: UUID? = null
        var speciesId: UUID? = null
        var moveId: UUID? = null
        var levelUpLearnMethodId: UUID? = null
        var machineLearnMethodId: UUID? = null
        var learnsetId: UUID? = null

        try {
            typeId =
                given()
                    .contentType(JSON)
                    .body(
                        mapOf(
                            "code" to "fire-learnset",
                            "name" to "Fire Learnset Type",
                            "description" to "Type used by learnset tests",
                            "sortingOrder" to 10,
                        ),
                    ).post("/api/catalog/types")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo("FIRE-LEARNSET"))
                    .extract()
                    .path<String>("id")
                    .toUuid()

            speciesId =
                given()
                    .contentType(JSON)
                    .body(
                        mapOf(
                            "code" to "flameling-learnset",
                            "dexNumber" to 9001,
                            "name" to "Flameling Learnset",
                            "description" to "Species used by learnset tests",
                            "primaryTypeId" to typeId,
                            "baseStats" to
                                    mapOf(
                                        "hp" to 39,
                                        "attack" to 52,
                                        "defense" to 43,
                                        "specialAttack" to 60,
                                        "specialDefense" to 50,
                                        "speed" to 65,
                                    ),
                            "sortingOrder" to 10,
                        ),
                    ).post("/api/catalog/species")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo("FLAMELING-LEARNSET"))
                    .extract()
                    .path<String>("id")
                    .toUuid()

            moveId =
                given()
                    .contentType(JSON)
                    .body(
                        mapOf(
                            "code" to "ember-learnset",
                            "name" to "Ember Learnset",
                            "typeDefinitionId" to typeId,
                            "categoryCode" to "special",
                            "powerPoints" to 25,
                            "sortingOrder" to 10,
                        ),
                    ).post("/api/catalog/moves")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo("EMBER-LEARNSET"))
                    .extract()
                    .path<String>("id")
                    .toUuid()

            levelUpLearnMethodId =
                given()
                    .contentType(JSON)
                    .body(
                        mapOf(
                            "code" to "level-up",
                            "name" to "Level Up Learnset",
                            "description" to "Learned by leveling up",
                            "sortingOrder" to 10,
                        ),
                    ).post("/api/catalog/move-learn-methods")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo("LEVEL-UP"))
                    .extract()
                    .path<String>("id")
                    .toUuid()

            machineLearnMethodId =
                given()
                    .contentType(JSON)
                    .body(
                        mapOf(
                            "code" to "machine",
                            "name" to "Machine Learnset",
                            "description" to "Learned from a machine",
                            "sortingOrder" to 20,
                        ),
                    ).post("/api/catalog/move-learn-methods")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo("MACHINE"))
                    .extract()
                    .path<String>("id")
                    .toUuid()

            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "speciesId" to speciesId,
                        "moveId" to moveId,
                        "learnMethodId" to levelUpLearnMethodId,
                        "sortingOrder" to 15,
                    ),
                ).post("/api/catalog/species-move-learnsets")
                .then()
                .statusCode(400)
                .body("type", equalTo("urn:avalon:problem:catalog:bad-request"))
                .body("title", equalTo("Bad Request"))
                .body("status", equalTo(400))
                .body("code", equalTo("catalog_bad_request"))
                .body("detail", equalTo("level is required when learn method code is LEVEL-UP."))

            learnsetId =
                given()
                    .contentType(JSON)
                    .body(
                        mapOf(
                            "speciesId" to speciesId,
                            "moveId" to moveId,
                            "learnMethodId" to levelUpLearnMethodId,
                            "level" to 7,
                            "sortingOrder" to 10,
                        ),
                    ).post("/api/catalog/species-move-learnsets")
                    .then()
                    .statusCode(200)
                    .body("species.code", equalTo("FLAMELING-LEARNSET"))
                    .body("move.code", equalTo("EMBER-LEARNSET"))
                    .body("learnMethod.code", equalTo("LEVEL-UP"))
                    .body("level", equalTo(7))
                    .extract()
                    .path<String>("id")
                    .toUuid()

            given()
                .`when`()
                .get("/api/catalog/species-move-learnsets/$learnsetId")
                .then()
                .statusCode(200)
                .body("move.name", equalTo("Ember Learnset"))

            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "speciesId" to speciesId,
                        "moveId" to moveId,
                        "learnMethodId" to machineLearnMethodId,
                        "level" to 17,
                        "sortingOrder" to 5,
                        "enabled" to false,
                    ),
                ).put("/api/catalog/species-move-learnsets/$learnsetId")
                .then()
                .statusCode(400)
                .body("type", equalTo("urn:avalon:problem:catalog:bad-request"))
                .body("title", equalTo("Bad Request"))
                .body("status", equalTo(400))
                .body("code", equalTo("catalog_bad_request"))
                .body("detail", equalTo("level must be null unless learn method code is LEVEL-UP."))

            given()
                .`when`()
                .get("/api/catalog/species-move-learnsets/$learnsetId")
                .then()
                .statusCode(200)
                .body("learnMethod.code", equalTo("LEVEL-UP"))
                .body("level", equalTo(7))

            given()
                .`when`()
                .get("/api/catalog/species-move-learnsets")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].species.code", equalTo("FLAMELING-LEARNSET"))
        } finally {
            learnsetId?.let {
                given()
                    .delete("/api/catalog/species-move-learnsets/$it")
                    .then()
                    .statusCode(204)
            }

            machineLearnMethodId?.let {
                given()
                    .delete("/api/catalog/move-learn-methods/$it")
                    .then()
                    .statusCode(204)
            }

            levelUpLearnMethodId?.let {
                given()
                    .delete("/api/catalog/move-learn-methods/$it")
                    .then()
                    .statusCode(204)
            }

            moveId?.let {
                given()
                    .delete("/api/catalog/moves/$it")
                    .then()
                    .statusCode(204)
            }

            speciesId?.let {
                given()
                    .delete("/api/catalog/species/$it")
                    .then()
                    .statusCode(204)
            }

            typeId?.let {
                given()
                    .delete("/api/catalog/types/$it")
                    .then()
                    .statusCode(204)
            }
        }
    }
}

