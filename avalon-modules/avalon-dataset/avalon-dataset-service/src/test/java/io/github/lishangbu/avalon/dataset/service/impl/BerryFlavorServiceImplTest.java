package io.github.lishangbu.avalon.dataset.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import io.github.lishangbu.avalon.dataset.repository.BerryFlavorRepository;
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
class BerryFlavorServiceImplTest {
    @Mock private BerryFlavorRepository berryFlavorRepository;
    @InjectMocks private BerryFlavorServiceImpl berryFlavorService;

    @Test
    void getPageByCondition_callsRepository() {
        BerryFlavor berryFlavor = new BerryFlavor();
        Pageable pageable = PageRequest.of(0, 5);
        Page<BerryFlavor> page = new PageImpl<>(List.of(berryFlavor));
        when(berryFlavorRepository.findAll(any(Example.class), eq(pageable))).thenReturn(page);

        Page<BerryFlavor> result = berryFlavorService.getPageByCondition(berryFlavor, pageable);

        assertSame(page, result);
        ArgumentCaptor<Example> captor = ArgumentCaptor.forClass(Example.class);
        verify(berryFlavorRepository).findAll(captor.capture(), eq(pageable));
        assertNotNull(captor.getValue().getProbe());
    }

    @Test
    void save_usesRepository() {
        BerryFlavor berryFlavor = new BerryFlavor();
        when(berryFlavorRepository.save(berryFlavor)).thenReturn(berryFlavor);

        BerryFlavor result = berryFlavorService.save(berryFlavor);

        assertSame(berryFlavor, result);
        verify(berryFlavorRepository).save(berryFlavor);
    }

    @Test
    void update_usesRepository() {
        BerryFlavor berryFlavor = new BerryFlavor();
        when(berryFlavorRepository.save(berryFlavor)).thenReturn(berryFlavor);

        BerryFlavor result = berryFlavorService.update(berryFlavor);

        assertSame(berryFlavor, result);
        verify(berryFlavorRepository).save(berryFlavor);
    }

    @Test
    void removeById_callsRepository() {
        berryFlavorService.removeById(1L);
        verify(berryFlavorRepository).deleteById(1L);
    }
}
