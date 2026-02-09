package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.MoveCategoryExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveCategory;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.stereotype.Service;

/// 招式分类数据提供者
///
/// @author lishangbu
/// @since 2026/2/4
@Service
public class MoveCategoryDataProvider
    extends AbstractPokeApiDataProvider<MoveCategory, MoveCategoryExcelDTO> {

  @Override
  public MoveCategoryExcelDTO convert(MoveCategory moveCategory) {
    MoveCategoryExcelDTO result = new MoveCategoryExcelDTO();
    result.setId(moveCategory.id());
    result.setInternalName(moveCategory.name());
    result.setName(moveCategory.name());
    LocalizationUtils.getLocalizationDescription(moveCategory.descriptions())
        .ifPresent(
            description -> {
              result.setDescription(description.description());
            });
    return result;
  }
}
