package nus.edu.u.system.service.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;

import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.framework.security.config.SecurityProperties;
import nus.edu.u.system.domain.dto.UserTokenDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private UserTokenDTO userTokenDTO;
    // 根据实际的Redis key前缀
    private static final String LOGIN_REFRESH_TOKEN_KEY = "Authorization:login:refresh_token:";
    private static final Long REFRESH_TOKEN_EXPIRE = 604800L; // 7 days in seconds

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        userTokenDTO = new UserTokenDTO();
        userTokenDTO.setId(1L);
        userTokenDTO.setRemember(true);
    }

    @Test
    void createRefreshToken_RememberTrue_Success() {
        // Given
        userTokenDTO.setRemember(true);
        when(securityProperties.getRefreshTokenExpire()).thenReturn(REFRESH_TOKEN_EXPIRE);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        String result = tokenService.createRefreshToken(userTokenDTO);

        // Then
        assertNotNull(result);
        assertTrue(result.length() > 0);
        // 验证 UUID 格式（36个字符，包含4个连字符）
        assertTrue(result.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"));

        // 验证 Redis 存储操作被正确调用
        verify(valueOperations).set(
                eq(LOGIN_REFRESH_TOKEN_KEY + result),
                eq("1"),
                eq(REFRESH_TOKEN_EXPIRE),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void createRefreshToken_RememberFalse_Success() {
        // Given
        userTokenDTO.setRemember(false);

        // When
        String result = tokenService.createRefreshToken(userTokenDTO);

        // Then
        assertNotNull(result);
        assertTrue(result.length() > 0);
        // 验证 UUID 格式
        assertTrue(result.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"));

        // 验证没有存储到Redis
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        verify(securityProperties, never()).getRefreshTokenExpire();
    }

    @Test
    void createRefreshToken_DifferentUserId_Success() {
        // Given
        userTokenDTO.setId(999L);
        userTokenDTO.setRemember(true);
        when(securityProperties.getRefreshTokenExpire()).thenReturn(REFRESH_TOKEN_EXPIRE);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        String result = tokenService.createRefreshToken(userTokenDTO);

        // Then
        assertNotNull(result);
        assertTrue(result.length() > 0);
        // 验证 UUID 格式
        assertTrue(result.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"));

        // 验证 Redis 存储操作被正确调用，用户ID为999
        verify(valueOperations).set(
                eq(LOGIN_REFRESH_TOKEN_KEY + result),
                eq("999"),
                eq(REFRESH_TOKEN_EXPIRE),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void removeToken_NormalScenario_Success() {
        // Given
        String token = "test-token-123";
        when(stringRedisTemplate.delete(LOGIN_REFRESH_TOKEN_KEY + token)).thenReturn(true);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::logout).thenAnswer(invocation -> null);

            // When
            tokenService.removeToken(token);

            // Then
            verify(stringRedisTemplate).delete(LOGIN_REFRESH_TOKEN_KEY + token);
            stpUtilMock.verify(StpUtil::logout);
        }
    }

    @Test
    void removeToken_RedisException_ThrowsServiceException() {
        // Given
        String token = "test-token-456";
        when(stringRedisTemplate.delete(LOGIN_REFRESH_TOKEN_KEY + token))
                .thenThrow(new RuntimeException("Redis connection error"));

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            // When & Then
            assertThrows(ServiceException.class, () -> {
                tokenService.removeToken(token);
            });

            verify(stringRedisTemplate).delete(LOGIN_REFRESH_TOKEN_KEY + token);
            // 验证由于异常，logout 不会被调用
            stpUtilMock.verifyNoInteractions();
        }
    }

    @Test
    void removeToken_EmptyToken_ThrowsServiceException() {
        // Given
        String token = "";
        // 空 token 可能导致业务逻辑抛出异常
        when(stringRedisTemplate.delete(LOGIN_REFRESH_TOKEN_KEY + token))
                .thenThrow(new RuntimeException("Invalid token"));

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            // When & Then
            assertThrows(ServiceException.class, () -> {
                tokenService.removeToken(token);
            });

            verify(stringRedisTemplate).delete(LOGIN_REFRESH_TOKEN_KEY + token);
            // 验证由于异常，logout 不会被调用
            stpUtilMock.verifyNoInteractions();
        }
    }

    @Test
    void removeToken_ValidToken_Success() {
        // Given
        String token = "valid-token-123";
        when(stringRedisTemplate.delete(LOGIN_REFRESH_TOKEN_KEY + token)).thenReturn(true);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::logout).thenAnswer(invocation -> null);

            // When
            tokenService.removeToken(token);

            // Then
            verify(stringRedisTemplate).delete(LOGIN_REFRESH_TOKEN_KEY + token);
            stpUtilMock.verify(StpUtil::logout);
        }
    }

    @Test
    void getUserIdFromRefreshToken_ValidToken_ReturnsUserId() {
        // Given
        String refreshToken = "valid-refresh-token";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOGIN_REFRESH_TOKEN_KEY + refreshToken)).thenReturn("123");

        // When
        Long result = tokenService.getUserIdFromRefreshToken(refreshToken);

        // Then
        assertEquals(123L, result);
        verify(valueOperations).get(LOGIN_REFRESH_TOKEN_KEY + refreshToken);
    }

    @Test
    void getUserIdFromRefreshToken_InvalidToken_ReturnsNull() {
        // Given
        String refreshToken = "invalid-refresh-token";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOGIN_REFRESH_TOKEN_KEY + refreshToken)).thenReturn(null);

        // When
        Long result = tokenService.getUserIdFromRefreshToken(refreshToken);

        // Then
        assertNull(result);
        verify(valueOperations).get(LOGIN_REFRESH_TOKEN_KEY + refreshToken);
    }

    @Test
    void getUserIdFromRefreshToken_EmptyValue_ReturnsNull() {
        // Given
        String refreshToken = "empty-value-token";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOGIN_REFRESH_TOKEN_KEY + refreshToken)).thenReturn("");

        // When
        Long result = tokenService.getUserIdFromRefreshToken(refreshToken);

        // Then
        assertNull(result);
        verify(valueOperations).get(LOGIN_REFRESH_TOKEN_KEY + refreshToken);
    }

    @Test
    void getUserIdFromRefreshToken_WhitespaceValue_ThrowsException() {
        // Given
        String refreshToken = "whitespace-token";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOGIN_REFRESH_TOKEN_KEY + refreshToken)).thenReturn("   ");

        // When & Then
        assertThrows(NumberFormatException.class, () -> {
            tokenService.getUserIdFromRefreshToken(refreshToken);
        });
        verify(valueOperations).get(LOGIN_REFRESH_TOKEN_KEY + refreshToken);
    }

    @Test
    void getUserIdFromRefreshToken_InvalidNumberFormat_ThrowsException() {
        // Given
        String refreshToken = "invalid-number-token";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOGIN_REFRESH_TOKEN_KEY + refreshToken)).thenReturn("not-a-number");

        // When & Then
        assertThrows(NumberFormatException.class, () -> {
            tokenService.getUserIdFromRefreshToken(refreshToken);
        });
        verify(valueOperations).get(LOGIN_REFRESH_TOKEN_KEY + refreshToken);
    }

    @Test
    void getUserIdFromRefreshToken_LargeUserId_Success() {
        // Given
        String refreshToken = "large-user-id-token";
        String largeUserId = String.valueOf(Long.MAX_VALUE);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOGIN_REFRESH_TOKEN_KEY + refreshToken)).thenReturn(largeUserId);

        // When
        Long result = tokenService.getUserIdFromRefreshToken(refreshToken);

        // Then
        assertEquals(Long.MAX_VALUE, result);
        verify(valueOperations).get(LOGIN_REFRESH_TOKEN_KEY + refreshToken);
    }

    @Test
    void getUserIdFromRefreshToken_ZeroUserId_Success() {
        // Given
        String refreshToken = "zero-user-id-token";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOGIN_REFRESH_TOKEN_KEY + refreshToken)).thenReturn("0");

        // When
        Long result = tokenService.getUserIdFromRefreshToken(refreshToken);

        // Then
        assertEquals(0L, result);
        verify(valueOperations).get(LOGIN_REFRESH_TOKEN_KEY + refreshToken);
    }

    @Test
    void getUserIdFromRefreshToken_NegativeUserId_Success() {
        // Given
        String refreshToken = "negative-user-id-token";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOGIN_REFRESH_TOKEN_KEY + refreshToken)).thenReturn("-1");

        // When
        Long result = tokenService.getUserIdFromRefreshToken(refreshToken);

        // Then
        assertEquals(-1L, result);
        verify(valueOperations).get(LOGIN_REFRESH_TOKEN_KEY + refreshToken);
    }

    @Test
    void createRefreshToken_NullUserTokenDTO_ThrowsException() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            tokenService.createRefreshToken(null);
        });
    }

    @Test
    void createRefreshToken_NullUserId_ThrowsException() {
        // Given
        userTokenDTO.setId(null);
        userTokenDTO.setRemember(true);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            tokenService.createRefreshToken(userTokenDTO);
        });
    }
}