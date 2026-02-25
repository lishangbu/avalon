package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.AbilityExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Ability;
import jakarta.annotation.Resource;

/// 特性数据写入器测试
///
/// 测试 AbilityDataWriter 的功能，包括数据获取和Excel写入
class AbilityDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<Ability> abilityDataProvider;

    /// 返回数据类型枚举
    ///
    /// @return ABILITY 枚举值
    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.ABILITY;
    }

    /// 返回Excel数据传输对象类
    ///
    /// @return AbilityExcelDTO.class
    @Override
    Class<AbilityExcelDTO> getExcelClass() {
        return AbilityExcelDTO.class;
    }

    /// 返回数据提供者
    ///
    /// @return 特性数据提供者实例
    @Override
    PokeApiDataProvider<Ability> getDataProvider() {
        return abilityDataProvider;
    }
}
