package nus.edu.u.system.service.group;

import static nus.edu.u.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.*;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.dept.DeptDO;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dataobject.user.UserGroupDO;
import nus.edu.u.system.domain.dto.UserPermissionDTO;
import nus.edu.u.system.domain.dto.UserRoleDTO;
import nus.edu.u.system.domain.vo.group.CreateGroupReqVO;
import nus.edu.u.system.domain.vo.group.GroupRespVO;
import nus.edu.u.system.domain.vo.user.UserProfileRespVO;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.mapper.user.UserGroupMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.service.user.UserService;
import org.apache.ibatis.session.ResultHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

class GroupServiceImplTest {

    private GroupServiceImpl target;
    private GroupService proxy;

    private FakeDeptMapper deptMapper;
    private FakeUserMapper userMapper;
    private FakeEventMapper eventMapper;
    private FakeUserGroupMapper userGroupMapper;
    private FakeTaskMapper taskMapper;
    private StubUserService userService;

    @BeforeEach
    void setUp() throws Exception {
        target = new GroupServiceImpl();
        deptMapper = new FakeDeptMapper();
        userMapper = new FakeUserMapper();
        eventMapper = new FakeEventMapper();
        userGroupMapper = new FakeUserGroupMapper();
        taskMapper = new FakeTaskMapper();
        userService = new StubUserService();

        inject("deptMapper", deptMapper);
        inject("userMapper", userMapper);
        inject("eventMapper", eventMapper);
        inject("userService", userService);
        inject("userGroupMapper", userGroupMapper);
        inject("taskMapper", taskMapper);

        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.setExposeProxy(true);
        proxy = (GroupService) factory.getProxy();
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field field = GroupServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void createGroup_addsLeaderAsMember() {
        EventDO event = EventDO.builder().id(10L).name("Summit").build();
        event.setTenantId(77L);
        eventMapper.put(event);
        userMapper.put(
                UserDO.builder()
                        .id(5L)
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .username("Lead")
                        .build());

        CreateGroupReqVO req = new CreateGroupReqVO();
        req.setEventId(10L);
        req.setName("Ops");
        req.setLeadUserId(5L);
        req.setRemark("on-call");

        Long groupId = proxy.createGroup(req);

        assertThat(groupId).isNotNull();
        assertThat(deptMapper.store().get(groupId).getName()).isEqualTo("Ops");
        assertThat(userGroupMapper.relations())
                .singleElement()
                .extracting(UserGroupDO::getUserId)
                .isEqualTo(5L);
    }

    @Test
    void createGroup_whenEventMissing_throws() {
        CreateGroupReqVO req = new CreateGroupReqVO();
        req.setName("Ops");
        req.setEventId(404L);

        assertThatThrownBy(() -> proxy.createGroup(req))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(EVENT_NOT_FOUND.getCode());
    }

    @Test
    void addMemberToGroup_rejectsUsersAlreadyInAnotherGroup() {
        DeptDO dept = DeptDO.builder().id(1L).eventId(99L).name("Ops").build();
        deptMapper.put(dept);
        userMapper.put(
                UserDO.builder()
                        .id(8L)
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .username("member")
                        .build());
        userGroupMapper.enqueueSelectOne(
                UserGroupDO.builder().id(3L).userId(8L).deptId(123L).eventId(99L).build());

        assertThatThrownBy(() -> proxy.addMemberToGroup(1L, 8L))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(USER_ALREADY_IN_OTHER_GROUP_OF_EVENT.getCode());
    }

    @Test
    void removeMemberFromGroup_preventsRemovingLeader() {
        DeptDO dept = DeptDO.builder().id(2L).eventId(50L).leadUserId(88L).name("Crew").build();
        deptMapper.put(dept);
        userGroupMapper.enqueueSelectOne(
                UserGroupDO.builder().id(6L).userId(88L).deptId(2L).eventId(50L).build());

        assertThatThrownBy(() -> proxy.removeMemberFromGroup(2L, 88L))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(CANNOT_REMOVE_GROUP_LEADER.getCode());
    }

    @Test
    void addMembersToGroup_whenOneFails_throwsAddMembersFailed() {
        DeptDO dept = DeptDO.builder().id(3L).eventId(66L).name("Ops").build();
        deptMapper.put(dept);
        userMapper.put(
                UserDO.builder()
                        .id(101L)
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .username("alpha")
                        .build());

        assertThatThrownBy(() -> proxy.addMembersToGroup(3L, List.of(101L, 202L)))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(ADD_MEMBERS_FAILED.getCode());

        assertThat(userGroupMapper.relations())
                .hasSize(1)
                .first()
                .extracting(UserGroupDO::getUserId)
                .isEqualTo(101L);
    }

    @Test
    void removeMembersToGroup_propagatesFirstFailure() {
        DeptDO dept = DeptDO.builder().id(4L).eventId(70L).name("Ops").leadUserId(1L).build();
        deptMapper.put(dept);
        userGroupMapper.enqueueSelectOne(
                UserGroupDO.builder().id(11L).userId(301L).deptId(4L).eventId(70L).build());
        userGroupMapper.enqueueSelectOne(null);
        taskMapper.queueSelectCount(0L);

        assertThatThrownBy(() -> proxy.removeMembersToGroup(4L, List.of(301L, 302L)))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(USER_NOT_IN_GROUP.getCode());

        assertThat(userGroupMapper.relations()).isEmpty();
    }

    @Test
    void getGroupsByEvent_returnsEnrichedInformation() {
        EventDO event = EventDO.builder().id(90L).name("Expo").build();
        event.setTenantId(1L);
        eventMapper.put(event);
        DeptDO first =
                DeptDO.builder()
                        .id(501L)
                        .eventId(90L)
                        .leadUserId(700L)
                        .name("Crew A")
                        .sort(1)
                        .status(0)
                        .build();
        first.setCreateTime(java.time.LocalDateTime.now());
        first.setUpdateTime(java.time.LocalDateTime.now());
        DeptDO second =
                DeptDO.builder()
                        .id(502L)
                        .eventId(90L)
                        .leadUserId(null)
                        .name("Crew B")
                        .sort(2)
                        .status(1)
                        .build();
        deptMapper.setSelectListResult(List.of(first, second));
        userMapper.put(UserDO.builder().id(700L).username("captain").status(0).build());
        userGroupMapper.queueSelectCount(2L);
        userGroupMapper.queueSelectCount(0L);

        List<GroupRespVO> groups = proxy.getGroupsByEvent(90L);

        assertThat(groups).hasSize(2);
        assertThat(groups.get(0).getLeadUserName()).isEqualTo("captain");
        assertThat(groups.get(0).getMemberCount()).isEqualTo(2);
        assertThat(groups.get(1).getMemberCount()).isZero();
    }

    @Test
    void getAllUserProfiles_delegatesToUserService() {
        UserProfileRespVO profile = new UserProfileRespVO();
        profile.setId(1L);
        profile.setName("Alice");
        userService.setProfiles(List.of(profile));

        assertThat(proxy.getAllUserProfiles()).containsExactly(profile);
    }

    // ---------------------------------------------------------------------
    // Helper stubs
    // ---------------------------------------------------------------------

    private abstract static class BaseMapperAdapter<T> implements BaseMapper<T> {
        protected RuntimeException unsupported() {
            return new UnsupportedOperationException("Not implemented");
        }

        @Override
        public int deleteById(Serializable id) {
            throw unsupported();
        }

        @Override
        public int deleteById(T entity) {
            throw unsupported();
        }

        @Override
        public int delete(Wrapper<T> queryWrapper) {
            throw unsupported();
        }

        @Override
        public int deleteBatchIds(Collection<?> idList) {
            throw unsupported();
        }

        @Override
        public int insert(T entity) {
            throw unsupported();
        }

        @Override
        public int updateById(T entity) {
            throw unsupported();
        }

        @Override
        public int update(T entity, Wrapper<T> updateWrapper) {
            throw unsupported();
        }

        @Override
        public T selectById(Serializable id) {
            throw unsupported();
        }

        @Override
        public List<T> selectBatchIds(Collection<? extends Serializable> idList) {
            throw unsupported();
        }

        @Override
        public void selectBatchIds(
                Collection<? extends Serializable> idList, ResultHandler<T> resultHandler) {
            throw unsupported();
        }

        @Override
        public T selectOne(Wrapper<T> queryWrapper) {
            throw unsupported();
        }

        @Override
        public Long selectCount(Wrapper<T> queryWrapper) {
            throw unsupported();
        }

        @Override
        public List<T> selectList(Wrapper<T> queryWrapper) {
            throw unsupported();
        }

        @Override
        public void selectList(Wrapper<T> queryWrapper, ResultHandler<T> resultHandler) {
            throw unsupported();
        }

        @Override
        public List<T> selectList(IPage<T> page, Wrapper<T> queryWrapper) {
            throw unsupported();
        }

        @Override
        public void selectList(
                IPage<T> page, Wrapper<T> queryWrapper, ResultHandler<T> resultHandler) {
            throw unsupported();
        }

        @Override
        public List<Map<String, Object>> selectMaps(Wrapper<T> queryWrapper) {
            throw unsupported();
        }

        @Override
        public void selectMaps(
                Wrapper<T> queryWrapper, ResultHandler<Map<String, Object>> resultHandler) {
            throw unsupported();
        }

        @Override
        public List<Map<String, Object>> selectMaps(
                IPage<? extends Map<String, Object>> page, Wrapper<T> queryWrapper) {
            throw unsupported();
        }

        @Override
        public void selectMaps(
                IPage<? extends Map<String, Object>> page,
                Wrapper<T> queryWrapper,
                ResultHandler<Map<String, Object>> resultHandler) {
            throw unsupported();
        }

        @Override
        public <E> List<E> selectObjs(Wrapper<T> queryWrapper) {
            throw unsupported();
        }

        @Override
        public <E> void selectObjs(Wrapper<T> queryWrapper, ResultHandler<E> resultHandler) {
            throw unsupported();
        }

        @Override
        public <P extends IPage<T>> P selectPage(P page, Wrapper<T> queryWrapper) {
            throw unsupported();
        }

        @Override
        public <P extends IPage<Map<String, Object>>> P selectMapsPage(
                P page, Wrapper<T> queryWrapper) {
            throw unsupported();
        }
    }

    private static final class FakeDeptMapper extends BaseMapperAdapter<DeptDO>
            implements DeptMapper {
        private final Map<Long, DeptDO> store = new LinkedHashMap<>();
        private final Deque<DeptDO> selectOneQueue = new ArrayDeque<>();
        private List<DeptDO> selectList = new ArrayList<>();
        private long seq = 1;

        Map<Long, DeptDO> store() {
            return store;
        }

        void put(DeptDO dept) {
            if (dept.getId() == null) {
                dept.setId(seq++);
            }
            store.put(dept.getId(), dept);
        }

        void setSelectListResult(List<DeptDO> result) {
            this.selectList = new ArrayList<>(result);
        }

        void enqueueSelectOne(DeptDO value) {
            selectOneQueue.add(value);
        }

        @Override
        public DeptDO selectById(Serializable id) {
            return store.get(id);
        }

        @Override
        public DeptDO selectOne(Wrapper<DeptDO> queryWrapper) {
            return selectOneQueue.isEmpty() ? null : selectOneQueue.removeFirst();
        }

        @Override
        public int insert(DeptDO entity) {
            put(entity);
            return 1;
        }

        @Override
        public int updateById(DeptDO entity) {
            store.put(entity.getId(), entity);
            return 1;
        }

        @Override
        public int update(DeptDO entity, Wrapper<DeptDO> updateWrapper) {
            return 1;
        }

        @Override
        public List<DeptDO> selectList(Wrapper<DeptDO> queryWrapper) {
            return new ArrayList<>(selectList);
        }
    }

    private static final class FakeUserMapper extends BaseMapperAdapter<UserDO>
            implements UserMapper {
        private final Map<Long, UserDO> store = new LinkedHashMap<>();
        private long seq = 1;
        UpdateWrapper<UserDO> lastUpdateWrapper;

        void put(UserDO user) {
            if (user.getId() == null) {
                user.setId(seq++);
            }
            store.put(user.getId(), user);
        }

        @Override
        public UserDO selectById(Serializable id) {
            return store.get(id);
        }

        @Override
        public List<UserDO> selectBatchIds(Collection<? extends Serializable> idList) {
            List<UserDO> result = new ArrayList<>();
            for (Serializable id : idList) {
                UserDO user = store.get(id);
                if (user != null) {
                    result.add(user);
                }
            }
            return result;
        }

        @Override
        public int insert(UserDO entity) {
            put(entity);
            return 1;
        }

        @Override
        public int update(UserDO entity, Wrapper<UserDO> updateWrapper) {
            this.lastUpdateWrapper = (UpdateWrapper<UserDO>) updateWrapper;
            return 1;
        }

        @Override
        public UserRoleDTO selectUserWithRole(Long userId) {
            return null;
        }

        @Override
        public UserDO selectRawById(Long id) {
            return store.get(id);
        }

        @Override
        public List<UserRoleDTO> selectAllUsersWithRoles() {
            throw unsupported();
        }

        @Override
        public UserDO selectByUsername(String username) {
            throw unsupported();
        }

        @Override
        public List<UserPermissionDTO> selectUserWithPermission(Long userId) {
            throw unsupported();
        }

        @Override
        public UserDO selectByIdWithoutTenant(Long id) {
            return selectById(id);
        }

        @Override
        public Integer updateByIdWithoutTenant(UserDO userDO) {
            return 1;
        }
    }

    private static final class FakeEventMapper extends BaseMapperAdapter<EventDO>
            implements EventMapper {
        private final Map<Long, EventDO> store = new LinkedHashMap<>();
        private long seq = 1;

        void put(EventDO event) {
            if (event.getId() == null) {
                event.setId(seq++);
            }
            store.put(event.getId(), event);
        }

        @Override
        public EventDO selectById(Serializable id) {
            return store.get(id);
        }

        @Override
        public int insert(EventDO entity) {
            put(entity);
            return 1;
        }

        @Override
        public EventDO selectRawById(Long id) {
            return store.get(id);
        }

        @Override
        public int restoreById(Long id) {
            return 0;
        }
    }

    private static final class FakeUserGroupMapper extends BaseMapperAdapter<UserGroupDO>
            implements UserGroupMapper {
        private final Map<Long, UserGroupDO> store = new LinkedHashMap<>();
        private final Deque<UserGroupDO> selectOneQueue = new ArrayDeque<>();
        private final Deque<Long> selectCountQueue = new ArrayDeque<>();
        private List<UserGroupDO> selectList = new ArrayList<>();
        private long seq = 1;

        Collection<UserGroupDO> relations() {
            return store.values();
        }

        private int nullSelectOneCount = 0;

        void enqueueSelectOne(UserGroupDO value) {
            if (value == null) {
                nullSelectOneCount++;
            } else {
                selectOneQueue.add(value);
            }
        }

        void setSelectListResult(List<UserGroupDO> list) {
            selectList = new ArrayList<>(list);
        }

        void queueSelectCount(long count) {
            selectCountQueue.add(count);
        }

        @Override
        public int insert(UserGroupDO entity) {
            if (entity.getId() == null) {
                entity.setId(seq++);
            }
            store.put(entity.getId(), entity);
            return 1;
        }

        @Override
        public UserGroupDO selectOne(Wrapper<UserGroupDO> queryWrapper) {
            if (nullSelectOneCount > 0) {
                nullSelectOneCount--;
                return null;
            }
            return selectOneQueue.isEmpty() ? null : selectOneQueue.removeFirst();
        }

        @Override
        public List<UserGroupDO> selectList(Wrapper<UserGroupDO> queryWrapper) {
            return new ArrayList<>(selectList);
        }

        @Override
        public Long selectCount(Wrapper<UserGroupDO> queryWrapper) {
            return selectCountQueue.isEmpty() ? 0L : selectCountQueue.removeFirst();
        }

        @Override
        public int deleteById(Serializable id) {
            return store.remove(id) != null ? 1 : 0;
        }

        @Override
        public int restoreByEventId(Long eventId) {
            return 0;
        }
    }

    private static final class FakeTaskMapper extends BaseMapperAdapter<TaskDO>
            implements TaskMapper {
        private final Deque<Long> selectCountQueue = new ArrayDeque<>();

        void queueSelectCount(long value) {
            selectCountQueue.add(value);
        }

        @Override
        public Long selectCount(Wrapper<TaskDO> queryWrapper) {
            return selectCountQueue.isEmpty() ? 0L : selectCountQueue.removeFirst();
        }
    }

    private static final class StubUserService implements UserService {
        private List<UserProfileRespVO> profiles = new ArrayList<>();

        void setProfiles(List<UserProfileRespVO> profiles) {
            this.profiles = new ArrayList<>(profiles);
        }

        @Override
        public UserDO getUserByUsername(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isPasswordMatch(String rawPassword, String encodedPassword) {
            throw new UnsupportedOperationException();
        }

        @Override
        public nus.edu.u.system.domain.dto.UserRoleDTO selectUserWithRole(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserDO selectUserById(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long createUserWithRoleIds(nus.edu.u.system.domain.dto.CreateUserDTO dto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserDO updateUserWithRoleIds(nus.edu.u.system.domain.dto.UpdateUserDTO dto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void softDeleteUser(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void restoreUser(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void disableUser(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void enableUser(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UserProfileRespVO> getAllUserProfiles() {
            return new ArrayList<>(profiles);
        }

        @Override
        public nus.edu.u.system.domain.vo.user.BulkUpsertUsersRespVO bulkUpsertUsers(
                List<nus.edu.u.system.domain.dto.CreateUserDTO> rawRows) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tryCreateOrFallbackToUpdate(
                String email, String remark, List<Long> roleIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Long> getAliveRoleIdsByUserId(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UserProfileRespVO> getEnabledUserProfiles() {
            return new ArrayList<>(profiles);
        }
    }
}
