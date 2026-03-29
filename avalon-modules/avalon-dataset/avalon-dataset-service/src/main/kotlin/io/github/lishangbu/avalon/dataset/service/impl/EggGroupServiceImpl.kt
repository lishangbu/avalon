package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEggGroupInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEggGroupInput
import io.github.lishangbu.avalon.dataset.repository.EggGroupRepository
import io.github.lishangbu.avalon.dataset.service.EggGroupService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 蛋组服务实现 */
@Service
class EggGroupServiceImpl(
    private val eggGroupRepository: EggGroupRepository,
) : EggGroupService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveEggGroupInput): EggGroupView = EggGroupView(eggGroupRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateEggGroupInput): EggGroupView = EggGroupView(eggGroupRepository.save(command.toEntity(), SaveMode.UPSERT))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        eggGroupRepository.deleteById(id)
    }

    override fun listByCondition(specification: EggGroupSpecification): List<EggGroupView> = eggGroupRepository.findAll(specification).map(::EggGroupView)
}
