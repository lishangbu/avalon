package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Item
import io.github.lishangbu.avalon.dataset.entity.dto.ItemSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemInput
import io.github.lishangbu.avalon.dataset.repository.ItemRepository
import jakarta.validation.Valid
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
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

/** 道具管理控制器 */
@RestController
@RequestMapping("/item")
class ItemController(
    private val itemRepository: ItemRepository,
) {
    /** 按筛选条件分页查询道具 */
    @GetMapping("/page")
    fun getItemPage(
        pageable: Pageable,
        @ModelAttribute specification: ItemSpecification,
    ): Page<ItemView> = itemRepository.pageViews(specification, pageable)

    /** 创建道具 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveItemInput,
    ): ItemView = itemRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    /** 更新道具 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateItemInput,
    ): ItemView = itemRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    /** 删除指定 ID 的道具 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        itemRepository.deleteById(id)
    }

    private fun reloadView(item: Item): ItemView = requireNotNull(itemRepository.loadViewById(item.id)) { "未找到 ID=${item.id} 对应的道具" }
}
