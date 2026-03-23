package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.service.BerryFlavorService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

/** 树果风味控制器*/
@RestController
@RequestMapping("/berry-flavor")
class BerryFlavorController(
    /** 树果风味服务 */
    private val berryFlavorService: BerryFlavorService,
) {
    /** 获取树果风味分页结果 */
    @GetMapping("/page")
    fun getBerryFlavorPage(
        pageable: Pageable,
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) internalName: String?,
        @RequestParam(required = false) name: String?,
    ): Page<BerryFlavor> =
        berryFlavorService.getPageByCondition(
            BerryFlavor {
                id?.let { this.id = it }
                internalName?.let { this.internalName = it }
                name?.let { this.name = it }
            },
            pageable,
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
