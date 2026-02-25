package io.github.lishangbu.avalon.pokeapi.writer;

import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.EvolutionChainExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.evolution.EvolutionChain;
import jakarta.annotation.Resource;

/// 进化链数据写入器测试
///
/// 测试 EvolutionChainDataWriter 的功能，包括数据获取和Excel写入
class EvolutionChainDataWriterTest extends AbstractExcelWriterTest {

    @Resource private PokeApiDataProvider<EvolutionChain> evolutionChainDataProvider;

    /// 返回数据类型枚举
    ///
    /// @return EVOLUTION_CHAIN 枚举值
    @Override
    PokeDataTypeEnum getDataTypeEnum() {
        return PokeDataTypeEnum.EVOLUTION_CHAIN;
    }

    /// 返回Excel数据传输对象类
    ///
    /// @return EvolutionChainExcelDTO.class
    @Override
    Class<EvolutionChainExcelDTO> getExcelClass() {
        return EvolutionChainExcelDTO.class;
    }

    /// 返回数据提供者
    ///
    /// @return 进化链数据提供者实例
    @Override
    PokeApiDataProvider<EvolutionChain> getDataProvider() {
        return evolutionChainDataProvider;
    }
}
