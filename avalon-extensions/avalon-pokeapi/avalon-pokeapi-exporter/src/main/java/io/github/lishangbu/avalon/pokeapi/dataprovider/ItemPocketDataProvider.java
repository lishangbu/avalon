package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.ItemPocketExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.item.ItemPocket;
import org.springframework.stereotype.Service;

/// 道具口袋数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class ItemPocketDataProvider
        extends AbstractPokeApiDataProvider<ItemPocket, ItemPocketExcelDTO> {

    @Override
    public ItemPocketExcelDTO convert(ItemPocket itemPocket) {
        ItemPocketExcelDTO result = new ItemPocketExcelDTO();
        result.setId(itemPocket.id());
        result.setInternalName(itemPocket.name());
        result.setName(resolveLocalizedName(itemPocket.names(), itemPocket.name()));
        return result;
    }
}
