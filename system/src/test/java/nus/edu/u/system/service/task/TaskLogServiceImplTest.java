package nus.edu.u.system.service.task;

import static org.assertj.core.api.Assertions.*;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.task.TaskLogDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.vo.file.FileResultVO;
import nus.edu.u.system.domain.vo.file.FileUploadReqVO;
import nus.edu.u.system.domain.vo.task.TaskLogRespVO;
import nus.edu.u.system.mapper.task.TaskLogMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.service.file.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskLogServiceImplTest {

    private TaskLogServiceImpl taskLogService;
    private RecordingTaskLogMapper taskLogMapper;
    private RecordingUserMapper userMapper;
    private RecordingFileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        taskLogService = new TaskLogServiceImpl();
        taskLogMapper = new RecordingTaskLogMapper();
        userMapper = new RecordingUserMapper();
        fileStorageService = new RecordingFileStorageService();

        setField("taskLogMapper", taskLogMapper);
        setField("userMapper", userMapper);
        setField("fileStorageService", fileStorageService);
    }

    @Test
    void insertTaskLog_success() {
        Long id = taskLogService.insertTaskLog(1L, 2L, 3, "remark");

        assertThat(id).isEqualTo(1L);
        assertThat(taskLogMapper.inserted).hasSize(1);
    }

    @Test
    void insertTaskLog_failureThrows() {
        taskLogMapper.nextInsertResult = 0;

        assertThatThrownBy(() -> taskLogService.insertTaskLog(1L, null, 3, null))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void getTaskLog_returnsEnrichedResponse() {
        TaskLogDO log1 =
                TaskLogDO.builder()
                        .id(10L)
                        .taskId(1L)
                        .targetUserId(5L)
                        .action(7)
                        .remark("first")
                        .build();
        log1.setCreator("8");
        log1.setCreateTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        TaskLogDO log2 =
                TaskLogDO.builder()
                        .id(11L)
                        .taskId(1L)
                        .targetUserId(null)
                        .action(8)
                        .remark("second")
                        .build();
        log2.setCreator("5");
        log2.setCreateTime(LocalDateTime.of(2025, 1, 2, 12, 0));
        taskLogMapper.logs.addAll(List.of(log1, log2));

        userMapper.users.put(5L, user(5L, "target"));
        userMapper.users.put(8L, user(8L, "creator"));

        fileStorageService.filesByLogId.put(10L, List.of(file("f1")));
        fileStorageService.filesByLogId.put(11L, List.of());

        List<TaskLogRespVO> logs = taskLogService.getTaskLog(1L);

        assertThat(logs).hasSize(2);
        TaskLogRespVO first = logs.get(0);
        assertThat(first.getTargetUser().getName()).isEqualTo("target");
        assertThat(first.getSourceUser().getName()).isEqualTo("creator");
        assertThat(first.getFileResults()).hasSize(1);

        TaskLogRespVO second = logs.get(1);
        assertThat(second.getTargetUser()).isNull();
        assertThat(second.getSourceUser().getId()).isEqualTo(5L);
    }

    @Test
    void getTaskLog_whenNoLogsReturnsEmpty() {
        assertThat(taskLogService.getTaskLog(99L)).isEmpty();
    }

    private static UserDO user(Long id, String name) {
        return UserDO.builder().id(id).username(name).email(name + "@example.com").build();
    }

    private static FileResultVO file(String name) {
        FileResultVO vo = new FileResultVO();
        vo.setObjectName(name);
        return vo;
    }

    private void setField(String name, Object value) {
        try {
            Field field = TaskLogServiceImpl.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(taskLogService, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class RecordingTaskLogMapper implements TaskLogMapper {
        private final List<TaskLogDO> inserted = new ArrayList<>();
        private final List<TaskLogDO> logs = new ArrayList<>();
        private int nextInsertResult = 1;

        @Override
        public int insert(TaskLogDO entity) {
            if (nextInsertResult <= 0) {
                return 0;
            }
            inserted.add(entity);
            if (entity.getId() == null) {
                entity.setId((long) inserted.size());
            }
            return nextInsertResult;
        }

        @Override
        public List<TaskLogDO> selectList(Wrapper<TaskLogDO> queryWrapper) {
            return new ArrayList<>(logs);
        }

        @Override
        public int deleteById(Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteById(TaskLogDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int delete(Wrapper<TaskLogDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteBatchIds(Collection<?> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateById(TaskLogDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(TaskLogDO entity, Wrapper<TaskLogDO> updateWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskLogDO selectById(Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TaskLogDO> selectBatchIds(Collection<? extends Serializable> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectBatchIds(
                Collection<? extends Serializable> idList,
                org.apache.ibatis.session.ResultHandler<TaskLogDO> resultHandler) {}

        @Override
        public TaskLogDO selectOne(Wrapper<TaskLogDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long selectCount(Wrapper<TaskLogDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(
                Wrapper<TaskLogDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<TaskLogDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TaskLogDO> selectList(IPage<TaskLogDO> page, Wrapper<TaskLogDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(
                IPage<TaskLogDO> page,
                Wrapper<TaskLogDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<TaskLogDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Map<String, Object>> selectMaps(Wrapper<TaskLogDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(
                Wrapper<TaskLogDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<Map<String, Object>> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Map<String, Object>> selectMaps(
                IPage<? extends Map<String, Object>> page, Wrapper<TaskLogDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(
                IPage<? extends Map<String, Object>> page,
                Wrapper<TaskLogDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<Map<String, Object>> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> List<E> selectObjs(Wrapper<TaskLogDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> void selectObjs(
                Wrapper<TaskLogDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<E> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <P extends IPage<TaskLogDO>> P selectPage(P page, Wrapper<TaskLogDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <P extends IPage<Map<String, Object>>> P selectMapsPage(
                P page, Wrapper<TaskLogDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingUserMapper implements UserMapper {
        private final Map<Long, UserDO> users = new HashMap<>();

        @Override
        public List<UserDO> selectBatchIds(Collection<? extends Serializable> idList) {
            List<UserDO> list = new ArrayList<>();
            Set<Long> seen = new HashSet<>();
            for (Serializable id : idList) {
                Long longId = (Long) id;
                if (seen.add(longId)) {
                    UserDO u = users.get(longId);
                    if (u != null) {
                        list.add(u);
                    }
                }
            }
            return list;
        }

        @Override
        public void selectBatchIds(
                Collection<? extends Serializable> idList,
                org.apache.ibatis.session.ResultHandler<UserDO> resultHandler) {}

        @Override
        public UserDO selectById(Serializable id) {
            return users.get(id);
        }

        @Override
        public int insert(UserDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long selectCount(Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserDO selectOne(Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteById(Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteById(UserDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int delete(Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteBatchIds(Collection<?> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateById(UserDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(UserDO entity, Wrapper<UserDO> updateWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UserDO> selectList(Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(
                Wrapper<UserDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<UserDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UserDO> selectList(IPage<UserDO> page, Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(
                IPage<UserDO> page,
                Wrapper<UserDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<UserDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Map<String, Object>> selectMaps(Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(
                Wrapper<UserDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<Map<String, Object>> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Map<String, Object>> selectMaps(
                IPage<? extends Map<String, Object>> page, Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(
                IPage<? extends Map<String, Object>> page,
                Wrapper<UserDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<Map<String, Object>> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> List<E> selectObjs(Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> void selectObjs(
                Wrapper<UserDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<E> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <P extends IPage<UserDO>> P selectPage(P page, Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <P extends IPage<Map<String, Object>>> P selectMapsPage(
                P page, Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        // unused custom methods
        @Override
        public nus.edu.u.system.domain.dto.UserRoleDTO selectUserWithRole(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserDO selectRawById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<nus.edu.u.system.domain.dto.UserRoleDTO> selectAllUsersWithRoles() {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserDO selectByUsername(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<nus.edu.u.system.domain.dto.UserPermissionDTO> selectUserWithPermission(
                Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserDO selectByIdWithoutTenant(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Integer updateByIdWithoutTenant(UserDO userDO) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsUsername(String username, Long excludeId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsEmail(String email, Long excludeId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsPhone(String phone, Long excludeId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long selectIdByEmail(String email) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Set<String> selectExistingEmails(java.util.Collection<String> emails) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingFileStorageService implements FileStorageService {
        private final Map<Long, List<FileResultVO>> filesByLogId = new HashMap<>();

        @Override
        public List<FileResultVO> uploadToTaskLog(FileUploadReqVO req) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileResultVO downloadFile(Long fileId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<FileResultVO> downloadFilesByTaskLogId(Long taskLogId) {
            return filesByLogId.getOrDefault(taskLogId, List.of());
        }
    }
}
