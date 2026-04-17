package io.github.lishangbu.avalon.app.interfaces.http

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType.JSON
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
class TypeResourceTest : AuthenticatedHttpResourceTest() {
    private val createdTypeIds = mutableListOf<UUID>()
    private var originalChartEntries: List<Map<String, Any?>>? = null

    @AfterEach
    fun cleanup() {
        originalChartEntries?.let { entries ->
            givenAsSeedAdmin()
                .contentType(JSON)
                .body(mapOf("entries" to entries))
                .put("/catalog/type-chart")
                .then()
                .statusCode(200)
            originalChartEntries = null
        }

        createdTypeIds.asReversed().forEach { id ->
            givenAsSeedAdmin()
                .delete("/catalog/types/$id")
                .then()
                .statusCode(204)
        }
        createdTypeIds.clear()
    }

    @Test
    fun shouldExposeSeededTypesAndTypeChart() {
        given()
            .`when`()
            .get("/catalog/types")
            .then()
            .statusCode(200)
            .body("size()", equalTo(18))
            .body("code", hasItems("NORMAL", "FIGHTING", "FIRE", "WATER", "GRASS", "ELECTRIC", "DRAGON", "FAIRY"))
            .body("find { it.code == 'FIRE' }.name", equalTo("火"))
            .body("find { it.code == 'FAIRY' }.name", equalTo("妖精"))

        given()
            .`when`()
            .get("/catalog/type-chart")
            .then()
            .statusCode(200)
            .body("types.size()", equalTo(18))
            .body("entries.size()", equalTo(324))
            .body("entries.find { it.attackingType.code == 'WATER' && it.defendingType.code == 'FIRE' }.multiplier", equalTo(2.0f))
            .body("entries.find { it.attackingType.code == 'NORMAL' && it.defendingType.code == 'GHOST' }.multiplier", equalTo(0.0f))
            .body("entries.find { it.attackingType.code == 'FAIRY' && it.defendingType.code == 'DRAGON' }.multiplier", equalTo(2.0f))
    }

    @Test
    fun shouldManageCustomTypeDefinitions() {
        val suffix = UUID.randomUUID().toString().takeLast(8)
        val typeCode = "TYPE-CUSTOM-$suffix".uppercase()
        val typeId =
            createType(
                code = "type-custom-$suffix",
                name = "Type Custom $suffix",
                sortingOrder = 999,
            )

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "type-custom-$suffix",
                    "name" to "Type Custom Revised $suffix",
                    "description" to "Custom type for test",
                    "sortingOrder" to 888,
                    "enabled" to false,
                ),
            ).put("/catalog/types/$typeId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Type Custom Revised $suffix"))
            .body("enabled", equalTo(false))

        given()
            .`when`()
            .get("/catalog/types")
            .then()
            .statusCode(200)
            .body("code", hasItems(typeCode))
            .body("find { it.id == '$typeId' }.name", equalTo("Type Custom Revised $suffix"))
    }

    @Test
    fun shouldReplaceTypeChart() {
        val entries = currentChartEntries()
        originalChartEntries = entries

        val updatedEntries =
            entries.map { entry ->
                val attackingTypeId = entry["attackingTypeId"]
                val defendingTypeId = entry["defendingTypeId"]
                if (attackingTypeId == waterTypeId() && defendingTypeId == fireTypeId()) {
                    entry + ("multiplier" to 0.5)
                } else {
                    entry
                }
            }

        given()
            .contentType(JSON)
            .body(mapOf("entries" to updatedEntries))
            .put("/catalog/type-chart")
            .then()
            .statusCode(200)
            .body("entries.size()", equalTo(324))
            .body("entries.find { it.attackingType.code == 'WATER' && it.defendingType.code == 'FIRE' }.multiplier", equalTo(0.5f))
    }

    @Test
    fun shouldRejectIncompleteTypeChart() {
        val entries = currentChartEntries()

        given()
            .contentType(JSON)
            .body(mapOf("entries" to entries.dropLast(1)))
            .put("/catalog/type-chart")
            .then()
            .statusCode(400)
    }

    private fun createType(
        code: String,
        name: String,
        sortingOrder: Int,
    ): UUID {
        val id =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to code,
                        "name" to name,
                        "sortingOrder" to sortingOrder,
                    ),
                ).post("/catalog/types")
                .then()
                .statusCode(200)
                .extract()
                .path<String>("id")
                .toUuid()
        createdTypeIds += id
        return id
    }

    @Suppress("UNCHECKED_CAST")
    private fun currentChartEntries(): List<Map<String, Any?>> {
        val rawEntries =
            given()
                .`when`()
                .get("/catalog/type-chart")
                .then()
                .statusCode(200)
                .extract()
                .path<List<Map<String, Any?>>>("entries")

        return rawEntries.map { entry ->
            val attackingType = entry["attackingType"] as Map<String, Any?>
            val defendingType = entry["defendingType"] as Map<String, Any?>
            mapOf(
                "attackingTypeId" to attackingType["id"],
                "defendingTypeId" to defendingType["id"],
                "multiplier" to entry["multiplier"],
            )
        }
    }

    private fun fireTypeId(): String = typeIdByCode("FIRE")

    private fun waterTypeId(): String = typeIdByCode("WATER")

    private fun typeIdByCode(code: String): String =
        given()
            .`when`()
            .get("/catalog/types")
            .then()
            .statusCode(200)
            .extract()
            .path("find { it.code == '$code' }.id")
}
