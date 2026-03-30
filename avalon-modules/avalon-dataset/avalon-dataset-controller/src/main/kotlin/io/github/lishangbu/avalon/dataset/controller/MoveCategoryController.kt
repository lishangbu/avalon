package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.MoveCategorySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveCategoryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveCategoryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveCategoryInput
import io.github.lishangbu.avalon.dataset.service.MoveCategoryService
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

/** 招式类别控制器 */
@RestController
@RequestMapping("/move-category")
class MoveCategoryController(
    private val moveCategoryService: MoveCategoryService,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveMoveCategoryInput,
    ): MoveCategoryView = moveCategoryService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateMoveCategoryInput,
    ): MoveCategoryView = moveCategoryService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        moveCategoryService.removeById(id)
    }

    @GetMapping("/list")
    fun listMoveCategories(
        @ModelAttribute specification: MoveCategorySpecification,
    ): List<MoveCategoryView> = moveCategoryService.listByCondition(specification)
}
