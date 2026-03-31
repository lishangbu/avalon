package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpeciesSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpeciesView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureSpeciesInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureSpeciesInput
import io.github.lishangbu.avalon.dataset.service.CreatureSpeciesService
import jakarta.validation.Valid
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 生物种族管理控制器 */
@RestController
@RequestMapping("/creature-species")
class CreatureSpeciesController(
    private val creatureSpeciesService: CreatureSpeciesService,
) {
    /** 按筛选条件分页查询生物种族 */
    @GetMapping("/page")
    fun getCreatureSpeciesPage(
        pageable: Pageable,
        @ModelAttribute specification: CreatureSpeciesSpecification,
    ): Page<CreatureSpeciesView> = creatureSpeciesService.getPageByCondition(specification, pageable)

    /** 创建生物种族 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveCreatureSpeciesInput,
    ): CreatureSpeciesView = creatureSpeciesService.save(command)

    /** 更新生物种族 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateCreatureSpeciesInput,
    ): CreatureSpeciesView = creatureSpeciesService.update(command)

    /** 删除指定 ID 的生物种族 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        creatureSpeciesService.removeById(id)
    }
}
