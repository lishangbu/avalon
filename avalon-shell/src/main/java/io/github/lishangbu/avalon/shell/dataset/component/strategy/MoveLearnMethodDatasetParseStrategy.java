package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.dataset.entity.MoveLearnMethod;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.stereotype.Service;

/**
 * 招式学习方法数据处理策略
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Service
public class MoveLearnMethodDatasetParseStrategy implements BasicDataSetParseStrategy {

  @Override
  public Object convertToEntity(Object singleResource) {
    if (singleResource
        instanceof
        io.github.lishangbu.avalon.pokeapi.model.move.MoveLearnMethod moveLearnMethodData) {
      MoveLearnMethod moveLearnMethod = new MoveLearnMethod();
      moveLearnMethod.setId(moveLearnMethodData.id().longValue());
      moveLearnMethod.setInternalName(moveLearnMethodData.name());
      LocalizationUtils.getLocalizationName(moveLearnMethodData.names())
          .ifPresent(
              name -> {
                moveLearnMethod.setName(name.name());
              });
      LocalizationUtils.getLocalizationDescription(moveLearnMethodData.descriptions())
          .ifPresent(description -> moveLearnMethod.setDescription(description.description()));
      return moveLearnMethod;
    }
    return null;
  }

  @Override
  public PokeApiDataTypeEnum getDataType() {
    return PokeApiDataTypeEnum.MOVE_LEARN_METHOD;
  }
}
