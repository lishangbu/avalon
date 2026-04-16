package io.github.lishangbu.avalon.app.interfaces.http

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType.JSON
import io.restassured.RestAssured.given
import java.util.UUID
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.Test

@QuarkusTest
class SpeciesResourceTest : AuthenticatedHttpResourceTest() {
    @Test
    fun shouldPageSpeciesResults() {
        val suffix = System.nanoTime().toString()
        val initialTotalItems =
            given()
                .`when`()
                .get("/api/catalog/species?size=1")
                .then()
                .statusCode(200)
                .extract()
                .path<Number>("totalItems")
                .toLong()

        val firstSpeciesCode = "SPECIES-PAGE-ONE-$suffix"
        val thirdSpeciesCode = "SPECIES-PAGE-THREE-$suffix"
        var fireTypeId: UUID? = null
        var growthRateId: UUID? = null
        var firstSpeciesId: UUID? = null
        var secondSpeciesId: UUID? = null
        var thirdSpeciesId: UUID? = null

        try {
            val createdFireTypeId = createType("species-page-fire-$suffix", "Species Page Fire $suffix", 900_000)
            fireTypeId = createdFireTypeId

            val createdGrowthRateId =
                createGrowthRate(
                    code = "species-page-growth-$suffix",
                    name = "Species Page Growth $suffix",
                    formulaCode = "medium_fast",
                    sortingOrder = 900_000,
                )
            growthRateId = createdGrowthRateId

            firstSpeciesId =
                createSpecies(
                    code = "species-page-one-$suffix",
                    dexNumber = 900_001,
                    name = "Species Page One $suffix",
                    primaryTypeId = createdFireTypeId,
                    growthRateId = createdGrowthRateId,
                    sortingOrder = 900_001,
                )
            secondSpeciesId =
                createSpecies(
                    code = "species-page-two-$suffix",
                    dexNumber = 900_002,
                    name = "Species Page Two $suffix",
                    primaryTypeId = createdFireTypeId,
                    growthRateId = createdGrowthRateId,
                    sortingOrder = 900_002,
                )
            thirdSpeciesId =
                createSpecies(
                    code = "species-page-three-$suffix",
                    dexNumber = 900_003,
                    name = "Species Page Three $suffix",
                    primaryTypeId = createdFireTypeId,
                    growthRateId = createdGrowthRateId,
                    sortingOrder = 900_003,
                )

            val firstNewPage = initialTotalItems.toInt() + 1
            val lastNewPage = initialTotalItems.toInt() + 3

            given()
                .`when`()
                .get("/api/catalog/species?page=$firstNewPage&size=1")
                .then()
                .statusCode(200)
                .body("items.size()", equalTo(1))
                .body("items[0].code", equalTo(firstSpeciesCode))
                .body("page", equalTo(firstNewPage))
                .body("size", equalTo(1))
                .body("totalItems", equalTo((initialTotalItems + 3L).toInt()))
                .body("totalPages", equalTo((initialTotalItems + 3L).toInt()))
                .body("hasNext", equalTo(true))

            given()
                .`when`()
                .get("/api/catalog/species?page=$lastNewPage&size=1")
                .then()
                .statusCode(200)
                .body("items.size()", equalTo(1))
                .body("items[0].code", equalTo(thirdSpeciesCode))
                .body("page", equalTo(lastNewPage))
                .body("size", equalTo(1))
                .body("totalItems", equalTo((initialTotalItems + 3L).toInt()))
                .body("totalPages", equalTo((initialTotalItems + 3L).toInt()))
                .body("hasNext", equalTo(false))

            given()
                .`when`()
                .get("/api/catalog/species?page=${lastNewPage + 1}&size=1")
                .then()
                .statusCode(200)
                .body("items.size()", equalTo(0))
                .body("page", equalTo(lastNewPage + 1))
                .body("size", equalTo(1))
                .body("totalItems", equalTo((initialTotalItems + 3L).toInt()))
                .body("hasNext", equalTo(false))
        } finally {
            thirdSpeciesId?.let(::deleteSpecies)
            secondSpeciesId?.let(::deleteSpecies)
            firstSpeciesId?.let(::deleteSpecies)
            growthRateId?.let(::deleteGrowthRate)
            fireTypeId?.let(::deleteType)
        }
    }

    @Test
    fun shouldRejectInvalidSpeciesPageParameters() {
        given()
            .`when`()
            .get("/api/catalog/species?page=0&size=101")
            .then()
            .statusCode(400)
            .body("code", equalTo("request_validation_failed"))
            .body("errors.field", hasItems("page", "size"))
    }

    private fun createType(
        code: String,
        name: String,
        sortingOrder: Int,
    ): UUID =
        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to code,
                    "name" to name,
                    "sortingOrder" to sortingOrder,
                ),
            ).post("/api/catalog/types")
            .then()
            .statusCode(200)
            .extract()
            .path<String>("id")
            .toUuid()

    private fun createGrowthRate(
        code: String,
        name: String,
        formulaCode: String,
        sortingOrder: Int,
    ): UUID =
        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to code,
                    "name" to name,
                    "formulaCode" to formulaCode,
                    "sortingOrder" to sortingOrder,
                ),
            ).post("/api/catalog/growth-rates")
            .then()
            .statusCode(200)
            .extract()
            .path<String>("id")
            .toUuid()

    private fun createSpecies(
        code: String,
        dexNumber: Int,
        name: String,
        primaryTypeId: UUID,
        growthRateId: UUID,
        sortingOrder: Int,
    ): UUID =
        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to code,
                    "dexNumber" to dexNumber,
                    "name" to name,
                    "primaryTypeId" to primaryTypeId,
                    "growthRateId" to growthRateId,
                    "baseStats" to
                            mapOf(
                                "hp" to 40,
                                "attack" to 50,
                                "defense" to 45,
                                "specialAttack" to 55,
                                "specialDefense" to 50,
                                "speed" to 60,
                            ),
                    "sortingOrder" to sortingOrder,
                ),
            ).post("/api/catalog/species")
            .then()
            .statusCode(200)
            .extract()
            .path<String>("id")
            .toUuid()

    private fun deleteSpecies(id: UUID) {
        given()
            .delete("/api/catalog/species/$id")
            .then()
            .statusCode(204)
    }

    private fun deleteGrowthRate(id: UUID) {
        given()
            .delete("/api/catalog/growth-rates/$id")
            .then()
            .statusCode(204)
    }

    private fun deleteType(id: UUID) {
        given()
            .delete("/api/catalog/types/$id")
            .then()
            .statusCode(204)
    }
}

