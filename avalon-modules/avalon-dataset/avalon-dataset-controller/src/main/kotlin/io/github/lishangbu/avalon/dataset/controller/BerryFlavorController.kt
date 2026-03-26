package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryFlavorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryFlavorInput
import io.github.lishangbu.avalon.dataset.service.BerryFlavorService
import org.springframework.web.bind.annotation.*

/** 树果风味控制器*/
@RestController
@RequestMapping("/berry-flavor")
class BerryFlavorController(
    /** 树果风味服务 */
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
        @RequestBody command: SaveBerryFlavorInput,
    ): BerryFlavorView = berryFlavorService.save(command)

    /** 更新树果风味 */
    @PutMapping
    fun update(
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
