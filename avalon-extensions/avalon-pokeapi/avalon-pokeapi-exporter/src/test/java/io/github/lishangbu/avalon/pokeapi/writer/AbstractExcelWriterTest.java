package io.github.lishangbu.avalon.pokeapi.writer;

import cn.idev.excel.FastExcel;
import io.github.lishangbu.avalon.pokeapi.TestEnvironmentApplication;
import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {TestEnvironmentApplication.class})
abstract class AbstractExcelWriterTest {

    @Test
    void write_shouldFetchAndDelegateToSheetWriter() {
        String fileName = getDataTypeEnum().name() + ".xlsx";

        FastExcel.write(fileName, getExcelClass())
                .sheet("Sheet1")
                .doWrite(() -> getDataProvider().fetch(getDataTypeEnum(), getExcelClass()));
    }

    abstract PokeDataTypeEnum getDataTypeEnum();

    abstract Class getExcelClass();

    abstract PokeApiDataProvider getDataProvider();
}
