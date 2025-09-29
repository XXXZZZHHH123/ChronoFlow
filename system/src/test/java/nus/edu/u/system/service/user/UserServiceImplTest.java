package nus.edu.u.system.service.user;

import static nus.edu.u.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.CreateUserDTO;
import nus.edu.u.system.domain.dto.RoleDTO;
import nus.edu.u.system.domain.dto.UpdateUserDTO;
import nus.edu.u.system.domain.dto.UserRoleDTO;
import nus.edu.u.system.domain.vo.user.BulkUpsertUsersRespVO;
import nus.edu.u.system.domain.vo.user.UserProfileRespVO;
import nus.edu.u.system.enums.user.UserStatusEnum;
import nus.edu.u.system.mapper.role.RoleMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.mapper.user.UserRoleMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceImplTest {
    @Mock private UserMapper userMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserService userServiceProxy;
    @InjectMocks private UserServiceImpl userService;

    private MockedStatic<StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.remove(UserDO.class);
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "testMapper");
        assistant.setCurrentNamespace("testMapperNamespace");
        TableInfoHelper.initTableInfo(assistant, UserDO.class);

        stpUtilMock = mockStatic(StpUtil.class);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(0L);
    }

    @AfterEach
    void tearDown() {
        if (stpUtilMock != null) {
            stpUtilMock.close();
        }
        TableInfoHelper.remove(UserDO.class);
    }

    @Test
    void testGetUserByUsername() {
        UserDO user = new UserDO();
        when(userMapper.selectByUsername("testuser")).thenReturn(user);
        assertEquals(user, userService.getUserByUsername("testuser"));
    }

    @Test
    void testIsPasswordMatch() {
        when(passwordEncoder.matches("raw", "encoded")).thenReturn(true);
        assertTrue(userService.isPasswordMatch("raw", "encoded"));
    }

    @Test
    void testSelectUserWithRole() {
        UserRoleDTO dto = new UserRoleDTO();
        when(userMapper.selectUserWithRole(1L)).thenReturn(dto);
        assertEquals(dto, userService.selectUserWithRole(1L));
    }

    @Test
    void testSelectUserById() {
        UserDO user = new UserDO();
        when(userMapper.selectById(2L)).thenReturn(user);
        assertEquals(user, userService.selectUserById(2L));
    }

    @Test
    void testCreateUserWithRoleIds_success() {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setEmail("a@b.com");
        dto.setRoleIds(List.of(2L));
        dto.setRemark("r");
        when(userMapper.existsEmail(anyString(), any())).thenReturn(false);
        when(roleMapper.countByIds(anyList())).thenReturn(1);
        when(userMapper.insert(any()))
                .thenAnswer(
                        inv -> {
                            ((UserDO) inv.getArgument(0)).setId(10L);
                            return 1;
                        });
        when(userRoleMapper.insert(any())).thenReturn(1);

        assertEquals(10L, userService.createUserWithRoleIds(dto));
    }

    @Test
    void testCreateUserWithRoleIds_noRoles() {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setEmail("a@b.com");
        dto.setRoleIds(Collections.emptyList());
        when(userMapper.existsEmail(anyString(), any())).thenReturn(false);
        when(roleMapper.countByIds(eq(Collections.emptyList()))).thenReturn(0);
        when(userMapper.insert(any()))
                .thenAnswer(
                        inv -> {
                            ((UserDO) inv.getArgument(0)).setId(11L);
                            return 1;
                        });

        long id = userService.createUserWithRoleIds(dto);

        assertEquals(11L, id);
        verify(userRoleMapper, never()).insert(any());
    }

    @Test
    void testCreateUserWithRoleIds_emailExists() {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setEmail("a@b.com");
        dto.setRoleIds(List.of(2L));
        when(userMapper.existsEmail(anyString(), any())).thenReturn(true);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.createUserWithRoleIds(dto));
        assertEquals(EMAIL_EXIST.getCode(), ex.getCode());
    }

    @Test
    void testCreateUserWithRoleIds_forbiddenRole() {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setEmail("a@b.com");
        dto.setRoleIds(List.of(1L));

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.createUserWithRoleIds(dto));
        assertEquals(ROLE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void testCreateUserWithRoleIds_roleNotFound() {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setEmail("a@b.com");
        dto.setRoleIds(List.of(2L));
        when(userMapper.existsEmail(anyString(), any())).thenReturn(false);
        when(roleMapper.countByIds(anyList())).thenReturn(0);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.createUserWithRoleIds(dto));
        assertEquals(ROLE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void testCreateUserWithRoleIds_insertFailure() {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setEmail("a@b.com");
        dto.setRoleIds(List.of(2L));
        when(userMapper.existsEmail(anyString(), any())).thenReturn(false);
        when(roleMapper.countByIds(anyList())).thenReturn(1);
        when(userMapper.insert(any())).thenReturn(0);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.createUserWithRoleIds(dto));
        assertEquals(USER_INSERT_FAILURE.getCode(), ex.getCode());
    }

    @Test
    void testCreateUserWithRoleIds_roleBindFailure() {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setEmail("a@b.com");
        dto.setRoleIds(List.of(2L));
        when(userMapper.existsEmail(anyString(), any())).thenReturn(false);
        when(roleMapper.countByIds(anyList())).thenReturn(1);
        when(userMapper.insert(any()))
                .thenAnswer(
                        inv -> {
                            ((UserDO) inv.getArgument(0)).setId(20L);
                            return 1;
                        });
        when(userRoleMapper.insert(any())).thenReturn(0);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.createUserWithRoleIds(dto));
        assertEquals(USER_ROLE_BIND_FAILURE.getCode(), ex.getCode());
    }

    @Test
    void testUpdateUserWithRoleIds_success() {
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setId(1L);
        dto.setEmail("a@b.com");
        dto.setRoleIds(List.of(2L));
        dto.setRemark("r");
        UserDO dbUser = new UserDO();
        dbUser.setId(1L);
        dbUser.setDeleted(false);
        when(userMapper.selectById(1L)).thenReturn(dbUser);
        when(userMapper.existsEmail(anyString(), any())).thenReturn(false);
        when(userMapper.update(any(), any())).thenReturn(1);
        when(roleMapper.countByIds(anyList())).thenReturn(1);
        when(userRoleMapper.selectAliveRoleIdsByUser(1L)).thenReturn(List.of());
        when(userRoleMapper.batchRevive(anyLong(), anyCollection())).thenReturn(0);
        when(userRoleMapper.insertMissing(anyLong(), anyCollection())).thenReturn(0);

        assertEquals(dbUser, userService.updateUserWithRoleIds(dto));
    }

    @Test
    void testUpdateUserWithRoleIds_rolesNullSkipsSync() {
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setId(1L);
        dto.setRoleIds(null);
        UserDO dbUser = new UserDO();
        dbUser.setId(1L);
        dbUser.setDeleted(false);
        when(userMapper.selectById(1L)).thenReturn(dbUser);
        when(userMapper.existsEmail(anyString(), any())).thenReturn(false);
        when(userMapper.update(any(), any())).thenReturn(1);

        userService.updateUserWithRoleIds(dto);

        verify(userRoleMapper, never()).selectAliveRoleIdsByUser(anyLong());
    }

    @Test
    void testUpdateUserWithRoleIds_clearRoles() {
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setId(1L);
        dto.setRoleIds(Collections.emptyList());
        UserDO dbUser = new UserDO();
        dbUser.setId(1L);
        dbUser.setDeleted(false);
        when(userMapper.selectById(1L)).thenReturn(dbUser);
        when(userMapper.existsEmail(anyString(), any())).thenReturn(false);
        when(userMapper.update(any(), any())).thenReturn(1);
        when(userRoleMapper.selectAliveRoleIdsByUser(1L)).thenReturn(List.of(5L));
        when(userRoleMapper.batchLogicalDelete(eq(1L), eq(Set.of(5L)))).thenReturn(1);

        userService.updateUserWithRoleIds(dto);

        verify(roleMapper, never()).countByIds(anyList());
        verify(userRoleMapper).batchLogicalDelete(eq(1L), eq(Set.of(5L)));
        verify(userRoleMapper, never()).batchRevive(anyLong(), anyCollection());
        verify(userRoleMapper, never()).insertMissing(anyLong(), anyCollection());
    }

    @Test
    void testUpdateUserWithRoleIds_userMissing() {
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(null);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.updateUserWithRoleIds(dto));
        assertEquals(USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void testUpdateUserWithRoleIds_userDeleted() {
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setId(1L);
        UserDO dbUser = new UserDO();
        dbUser.setDeleted(true);
        when(userMapper.selectById(1L)).thenReturn(dbUser);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.updateUserWithRoleIds(dto));
        assertEquals(USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void testUpdateUserWithRoleIds_emailExists() {
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setId(1L);
        dto.setEmail("new@b.com");
        UserDO dbUser = new UserDO();
        dbUser.setDeleted(false);
        when(userMapper.selectById(1L)).thenReturn(dbUser);
        when(userMapper.existsEmail(eq("new@b.com"), eq(1L))).thenReturn(true);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.updateUserWithRoleIds(dto));
        assertEquals(EMAIL_EXIST.getCode(), ex.getCode());
    }

    @Test
    void testUpdateUserWithRoleIds_updateFailure() {
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setId(1L);
        UserDO dbUser = new UserDO();
        dbUser.setDeleted(false);
        when(userMapper.selectById(1L)).thenReturn(dbUser);
        when(userMapper.existsEmail(anyString(), any())).thenReturn(false);
        when(userMapper.update(any(), any())).thenReturn(0);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.updateUserWithRoleIds(dto));
        assertEquals(UPDATE_FAILURE.getCode(), ex.getCode());
    }

    @Test
    void testUpdateUserWithRoleIds_forbiddenRole() {
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setId(1L);
        dto.setRoleIds(List.of(1L));
        UserDO dbUser = new UserDO();
        dbUser.setDeleted(false);
        when(userMapper.selectById(1L)).thenReturn(dbUser);
        when(userMapper.update(any(), any())).thenReturn(1);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.updateUserWithRoleIds(dto));
        assertEquals(ROLE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void testUpdateUserWithRoleIds_roleNotFound() {
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setId(1L);
        dto.setRoleIds(List.of(3L));
        UserDO dbUser = new UserDO();
        dbUser.setDeleted(false);
        when(userMapper.selectById(1L)).thenReturn(dbUser);
        when(userMapper.update(any(), any())).thenReturn(1);
        when(roleMapper.countByIds(anyList())).thenReturn(0);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.updateUserWithRoleIds(dto));
        assertEquals(ROLE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void testSoftDeleteUser_success() {
        UserDO db = new UserDO();
        db.setId(1L);
        db.setDeleted(false);
        when(userMapper.selectRawById(1L)).thenReturn(db);
        when(userMapper.update(any(), any())).thenReturn(1);
        when(userRoleMapper.delete(any())).thenReturn(1);

        assertDoesNotThrow(() -> userService.softDeleteUser(1L));
    }

    @Test
    void testSoftDeleteUser_notFound() {
        when(userMapper.selectRawById(1L)).thenReturn(null);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.softDeleteUser(1L));
        assertEquals(USER_NOTFOUND.getCode(), ex.getCode());
    }

    @Test
    void testSoftDeleteUser_alreadyDeleted() {
        UserDO db = new UserDO();
        db.setDeleted(true);
        when(userMapper.selectRawById(1L)).thenReturn(db);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.softDeleteUser(1L));
        assertEquals(USER_ALREADY_DELETED.getCode(), ex.getCode());
    }

    @Test
    void testSoftDeleteUser_updateFailure() {
        UserDO db = new UserDO();
        db.setDeleted(false);
        when(userMapper.selectRawById(1L)).thenReturn(db);
        when(userMapper.update(any(), any())).thenReturn(0);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.softDeleteUser(1L));
        assertEquals(UPDATE_FAILURE.getCode(), ex.getCode());
        verify(userRoleMapper, never()).delete(any());
    }

    @Test
    void testRestoreUser_success() {
        UserDO db = new UserDO();
        db.setId(1L);
        db.setDeleted(true);
        when(userMapper.selectRawById(1L)).thenReturn(db);
        when(userMapper.update(any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> userService.restoreUser(1L));
    }

    @Test
    void testRestoreUser_notFound() {
        when(userMapper.selectRawById(1L)).thenReturn(null);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.restoreUser(1L));
        assertEquals(USER_NOTFOUND.getCode(), ex.getCode());
    }

    @Test
    void testRestoreUser_notDeleted() {
        UserDO db = new UserDO();
        db.setDeleted(false);
        when(userMapper.selectRawById(1L)).thenReturn(db);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.restoreUser(1L));
        assertEquals(USER_NOT_DELETED.getCode(), ex.getCode());
    }

    @Test
    void testRestoreUser_updateFailure() {
        UserDO db = new UserDO();
        db.setDeleted(true);
        when(userMapper.selectRawById(1L)).thenReturn(db);
        when(userMapper.update(any(), any())).thenReturn(0);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.restoreUser(1L));
        assertEquals(UPDATE_FAILURE.getCode(), ex.getCode());
    }

    @Test
    void testDisableUser_success() {
        UserDO db = new UserDO();
        db.setId(1L);
        db.setDeleted(false);
        db.setStatus(UserStatusEnum.ENABLE.getCode());
        when(userMapper.selectById(1L)).thenReturn(db);
        when(userMapper.update(any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> userService.disableUser(1L));
    }

    @Test
    void testDisableUser_notFound() {
        when(userMapper.selectById(1L)).thenReturn(null);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.disableUser(1L));
        assertEquals(USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void testDisableUser_deleted() {
        UserDO db = new UserDO();
        db.setDeleted(true);
        when(userMapper.selectById(1L)).thenReturn(db);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.disableUser(1L));
        assertEquals(USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void testDisableUser_alreadyDisabled() {
        UserDO db = new UserDO();
        db.setDeleted(false);
        db.setStatus(UserStatusEnum.DISABLE.getCode());
        when(userMapper.selectById(1L)).thenReturn(db);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.disableUser(1L));
        assertEquals(USER_ALREADY_DISABLED.getCode(), ex.getCode());
    }

    @Test
    void testDisableUser_updateFailure() {
        UserDO db = new UserDO();
        db.setDeleted(false);
        db.setStatus(UserStatusEnum.ENABLE.getCode());
        when(userMapper.selectById(1L)).thenReturn(db);
        when(userMapper.update(any(), any())).thenReturn(0);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.disableUser(1L));
        assertEquals(USER_DISABLE_FAILURE.getCode(), ex.getCode());
    }

    @Test
    void testEnableUser_success() {
        UserDO db = new UserDO();
        db.setId(1L);
        db.setDeleted(false);
        db.setStatus(UserStatusEnum.DISABLE.getCode());
        when(userMapper.selectById(1L)).thenReturn(db);
        when(userMapper.update(any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> userService.enableUser(1L));
    }

    @Test
    void testEnableUser_notFound() {
        when(userMapper.selectById(1L)).thenReturn(null);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.enableUser(1L));
        assertEquals(USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void testEnableUser_deleted() {
        UserDO db = new UserDO();
        db.setDeleted(true);
        when(userMapper.selectById(1L)).thenReturn(db);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.enableUser(1L));
        assertEquals(USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void testEnableUser_alreadyEnabled() {
        UserDO db = new UserDO();
        db.setDeleted(false);
        db.setStatus(UserStatusEnum.ENABLE.getCode());
        when(userMapper.selectById(1L)).thenReturn(db);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.enableUser(1L));
        assertEquals(USER_ALREADY_ENABLED.getCode(), ex.getCode());
    }

    @Test
    void testEnableUser_updateFailure() {
        UserDO db = new UserDO();
        db.setDeleted(false);
        db.setStatus(UserStatusEnum.DISABLE.getCode());
        when(userMapper.selectById(1L)).thenReturn(db);
        when(userMapper.update(any(), any())).thenReturn(0);

        ServiceException ex =
                assertThrows(ServiceException.class, () -> userService.enableUser(1L));
        assertEquals(USER_ENABLE_FAILURE.getCode(), ex.getCode());
    }

    @Test
    void testGetAllUserProfiles_returnsProfiles() {
        UserRoleDTO dto = new UserRoleDTO();
        dto.setUserId(1L);
        dto.setUsername("u");
        dto.setEmail("e");
        dto.setPhone("p");
        dto.setRoles(List.of(new RoleDTO(2L, null, null, null, null)));
        dto.setStatus(UserStatusEnum.ENABLE.getCode());
        when(userMapper.selectAllUsersWithRoles()).thenReturn(List.of(dto));

        List<UserProfileRespVO> result = userService.getAllUserProfiles();
        assertEquals(1, result.size());
        UserProfileRespVO vo = result.get(0);
        assertEquals("u", vo.getName());
        assertEquals(List.of(2L), vo.getRoles());
        assertTrue(vo.isRegistered());
    }

    @Test
    void testGetAllUserProfiles_emptyList() {
        when(userMapper.selectAllUsersWithRoles()).thenReturn(Collections.emptyList());

        assertTrue(userService.getAllUserProfiles().isEmpty());
    }

    @Test
    void testGetAllUserProfiles_filtersCurrentUser() {
        UserRoleDTO current = new UserRoleDTO();
        current.setUserId(0L);
        current.setUsername("self");
        current.setStatus(UserStatusEnum.ENABLE.getCode());

        UserRoleDTO other = new UserRoleDTO();
        other.setUserId(2L);
        other.setUsername("other");
        other.setEmail("o");
        other.setPhone("p");
        other.setRoles(List.of());
        other.setStatus(UserStatusEnum.DISABLE.getCode());

        when(userMapper.selectAllUsersWithRoles()).thenReturn(List.of(current, other));

        List<UserProfileRespVO> result = userService.getAllUserProfiles();
        assertEquals(1, result.size());
        assertEquals("other", result.get(0).getName());
        assertTrue(result.get(0).isRegistered());
    }

    @Test
    void testGetAllUserProfiles_handlesNullRolesAndPending() {
        UserRoleDTO dto = new UserRoleDTO();
        dto.setUserId(5L);
        dto.setUsername("pending-user");
        dto.setStatus(UserStatusEnum.PENDING.getCode());
        dto.setRoles(null);
        when(userMapper.selectAllUsersWithRoles()).thenReturn(List.of(dto));

        List<UserProfileRespVO> result = userService.getAllUserProfiles();
        assertEquals(1, result.size());
        UserProfileRespVO vo = result.get(0);
        assertTrue(vo.getRoles().isEmpty());
        assertFalse(vo.isRegistered());
    }

    @Test
    void testBulkUpsertUsers_emptyList() {
        BulkUpsertUsersRespVO resp = userService.bulkUpsertUsers(Collections.emptyList());
        assertEquals(0, resp.getTotalRows());
        assertEquals(0, resp.getCreatedCount());
        assertEquals(0, resp.getUpdatedCount());
        assertEquals(0, resp.getFailedCount());
    }

    @Test
    void testBulkUpsertUsers_nullInput() {
        BulkUpsertUsersRespVO resp = userService.bulkUpsertUsers(null);
        assertEquals(0, resp.getTotalRows());
        assertEquals(0, resp.getFailedCount());
    }

    @Test
    void testBulkUpsertUsers_countsCreatedAndUpdated() {
        CreateUserDTO row1 =
                CreateUserDTO.builder()
                        .rowIndex(1)
                        .email(" User1@Example.com ")
                        .roleIds(List.of(2L, 2L))
                        .remark("r1")
                        .build();
        CreateUserDTO row2 =
                CreateUserDTO.builder()
                        .rowIndex(2)
                        .email("user2@example.com")
                        .roleIds(List.of(3L))
                        .remark("r2")
                        .build();
        when(userServiceProxy.tryCreateOrFallbackToUpdate(anyString(), any(), anyList()))
                .thenReturn(true)
                .thenReturn(false);

        BulkUpsertUsersRespVO resp = userService.bulkUpsertUsers(List.of(row1, row2));

        assertEquals(2, resp.getTotalRows());
        assertEquals(1, resp.getCreatedCount());
        assertEquals(1, resp.getUpdatedCount());
        assertEquals(0, resp.getFailedCount());
        verify(userServiceProxy)
                .tryCreateOrFallbackToUpdate("user1@example.com", "r1", List.of(2L));
        verify(userServiceProxy)
                .tryCreateOrFallbackToUpdate("user2@example.com", "r2", List.of(3L));
    }

    @Test
    void testBulkUpsertUsers_serviceExceptionRecorded() {
        CreateUserDTO invalidEmail =
                CreateUserDTO.builder().rowIndex(1).email("bad").roleIds(List.of(2L)).build();
        CreateUserDTO emptyRoles =
                CreateUserDTO.builder()
                        .rowIndex(3)
                        .email("user3@example.com")
                        .roleIds(Collections.emptyList())
                        .build();
        CreateUserDTO forbiddenRole =
                CreateUserDTO.builder()
                        .rowIndex(2)
                        .email("user@example.com")
                        .roleIds(List.of(1L))
                        .build();

        BulkUpsertUsersRespVO resp =
                userService.bulkUpsertUsers(List.of(invalidEmail, forbiddenRole, emptyRoles));

        assertEquals(3, resp.getFailedCount());
        List<String> reasons =
                resp.getFailures().stream()
                        .map(BulkUpsertUsersRespVO.RowFailure::getReason)
                        .toList();
        assertTrue(reasons.contains(INVALID_EMAIL.getMsg()));
        assertTrue(reasons.contains(ROLE_NOT_FOUND.getMsg()));
        assertTrue(reasons.contains(EMPTY_ROLEIDS.getMsg()));
    }

    @Test
    void testBulkUpsertUsers_internalErrorRecorded() {
        CreateUserDTO row =
                CreateUserDTO.builder()
                        .rowIndex(3)
                        .email("user@example.com")
                        .roleIds(List.of(2L))
                        .build();
        when(userServiceProxy.tryCreateOrFallbackToUpdate(anyString(), any(), anyList()))
                .thenThrow(new RuntimeException("boom"));

        BulkUpsertUsersRespVO resp = userService.bulkUpsertUsers(List.of(row));

        assertEquals(1, resp.getFailedCount());
        assertEquals("INTERNAL_ERROR: RuntimeException", resp.getFailures().get(0).getReason());
    }

    @Test
    void testBulkUpsertUsers_handlesNullRowIndexAndRoles() {
        CreateUserDTO row = CreateUserDTO.builder().email("user@example.com").roleIds(null).build();

        BulkUpsertUsersRespVO resp = userService.bulkUpsertUsers(List.of(row));

        assertEquals(1, resp.getFailedCount());
        BulkUpsertUsersRespVO.RowFailure failure = resp.getFailures().get(0);
        assertEquals(0, failure.getRowIndex());
        assertEquals(EMPTY_ROLEIDS.getMsg(), failure.getReason());
    }

    @Test
    void testBulkUpsertUsers_handlesNullEmail() {
        CreateUserDTO row =
                CreateUserDTO.builder().rowIndex(4).email(null).roleIds(List.of(2L)).build();

        BulkUpsertUsersRespVO resp = userService.bulkUpsertUsers(List.of(row));

        assertEquals(1, resp.getFailedCount());
        assertEquals(EMAIL_BLANK.getMsg(), resp.getFailures().get(0).getReason());
    }

    @Test
    void testTryCreateOrFallbackToUpdate_createSuccess() {
        List<Long> roleIds = List.of(2L);
        when(userMapper.existsEmail(anyString(), any())).thenReturn(false);
        when(roleMapper.countByIds(eq(roleIds))).thenReturn(1);
        when(userMapper.insert(any()))
                .thenAnswer(
                        inv -> {
                            ((UserDO) inv.getArgument(0)).setId(6L);
                            return 1;
                        });
        when(userRoleMapper.insert(any())).thenReturn(1);

        assertTrue(userService.tryCreateOrFallbackToUpdate("user@example.com", "remark", roleIds));
        verify(userMapper, never()).selectIdByEmail(anyString());
    }

    @Test
    void testTryCreateOrFallbackToUpdate_fallsBackToUpdate() {
        List<Long> roleIds = List.of(2L);
        when(userMapper.existsEmail(eq("user@example.com"), isNull())).thenReturn(true);
        when(userMapper.selectIdByEmail("user@example.com")).thenReturn(8L);
        UserDO dbUser = new UserDO();
        dbUser.setId(8L);
        dbUser.setDeleted(false);
        when(userMapper.selectById(8L)).thenReturn(dbUser);
        when(userMapper.update(any(), any())).thenReturn(1);
        when(roleMapper.countByIds(eq(roleIds))).thenReturn(1);
        when(userRoleMapper.selectAliveRoleIdsByUser(8L)).thenReturn(List.of());
        when(userRoleMapper.batchRevive(anyLong(), anyCollection())).thenReturn(0);
        when(userRoleMapper.insertMissing(anyLong(), anyCollection())).thenReturn(0);

        assertFalse(userService.tryCreateOrFallbackToUpdate("user@example.com", "remark", roleIds));
    }

    @Test
    void testTryCreateOrFallbackToUpdate_missingUserId() {
        when(userMapper.existsEmail(eq("user@example.com"), isNull())).thenReturn(true);
        when(userMapper.selectIdByEmail("user@example.com")).thenReturn(null);

        ServiceException ex =
                assertThrows(
                        ServiceException.class,
                        () ->
                                userService.tryCreateOrFallbackToUpdate(
                                        "user@example.com", "remark", List.of(2L)));
        assertEquals(NULL_USERID.getCode(), ex.getCode());
    }

    @Test
    void testTryCreateOrFallbackToUpdate_rethrowsNonEmailExist() {
        when(userMapper.existsEmail(anyString(), any())).thenReturn(false);
        when(roleMapper.countByIds(anyList())).thenReturn(0);

        ServiceException ex =
                assertThrows(
                        ServiceException.class,
                        () ->
                                userService.tryCreateOrFallbackToUpdate(
                                        "user@example.com", "remark", List.of(2L)));
        assertEquals(ROLE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void testTryCreateOrFallbackToUpdate_updateArgsValidation() {
        List<Long> emptyRoles = Collections.emptyList();
        when(userMapper.existsEmail(eq("user@example.com"), isNull())).thenReturn(true);
        when(userMapper.selectIdByEmail("user@example.com")).thenReturn(9L);

        ServiceException ex =
                assertThrows(
                        ServiceException.class,
                        () ->
                                userService.tryCreateOrFallbackToUpdate(
                                        "user@example.com", "remark", emptyRoles));
        assertEquals(EMPTY_ROLEIDS.getCode(), ex.getCode());
    }

    @Test
    void testUpdateUserWithRoleIds_syncsRoleDifferences() {
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setId(1L);
        dto.setRoleIds(List.of(6L, 7L));
        UserDO dbUser = new UserDO();
        dbUser.setId(1L);
        dbUser.setDeleted(false);
        when(userMapper.selectById(1L)).thenReturn(dbUser);
        when(userMapper.existsEmail(anyString(), any())).thenReturn(false);
        when(userMapper.update(any(), any())).thenReturn(1);
        when(roleMapper.countByIds(eq(List.of(6L, 7L)))).thenReturn(2);
        when(userRoleMapper.selectAliveRoleIdsByUser(1L)).thenReturn(List.of(5L, 6L));
        when(userRoleMapper.batchLogicalDelete(eq(1L), eq(Set.of(5L)))).thenReturn(1);
        when(userRoleMapper.batchRevive(eq(1L), eq(Set.of(7L)))).thenReturn(1);
        when(userRoleMapper.insertMissing(eq(1L), eq(Set.of(7L)))).thenReturn(1);

        userService.updateUserWithRoleIds(dto);

        verify(userRoleMapper).batchLogicalDelete(eq(1L), eq(Set.of(5L)));
        verify(userRoleMapper).batchRevive(eq(1L), eq(Set.of(7L)));
        verify(userRoleMapper).insertMissing(eq(1L), eq(Set.of(7L)));
    }

    @Test
    void testGetAliveRoleIdsByUserId() {
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(2L, 3L));
        assertEquals(List.of(2L, 3L), userService.getAliveRoleIdsByUserId(1L));
    }

    @Test
    void testIsValidEmailHandlesNull() throws Exception {
        var method = UserServiceImpl.class.getDeclaredMethod("isValidEmail", String.class);
        method.setAccessible(true);
        assertFalse((Boolean) method.invoke(userService, (Object) null));
    }

    @Test
    void testValidateUpdateArgsHandlesNull() throws Exception {
        var method = UserServiceImpl.class.getDeclaredMethod("validateUpdateArgs", List.class);
        method.setAccessible(true);
        var invocation =
                assertThrows(
                        InvocationTargetException.class,
                        () -> method.invoke(userService, (Object) null));
        Throwable cause = invocation.getCause();
        assertTrue(cause instanceof ServiceException);
        assertEquals(EMPTY_ROLEIDS.getCode(), ((ServiceException) cause).getCode());
    }
}
