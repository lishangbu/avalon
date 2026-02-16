package io.github.lishangbu.avalon.pokeapi.writer;

import cn.idev.excel.FastExcel;
import io.github.lishangbu.avalon.pokeapi.TestEnvironmentApplication;
import io.github.lishangbu.avalon.pokeapi.dataprovider.PokeApiDataProvider;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.ItemAttributeRelationExcelDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {TestEnvironmentApplication.class})
class ItemAttributeRelationDataWriterTest {
  @Resource
  private PokeApiDataProvider<ItemAttributeRelationExcelDTO> itemAttributeRelationDataProvider;

  @Test
  void write_shouldFetchAndDelegateToSheetWriter() {
    FastExcel.write("ITEM_ATTRIBUTE_RELATION.xlsx", ItemAttributeRelationExcelDTO.class)
        .sheet("Sheet1")
        .doWrite(
            () ->
                itemAttributeRelationDataProvider.fetch(
                    PokeDataTypeEnum.ITEM, ItemAttributeRelationExcelDTO.class));
  }
}
