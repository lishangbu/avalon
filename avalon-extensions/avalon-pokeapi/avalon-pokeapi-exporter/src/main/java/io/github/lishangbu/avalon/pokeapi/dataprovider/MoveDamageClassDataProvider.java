package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.MoveDamageClassExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveDamageClass;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import org.springframework.stereotype.Service;

/// 招式伤害类别数据提供者
///
/// @author lishangbu
/// @since 2026/2/4
@Service
public class MoveDamageClassDataProvider
        extends AbstractPokeApiDataProvider<MoveDamageClass, MoveDamageClassExcelDTO> {

    @Override
    public MoveDamageClassExcelDTO convert(MoveDamageClass moveDamageClass) {
        MoveDamageClassExcelDTO result = new MoveDamageClassExcelDTO();
        result.setId(moveDamageClass.id());
        result.setInternalName(moveDamageClass.name());
        result.setName(resolveLocalizedName(moveDamageClass.names(), moveDamageClass.name()));
        LocalizationUtils.getLocalizationDescription(moveDamageClass.descriptions())
                .ifPresent(
                        description -> {
                            result.setDescription(description.description());
                        });
        return result;
    }
}
