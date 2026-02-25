package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.MoveLearnMethodExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveLearnMethod;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.stereotype.Service;

/// 招式学习方法数据提供者
///
/// @author lishangbu
/// @since 2026/2/4
@Service
public class MoveLearnMethodDataProvider
        extends AbstractPokeApiDataProvider<MoveLearnMethod, MoveLearnMethodExcelDTO> {

    @Override
    public MoveLearnMethodExcelDTO convert(MoveLearnMethod moveLearnMethod) {
        MoveLearnMethodExcelDTO result = new MoveLearnMethodExcelDTO();
        result.setId(moveLearnMethod.id());
        result.setInternalName(moveLearnMethod.name());
        result.setName(resolveLocalizedName(moveLearnMethod.names(), moveLearnMethod.name()));
        LocalizationUtils.getLocalizationDescription(moveLearnMethod.descriptions())
                .ifPresent(
                        description -> {
                            result.setDescription(description.description());
                        });
        return result;
    }
}
