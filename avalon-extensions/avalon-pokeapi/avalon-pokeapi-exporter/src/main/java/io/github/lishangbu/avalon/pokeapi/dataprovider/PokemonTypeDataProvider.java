package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.PokemonTypeExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Pokemon;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/// 宝可梦属性数据提供者
///
/// @author lishangbu
/// @since 2026/2/16
@Service
public class PokemonTypeDataProvider implements PokeApiDataProvider<PokemonTypeExcelDTO> {
    @Autowired protected PokeApiService pokeApiService;

    @Override
    public List<PokemonTypeExcelDTO> fetch(
            PokeDataTypeEnum typeEnum, Class<PokemonTypeExcelDTO> type) {
        NamedAPIResourceList namedAPIResourceList =
                pokeApiService.listNamedAPIResources(PokeDataTypeEnum.POKEMON);
        List<PokemonTypeExcelDTO> result = new ArrayList<>();
        namedAPIResourceList
                .results()
                .forEach(
                        namedApiResource -> {
                            Pokemon pokemon =
                                    (Pokemon)
                                            pokeApiService.getEntityFromUri(
                                                    PokeDataTypeEnum.POKEMON,
                                                    NamedApiResourceUtils.getId(namedApiResource));
                            result.addAll(
                                    pokemon.types().stream()
                                            .map(
                                                    pokemonType -> {
                                                        PokemonTypeExcelDTO tmp =
                                                                new PokemonTypeExcelDTO();
                                                        tmp.setTypeId(
                                                                NamedApiResourceUtils.getId(
                                                                        pokemonType.type()));
                                                        tmp.setPokemonId(pokemon.id());
                                                        tmp.setSlot(pokemonType.slot());
                                                        return tmp;
                                                    })
                                            .toList());
                        });
        return result;
    }
}
