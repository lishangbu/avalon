package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.PropOverride

@Entity
interface CreatureVariantType {
    /** ID */
    @Id
    @PropOverride(prop = "creatureVariantId", columnName = "pokemon_form_id")
    @PropOverride(prop = "typeId", columnName = "type_id")
    val id: CreatureVariantTypeId

    /** 槽位 */
    val slot: Int?
}
