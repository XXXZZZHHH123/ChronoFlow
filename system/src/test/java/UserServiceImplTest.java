//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//import cn.dev33.satoken.stp.StpUtil;
//import java.util.*;
//import nus.edu.u.common.exception.ServiceException;
//import nus.edu.u.system.domain.dataobject.user.UserDO;
//import nus.edu.u.system.domain.dataobject.user.UserRoleDO;
//import nus.edu.u.system.domain.dto.*;
//import nus.edu.u.system.domain.vo.user.BulkUpsertUsersRespVO;
//import nus.edu.u.system.domain.vo.user.UserProfileRespVO;
//import nus.edu.u.system.enums.user.UserStatusEnum;
//import nus.edu.u.system.mapper.role.RoleMapper;
//import nus.edu.u.system.mapper.user.UserMapper;
//import nus.edu.u.system.mapper.user.UserRoleMapper;
//import nus.edu.u.system.service.user.UserServiceImpl;
//import org.junit.jupiter.api.*;
//import org.mockito.*;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.util.ReflectionTestUtils;
//
///**
// * 单元测试覆盖 UserServiceImpl 的核心逻辑： - createUserWithRoleIds - updateUserWithRoleIds（含角色差异同步） -
// * softDeleteUser / restoreUser - disableUser / enableUser - bulkUpsertUsers（含 self 代理与
// * REQUIRES_NEW） - processSingleRowWithNewTx - getAllUserProfiles（mock 静态 StpUtil）
// */
//@SpringBootTest(classes = UserServiceImpl.class)
//class UserServiceImplTest {
//
//  @Mock private UserMapper userMapper;
//  @Mock private UserRoleMapper userRoleMapper;
//  @Mock private RoleMapper roleMapper;
//
//  // Spy + InjectMocks：允许对同类其他方法打桩
//  @Spy @InjectMocks private UserServiceImpl userService;
//
//  @BeforeEach
//  void setUp() {
//    MockitoAnnotations.openMocks(this);
//    ReflectionTestUtils.setField(userService, "self", userService);
//  }
//
//  // ============ createUserWithRoleIds ============
//
//  @Test
//  void createUser_success() {
//    CreateUserDTO dto =
//        CreateUserDTO.builder().email("t1@ex.com").remark("r").roleIds(List.of(1L, 2L)).build();
//
//    when(userMapper.existsEmail("t1@ex.com", null)).thenReturn(false);
//    when(roleMapper.countByIds(List.of(1L, 2L))).thenReturn(2);
//    when(userMapper.insert(any(UserDO.class)))
//        .thenAnswer(
//            inv -> {
//              UserDO u = inv.getArgument(0);
//              u.setId(100L);
//              return 1;
//            });
//    when(userRoleMapper.insert(any(UserRoleDO.class))).thenReturn(1);
//
//    Long id = userService.createUserWithRoleIds(dto);
//
//    assertEquals(100L, id);
//    verify(userRoleMapper, times(2)).insert(any(UserRoleDO.class));
//  }
//
//  @Test
//  void createUser_emailExists() {
//    CreateUserDTO dto = CreateUserDTO.builder().email("dup@ex.com").roleIds(List.of(1L)).build();
//    when(userMapper.existsEmail("dup@ex.com", null)).thenReturn(true);
//
//    assertThrows(ServiceException.class, () -> userService.createUserWithRoleIds(dto));
//    verify(userMapper, never()).insert(any());
//  }
//
//  @Test
//  void createUser_roleNotFound() {
//    CreateUserDTO dto =
//        CreateUserDTO.builder().email("t2@ex.com").roleIds(List.of(9L, 10L)).build();
//
//    when(userMapper.existsEmail("t2@ex.com", null)).thenReturn(false);
//    when(roleMapper.countByIds(List.of(9L, 10L))).thenReturn(1); // 少一个
//    assertThrows(ServiceException.class, () -> userService.createUserWithRoleIds(dto));
//  }
//
//  // ============ updateUserWithRoleIds ============
//
//  @Test
//  void updateUser_success_withRoleDiff() {
//    UpdateUserDTO dto =
//        UpdateUserDTO.builder()
//            .id(200L)
//            .email("new@ex.com")
//            .remark("after")
//            .roleIds(List.of(1L, 3L))
//            .build();
//
//    UserDO db = new UserDO();
//    db.setId(200L);
//    db.setDeleted(false);
//
//    when(userMapper.selectById(200L)).thenReturn(db);
//    when(userMapper.existsEmail("new@ex.com", 200L)).thenReturn(false);
//    when(userMapper.update(any(), any())).thenReturn(1);
//    when(roleMapper.countByIds(List.of(1L, 3L))).thenReturn(2);
//
//    // 现有角色：[1]，目标：[1,3] → 需要新增 3
//    when(userRoleMapper.selectAliveRoleIdsByUser(200L)).thenReturn(List.of(1L));
//    // 角色差异相关批处理（返回值不关心）
//    when(userRoleMapper.batchLogicalDelete(anyLong(), anyList())).thenReturn(3); // 假设影响3行
//    doNothing().when(userRoleMapper).batchRevive(eq(200L), anySet());
//    doNothing().when(userRoleMapper).insertMissing(eq(200L), anySet());
//
//    when(userMapper.selectById(200L)).thenReturn(db);
//
//    UserDO updated = userService.updateUserWithRoleIds(dto);
//    assertNotNull(updated);
//    verify(userRoleMapper, times(1)).insertMissing(eq(200L), anySet());
//  }
//
//  @Test
//  void updateUser_notFoundOrDeleted() {
//    UpdateUserDTO dto = UpdateUserDTO.builder().id(201L).build();
//    when(userMapper.selectById(201L)).thenReturn(null);
//    assertThrows(ServiceException.class, () -> userService.updateUserWithRoleIds(dto));
//
//    UserDO del = new UserDO();
//    del.setId(202L);
//    del.setDeleted(true);
//    when(userMapper.selectById(202L)).thenReturn(del);
//    assertThrows(
//        ServiceException.class,
//        () -> userService.updateUserWithRoleIds(UpdateUserDTO.builder().id(202L).build()));
//  }
//
//  @Test
//  void updateUser_emailExists() {
//    UpdateUserDTO dto = UpdateUserDTO.builder().id(203L).email("dup@ex.com").build();
//    UserDO db = new UserDO();
//    db.setId(203L);
//    db.setDeleted(false);
//    when(userMapper.selectById(203L)).thenReturn(db);
//    when(userMapper.existsEmail("dup@ex.com", 203L)).thenReturn(true);
//    assertThrows(ServiceException.class, () -> userService.updateUserWithRoleIds(dto));
//  }
//
//  // ============ softDelete / restore ============
//
//  @Test
//  void softDelete_success() {
//    UserDO db = new UserDO();
//    db.setId(300L);
//    db.setDeleted(false);
//    when(userMapper.selectRawById(300L)).thenReturn(db);
//    when(userMapper.update(any(), any())).thenReturn(1);
//    when(userRoleMapper.delete(any())).thenReturn(2);
//
//    assertDoesNotThrow(() -> userService.softDeleteUser(300L));
//    verify(userRoleMapper, times(1)).delete(any());
//  }
//
//  @Test
//  void softDelete_alreadyDeleted_orNotFound() {
//    when(userMapper.selectRawById(301L)).thenReturn(null);
//    assertThrows(ServiceException.class, () -> userService.softDeleteUser(301L));
//
//    UserDO db = new UserDO();
//    db.setId(302L);
//    db.setDeleted(true);
//    when(userMapper.selectRawById(302L)).thenReturn(db);
//    assertThrows(ServiceException.class, () -> userService.softDeleteUser(302L));
//  }
//
//  @Test
//  void restore_success() {
//    UserDO db = new UserDO();
//    db.setId(400L);
//    db.setDeleted(true);
//    when(userMapper.selectRawById(400L)).thenReturn(db);
//    when(userMapper.update(any(), any())).thenReturn(1);
//
//    assertDoesNotThrow(() -> userService.restoreUser(400L));
//  }
//
//  @Test
//  void restore_notDeleted_orNotFound() {
//    when(userMapper.selectRawById(401L)).thenReturn(null);
//    assertThrows(ServiceException.class, () -> userService.restoreUser(401L));
//
//    UserDO db = new UserDO();
//    db.setId(402L);
//    db.setDeleted(false);
//    when(userMapper.selectRawById(402L)).thenReturn(db);
//    assertThrows(ServiceException.class, () -> userService.restoreUser(402L));
//  }
//
//  // ============ disable / enable ============
//
//  @Test
//  void disable_success() {
//    UserDO db = new UserDO();
//    db.setId(500L);
//    db.setDeleted(false);
//    db.setStatus(UserStatusEnum.ENABLE.getCode());
//    when(userMapper.selectById(500L)).thenReturn(db);
//    when(userMapper.update(any(), any())).thenReturn(1);
//
//    assertDoesNotThrow(() -> userService.disableUser(500L));
//  }
//
//  @Test
//  void disable_alreadyDisabled_orNotFound() {
//    when(userMapper.selectById(501L)).thenReturn(null);
//    assertThrows(ServiceException.class, () -> userService.disableUser(501L));
//
//    UserDO db = new UserDO();
//    db.setId(502L);
//    db.setDeleted(false);
//    db.setStatus(UserStatusEnum.DISABLE.getCode());
//    when(userMapper.selectById(502L)).thenReturn(db);
//    assertThrows(ServiceException.class, () -> userService.disableUser(502L));
//  }
//
//  @Test
//  void enable_success() {
//    UserDO db = new UserDO();
//    db.setId(600L);
//    db.setDeleted(false);
//    db.setStatus(UserStatusEnum.DISABLE.getCode());
//    when(userMapper.selectById(600L)).thenReturn(db);
//    when(userMapper.update(any(), any())).thenReturn(1);
//
//    assertDoesNotThrow(() -> userService.enableUser(600L));
//  }
//
//  @Test
//  void enable_alreadyEnabled_orNotFound() {
//    when(userMapper.selectById(601L)).thenReturn(null);
//    assertThrows(ServiceException.class, () -> userService.enableUser(601L));
//
//    UserDO db = new UserDO();
//    db.setId(602L);
//    db.setDeleted(false);
//    db.setStatus(UserStatusEnum.ENABLE.getCode());
//    when(userMapper.selectById(602L)).thenReturn(db);
//    assertThrows(ServiceException.class, () -> userService.enableUser(602L));
//  }
//
//  // ============ bulkUpsertUsers + processSingleRowWithNewTx ============
//
//  @Test
//  void bulkUpsert_success_mixedCreateAndUpdate() {
//    CreateUserDTO r1 = CreateUserDTO.builder().email("a@ex.com").roleIds(List.of(1L)).build();
//    CreateUserDTO r2 = CreateUserDTO.builder().email("b@ex.com").roleIds(List.of(2L)).build();
//
//    // 预查已存在邮箱
//    when(userMapper.selectExistingEmails(List.of("a@ex.com", "b@ex.com")))
//        .thenReturn((Set<String>) List.of("a@ex.com"));
//
//    // 行内分支：a 存在（更新），b 不存在（创建）
//    doReturn(false).when(userService).processSingleRowWithNewTx(eq(r1), eq(true)); // updated
//    doReturn(true).when(userService).processSingleRowWithNewTx(eq(r2), eq(false)); // created
//
//    BulkUpsertUsersRespVO resp = userService.bulkUpsertUsers(List.of(r1, r2));
//
//    assertEquals(2, resp.getTotalRows());
//    assertEquals(1, resp.getCreatedCount());
//    assertEquals(1, resp.getUpdatedCount());
//    assertEquals(0, resp.getFailedCount());
//  }
//
//  @Test
//  void processSingleRowWithNewTx_createPath() {
//    CreateUserDTO row = CreateUserDTO.builder().email("n@ex.com").roleIds(List.of(1L)).build();
//    // 打桩：createUserWithRoleIds 不真正执行业务
//    doReturn(777L).when(userService).createUserWithRoleIds(row);
//
//    boolean created = userService.processSingleRowWithNewTx(row, false);
//    assertTrue(created);
//  }
//
//  @Test
//  void processSingleRowWithNewTx_updatePath() {
//    CreateUserDTO row = CreateUserDTO.builder().email("e@ex.com").roleIds(List.of(1L)).build();
//    when(userMapper.selectIdByEmail("e@ex.com")).thenReturn(900L);
//    // 打桩 update
//    doReturn(new UserDO()).when(userService).updateUserWithRoleIds(any(UpdateUserDTO.class));
//
//    boolean created = userService.processSingleRowWithNewTx(row, true);
//    assertFalse(created);
//    verify(userService, times(1)).updateUserWithRoleIds(any(UpdateUserDTO.class));
//  }
//
//  // ============ getAllUserProfiles ============
//  @Test
//  void getAllUserProfiles_filterSelf_andMapFields() {
//    // 1) 造角色
//    RoleDTO r1 = new RoleDTO(10L, "R10", "r10", 0, "ok");
//    RoleDTO r2 = new RoleDTO(20L, "R20", "r20", 0, "ok");
//
//    // 2) 造“自己”的一条记录（会被过滤掉）
//    UserRoleDTO me = new UserRoleDTO();
//    me.setUserId(100L);
//    me.setUsername("me");
//    me.setEmail("me@ex.com");
//    me.setPhone("111");
//    me.setStatus(UserStatusEnum.ENABLE.getCode());
//    me.setRoles(List.of(r1)); // ✅ List<RoleDTO>
//
//    // 3) 造“别人”的一条记录（会被保留）
//    UserRoleDTO other = new UserRoleDTO();
//    other.setUserId(200L);
//    other.setUsername("you");
//    other.setEmail("you@ex.com");
//    other.setPhone("222");
//    other.setStatus(UserStatusEnum.PENDING.getCode()); // PENDING → 未注册
//    other.setRoles(List.of(r2)); // ✅ List<RoleDTO>
//
//    // 4) mapper 返回两条
//    when(userMapper.selectAllUsersWithRoles()).thenReturn(List.of(me, other));
//
//    // 5) mock 静态方法：当前登录用户 id = 100（过滤掉 me）
//    try (MockedStatic<StpUtil> mocked = mockStatic(StpUtil.class)) {
//      mocked.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
//
//      List<UserProfileRespVO> vos = userService.getAllUserProfiles();
//
//      // 6) 断言
//      assertEquals(1, vos.size());
//      UserProfileRespVO vo = vos.get(0);
//      assertEquals(200L, vo.getId());
//      assertEquals("you@ex.com", vo.getEmail());
//      assertEquals("222", vo.getPhone());
//      assertEquals(List.of(20L), vo.getRoles()); // RoleDTO → id 列表
//      assertFalse(vo.isRegistered()); // PENDING → 未注册
//    }
//  }
//}
