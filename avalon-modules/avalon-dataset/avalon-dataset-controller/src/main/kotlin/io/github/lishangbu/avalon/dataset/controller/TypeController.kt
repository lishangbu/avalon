package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Type
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
        @RequestBody type: Type,
    ): Type = typeService.save(type)

    /** 更新属性 */
    @PutMapping
    fun update(
        @RequestBody type: Type,
    ): Type = typeService.update(type)

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
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) internalName: String?,
        @RequestParam(required = false) name: String?,
    ): List<Type> =
        typeService.listByCondition(
            Type {
                id?.let { this.id = it }
                internalName?.let { this.internalName = it }
                name?.let { this.name = it }
            },
        )
}
