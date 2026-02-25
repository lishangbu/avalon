package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.ItemAttributeExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.item.ItemAttribute;
import org.springframework.stereotype.Service;

/// 道具属性数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class ItemAttributeDataProvider
        extends AbstractPokeApiDataProvider<ItemAttribute, ItemAttributeExcelDTO> {

    @Override
    public ItemAttributeExcelDTO convert(ItemAttribute itemAttribute) {
        ItemAttributeExcelDTO result = new ItemAttributeExcelDTO();
        result.setId(itemAttribute.id());
        result.setInternalName(itemAttribute.name());
        result.setName(resolveLocalizedName(itemAttribute.names(), itemAttribute.name()));
        result.setDescription(resolveLocalizedDescription(itemAttribute.descriptions()));
        return result;
    }
}
