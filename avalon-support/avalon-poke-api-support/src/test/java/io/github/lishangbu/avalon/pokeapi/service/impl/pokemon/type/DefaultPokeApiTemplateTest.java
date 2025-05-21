package io.github.lishangbu.avalon.pokeapi.service.impl.pokemon.type;

import io.github.lishangbu.avalon.pokeapi.autoconfiguration.PokeApiRestClientAutoConfiguration;
import io.github.lishangbu.avalon.pokeapi.autoconfiguration.PokeApiTemplateAutoConfiguration;
import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type;
import io.github.lishangbu.avalon.pokeapi.service.PokeApiTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author lishangbu
 * @since 2025/5/20
 */
@SpringBootTest(
    classes = {PokeApiRestClientAutoConfiguration.class, PokeApiTemplateAutoConfiguration.class})
class DefaultPokeApiTemplateTest {
  @Autowired private PokeApiTemplate pokeApiTemplate;

  @Test
  void listTypes() {
    NamedAPIResourceList typeList = pokeApiTemplate.listTypes(0, 100);
    Assertions.assertNull(typeList.next());
    Assertions.assertNull(typeList.previous());
    Assertions.assertEquals(21, typeList.count());
    Assertions.assertEquals(21, typeList.results().size());
  }

  @Test
  void getType() {
    Type type = pokeApiTemplate.getType(1);
    Assertions.assertEquals(1, type.id());
    Assertions.assertEquals("normal", type.name());
    Assertions.assertEquals("physical", type.moveDamageClass().name());
  }

  @Test
  void getTypeByName() {
    Type type = pokeApiTemplate.getType("normal");
    Assertions.assertEquals(1, type.id());
    Assertions.assertEquals("normal", type.name());
    Assertions.assertEquals("physical", type.moveDamageClass().name());
  }
}
