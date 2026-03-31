package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.LocationAreaEncounterConditionValue
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/** 地点区域遭遇条件值仓储接口 */
@Repository
interface LocationAreaEncounterConditionValueRepository : KRepository<LocationAreaEncounterConditionValue, Long> {
    /** 判断指定遭遇条件值是否已被地点区域遭遇引用 */
    fun existsByEncounterConditionValueId(encounterConditionValueId: Long): Boolean = findAll().any { it.id.encounterConditionValueId == encounterConditionValueId }
}
