package io.github.lishangbu.avalon.dataset.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation.TypeDamageRelationId;
import io.github.lishangbu.avalon.dataset.service.TypeDamageRelationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TypeDamageRelationControllerTest {
    @Mock private TypeDamageRelationService typeDamageRelationService;
    @InjectMocks private TypeDamageRelationController typeDamageRelationController;

    @Test
    void getTypeDamageRelationPage_delegatesToService() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<TypeDamageRelation> page = new PageImpl<>(List.of(new TypeDamageRelation()));
        when(typeDamageRelationService.getPageByCondition(
                        any(TypeDamageRelation.class), eq(pageable)))
                .thenReturn(page);

        Page<TypeDamageRelation> result =
                typeDamageRelationController.getTypeDamageRelationPage(pageable, 1L, 2L, 2.0f);

        assertSame(page, result);
        verify(typeDamageRelationService)
                .getPageByCondition(any(TypeDamageRelation.class), eq(pageable));
    }

    @Test
    void listTypeDamageRelations_delegatesToService() {
        List<TypeDamageRelation> list = List.of(new TypeDamageRelation());
        when(typeDamageRelationService.listByCondition(any(TypeDamageRelation.class)))
                .thenReturn(list);

        List<TypeDamageRelation> result =
                typeDamageRelationController.listTypeDamageRelations(1L, 2L, 0.5f);

        assertSame(list, result);
        verify(typeDamageRelationService).listByCondition(any(TypeDamageRelation.class));
    }

    @Test
    void save_delegatesToService() {
        TypeDamageRelation relation = new TypeDamageRelation();
        when(typeDamageRelationService.save(relation)).thenReturn(relation);

        TypeDamageRelation result = typeDamageRelationController.save(relation);

        assertSame(relation, result);
        verify(typeDamageRelationService).save(relation);
    }

    @Test
    void update_delegatesToService() {
        TypeDamageRelation relation = new TypeDamageRelation();
        when(typeDamageRelationService.update(relation)).thenReturn(relation);

        TypeDamageRelation result = typeDamageRelationController.update(relation);

        assertSame(relation, result);
        verify(typeDamageRelationService).update(relation);
    }

    @Test
    void deleteById_delegatesToService() {
        typeDamageRelationController.deleteById(1L, 2L);
        verify(typeDamageRelationService).removeById(any(TypeDamageRelationId.class));
    }
}
