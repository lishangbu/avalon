package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.ItemCategorySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemCategoryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemCategoryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemCategoryInput
import io.github.lishangbu.avalon.dataset.service.ItemCategoryService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 道具类别控制器 */
@RestController
@RequestMapping("/item-category")
class ItemCategoryController(
    private val itemCategoryService: ItemCategoryService,
) {
    @PostMapping
    fun save(
        @RequestBody command: SaveItemCategoryInput,
    ): ItemCategoryView = itemCategoryService.save(command)

    @PutMapping
    fun update(
        @RequestBody command: UpdateItemCategoryInput,
    ): ItemCategoryView = itemCategoryService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        itemCategoryService.removeById(id)
    }

    @GetMapping("/list")
    fun listItemCategories(
        @ModelAttribute specification: ItemCategorySpecification,
    ): List<ItemCategoryView> = itemCategoryService.listByCondition(specification)
}
