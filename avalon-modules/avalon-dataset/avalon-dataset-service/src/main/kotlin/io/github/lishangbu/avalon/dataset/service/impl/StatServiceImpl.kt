package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.repository.StatRepository
import io.github.lishangbu.avalon.dataset.service.StatService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 能力服务实现。 */
@Service
class StatServiceImpl(
    private val statRepository: StatRepository,
) : StatService {
    override fun getPageByCondition(
        stat: Stat,
        pageable: Pageable,
    ): Page<Stat> =
        statRepository.findAll(
            Example.of(
                stat,
                ExampleMatcher
                    .matching()
                    .withIgnoreNullValues()
                    .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains()),
            ),
            pageable,
        )

    override fun listByCondition(stat: Stat): List<Stat> {
        val matcher =
            ExampleMatcher
                .matching()
                .withIgnoreNullValues()
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains())
        return statRepository.findAll(Example.of(stat, matcher))
    }

    override fun save(stat: Stat): Stat = statRepository.save(stat)

    override fun update(stat: Stat): Stat = statRepository.save(stat)

    override fun removeById(id: Long) {
        statRepository.deleteById(id)
    }
}
