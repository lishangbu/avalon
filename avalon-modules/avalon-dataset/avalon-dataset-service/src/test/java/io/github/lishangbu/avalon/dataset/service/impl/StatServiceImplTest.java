package io.github.lishangbu.avalon.dataset.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lishangbu.avalon.dataset.entity.Stat;
import io.github.lishangbu.avalon.dataset.repository.StatRepository;
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
class StatServiceImplTest {
    @Mock private StatRepository statRepository;
    @InjectMocks private StatServiceImpl statService;

    @Test
    void getPageByCondition_callsRepository() {
        Stat stat = new Stat();
        Pageable pageable = PageRequest.of(0, 5);
        Page<Stat> page = new PageImpl<>(List.of(stat));
        when(statRepository.findAll(any(Example.class), eq(pageable))).thenReturn(page);

        Page<Stat> result = statService.getPageByCondition(stat, pageable);

        assertSame(page, result);
        ArgumentCaptor<Example> captor = ArgumentCaptor.forClass(Example.class);
        verify(statRepository).findAll(captor.capture(), eq(pageable));
        assertNotNull(captor.getValue().getProbe());
    }

    @Test
    void listByCondition_callsRepository() {
        Stat stat = new Stat();
        List<Stat> expected = List.of(stat);
        when(statRepository.findAll(any(Example.class))).thenReturn(expected);

        List<Stat> result = statService.listByCondition(stat);

        assertSame(expected, result);
        verify(statRepository).findAll(any(Example.class));
    }

    @Test
    void save_usesRepository() {
        Stat stat = new Stat();
        when(statRepository.save(stat)).thenReturn(stat);

        Stat result = statService.save(stat);

        assertSame(stat, result);
        verify(statRepository).save(stat);
    }

    @Test
    void update_usesRepository() {
        Stat stat = new Stat();
        when(statRepository.save(stat)).thenReturn(stat);

        Stat result = statService.update(stat);

        assertSame(stat, result);
        verify(statRepository).save(stat);
    }

    @Test
    void removeById_callsRepository() {
        statService.removeById(1L);
        verify(statRepository).deleteById(1L);
    }
}
