package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.CreatureElement
import io.github.lishangbu.avalon.dataset.entity.CreatureElementId
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 生物属性仓储接口
 *
 * 定义宝可梦属性数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
interface CreatureElementRepository : KRepository<CreatureElement, CreatureElementId>
