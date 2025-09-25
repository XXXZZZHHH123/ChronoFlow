package nus.edu.u.system.service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import nus.edu.u.common.constant.PermissionConstants;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.permission.PermissionDO;
import nus.edu.u.system.domain.dataobject.role.RoleDO;
import nus.edu.u.system.domain.dataobject.role.RolePermissionDO;
import nus.edu.u.system.domain.dataobject.tenant.TenantDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dataobject.user.UserRoleDO;
import nus.edu.u.system.domain.vo.reg.RegMemberReqVO;
import nus.edu.u.system.domain.vo.reg.RegOrganizerReqVO;
import nus.edu.u.system.domain.vo.reg.RegSearchReqVO;
import nus.edu.u.system.domain.vo.reg.RegSearchRespVO;
import nus.edu.u.system.enums.user.UserStatusEnum;
import nus.edu.u.system.mapper.permission.PermissionMapper;
import nus.edu.u.system.mapper.role.RoleMapper;
import nus.edu.u.system.mapper.role.RolePermissionMapper;
import nus.edu.u.system.mapper.tenant.TenantMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.mapper.user.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegServiceImplTest {

    @Mock private TenantMapper tenantMapper;
    @Mock private UserMapper userMapper;
    @Mock private PermissionMapper permissionMapper;
    @Mock private RolePermissionMapper rolePermissionMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private RegServiceImpl regService;

    private RegSearchReqVO regSearchReqVO;
    private RegMemberReqVO regMemberReqVO;
    private RegOrganizerReqVO regOrganizerReqVO;
    private TenantDO tenantDO;
    private UserDO userDO;
    private PermissionDO permissionDO;

    @BeforeEach
    void setUp() {
        // 初始化搜索请求
        regSearchReqVO = new RegSearchReqVO();
        regSearchReqVO.setOrganizationId(1L);
        regSearchReqVO.setUserId(100L);

        // 初始化成员注册请求
        regMemberReqVO = new RegMemberReqVO();
        regMemberReqVO.setUserId(100L);
        regMemberReqVO.setUsername("member001");
        regMemberReqVO.setPhone("12345678901");
        regMemberReqVO.setPassword("password123");

        // 初始化组织者注册请求
        regOrganizerReqVO = new RegOrganizerReqVO();
        regOrganizerReqVO.setUsername("organizer001");
        regOrganizerReqVO.setOrganizationName("Test Organization");
        regOrganizerReqVO.setMobile("12345678901");
        regOrganizerReqVO.setOrganizationAddress("Test Address");
        regOrganizerReqVO.setName("Test Organizer");
        regOrganizerReqVO.setUserEmail("organizer@test.com");
        regOrganizerReqVO.setUserPassword("password123");

        // 初始化租户数据
        tenantDO = new TenantDO();
        tenantDO.setId(1L);
        tenantDO.setName("Test Organization");

        // 初始化用户数据
        userDO = new UserDO();
        userDO.setId(100L);
        userDO.setTenantId(1L);
        userDO.setEmail("test@example.com");
        userDO.setStatus(UserStatusEnum.PENDING.getCode());

        // 初始化权限数据
        permissionDO = new PermissionDO();
        permissionDO.setId(1L);
        permissionDO.setPermissionKey(PermissionConstants.ALL_ORGANIZER_PERMISSION);
    }

    // ==================== search 方法测试 ====================

    @Test
    void search_Success() {
        // Given
        when(tenantMapper.selectById(1L)).thenReturn(tenantDO);
        when(userMapper.selectById(100L)).thenReturn(userDO);

        // When
        RegSearchRespVO result = regService.search(regSearchReqVO);

        // Then
        assertNotNull(result);
        assertEquals("Test Organization", result.getOrganizationName());
        assertEquals("test@example.com", result.getEmail());

        verify(tenantMapper).selectById(1L);
        verify(userMapper).selectById(100L);
    }

    @Test
    void search_TenantNotFound_ThrowsException() {
        // Given
        when(tenantMapper.selectById(1L)).thenReturn(null);

        // When & Then
        assertThrows(
                ServiceException.class,
                () -> {
                    regService.search(regSearchReqVO);
                });

        verify(tenantMapper).selectById(1L);
        verify(userMapper, never()).selectById(anyLong());
    }

    @Test
    void search_UserNotFound_ThrowsException() {
        // Given
        when(tenantMapper.selectById(1L)).thenReturn(tenantDO);
        when(userMapper.selectById(100L)).thenReturn(null);

        // When & Then
        assertThrows(
                ServiceException.class,
                () -> {
                    regService.search(regSearchReqVO);
                });

        verify(tenantMapper).selectById(1L);
        verify(userMapper).selectById(100L);
    }

    @Test
    void search_UserNotBelongToTenant_ThrowsException() {
        // Given
        userDO.setTenantId(999L); // 不同的租户ID
        when(tenantMapper.selectById(1L)).thenReturn(tenantDO);
        when(userMapper.selectById(100L)).thenReturn(userDO);

        // When & Then
        assertThrows(
                ServiceException.class,
                () -> {
                    regService.search(regSearchReqVO);
                });
    }

    @Test
    void search_UserAlreadyRegistered_ThrowsException() {
        // Given
        userDO.setStatus(UserStatusEnum.ENABLE.getCode()); // 已注册状态
        when(tenantMapper.selectById(1L)).thenReturn(tenantDO);
        when(userMapper.selectById(100L)).thenReturn(userDO);

        // When & Then
        assertThrows(
                ServiceException.class,
                () -> {
                    regService.search(regSearchReqVO);
                });
    }

    // ==================== registerAsMember 方法测试 ====================

    @Test
    void registerAsMember_Success() {
        // Given
        when(userMapper.selectById(100L)).thenReturn(userDO);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userMapper.updateById(any(UserDO.class))).thenReturn(1);

        // When
        boolean result = regService.registerAsMember(regMemberReqVO);

        // Then
        assertTrue(result);
        verify(userMapper).selectById(100L);
        verify(passwordEncoder).encode("password123");
        verify(userMapper)
                .updateById(
                        argThat(
                                user ->
                                        user.getId().equals(100L)
                                                && user.getUsername().equals("member001")
                                                && user.getPhone().equals("12345678901")
                                                && user.getPassword().equals("encoded_password")
                                                && user.getStatus()
                                                        .equals(UserStatusEnum.ENABLE.getCode())));
    }

    @Test
    void registerAsMember_UserNotFound_ThrowsException() {
        // Given
        when(userMapper.selectById(100L)).thenReturn(null);

        // When & Then
        assertThrows(
                ServiceException.class,
                () -> {
                    regService.registerAsMember(regMemberReqVO);
                });

        verify(userMapper).selectById(100L);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).updateById(any(UserDO.class));
    }

    @Test
    void registerAsMember_UserAlreadyRegistered_ThrowsException() {
        // Given
        userDO.setStatus(UserStatusEnum.ENABLE.getCode());
        when(userMapper.selectById(100L)).thenReturn(userDO);

        // When & Then
        assertThrows(
                ServiceException.class,
                () -> {
                    regService.registerAsMember(regMemberReqVO);
                });

        verify(userMapper).selectById(100L);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void registerAsMember_UpdateFailed_ReturnsFalse() {
        // Given
        when(userMapper.selectById(100L)).thenReturn(userDO);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userMapper.updateById(any(UserDO.class))).thenReturn(0);

        // When
        boolean result = regService.registerAsMember(regMemberReqVO);

        // Then
        assertFalse(result);
    }

    // ==================== registerAsOrganizer 方法测试 ====================

    @Test
    void registerAsOrganizer_Success() {
        // Given
        when(userMapper.selectByUsername("organizer001")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(tenantMapper.insert(any(TenantDO.class)))
                .thenAnswer(
                        invocation -> {
                            TenantDO tenant = invocation.getArgument(0);
                            tenant.setId(1L);
                            return 1;
                        });
        when(userMapper.insert(any(UserDO.class)))
                .thenAnswer(
                        invocation -> {
                            UserDO user = invocation.getArgument(0);
                            user.setId(100L);
                            return 1;
                        });
        when(tenantMapper.updateById(any(TenantDO.class))).thenReturn(1);
        when(roleMapper.insert(any(RoleDO.class)))
                .thenAnswer(
                        invocation -> {
                            RoleDO role = invocation.getArgument(0);
                            if (role.getRoleKey().equals("ORGANIZER")) {
                                role.setId(1L);
                            } else {
                                role.setId(2L);
                            }
                            return 1;
                        });
        when(userRoleMapper.insert(any(UserRoleDO.class))).thenReturn(1);
        when(permissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(permissionDO);
        when(rolePermissionMapper.insert(any(RolePermissionDO.class))).thenReturn(1);

        // When
        boolean result = regService.registerAsOrganizer(regOrganizerReqVO);

        // Then
        assertTrue(result);

        // 验证所有操作都被调用
        verify(userMapper).selectByUsername("organizer001");
        verify(tenantMapper).insert(any(TenantDO.class));
        verify(userMapper).insert(any(UserDO.class));
        verify(tenantMapper).updateById(any(TenantDO.class));
        verify(roleMapper, times(2)).insert(any(RoleDO.class)); // 组织者和成员角色
        verify(userRoleMapper).insert(any(UserRoleDO.class));
        verify(permissionMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(rolePermissionMapper).insert(any(RolePermissionDO.class));
    }

    @Test
    void registerAsOrganizer_UsernameExists_ThrowsException() {
        // Given
        UserDO existingUser = new UserDO();
        existingUser.setUsername("organizer001");
        when(userMapper.selectByUsername("organizer001")).thenReturn(existingUser);

        // When & Then
        assertThrows(
                ServiceException.class,
                () -> {
                    regService.registerAsOrganizer(regOrganizerReqVO);
                });

        verify(userMapper).selectByUsername("organizer001");
        verify(tenantMapper, never()).insert(any(TenantDO.class));
    }

    @Test
    void registerAsOrganizer_TenantInsertFailed_ThrowsException() {
        // Given
        when(userMapper.selectByUsername("organizer001")).thenReturn(null);
        when(tenantMapper.insert(any(TenantDO.class))).thenReturn(0);

        // When & Then
        assertThrows(
                ServiceException.class,
                () -> {
                    regService.registerAsOrganizer(regOrganizerReqVO);
                });

        verify(tenantMapper).insert(any(TenantDO.class));
        verify(userMapper, never()).insert(any(UserDO.class));
    }

    @Test
    void registerAsOrganizer_UserInsertFailed_ThrowsException() {
        // Given
        when(userMapper.selectByUsername("organizer001")).thenReturn(null);
        when(tenantMapper.insert(any(TenantDO.class)))
                .thenAnswer(
                        invocation -> {
                            TenantDO tenant = invocation.getArgument(0);
                            tenant.setId(1L);
                            return 1;
                        });
        when(userMapper.insert(any(UserDO.class))).thenReturn(0);

        // When & Then
        assertThrows(
                ServiceException.class,
                () -> {
                    regService.registerAsOrganizer(regOrganizerReqVO);
                });

        verify(userMapper).insert(any(UserDO.class));
        verify(tenantMapper, never()).updateById(any(TenantDO.class));
    }

    @Test
    void registerAsOrganizer_TenantUpdateFailed_ThrowsException() {
        // Given
        when(userMapper.selectByUsername("organizer001")).thenReturn(null);
        when(tenantMapper.insert(any(TenantDO.class)))
                .thenAnswer(
                        invocation -> {
                            TenantDO tenant = invocation.getArgument(0);
                            tenant.setId(1L);
                            return 1;
                        });
        when(userMapper.insert(any(UserDO.class)))
                .thenAnswer(
                        invocation -> {
                            UserDO user = invocation.getArgument(0);
                            user.setId(100L);
                            return 1;
                        });
        when(tenantMapper.updateById(any(TenantDO.class))).thenReturn(0);

        // When & Then
        assertThrows(
                ServiceException.class,
                () -> {
                    regService.registerAsOrganizer(regOrganizerReqVO);
                });
    }

    @Test
    void registerAsOrganizer_RoleInsertFailed_ThrowsException() {
        // Given
        when(userMapper.selectByUsername("organizer001")).thenReturn(null);
        when(tenantMapper.insert(any(TenantDO.class)))
                .thenAnswer(
                        invocation -> {
                            TenantDO tenant = invocation.getArgument(0);
                            tenant.setId(1L);
                            return 1;
                        });
        when(userMapper.insert(any(UserDO.class)))
                .thenAnswer(
                        invocation -> {
                            UserDO user = invocation.getArgument(0);
                            user.setId(100L);
                            return 1;
                        });
        when(tenantMapper.updateById(any(TenantDO.class))).thenReturn(1);
        when(roleMapper.insert(any(RoleDO.class))).thenReturn(0); // 第一个角色插入失败

        // When & Then
        assertThrows(
                ServiceException.class,
                () -> {
                    regService.registerAsOrganizer(regOrganizerReqVO);
                });
    }

    @Test
    void registerAsOrganizer_PermissionNotFound_ThrowsException() {
        // Given
        when(userMapper.selectByUsername("organizer001")).thenReturn(null);
        when(tenantMapper.insert(any(TenantDO.class)))
                .thenAnswer(
                        invocation -> {
                            TenantDO tenant = invocation.getArgument(0);
                            tenant.setId(1L);
                            return 1;
                        });
        when(userMapper.insert(any(UserDO.class)))
                .thenAnswer(
                        invocation -> {
                            UserDO user = invocation.getArgument(0);
                            user.setId(100L);
                            return 1;
                        });
        when(tenantMapper.updateById(any(TenantDO.class))).thenReturn(1);
        when(roleMapper.insert(any(RoleDO.class)))
                .thenAnswer(
                        invocation -> {
                            RoleDO role = invocation.getArgument(0);
                            role.setId(1L);
                            return 1;
                        });
        when(userRoleMapper.insert(any(UserRoleDO.class))).thenReturn(1);
        when(permissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // When & Then
        assertThrows(
                ServiceException.class,
                () -> {
                    regService.registerAsOrganizer(regOrganizerReqVO);
                });
    }

    @Test
    void registerAsOrganizer_VerifyDataCorrectness() {
        // Given
        when(userMapper.selectByUsername("organizer001")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(tenantMapper.insert(any(TenantDO.class)))
                .thenAnswer(
                        invocation -> {
                            TenantDO tenant = invocation.getArgument(0);
                            tenant.setId(1L);
                            return 1;
                        });
        when(userMapper.insert(any(UserDO.class)))
                .thenAnswer(
                        invocation -> {
                            UserDO user = invocation.getArgument(0);
                            user.setId(100L);
                            return 1;
                        });
        when(tenantMapper.updateById(any(TenantDO.class))).thenReturn(1);
        when(roleMapper.insert(any(RoleDO.class)))
                .thenAnswer(
                        invocation -> {
                            RoleDO role = invocation.getArgument(0);
                            role.setId(1L);
                            return 1;
                        });
        when(userRoleMapper.insert(any(UserRoleDO.class))).thenReturn(1);
        when(permissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(permissionDO);
        when(rolePermissionMapper.insert(any(RolePermissionDO.class))).thenReturn(1);

        // When
        regService.registerAsOrganizer(regOrganizerReqVO);

        // Then - 验证租户数据
        verify(tenantMapper)
                .insert(
                        argThat(
                                tenant ->
                                        tenant.getName().equals("Test Organization")
                                                && tenant.getContactMobile().equals("12345678901")
                                                && tenant.getAddress().equals("Test Address")
                                                && tenant.getContactName()
                                                        .equals("Test Organizer")));

        // 验证用户数据
        verify(userMapper)
                .insert(
                        argThat(
                                user ->
                                        user.getUsername().equals("organizer001")
                                                && user.getEmail().equals("organizer@test.com")
                                                && user.getPhone().equals("12345678901")
                                                && user.getPassword().equals("encoded_password")
                                                && user.getRemark()
                                                        .equals(RegServiceImpl.ORGANIZER_REMARK)
                                                && user.getStatus()
                                                        .equals(UserStatusEnum.ENABLE.getCode())));

        // 验证角色数据
        verify(roleMapper)
                .insert(
                        argThat(
                                role ->
                                        role.getName().equals("Organizer")
                                                && role.getRoleKey().equals("ORGANIZER")
                                                && role.getStatus()
                                                        .equals(
                                                                CommonStatusEnum.ENABLE
                                                                        .getStatus())));

        verify(roleMapper)
                .insert(
                        argThat(
                                role ->
                                        role.getName().equals("Member")
                                                && role.getRoleKey().equals("MEMBER")
                                                && role.getStatus()
                                                        .equals(
                                                                CommonStatusEnum.ENABLE
                                                                        .getStatus())));
    }
}
