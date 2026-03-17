package io.github.lishangbu.avalon.dataset.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lishangbu.avalon.dataset.entity.Stat;
import io.github.lishangbu.avalon.dataset.service.StatService;
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
class StatControllerTest {
    @Mock private StatService statService;
    @InjectMocks private StatController statController;

    @Test
    void getStatPage_delegatesToService() {
        Stat stat = new Stat();
        Pageable pageable = PageRequest.of(0, 5);
        Page<Stat> page = new PageImpl<>(List.of(stat));
        when(statService.getPageByCondition(any(Stat.class), eq(pageable))).thenReturn(page);

        Page<Stat> result = statController.getStatPage(pageable, stat);

        assertSame(page, result);
        verify(statService).getPageByCondition(stat, pageable);
    }

    @Test
    void listStats_delegatesToService() {
        Stat stat = new Stat();
        List<Stat> list = List.of(stat);
        when(statService.listByCondition(stat)).thenReturn(list);

        List<Stat> result = statController.listStats(stat);

        assertSame(list, result);
        verify(statService).listByCondition(stat);
    }

    @Test
    void save_delegatesToService() {
        Stat stat = new Stat();
        when(statService.save(stat)).thenReturn(stat);

        Stat result = statController.save(stat);

        assertSame(stat, result);
        verify(statService).save(stat);
    }

    @Test
    void update_delegatesToService() {
        Stat stat = new Stat();
        when(statService.update(stat)).thenReturn(stat);

        Stat result = statController.update(stat);

        assertSame(stat, result);
        verify(statService).update(stat);
    }

    @Test
    void deleteById_delegatesToService() {
        statController.deleteById(1L);
        verify(statService).removeById(1L);
    }
}
