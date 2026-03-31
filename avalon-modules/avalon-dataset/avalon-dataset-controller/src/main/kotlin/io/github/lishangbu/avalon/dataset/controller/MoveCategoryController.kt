package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.MoveCategorySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveCategoryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveCategoryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveCategoryInput
import io.github.lishangbu.avalon.dataset.repository.MoveCategoryRepository
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

/** 招式类别控制器 */
@RestController
@RequestMapping("/move-category")
class MoveCategoryController(
    private val moveCategoryRepository: MoveCategoryRepository,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveMoveCategoryInput,
    ): MoveCategoryView = MoveCategoryView(moveCategoryRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateMoveCategoryInput,
    ): MoveCategoryView = MoveCategoryView(moveCategoryRepository.save(command.toEntity(), SaveMode.UPSERT))

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        moveCategoryRepository.deleteById(id)
    }

    @GetMapping("/list")
    fun listMoveCategories(
        @ModelAttribute specification: MoveCategorySpecification,
    ): List<MoveCategoryView> = moveCategoryRepository.listViews(specification)
}
