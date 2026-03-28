package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEggGroupInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEggGroupInput
import io.github.lishangbu.avalon.dataset.service.EggGroupService
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
    private val eggGroupService: EggGroupService,
) {
    @PostMapping
    fun save(
        @RequestBody command: SaveEggGroupInput,
    ): EggGroupView = eggGroupService.save(command)

    @PutMapping
    fun update(
        @RequestBody command: UpdateEggGroupInput,
    ): EggGroupView = eggGroupService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        eggGroupService.removeById(id)
    }

    @GetMapping("/list")
    fun listEggGroups(
        @ModelAttribute specification: EggGroupSpecification,
    ): List<EggGroupView> = eggGroupService.listByCondition(specification)
}
