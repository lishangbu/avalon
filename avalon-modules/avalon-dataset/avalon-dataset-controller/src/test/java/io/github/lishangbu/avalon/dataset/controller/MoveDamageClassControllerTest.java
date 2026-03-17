package io.github.lishangbu.avalon.dataset.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass;
import io.github.lishangbu.avalon.dataset.service.MoveDamageClassService;
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
class MoveDamageClassControllerTest {
    @Mock private MoveDamageClassService moveDamageClassService;
    @InjectMocks private MoveDamageClassController moveDamageClassController;

    @Test
    void getMoveDamageClassPage_delegatesToService() {
        MoveDamageClass moveDamageClass = new MoveDamageClass();
        Pageable pageable = PageRequest.of(0, 5);
        Page<MoveDamageClass> page = new PageImpl<>(List.of(moveDamageClass));
        when(moveDamageClassService.getPageByCondition(any(MoveDamageClass.class), eq(pageable)))
                .thenReturn(page);

        Page<MoveDamageClass> result =
                moveDamageClassController.getMoveDamageClassPage(pageable, moveDamageClass);

        assertSame(page, result);
        verify(moveDamageClassService).getPageByCondition(moveDamageClass, pageable);
    }

    @Test
    void listMoveDamageClasses_delegatesToService() {
        MoveDamageClass moveDamageClass = new MoveDamageClass();
        List<MoveDamageClass> list = List.of(moveDamageClass);
        when(moveDamageClassService.listByCondition(moveDamageClass)).thenReturn(list);

        List<MoveDamageClass> result =
                moveDamageClassController.listMoveDamageClasses(moveDamageClass);

        assertSame(list, result);
        verify(moveDamageClassService).listByCondition(moveDamageClass);
    }

    @Test
    void save_delegatesToService() {
        MoveDamageClass moveDamageClass = new MoveDamageClass();
        when(moveDamageClassService.save(moveDamageClass)).thenReturn(moveDamageClass);

        MoveDamageClass result = moveDamageClassController.save(moveDamageClass);

        assertSame(moveDamageClass, result);
        verify(moveDamageClassService).save(moveDamageClass);
    }

    @Test
    void update_delegatesToService() {
        MoveDamageClass moveDamageClass = new MoveDamageClass();
        when(moveDamageClassService.update(moveDamageClass)).thenReturn(moveDamageClass);

        MoveDamageClass result = moveDamageClassController.update(moveDamageClass);

        assertSame(moveDamageClass, result);
        verify(moveDamageClassService).update(moveDamageClass);
    }

    @Test
    void deleteById_delegatesToService() {
        moveDamageClassController.deleteById(1L);
        verify(moveDamageClassService).removeById(1L);
    }
}
