package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.service.TypeService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 属性控制器。 */
@RestController
@RequestMapping("/type")
class TypeController(
    private val typeService: TypeService,
) {
    @GetMapping("/page")
    fun getTypePage(
        pageable: Pageable,
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) internalName: String?,
        @RequestParam(required = false) name: String?,
    ): Page<Type> =
        typeService.getPageByCondition(
            Type {
                id?.let { this.id = it }
                internalName?.let { this.internalName = it }
                name?.let { this.name = it }
            },
            pageable,
        )

    @PostMapping
    fun save(
        @RequestBody type: Type,
    ): Type = typeService.save(type)

    @PutMapping
    fun update(
        @RequestBody type: Type,
    ): Type = typeService.update(type)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        typeService.removeById(id)
    }

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
