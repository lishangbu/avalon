package io.github.lishangbu.avalon.dataset.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lishangbu.avalon.dataset.entity.Berry;
import io.github.lishangbu.avalon.dataset.service.BerryService;
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
class BerryControllerTest {
    @Mock private BerryService berryService;
    @InjectMocks private BerryController berryController;

    @Test
    void getBerryPage_delegatesToService() {
        Berry berry = new Berry();
        Pageable pageable = PageRequest.of(0, 5);
        Page<Berry> page = new PageImpl<>(List.of(berry));
        when(berryService.getPageByCondition(any(Berry.class), eq(pageable))).thenReturn(page);

        Page<Berry> result = berryController.getBerryPage(pageable, berry);

        assertSame(page, result);
        verify(berryService).getPageByCondition(berry, pageable);
    }

    @Test
    void save_delegatesToService() {
        Berry berry = new Berry();
        when(berryService.save(berry)).thenReturn(berry);

        Berry result = berryController.save(berry);

        assertSame(berry, result);
        verify(berryService).save(berry);
    }

    @Test
    void update_delegatesToService() {
        Berry berry = new Berry();
        when(berryService.update(berry)).thenReturn(berry);

        Berry result = berryController.update(berry);

        assertSame(berry, result);
        verify(berryService).update(berry);
    }

    @Test
    void deleteById_delegatesToService() {
        berryController.deleteById(1L);
        verify(berryService).removeById(1L);
    }
}
