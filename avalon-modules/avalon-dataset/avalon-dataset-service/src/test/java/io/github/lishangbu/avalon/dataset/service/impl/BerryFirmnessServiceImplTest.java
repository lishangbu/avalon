package io.github.lishangbu.avalon.dataset.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository;
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
class BerryFirmnessServiceImplTest {
    @Mock private BerryFirmnessRepository berryFirmnessRepository;
    @InjectMocks private BerryFirmnessServiceImpl berryFirmnessService;

    @Test
    void getPageByCondition_callsRepository() {
        BerryFirmness berryFirmness = new BerryFirmness();
        Pageable pageable = PageRequest.of(0, 5);
        Page<BerryFirmness> page = new PageImpl<>(List.of(berryFirmness));
        when(berryFirmnessRepository.findAll(any(Example.class), eq(pageable))).thenReturn(page);

        Page<BerryFirmness> result =
                berryFirmnessService.getPageByCondition(berryFirmness, pageable);

        assertSame(page, result);
        ArgumentCaptor<Example> captor = ArgumentCaptor.forClass(Example.class);
        verify(berryFirmnessRepository).findAll(captor.capture(), eq(pageable));
        assertNotNull(captor.getValue().getProbe());
    }

    @Test
    void listByCondition_callsRepository() {
        BerryFirmness berryFirmness = new BerryFirmness();
        List<BerryFirmness> expected = List.of(berryFirmness);
        when(berryFirmnessRepository.findAll(any(Example.class))).thenReturn(expected);

        List<BerryFirmness> result = berryFirmnessService.listByCondition(berryFirmness);

        assertSame(expected, result);
        verify(berryFirmnessRepository).findAll(any(Example.class));
    }

    @Test
    void save_usesRepository() {
        BerryFirmness berryFirmness = new BerryFirmness();
        when(berryFirmnessRepository.save(berryFirmness)).thenReturn(berryFirmness);

        BerryFirmness result = berryFirmnessService.save(berryFirmness);

        assertSame(berryFirmness, result);
        verify(berryFirmnessRepository).save(berryFirmness);
    }

    @Test
    void update_usesRepository() {
        BerryFirmness berryFirmness = new BerryFirmness();
        when(berryFirmnessRepository.save(berryFirmness)).thenReturn(berryFirmness);

        BerryFirmness result = berryFirmnessService.update(berryFirmness);

        assertSame(berryFirmness, result);
        verify(berryFirmnessRepository).save(berryFirmness);
    }

    @Test
    void removeById_callsRepository() {
        berryFirmnessService.removeById(1L);
        verify(berryFirmnessRepository).deleteById(1L);
    }
}
