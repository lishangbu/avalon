package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.service.MoveDamageClassService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

/** 招式伤害分类控制器*/
@RestController
@RequestMapping("/move-damage-class")
class MoveDamageClassController(
    /** 招式伤害分类服务 */
    private val moveDamageClassService: MoveDamageClassService,
) {
    /** 获取招式伤害分类分页结果 */
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

    /** 保存招式伤害分类 */
    @PostMapping
    fun save(
        @RequestBody moveDamageClass: MoveDamageClass,
    ): MoveDamageClass = moveDamageClassService.save(moveDamageClass)

    /** 更新招式伤害分类 */
    @PutMapping
    fun update(
        @RequestBody moveDamageClass: MoveDamageClass,
    ): MoveDamageClass = moveDamageClassService.update(moveDamageClass)

    /** 按 ID 删除招式伤害分类 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        moveDamageClassService.removeById(id)
    }

    /** 查询招式伤害分类列表 */
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
