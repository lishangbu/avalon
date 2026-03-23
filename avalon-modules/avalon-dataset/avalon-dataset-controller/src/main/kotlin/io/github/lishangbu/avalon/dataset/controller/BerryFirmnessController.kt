package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.service.BerryFirmnessService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

/** 树果硬度控制器*/
@RestController
@RequestMapping("/berry-firmness")
class BerryFirmnessController(
    /** 树果硬度服务 */
    private val berryFirmnessService: BerryFirmnessService,
) {
    /** 获取树果硬度分页结果 */
    @GetMapping("/page")
    fun getBerryFirmnessPage(
        pageable: Pageable,
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) internalName: String?,
        @RequestParam(required = false) name: String?,
    ): Page<BerryFirmness> =
        berryFirmnessService.getPageByCondition(
            BerryFirmness {
                id?.let { this.id = it }
                internalName?.let { this.internalName = it }
                name?.let { this.name = it }
            },
            pageable,
        )

    /** 保存树果硬度 */
    @PostMapping
    fun save(
        @RequestBody berryFirmness: BerryFirmness,
    ): BerryFirmness = berryFirmnessService.save(berryFirmness)

    /** 更新树果硬度 */
    @PutMapping
    fun update(
        @RequestBody berryFirmness: BerryFirmness,
    ): BerryFirmness = berryFirmnessService.update(berryFirmness)

    /** 按 ID 删除树果硬度 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        berryFirmnessService.removeById(id)
    }

    /** 查询树果硬度列表 */
    @GetMapping("/list")
    fun listBerryFirmnesses(
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) internalName: String?,
        @RequestParam(required = false) name: String?,
    ): List<BerryFirmness> =
        berryFirmnessService.listByCondition(
            BerryFirmness {
                id?.let { this.id = it }
                internalName?.let { this.internalName = it }
                name?.let { this.name = it }
            },
        )
}
