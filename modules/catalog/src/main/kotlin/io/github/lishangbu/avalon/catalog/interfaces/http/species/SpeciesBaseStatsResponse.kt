package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.domain.SpeciesBaseStats

/**
 * 物种基础种族值响应。
 *
 * @property hp 基础生命值。
 * @property attack 基础攻击。
 * @property defense 基础防御。
 * @property specialAttack 基础特攻。
 * @property specialDefense 基础特防。
 * @property speed 基础速度。
 */
data class SpeciesBaseStatsResponse(
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val specialAttack: Int,
    val specialDefense: Int,
    val speed: Int,
)

internal fun SpeciesBaseStats.toResponse(): SpeciesBaseStatsResponse =
    SpeciesBaseStatsResponse(
        hp = hp,
        attack = attack,
        defense = defense,
        specialAttack = specialAttack,
        specialDefense = specialDefense,
        speed = speed,
    )