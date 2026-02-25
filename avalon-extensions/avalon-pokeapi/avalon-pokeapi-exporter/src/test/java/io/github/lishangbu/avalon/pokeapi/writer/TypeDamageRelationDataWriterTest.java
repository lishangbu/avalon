package io.github.lishangbu.avalon.pokeapi.writer;

import cn.idev.excel.FastExcel;
import io.github.lishangbu.avalon.pokeapi.TestEnvironmentApplication;
import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.TypeDamageRelationExcelDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {TestEnvironmentApplication.class})
class TypeDamageRelationDataWriterTest {
    @Resource
    private PokeApiDataProvider<TypeDamageRelationExcelDTO> typeDamageRelationDataProvider;

    @Test
    void write_shouldFetchAndDelegateToSheetWriter() {
        FastExcel.write("TYPE_DAMAGE_RELATION.xlsx", TypeDamageRelationExcelDTO.class)
                .sheet("Sheet1")
                .doWrite(
                        () ->
                                typeDamageRelationDataProvider.fetch(
                                        PokeDataTypeEnum.TYPE, TypeDamageRelationExcelDTO.class));
    }
}
