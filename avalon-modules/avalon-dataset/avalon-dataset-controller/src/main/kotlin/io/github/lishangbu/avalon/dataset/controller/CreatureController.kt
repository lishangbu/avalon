package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureInput
import io.github.lishangbu.avalon.dataset.service.CreatureService
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

/** 生物管理控制器 */
@RestController
@RequestMapping("/creatures")
class CreatureController(
    private val creatureService: CreatureService,
) {
    /** 按筛选条件分页查询生物 */
    @GetMapping("/page")
    fun getCreaturePage(
        pageable: Pageable,
        @ModelAttribute specification: CreatureSpecification,
    ): Page<CreatureView> = creatureService.getPageByCondition(specification, pageable)

    /** 创建生物 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveCreatureInput,
    ): CreatureView = creatureService.save(command)

    /** 更新生物 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateCreatureInput,
    ): CreatureView = creatureService.update(command)

    /** 删除指定 ID 的生物 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        creatureService.removeById(id)
    }
}
