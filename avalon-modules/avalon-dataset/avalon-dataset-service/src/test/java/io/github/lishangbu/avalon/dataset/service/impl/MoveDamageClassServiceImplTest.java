package io.github.lishangbu.avalon.dataset.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass;
import io.github.lishangbu.avalon.dataset.repository.MoveDamageClassRepository;
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
class MoveDamageClassServiceImplTest {
    @Mock private MoveDamageClassRepository moveDamageClassRepository;
    @InjectMocks private MoveDamageClassServiceImpl moveDamageClassService;

    @Test
    void getPageByCondition_callsRepository() {
        MoveDamageClass moveDamageClass = new MoveDamageClass();
        Pageable pageable = PageRequest.of(0, 5);
        Page<MoveDamageClass> page = new PageImpl<>(List.of(moveDamageClass));
        when(moveDamageClassRepository.findAll(any(Example.class), eq(pageable))).thenReturn(page);

        Page<MoveDamageClass> result =
                moveDamageClassService.getPageByCondition(moveDamageClass, pageable);

        assertSame(page, result);
        ArgumentCaptor<Example> captor = ArgumentCaptor.forClass(Example.class);
        verify(moveDamageClassRepository).findAll(captor.capture(), eq(pageable));
        assertNotNull(captor.getValue().getProbe());
    }

    @Test
    void listByCondition_callsRepository() {
        MoveDamageClass moveDamageClass = new MoveDamageClass();
        List<MoveDamageClass> expected = List.of(moveDamageClass);
        when(moveDamageClassRepository.findAll(any(Example.class))).thenReturn(expected);

        List<MoveDamageClass> result = moveDamageClassService.listByCondition(moveDamageClass);

        assertSame(expected, result);
        verify(moveDamageClassRepository).findAll(any(Example.class));
    }

    @Test
    void save_usesRepository() {
        MoveDamageClass moveDamageClass = new MoveDamageClass();
        when(moveDamageClassRepository.save(moveDamageClass)).thenReturn(moveDamageClass);

        MoveDamageClass result = moveDamageClassService.save(moveDamageClass);

        assertSame(moveDamageClass, result);
        verify(moveDamageClassRepository).save(moveDamageClass);
    }

    @Test
    void update_usesRepository() {
        MoveDamageClass moveDamageClass = new MoveDamageClass();
        when(moveDamageClassRepository.save(moveDamageClass)).thenReturn(moveDamageClass);

        MoveDamageClass result = moveDamageClassService.update(moveDamageClass);

        assertSame(moveDamageClass, result);
        verify(moveDamageClassRepository).save(moveDamageClass);
    }

    @Test
    void removeById_callsRepository() {
        moveDamageClassService.removeById(1L);
        verify(moveDamageClassRepository).deleteById(1L);
    }
}
