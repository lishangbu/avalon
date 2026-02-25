package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.MoveTargetExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveTarget;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.stereotype.Service;

/// 招式目标数据提供者
///
/// @author lishangbu
/// @since 2026/2/9
@Service
public class MoveTargetDataProvider
        extends AbstractPokeApiDataProvider<MoveTarget, MoveTargetExcelDTO> {

    @Override
    public MoveTargetExcelDTO convert(MoveTarget moveTarget) {
        MoveTargetExcelDTO result = new MoveTargetExcelDTO();
        result.setId(moveTarget.id());
        result.setInternalName(moveTarget.name());
        result.setName(moveTarget.name());
        LocalizationUtils.getLocalizationDescription(moveTarget.descriptions())
                .ifPresent(description -> result.setDescription(description.description()));
        return result;
    }
}
