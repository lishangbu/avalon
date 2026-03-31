package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEggGroupInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEggGroupInput
import io.github.lishangbu.avalon.dataset.repository.EggGroupRepository
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

/** 蛋组控制器 */
@RestController
@RequestMapping("/egg-group")
class EggGroupController(
    private val eggGroupRepository: EggGroupRepository,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveEggGroupInput,
    ): EggGroupView = EggGroupView(eggGroupRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateEggGroupInput,
    ): EggGroupView = EggGroupView(eggGroupRepository.save(command.toEntity(), SaveMode.UPSERT))

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        eggGroupRepository.deleteById(id)
    }

    @GetMapping("/list")
    fun listEggGroups(
        @ModelAttribute specification: EggGroupSpecification,
    ): List<EggGroupView> = eggGroupRepository.listViews(specification)
}
