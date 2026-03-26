package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.SaveTypeInput
import io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.TypeView
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateTypeInput
import io.github.lishangbu.avalon.dataset.service.TypeService
import org.springframework.web.bind.annotation.*

/** 属性控制器 */
@RestController
@RequestMapping("/type")
class TypeController(
    /** 属性服务 */
    private val typeService: TypeService,
) {
    /** 保存属性 */
    @PostMapping
    fun save(
        @RequestBody command: SaveTypeInput,
    ): TypeView = typeService.save(command)

    /** 更新属性 */
    @PutMapping
    fun update(
        @RequestBody command: UpdateTypeInput,
    ): TypeView = typeService.update(command)

    /** 按 ID 删除属性 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        typeService.removeById(id)
    }

    /** 查询属性列表 */
    @GetMapping("/list")
    fun listTypes(
        @ModelAttribute specification: TypeSpecification,
    ): List<TypeView> = typeService.listByCondition(specification)
}
