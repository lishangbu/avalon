package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassSpecification
import io.github.lishangbu.avalon.dataset.repository.MoveDamageClassRepository
import io.github.lishangbu.avalon.dataset.service.MoveDamageClassService
import org.babyfish.jimmer.Page
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
    ): Page<MoveDamageClass> = moveDamageClassRepository.findAll(specification, pageable)

    /** 根据条件查询招式伤害分类列表 */
    override fun listByCondition(specification: MoveDamageClassSpecification): List<MoveDamageClass> = moveDamageClassRepository.findAll(specification)

    /** 保存招式伤害分类 */
    override fun save(moveDamageClass: MoveDamageClass): MoveDamageClass = moveDamageClassRepository.save(moveDamageClass)

    /** 更新招式伤害分类 */
    override fun update(moveDamageClass: MoveDamageClass): MoveDamageClass = moveDamageClassRepository.save(moveDamageClass)

    /** 按 ID 删除招式伤害分类 */
    override fun removeById(id: Long) {
        moveDamageClassRepository.deleteById(id)
    }
}
