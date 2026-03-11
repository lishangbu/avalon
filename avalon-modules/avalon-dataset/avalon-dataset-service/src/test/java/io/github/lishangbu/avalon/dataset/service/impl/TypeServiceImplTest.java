package io.github.lishangbu.avalon.dataset.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
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
class TypeServiceImplTest {
    @Mock private TypeRepository typeRepository;
    @InjectMocks private TypeServiceImpl typeService;

    @Test
    void getPageByCondition_callsRepository() {
        Type type = new Type();
        Pageable pageable = PageRequest.of(0, 5);
        Page<Type> page = new PageImpl<>(List.of(type));
        when(typeRepository.findAll(any(Example.class), eq(pageable))).thenReturn(page);

        Page<Type> result = typeService.getPageByCondition(type, pageable);

        assertSame(page, result);
        ArgumentCaptor<Example> captor = ArgumentCaptor.forClass(Example.class);
        verify(typeRepository).findAll(captor.capture(), eq(pageable));
        assertNotNull(captor.getValue().getProbe());
    }

    @Test
    void listByCondition_callsRepository() {
        Type type = new Type();
        List<Type> expected = List.of(type);
        when(typeRepository.findAll(any(Example.class))).thenReturn(expected);

        List<Type> result = typeService.listByCondition(type);

        assertSame(expected, result);
        verify(typeRepository).findAll(any(Example.class));
    }

    @Test
    void save_usesRepository() {
        Type type = new Type();
        when(typeRepository.save(type)).thenReturn(type);

        Type result = typeService.save(type);

        assertSame(type, result);
        verify(typeRepository).save(type);
    }

    @Test
    void update_usesRepository() {
        Type type = new Type();
        when(typeRepository.save(type)).thenReturn(type);

        Type result = typeService.update(type);

        assertSame(type, result);
        verify(typeRepository).save(type);
    }

    @Test
    void removeById_callsRepository() {
        typeService.removeById(1L);
        verify(typeRepository).deleteById(1L);
    }
}
