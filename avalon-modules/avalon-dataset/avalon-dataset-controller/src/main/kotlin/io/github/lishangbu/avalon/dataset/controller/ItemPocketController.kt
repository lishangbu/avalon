package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.ItemPocketSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemPocketView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemPocketInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemPocketInput
import io.github.lishangbu.avalon.dataset.service.ItemPocketService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 道具口袋控制器 */
@RestController
@RequestMapping("/item-pocket")
class ItemPocketController(
    private val itemPocketService: ItemPocketService,
) {
    @PostMapping
    fun save(
        @RequestBody command: SaveItemPocketInput,
    ): ItemPocketView = itemPocketService.save(command)

    @PutMapping
    fun update(
        @RequestBody command: UpdateItemPocketInput,
    ): ItemPocketView = itemPocketService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        itemPocketService.removeById(id)
    }

    @GetMapping("/list")
    fun listItemPockets(
        @ModelAttribute specification: ItemPocketSpecification,
    ): List<ItemPocketView> = itemPocketService.listByCondition(specification)
}
