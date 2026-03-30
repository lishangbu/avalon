package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.SaveStatInput
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.StatView
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateStatInput
import io.github.lishangbu.avalon.dataset.service.StatService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

/** 能力值控制器 */
@RestController
@RequestMapping("/stat")
class StatController(
    /** 能力值服务*/
    private val statService: StatService,
) {
    /** 保存能力值*/
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveStatInput,
    ): StatView = statService.save(command)

    /** 更新能力值*/
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateStatInput,
    ): StatView = statService.update(command)

    /** 按 ID 删除能力值*/
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        statService.removeById(id)
    }

    /** 查询能力值列表*/
    @GetMapping("/list")
    fun listStats(
        @ModelAttribute specification: StatSpecification,
    ): List<StatView> = statService.listByCondition(specification)
}
