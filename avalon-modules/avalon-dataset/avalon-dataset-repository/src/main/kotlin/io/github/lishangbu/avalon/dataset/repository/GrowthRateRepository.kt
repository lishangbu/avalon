package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 成长速率仓储接口
 *
 * 定义成长速率数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/2/10
 */
@Repository
interface GrowthRateRepository : KRepository<GrowthRate, Long>
