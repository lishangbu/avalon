package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.repository.BerryFlavorRepository
import io.github.lishangbu.avalon.dataset.service.BerryFlavorService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 树果风味服务实现。 */
@Service
class BerryFlavorServiceImpl(
    private val berryFlavorRepository: BerryFlavorRepository,
) : BerryFlavorService {
    override fun getPageByCondition(
        berryFlavor: BerryFlavor,
        pageable: Pageable,
    ): Page<BerryFlavor> =
        berryFlavorRepository.findAll(
            Example.of(
                berryFlavor,
                ExampleMatcher
                    .matching()
                    .withIgnoreNullValues()
                    .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains()),
            ),
            pageable,
        )

    override fun save(berryFlavor: BerryFlavor): BerryFlavor = berryFlavorRepository.save(berryFlavor)

    override fun update(berryFlavor: BerryFlavor): BerryFlavor = berryFlavorRepository.save(berryFlavor)

    override fun removeById(id: Long) {
        berryFlavorRepository.deleteById(id)
    }
}
