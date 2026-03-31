package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveDamageClassInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveDamageClassInput
import io.github.lishangbu.avalon.dataset.repository.MoveDamageClassRepository
import jakarta.validation.Valid
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

/** 招式伤害分类控制器*/
@RestController
@RequestMapping("/move-damage-class")
class MoveDamageClassController(
    /** 招式伤害分类仓储 */
    private val moveDamageClassRepository: MoveDamageClassRepository,
) {
    /** 获取招式伤害分类分页结果 */
    @GetMapping("/page")
    fun getMoveDamageClassPage(
        pageable: Pageable,
        @ModelAttribute specification: MoveDamageClassSpecification,
    ): Page<MoveDamageClassView> = moveDamageClassRepository.pageViews(specification, pageable)

    /** 保存招式伤害分类 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveMoveDamageClassInput,
    ): MoveDamageClassView = moveDamageClassRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    /** 更新招式伤害分类 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateMoveDamageClassInput,
    ): MoveDamageClassView = moveDamageClassRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    /** 按 ID 删除招式伤害分类 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        moveDamageClassRepository.deleteById(id)
    }

    /** 查询招式伤害分类列表 */
    @GetMapping("/list")
    fun listMoveDamageClasses(
        @ModelAttribute specification: MoveDamageClassSpecification,
    ): List<MoveDamageClassView> = moveDamageClassRepository.listViews(specification)

    private fun reloadView(moveDamageClass: MoveDamageClass): MoveDamageClassView = requireNotNull(moveDamageClassRepository.loadViewById(moveDamageClass.id)) { "未找到 ID=${moveDamageClass.id} 对应的招式伤害分类" }
}
