package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.MoveTargetSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveTargetView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveTargetInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveTargetInput
import io.github.lishangbu.avalon.dataset.repository.MoveTargetRepository
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

/** 招式目标控制器 */
@RestController
@RequestMapping("/move-target")
class MoveTargetController(
    private val moveTargetRepository: MoveTargetRepository,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveMoveTargetInput,
    ): MoveTargetView = MoveTargetView(moveTargetRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateMoveTargetInput,
    ): MoveTargetView = MoveTargetView(moveTargetRepository.save(command.toEntity(), SaveMode.UPSERT))

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        moveTargetRepository.deleteById(id)
    }

    @GetMapping("/list")
    fun listMoveTargets(
        @ModelAttribute specification: MoveTargetSpecification,
    ): List<MoveTargetView> = moveTargetRepository.listViews(specification)
}
