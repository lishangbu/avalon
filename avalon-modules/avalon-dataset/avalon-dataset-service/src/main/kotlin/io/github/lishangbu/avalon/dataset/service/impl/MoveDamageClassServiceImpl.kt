package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.repository.MoveDamageClassRepository
import io.github.lishangbu.avalon.dataset.service.MoveDamageClassService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 招式伤害类别服务实现。 */
@Service
class MoveDamageClassServiceImpl(
    private val moveDamageClassRepository: MoveDamageClassRepository,
) : MoveDamageClassService {
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

    override fun listByCondition(moveDamageClass: MoveDamageClass): List<MoveDamageClass> {
        val matcher =
            ExampleMatcher
                .matching()
                .withIgnoreNullValues()
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains())
        return moveDamageClassRepository.findAll(Example.of(moveDamageClass, matcher))
    }

    override fun save(moveDamageClass: MoveDamageClass): MoveDamageClass = moveDamageClassRepository.save(moveDamageClass)

    override fun update(moveDamageClass: MoveDamageClass): MoveDamageClass = moveDamageClassRepository.save(moveDamageClass)

    override fun removeById(id: Long) {
        moveDamageClassRepository.deleteById(id)
    }
}
