package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Move
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 招式仓储接口
 *
 * 定义招式数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
interface MoveRepository : KRepository<Move, Long>
