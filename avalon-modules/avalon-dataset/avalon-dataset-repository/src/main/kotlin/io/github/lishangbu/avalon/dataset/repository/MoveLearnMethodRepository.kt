package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.MoveLearnMethod
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 招式学习方式仓储接口
 *
 * 定义招式学习方式数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
interface MoveLearnMethodRepository : KRepository<MoveLearnMethod, Long>
