package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.ItemPocketSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemPocketView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemPocketInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemPocketInput
import io.github.lishangbu.avalon.dataset.repository.ItemPocketRepository
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

/** 道具口袋控制器 */
@RestController
@RequestMapping("/item-pocket")
class ItemPocketController(
    private val itemPocketRepository: ItemPocketRepository,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveItemPocketInput,
    ): ItemPocketView = ItemPocketView(itemPocketRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateItemPocketInput,
    ): ItemPocketView = ItemPocketView(itemPocketRepository.save(command.toEntity(), SaveMode.UPSERT))

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        itemPocketRepository.deleteById(id)
    }

    @GetMapping("/list")
    fun listItemPockets(
        @ModelAttribute specification: ItemPocketSpecification,
    ): List<ItemPocketView> = itemPocketRepository.listViews(specification)
}
