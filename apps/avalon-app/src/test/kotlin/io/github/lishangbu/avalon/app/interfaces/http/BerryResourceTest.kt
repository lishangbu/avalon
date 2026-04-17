package io.github.lishangbu.avalon.app.interfaces.http

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType.JSON
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class BerryResourceTest : AuthenticatedHttpResourceTest() {
    @Order(1)
    @Test
    fun shouldExposeSeededBerryReferenceData() {
        given()
            .`when`()
            .get("/catalog/berries")
            .then()
            .statusCode(200)
            .body("size()", equalTo(79))
            .body("code", hasItems("CHERI_BERRY", "SITRUS_BERRY", "ENIGMA_BERRY", "ROSELI_BERRY", "HOPO_BERRY"))
            .body("find { it.code == 'SITRUS_BERRY' }.name", equalTo("文柚果"))

        val berryId =
            given()
                .`when`()
                .get("/catalog/berries")
                .then()
                .statusCode(200)
                .extract()
                .path<String>("find { it.code == 'SITRUS_BERRY' }.id")

        given()
            .`when`()
            .get("/catalog/berries/$berryId")
            .then()
            .statusCode(200)
            .body("name", equalTo("文柚果"))
            .body("firmnessCode", equalTo("VERY_HARD"))
            .body("sizeCm", equalTo(9.5f))
            .body("naturalGiftType.code", equalTo("PSYCHIC"))
            .body("naturalGiftPower", equalTo(60))

        given()
            .`when`()
            .get("/catalog/berries/$berryId/cultivation")
            .then()
            .statusCode(200)
            .body("growthHoursMin", equalTo(24))
            .body("growthHoursMax", equalTo(48))
            .body("yieldMin", equalTo(2))
            .body("yieldMax", equalTo(27))

        given()
            .`when`()
            .get("/catalog/berries/$berryId/battle-effect")
            .then()
            .statusCode(200)
            .body("holdEffectSummary", equalTo("回复 1 ⁄ 4 最大 ＨＰ"))
            .body("flingPower", equalTo(10))

        given()
            .`when`()
            .get("/catalog/berries/$berryId/moves")
            .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
            .body("moveCode", hasItems("FLING", "NATURAL_GIFT", "PLUCK", "BUG_BITE"))
    }

    @Order(2)
    @Test
    fun shouldManageCustomBerryAndSubResources() {
        val fireTypeId =
            given()
                .`when`()
                .get("/catalog/types")
                .then()
                .statusCode(200)
                .extract()
                .path<String>("find { it.code == 'FIRE' }.id")

        val berryId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "test-berry",
                        "name" to "测试树果",
                        "description" to "测试树果说明",
                        "colorCode" to "RED",
                        "firmnessCode" to "SOFT",
                        "sizeCm" to 7.5,
                        "smoothness" to 20,
                        "spicy" to 10,
                        "dry" to 0,
                        "sweet" to 0,
                        "bitter" to 0,
                        "sour" to 0,
                        "naturalGiftTypeId" to fireTypeId,
                        "naturalGiftPower" to 60,
                        "sortingOrder" to 999,
                    ),
                ).post("/catalog/berries")
                .then()
                .statusCode(200)
                .body("code", equalTo("TEST_BERRY"))
                .extract()
                .path<String>("id")

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "test-berry",
                    "name" to "测试树果改",
                    "description" to "测试树果说明更新",
                    "colorCode" to "PURPLE",
                    "firmnessCode" to "VERY_SOFT",
                    "sizeCm" to 8.5,
                    "smoothness" to 30,
                    "spicy" to 0,
                    "dry" to 10,
                    "sweet" to 10,
                    "bitter" to 0,
                    "sour" to 0,
                    "naturalGiftTypeId" to fireTypeId,
                    "naturalGiftPower" to 70,
                    "sortingOrder" to 1000,
                    "enabled" to false,
                ),
            ).put("/catalog/berries/$berryId")
            .then()
            .statusCode(200)
            .body("name", equalTo("测试树果改"))
            .body("enabled", equalTo(false))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "holdEffectSummary" to "危机时回复ＨＰ",
                    "directUseEffectSummary" to "直接使用会回复ＨＰ",
                    "flingPower" to 10,
                    "flingEffectSummary" to "投掷命中后回复目标ＨＰ",
                    "pluckEffectSummary" to "被啄食后效果转移给对手",
                    "bugBiteEffectSummary" to "被虫咬后效果转移给对手",
                ),
            ).put("/catalog/berries/$berryId/battle-effect")
            .then()
            .statusCode(200)
            .body("holdEffectSummary", equalTo("危机时回复ＨＰ"))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "growthHoursMin" to 24,
                    "growthHoursMax" to 48,
                    "yieldMin" to 2,
                    "yieldMax" to 5,
                    "cultivationSummary" to "适合测试的种植资料",
                ),
            ).put("/catalog/berries/$berryId/cultivation")
            .then()
            .statusCode(200)
            .body("yieldMax", equalTo(5))

        val acquisitionId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "sourceType" to "BERRY_TREE",
                        "conditionNote" to "可以从树果树获得",
                        "sortingOrder" to 10,
                    ),
                ).post("/catalog/berries/$berryId/acquisitions")
                .then()
                .statusCode(200)
                .body("sourceType", equalTo("BERRY_TREE"))
                .extract()
                .path<String>("id")

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "sourceType" to "NPC_GIFT",
                    "conditionNote" to "作为赠送道具获得",
                    "sortingOrder" to 20,
                ),
            ).put("/catalog/berries/$berryId/acquisitions/$acquisitionId")
            .then()
            .statusCode(200)
            .body("sourceType", equalTo("NPC_GIFT"))

        val moveRelationId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "moveCode" to "NATURAL_GIFT",
                        "moveName" to "自然之恩",
                        "relationKind" to "NATURAL_GIFT",
                        "note" to "会改变招式属性和威力",
                        "sortingOrder" to 10,
                    ),
                ).post("/catalog/berries/$berryId/moves")
                .then()
                .statusCode(200)
                .body("moveCode", equalTo("NATURAL_GIFT"))
                .extract()
                .path<String>("id")

        val abilityRelationId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "abilityCode" to "RIPEN",
                        "abilityName" to "熟成",
                        "relationKind" to "BATTLE_INTERACTION",
                        "note" to "使树果效果翻倍",
                        "sortingOrder" to 10,
                    ),
                ).post("/catalog/berries/$berryId/abilities")
                .then()
                .statusCode(200)
                .body("abilityCode", equalTo("RIPEN"))
                .extract()
                .path<String>("id")

        given()
            .`when`()
            .get("/catalog/berries/$berryId")
            .then()
            .statusCode(200)
            .body("colorCode", equalTo("PURPLE"))

        given()
            .`when`()
            .get("/catalog/berries/$berryId/acquisitions")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].sourceType", equalTo("NPC_GIFT"))

        given()
            .`when`()
            .get("/catalog/berries/$berryId/moves")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))

        given()
            .`when`()
            .get("/catalog/berries/$berryId/abilities")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))

        given()
            .delete("/catalog/berries/$berryId/moves/$moveRelationId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/berries/$berryId/abilities/$abilityRelationId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/berries/$berryId/acquisitions/$acquisitionId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/berries/$berryId")
            .then()
            .statusCode(204)
    }
}
