package io.github.lishangbu.avalon.dataset.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import io.github.lishangbu.avalon.dataset.service.BerryFlavorService;
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
class BerryFlavorControllerTest {
    @Mock private BerryFlavorService berryFlavorService;
    @InjectMocks private BerryFlavorController berryFlavorController;

    @Test
    void getBerryFlavorPage_delegatesToService() {
        BerryFlavor berryFlavor = new BerryFlavor();
        Pageable pageable = PageRequest.of(0, 5);
        Page<BerryFlavor> page = new PageImpl<>(List.of(berryFlavor));
        when(berryFlavorService.getPageByCondition(any(BerryFlavor.class), eq(pageable)))
                .thenReturn(page);

        Page<BerryFlavor> result = berryFlavorController.getBerryFlavorPage(pageable, berryFlavor);

        assertSame(page, result);
        verify(berryFlavorService).getPageByCondition(berryFlavor, pageable);
    }

    @Test
    void save_delegatesToService() {
        BerryFlavor berryFlavor = new BerryFlavor();
        when(berryFlavorService.save(berryFlavor)).thenReturn(berryFlavor);

        BerryFlavor result = berryFlavorController.save(berryFlavor);

        assertSame(berryFlavor, result);
        verify(berryFlavorService).save(berryFlavor);
    }

    @Test
    void update_delegatesToService() {
        BerryFlavor berryFlavor = new BerryFlavor();
        when(berryFlavorService.update(berryFlavor)).thenReturn(berryFlavor);

        BerryFlavor result = berryFlavorController.update(berryFlavor);

        assertSame(berryFlavor, result);
        verify(berryFlavorService).update(berryFlavor);
    }

    @Test
    void deleteById_delegatesToService() {
        berryFlavorController.deleteById(1L);
        verify(berryFlavorService).removeById(1L);
    }
}
