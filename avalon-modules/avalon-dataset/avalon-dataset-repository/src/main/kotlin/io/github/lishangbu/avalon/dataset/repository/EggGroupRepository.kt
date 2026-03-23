package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.EggGroup
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 蛋组仓储接口
 *
 * 定义蛋组数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
interface EggGroupRepository : KRepository<EggGroup, Long>
