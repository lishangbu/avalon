package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Gender
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 性别仓储接口
 *
 * 定义性别数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
interface GenderRepository : KRepository<Gender, Long>
