package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryFirmnessInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryFirmnessInput
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository
import jakarta.validation.Valid
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

/** 树果硬度控制器*/
@RestController
@RequestMapping("/berry-firmness")
class BerryFirmnessController(
    /** 树果硬度仓储 */
    private val berryFirmnessRepository: BerryFirmnessRepository,
) {
    /** 获取树果硬度分页结果 */
    @GetMapping("/page")
    fun getBerryFirmnessPage(
        pageable: Pageable,
        @ModelAttribute specification: BerryFirmnessSpecification,
    ): Page<BerryFirmnessView> = berryFirmnessRepository.pageViews(specification, pageable)

    /** 保存树果硬度 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveBerryFirmnessInput,
    ): BerryFirmnessView = berryFirmnessRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    /** 更新树果硬度 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateBerryFirmnessInput,
    ): BerryFirmnessView = berryFirmnessRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    /** 按 ID 删除树果硬度 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        berryFirmnessRepository.deleteById(id)
    }

    /** 查询树果硬度列表 */
    @GetMapping("/list")
    fun listBerryFirmnesses(
        @ModelAttribute specification: BerryFirmnessSpecification,
    ): List<BerryFirmnessView> = berryFirmnessRepository.listViews(specification)

    private fun reloadView(berryFirmness: BerryFirmness): BerryFirmnessView = requireNotNull(berryFirmnessRepository.loadViewById(berryFirmness.id)) { "未找到 ID=${berryFirmness.id} 对应的树果硬度" }
}
