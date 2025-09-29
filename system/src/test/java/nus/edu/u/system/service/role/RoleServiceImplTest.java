package nus.edu.u.system.service.role;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.*;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.permission.PermissionDO;
import nus.edu.u.system.domain.dataobject.role.RoleDO;
import nus.edu.u.system.domain.dataobject.role.RolePermissionDO;
import nus.edu.u.system.domain.dataobject.user.UserRoleDO;
import nus.edu.u.system.domain.dto.RoleDTO;
import nus.edu.u.system.domain.dto.UserRoleDTO;
import nus.edu.u.system.domain.vo.role.RoleAssignReqVO;
import nus.edu.u.system.domain.vo.role.RoleReqVO;
import nus.edu.u.system.domain.vo.role.RoleRespVO;
import nus.edu.u.system.mapper.permission.PermissionMapper;
import nus.edu.u.system.mapper.role.RoleMapper;
import nus.edu.u.system.mapper.role.RolePermissionMapper;
import nus.edu.u.system.mapper.user.UserRoleMapper;
import nus.edu.u.system.service.auth.AuthService;
import nus.edu.u.system.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for RoleServiceImpl */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock private RoleMapper roleMapper;

    @Mock private PermissionMapper permissionMapper;

    @Mock private RolePermissionMapper rolePermissionMapper;

    @Mock private UserRoleMapper userRoleMapper;

    @Mock private UserService userService;

    @Mock private AuthService authService;

    @InjectMocks private RoleServiceImpl roleService;

    private RoleDO roleDO;
    private RoleDO organizerRole;
    private RoleReqVO roleReqVO;
    private PermissionDO permissionDO1;
    private PermissionDO permissionDO2;
    private RoleAssignReqVO roleAssignReqVO;
    private UserRoleDTO userRoleDTO;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        roleDO =
                RoleDO.builder()
                        .id(1L)
                        .name("Admin")
                        .roleKey("ADMIN")
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .permissionList(Arrays.asList(1L, 2L))
                        .build();

        organizerRole =
                RoleDO.builder()
                        .id(2L)
                        .name("Organizer")
                        .roleKey("ORGANIZER")
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .permissionList(Collections.emptyList())
                        .build();

        roleReqVO = new RoleReqVO();
        roleReqVO.setName("Manager");
        roleReqVO.setKey("MANAGER");
        roleReqVO.setPermissions(Arrays.asList(1L, 2L));

        permissionDO1 =
                PermissionDO.builder()
                        .id(1L)
                        .name("Read")
                        .permissionKey("READ")
                        .description("Read permission")
                        .build();

        permissionDO2 =
                PermissionDO.builder()
                        .id(2L)
                        .name("Write")
                        .permissionKey("WRITE")
                        .description("Write permission")
                        .build();

        roleAssignReqVO = new RoleAssignReqVO();
        roleAssignReqVO.setUserId(1L);
        roleAssignReqVO.setRoles(Arrays.asList(1L, 2L));

        RoleDTO roleDTO1 = RoleDTO.builder().id(1L).roleKey("ADMIN").build();
        RoleDTO roleDTO2 = RoleDTO.builder().id(2L).roleKey("USER").build();

        userRoleDTO =
                UserRoleDTO.builder()
                        .userId(1L)
                        .username("testuser")
                        .roles(Arrays.asList(roleDTO1, roleDTO2))
                        .build();
    }

    // ==================== listRoles 方法测试 ====================

    @Test
    void listRoles_Success_WithRoles() {
        // Given
        List<RoleDO> roles = Arrays.asList(roleDO, organizerRole);
        when(roleMapper.selectList(null)).thenReturn(roles);
        when(permissionMapper.selectBatchIds(anyList()))
                .thenReturn(Arrays.asList(permissionDO1, permissionDO2));

        // When
        List<RoleRespVO> result = roleService.listRoles();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size()); // ORGANIZER被过滤掉
        assertEquals("Admin", result.get(0).getName());
        assertEquals("ADMIN", result.get(0).getKey());
        verify(roleMapper).selectList(null);
    }

    @Test
    void listRoles_Success_EmptyList() {
        // Given
        when(roleMapper.selectList(null)).thenReturn(Collections.emptyList());

        // When
        List<RoleRespVO> result = roleService.listRoles();

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(roleMapper).selectList(null);
    }

    @Test
    void listRoles_Success_OnlyOrganizerRole() {
        // Given
        when(roleMapper.selectList(null)).thenReturn(Collections.singletonList(organizerRole));

        // When
        List<RoleRespVO> result = roleService.listRoles();

        // Then
        assertNotNull(result);
        assertEquals(0, result.size()); // ORGANIZER被过滤掉
    }

    @Test
    void listRoles_Success_WithNullPermissionList() {
        // Given
        roleDO.setPermissionList(null);
        when(roleMapper.selectList(null)).thenReturn(Collections.singletonList(roleDO));

        // When
        List<RoleRespVO> result = roleService.listRoles();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getPermissions());
    }

    // ==================== createRole 方法测试 ====================

    @Test
    void createRole_Success_WithPermissions() {
        // Given
        when(roleMapper.insert(any(RoleDO.class)))
                .thenAnswer(
                        invocation -> {
                            RoleDO role = invocation.getArgument(0);
                            role.setId(1L);
                            return 1;
                        });
        when(rolePermissionMapper.insert(any(RolePermissionDO.class))).thenReturn(1);
        when(permissionMapper.selectBatchIds(anyList()))
                .thenReturn(Arrays.asList(permissionDO1, permissionDO2));

        // When
        RoleRespVO result = roleService.createRole(roleReqVO);

        // Then
        assertNotNull(result);
        assertEquals("Manager", result.getName());
        assertEquals("MANAGER", result.getKey());
        verify(roleMapper).insert(any(RoleDO.class));
        verify(rolePermissionMapper, times(2)).insert(any(RolePermissionDO.class));
    }

    @Test
    void createRole_Success_WithoutPermissions() {
        // Given
        roleReqVO.setPermissions(Collections.emptyList());
        when(roleMapper.insert(any(RoleDO.class)))
                .thenAnswer(
                        invocation -> {
                            RoleDO role = invocation.getArgument(0);
                            role.setId(1L);
                            return 1;
                        });

        // When
        RoleRespVO result = roleService.createRole(roleReqVO);

        // Then
        assertNotNull(result);
        assertEquals("Manager", result.getName());
        verify(roleMapper).insert(any(RoleDO.class));
        verify(rolePermissionMapper, never()).insert(any());
    }

    @Test
    void createRole_Success_WithNullPermissions() {
        // Given
        roleReqVO.setPermissions(null);
        when(roleMapper.insert(any(RoleDO.class)))
                .thenAnswer(
                        invocation -> {
                            RoleDO role = invocation.getArgument(0);
                            role.setId(1L);
                            return 1;
                        });

        // When
        RoleRespVO result = roleService.createRole(roleReqVO);

        // Then
        assertNotNull(result);
        verify(rolePermissionMapper, never()).insert(any());
    }

    @Test
    void createRole_RoleInsertFailed_ThrowsException() {
        // Given
        when(roleMapper.insert(any(RoleDO.class))).thenReturn(0);

        // When & Then
        assertThrows(ServiceException.class, () -> roleService.createRole(roleReqVO));
        verify(roleMapper).insert(any(RoleDO.class));
        verify(rolePermissionMapper, never()).insert(any());
    }

    @Test
    void createRole_PermissionInsertFailed_ThrowsException() {
        // Given
        when(roleMapper.insert(any(RoleDO.class)))
                .thenAnswer(
                        invocation -> {
                            RoleDO role = invocation.getArgument(0);
                            role.setId(1L);
                            return 1;
                        });
        when(rolePermissionMapper.insert(any(RolePermissionDO.class))).thenReturn(0);

        // When & Then
        assertThrows(ServiceException.class, () -> roleService.createRole(roleReqVO));
        verify(rolePermissionMapper).insert(any(RolePermissionDO.class));
    }

    // ==================== getRole 方法测试 ====================

    @Test
    void getRole_Success() {
        // Given
        when(roleMapper.selectById(1L)).thenReturn(roleDO);
        when(permissionMapper.selectBatchIds(anyList()))
                .thenReturn(Arrays.asList(permissionDO1, permissionDO2));

        // When
        RoleRespVO result = roleService.getRole(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Admin", result.getName());
        assertEquals("ADMIN", result.getKey());
        assertEquals(2, result.getPermissions().size());
        verify(roleMapper).selectById(1L);
    }

    @Test
    void getRole_RoleIdNull_ThrowsException() {
        // When & Then
        assertThrows(ServiceException.class, () -> roleService.getRole(null));
        verify(roleMapper, never()).selectById(any());
    }

    @Test
    void getRole_RoleNotFound_ThrowsException() {
        // Given
        when(roleMapper.selectById(1L)).thenReturn(null);

        // When & Then
        assertThrows(ServiceException.class, () -> roleService.getRole(1L));
        verify(roleMapper).selectById(1L);
    }

    @Test
    void getRole_Success_WithEmptyPermissions() {
        // Given
        roleDO.setPermissionList(Collections.emptyList());
        when(roleMapper.selectById(1L)).thenReturn(roleDO);

        // When
        RoleRespVO result = roleService.getRole(1L);

        // Then
        assertNotNull(result);
        assertNull(result.getPermissions());
        verify(permissionMapper, never()).selectBatchIds(anyList());
    }

    // ==================== deleteRole 方法测试 ====================

    @Test
    void deleteRole_RoleIdNull_ThrowsException() {
        // When & Then
        assertThrows(ServiceException.class, () -> roleService.deleteRole(null));
        verify(roleMapper, never()).selectById(any());
        verify(roleMapper, never()).deleteById(any());
    }

    @Test
    void deleteRole_RoleNotFound_ThrowsException() {
        // Given
        when(roleMapper.selectById(1L)).thenReturn(null);

        // When & Then
        assertThrows(ServiceException.class, () -> roleService.deleteRole(1L));
        verify(roleMapper).selectById(1L);
        verify(roleMapper, never()).deleteById(any());
    }

    @Test
    void deleteRole_OrganizerRole_ThrowsException() {
        // Given
        when(roleMapper.selectById(2L)).thenReturn(organizerRole);

        // When & Then
        assertThrows(ServiceException.class, () -> roleService.deleteRole(2L));
        verify(roleMapper).selectById(2L);
        verify(userRoleMapper, never()).selectList(any());
        verify(roleMapper, never()).deleteById(any());
    }

    @Test
    void deleteRole_MemberRole_ThrowsException() {
        // Given
        RoleDO memberRole =
                RoleDO.builder()
                        .id(3L)
                        .name("Member")
                        .roleKey("MEMBER")
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .build();
        when(roleMapper.selectById(3L)).thenReturn(memberRole);

        // When & Then
        assertThrows(ServiceException.class, () -> roleService.deleteRole(3L));
        verify(roleMapper).selectById(3L);
        verify(userRoleMapper, never()).selectList(any());
        verify(roleMapper, never()).deleteById(any());
    }

    @Test
    void deleteRole_HasUserRoles_ThrowsException() {
        // Given
        List<UserRoleDO> userRoles =
                Arrays.asList(UserRoleDO.builder().userId(1L).roleId(1L).build());
        when(roleMapper.selectById(1L)).thenReturn(roleDO);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(userRoles);

        // When & Then
        assertThrows(ServiceException.class, () -> roleService.deleteRole(1L));
        verify(roleMapper).selectById(1L);
        verify(userRoleMapper).selectList(any(LambdaQueryWrapper.class));
        verify(roleMapper, never()).deleteById(any());
    }

    @Test
    void deleteRole_Success() {
        // Given
        when(roleMapper.selectById(1L)).thenReturn(roleDO);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(roleMapper.deleteById(1L)).thenReturn(1);
        when(rolePermissionMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        // When
        roleService.deleteRole(1L);

        // Then
        verify(roleMapper).selectById(1L);
        verify(userRoleMapper).selectList(any(LambdaQueryWrapper.class));
        verify(roleMapper).deleteById(1L);
        verify(rolePermissionMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void deleteRole_MultipleUserRoles_ThrowsException() {
        // Given - 角色被多个用户使用
        List<UserRoleDO> userRoles =
                Arrays.asList(
                        UserRoleDO.builder().userId(1L).roleId(1L).build(),
                        UserRoleDO.builder().userId(2L).roleId(1L).build());
        when(roleMapper.selectById(1L)).thenReturn(roleDO);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(userRoles);

        // When & Then
        assertThrows(ServiceException.class, () -> roleService.deleteRole(1L));
        verify(roleMapper).selectById(1L);
        verify(userRoleMapper).selectList(any(LambdaQueryWrapper.class));
        verify(roleMapper, never()).deleteById(any());
    }

    // ==================== updateRole 方法测试 ====================

    @Test
    void updateRole_Success_AddPermissions() {
        // Given
        roleDO.setPermissionList(Arrays.asList(1L)); // 原有权限：1
        roleReqVO.setPermissions(Arrays.asList(1L, 2L)); // 新权限：1,2

        when(roleMapper.selectById(1L)).thenReturn(roleDO);
        when(roleMapper.updateById(any(RoleDO.class))).thenReturn(1);
        when(rolePermissionMapper.insert(any(RolePermissionDO.class))).thenReturn(1);
        when(permissionMapper.selectBatchIds(anyList()))
                .thenReturn(Arrays.asList(permissionDO1, permissionDO2));

        // When
        RoleRespVO result = roleService.updateRole(1L, roleReqVO);

        // Then
        assertNotNull(result);
        assertEquals("Manager", result.getName());
        verify(roleMapper).updateById(any(RoleDO.class));
        verify(rolePermissionMapper).insert(argThat(rp -> rp.getPermissionId().equals(2L)));
        verify(rolePermissionMapper, never()).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void updateRole_Success_RemovePermissions() {
        // Given
        roleDO.setPermissionList(Arrays.asList(1L, 2L)); // 原有权限：1,2
        roleReqVO.setPermissions(Arrays.asList(1L)); // 新权限：1

        when(roleMapper.selectById(1L)).thenReturn(roleDO);
        when(roleMapper.updateById(any(RoleDO.class))).thenReturn(1);
        when(rolePermissionMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(permissionMapper.selectBatchIds(anyList()))
                .thenReturn(Collections.singletonList(permissionDO1));

        // When
        RoleRespVO result = roleService.updateRole(1L, roleReqVO);

        // Then
        assertNotNull(result);
        verify(rolePermissionMapper).delete(any(LambdaQueryWrapper.class));
        verify(rolePermissionMapper, never()).insert(any());
    }

    @Test
    void updateRole_Success_ReplacePermissions() {
        // Given
        roleDO.setPermissionList(Arrays.asList(1L)); // 原有权限：1
        roleReqVO.setPermissions(Arrays.asList(2L)); // 新权限：2

        when(roleMapper.selectById(1L)).thenReturn(roleDO);
        when(roleMapper.updateById(any(RoleDO.class))).thenReturn(1);
        when(rolePermissionMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(rolePermissionMapper.insert(any(RolePermissionDO.class))).thenReturn(1);
        when(permissionMapper.selectBatchIds(anyList()))
                .thenReturn(Collections.singletonList(permissionDO2));

        // When
        RoleRespVO result = roleService.updateRole(1L, roleReqVO);

        // Then
        assertNotNull(result);
        verify(rolePermissionMapper).delete(any(LambdaQueryWrapper.class));
        verify(rolePermissionMapper).insert(any(RolePermissionDO.class));
    }

    @Test
    void updateRole_Success_NoPermissionChange() {
        // Given
        roleDO.setPermissionList(Arrays.asList(1L, 2L));
        roleReqVO.setPermissions(Arrays.asList(1L, 2L));

        when(roleMapper.selectById(1L)).thenReturn(roleDO);
        when(roleMapper.updateById(any(RoleDO.class))).thenReturn(1);
        when(permissionMapper.selectBatchIds(anyList()))
                .thenReturn(Arrays.asList(permissionDO1, permissionDO2));

        // When
        RoleRespVO result = roleService.updateRole(1L, roleReqVO);

        // Then
        assertNotNull(result);
        verify(rolePermissionMapper, never()).delete(any());
        verify(rolePermissionMapper, never()).insert(any());
    }

    @Test
    void updateRole_RoleIdNull_ThrowsException() {
        // When & Then
        assertThrows(ServiceException.class, () -> roleService.updateRole(null, roleReqVO));
        verify(roleMapper, never()).selectById(any());
    }

    @Test
    void updateRole_RoleReqVONull_ThrowsException() {
        // When & Then
        assertThrows(ServiceException.class, () -> roleService.updateRole(1L, null));
        verify(roleMapper, never()).selectById(any());
    }

    @Test
    void updateRole_RoleNotFound_ThrowsException() {
        // Given
        when(roleMapper.selectById(1L)).thenReturn(null);

        // When & Then
        assertThrows(ServiceException.class, () -> roleService.updateRole(1L, roleReqVO));
        verify(roleMapper).selectById(1L);
        verify(roleMapper, never()).updateById(any());
    }

    @Test
    void updateRole_UpdateFailed_ThrowsException() {
        // Given
        when(roleMapper.selectById(1L)).thenReturn(roleDO);
        when(roleMapper.updateById(any(RoleDO.class))).thenReturn(0);

        // When & Then
        assertThrows(ServiceException.class, () -> roleService.updateRole(1L, roleReqVO));
        verify(roleMapper).updateById(any());
    }

    @Test
    void updateRole_PermissionInsertFailed_ThrowsException() {
        // Given
        roleDO.setPermissionList(Arrays.asList(1L));
        roleReqVO.setPermissions(Arrays.asList(1L, 2L));

        when(roleMapper.selectById(1L)).thenReturn(roleDO);
        when(roleMapper.updateById(any(RoleDO.class))).thenReturn(1);
        when(rolePermissionMapper.insert(any(RolePermissionDO.class))).thenReturn(0);

        // When & Then
        assertThrows(ServiceException.class, () -> roleService.updateRole(1L, roleReqVO));
        verify(rolePermissionMapper).insert(any());
    }

    // ==================== assignRoles 方法测试 ====================

    @Test
    void assignRoles_Success_AddRoles() {
        // Given - 用户原有角色：1, 新角色：1,2
        RoleDTO roleDTO = RoleDTO.builder().id(1L).roleKey("ADMIN").build();
        userRoleDTO.setRoles(Collections.singletonList(roleDTO));
        roleAssignReqVO.setRoles(Arrays.asList(1L, 2L));

        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(userRoleMapper.insert(any(UserRoleDO.class))).thenReturn(1);

        // When
        roleService.assignRoles(roleAssignReqVO);

        // Then
        verify(userService).selectUserWithRole(1L);
        verify(userRoleMapper).insert(argThat(ur -> ur.getRoleId().equals(2L)));
        verify(userRoleMapper, never()).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void assignRoles_Success_RemoveRoles() {
        // Given - 用户原有角色：1,2, 新角色：1
        roleAssignReqVO.setRoles(Collections.singletonList(1L));

        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        // When
        roleService.assignRoles(roleAssignReqVO);

        // Then
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper, never()).insert(any());
    }

    @Test
    void assignRoles_Success_ReplaceRoles() {
        // Given - 用户原有角色：1, 新角色：2
        RoleDTO roleDTO = RoleDTO.builder().id(1L).roleKey("ADMIN").build();
        userRoleDTO.setRoles(Collections.singletonList(roleDTO));
        roleAssignReqVO.setRoles(Collections.singletonList(2L));

        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(userRoleMapper.insert(any(UserRoleDO.class))).thenReturn(1);

        // When
        roleService.assignRoles(roleAssignReqVO);

        // Then
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper).insert(any(UserRoleDO.class));
    }

    @Test
    void assignRoles_Success_NoChange() {
        // Given - 用户原有角色：1,2, 新角色：1,2
        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);

        // When
        roleService.assignRoles(roleAssignReqVO);

        // Then
        verify(userRoleMapper, never()).delete(any());
        verify(userRoleMapper, never()).insert(any());
    }

    @Test
    void assignRoles_Success_PreserveOrganizerRole() {
        // Given - 用户有ORGANIZER角色，即使新角色列表中没有也应该保留
        RoleDTO adminRole = RoleDTO.builder().id(1L).roleKey("ADMIN").build();
        RoleDTO organizerRoleDTO = RoleDTO.builder().id(10L).roleKey("ORGANIZER").build();
        userRoleDTO.setRoles(Arrays.asList(adminRole, organizerRoleDTO));

        // 新角色列表只包含ADMIN，不包含ORGANIZER
        roleAssignReqVO.setRoles(Collections.singletonList(1L));

        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);

        // When
        roleService.assignRoles(roleAssignReqVO);

        // Then
        // ORGANIZER角色被保护，不应该被删除
        verify(userRoleMapper, never()).delete(any());
        verify(userRoleMapper, never()).insert(any());
    }

    @Test
    void assignRoles_Success_AddRolesWithOrganizerPreserved() {
        // Given - 用户有ORGANIZER和ADMIN角色，要添加USER角色
        RoleDTO adminRole = RoleDTO.builder().id(1L).roleKey("ADMIN").build();
        RoleDTO organizerRoleDTO = RoleDTO.builder().id(10L).roleKey("ORGANIZER").build();
        userRoleDTO.setRoles(Arrays.asList(adminRole, organizerRoleDTO));

        // 新角色列表包含ADMIN和USER，不包含ORGANIZER（但应该被自动保留）
        roleAssignReqVO.setRoles(Arrays.asList(1L, 2L));

        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(userRoleMapper.insert(any(UserRoleDO.class))).thenReturn(1);

        // When
        roleService.assignRoles(roleAssignReqVO);

        // Then
        // 应该只插入USER角色（ID=2），不删除ORGANIZER
        verify(userRoleMapper).insert(argThat(ur -> ur.getRoleId().equals(2L)));
        verify(userRoleMapper, never()).delete(any());
    }

    @Test
    void assignRoles_Success_RemoveRolesButKeepOrganizer() {
        // Given - 用户有ORGANIZER、ADMIN、USER三个角色，要删除ADMIN和USER
        RoleDTO adminRole = RoleDTO.builder().id(1L).roleKey("ADMIN").build();
        RoleDTO userRole = RoleDTO.builder().id(2L).roleKey("USER").build();
        RoleDTO organizerRoleDTO = RoleDTO.builder().id(10L).roleKey("ORGANIZER").build();
        userRoleDTO.setRoles(Arrays.asList(adminRole, userRole, organizerRoleDTO));

        // 新角色列表为空（但ORGANIZER应该被保留）
        roleAssignReqVO.setRoles(Collections.emptyList());

        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        // When
        roleService.assignRoles(roleAssignReqVO);

        // Then
        // 应该删除ADMIN和USER，但保留ORGANIZER
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper, never()).insert(any());
    }

    @Test
    void assignRoles_Success_NoOrganizerRole() {
        // Given - 用户没有ORGANIZER角色，普通角色分配
        RoleDTO adminRole = RoleDTO.builder().id(1L).roleKey("ADMIN").build();
        userRoleDTO.setRoles(Collections.singletonList(adminRole));
        roleAssignReqVO.setRoles(Arrays.asList(1L, 2L));

        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(userRoleMapper.insert(any(UserRoleDO.class))).thenReturn(1);

        // When
        roleService.assignRoles(roleAssignReqVO);

        // Then
        verify(userRoleMapper).insert(argThat(ur -> ur.getRoleId().equals(2L)));
        verify(userRoleMapper, never()).delete(any());
    }

    @Test
    void assignRoles_ReqVONull_ThrowsException() {
        // When & Then
        assertThrows(ServiceException.class, () -> roleService.assignRoles(null));
        verify(userService, never()).selectUserWithRole(any());
    }

    @Test
    void assignRoles_UserNotFound_ThrowsException() {
        // Given
        when(userService.selectUserWithRole(1L)).thenReturn(null);

        // When & Then
        assertThrows(ServiceException.class, () -> roleService.assignRoles(roleAssignReqVO));
        verify(userService).selectUserWithRole(1L);
        verify(userRoleMapper, never()).delete(any());
        verify(userRoleMapper, never()).insert(any());
    }

    @Test
    void assignRoles_DeleteFailed_ThrowsException() {
        // Given - 用户没有ORGANIZER角色
        RoleDTO adminRole = RoleDTO.builder().id(1L).roleKey("ADMIN").build();
        RoleDTO userRole = RoleDTO.builder().id(2L).roleKey("USER").build();
        userRoleDTO.setRoles(Arrays.asList(adminRole, userRole));
        roleAssignReqVO.setRoles(Collections.singletonList(1L));

        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);

        // When & Then
        assertThrows(ServiceException.class, () -> roleService.assignRoles(roleAssignReqVO));
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void assignRoles_InsertFailed_ThrowsException() {
        // Given
        RoleDTO roleDTO = RoleDTO.builder().id(1L).roleKey("ADMIN").build();
        userRoleDTO.setRoles(Collections.singletonList(roleDTO));
        roleAssignReqVO.setRoles(Arrays.asList(1L, 2L));

        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(userRoleMapper.insert(any(UserRoleDO.class))).thenReturn(0);

        // When & Then
        assertThrows(ServiceException.class, () -> roleService.assignRoles(roleAssignReqVO));
        verify(userRoleMapper).insert(any());
    }

    @Test
    void assignRoles_Success_EmptyExistingRoles() {
        // Given - 用户原来没有角色，新角色：1,2
        userRoleDTO.setRoles(Collections.emptyList());

        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(userRoleMapper.insert(any(UserRoleDO.class))).thenReturn(1);

        // When
        roleService.assignRoles(roleAssignReqVO);

        // Then
        verify(userRoleMapper, times(2)).insert(any(UserRoleDO.class));
        verify(userRoleMapper, never()).delete(any());
    }

    @Test
    void assignRoles_Success_EmptyNewRolesWithoutOrganizer() {
        // Given - 用户原有角色：1,2（没有ORGANIZER），新角色：空
        RoleDTO adminRole = RoleDTO.builder().id(1L).roleKey("ADMIN").build();
        RoleDTO userRole = RoleDTO.builder().id(2L).roleKey("USER").build();
        userRoleDTO.setRoles(Arrays.asList(adminRole, userRole));
        roleAssignReqVO.setRoles(Collections.emptyList());

        when(userService.selectUserWithRole(1L)).thenReturn(userRoleDTO);
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        // When
        roleService.assignRoles(roleAssignReqVO);

        // Then
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper, never()).insert(any());
    }

    // ==================== convert 方法测试（通过其他方法间接测试）====================

    @Test
    void convert_NullRole_ReturnsNull() {
        // Given
        roleDO.setPermissionList(null); // 设置为null，不会调用selectBatchIds
        when(roleMapper.selectById(1L)).thenReturn(roleDO);
        // 注意：不需要mock permissionMapper.selectBatchIds，因为permissionList为null时不会调用

        // When
        RoleRespVO result = roleService.getRole(1L);

        // Then
        assertNotNull(result);
        assertNull(result.getPermissions());
    }

    @Test
    void convert_WithPermissions_Success() {
        // Given
        when(roleMapper.selectById(1L)).thenReturn(roleDO);
        when(permissionMapper.selectBatchIds(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(permissionDO1, permissionDO2));

        // When
        RoleRespVO result = roleService.getRole(1L);

        // Then
        assertNotNull(result);
        assertNotNull(result.getPermissions());
        assertEquals(2, result.getPermissions().size());
        assertEquals("Read", result.getPermissions().get(0).getName());
        assertEquals("READ", result.getPermissions().get(0).getKey());
        assertEquals("Write", result.getPermissions().get(1).getName());
        assertEquals("WRITE", result.getPermissions().get(1).getKey());
    }

    // ==================== 集成场景测试 ====================

    @Test
    void scenario_CreateAndGetRole() {
        // Given - 创建角色
        when(roleMapper.insert(any(RoleDO.class)))
                .thenAnswer(
                        invocation -> {
                            RoleDO role = invocation.getArgument(0);
                            role.setId(1L);
                            return 1;
                        });
        when(rolePermissionMapper.insert(any(RolePermissionDO.class))).thenReturn(1);
        when(permissionMapper.selectBatchIds(anyList()))
                .thenReturn(Arrays.asList(permissionDO1, permissionDO2));

        // When - 创建角色
        RoleRespVO created = roleService.createRole(roleReqVO);

        // Then - 验证创建结果
        assertNotNull(created);
        assertEquals("Manager", created.getName());

        // Given - 获取角色
        when(roleMapper.selectById(1L))
                .thenReturn(
                        RoleDO.builder()
                                .id(1L)
                                .name("Manager")
                                .roleKey("MANAGER")
                                .permissionList(Arrays.asList(1L, 2L))
                                .build());

        // When - 获取角色
        RoleRespVO retrieved = roleService.getRole(1L);

        // Then - 验证获取结果
        assertNotNull(retrieved);
        assertEquals(created.getName(), retrieved.getName());
        assertEquals(created.getKey(), retrieved.getKey());
    }

    @Test
    void scenario_UpdateRole_ComplexPermissionChanges() {
        // Given - 原有权限：1,2，新权限：2,3
        roleDO.setPermissionList(Arrays.asList(1L, 2L));
        roleReqVO.setPermissions(Arrays.asList(2L, 3L));

        when(roleMapper.selectById(1L)).thenReturn(roleDO);
        when(roleMapper.updateById(any(RoleDO.class))).thenReturn(1);
        when(rolePermissionMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(rolePermissionMapper.insert(any(RolePermissionDO.class))).thenReturn(1);

        PermissionDO permissionDO3 =
                PermissionDO.builder().id(3L).name("Delete").permissionKey("DELETE").build();
        when(permissionMapper.selectBatchIds(anyList()))
                .thenReturn(Arrays.asList(permissionDO2, permissionDO3));

        // When
        RoleRespVO result = roleService.updateRole(1L, roleReqVO);

        // Then
        assertNotNull(result);
        verify(rolePermissionMapper).delete(any(LambdaQueryWrapper.class)); // 删除权限1
        verify(rolePermissionMapper)
                .insert(argThat(rp -> rp.getPermissionId().equals(3L))); // 添加权限3
    }
}
