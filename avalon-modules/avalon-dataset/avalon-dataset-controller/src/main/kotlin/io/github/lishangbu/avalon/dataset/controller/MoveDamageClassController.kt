package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.service.MoveDamageClassService
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

/** 招式伤害类别控制器。 */
@RestController
@RequestMapping("/move-damage-class")
class MoveDamageClassController(
    private val moveDamageClassService: MoveDamageClassService,
) {
    @GetMapping("/page")
    fun getMoveDamageClassPage(
        pageable: Pageable,
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) internalName: String?,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) description: String?,
    ): Page<MoveDamageClass> =
        moveDamageClassService.getPageByCondition(
            MoveDamageClass {
                id?.let { this.id = it }
                internalName?.let { this.internalName = it }
                name?.let { this.name = it }
                description?.let { this.description = it }
            },
            pageable,
        )

    @PostMapping
    fun save(
        @RequestBody moveDamageClass: MoveDamageClass,
    ): MoveDamageClass = moveDamageClassService.save(moveDamageClass)

    @PutMapping
    fun update(
        @RequestBody moveDamageClass: MoveDamageClass,
    ): MoveDamageClass = moveDamageClassService.update(moveDamageClass)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        moveDamageClassService.removeById(id)
    }

    @GetMapping("/list")
    fun listMoveDamageClasses(
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) internalName: String?,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) description: String?,
    ): List<MoveDamageClass> =
        moveDamageClassService.listByCondition(
            MoveDamageClass {
                id?.let { this.id = it }
                internalName?.let { this.internalName = it }
                name?.let { this.name = it }
                description?.let { this.description = it }
            },
        )
}
