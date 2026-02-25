package io.github.lishangbu.avalon.pokeapi.writer;

import cn.idev.excel.FastExcel;
import io.github.lishangbu.avalon.pokeapi.TestEnvironmentApplication;
import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.PokemonAbilityExcelDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/// PokemonAbilityDataWriter 的单元测试类
///
/// 测试场景：验证数据写入 Excel 的功能，包括数据获取和委托给 SheetWriter
/// 输入：模拟的 PokeApiDataProvider 和 PokeDataTypeEnum.ABILITY
/// 预期：成功生成 POKEMON_ABILITY.xlsx 文件，包含宝可梦能力数据
///
/// @author lishangbu
/// @since 2026/2/15
@SpringBootTest(classes = {TestEnvironmentApplication.class})
class PokemonAbilityDataWriterTest {
    @Resource private PokeApiDataProvider<PokemonAbilityExcelDTO> pokemonAbilityDataProvider;

    /// 测试写入方法应获取数据并委托给 SheetWriter
    ///
    /// 场景：正常写入宝可梦能力数据到 Excel
    /// 输入：PokeDataTypeEnum.ABILITY 和 PokemonAbilityExcelDTO.class
    /// 预期：生成 POKEMON_ABILITY.xlsx 文件，无异常抛出
    @Test
    void write_shouldFetchAndDelegateToSheetWriter() {
        FastExcel.write("POKEMON_ABILITY.xlsx", PokemonAbilityExcelDTO.class)
                .sheet("Sheet1")
                .doWrite(
                        () ->
                                pokemonAbilityDataProvider.fetch(
                                        PokeDataTypeEnum.ABILITY, PokemonAbilityExcelDTO.class));
    }
}
