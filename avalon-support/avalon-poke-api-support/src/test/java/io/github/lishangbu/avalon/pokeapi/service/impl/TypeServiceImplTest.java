package io.github.lishangbu.avalon.pokeapi.service.impl;

import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lishangbu.avalon.pokeapi.RestClientMockSupport;
import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.util.ResourceUtils;

/**
 * @author lishangbu
 * @since 2025/5/20
 */
class TypeServiceImplTest extends RestClientMockSupport {
  @InjectMocks private TypeServiceImpl typeService;

  @Test
  void testListTypes() throws IOException {
    String mockResource =
        Files.readString(ResourceUtils.getFile(MOCK_RESOURCE_PREFIX + "type.json").toPath());
    initRestClientMock();
    when(responseSpec.toEntity(NamedAPIResourceList.class)).thenReturn(responseEntity);
    when(responseEntity.getBody())
        .thenReturn(new ObjectMapper().readValue(mockResource, NamedAPIResourceList.class));
    NamedAPIResourceList typeList = typeService.listTypes(0, 1);
    Assertions.assertEquals("https://pokeapi.co/api/v2/type?offset=1&limit=1", typeList.next());
    Assertions.assertNull(typeList.previous());
    Assertions.assertEquals(21, typeList.count());
    Assertions.assertEquals(1, typeList.results().size());
    Assertions.assertEquals("normal", typeList.results().get(0).name());
    Assertions.assertEquals("https://pokeapi.co/api/v2/type/1/", typeList.results().get(0).url());
  }

  @Test
  void testGetType() throws IOException {
    String mockResource =
        Files.readString(ResourceUtils.getFile(MOCK_RESOURCE_PREFIX + "type-1.json").toPath());
    initRestClientMock();
    when(responseSpec.toEntity(Type.class)).thenReturn(responseEntity);
    when(responseEntity.getBody())
        .thenReturn(new ObjectMapper().readValue(mockResource, Type.class));
    Type type = typeService.getType(1);
    Assertions.assertEquals(1, type.id());
    Assertions.assertEquals("normal", type.name());
    Assertions.assertEquals("physical", type.moveDamageClass().name());
    // 进一步断言
    Assertions.assertNotNull(type.damageRelations());
    Assertions.assertFalse(type.damageRelations().doubleDamageFrom().isEmpty());
    Assertions.assertEquals("fighting", type.damageRelations().doubleDamageFrom().get(0).name());
    Assertions.assertNotNull(type.names());
    Assertions.assertTrue(
        type.names().stream()
            .anyMatch(n -> "zh-Hans".equals(n.language().name()) && "一般".equals(n.name())));
  }
}
