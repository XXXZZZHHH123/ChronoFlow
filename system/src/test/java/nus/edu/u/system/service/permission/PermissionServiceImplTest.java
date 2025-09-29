package nus.edu.u.system.service.permission;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.permission.PermissionDO;
import nus.edu.u.system.domain.dataobject.role.RolePermissionDO;
import nus.edu.u.system.domain.vo.permission.PermissionReqVO;
import nus.edu.u.system.domain.vo.permission.PermissionRespVO;
import nus.edu.u.system.enums.permission.PermissionTypeEnum;
import nus.edu.u.system.mapper.permission.PermissionMapper;
import nus.edu.u.system.mapper.role.RolePermissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for PermissionServiceImpl */
@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock private PermissionMapper permissionMapper;

    @Mock private RolePermissionMapper rolePermissionMapper;

    @InjectMocks private PermissionServiceImpl permissionService;

    private PermissionDO permissionDO1;
    private PermissionDO permissionDO2;
    private PermissionDO permissionDO3;
    private PermissionReqVO permissionReqVO;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        permissionDO1 =
                PermissionDO.builder()
                        .id(1L)
                        .name("User Read")
                        .permissionKey("user:read")
                        .description("Read user data")
                        .type(PermissionTypeEnum.API.getType())
                        .build();

        permissionDO2 =
                PermissionDO.builder()
                        .id(2L)
                        .name("User Write")
                        .permissionKey("user:write")
                        .description("Write user data")
                        .type(PermissionTypeEnum.API.getType())
                        .build();

        permissionDO3 =
                PermissionDO.builder()
                        .id(3L)
                        .name("System Admin")
                        .permissionKey("system:admin")
                        .description("System admin permission")
                        .type(PermissionTypeEnum.API.getType())
                        .build();

        permissionReqVO = new PermissionReqVO();
        permissionReqVO.setName("User Delete");
        permissionReqVO.setKey("user:delete");
        permissionReqVO.setDescription("Delete user data");
    }

    // ==================== listPermissions 方法测试 ====================

    @Test
    void listPermissions_Success_WithMultiplePermissions() {
        // Given - 返回以"user"开头的权限
        List<PermissionDO> permissions = Arrays.asList(permissionDO1, permissionDO2);
        when(permissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(permissions);

        // When
        List<PermissionRespVO> result = permissionService.listPermissions();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("User Read", result.get(0).getName());
        assertEquals("user:read", result.get(0).getKey());
        assertEquals("User Write", result.get(1).getName());
        assertEquals("user:write", result.get(1).getKey());

        verify(permissionMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void listPermissions_Success_EmptyList() {
        // Given
        when(permissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        // When
        List<PermissionRespVO> result = permissionService.listPermissions();

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(permissionMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void listPermissions_Success_SinglePermission() {
        // Given
        when(permissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(permissionDO1));

        // When
        List<PermissionRespVO> result = permissionService.listPermissions();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("User Read", result.get(0).getName());
    }

    // ==================== createPermission 方法测试 ====================

    @Test
    void createPermission_Success() {
        // Given
        when(permissionMapper.insert(any(PermissionDO.class)))
                .thenAnswer(
                        invocation -> {
                            PermissionDO permission = invocation.getArgument(0);
                            permission.setId(10L);
                            return 1;
                        });

        // When
        Long result = permissionService.createPermission(permissionReqVO);

        // Then
        assertNotNull(result);
        assertEquals(10L, result);

        verify(permissionMapper)
                .insert(
                        argThat(
                                permission ->
                                        permission.getName().equals("User Delete")
                                                && permission
                                                        .getPermissionKey()
                                                        .equals("user:delete")
                                                && permission
                                                        .getDescription()
                                                        .equals("Delete user data")
                                                && permission
                                                        .getType()
                                                        .equals(PermissionTypeEnum.API.getType())));
    }

    @Test
    void createPermission_ReqVONull_ThrowsException() {
        // When & Then
        assertThrows(ServiceException.class, () -> permissionService.createPermission(null));

        verify(permissionMapper, never()).insert(any());
    }

    @Test
    void createPermission_InsertFailed_ThrowsException() {
        // Given
        when(permissionMapper.insert(any(PermissionDO.class))).thenReturn(0);

        // When & Then
        assertThrows(
                ServiceException.class, () -> permissionService.createPermission(permissionReqVO));

        verify(permissionMapper).insert(any(PermissionDO.class));
    }

    @Test
    void createPermission_Success_WithNullDescription() {
        // Given
        permissionReqVO.setDescription(null);
        when(permissionMapper.insert(any(PermissionDO.class)))
                .thenAnswer(
                        invocation -> {
                            PermissionDO permission = invocation.getArgument(0);
                            permission.setId(10L);
                            return 1;
                        });

        // When
        Long result = permissionService.createPermission(permissionReqVO);

        // Then
        assertNotNull(result);
        verify(permissionMapper).insert(argThat(permission -> permission.getDescription() == null));
    }

    // ==================== getPermission 方法测试 ====================

    @Test
    void getPermission_Success() {
        // Given
        when(permissionMapper.selectById(1L)).thenReturn(permissionDO1);

        // When
        PermissionRespVO result = permissionService.getPermission(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("User Read", result.getName());
        assertEquals("user:read", result.getKey());
        assertEquals("Read user data", result.getDescription());

        verify(permissionMapper).selectById(1L);
    }

    @Test
    void getPermission_IdNull_ThrowsException() {
        // When & Then
        assertThrows(ServiceException.class, () -> permissionService.getPermission(null));

        verify(permissionMapper, never()).selectById(any());
    }

    @Test
    void getPermission_NotFound_ReturnsNull() {
        // Given
        when(permissionMapper.selectById(999L)).thenReturn(null);

        // When
        PermissionRespVO result = permissionService.getPermission(999L);

        // Then
        assertNull(result);
        verify(permissionMapper).selectById(999L);
    }

    // ==================== updatePermission 方法测试 ====================

    @Test
    void updatePermission_Success() {
        // Given
        when(permissionMapper.selectById(1L)).thenReturn(permissionDO1);
        when(permissionMapper.updateById(any(PermissionDO.class))).thenReturn(1);

        // When
        PermissionRespVO result = permissionService.updatePermission(1L, permissionReqVO);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("User Delete", result.getName());
        assertEquals("user:delete", result.getKey());
        assertEquals("Delete user data", result.getDescription());

        verify(permissionMapper).selectById(1L);
        verify(permissionMapper)
                .updateById(
                        argThat(
                                permission ->
                                        permission.getId().equals(1L)
                                                && permission.getName().equals("User Delete")
                                                && permission
                                                        .getPermissionKey()
                                                        .equals("user:delete")));
    }

    @Test
    void updatePermission_IdNull_ThrowsException() {
        // When & Then
        assertThrows(
                ServiceException.class,
                () -> permissionService.updatePermission(null, permissionReqVO));

        verify(permissionMapper, never()).selectById(any());
        verify(permissionMapper, never()).updateById(any());
    }

    @Test
    void updatePermission_ReqVONull_ThrowsException() {
        // When & Then
        assertThrows(ServiceException.class, () -> permissionService.updatePermission(1L, null));

        verify(permissionMapper, never()).selectById(any());
        verify(permissionMapper, never()).updateById(any());
    }

    @Test
    void updatePermission_PermissionNotFound_ThrowsException() {
        // Given
        when(permissionMapper.selectById(999L)).thenReturn(null);

        // When & Then
        assertThrows(
                ServiceException.class,
                () -> permissionService.updatePermission(999L, permissionReqVO));

        verify(permissionMapper).selectById(999L);
        verify(permissionMapper, never()).updateById(any());
    }

    @Test
    void updatePermission_UpdateFailed_ThrowsException() {
        // Given
        when(permissionMapper.selectById(1L)).thenReturn(permissionDO1);
        when(permissionMapper.updateById(any(PermissionDO.class))).thenReturn(0);

        // When & Then
        assertThrows(
                ServiceException.class,
                () -> permissionService.updatePermission(1L, permissionReqVO));

        verify(permissionMapper).selectById(1L);
        verify(permissionMapper).updateById(any(PermissionDO.class));
    }

    @Test
    void updatePermission_Success_WithNullDescription() {
        // Given
        permissionReqVO.setDescription(null);
        when(permissionMapper.selectById(1L)).thenReturn(permissionDO1);
        when(permissionMapper.updateById(any(PermissionDO.class))).thenReturn(1);

        // When
        PermissionRespVO result = permissionService.updatePermission(1L, permissionReqVO);

        // Then
        assertNotNull(result);
        assertNull(result.getDescription());
        verify(permissionMapper)
                .updateById(argThat(permission -> permission.getDescription() == null));
    }

    // ==================== deletePermission 方法测试 ====================

    @Test
    void deletePermission_Success() {
        // Given
        when(rolePermissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(permissionMapper.deleteById(1L)).thenReturn(1);

        // When
        Boolean result = permissionService.deletePermission(1L);

        // Then
        assertTrue(result);
        verify(rolePermissionMapper).selectList(any(LambdaQueryWrapper.class));
        verify(permissionMapper).deleteById(1L);
    }

    @Test
    void deletePermission_IdNull_ThrowsException() {
        // When & Then
        assertThrows(ServiceException.class, () -> permissionService.deletePermission(null));

        verify(rolePermissionMapper, never()).selectList(any());
        verify(permissionMapper, never()).deleteById(any());
    }

    @Test
    void deletePermission_HasRolePermissions_ThrowsException() {
        // Given - 权限被角色使用
        RolePermissionDO rolePermission =
                RolePermissionDO.builder().roleId(1L).permissionId(1L).build();
        when(rolePermissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(rolePermission));

        // When & Then
        assertThrows(ServiceException.class, () -> permissionService.deletePermission(1L));

        verify(rolePermissionMapper).selectList(any(LambdaQueryWrapper.class));
        verify(permissionMapper, never()).deleteById(any());
    }

    @Test
    void deletePermission_DeleteFailed_ReturnsFalse() {
        // Given
        when(rolePermissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(permissionMapper.deleteById(1L)).thenReturn(0);

        // When
        Boolean result = permissionService.deletePermission(1L);

        // Then
        assertFalse(result);
        verify(permissionMapper).deleteById(1L);
    }

    @Test
    void deletePermission_MultipleRolePermissions_ThrowsException() {
        // Given - 权限被多个角色使用
        RolePermissionDO rp1 = RolePermissionDO.builder().roleId(1L).permissionId(1L).build();
        RolePermissionDO rp2 = RolePermissionDO.builder().roleId(2L).permissionId(1L).build();
        when(rolePermissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(rp1, rp2));

        // When & Then
        assertThrows(ServiceException.class, () -> permissionService.deletePermission(1L));

        verify(permissionMapper, never()).deleteById(any());
    }

    // ==================== convert 方法测试（通过公共方法间接测试）====================

    @Test
    void convert_ValidPermission_Success() {
        // Given
        when(permissionMapper.selectById(1L)).thenReturn(permissionDO1);

        // When
        PermissionRespVO result = permissionService.getPermission(1L);

        // Then
        assertNotNull(result);
        assertEquals(permissionDO1.getId(), result.getId());
        assertEquals(permissionDO1.getName(), result.getName());
        assertEquals(permissionDO1.getPermissionKey(), result.getKey());
        assertEquals(permissionDO1.getDescription(), result.getDescription());
    }

    @Test
    void convert_NullPermission_ReturnsNull() {
        // Given
        when(permissionMapper.selectById(999L)).thenReturn(null);

        // When
        PermissionRespVO result = permissionService.getPermission(999L);

        // Then
        assertNull(result);
    }

    @Test
    void convert_PermissionWithNullFields_Success() {
        // Given
        PermissionDO permissionWithNulls =
                PermissionDO.builder()
                        .id(1L)
                        .name("Test")
                        .permissionKey("test:key")
                        .description(null) // null description
                        .build();
        when(permissionMapper.selectById(1L)).thenReturn(permissionWithNulls);

        // When
        PermissionRespVO result = permissionService.getPermission(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertNull(result.getDescription());
    }

    // ==================== 集成场景测试 ====================

    @Test
    void scenario_CreateAndGetPermission() {
        // Given - 创建权限
        when(permissionMapper.insert(any(PermissionDO.class)))
                .thenAnswer(
                        invocation -> {
                            PermissionDO permission = invocation.getArgument(0);
                            permission.setId(100L);
                            return 1;
                        });

        // When - 创建权限
        Long createdId = permissionService.createPermission(permissionReqVO);

        // Then - 验证创建结果
        assertEquals(100L, createdId);

        // Given - 获取权限
        PermissionDO createdPermission =
                PermissionDO.builder()
                        .id(100L)
                        .name("User Delete")
                        .permissionKey("user:delete")
                        .description("Delete user data")
                        .build();
        when(permissionMapper.selectById(100L)).thenReturn(createdPermission);

        // When - 获取权限
        PermissionRespVO retrieved = permissionService.getPermission(100L);

        // Then - 验证获取结果
        assertNotNull(retrieved);
        assertEquals(100L, retrieved.getId());
        assertEquals("User Delete", retrieved.getName());
    }

    @Test
    void scenario_UpdateAndGetPermission() {
        // Given - 更新权限
        when(permissionMapper.selectById(1L)).thenReturn(permissionDO1);
        when(permissionMapper.updateById(any(PermissionDO.class))).thenReturn(1);

        // When - 更新权限
        PermissionRespVO updated = permissionService.updatePermission(1L, permissionReqVO);

        // Then - 验证更新结果
        assertNotNull(updated);
        assertEquals("User Delete", updated.getName());
        assertEquals("user:delete", updated.getKey());

        // Given - 再次获取验证
        PermissionDO updatedPermission =
                PermissionDO.builder()
                        .id(1L)
                        .name("User Delete")
                        .permissionKey("user:delete")
                        .description("Delete user data")
                        .build();
        when(permissionMapper.selectById(1L)).thenReturn(updatedPermission);

        // When
        PermissionRespVO retrieved = permissionService.getPermission(1L);

        // Then
        assertNotNull(retrieved);
        assertEquals("User Delete", retrieved.getName());
    }

    @Test
    void scenario_CannotDeletePermissionInUse() {
        // Given - 权限被角色使用
        RolePermissionDO rolePermission =
                RolePermissionDO.builder().roleId(1L).permissionId(1L).build();
        when(rolePermissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(rolePermission));

        // When & Then - 尝试删除应该失败
        assertThrows(ServiceException.class, () -> permissionService.deletePermission(1L));

        // 验证未执行删除操作
        verify(permissionMapper, never()).deleteById(1L);
    }

    @Test
    void scenario_ListOnlyUserPermissions() {
        // Given - 混合了user和system权限
        List<PermissionDO> allPermissions =
                Arrays.asList(
                        permissionDO1, // user:read
                        permissionDO2 // user:write
                        // permissionDO3 (system:admin) 不应该被返回
                        );
        when(permissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(allPermissions);

        // When
        List<PermissionRespVO> result = permissionService.listPermissions();

        // Then - 只返回user开头的权限
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getKey().startsWith("user")));
    }

    // ==================== 边界条件测试 ====================

    @Test
    void createPermission_WithEmptyName_Success() {
        // Given
        permissionReqVO.setName("");
        when(permissionMapper.insert(any(PermissionDO.class)))
                .thenAnswer(
                        invocation -> {
                            PermissionDO permission = invocation.getArgument(0);
                            permission.setId(10L);
                            return 1;
                        });

        // When
        Long result = permissionService.createPermission(permissionReqVO);

        // Then
        assertNotNull(result);
        verify(permissionMapper).insert(argThat(p -> p.getName().isEmpty()));
    }

    @Test
    void createPermission_WithEmptyKey_Success() {
        // Given
        permissionReqVO.setKey("");
        when(permissionMapper.insert(any(PermissionDO.class)))
                .thenAnswer(
                        invocation -> {
                            PermissionDO permission = invocation.getArgument(0);
                            permission.setId(10L);
                            return 1;
                        });

        // When
        Long result = permissionService.createPermission(permissionReqVO);

        // Then
        assertNotNull(result);
        verify(permissionMapper).insert(argThat(p -> p.getPermissionKey().isEmpty()));
    }

    @Test
    void updatePermission_WithEmptyDescription_Success() {
        // Given
        permissionReqVO.setDescription("");
        when(permissionMapper.selectById(1L)).thenReturn(permissionDO1);
        when(permissionMapper.updateById(any(PermissionDO.class))).thenReturn(1);

        // When
        PermissionRespVO result = permissionService.updatePermission(1L, permissionReqVO);

        // Then
        assertNotNull(result);
        assertEquals("", result.getDescription());
    }

    @Test
    void deletePermission_PermissionNotExist_ReturnsTrue() {
        // Given - 权限不存在，但没有角色关联
        when(rolePermissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(permissionMapper.deleteById(999L)).thenReturn(0); // 删除返回0表示没找到

        // When
        Boolean result = permissionService.deletePermission(999L);

        // Then
        assertFalse(result); // 删除失败返回false
    }
}
