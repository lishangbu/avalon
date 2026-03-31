package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Creature
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureInput
import io.github.lishangbu.avalon.dataset.repository.CreatureRepository
import io.github.lishangbu.avalon.dataset.service.CreatureService
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 生物应用服务实现 */
@Service
class CreatureServiceImpl(
    private val creatureRepository: CreatureRepository,
) : CreatureService {
    /** 按筛选条件分页查询生物 */
    override fun getPageByCondition(
        specification: CreatureSpecification,
        pageable: Pageable,
    ): Page<CreatureView> = creatureRepository.pageViews(specification, pageable)

    /** 创建生物 */
    override fun save(command: SaveCreatureInput): CreatureView = creatureRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    /** 更新生物 */
    override fun update(command: UpdateCreatureInput): CreatureView = creatureRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY).let(::reloadView)

    /** 删除指定 ID 的生物 */
    override fun removeById(id: Long) {
        creatureRepository.deleteById(id)
    }

    private fun reloadView(creature: Creature): CreatureView = requireNotNull(creatureRepository.loadViewById(creature.id)) { "未找到 ID=${creature.id} 对应的生物" }
}
