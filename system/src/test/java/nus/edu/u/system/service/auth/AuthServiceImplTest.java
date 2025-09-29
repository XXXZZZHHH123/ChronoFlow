package nus.edu.u.system.service.auth;

import static nus.edu.u.common.constant.Constants.DEFAULT_DELIMITER;
import static nus.edu.u.common.constant.Constants.SESSION_TENANT_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import java.util.Arrays;
import java.util.Collections;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.RoleDTO;
import nus.edu.u.system.domain.dto.UserRoleDTO;
import nus.edu.u.system.domain.dto.UserTokenDTO;
import nus.edu.u.system.domain.vo.auth.LoginReqVO;
import nus.edu.u.system.domain.vo.auth.LoginRespVO;
import nus.edu.u.system.domain.vo.role.RoleRespVO;
import nus.edu.u.system.service.role.RoleService;
import nus.edu.u.system.service.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for AuthServiceImpl
 *
 * @author Test Author
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserService userService;

    @Mock private TokenService tokenService;

    @Mock private RoleService roleService;

    @Mock private SaSession saSession;

    @InjectMocks private AuthServiceImpl authService;

    private MockedStatic<StpUtil> stpUtilMock;

    private UserDO userDO;
    private LoginReqVO loginReqVO;
    private UserRoleDTO userRoleDTO;
    private RoleDTO roleDTO1;
    private RoleDTO roleDTO2;
    private RoleRespVO roleRespVO1;
    private RoleRespVO roleRespVO2;

    @BeforeEach
    void setUp() {
        stpUtilMock = mockStatic(StpUtil.class);

        // 初始化测试数据
        userDO =
                UserDO.builder()
                        .id(1L)
                        .username("testuser")
                        .password("encodedPassword")
                        .email("test@example.com")
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .build();
        userDO.setTenantId(100L);

        loginReqVO = new LoginReqVO();
        loginReqVO.setUsername("testuser");
        loginReqVO.setPassword("password123");
        loginReqVO.setRemember(false);

        roleDTO1 = RoleDTO.builder().id(1L).roleKey("ADMIN").name("Administrator").build();

        roleDTO2 = RoleDTO.builder().id(2L).roleKey("USER").name("User").build();

        userRoleDTO =
                UserRoleDTO.builder()
                        .userId(1L)
                        .username("testuser")
                        .email("test@example.com")
                        .roles(Arrays.asList(roleDTO1, roleDTO2))
                        .build();

        roleRespVO1 = RoleRespVO.builder().id(1L).key("ADMIN").name("Administrator").build();

        roleRespVO2 = RoleRespVO.builder().id(2L).key("USER").name("User").build();
    }

    @AfterEach
    void tearDown() {
        if (stpUtilMock != null) {
            stpUtilMock.close();
        }
    }

    // ==================== authenticate 方法测试 ====================

    @Test
    void authenticate_Success() {
        // Given
        when(userService.getUserByUsername("testuser")).thenReturn(userDO);
        when(userService.isPasswordMatch("password123", "encodedPassword")).thenReturn(true);

        // When
        UserDO result = authService.authenticate("testuser", "password123");

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        verify(userService).getUserByUsername("testuser");
        verify(userService).isPasswordMatch("password123", "encodedPassword");
    }

    @Test
    void authenticate_UserNotFound_ThrowsException() {
        // Given
        when(userService.getUserByUsername("nonexistent")).thenReturn(null);

        // When & Then
        ServiceException exception =
                assertThrows(
                        ServiceException.class,
                        () -> authService.authenticate("nonexistent", "password123"));

        verify(userService).getUserByUsername("nonexistent");
        verify(userService, never()).isPasswordMatch(anyString(), anyString());
    }

    @Test
    void authenticate_WrongPassword_ThrowsException() {
        // Given
        when(userService.getUserByUsername("testuser")).thenReturn(userDO);
        when(userService.isPasswordMatch("wrongpassword", "encodedPassword")).thenReturn(false);

        // When & Then
        ServiceException exception =
                assertThrows(
                        ServiceException.class,
                        () -> authService.authenticate("testuser", "wrongpassword"));

        verify(userService).getUserByUsername("testuser");
        verify(userService).isPasswordMatch("wrongpassword", "encodedPassword");
    }

    @Test
    void authenticate_UserDisabled_ThrowsException() {
        // Given
        userDO.setStatus(CommonStatusEnum.DISABLE.getStatus());
        when(userService.getUserByUsername("testuser")).thenReturn(userDO);
        when(userService.isPasswordMatch("password123", "encodedPassword")).thenReturn(true);

        // When & Then
        ServiceException exception =
                assertThrows(
                        ServiceException.class,
                        () -> authService.authenticate("testuser", "password123"));

        verify(userService).getUserByUsername("testuser");
        verify(userService).isPasswordMatch("password123", "encodedPassword");
    }

    // ==================== login 方法测试 ====================

    @Test
    void login_Success_WithoutRememberMe_WithoutRefreshToken() {
        // Given
        when(userService.getUserByUsername("testuser")).thenReturn(userDO);
        when(userService.isPasswordMatch("password123", "encodedPassword")).thenReturn(true);
        when(tokenService.createRefreshToken(any(UserTokenDTO.class)))
                .thenReturn("new-refresh-token");
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(roleService.getRole(1L)).thenReturn(roleRespVO1);
        when(roleService.getRole(2L)).thenReturn(roleRespVO2);

        stpUtilMock.when(() -> StpUtil.login(1L)).thenAnswer(invocation -> null);
        stpUtilMock.when(StpUtil::getSession).thenReturn(saSession);
        stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

        // When
        LoginRespVO result = authService.login(loginReqVO);

        // Then
        assertNotNull(result);
        assertEquals("new-refresh-token", result.getRefreshToken());
        assertNotNull(result.getUser());
        assertEquals(1L, result.getUser().getId());
        assertEquals("testuser", result.getUser().getName());
        assertEquals("ADMIN" + DEFAULT_DELIMITER + "USER", result.getUser().getRole());
        assertEquals(2, result.getRoles().size());

        verify(userService).getUserByUsername("testuser");
        verify(tokenService).createRefreshToken(any(UserTokenDTO.class));
        verify(saSession).set(SESSION_TENANT_ID, 100L);
        stpUtilMock.verify(() -> StpUtil.login(1L));
    }

    @Test
    void login_Success_WithRememberMe_WithExistingRefreshToken() {
        // Given
        loginReqVO.setRemember(true);
        loginReqVO.setRefreshToken("existing-refresh-token");

        when(userService.getUserByUsername("testuser")).thenReturn(userDO);
        when(userService.isPasswordMatch("password123", "encodedPassword")).thenReturn(true);
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(roleService.getRole(1L)).thenReturn(roleRespVO1);
        when(roleService.getRole(2L)).thenReturn(roleRespVO2);

        stpUtilMock.when(() -> StpUtil.login(1L)).thenAnswer(invocation -> null);
        stpUtilMock.when(StpUtil::getSession).thenReturn(saSession);
        stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

        // When
        LoginRespVO result = authService.login(loginReqVO);

        // Then
        assertNotNull(result);
        assertEquals("existing-refresh-token", result.getRefreshToken());
        verify(tokenService, never()).createRefreshToken(any());
        stpUtilMock.verify(() -> StpUtil.login(1L));
    }

    @Test
    void login_AuthenticationFailed_ThrowsException() {
        // Given
        when(userService.getUserByUsername("testuser")).thenReturn(null);

        // When & Then
        assertThrows(ServiceException.class, () -> authService.login(loginReqVO));

        verify(userService).getUserByUsername("testuser");
        stpUtilMock.verify(() -> StpUtil.login(anyLong()), never());
    }

    // ==================== logout 方法测试 ====================

    @Test
    void logout_Success() {
        // Given
        String token = "test-token";
        stpUtilMock.when(StpUtil::logout).thenAnswer(invocation -> null);

        // When
        authService.logout(token);

        // Then
        verify(tokenService).removeToken(token);
        stpUtilMock.verify(StpUtil::logout);
    }

    // ==================== refresh 方法测试 ====================

    @Test
    void refresh_AlreadyLoggedIn_Success() {
        // Given
        String refreshToken = "valid-refresh-token";
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(roleService.getRole(1L)).thenReturn(roleRespVO1);
        when(roleService.getRole(2L)).thenReturn(roleRespVO2);

        // When
        LoginRespVO result = authService.refresh(refreshToken);

        // Then
        assertNotNull(result);
        assertEquals(refreshToken, result.getRefreshToken());
        assertEquals("testuser", result.getUser().getName());

        verify(tokenService, never()).getUserIdFromRefreshToken(anyString());
        stpUtilMock.verify(() -> StpUtil.login(anyLong()), never());
    }

    @Test
    void refresh_NotLoggedIn_ValidToken_Success() {
        // Given
        String refreshToken = "valid-refresh-token";
        stpUtilMock.when(StpUtil::isLogin).thenReturn(false);
        stpUtilMock.when(() -> StpUtil.login(1L)).thenAnswer(invocation -> null);
        stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

        when(tokenService.getUserIdFromRefreshToken(refreshToken)).thenReturn(1L);
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(roleService.getRole(1L)).thenReturn(roleRespVO1);
        when(roleService.getRole(2L)).thenReturn(roleRespVO2);

        // When
        LoginRespVO result = authService.refresh(refreshToken);

        // Then
        assertNotNull(result);
        assertEquals(refreshToken, result.getRefreshToken());
        assertEquals(1L, result.getUser().getId());

        verify(tokenService).getUserIdFromRefreshToken(refreshToken);
        stpUtilMock.verify(() -> StpUtil.login(1L));
    }

    @Test
    void refresh_NotLoggedIn_InvalidToken_ThrowsException() {
        // Given
        String refreshToken = "invalid-refresh-token";
        stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

        when(tokenService.getUserIdFromRefreshToken(refreshToken)).thenReturn(null);

        // When & Then
        assertThrows(ServiceException.class, () -> authService.refresh(refreshToken));

        verify(tokenService).getUserIdFromRefreshToken(refreshToken);
        stpUtilMock.verify(() -> StpUtil.login(anyLong()), never());
    }

    @Test
    void refresh_UserNotFound_ThrowsException() {
        // Given
        String refreshToken = "valid-refresh-token";
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

        when(userService.selectUserWithRole(1L)).thenReturn(null);

        // When & Then
        assertThrows(ServiceException.class, () -> authService.refresh(refreshToken));

        verify(userService).selectUserWithRole(1L);
    }

    // ==================== getInfo 方法测试（通过其他方法间接测试）====================

    @Test
    void getInfo_WithMultipleRoles_Success() {
        // Given
        loginReqVO.setRemember(false);
        when(userService.getUserByUsername("testuser")).thenReturn(userDO);
        when(userService.isPasswordMatch("password123", "encodedPassword")).thenReturn(true);
        when(tokenService.createRefreshToken(any(UserTokenDTO.class))).thenReturn("refresh-token");
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(roleService.getRole(1L)).thenReturn(roleRespVO1);
        when(roleService.getRole(2L)).thenReturn(roleRespVO2);

        stpUtilMock.when(() -> StpUtil.login(1L)).thenAnswer(invocation -> null);
        stpUtilMock.when(StpUtil::getSession).thenReturn(saSession);
        stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

        // When
        LoginRespVO result = authService.login(loginReqVO);

        // Then
        assertNotNull(result);
        assertNotNull(result.getUser());
        assertEquals("ADMIN" + DEFAULT_DELIMITER + "USER", result.getUser().getRole());
        assertEquals(2, result.getRoles().size());
        assertTrue(result.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getKey())));
        assertTrue(result.getRoles().stream().anyMatch(r -> "USER".equals(r.getKey())));
    }

    @Test
    void getInfo_WithSingleRole_Success() {
        // Given
        userRoleDTO.setRoles(Collections.singletonList(roleDTO1));
        loginReqVO.setRemember(false);

        when(userService.getUserByUsername("testuser")).thenReturn(userDO);
        when(userService.isPasswordMatch("password123", "encodedPassword")).thenReturn(true);
        when(tokenService.createRefreshToken(any(UserTokenDTO.class))).thenReturn("refresh-token");
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(roleService.getRole(1L)).thenReturn(roleRespVO1);

        stpUtilMock.when(() -> StpUtil.login(1L)).thenAnswer(invocation -> null);
        stpUtilMock.when(StpUtil::getSession).thenReturn(saSession);
        stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

        // When
        LoginRespVO result = authService.login(loginReqVO);

        // Then
        assertNotNull(result);
        assertEquals("ADMIN", result.getUser().getRole());
        assertEquals(1, result.getRoles().size());
    }

    @Test
    void getInfo_WithNullRole_FiltersOut() {
        // Given
        loginReqVO.setRemember(false);

        when(userService.getUserByUsername("testuser")).thenReturn(userDO);
        when(userService.isPasswordMatch("password123", "encodedPassword")).thenReturn(true);
        when(tokenService.createRefreshToken(any(UserTokenDTO.class))).thenReturn("refresh-token");
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(roleService.getRole(1L)).thenReturn(roleRespVO1);
        when(roleService.getRole(2L)).thenReturn(null); // 第二个角色返回null

        stpUtilMock.when(() -> StpUtil.login(1L)).thenAnswer(invocation -> null);
        stpUtilMock.when(StpUtil::getSession).thenReturn(saSession);
        stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

        // When
        LoginRespVO result = authService.login(loginReqVO);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRoles().size()); // 只有一个非null的角色
        assertEquals("ADMIN", result.getRoles().get(0).getKey());
    }

    @Test
    void getInfo_WithEmptyRoles_Success() {
        // Given
        userRoleDTO.setRoles(Collections.emptyList());
        loginReqVO.setRemember(false);

        when(userService.getUserByUsername("testuser")).thenReturn(userDO);
        when(userService.isPasswordMatch("password123", "encodedPassword")).thenReturn(true);
        when(tokenService.createRefreshToken(any(UserTokenDTO.class))).thenReturn("refresh-token");
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);

        stpUtilMock.when(() -> StpUtil.login(1L)).thenAnswer(invocation -> null);
        stpUtilMock.when(StpUtil::getSession).thenReturn(saSession);
        stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

        // When
        LoginRespVO result = authService.login(loginReqVO);

        // Then
        assertNotNull(result);
        assertEquals("", result.getUser().getRole()); // 空字符串
        assertEquals(0, result.getRoles().size());
    }

    // ==================== 边界情况测试 ====================

    @Test
    void authenticate_WithNullUsername_ThrowsException() {
        // Given
        when(userService.getUserByUsername(null)).thenReturn(null);

        // When & Then
        assertThrows(ServiceException.class, () -> authService.authenticate(null, "password123"));
    }

    @Test
    void authenticate_WithEmptyPassword_ThrowsException() {
        // Given
        when(userService.getUserByUsername("testuser")).thenReturn(userDO);
        when(userService.isPasswordMatch("", "encodedPassword")).thenReturn(false);

        // When & Then
        assertThrows(ServiceException.class, () -> authService.authenticate("testuser", ""));
    }

    @Test
    void login_WithNullRefreshToken_CreatesNewToken() {
        // Given
        loginReqVO.setRefreshToken(null);

        when(userService.getUserByUsername("testuser")).thenReturn(userDO);
        when(userService.isPasswordMatch("password123", "encodedPassword")).thenReturn(true);
        when(tokenService.createRefreshToken(any(UserTokenDTO.class))).thenReturn("new-token");
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(roleService.getRole(1L)).thenReturn(roleRespVO1);
        when(roleService.getRole(2L)).thenReturn(roleRespVO2);

        stpUtilMock.when(() -> StpUtil.login(1L)).thenAnswer(invocation -> null);
        stpUtilMock.when(StpUtil::getSession).thenReturn(saSession);
        stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

        // When
        LoginRespVO result = authService.login(loginReqVO);

        // Then
        assertNotNull(result);
        assertEquals("new-token", result.getRefreshToken());
        verify(tokenService).createRefreshToken(any(UserTokenDTO.class));
    }

    @Test
    void login_WithEmptyRefreshToken_CreatesNewToken() {
        // Given
        loginReqVO.setRefreshToken("");

        when(userService.getUserByUsername("testuser")).thenReturn(userDO);
        when(userService.isPasswordMatch("password123", "encodedPassword")).thenReturn(true);
        when(tokenService.createRefreshToken(any(UserTokenDTO.class))).thenReturn("new-token");
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(roleService.getRole(1L)).thenReturn(roleRespVO1);
        when(roleService.getRole(2L)).thenReturn(roleRespVO2);

        stpUtilMock.when(() -> StpUtil.login(1L)).thenAnswer(invocation -> null);
        stpUtilMock.when(StpUtil::getSession).thenReturn(saSession);
        stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

        // When
        LoginRespVO result = authService.login(loginReqVO);

        // Then
        assertNotNull(result);
        assertEquals("new-token", result.getRefreshToken());
        verify(tokenService).createRefreshToken(any(UserTokenDTO.class));
    }
}
