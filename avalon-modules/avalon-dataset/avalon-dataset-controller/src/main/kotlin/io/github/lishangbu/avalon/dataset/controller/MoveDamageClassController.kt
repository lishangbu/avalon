package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassSpecification
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
        @ModelAttribute specification: MoveDamageClassSpecification,
    ): Page<MoveDamageClass> = moveDamageClassService.getPageByCondition(specification, pageable)

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
        @ModelAttribute specification: MoveDamageClassSpecification,
    ): List<MoveDamageClass> = moveDamageClassService.listByCondition(specification)
}
