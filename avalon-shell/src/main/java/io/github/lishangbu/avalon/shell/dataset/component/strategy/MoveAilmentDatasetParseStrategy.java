package io.github.lishangbu.avalon.shell.dataset.component.strategy;

import io.github.lishangbu.avalon.dataset.entity.MoveAilment;
import io.github.lishangbu.avalon.dataset.repository.MoveAilmentRepository;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/**
 * 招式状态异常数据处理策略
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Service
public class MoveAilmentDatasetParseStrategy implements BasicDataSetParseStrategy {
  private final MoveAilmentRepository moveAilmentRepository;

  public MoveAilmentDatasetParseStrategy(MoveAilmentRepository moveAilmentRepository) {
    this.moveAilmentRepository = moveAilmentRepository;
  }

  @Override
  public Object convertToEntity(Object singleResource) {
    if (singleResource
        instanceof io.github.lishangbu.avalon.pokeapi.model.move.MoveAilment moveAilmentData) {
      MoveAilment moveAilment = new MoveAilment();
      moveAilment.setId(moveAilmentData.id());
      moveAilment.setInternalName(moveAilmentData.name());
      LocalizationUtils.getLocalizationName(moveAilmentData.names())
          .ifPresent(
              name -> {
                moveAilment.setName(name.name());
              });
      return moveAilment;
    }
    return null;
  }

  @Override
  public PokeApiDataTypeEnum getDataType() {
    return PokeApiDataTypeEnum.MOVE_AILMENT;
  }

  @Override
  public JpaRepository getRepository() {
    return this.moveAilmentRepository;
  }
}
