package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Nature
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 性格仓储接口
 *
 * 定义性格数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/2/12
 */
@Repository
interface NatureRepository : KRepository<Nature, Long>
