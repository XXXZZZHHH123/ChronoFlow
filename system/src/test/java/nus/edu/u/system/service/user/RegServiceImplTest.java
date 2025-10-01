package nus.edu.u.system.service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import nus.edu.u.common.constant.PermissionConstants;
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
    private TenantDO tenantDO;
    private UserDO userDO;
    private RegMemberReqVO regMemberReqVO;
    private RegOrganizerReqVO regOrganizerReqVO;
    private PermissionDO permissionDO;
    private RoleDO roleDO;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        regSearchReqVO = new RegSearchReqVO();
        regSearchReqVO.setOrganizationId(1L);
        regSearchReqVO.setUserId(1L);

        tenantDO = TenantDO.builder().id(1L).name("Test Organization").build();

        userDO =
                UserDO.builder()
                        .id(1L)
                        .username("testuser")
                        .email("test@example.com")
                        .status(UserStatusEnum.PENDING.getCode())
                        .build();
        userDO.setTenantId(1L);

        regMemberReqVO = new RegMemberReqVO();
        regMemberReqVO.setUserId(1L);
        regMemberReqVO.setUsername("newuser");
        regMemberReqVO.setPhone("13800138000");
        regMemberReqVO.setPassword("password123");

        regOrganizerReqVO = new RegOrganizerReqVO();
        regOrganizerReqVO.setUsername("organizer");
        regOrganizerReqVO.setOrganizationName("New Org");
        regOrganizerReqVO.setMobile("13800138000");
        regOrganizerReqVO.setOrganizationAddress("Test Address");
        regOrganizerReqVO.setName("Organizer Name");
        regOrganizerReqVO.setUserEmail("organizer@test.com");
        regOrganizerReqVO.setUserPassword("password123");

        permissionDO =
                PermissionDO.builder()
                        .id(1L)
                        .permissionKey(PermissionConstants.ALL_SYSTEM_PERMISSION)
                        .build();

        roleDO = RoleDO.builder().id(1L).name("Test Role").build();
    }

    // ==================== search 方法测试 ====================

    @Test
    void search_Success() {
        // Given
        when(tenantMapper.selectById(1L)).thenReturn(tenantDO);
        when(userMapper.selectByIdWithoutTenant(1L)).thenReturn(userDO);

        // When
        RegSearchRespVO result = regService.search(regSearchReqVO);

        // Then
        assertNotNull(result);
        assertEquals("Test Organization", result.getOrganizationName());
        assertEquals("test@example.com", result.getEmail());
        verify(tenantMapper).selectById(1L);
        verify(userMapper).selectByIdWithoutTenant(1L);
    }

    @Test
    void search_TenantNotFound_ThrowsException() {
        // Given
        when(tenantMapper.selectById(1L)).thenReturn(null);

        // When & Then
        assertThrows(ServiceException.class, () -> regService.search(regSearchReqVO));
        verify(tenantMapper).selectById(1L);
        verify(userMapper, never()).selectById(anyLong());
    }

    @Test
    void search_UserNotFound_ThrowsException() {
        // Given
        when(tenantMapper.selectById(1L)).thenReturn(tenantDO);
        when(userMapper.selectByIdWithoutTenant(1L)).thenReturn(null);

        // When & Then
        assertThrows(ServiceException.class, () -> regService.search(regSearchReqVO));
        verify(tenantMapper).selectById(1L);
        verify(userMapper).selectByIdWithoutTenant(1L);
    }

    @Test
    void search_UserNotBelongToTenant_ThrowsException() {
        // Given
        userDO.setTenantId(2L); // 不同的租户ID
        when(tenantMapper.selectById(1L)).thenReturn(tenantDO);
        when(userMapper.selectByIdWithoutTenant(1L)).thenReturn(userDO);

        // When & Then
        assertThrows(ServiceException.class, () -> regService.search(regSearchReqVO));
    }

    @Test
    void search_UserNotPending_ThrowsException() {
        // Given
        userDO.setStatus(UserStatusEnum.ENABLE.getCode());
        when(tenantMapper.selectById(1L)).thenReturn(tenantDO);
        when(userMapper.selectByIdWithoutTenant(1L)).thenReturn(userDO);

        // When & Then
        assertThrows(ServiceException.class, () -> regService.search(regSearchReqVO));
    }

    // ==================== registerAsMember 方法测试 ====================

    @Test
    void registerAsMember_Success() {
        // Given
        when(userMapper.selectByIdWithoutTenant(1L)).thenReturn(userDO);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userMapper.updateByIdWithoutTenant(any(UserDO.class))).thenReturn(1);

        // When
        boolean result = regService.registerAsMember(regMemberReqVO);

        // Then
        assertTrue(result);
        verify(userMapper).selectByIdWithoutTenant(1L);
        verify(passwordEncoder).encode("password123");
        verify(userMapper)
                .updateByIdWithoutTenant(
                        argThat(
                                user ->
                                        user.getId().equals(1L)
                                                && user.getUsername().equals("newuser")
                                                && user.getPhone().equals("13800138000")
                                                && user.getPassword().equals("encodedPassword")
                                                && user.getStatus()
                                                        .equals(UserStatusEnum.ENABLE.getCode())));
    }

    @Test
    void registerAsMember_UserNotFound_ThrowsException() {
        // Given
        when(userMapper.selectByIdWithoutTenant(1L)).thenReturn(null);

        // When & Then
        assertThrows(ServiceException.class, () -> regService.registerAsMember(regMemberReqVO));
        verify(userMapper).selectByIdWithoutTenant(1L);
        verify(userMapper, never()).updateByIdWithoutTenant(any());
    }

    @Test
    void registerAsMember_UserNotPending_ThrowsException() {
        // Given
        userDO.setStatus(UserStatusEnum.ENABLE.getCode());
        when(userMapper.selectByIdWithoutTenant(1L)).thenReturn(userDO);

        // When & Then
        assertThrows(ServiceException.class, () -> regService.registerAsMember(regMemberReqVO));
        verify(userMapper).selectByIdWithoutTenant(1L);
        verify(userMapper, never()).updateByIdWithoutTenant(any());
    }

    // ==================== registerAsOrganizer 方法测试 ====================

    @Test
    void registerAsOrganizer_Success() {
        // Given
        when(userMapper.selectByUsername("organizer")).thenReturn(null);
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
                            user.setId(1L);
                            return 1;
                        });
        when(tenantMapper.updateById(any(TenantDO.class))).thenReturn(1);
        when(permissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(permissionDO);
        when(roleMapper.insert(any(RoleDO.class)))
                .thenAnswer(
                        invocation -> {
                            RoleDO role = invocation.getArgument(0);
                            role.setId(1L);
                            return 1;
                        });
        when(userRoleMapper.insert(any(UserRoleDO.class))).thenReturn(1);
        when(rolePermissionMapper.insert(any(RolePermissionDO.class))).thenReturn(1);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        // When
        boolean result = regService.registerAsOrganizer(regOrganizerReqVO);

        // Then
        assertTrue(result);
        verify(userMapper).selectByUsername("organizer");
        verify(tenantMapper).insert(any(TenantDO.class));
        verify(userMapper).insert(any(UserDO.class));
        verify(tenantMapper).updateById(any(TenantDO.class));
        verify(permissionMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(roleMapper, times(2)).insert(any(RoleDO.class)); // Organizer和Member两个角色
        verify(userRoleMapper).insert(any(UserRoleDO.class));
        verify(rolePermissionMapper).insert(any(RolePermissionDO.class));
    }

    @Test
    void registerAsOrganizer_UsernameExists_ThrowsException() {
        // Given
        when(userMapper.selectByUsername("organizer")).thenReturn(userDO);

        // When & Then
        assertThrows(
                ServiceException.class, () -> regService.registerAsOrganizer(regOrganizerReqVO));
        verify(userMapper).selectByUsername("organizer");
        verify(tenantMapper, never()).insert(any());
    }

    @Test
    void registerAsOrganizer_TenantInsertFailed_ThrowsException() {
        // Given
        when(userMapper.selectByUsername("organizer")).thenReturn(null);
        when(tenantMapper.insert(any(TenantDO.class))).thenReturn(0);

        // When & Then
        assertThrows(
                ServiceException.class, () -> regService.registerAsOrganizer(regOrganizerReqVO));
        verify(tenantMapper).insert(any(TenantDO.class));
        verify(userMapper, never()).insert(any());
    }

    @Test
    void registerAsOrganizer_UserInsertFailed_ThrowsException() {
        // Given
        when(userMapper.selectByUsername("organizer")).thenReturn(null);
        when(tenantMapper.insert(any(TenantDO.class)))
                .thenAnswer(
                        invocation -> {
                            TenantDO tenant = invocation.getArgument(0);
                            tenant.setId(1L);
                            return 1;
                        });
        when(userMapper.insert(any(UserDO.class))).thenReturn(0);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // When & Then
        assertThrows(
                ServiceException.class, () -> regService.registerAsOrganizer(regOrganizerReqVO));
        verify(userMapper).insert(any(UserDO.class));
        verify(tenantMapper, never()).updateById(any());
    }

    @Test
    void registerAsOrganizer_TenantUpdateFailed_ThrowsException() {
        // Given
        when(userMapper.selectByUsername("organizer")).thenReturn(null);
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
                            user.setId(1L);
                            return 1;
                        });
        when(tenantMapper.updateById(any(TenantDO.class))).thenReturn(0);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // When & Then
        assertThrows(
                ServiceException.class, () -> regService.registerAsOrganizer(regOrganizerReqVO));
        verify(tenantMapper).updateById(any(TenantDO.class));
        verify(permissionMapper, never()).selectOne(any());
    }

    @Test
    void registerAsOrganizer_PermissionNotFound_ThrowsException() {
        // Given
        when(userMapper.selectByUsername("organizer")).thenReturn(null);
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
                            user.setId(1L);
                            return 1;
                        });
        when(tenantMapper.updateById(any(TenantDO.class))).thenReturn(1);
        when(permissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // When & Then
        assertThrows(
                ServiceException.class, () -> regService.registerAsOrganizer(regOrganizerReqVO));
        verify(permissionMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(roleMapper, never()).insert(any());
    }

    @Test
    void registerAsOrganizer_OrganizerRoleInsertFailed_ThrowsException() {
        // Given
        when(userMapper.selectByUsername("organizer")).thenReturn(null);
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
                            user.setId(1L);
                            return 1;
                        });
        when(tenantMapper.updateById(any(TenantDO.class))).thenReturn(1);
        when(permissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(permissionDO);
        when(roleMapper.insert(any(RoleDO.class))).thenReturn(0); // 第一次插入失败
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // When & Then
        assertThrows(
                ServiceException.class, () -> regService.registerAsOrganizer(regOrganizerReqVO));
        verify(roleMapper)
                .insert(argThat(role -> RegServiceImpl.ORGANIZER_ROLE_NAME.equals(role.getName())));
    }

    @Test
    void registerAsOrganizer_MemberRoleInsertFailed_ThrowsException() {
        // Given
        when(userMapper.selectByUsername("organizer")).thenReturn(null);
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
                            user.setId(1L);
                            return 1;
                        });
        when(tenantMapper.updateById(any(TenantDO.class))).thenReturn(1);
        when(permissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(permissionDO);
        when(roleMapper.insert(any(RoleDO.class)))
                .thenAnswer(
                        invocation -> {
                            RoleDO role = invocation.getArgument(0);
                            role.setId(1L);
                            return 1;
                        }) // Organizer角色插入成功
                .thenReturn(0); // Member角色插入失败
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // When & Then
        assertThrows(
                ServiceException.class, () -> regService.registerAsOrganizer(regOrganizerReqVO));
        verify(roleMapper, times(2)).insert(any(RoleDO.class));
    }

    @Test
    void registerAsOrganizer_UserRoleInsertFailed_ThrowsException() {
        // Given
        when(userMapper.selectByUsername("organizer")).thenReturn(null);
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
                            user.setId(1L);
                            return 1;
                        });
        when(tenantMapper.updateById(any(TenantDO.class))).thenReturn(1);
        when(permissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(permissionDO);
        when(roleMapper.insert(any(RoleDO.class)))
                .thenAnswer(
                        invocation -> {
                            RoleDO role = invocation.getArgument(0);
                            role.setId(1L);
                            return 1;
                        });
        when(userRoleMapper.insert(any(UserRoleDO.class))).thenReturn(0);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // When & Then
        assertThrows(
                ServiceException.class, () -> regService.registerAsOrganizer(regOrganizerReqVO));
        verify(userRoleMapper).insert(any(UserRoleDO.class));
        verify(rolePermissionMapper, never()).insert(any());
    }

    @Test
    void registerAsOrganizer_RolePermissionInsertFailed_ThrowsException() {
        // Given
        when(userMapper.selectByUsername("organizer")).thenReturn(null);
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
                            user.setId(1L);
                            return 1;
                        });
        when(tenantMapper.updateById(any(TenantDO.class))).thenReturn(1);
        when(permissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(permissionDO);
        when(roleMapper.insert(any(RoleDO.class)))
                .thenAnswer(
                        invocation -> {
                            RoleDO role = invocation.getArgument(0);
                            role.setId(1L);
                            return 1;
                        });
        when(userRoleMapper.insert(any(UserRoleDO.class))).thenReturn(1);
        when(rolePermissionMapper.insert(any(RolePermissionDO.class))).thenReturn(0);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // When & Then
        assertThrows(
                ServiceException.class, () -> regService.registerAsOrganizer(regOrganizerReqVO));
        verify(rolePermissionMapper).insert(any(RolePermissionDO.class));
    }
}
