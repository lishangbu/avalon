package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.entity.dto.SaveStatInput
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.StatView
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateStatInput
import io.github.lishangbu.avalon.dataset.repository.StatRepository
import jakarta.validation.Valid
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.web.bind.annotation.*

/** 能力值控制器 */
@RestController
@RequestMapping("/stat")
class StatController(
    /** 能力值仓储*/
    private val statRepository: StatRepository,
) {
    /** 保存能力值*/
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveStatInput,
    ): StatView = statRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    /** 更新能力值*/
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateStatInput,
    ): StatView {
        requireStatEditable(command.id.toLong(), "能力值已设为只读，禁止修改")
        return statRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)
    }

    /** 按 ID 删除能力值*/
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        requireStatEditable(id, "能力值已设为只读，禁止删除")
        statRepository.deleteById(id)
    }

    /** 查询能力值列表*/
    @GetMapping("/list")
    fun listStats(
        @ModelAttribute specification: StatSpecification,
    ): List<StatView> = statRepository.listViews(specification)

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
