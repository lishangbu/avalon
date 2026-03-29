package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.MoveSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveInput
import io.github.lishangbu.avalon.dataset.service.MoveService
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

/** 招式管理控制器 */
@RestController
@RequestMapping("/move")
class MoveController(
    private val moveService: MoveService,
) {
    /** 按筛选条件分页查询招式 */
    @GetMapping("/page")
    fun getMovePage(
        pageable: Pageable,
        @ModelAttribute specification: MoveSpecification,
    ): Page<MoveView> = moveService.getPageByCondition(specification, pageable)

    /** 创建招式 */
    @PostMapping
    fun save(
        @RequestBody command: SaveMoveInput,
    ): MoveView = moveService.save(command)

    /** 更新招式 */
    @PutMapping
    fun update(
        @RequestBody command: UpdateMoveInput,
    ): MoveView = moveService.update(command)

    /** 删除指定 ID 的招式 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        moveService.removeById(id)
    }
}
