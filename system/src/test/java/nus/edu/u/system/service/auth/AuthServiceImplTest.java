package nus.edu.u.system.service.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.stp.StpUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.RoleDTO;
import nus.edu.u.system.domain.dto.UserRoleDTO;
import nus.edu.u.system.domain.dto.UserTokenDTO;
import nus.edu.u.system.domain.vo.auth.LoginReqVO;
import nus.edu.u.system.domain.vo.auth.LoginRespVO;
import nus.edu.u.system.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserService userService;

    @Mock private TokenService tokenService;

    @InjectMocks private AuthServiceImpl authService;

    private UserDO testUser;
    private LoginReqVO loginReqVO;
    private UserRoleDTO userRoleDTO;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testUser = new UserDO();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("hashedPassword");
        testUser.setStatus(CommonStatusEnum.ENABLE.getStatus());
        testUser.setTenantId(100L);
        testUser.setEmail("test@example.com");

        loginReqVO = new LoginReqVO();
        loginReqVO.setUsername("testuser");
        loginReqVO.setPassword("password123");
        loginReqVO.setRemember(false);

        // 模拟用户角色数据
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setRoleKey("admin");

        userRoleDTO = new UserRoleDTO();
        userRoleDTO.setUserId(1L);
        userRoleDTO.setUsername("testuser");
        userRoleDTO.setEmail("test@example.com");
        userRoleDTO.setRoles(List.of(roleDTO));
    }

    @Test
    void authenticate_Success() {
        // Given
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(userService.isPasswordMatch("password123", "hashedPassword")).thenReturn(true);

        // When
        UserDO result = authService.authenticate("testuser", "password123");

        // Then
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUsername(), result.getUsername());
        verify(userService).getUserByUsername("testuser");
        verify(userService).isPasswordMatch("password123", "hashedPassword");
    }

    @Test
    void authenticate_UserNotFound_ThrowsException() {
        // Given
        when(userService.getUserByUsername("nonexistent")).thenReturn(null);

        // When & Then
        assertThrows(
                RuntimeException.class,
                () -> {
                    authService.authenticate("nonexistent", "password123");
                });
        verify(userService).getUserByUsername("nonexistent");
        verify(userService, never()).isPasswordMatch(anyString(), anyString());
    }

    @Test
    void authenticate_PasswordMismatch_ThrowsException() {
        // Given
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(userService.isPasswordMatch("wrongpassword", "hashedPassword")).thenReturn(false);

        // When & Then
        assertThrows(
                RuntimeException.class,
                () -> {
                    authService.authenticate("testuser", "wrongpassword");
                });
        verify(userService).getUserByUsername("testuser");
        verify(userService).isPasswordMatch("wrongpassword", "hashedPassword");
    }

    @Test
    void authenticate_UserDisabled_ThrowsException() {
        // Given
        testUser.setStatus(CommonStatusEnum.DISABLE.getStatus());
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(userService.isPasswordMatch("password123", "hashedPassword")).thenReturn(true);

        // When & Then
        assertThrows(
                RuntimeException.class,
                () -> {
                    authService.authenticate("testuser", "password123");
                });
        verify(userService).getUserByUsername("testuser");
        verify(userService).isPasswordMatch("password123", "hashedPassword");
    }

    @Test
    void login_Success() {
        // Given
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(userService.isPasswordMatch("password123", "hashedPassword")).thenReturn(true);
        when(tokenService.createRefreshToken(any(UserTokenDTO.class)))
                .thenReturn("refresh_token_123");
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(() -> StpUtil.login(1L)).thenAnswer(invocation -> null);
            stpUtilMock
                    .when(StpUtil::getSession)
                    .thenReturn(mock(cn.dev33.satoken.session.SaSession.class));
            stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

            // When
            LoginRespVO result = authService.login(loginReqVO);

            // Then
            assertNotNull(result);
            assertNotNull(result.getRefreshToken());
            assertNotNull(result.getUser());
            assertEquals("testuser", result.getUser().getName());
            assertEquals("test@example.com", result.getUser().getEmail());
            assertEquals("admin", result.getUser().getRole());

            verify(userService).getUserByUsername("testuser");
            verify(userService).isPasswordMatch("password123", "hashedPassword");
            verify(tokenService).createRefreshToken(any(UserTokenDTO.class));
            verify(userService).selectUserWithRole(1L);
        }
    }

    @Test
    void login_WithExistingRefreshToken_Success() {
        // Given
        loginReqVO.setRefreshToken("existing_refresh_token");
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(userService.isPasswordMatch("password123", "hashedPassword")).thenReturn(true);
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(() -> StpUtil.login(1L)).thenAnswer(invocation -> null);
            stpUtilMock
                    .when(StpUtil::getSession)
                    .thenReturn(mock(cn.dev33.satoken.session.SaSession.class));
            stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

            // When
            LoginRespVO result = authService.login(loginReqVO);

            // Then
            assertNotNull(result);
            assertEquals("existing_refresh_token", result.getRefreshToken());
            verify(tokenService, never()).createRefreshToken(any(UserTokenDTO.class));
        }
    }

    @Test
    void logout_Success() {
        // Given
        String token = "test_token";

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::logout).thenAnswer(invocation -> null);

            // When
            authService.logout(token);

            // Then
            verify(tokenService).removeToken(token);
            stpUtilMock.verify(StpUtil::logout);
        }
    }

    @Test
    void refresh_UserLoggedIn_Success() {
        // Given
        String refreshToken = "refresh_token_123";
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
            stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

            // When
            LoginRespVO result = authService.refresh(refreshToken);

            // Then
            assertNotNull(result);
            assertEquals(refreshToken, result.getRefreshToken());
            assertNotNull(result.getUser());
            verify(userService).selectUserWithRole(1L);
            verify(tokenService, never()).getUserIdFromRefreshToken(anyString());
        }
    }

    @Test
    void refresh_UserNotLoggedIn_Success() {
        // Given
        String refreshToken = "refresh_token_123";
        when(tokenService.getUserIdFromRefreshToken(refreshToken)).thenReturn(1L);
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::isLogin).thenReturn(false);
            stpUtilMock.when(() -> StpUtil.login(1L)).thenAnswer(invocation -> null);
            stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

            // When
            LoginRespVO result = authService.refresh(refreshToken);

            // Then
            assertNotNull(result);
            assertEquals(refreshToken, result.getRefreshToken());
            verify(tokenService).getUserIdFromRefreshToken(refreshToken);
            verify(userService).selectUserWithRole(1L);
            stpUtilMock.verify(() -> StpUtil.login(1L));
        }
    }

    @Test
    void refresh_InvalidRefreshToken_ThrowsException() {
        // Given
        String invalidRefreshToken = "invalid_token";
        when(tokenService.getUserIdFromRefreshToken(invalidRefreshToken)).thenReturn(null);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

            // When & Then
            assertThrows(
                    RuntimeException.class,
                    () -> {
                        authService.refresh(invalidRefreshToken);
                    });
            verify(tokenService).getUserIdFromRefreshToken(invalidRefreshToken);
        }
    }

    @Test
    void getInfo_UserNotFound_ThrowsException() {
        // Given
        String refreshToken = "refresh_token_123";
        when(userService.selectUserWithRole(1L)).thenReturn(null);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

            // When & Then
            assertThrows(
                    RuntimeException.class,
                    () -> {
                        authService.refresh(refreshToken);
                    });
            verify(userService).selectUserWithRole(1L);
        }
    }

    @Test
    void getInfo_MultipleRoles_Success() {
        // Given
        RoleDTO role1 = new RoleDTO();
        role1.setRoleKey("admin");
        RoleDTO role2 = new RoleDTO();
        role2.setRoleKey("user");

        userRoleDTO.setRoles(Arrays.asList(role1, role2));

        String refreshToken = "refresh_token_123";
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
            stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

            // When
            LoginRespVO result = authService.refresh(refreshToken);

            // Then
            assertNotNull(result);
            assertTrue(result.getUser().getRole().contains("admin"));
            assertTrue(result.getUser().getRole().contains("user"));
        }
    }

    @Test
    void getInfo_NoRoles_Success() {
        // Given
        userRoleDTO.setRoles(Collections.emptyList());

        String refreshToken = "refresh_token_123";
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
            stpUtilMock.when(StpUtil::getLoginId).thenReturn(1L);

            // When
            LoginRespVO result = authService.refresh(refreshToken);

            // Then
            assertNotNull(result);
            assertEquals("", result.getUser().getRole());
        }
    }
}
