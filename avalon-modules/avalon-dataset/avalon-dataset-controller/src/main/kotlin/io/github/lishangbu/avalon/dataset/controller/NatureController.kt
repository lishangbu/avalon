package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Nature
import io.github.lishangbu.avalon.dataset.entity.dto.NatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.NatureView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveNatureInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateNatureInput
import io.github.lishangbu.avalon.dataset.repository.NatureRepository
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

/** 性格控制器 */
@RestController
@RequestMapping("/nature")
class NatureController(
    private val natureRepository: NatureRepository,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveNatureInput,
    ): NatureView = natureRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateNatureInput,
    ): NatureView = natureRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        natureRepository.deleteById(id)
    }

    @GetMapping("/list")
    fun listNatures(
        @ModelAttribute specification: NatureSpecification,
    ): List<NatureView> = natureRepository.listViews(specification)

    private fun reloadView(nature: Nature): NatureView = requireNotNull(natureRepository.loadViewById(nature.id)) { "未找到 ID=${nature.id} 对应的性格" }
}
