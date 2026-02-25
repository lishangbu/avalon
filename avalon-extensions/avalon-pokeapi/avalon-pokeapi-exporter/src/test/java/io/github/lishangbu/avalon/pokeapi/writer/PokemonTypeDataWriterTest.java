package io.github.lishangbu.avalon.pokeapi.writer;

import cn.idev.excel.FastExcel;
import io.github.lishangbu.avalon.pokeapi.TestEnvironmentApplication;
import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.PokemonTypeExcelDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {TestEnvironmentApplication.class})
class PokemonTypeDataWriterTest {
    @Resource private PokeApiDataProvider<PokemonTypeExcelDTO> pokemonTypeDataProvider;

    @Test
    void write_shouldFetchAndDelegateToSheetWriter() {
        FastExcel.write("POKEMON_TYPE.xlsx", PokemonTypeExcelDTO.class)
                .sheet("Sheet1")
                .doWrite(
                        () ->
                                pokemonTypeDataProvider.fetch(
                                        PokeDataTypeEnum.POKEMON, PokemonTypeExcelDTO.class));
    }
}
