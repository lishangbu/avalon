package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveDamageClassInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveDamageClassInput
import io.github.lishangbu.avalon.dataset.repository.MoveDamageClassRepository
import io.github.lishangbu.avalon.dataset.service.MoveDamageClassService
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 招式伤害分类服务实现 */
@Service
class MoveDamageClassServiceImpl(
    /** 招式伤害分类仓储 */
    private val moveDamageClassRepository: MoveDamageClassRepository,
) : MoveDamageClassService {
    /** 按条件分页查询招式伤害分类*/
    override fun getPageByCondition(
        specification: MoveDamageClassSpecification,
        pageable: Pageable,
    ): Page<MoveDamageClassView> = moveDamageClassRepository.pageViews(specification, pageable)

    /** 根据条件查询招式伤害分类列表 */
    override fun listByCondition(
        specification: MoveDamageClassSpecification,
    ): List<MoveDamageClassView> = moveDamageClassRepository.listViews(specification)

    /** 保存招式伤害分类 */
    override fun save(command: SaveMoveDamageClassInput): MoveDamageClassView = MoveDamageClassView(moveDamageClassRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    /** 更新招式伤害分类 */
    override fun update(
        command: UpdateMoveDamageClassInput,
    ): MoveDamageClassView = MoveDamageClassView(moveDamageClassRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY))

    /** 按 ID 删除招式伤害分类 */
    override fun removeById(id: Long) {
        moveDamageClassRepository.deleteById(id)
    }
}
