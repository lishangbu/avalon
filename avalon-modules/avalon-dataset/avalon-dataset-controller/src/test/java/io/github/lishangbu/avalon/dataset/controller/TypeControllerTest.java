package io.github.lishangbu.avalon.dataset.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.service.TypeService;
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
class TypeControllerTest {
    @Mock private TypeService typeService;
    @InjectMocks private TypeController typeController;

    @Test
    void getTypePage_delegatesToService() {
        Type type = new Type();
        Pageable pageable = PageRequest.of(0, 5);
        Page<Type> page = new PageImpl<>(List.of(type));
        when(typeService.getPageByCondition(any(Type.class), eq(pageable))).thenReturn(page);

        Page<Type> result = typeController.getTypePage(pageable, type);

        assertSame(page, result);
        verify(typeService).getPageByCondition(type, pageable);
    }

    @Test
    void listTypes_delegatesToService() {
        Type type = new Type();
        List<Type> list = List.of(type);
        when(typeService.listByCondition(type)).thenReturn(list);

        List<Type> result = typeController.listTypes(type);

        assertSame(list, result);
        verify(typeService).listByCondition(type);
    }

    @Test
    void save_delegatesToService() {
        Type type = new Type();
        when(typeService.save(type)).thenReturn(type);

        Type result = typeController.save(type);

        assertSame(type, result);
        verify(typeService).save(type);
    }

    @Test
    void update_delegatesToService() {
        Type type = new Type();
        when(typeService.update(type)).thenReturn(type);

        Type result = typeController.update(type);

        assertSame(type, result);
        verify(typeService).update(type);
    }

    @Test
    void deleteById_delegatesToService() {
        typeController.deleteById(1L);
        verify(typeService).removeById(1L);
    }
}
