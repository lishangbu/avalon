package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelationId
import io.github.lishangbu.avalon.dataset.repository.TypeDamageRelationRepository
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class TypeDamageRelationServiceImplTest {
    @Mock
    private lateinit var typeDamageRelationRepository: TypeDamageRelationRepository

    @InjectMocks
    private lateinit var typeDamageRelationService: TypeDamageRelationServiceImpl

    @Test
    fun getPageByCondition_callsRepository() {
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<TypeDamageRelation> = Page(listOf(TypeDamageRelation()), 1, 1)
        Mockito.`when`(
            typeDamageRelationRepository.findPage(1L, 2L, 2.0f, pageable),
        ).thenReturn(page)

        val result = typeDamageRelationService.getPageByCondition(1L, 2L, 2.0f, pageable)

        assertSame(page, result)
        Mockito.verify(typeDamageRelationRepository).findPage(1L, 2L, 2.0f, pageable)
    }

    @Test
    fun listByCondition_allowsNullFilter() {
        val expected = listOf<TypeDamageRelation>()
        Mockito.`when`(typeDamageRelationRepository.findAll(null, null, null)).thenReturn(expected)

        val result = typeDamageRelationService.listByCondition(null, null, null)

        assertSame(expected, result)
        Mockito.verify(typeDamageRelationRepository).findAll(null, null, null)
    }

    @Test
    fun save_usesRepository() {
        val relation = TypeDamageRelation()
        Mockito.`when`(typeDamageRelationRepository.save(relation)).thenReturn(relation)

        val result = typeDamageRelationService.save(relation)

        assertSame(relation, result)
        Mockito.verify(typeDamageRelationRepository).save(relation)
    }

    @Test
    fun update_usesRepository() {
        val relation = TypeDamageRelation()
        Mockito.`when`(typeDamageRelationRepository.save(relation)).thenReturn(relation)

        val result = typeDamageRelationService.update(relation)

        assertSame(relation, result)
        Mockito.verify(typeDamageRelationRepository).save(relation)
    }

    @Test
    fun removeById_callsRepository() {
        val id = TypeDamageRelationId()
        typeDamageRelationService.removeById(id)
        Mockito.verify(typeDamageRelationRepository).deleteById(id)
    }
}
