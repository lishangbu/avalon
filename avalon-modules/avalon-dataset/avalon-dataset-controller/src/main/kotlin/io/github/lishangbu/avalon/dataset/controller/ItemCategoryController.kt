package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.ItemCategorySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemCategoryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemCategoryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemCategoryInput
import io.github.lishangbu.avalon.dataset.repository.ItemCategoryRepository
import jakarta.validation.Valid
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
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
    private val itemCategoryRepository: ItemCategoryRepository,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveItemCategoryInput,
    ): ItemCategoryView = ItemCategoryView(itemCategoryRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateItemCategoryInput,
    ): ItemCategoryView = ItemCategoryView(itemCategoryRepository.save(command.toEntity(), SaveMode.UPSERT))

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        itemCategoryRepository.deleteById(id)
    }

    @GetMapping("/list")
    fun listItemCategories(
        @ModelAttribute specification: ItemCategorySpecification,
    ): List<ItemCategoryView> = itemCategoryRepository.listViews(specification)
}
