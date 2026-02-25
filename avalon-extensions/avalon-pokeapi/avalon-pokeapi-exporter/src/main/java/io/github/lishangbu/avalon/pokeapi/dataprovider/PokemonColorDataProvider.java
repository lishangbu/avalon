package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.PokemonColorExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.PokemonColor;
import org.springframework.stereotype.Service;

/// 宝可梦颜色数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class PokemonColorDataProvider
        extends AbstractPokeApiDataProvider<PokemonColor, PokemonColorExcelDTO> {

    @Override
    public PokemonColorExcelDTO convert(PokemonColor pokemonColor) {
        PokemonColorExcelDTO result = new PokemonColorExcelDTO();
        result.setId(pokemonColor.id());
        result.setInternalName(pokemonColor.name());
        result.setName(resolveLocalizedName(pokemonColor.names(), pokemonColor.name()));
        return result;
    }
}
