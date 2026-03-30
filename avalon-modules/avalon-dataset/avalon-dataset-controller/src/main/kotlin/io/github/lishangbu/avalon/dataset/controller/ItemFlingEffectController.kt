package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.ItemFlingEffectSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemFlingEffectView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemFlingEffectInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemFlingEffectInput
import io.github.lishangbu.avalon.dataset.service.ItemFlingEffectService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 道具投掷效果控制器 */
@RestController
@RequestMapping("/item-fling-effect")
class ItemFlingEffectController(
    private val itemFlingEffectService: ItemFlingEffectService,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveItemFlingEffectInput,
    ): ItemFlingEffectView = itemFlingEffectService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateItemFlingEffectInput,
    ): ItemFlingEffectView = itemFlingEffectService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        itemFlingEffectService.removeById(id)
    }

    @GetMapping("/list")
    fun listItemFlingEffects(
        @ModelAttribute specification: ItemFlingEffectSpecification,
    ): List<ItemFlingEffectView> = itemFlingEffectService.listByCondition(specification)
}
