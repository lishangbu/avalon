package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.repository.MoveDamageClassRepository
import io.github.lishangbu.avalon.dataset.service.MoveDamageClassService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
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
        moveDamageClass: MoveDamageClass,
        pageable: Pageable,
    ): Page<MoveDamageClass> =
        moveDamageClassRepository.findAll(
            Example.of(
                moveDamageClass,
                ExampleMatcher
                    .matching()
                    .withIgnoreNullValues()
                    .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains()),
            ),
            pageable,
        )

    /** 根据条件查询招式伤害分类列表 */
    override fun listByCondition(moveDamageClass: MoveDamageClass): List<MoveDamageClass> {
        val matcher =
            ExampleMatcher
                .matching()
                .withIgnoreNullValues()
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains())
        return moveDamageClassRepository.findAll(Example.of(moveDamageClass, matcher))
    }

    /** 保存招式伤害分类 */
    override fun save(moveDamageClass: MoveDamageClass): MoveDamageClass = moveDamageClassRepository.save(moveDamageClass)

    /** 更新招式伤害分类 */
    override fun update(moveDamageClass: MoveDamageClass): MoveDamageClass = moveDamageClassRepository.save(moveDamageClass)

    /** 按 ID 删除招式伤害分类 */
    override fun removeById(id: Long) {
        moveDamageClassRepository.deleteById(id)
    }
}
