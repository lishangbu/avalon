package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.entity.dto.SaveStatInput
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.StatView
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateStatInput
import io.github.lishangbu.avalon.dataset.repository.StatRepository
import io.github.lishangbu.avalon.dataset.service.StatService
import org.springframework.stereotype.Service

/** 能力值服务实现*/
@Service
class StatServiceImpl(
    /** 能力值仓储*/
    private val statRepository: StatRepository,
) : StatService {
    /** 根据条件查询能力值列表*/
    override fun listByCondition(specification: StatSpecification): List<StatView> = statRepository.findAll(specification)

    /** 保存能力值*/
    override fun save(command: SaveStatInput): StatView = statRepository.save(command.toEntity()).let(::reloadView)

    /** 更新能力值*/
    override fun update(command: UpdateStatInput): StatView = statRepository.save(command.toEntity()).let(::reloadView)

    /** 按 ID 删除能力值*/
    override fun removeById(id: Long) {
        statRepository.deleteById(id)
    }

    private fun reloadView(stat: Stat): StatView = requireNotNull(statRepository.findById(stat.id)) { "未找到 ID=${stat.id} 对应的能力值" }
}
