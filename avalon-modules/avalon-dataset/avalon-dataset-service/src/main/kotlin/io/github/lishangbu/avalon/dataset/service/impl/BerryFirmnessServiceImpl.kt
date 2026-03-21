package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository
import io.github.lishangbu.avalon.dataset.service.BerryFirmnessService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 树果坚硬度服务实现。 */
@Service
class BerryFirmnessServiceImpl(
    private val berryFirmnessRepository: BerryFirmnessRepository,
) : BerryFirmnessService {
    override fun getPageByCondition(
        berryFirmness: BerryFirmness,
        pageable: Pageable,
    ): Page<BerryFirmness> =
        berryFirmnessRepository.findAll(
            Example.of(
                berryFirmness,
                ExampleMatcher
                    .matching()
                    .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withIgnoreNullValues(),
            ),
            pageable,
        )

    override fun listByCondition(berryFirmness: BerryFirmness): List<BerryFirmness> {
        val matcher =
            ExampleMatcher
                .matching()
                .withIgnoreNullValues()
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains())
        return berryFirmnessRepository.findAll(Example.of(berryFirmness, matcher))
    }

    override fun save(berryFirmness: BerryFirmness): BerryFirmness = berryFirmnessRepository.save(berryFirmness)

    override fun update(berryFirmness: BerryFirmness): BerryFirmness = berryFirmnessRepository.save(berryFirmness)

    override fun removeById(id: Long) {
        berryFirmnessRepository.deleteById(id)
    }
}
