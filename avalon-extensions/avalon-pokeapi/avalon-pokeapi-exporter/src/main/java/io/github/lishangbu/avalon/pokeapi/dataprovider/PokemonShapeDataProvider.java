package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.PokemonShapeExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.PokemonShape;
import org.springframework.stereotype.Service;

/// 宝可梦形状数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class PokemonShapeDataProvider
        extends AbstractPokeApiDataProvider<PokemonShape, PokemonShapeExcelDTO> {

    @Override
    public PokemonShapeExcelDTO convert(PokemonShape pokemonShape) {
        PokemonShapeExcelDTO result = new PokemonShapeExcelDTO();
        result.setId(pokemonShape.id());
        result.setInternalName(pokemonShape.name());
        result.setName(resolveLocalizedName(pokemonShape.names(), pokemonShape.name()));
        return result;
    }
}
