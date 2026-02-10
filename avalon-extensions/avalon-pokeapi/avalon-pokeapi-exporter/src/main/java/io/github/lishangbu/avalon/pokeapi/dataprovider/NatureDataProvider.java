package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.NatureExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Nature;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.stereotype.Service;

/// 性格数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class NatureDataProvider extends AbstractPokeApiDataProvider<Nature, NatureExcelDTO> {

  @Override
  public NatureExcelDTO convert(Nature nature) {
    NatureExcelDTO result = new NatureExcelDTO();
    result.setId(nature.id());
    result.setInternalName(nature.name());
    result.setName(resolveLocalizedName(nature.names(), nature.name()));
    result.setDecreasedStatId(NamedApiResourceUtils.getId(nature.decreasedStat()));
    result.setIncreasedStatId(NamedApiResourceUtils.getId(nature.increasedStat()));
    result.setHatesFlavorId(NamedApiResourceUtils.getId(nature.hatesFlavor()));
    result.setLikesFlavorId(NamedApiResourceUtils.getId(nature.likesFlavor()));
    return result;
  }
}
