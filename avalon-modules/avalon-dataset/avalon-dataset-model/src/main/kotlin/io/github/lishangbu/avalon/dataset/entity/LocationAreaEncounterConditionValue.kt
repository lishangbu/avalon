package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.PropOverride

@Entity
interface LocationAreaEncounterConditionValue {
    /** ID */
    @Id
    @PropOverride(prop = "locationAreaEncounterId", columnName = "location_area_encounter_id")
    @PropOverride(prop = "encounterConditionValueId", columnName = "encounter_condition_value_id")
    val id: LocationAreaEncounterConditionValueId
}
