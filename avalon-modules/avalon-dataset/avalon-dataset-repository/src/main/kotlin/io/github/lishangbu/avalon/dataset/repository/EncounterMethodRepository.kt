package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.EncounterMethod
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 遭遇方法仓储接口
 *
 * 定义遭遇方法数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/2/12
 */
@Repository
interface EncounterMethodRepository : KRepository<EncounterMethod, Long>
