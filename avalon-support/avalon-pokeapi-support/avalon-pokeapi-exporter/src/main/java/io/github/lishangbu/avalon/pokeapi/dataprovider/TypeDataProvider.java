package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.TypeExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;
import org.springframework.stereotype.Service;

/// 属性数据提供者
///
/// @author lishangbu
/// @since 2026/2/4
@Service
public class TypeDataProvider extends AbstractPokeApiDataProvider<Type, TypeExcelDTO> {

  @Override
  public TypeExcelDTO convert(Type type) {
    TypeExcelDTO result = new TypeExcelDTO();
    result.setId(type.id());
    result.setName(type.name());
    result.setDisplayName(resolveLocalizedNameFromNames(type.names(), type.name()));
    return result;
  }
}
