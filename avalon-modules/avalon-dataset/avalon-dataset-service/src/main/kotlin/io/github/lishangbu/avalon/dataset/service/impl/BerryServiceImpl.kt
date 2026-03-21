package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Berry
import io.github.lishangbu.avalon.dataset.repository.BerryRepository
import io.github.lishangbu.avalon.dataset.service.BerryService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 树果服务实现。 */
@Service
class BerryServiceImpl(
    private val berryRepository: BerryRepository,
) : BerryService {
    override fun getPageByCondition(
        berry: Berry,
        pageable: Pageable,
    ): Page<Berry> =
        berryRepository.findAll(
            Example.of(
                berry,
                ExampleMatcher
                    .matching()
                    .withIgnoreNullValues()
                    .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains()),
            ),
            pageable,
        )

    override fun save(berry: Berry): Berry = berryRepository.save(berry)

    override fun update(berry: Berry): Berry = berryRepository.save(berry)

    override fun removeById(id: Long) {
        berryRepository.deleteById(id)
    }
}
