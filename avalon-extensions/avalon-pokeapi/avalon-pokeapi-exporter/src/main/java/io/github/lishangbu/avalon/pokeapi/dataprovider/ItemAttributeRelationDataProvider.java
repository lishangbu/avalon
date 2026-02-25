package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.ItemAttributeRelationExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.item.Item;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/// 道具属性关联数据提供者
///
/// @author lishangbu
/// @since 2026/2/16
@Service
public class ItemAttributeRelationDataProvider
        implements PokeApiDataProvider<ItemAttributeRelationExcelDTO> {
    @Autowired protected PokeApiService pokeApiService;

    @Override
    public List<ItemAttributeRelationExcelDTO> fetch(
            PokeDataTypeEnum typeEnum, Class<ItemAttributeRelationExcelDTO> type) {
        NamedAPIResourceList namedAPIResourceList =
                pokeApiService.listNamedAPIResources(PokeDataTypeEnum.ITEM);
        List<ItemAttributeRelationExcelDTO> result = new ArrayList<>();
        namedAPIResourceList
                .results()
                .forEach(
                        namedApiResource -> {
                            Item item =
                                    (Item)
                                            pokeApiService.getEntityFromUri(
                                                    PokeDataTypeEnum.ITEM,
                                                    NamedApiResourceUtils.getId(namedApiResource));
                            result.addAll(
                                    item.attributes().stream()
                                            .map(
                                                    itemAttribute -> {
                                                        ItemAttributeRelationExcelDTO tmp =
                                                                new ItemAttributeRelationExcelDTO();
                                                        tmp.setAttributeId(
                                                                NamedApiResourceUtils.getId(
                                                                        itemAttribute));
                                                        tmp.setItemId(item.id());
                                                        return tmp;
                                                    })
                                            .toList());
                        });
        return result;
    }
}
