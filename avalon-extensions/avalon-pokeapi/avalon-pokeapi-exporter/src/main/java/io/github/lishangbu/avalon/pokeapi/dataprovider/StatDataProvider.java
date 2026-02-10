package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.StatExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Stat;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.stereotype.Service;

/// 属性数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class StatDataProvider extends AbstractPokeApiDataProvider<Stat, StatExcelDTO> {

  @Override
  public StatExcelDTO convert(Stat stat) {
    StatExcelDTO result = new StatExcelDTO();
    result.setId(stat.id());
    result.setInternalName(stat.name());
    result.setName(resolveLocalizedName(stat.names(), stat.name()));
    result.setGameIndex(stat.gameIndex());
    result.setIsBattleOnly(stat.isBattleOnly());
    result.setMoveDamageClassId(NamedApiResourceUtils.getId(stat.moveDamageClass()));
    return result;
  }
}
