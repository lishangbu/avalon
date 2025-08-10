package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.stereotype.Service;

/**
 * 招式伤害类别数据处理策略
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Service
public class MoveDamageClassDatasetParseStrategy implements BasicDataSetParseStrategy {

  @Override
  public Object convertToEntity(Object singleResource) {
    if (singleResource
        instanceof
        io.github.lishangbu.avalon.pokeapi.model.move.MoveDamageClass moveDamageClassData) {

      MoveDamageClass moveDamageClass = new MoveDamageClass();
      moveDamageClass.setId(moveDamageClassData.id().longValue());
      moveDamageClass.setInternalName(moveDamageClassData.name());
      LocalizationUtils.getLocalizationName(moveDamageClassData.names())
          .ifPresent(
              name -> {
                moveDamageClass.setName(name.name());
              });
      LocalizationUtils.getLocalizationDescription(moveDamageClassData.descriptions())
          .ifPresent(description -> moveDamageClass.setDescription(description.description()));
      return moveDamageClass;
    }

    return null;
  }

  @Override
  public PokeApiDataTypeEnum getDataType() {
    return PokeApiDataTypeEnum.MOVE_DAMAGE_CLASS;
  }
}
