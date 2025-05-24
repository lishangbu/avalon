package io.github.lishangbu.avalon.pokeapi.util;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PokeApiFactoryTest {
  @Mock private PokeApiService pokeApiService;

  private PokeApiFactory pokeApiFactory;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    pokeApiFactory = new PokeApiFactory(pokeApiService);
  }

  @Test
  void testGetPagedResourceCacheMissAndHit() {
    PokeApiEndpointEnum endpoint = PokeApiEndpointEnum.TYPE;
    int offset = 0;
    int limit = 10;
    NamedAPIResourceList mockList = mock(NamedAPIResourceList.class);
    when(pokeApiService.listNamedAPIResources(endpoint.getUri(), offset, limit))
        .thenReturn(mockList);

    // 第一次调用，缓存未命中
    NamedAPIResourceList result1 = pokeApiFactory.getPagedResource(endpoint, offset, limit);
    assertSame(mockList, result1);
    verify(pokeApiService, times(1)).listNamedAPIResources(endpoint.getUri(), offset, limit);

    // 第二次调用，缓存命中
    NamedAPIResourceList result2 = pokeApiFactory.getPagedResource(endpoint, offset, limit);
    assertSame(mockList, result2);
    // 依然只调用一次
    verify(pokeApiService, times(1)).listNamedAPIResources(endpoint.getUri(), offset, limit);
  }

  @Test
  void testGetSingleResourceCacheMissAndHit() {
    PokeApiEndpointEnum endpoint = PokeApiEndpointEnum.TYPE;
    Object[] uriVars = new Object[] {1};
    Object mockObj = new Object();
    when(pokeApiService.getEntityFromUri(endpoint.getResponseType(), endpoint.getUri(), uriVars))
        .thenReturn(mockObj);

    // 第一次调用，缓存未命中
    Object result1 = pokeApiFactory.getSingleResource(endpoint, uriVars);
    assertSame(mockObj, result1);
    verify(pokeApiService, times(1))
        .getEntityFromUri(endpoint.getResponseType(), endpoint.getUri(), uriVars);

    // 第二次调用，缓存命中
    Object result2 = pokeApiFactory.getSingleResource(endpoint, uriVars);
    assertSame(mockObj, result2);
    // 依然只调用一次
    verify(pokeApiService, times(1))
        .getEntityFromUri(endpoint.getResponseType(), endpoint.getUri(), uriVars);
  }

  @Test
  void testGetPagedResourceNullResultNotCached() {
    PokeApiEndpointEnum endpoint = PokeApiEndpointEnum.TYPE;
    int offset = 0;
    int limit = 10;
    when(pokeApiService.listNamedAPIResources(endpoint.getUri(), offset, limit)).thenReturn(null);
    NamedAPIResourceList result1 = pokeApiFactory.getPagedResource(endpoint, offset, limit);
    assertNull(result1);
    // 再次调用依然会调用service
    NamedAPIResourceList result2 = pokeApiFactory.getPagedResource(endpoint, offset, limit);
    assertNull(result2);
    verify(pokeApiService, times(2)).listNamedAPIResources(endpoint.getUri(), offset, limit);
  }

  @Test
  void testGetSingleResourceNullResultNotCached() {
    PokeApiEndpointEnum endpoint = PokeApiEndpointEnum.TYPE;
    Object[] uriVars = new Object[] {1};
    when(pokeApiService.getEntityFromUri(endpoint.getResponseType(), endpoint.getUri(), uriVars))
        .thenReturn(null);
    Object result1 = pokeApiFactory.getSingleResource(endpoint, uriVars);
    assertNull(result1);
    // 再次调用依然会调用service
    Object result2 = pokeApiFactory.getSingleResource(endpoint, uriVars);
    assertNull(result2);
    verify(pokeApiService, times(2))
        .getEntityFromUri(endpoint.getResponseType(), endpoint.getUri(), uriVars);
  }
}
