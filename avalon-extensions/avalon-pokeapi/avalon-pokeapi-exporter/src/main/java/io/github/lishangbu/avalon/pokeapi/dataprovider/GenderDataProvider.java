package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.GenderExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Gender;
import org.springframework.stereotype.Service;

/// 性别数据提供者
///
/// @author lishangbu
/// @since 2026/2/4
@Service
public class GenderDataProvider extends AbstractPokeApiDataProvider<Gender, GenderExcelDTO> {

  @Override
  public GenderExcelDTO convert(Gender gender) {
    GenderExcelDTO result = new GenderExcelDTO();
    result.setId(gender.id());
    result.setInternalName(gender.name());
    return result;
  }
}
