package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.PropOverride

@Entity
interface PokemonFormType {
    /** ID */
    @Id
    @PropOverride(prop = "pokemonFormId", columnName = "pokemon_form_id")
    @PropOverride(prop = "typeId", columnName = "type_id")
    val id: PokemonFormTypeId

    /** 槽位 */
    val slot: Int?
}
