package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemAttributeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemAttributeInput
import io.github.lishangbu.avalon.dataset.service.ItemAttributeService
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
    private val itemAttributeService: ItemAttributeService,
) {
    @PostMapping
    fun save(
        @RequestBody command: SaveItemAttributeInput,
    ): ItemAttributeView = itemAttributeService.save(command)

    @PutMapping
    fun update(
        @RequestBody command: UpdateItemAttributeInput,
    ): ItemAttributeView = itemAttributeService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        itemAttributeService.removeById(id)
    }

    @GetMapping("/list")
    fun listItemAttributes(
        @ModelAttribute specification: ItemAttributeSpecification,
    ): List<ItemAttributeView> = itemAttributeService.listByCondition(specification)
}
