package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.dataset.entity.MoveCategory;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.stereotype.Service;

/**
 * 招式分类数据处理策略
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Service
public class MoveCategoryDatasetParseStrategy implements BasicDataSetParseStrategy {

  @Override
  public Object convertToEntity(Object singleResource) {

    if (singleResource
        instanceof io.github.lishangbu.avalon.pokeapi.model.move.MoveCategory moveCategoryData) {
      MoveCategory moveCategory = new MoveCategory();
      moveCategory.setId(moveCategoryData.id().longValue());
      moveCategory.setInternalName(moveCategoryData.name());
      moveCategory.setName(moveCategoryData.name());
      LocalizationUtils.getLocalizationDescription(moveCategoryData.descriptions())
          .ifPresent(description -> moveCategory.setDescription(description.description()));
      return moveCategory;
    }

    return null;
  }

  @Override
  public PokeApiDataTypeEnum getDataType() {
    return PokeApiDataTypeEnum.MOVE_CATEGORY;
  }
}
