package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.NatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.NatureView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveNatureInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateNatureInput
import io.github.lishangbu.avalon.dataset.service.NatureService
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
    private val natureService: NatureService,
) {
    @PostMapping
    fun save(
        @RequestBody command: SaveNatureInput,
    ): NatureView = natureService.save(command)

    @PutMapping
    fun update(
        @RequestBody command: UpdateNatureInput,
    ): NatureView = natureService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        natureService.removeById(id)
    }

    @GetMapping("/list")
    fun listNatures(
        @ModelAttribute specification: NatureSpecification,
    ): List<NatureView> = natureService.listByCondition(specification)
}
