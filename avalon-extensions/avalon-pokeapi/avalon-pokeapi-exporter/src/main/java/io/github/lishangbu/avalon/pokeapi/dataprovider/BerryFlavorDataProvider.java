package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.BerryFlavorExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.berry.BerryFlavor;
import org.springframework.stereotype.Service;

/// 树果风味数据提供者
///
/// @author lishangbu
/// @since 2026/2/4
@Service
public class BerryFlavorDataProvider
        extends AbstractPokeApiDataProvider<BerryFlavor, BerryFlavorExcelDTO> {

    @Override
    public BerryFlavorExcelDTO convert(BerryFlavor berryFlavor) {
        BerryFlavorExcelDTO result = new BerryFlavorExcelDTO();
        result.setId(berryFlavor.id());
        result.setInternalName(berryFlavor.name());
        result.setName(resolveLocalizedName(berryFlavor.names(), berryFlavor.name()));
        return result;
    }
}
