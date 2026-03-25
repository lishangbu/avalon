package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification
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
    override fun listByCondition(specification: StatSpecification): List<Stat> = statRepository.findAll(specification)

    /** 保存能力值*/
    override fun save(stat: Stat): Stat = statRepository.save(stat)

    /** 更新能力值*/
    override fun update(stat: Stat): Stat = statRepository.save(stat)

    /** 按 ID 删除能力值*/
    override fun removeById(id: Long) {
        statRepository.deleteById(id)
    }
}
