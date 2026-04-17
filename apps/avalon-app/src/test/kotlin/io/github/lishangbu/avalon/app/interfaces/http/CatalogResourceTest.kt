package io.github.lishangbu.avalon.app.interfaces.http

import io.github.lishangbu.avalon.shared.infra.mutiny.awaitSuspending
import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType.JSON
import io.restassured.RestAssured.given
import io.vertx.mutiny.sqlclient.Pool
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
class CatalogResourceTest : AuthenticatedHttpResourceTest() {
    @Inject
    lateinit var pool: Pool

    @Test
    fun shouldManageCatalogReferenceDataSlices() {
        val suffix = UUID.randomUUID().toString().takeLast(8)
        val fireCode = "FIRE-$suffix".uppercase()
        val waterCode = "WATER-$suffix".uppercase()
        val fireTypeId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to fireCode,
                        "name" to "Fire $suffix",
                        "description" to "Fire attribute",
                        "sortingOrder" to 10,
                    ),
                ).post("/catalog/types")
                .then()
                .statusCode(200)
                .body("code", equalTo(fireCode))
                .body("name", equalTo("Fire $suffix"))
                .extract()
                .path<String>("id")
                .toUuid()

        val waterTypeId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to waterCode,
                        "name" to "Water $suffix",
                        "sortingOrder" to 20,
                    ),
                ).post("/catalog/types")
                .then()
                .statusCode(200)
                .body("code", equalTo(waterCode))
                .extract()
                .path<String>("id")
                .toUuid()

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to fireCode,
                    "name" to "Flame $suffix",
                    "description" to "Renamed fire attribute",
                    "sortingOrder" to 5,
                    "enabled" to false,
                ),
            ).put("/catalog/types/$fireTypeId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Flame $suffix"))
            .body("enabled", equalTo(false))

        given()
            .`when`()
            .get("/catalog/types")
            .then()
            .statusCode(200)
            .body("code", hasItems(fireCode, waterCode))
            .body("find { it.id == '$fireTypeId' }.name", equalTo("Flame $suffix"))

        val hardyNatureId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "hardy",
                        "name" to "Hardy",
                        "description" to "Neutral nature",
                        "sortingOrder" to 10,
                    ),
                ).post("/catalog/natures")
                .then()
                .statusCode(200)
                .body("code", equalTo("HARDY"))
                .body("increasedStatCode", nullValue())
                .body("decreasedStatCode", nullValue())
                .extract()
                .path<String>("id")
                .toUuid()

        val adamantNatureId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "adamant",
                        "name" to "Adamant",
                        "increasedStatCode" to "attack",
                        "decreasedStatCode" to "special_attack",
                        "sortingOrder" to 20,
                    ),
                ).post("/catalog/natures")
                .then()
                .statusCode(200)
                .body("code", equalTo("ADAMANT"))
                .body("increasedStatCode", equalTo("ATTACK"))
                .body("decreasedStatCode", equalTo("SPECIAL_ATTACK"))
                .extract()
                .path<String>("id")
                .toUuid()

        given()
            .`when`()
            .get("/catalog/natures/$adamantNatureId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Adamant"))
            .body("increasedStatCode", equalTo("ATTACK"))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "adamant",
                    "name" to "Adamant Revised",
                    "description" to "Adjusted offensive nature",
                    "increasedStatCode" to "speed",
                    "decreasedStatCode" to "defense",
                    "sortingOrder" to 15,
                    "enabled" to false,
                ),
            ).put("/catalog/natures/$adamantNatureId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Adamant Revised"))
            .body("increasedStatCode", equalTo("SPEED"))
            .body("decreasedStatCode", equalTo("DEFENSE"))
            .body("enabled", equalTo(false))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "broken",
                    "name" to "Broken Nature",
                    "increasedStatCode" to "attack",
                    "decreasedStatCode" to "attack",
                ),
            ).post("/catalog/natures")
            .then()
            .statusCode(400)

        given()
            .`when`()
            .get("/catalog/natures")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].code", equalTo("HARDY"))

        val potionItemId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "potion",
                        "name" to "Potion",
                        "categoryCode" to "consumable",
                        "description" to "Restores a small amount of health",
                        "maxStackSize" to 99,
                        "consumable" to true,
                        "sortingOrder" to 10,
                    ),
                ).post("/catalog/items")
                .then()
                .statusCode(200)
                .body("code", equalTo("POTION"))
                .body("categoryCode", equalTo("CONSUMABLE"))
                .body("maxStackSize", equalTo(99))
                .body("consumable", equalTo(true))
                .extract()
                .path<String>("id")
                .toUuid()

        val keyItemId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "ancient-key",
                        "name" to "Ancient Key",
                        "categoryCode" to "key_item",
                        "maxStackSize" to 1,
                        "consumable" to false,
                        "sortingOrder" to 20,
                    ),
                ).post("/catalog/items")
                .then()
                .statusCode(200)
                .body("code", equalTo("ANCIENT-KEY"))
                .body("categoryCode", equalTo("KEY_ITEM"))
                .extract()
                .path<String>("id")
                .toUuid()

        given()
            .`when`()
            .get("/catalog/items/$potionItemId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Potion"))
            .body("consumable", equalTo(true))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "potion",
                    "name" to "Potion Plus",
                    "categoryCode" to "consumable",
                    "description" to "Updated restorative item",
                    "maxStackSize" to 50,
                    "consumable" to true,
                    "sortingOrder" to 5,
                    "enabled" to false,
                ),
            ).put("/catalog/items/$potionItemId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Potion Plus"))
            .body("maxStackSize", equalTo(50))
            .body("enabled", equalTo(false))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "broken-item",
                    "name" to "Broken Item",
                    "categoryCode" to "consumable",
                    "maxStackSize" to 0,
                ),
            ).post("/catalog/items")
            .then()
            .statusCode(400)

        given()
            .`when`()
            .get("/catalog/items")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].code", equalTo("POTION"))

        val blazeAbilityId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "blaze",
                        "name" to "Blaze",
                        "description" to "Empowers fire techniques at low health",
                        "sortingOrder" to 10,
                    ),
                ).post("/catalog/abilities")
                .then()
                .statusCode(200)
                .body("code", equalTo("BLAZE"))
                .body("name", equalTo("Blaze"))
                .extract()
                .path<String>("id")
                .toUuid()

        val torrentAbilityId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "torrent",
                        "name" to "Torrent",
                        "description" to "Empowers water techniques at low health",
                        "sortingOrder" to 20,
                    ),
                ).post("/catalog/abilities")
                .then()
                .statusCode(200)
                .body("code", equalTo("TORRENT"))
                .extract()
                .path<String>("id")
                .toUuid()

        given()
            .`when`()
            .get("/catalog/abilities/$blazeAbilityId")
            .then()
            .statusCode(200)
            .body("description", equalTo("Empowers fire techniques at low health"))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "blaze",
                    "name" to "Blaze Revised",
                    "description" to "Updated fire boost ability",
                    "icon" to "flame-badge",
                    "sortingOrder" to 5,
                    "enabled" to false,
                ),
            ).put("/catalog/abilities/$blazeAbilityId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Blaze Revised"))
            .body("icon", equalTo("flame-badge"))
            .body("enabled", equalTo(false))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "",
                    "name" to "Broken Ability",
                ),
            ).post("/catalog/abilities")
            .then()
            .statusCode(400)

        given()
            .`when`()
            .get("/catalog/abilities")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].code", equalTo("BLAZE"))

        val damageAilmentMoveCategoryId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "damage+ailment",
                        "name" to "Damage + Ailment",
                        "description" to "Damage plus a status ailment",
                        "sortingOrder" to 10,
                    ),
                ).post("/catalog/move-categories")
                .then()
                .statusCode(200)
                .body("code", equalTo("DAMAGE+AILMENT"))
                .body("name", equalTo("Damage + Ailment"))
                .extract()
                .path<String>("id")
                .toUuid()

        val ailmentMoveCategoryId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "ailment",
                        "name" to "Ailment",
                        "description" to "Pure status ailments",
                        "sortingOrder" to 20,
                    ),
                ).post("/catalog/move-categories")
                .then()
                .statusCode(200)
                .body("code", equalTo("AILMENT"))
                .body("name", equalTo("Ailment"))
                .extract()
                .path<String>("id")
                .toUuid()

        val burnMoveAilmentId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "burn",
                        "name" to "Burn",
                        "description" to "Inflicts burn",
                        "sortingOrder" to 10,
                    ),
                ).post("/catalog/move-ailments")
                .then()
                .statusCode(200)
                .body("code", equalTo("BURN"))
                .body("name", equalTo("Burn"))
                .extract()
                .path<String>("id")
                .toUuid()

        val paralysisMoveAilmentId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "paralysis",
                        "name" to "Paralysis",
                        "description" to "Inflicts paralysis",
                        "sortingOrder" to 20,
                    ),
                ).post("/catalog/move-ailments")
                .then()
                .statusCode(200)
                .body("code", equalTo("PARALYSIS"))
                .body("name", equalTo("Paralysis"))
                .extract()
                .path<String>("id")
                .toUuid()

        val selectedPokemonTargetId =
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

        val emberMoveId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "ember",
                        "name" to "Ember",
                        "typeDefinitionId" to fireTypeId,
                        "categoryCode" to "special",
                        "moveCategoryId" to damageAilmentMoveCategoryId,
                        "moveAilmentId" to burnMoveAilmentId,
                        "moveTargetId" to selectedPokemonTargetId,
                        "description" to "A small flame attack",
                        "effectChance" to 10,
                        "power" to 40,
                        "accuracy" to 100,
                        "powerPoints" to 25,
                        "priority" to 0,
                        "text" to "Ember text",
                        "shortEffect" to "May burn the target.",
                        "effect" to "A small flame attack with a burn chance.",
                        "sortingOrder" to 10,
                    ),
                ).post("/catalog/moves")
                .then()
                .statusCode(200)
                .body("code", equalTo("EMBER"))
                .body("type.code", equalTo(fireCode))
                .body("categoryCode", equalTo("SPECIAL"))
                .body("moveCategory.code", equalTo("DAMAGE+AILMENT"))
                .body("moveAilment.code", equalTo("BURN"))
                .body("moveTarget.code", equalTo("SELECTED-POKEMON"))
                .body("effectChance", equalTo(10))
                .body("power", equalTo(40))
                .extract()
                .path<String>("id")
                .toUuid()

        val growlMoveId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "growl",
                        "name" to "Growl",
                        "typeDefinitionId" to waterTypeId,
                        "categoryCode" to "status",
                        "moveCategoryId" to ailmentMoveCategoryId,
                        "moveAilmentId" to null,
                        "moveTargetId" to selectedPokemonTargetId,
                        "description" to "Lowers the target attack",
                        "effectChance" to 100,
                        "powerPoints" to 40,
                        "priority" to 0,
                        "text" to "Growl text",
                        "shortEffect" to "Lowers the target's attack.",
                        "effect" to "Lowers the target attack.",
                        "sortingOrder" to 20,
                    ),
                ).post("/catalog/moves")
                .then()
                .statusCode(200)
                .body("code", equalTo("GROWL"))
                .body("type.code", equalTo(waterCode))
                .body("categoryCode", equalTo("STATUS"))
                .body("moveCategory.code", equalTo("AILMENT"))
                .body("moveAilment", nullValue())
                .body("moveTarget.code", equalTo("SELECTED-POKEMON"))
                .body("effectChance", equalTo(100))
                .body("power", nullValue())
                .extract()
                .path<String>("id")
                .toUuid()

        given()
            .`when`()
            .get("/catalog/moves/$emberMoveId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Ember"))
            .body("accuracy", equalTo(100))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "ember",
                    "name" to "Ember Plus",
                    "typeDefinitionId" to fireTypeId,
                    "categoryCode" to "special",
                    "moveCategoryId" to ailmentMoveCategoryId,
                    "moveAilmentId" to paralysisMoveAilmentId,
                    "moveTargetId" to selectedPokemonTargetId,
                    "description" to "Updated flame attack",
                    "effectChance" to 15,
                    "power" to 50,
                    "accuracy" to 95,
                    "powerPoints" to 20,
                    "priority" to 1,
                    "text" to "Ember plus text",
                    "shortEffect" to "Has a stronger burn chance.",
                    "effect" to "Updated flame attack with a higher burn chance.",
                    "sortingOrder" to 5,
                    "enabled" to false,
                ),
            ).put("/catalog/moves/$emberMoveId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Ember Plus"))
            .body("moveCategory.code", equalTo("AILMENT"))
            .body("moveAilment.code", equalTo("PARALYSIS"))
            .body("moveTarget.code", equalTo("SELECTED-POKEMON"))
            .body("effectChance", equalTo(15))
            .body("power", equalTo(50))
            .body("accuracy", equalTo(95))
            .body("priority", equalTo(1))
            .body("enabled", equalTo(false))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "broken-move",
                    "name" to "Broken Move",
                    "typeDefinitionId" to fireTypeId,
                    "categoryCode" to "unknown",
                    "powerPoints" to 10,
                ),
            ).post("/catalog/moves")
            .then()
            .statusCode(400)

        given()
            .`when`()
            .get("/catalog/moves")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].code", equalTo("EMBER"))

        val mediumFastGrowthRateId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "medium-fast",
                        "name" to "Medium Fast",
                        "formulaCode" to "medium_fast",
                        "description" to "Standard balanced growth",
                        "sortingOrder" to 10,
                    ),
                ).post("/catalog/growth-rates")
                .then()
                .statusCode(200)
                .body("code", equalTo("MEDIUM-FAST"))
                .body("formulaCode", equalTo("MEDIUM_FAST"))
                .extract()
                .path<String>("id")
                .toUuid()

        val slowGrowthRateId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "slow",
                        "name" to "Slow",
                        "formulaCode" to "slow",
                        "sortingOrder" to 20,
                    ),
                ).post("/catalog/growth-rates")
                .then()
                .statusCode(200)
                .body("code", equalTo("SLOW"))
                .body("formulaCode", equalTo("SLOW"))
                .extract()
                .path<String>("id")
                .toUuid()

        given()
            .`when`()
            .get("/catalog/growth-rates/$mediumFastGrowthRateId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Medium Fast"))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "medium-fast",
                    "name" to "Medium Fast Revised",
                    "formulaCode" to "medium_slow",
                    "description" to "Adjusted growth reference",
                    "sortingOrder" to 5,
                    "enabled" to false,
                ),
            ).put("/catalog/growth-rates/$mediumFastGrowthRateId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Medium Fast Revised"))
            .body("formulaCode", equalTo("MEDIUM_SLOW"))
            .body("enabled", equalTo(false))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "broken-growth",
                    "name" to "Broken Growth",
                    "formulaCode" to "unknown",
                ),
            ).post("/catalog/growth-rates")
            .then()
            .statusCode(400)

        given()
            .`when`()
            .get("/catalog/growth-rates")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].code", equalTo("MEDIUM-FAST"))

        val flareCubSpeciesId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "flare-cub",
                        "dexNumber" to 4,
                        "name" to "Flare Cub",
                        "description" to "A young fire creature",
                        "primaryTypeId" to fireTypeId,
                        "growthRateId" to mediumFastGrowthRateId,
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
                ).post("/catalog/species")
                .then()
                .statusCode(200)
                .body("code", equalTo("FLARE-CUB"))
                .body("dexNumber", equalTo(4))
                .body("primaryType.code", equalTo(fireCode))
                .body("secondaryType", nullValue())
                .body("growthRate.code", equalTo("MEDIUM-FAST"))
                .body("baseStats.hp", equalTo(39))
                .extract()
                .path<String>("id")
                .toUuid()

        val steamDrakeSpeciesId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "code" to "steam-drake",
                        "dexNumber" to 5,
                        "name" to "Steam Drake",
                        "primaryTypeId" to fireTypeId,
                        "secondaryTypeId" to waterTypeId,
                        "growthRateId" to slowGrowthRateId,
                        "baseStats" to
                                mapOf(
                                    "hp" to 58,
                                    "attack" to 64,
                                    "defense" to 58,
                                    "specialAttack" to 80,
                                    "specialDefense" to 65,
                                    "speed" to 80,
                                ),
                        "sortingOrder" to 20,
                    ),
                ).post("/catalog/species")
                .then()
                .statusCode(200)
                .body("code", equalTo("STEAM-DRAKE"))
                .body("primaryType.code", equalTo(fireCode))
                .body("secondaryType.code", equalTo(waterCode))
                .body("growthRate.code", equalTo("SLOW"))
                .extract()
                .path<String>("id")
                .toUuid()

        given()
            .`when`()
            .get("/catalog/species/$flareCubSpeciesId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Flare Cub"))
            .body("growthRate.code", equalTo("MEDIUM-FAST"))
            .body("baseStats.speed", equalTo(65))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "flare-cub",
                    "dexNumber" to 4,
                    "name" to "Flare Cub Prime",
                    "description" to "Updated fire creature",
                    "primaryTypeId" to fireTypeId,
                    "secondaryTypeId" to waterTypeId,
                    "growthRateId" to slowGrowthRateId,
                    "baseStats" to
                            mapOf(
                                "hp" to 45,
                                "attack" to 60,
                                "defense" to 50,
                                "specialAttack" to 70,
                                "specialDefense" to 55,
                                "speed" to 70,
                            ),
                    "sortingOrder" to 5,
                    "enabled" to false,
                ),
            ).put("/catalog/species/$flareCubSpeciesId")
            .then()
            .statusCode(200)
            .body("name", equalTo("Flare Cub Prime"))
            .body("secondaryType.code", equalTo(waterCode))
            .body("growthRate.code", equalTo("SLOW"))
            .body("baseStats.hp", equalTo(45))
            .body("enabled", equalTo(false))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "code" to "broken-species",
                    "dexNumber" to 6,
                    "name" to "Broken Species",
                    "primaryTypeId" to fireTypeId,
                    "secondaryTypeId" to fireTypeId,
                    "growthRateId" to mediumFastGrowthRateId,
                    "baseStats" to
                            mapOf(
                                "hp" to 10,
                                "attack" to 10,
                                "defense" to 10,
                                "specialAttack" to 10,
                                "specialDefense" to 10,
                                "speed" to 10,
                            ),
                ),
            ).post("/catalog/species")
            .then()
            .statusCode(400)

        given()
            .`when`()
            .get("/catalog/species")
            .then()
            .statusCode(200)
            .body("items.size()", equalTo(2))
            .body("items[0].code", equalTo("FLARE-CUB"))
            .body("page", equalTo(1))
            .body("size", equalTo(20))
            .body("totalItems", equalTo(2))
            .body("totalPages", equalTo(1))
            .body("hasNext", equalTo(false))

        given()
            .`when`()
            .get("/catalog/species?page=2&size=1")
            .then()
            .statusCode(200)
            .body("items.size()", equalTo(1))
            .body("items[0].code", equalTo("STEAM-DRAKE"))
            .body("page", equalTo(2))
            .body("size", equalTo(1))
            .body("totalItems", equalTo(2))
            .body("totalPages", equalTo(2))
            .body("hasNext", equalTo(false))

        val flareEvolutionId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "fromSpeciesId" to flareCubSpeciesId,
                        "toSpeciesId" to steamDrakeSpeciesId,
                        "triggerCode" to "level",
                        "minLevel" to 16,
                        "description" to "Evolves after sustained training",
                        "sortingOrder" to 10,
                    ),
                ).post("/catalog/species-evolutions")
                .then()
                .statusCode(200)
                .body("fromSpecies.code", equalTo("FLARE-CUB"))
                .body("toSpecies.code", equalTo("STEAM-DRAKE"))
                .body("triggerCode", equalTo("LEVEL"))
                .body("minLevel", equalTo(16))
                .extract()
                .path<String>("id")
                .toUuid()

        given()
            .`when`()
            .get("/catalog/species-evolutions/$flareEvolutionId")
            .then()
            .statusCode(200)
            .body("description", equalTo("Evolves after sustained training"))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "fromSpeciesId" to flareCubSpeciesId,
                    "toSpeciesId" to steamDrakeSpeciesId,
                    "triggerCode" to "level",
                    "minLevel" to 18,
                    "description" to "Requires more battle experience",
                    "sortingOrder" to 5,
                    "enabled" to false,
                ),
            ).put("/catalog/species-evolutions/$flareEvolutionId")
            .then()
            .statusCode(200)
            .body("minLevel", equalTo(18))
            .body("description", equalTo("Requires more battle experience"))
            .body("enabled", equalTo(false))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "fromSpeciesId" to flareCubSpeciesId,
                    "toSpeciesId" to steamDrakeSpeciesId,
                    "triggerCode" to "level",
                ),
            ).post("/catalog/species-evolutions")
            .then()
            .statusCode(400)

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "fromSpeciesId" to flareCubSpeciesId,
                    "toSpeciesId" to flareCubSpeciesId,
                    "triggerCode" to "other",
                ),
            ).post("/catalog/species-evolutions")
            .then()
            .statusCode(400)

        given()
            .`when`()
            .get("/catalog/species-evolutions")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].toSpecies.code", equalTo("STEAM-DRAKE"))

        val flarePrimaryAbilityId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "speciesId" to flareCubSpeciesId,
                        "abilityId" to blazeAbilityId,
                        "slotCode" to "primary",
                        "sortingOrder" to 10,
                    ),
                ).post("/catalog/species-abilities")
                .then()
                .statusCode(200)
                .body("species.code", equalTo("FLARE-CUB"))
                .body("ability.code", equalTo("BLAZE"))
                .body("slotCode", equalTo("PRIMARY"))
                .extract()
                .path<String>("id")
                .toUuid()

        val flareHiddenAbilityId =
            given()
                .contentType(JSON)
                .body(
                    mapOf(
                        "speciesId" to flareCubSpeciesId,
                        "abilityId" to torrentAbilityId,
                        "slotCode" to "hidden",
                        "sortingOrder" to 20,
                    ),
                ).post("/catalog/species-abilities")
                .then()
                .statusCode(200)
                .body("slotCode", equalTo("HIDDEN"))
                .extract()
                .path<String>("id")
                .toUuid()

        given()
            .`when`()
            .get("/catalog/species-abilities/$flarePrimaryAbilityId")
            .then()
            .statusCode(200)
            .body("ability.name", equalTo("Blaze Revised"))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "speciesId" to flareCubSpeciesId,
                    "abilityId" to torrentAbilityId,
                    "slotCode" to "secondary",
                    "sortingOrder" to 15,
                    "enabled" to false,
                ),
            ).put("/catalog/species-abilities/$flareHiddenAbilityId")
            .then()
            .statusCode(200)
            .body("slotCode", equalTo("SECONDARY"))
            .body("enabled", equalTo(false))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "speciesId" to flareCubSpeciesId,
                    "abilityId" to torrentAbilityId,
                    "slotCode" to "primary",
                ),
            ).post("/catalog/species-abilities")
            .then()
            .statusCode(409)
            .body("type", equalTo("urn:avalon:problem:catalog:conflict"))
            .body("title", equalTo("Conflict"))
            .body("status", equalTo(409))
            .body("code", equalTo("catalog_conflict"))
            .body("detail", equalTo("The submitted catalog data conflicts with an existing record."))

        given()
            .contentType(JSON)
            .body(
                mapOf(
                    "speciesId" to flareCubSpeciesId,
                    "abilityId" to blazeAbilityId,
                    "slotCode" to "unknown",
                ),
            ).post("/catalog/species-abilities")
            .then()
            .statusCode(400)

        given()
            .`when`()
            .get("/catalog/species-abilities")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].species.code", equalTo("FLARE-CUB"))

        given()
            .delete("/catalog/types/$fireTypeId")
            .then()
            .statusCode(409)

        given()
            .delete("/catalog/species/$flareCubSpeciesId")
            .then()
            .statusCode(409)

        given()
            .delete("/catalog/abilities/$blazeAbilityId")
            .then()
            .statusCode(409)

        given()
            .delete("/catalog/natures/$adamantNatureId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/natures/$hardyNatureId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/items/$potionItemId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/items/$keyItemId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/moves/$emberMoveId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/moves/$growlMoveId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/move-targets/$selectedPokemonTargetId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/move-categories/$damageAilmentMoveCategoryId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/move-categories/$ailmentMoveCategoryId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/move-ailments/$burnMoveAilmentId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/move-ailments/$paralysisMoveAilmentId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/species-abilities/$flarePrimaryAbilityId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/species-abilities/$flareHiddenAbilityId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/abilities/$blazeAbilityId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/abilities/$torrentAbilityId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/species-evolutions/$flareEvolutionId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/species/$flareCubSpeciesId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/species/$steamDrakeSpeciesId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/growth-rates/$mediumFastGrowthRateId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/growth-rates/$slowGrowthRateId")
            .then()
            .statusCode(204)

        clearTypeChartEntries(fireTypeId, waterTypeId)

        given()
            .delete("/catalog/types/$fireTypeId")
            .then()
            .statusCode(204)

        given()
            .delete("/catalog/types/$waterTypeId")
            .then()
            .statusCode(204)
    }

    private fun clearTypeChartEntries(vararg typeIds: java.util.UUID) {
        runBlocking {
            val quotedIds = typeIds.joinToString(",") { "'$it'" }
            pool.query("DELETE FROM catalog.type_effectiveness WHERE attacking_type_id IN ($quotedIds) OR defending_type_id IN ($quotedIds)")
                .execute()
                .awaitSuspending()
        }
    }
}
