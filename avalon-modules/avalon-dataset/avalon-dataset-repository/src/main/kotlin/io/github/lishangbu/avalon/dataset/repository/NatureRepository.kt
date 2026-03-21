package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Nature
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 性格(Nature)数据访问层
 *
 * 提供基础的 CRUD 操作
 *
 * @author lishangbu
 * @since 2026/2/12
 */
@Repository
interface NatureRepository :
    KRepository<Nature, Long>
