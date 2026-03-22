package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.MoveLearnMethod
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 招式学习方式(MoveLearnMethod)数据访问层
 *
 * 提供基础的 CRUD 操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
interface MoveLearnMethodRepository : KRepository<MoveLearnMethod, Long>
