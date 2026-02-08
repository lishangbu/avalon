package io.github.lishangbu.avalon.pokeapi.util;

import static org.junit.jupiter.api.Assertions.*;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import org.junit.jupiter.api.Test;

class NamedApiResourceUtilsTest {

  @Test
  void testGetIdValidUrl() {
    NamedApiResource namedApiResource = new NamedApiResource("berry", "/api/v2/berry/9/");
    Integer id = NamedApiResourceUtils.getId(namedApiResource);
    // Assert that the extracted ID is correct
    assertNotNull(id);
    assertEquals(9, id);
  }

  @Test
  void testGetIdValidUrlWithBerryFirmness() {
    NamedApiResource namedApiResource =
        new NamedApiResource("berry-firmness", "/api/v2/berry-firmness/5/");
    Integer id = NamedApiResourceUtils.getId(namedApiResource);
    // Assert that the extracted ID is correct
    assertNotNull(id);
    assertEquals(5, id);
  }

  @Test
  void testGetIdInvalidUrl() {
    NamedApiResource namedApiResource = new NamedApiResource("", "/api/v2/unknown/9/");
    Integer id = NamedApiResourceUtils.getId(namedApiResource);

    // Assert that no ID is found
    assertNull(id);
  }

  @Test
  void testGetIdUrlWithInvalidNumber() {
    NamedApiResource namedApiResource = new NamedApiResource("berry", "/api/v2/berry/aptx4869/");

    Integer id = NamedApiResourceUtils.getId(namedApiResource);

    // Assert that invalid number leads to null return
    assertNull(id);
  }
}
