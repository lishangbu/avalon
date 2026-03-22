package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 成长速率(GrowthRate)数据访问层
 *
 * 提供基础的 CRUD 操作
 *
 * @author lishangbu
 * @since 2026/2/10
 */
@Repository
interface GrowthRateRepository : KRepository<GrowthRate, Long>
