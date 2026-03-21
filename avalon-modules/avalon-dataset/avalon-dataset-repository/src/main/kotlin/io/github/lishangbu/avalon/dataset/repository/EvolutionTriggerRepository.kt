package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.EvolutionTrigger
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 进化触发器(EvolutionTrigger)数据访问层
 *
 * 提供基础的 CRUD 操作
 *
 * @author lishangbu
 * @since 2026/2/12
 */
@Repository
interface EvolutionTriggerRepository :
    KRepository<EvolutionTrigger, Long>
