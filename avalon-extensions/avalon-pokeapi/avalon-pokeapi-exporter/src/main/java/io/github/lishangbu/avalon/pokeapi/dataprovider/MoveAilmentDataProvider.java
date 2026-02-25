package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.MoveAilmentExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveAilment;
import org.springframework.stereotype.Service;

/// 招式异常数据提供者
///
/// @author lishangbu
/// @since 2026/2/9
@Service
public class MoveAilmentDataProvider
        extends AbstractPokeApiDataProvider<MoveAilment, MoveAilmentExcelDTO> {

    @Override
    public MoveAilmentExcelDTO convert(MoveAilment moveAilment) {
        MoveAilmentExcelDTO result = new MoveAilmentExcelDTO();
        result.setId(moveAilment.id());
        result.setInternalName(moveAilment.name());
        result.setName(moveAilment.name());
        return result;
    }
}
