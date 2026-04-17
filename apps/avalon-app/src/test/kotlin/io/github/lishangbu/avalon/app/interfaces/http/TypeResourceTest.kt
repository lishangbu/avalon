package io.github.lishangbu.avalon.app.interfaces.http

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType.JSON
import io.restassured.RestAssured.given
import io.github.lishangbu.avalon.shared.infra.mutiny.awaitSuspending
import io.vertx.mutiny.sqlclient.Pool
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
class TypeResourceTest : AuthenticatedHttpResourceTest() {
    @Inject
    lateinit var pool: Pool

    private val createdTypeIds = mutableListOf<UUID>()

    @AfterEach
    fun cleanupTypes() {
        if (createdTypeIds.isEmpty()) {
            return
        }

        runBlocking {
            val quotedIds = createdTypeIds.joinToString(",") { "'$it'" }
            pool.query("DELETE FROM catalog.type_effectiveness WHERE attacking_type_id IN ($quotedIds) OR defending_type_id IN ($quotedIds)")
                .execute()
                .awaitSuspending()
            pool.query("DELETE FROM catalog.type_definition WHERE id IN ($quotedIds)")
                .execute()
                .awaitSuspending()
        }
        createdTypeIds.clear()
    }

    @Test
    fun shouldManageTypeDefinitionsAndTypeChart() {
        val suffix = UUID.randomUUID().toString().takeLast(8)
        val fireCode = "TYPE-FIRE-$suffix".uppercase()
        val waterCode = "TYPE-WATER-$suffix".uppercase()
        val fireTypeId =
            createType(
                code = "type-fire-$suffix",
                name = "Type Fire $suffix",
                sortingOrder = 10,
            )
        val waterTypeId =
            createType(
                code = "type-water-$suffix",
                name = "Type Water $suffix",
                sortingOrder = 20,
            )

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "type-fire-$suffix",
                    "name" to "Type Flame $suffix",
                    "description" to "Updated fire type",
                    "sortingOrder" to 5,
                    "enabled" to false,
                ),
            ).put("/catalog/types/$fireTypeId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Type Flame $suffix"))
            .body("enabled", equalTo(false))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "entries" to
                        listOf(
                            mapOf(
                                "attackingTypeId" to fireTypeId,
                                "defendingTypeId" to fireTypeId,
                                "multiplier" to 1.0,
                            ),
                            mapOf(
                                "attackingTypeId" to fireTypeId,
                                "defendingTypeId" to waterTypeId,
                                "multiplier" to 0.5,
                            ),
                            mapOf(
                                "attackingTypeId" to waterTypeId,
                                "defendingTypeId" to fireTypeId,
                                "multiplier" to 2.0,
                            ),
                            mapOf(
                                "attackingTypeId" to waterTypeId,
                                "defendingTypeId" to waterTypeId,
                                "multiplier" to 1.0,
                            ),
                        ),
                ),
            ).put("/catalog/type-chart")
            .then()
            .statusCode(200)
            .body("types.size()", equalTo(2))
            .body("types.code", hasItem(fireCode))
            .body("types.code", hasItem(waterCode))
            .body("entries.size()", equalTo(4))

        given()
            .`when`()
            .get("/catalog/type-chart")
            .then()
            .statusCode(200)
            .body("types.size()", equalTo(2))
            .body("entries.size()", equalTo(4))
            .body("entries.find { it.attackingType.code == '$waterCode' && it.defendingType.code == '$fireCode' }.multiplier", equalTo(2.0f))

        given()
            .`when`()
            .get("/catalog/types")
            .then()
            .statusCode(200)
            .body("find { it.id == '$fireTypeId' }.name", equalTo("Type Flame $suffix"))
    }

    @Test
    fun shouldRejectIncompleteTypeChart() {
        val suffix = UUID.randomUUID().toString().takeLast(8)
        val fireTypeId =
            createType(
                code = "chart-fire-$suffix",
                name = "Chart Fire $suffix",
                sortingOrder = 10,
            )
        val waterTypeId =
            createType(
                code = "chart-water-$suffix",
                name = "Chart Water $suffix",
                sortingOrder = 20,
            )

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "entries" to
                        listOf(
                            mapOf(
                                "attackingTypeId" to fireTypeId,
                                "defendingTypeId" to fireTypeId,
                                "multiplier" to 1.0,
                            ),
                            mapOf(
                                "attackingTypeId" to waterTypeId,
                                "defendingTypeId" to fireTypeId,
                                "multiplier" to 2.0,
                            ),
                        ),
                ),
            ).put("/catalog/type-chart")
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
}
