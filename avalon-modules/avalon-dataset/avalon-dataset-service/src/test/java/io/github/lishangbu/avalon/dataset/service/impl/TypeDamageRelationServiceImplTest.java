package io.github.lishangbu.avalon.dataset.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation.TypeDamageRelationId;
import io.github.lishangbu.avalon.dataset.repository.TypeDamageRelationRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TypeDamageRelationServiceImplTest {
    @Mock private TypeDamageRelationRepository typeDamageRelationRepository;
    @InjectMocks private TypeDamageRelationServiceImpl typeDamageRelationService;

    @Test
    void getPageByCondition_callsRepository() {
        TypeDamageRelation relation = new TypeDamageRelation();
        Pageable pageable = PageRequest.of(0, 5);
        Page<TypeDamageRelation> page = new PageImpl<>(List.of(relation));
        when(typeDamageRelationRepository.findAll(any(Example.class), eq(pageable)))
                .thenReturn(page);

        Page<TypeDamageRelation> result =
                typeDamageRelationService.getPageByCondition(relation, pageable);

        assertSame(page, result);
        ArgumentCaptor<Example> captor = ArgumentCaptor.forClass(Example.class);
        verify(typeDamageRelationRepository).findAll(captor.capture(), eq(pageable));
        assertNotNull(captor.getValue().getProbe());
    }

    @Test
    void listByCondition_allowsNullProbe() {
        List<TypeDamageRelation> expected = List.of();
        when(typeDamageRelationRepository.findAll(any(Example.class))).thenReturn(expected);

        List<TypeDamageRelation> result = typeDamageRelationService.listByCondition(null);

        assertSame(expected, result);
        ArgumentCaptor<Example> captor = ArgumentCaptor.forClass(Example.class);
        verify(typeDamageRelationRepository).findAll(captor.capture());
        assertNotNull(captor.getValue().getProbe());
    }

    @Test
    void save_usesRepository() {
        TypeDamageRelation relation = new TypeDamageRelation();
        when(typeDamageRelationRepository.save(relation)).thenReturn(relation);

        TypeDamageRelation result = typeDamageRelationService.save(relation);

        assertSame(relation, result);
        verify(typeDamageRelationRepository).save(relation);
    }

    @Test
    void update_usesRepository() {
        TypeDamageRelation relation = new TypeDamageRelation();
        when(typeDamageRelationRepository.save(relation)).thenReturn(relation);

        TypeDamageRelation result = typeDamageRelationService.update(relation);

        assertSame(relation, result);
        verify(typeDamageRelationRepository).save(relation);
    }

    @Test
    void removeById_callsRepository() {
        TypeDamageRelationId id = new TypeDamageRelationId();
        typeDamageRelationService.removeById(id);
        verify(typeDamageRelationRepository).deleteById(id);
    }
}
