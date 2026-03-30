package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryFirmnessInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryFirmnessInput
import io.github.lishangbu.avalon.dataset.service.BerryFirmnessService
import jakarta.validation.Valid
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
        @ModelAttribute specification: BerryFirmnessSpecification,
    ): Page<BerryFirmnessView> = berryFirmnessService.getPageByCondition(specification, pageable)

    /** 保存树果硬度 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveBerryFirmnessInput,
    ): BerryFirmnessView = berryFirmnessService.save(command)

    /** 更新树果硬度 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateBerryFirmnessInput,
    ): BerryFirmnessView = berryFirmnessService.update(command)

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
        @ModelAttribute specification: BerryFirmnessSpecification,
    ): List<BerryFirmnessView> = berryFirmnessService.listByCondition(specification)
}
