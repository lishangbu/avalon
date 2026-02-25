package io.github.lishangbu.avalon.pokeapi.writer;

import cn.idev.excel.FastExcel;
import io.github.lishangbu.avalon.pokeapi.TestEnvironmentApplication;
import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.PokemonStatExcelDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {TestEnvironmentApplication.class})
class PokemonStatDataWriterTest {
    @Resource private PokeApiDataProvider<PokemonStatExcelDTO> pokemonStatDataProvider;

    @Test
    void write_shouldFetchAndDelegateToSheetWriter() {
        FastExcel.write("POKEMON_STAT.xlsx", PokemonStatExcelDTO.class)
                .sheet("Sheet1")
                .doWrite(
                        () ->
                                pokemonStatDataProvider.fetch(
                                        PokeDataTypeEnum.POKEMON, PokemonStatExcelDTO.class));
    }
}
