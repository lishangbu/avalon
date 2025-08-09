package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.dataset.entity.MoveTarget;
import io.github.lishangbu.avalon.dataset.repository.MoveTargetRepository;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/**
 * 招式目标数据处理策略
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Service
public class MoveTargetDatasetParseStrategy implements BasicDataSetParseStrategy {
  private final MoveTargetRepository moveTargetRepository;

  public MoveTargetDatasetParseStrategy(MoveTargetRepository moveTargetRepository) {
    this.moveTargetRepository = moveTargetRepository;
  }

  @Override
  public Object convertToEntity(Object singleResource) {
    if (singleResource
        instanceof io.github.lishangbu.avalon.pokeapi.model.move.MoveTarget moveTargetData) {
      MoveTarget moveTarget = new MoveTarget();
      moveTarget.setId(moveTargetData.id());
      moveTarget.setInternalName(moveTargetData.name());
      LocalizationUtils.getLocalizationName(moveTargetData.names())
          .ifPresentOrElse(
              name -> {
                moveTarget.setName(name.name());
              },
              () -> moveTarget.setName(moveTargetData.name()));
      LocalizationUtils.getLocalizationDescription(moveTargetData.descriptions())
          .ifPresentOrElse(
              description -> moveTarget.setDescription(description.description()),
              () -> moveTarget.setDescription(""));
      return moveTarget;
    }
    return null;
  }

  @Override
  public PokeApiDataTypeEnum getDataType() {
    return PokeApiDataTypeEnum.MOVE_TARGET;
  }

  @Override
  public JpaRepository getRepository() {
    return this.moveTargetRepository;
  }
}
