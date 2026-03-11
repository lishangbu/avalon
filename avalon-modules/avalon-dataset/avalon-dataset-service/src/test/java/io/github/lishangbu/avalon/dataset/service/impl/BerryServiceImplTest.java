package io.github.lishangbu.avalon.dataset.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lishangbu.avalon.dataset.entity.Berry;
import io.github.lishangbu.avalon.dataset.repository.BerryRepository;
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
class BerryServiceImplTest {
    @Mock private BerryRepository berryRepository;
    @InjectMocks private BerryServiceImpl berryService;

    @Test
    void getPageByCondition_callsRepository() {
        Berry berry = new Berry();
        Pageable pageable = PageRequest.of(0, 5);
        Page<Berry> page = new PageImpl<>(List.of(berry));
        when(berryRepository.findAll(any(Example.class), eq(pageable))).thenReturn(page);

        Page<Berry> result = berryService.getPageByCondition(berry, pageable);

        assertSame(page, result);
        ArgumentCaptor<Example> captor = ArgumentCaptor.forClass(Example.class);
        verify(berryRepository).findAll(captor.capture(), eq(pageable));
        assertNotNull(captor.getValue().getProbe());
    }

    @Test
    void save_usesRepository() {
        Berry berry = new Berry();
        when(berryRepository.save(berry)).thenReturn(berry);

        Berry result = berryService.save(berry);

        assertSame(berry, result);
        verify(berryRepository).save(berry);
    }

    @Test
    void update_usesRepository() {
        Berry berry = new Berry();
        when(berryRepository.save(berry)).thenReturn(berry);

        Berry result = berryService.update(berry);

        assertSame(berry, result);
        verify(berryRepository).save(berry);
    }

    @Test
    void removeById_callsRepository() {
        berryService.removeById(1L);
        verify(berryRepository).deleteById(1L);
    }
}
