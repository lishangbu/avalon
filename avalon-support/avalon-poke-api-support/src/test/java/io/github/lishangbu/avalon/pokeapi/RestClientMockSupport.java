package io.github.lishangbu.avalon.pokeapi;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestClient;

/**
 * Rest Client 模拟支持类
 *
 * @author lishangbu
 * @since 2025/5/22
 */
public abstract class RestClientMockSupport {
  @Mock protected RestClient restClient;
  @Mock protected RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
  @Mock protected RestClient.RequestHeadersSpec requestHeadersSpec;
  @Mock protected RestClient.ResponseSpec responseSpec;
  @Mock protected ResponseEntity responseEntity;

  protected static final String MOCK_RESOURCE_PREFIX =
      ResourceUtils.CLASSPATH_URL_PREFIX + "pokeapi/";

  protected void initRestClientMock() {
    MockitoAnnotations.openMocks(this);
    when(restClient.get()).thenReturn(requestHeadersUriSpec);
    // mock uri(String)
    when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
    // mock uri(String, Object...)，用 ArgumentMatchers.<Object>any() 替代 anyVararg()
    when(requestHeadersUriSpec.uri(anyString(), org.mockito.ArgumentMatchers.<Object>any()))
        .thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
  }
}
