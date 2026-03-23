package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.PropOverride
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "pokemon_type")
interface PokemonType {
    /** ID */
    @Id
    @PropOverride(prop = "pokemonId", columnName = "pokemon_id")
    @PropOverride(prop = "typeId", columnName = "type_id")
    val id: PokemonTypeId

    /** 槽位 */
    val slot: Int?
}
