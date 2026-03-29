package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.entity.dto.SaveStatInput
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.StatView
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateStatInput
import io.github.lishangbu.avalon.dataset.repository.StatRepository
import io.github.lishangbu.avalon.dataset.service.StatService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service

/** 能力值服务实现*/
@Service
class StatServiceImpl(
    /** 能力值仓储*/
    private val statRepository: StatRepository,
) : StatService {
    /** 根据条件查询能力值列表*/
    override fun listByCondition(specification: StatSpecification): List<StatView> = statRepository.listViews(specification)

    /** 保存能力值*/
    override fun save(command: SaveStatInput): StatView = statRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    /** 更新能力值*/
    override fun update(command: UpdateStatInput): StatView {
        requireStatEditable(command.id.toLong(), "能力值已设为只读，禁止修改")
        return statRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)
    }

    /** 按 ID 删除能力值*/
    override fun removeById(id: Long) {
        requireStatEditable(id, "能力值已设为只读，禁止删除")
        statRepository.deleteById(id)
    }

    private fun reloadView(stat: Stat): StatView = requireNotNull(statRepository.loadViewById(stat.id)) { "未找到 ID=${stat.id} 对应的能力值" }

    private fun requireStatEditable(
        id: Long,
        errorMessage: String,
    ) {
        val stat = requireNotNull(statRepository.findNullable(id)) { "未找到 ID=$id 对应的能力值" }
        if (stat.readonly) {
            throw IllegalStateException(errorMessage)
        }
    }
}
