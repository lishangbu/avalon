package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Berry
import io.github.lishangbu.avalon.dataset.entity.dto.BerrySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryInput
import io.github.lishangbu.avalon.dataset.repository.BerryRepository
import jakarta.validation.Valid
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

/**
 * 树果管理控制器
 * 提供树果的分页查询、新增、更新和删除接口
 */
@RestController
@RequestMapping("/berry")
class BerryController(
    /** 树果仓储 */
    private val berryRepository: BerryRepository,
) {
    /** 按筛选条件分页查询树果*/
    @GetMapping("/page")
    fun getBerryPage(
        pageable: Pageable,
        @ModelAttribute specification: BerrySpecification,
    ): Page<BerryView> = berryRepository.pageViews(specification, pageable)

    /** 创建树果 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveBerryInput,
    ): BerryView = berryRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    /** 更新树果 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateBerryInput,
    ): BerryView = berryRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    /** 删除指定 ID 的树果*/
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        berryRepository.deleteById(id)
    }

    private fun reloadView(berry: Berry): BerryView = requireNotNull(berryRepository.loadViewById(berry.id)) { "未找到 ID=${berry.id} 对应的树果" }
}
