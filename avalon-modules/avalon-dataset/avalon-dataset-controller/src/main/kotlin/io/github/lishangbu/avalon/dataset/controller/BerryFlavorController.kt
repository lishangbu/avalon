package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryFlavorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryFlavorInput
import io.github.lishangbu.avalon.dataset.service.BerryFlavorService
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

/** 树果风味控制器*/
@RestController
@RequestMapping("/berry-flavor")
class BerryFlavorController(
    /** 树果风味应用服务 */
    private val berryFlavorService: BerryFlavorService,
) {
    /** 查询树果风味列表 */
    @GetMapping("/list")
    fun listBerryFlavors(
        @ModelAttribute specification: BerryFlavorSpecification,
    ): List<BerryFlavorView> = berryFlavorService.listByCondition(specification)

    /** 保存树果风味 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveBerryFlavorInput,
    ): BerryFlavorView = berryFlavorService.save(command)

    /** 更新树果风味 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateBerryFlavorInput,
    ): BerryFlavorView = berryFlavorService.update(command)

    /** 按 ID 删除树果风味 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        berryFlavorService.removeById(id)
    }
}
