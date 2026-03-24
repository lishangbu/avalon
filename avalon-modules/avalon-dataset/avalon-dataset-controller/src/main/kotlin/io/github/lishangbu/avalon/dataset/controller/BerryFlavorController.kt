package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
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
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) internalName: String?,
        @RequestParam(required = false) name: String?,
    ): List<BerryFlavor> =
        berryFlavorService.listByCondition(
            BerryFlavor {
                id?.let { this.id = it }
                internalName?.let { this.internalName = it }
                name?.let { this.name = it }
            },
        )

    /** 保存树果风味 */
    @PostMapping
    fun save(
        @RequestBody berryFlavor: BerryFlavor,
    ): BerryFlavor = berryFlavorService.save(berryFlavor)

    /** 更新树果风味 */
    @PutMapping
    fun update(
        @RequestBody berryFlavor: BerryFlavor,
    ): BerryFlavor = berryFlavorService.update(berryFlavor)

    /** 按 ID 删除树果风味 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        berryFlavorService.removeById(id)
    }
}
