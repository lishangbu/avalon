package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.MoveBattleStyleExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveBattleStyle;
import org.springframework.stereotype.Service;

/// 招式战斗风格数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class MoveBattleStyleDataProvider
    extends AbstractPokeApiDataProvider<MoveBattleStyle, MoveBattleStyleExcelDTO> {

  @Override
  public MoveBattleStyleExcelDTO convert(MoveBattleStyle moveBattleStyle) {
    MoveBattleStyleExcelDTO result = new MoveBattleStyleExcelDTO();
    result.setId(moveBattleStyle.id());
    result.setInternalName(moveBattleStyle.name());
    result.setName(resolveLocalizedName(moveBattleStyle.names(), moveBattleStyle.name()));
    return result;
  }
}
