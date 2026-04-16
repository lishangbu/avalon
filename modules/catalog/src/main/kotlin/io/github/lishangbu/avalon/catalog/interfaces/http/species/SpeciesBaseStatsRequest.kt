package io.github.lishangbu.avalon.catalog.interfaces.http.species

import io.github.lishangbu.avalon.catalog.domain.SpeciesBaseStats
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * 物种基础种族值请求。
 *
 * @property hp 基础生命值。
 * @property attack 基础攻击。
 * @property defense 基础防御。
 * @property specialAttack 基础特攻。
 * @property specialDefense 基础特防。
 * @property speed 基础速度。
 */
data class SpeciesBaseStatsRequest(
    @field:Min(1)
    @field:Max(255)
    val hp: Int,
    @field:Min(1)
    @field:Max(255)
    val attack: Int,
    @field:Min(1)
    @field:Max(255)
    val defense: Int,
    @field:Min(1)
    @field:Max(255)
    val specialAttack: Int,
    @field:Min(1)
    @field:Max(255)
    val specialDefense: Int,
    @field:Min(1)
    @field:Max(255)
    val speed: Int,
)

internal fun SpeciesBaseStatsRequest.toDomain(): SpeciesBaseStats =
    SpeciesBaseStats(
        hp = hp,
        attack = attack,
        defense = defense,
        specialAttack = specialAttack,
        specialDefense = specialDefense,
        speed = speed,
    )