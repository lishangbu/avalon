---
applyTo: "*Test.java"
---

# 单元测试编写规范

## 基本要求

- 测试类命名以 *Test 结尾，放在 test 包对应目录
- 使用 JUnit 5 + Mockito 进行单元测试
- 测试方法命名清晰，体现测试场景
- 测试覆盖率应达到 80% 以上
- 每个测试方法只测试一个功能点
- 测试类和测试方法必须编写 JavaDoc 注释
- 使用静态导入简化断言和验证代码

## 命名规约

- 测试类：`被测试类名 + Test`
- 测试方法：
  - 复杂场景：`should + 预期结果 + When + 测试条件`
  - 简单场景：`test + 方法名` （仅限单一分支或简单断言）
- Mock对象：`mock + 类名`

### 示例命名

```java
// 测试类命名
UserServiceTest
UserControllerTest
UserRepositoryTest

// 测试方法命名 - 推荐方式
shouldReturnUserWhenValidIdProvided()
shouldThrowExceptionWhenUserNotFound()
shouldSaveUserSuccessfullyWhenValidDataProvided()

// 测试方法命名 - 简单场景可选方式
testGetUserById()
testSaveUser()
```

## 测试结构规范

使用 AAA（Arrange-Act-Assert）模式组织测试代码：

```java
@Test
void shouldReturnUserWhenValidIdProvided() {
    // Arrange - 准备测试数据
    Long userId = 1L;
    User expectedUser = new User(userId, "testUser");
    when(mockUserRepository.findById(userId)).thenReturn(Optional.of(expectedUser));
    
    // Act - 执行测试方法
    User actualUser = userService.getUserById(userId);
    
    // Assert - 验证结果
    assertThat(actualUser).isNotNull();
    assertThat(actualUser.getId()).isEqualTo(userId);
    assertThat(actualUser.getUsername()).isEqualTo("testUser");
}
```

## Service层测试规范

```java
/**
 * 用户服务测试类
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository mockUserRepository;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .username("testUser")
            .email("test@example.com")
            .status(UserStatus.ACTIVE)
            .build();
    }
    
    /**
     * 测试根据有效ID获取用户
     */
    @Test
    void shouldReturnUserWhenValidIdProvided() {
        // Arrange
        when(mockUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // Act
        User result = userService.getUserById(1L);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(mockUserRepository).findById(1L);
    }
    
    /**
     * 测试用户不存在时抛出异常
     */
    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(mockUserRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            userService.getUserById(999L);
        });
        verify(mockUserRepository).findById(999L);
    }
    
    /**
     * 测试保存有效用户数据
     */
    @Test
    void shouldSaveUserSuccessfullyWhenValidDataProvided() {
        // Arrange
        User newUser = User.builder()
            .username("newUser")
            .email("new@example.com")
            .build();
        User savedUser = User.builder()
            .id(2L)
            .username("newUser")
            .email("new@example.com")
            .build();
        when(mockUserRepository.save(newUser)).thenReturn(savedUser);
        
        // Act
        User result = userService.saveUser(newUser);
        
        // Assert
        assertThat(result.getId()).isEqualTo(2L);
        verify(mockUserRepository).save(newUser);
    }
}
```

## Controller层测试规范

```java
/**
 * 用户控制器测试类
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService mockUserService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 测试根据有效ID获取用户
     */
    @Test
    void shouldReturnUserWhenGetByValidId() throws Exception {
        // Arrange
        User user = User.builder()
            .id(1L)
            .username("testUser")
            .build();
        when(mockUserService.getUserById(1L)).thenReturn(user);
        
        // Act & Assert
        mockMvc.perform(get("/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.username").value("testUser"));
            
        verify(mockUserService).getUserById(1L);
    }
    
    /**
     * 测试用户不存在时返回404
     */
    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        // Arrange
        when(mockUserService.getUserById(999L))
            .thenThrow(new EntityNotFoundException("用户不存在"));
        
        // Act & Assert
        mockMvc.perform(get("/users/999"))
            .andExpect(status().isNotFound());
    }
    
    /**
     * 测试成功创建用户
     */
    @Test
    void shouldCreateUserSuccessfully() throws Exception {
        // Arrange
        User newUser = User.builder()
            .username("newUser")
            .email("new@example.com")
            .build();
        User savedUser = User.builder()
            .id(1L)
            .username("newUser")
            .email("new@example.com")
            .build();
        when(mockUserService.saveUser(any(User.class))).thenReturn(savedUser);
        
        // Act & Assert
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.username").value("newUser"));
    }
}
```

## Repository层测试规范

```java
/**
 * 用户仓库测试类
 */
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 测试根据用户名查找用户
     */
    @Test
    void shouldFindUserByUsername() {
        // Arrange
        User user = User.builder()
            .username("testUser")
            .email("test@example.com")
            .build();
        entityManager.persistAndFlush(user);
        
        // Act
        Optional<User> result = userRepository.findByUsername("testUser");
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testUser");
    }
    
    /**
     * 测试用户名不存在时返回空结果
     */
    @Test
    void shouldReturnEmptyWhenUsernameNotFound() {
        // Act
        Optional<User> result = userRepository.findByUsername("nonexistent");
        
        // Assert
        assertThat(result).isEmpty();
    }
}
```

## 静态导入规范

在测试类中使用静态导入简化代码：

```java
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
```

## 测试注解使用

- `@ExtendWith(MockitoExtension.class)` - Service层单元测试
- `@WebMvcTest` - Controller层测试
- `@DataJpaTest` - Repository层测试
- `@SpringBootTest` - 集成测试
- `@Mock` - 模拟依赖对象
- `@MockBean` - Spring上下文中的模拟Bean
- `@InjectMocks` - 注入模拟对象

## 断言规范

- 使用 AssertJ 进行断言，提高可读性
- 优先使用 `assertThat()` 而非 JUnit 的断言方法
- 异常测试使用 `assertThrows()`
- 集合断言使用 `hasSize()`、`contains()`、`isEmpty()` 等方法

## 其他建议

- 测试数据使用 Builder 模式构建
- 避免测试间相互依赖，确保测试独立性
- 复杂测试数据可提取为测试工厂类或使用 `@TestConfiguration`
- 及时更新测试代码，保持与业务代码同步
- 参数化测试使用 `@ParameterizedTest` 和 `@ValueSource`
- 测试用例应覆盖正常流程、边界条件和异常情况
