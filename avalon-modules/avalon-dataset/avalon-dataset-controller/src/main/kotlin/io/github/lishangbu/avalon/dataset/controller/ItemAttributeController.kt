package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemAttributeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemAttributeInput
import io.github.lishangbu.avalon.dataset.repository.ItemAttributeRepository
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

/** 道具属性控制器 */
@RestController
@RequestMapping("/item-attribute")
class ItemAttributeController(
    private val itemAttributeRepository: ItemAttributeRepository,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveItemAttributeInput,
    ): ItemAttributeView = ItemAttributeView(itemAttributeRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateItemAttributeInput,
    ): ItemAttributeView = ItemAttributeView(itemAttributeRepository.save(command.toEntity(), SaveMode.UPSERT))

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        itemAttributeRepository.deleteById(id)
    }

    @GetMapping("/list")
    fun listItemAttributes(
        @ModelAttribute specification: ItemAttributeSpecification,
    ): List<ItemAttributeView> = itemAttributeRepository.listViews(specification)
}
