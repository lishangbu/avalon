package io.github.lishangbu.avalon.dataset.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import io.github.lishangbu.avalon.dataset.service.BerryFirmnessService;
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
class BerryFirmnessControllerTest {
    @Mock private BerryFirmnessService berryFirmnessService;
    @InjectMocks private BerryFirmnessController berryFirmnessController;

    @Test
    void getBerryFirmnessPage_delegatesToService() {
        BerryFirmness berryFirmness = new BerryFirmness();
        Pageable pageable = PageRequest.of(0, 5);
        Page<BerryFirmness> page = new PageImpl<>(List.of(berryFirmness));
        when(berryFirmnessService.getPageByCondition(any(BerryFirmness.class), eq(pageable)))
                .thenReturn(page);

        Page<BerryFirmness> result =
                berryFirmnessController.getBerryFirmnessPage(pageable, berryFirmness);

        assertSame(page, result);
        verify(berryFirmnessService).getPageByCondition(berryFirmness, pageable);
    }

    @Test
    void listBerryFirmnesses_delegatesToService() {
        BerryFirmness berryFirmness = new BerryFirmness();
        List<BerryFirmness> list = List.of(berryFirmness);
        when(berryFirmnessService.listByCondition(berryFirmness)).thenReturn(list);

        List<BerryFirmness> result = berryFirmnessController.listBerryFirmnesses(berryFirmness);

        assertSame(list, result);
        verify(berryFirmnessService).listByCondition(berryFirmness);
    }

    @Test
    void save_delegatesToService() {
        BerryFirmness berryFirmness = new BerryFirmness();
        when(berryFirmnessService.save(berryFirmness)).thenReturn(berryFirmness);

        BerryFirmness result = berryFirmnessController.save(berryFirmness);

        assertSame(berryFirmness, result);
        verify(berryFirmnessService).save(berryFirmness);
    }

    @Test
    void update_delegatesToService() {
        BerryFirmness berryFirmness = new BerryFirmness();
        when(berryFirmnessService.update(berryFirmness)).thenReturn(berryFirmness);

        BerryFirmness result = berryFirmnessController.update(berryFirmness);

        assertSame(berryFirmness, result);
        verify(berryFirmnessService).update(berryFirmness);
    }

    @Test
    void deleteById_delegatesToService() {
        berryFirmnessController.deleteById(1L);
        verify(berryFirmnessService).removeById(1L);
    }
}
