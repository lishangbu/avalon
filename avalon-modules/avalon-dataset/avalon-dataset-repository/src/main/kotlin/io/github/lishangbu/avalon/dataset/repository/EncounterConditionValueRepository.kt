package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.EncounterConditionValue
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 遭遇条件值仓储接口
 *
 * 定义遭遇条件值数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/2/12
 */
@Repository
interface EncounterConditionValueRepository : KRepository<EncounterConditionValue, Long>
