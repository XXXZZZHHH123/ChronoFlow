package nus.edu.u.system.service.task.action;

import static nus.edu.u.common.constant.PermissionConstants.ASSIGN_TASK;
import static nus.edu.u.common.constant.PermissionConstants.UPDATE_TASK;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.*;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dto.TaskActionDTO;
import nus.edu.u.system.domain.vo.file.FileResultVO;
import nus.edu.u.system.domain.vo.file.FileUploadReqVO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.service.file.FileStorageService;
import nus.edu.u.system.service.task.TaskLogService;
import nus.edu.u.system.service.task.action.strategy.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class TaskActionStrategyTest {

    private AcceptTask acceptTask;
    private ApproveTask approveTask;
    private AssignTask assignTask;
    private BlockTask blockTask;
    private CreateTask createTask;
    private DeleteTask deleteTask;
    private RejectTask rejectTask;
    private SubmitTask submitTask;
    private UpdateTask updateTask;
    private FakeTaskMapper taskMapper;
    private RecordingTaskLogService taskLogService;
    private RecordingFileStorageService fileStorageService;
    private TestStpLogic stpLogic;
    private StpLogic previousLogic;

    @BeforeEach
    void setUp() throws Exception {
        acceptTask = new AcceptTask();
        approveTask = new ApproveTask();
        assignTask = new AssignTask();
        blockTask = new BlockTask();
        createTask = new CreateTask();
        deleteTask = new DeleteTask();
        rejectTask = new RejectTask();
        submitTask = new SubmitTask();
        updateTask = new UpdateTask();

        taskMapper = new FakeTaskMapper();
        taskLogService = new RecordingTaskLogService();
        fileStorageService = new RecordingFileStorageService();

        stpLogic = new TestStpLogic();
        previousLogic = StpUtil.getStpLogic();
        StpUtil.setStpLogic(stpLogic);

        injectDependencies(acceptTask);
        injectDependencies(approveTask);
        injectDependencies(assignTask);
        injectDependencies(blockTask);
        injectDependencies(createTask);
        injectDependencies(deleteTask);
        injectDependencies(rejectTask);
        injectDependencies(submitTask);
        injectDependencies(updateTask);
    }

    @AfterEach
    void tearDown() {
        StpUtil.setStpLogic(previousLogic);
    }

    private void injectDependencies(AbstractTaskStrategy strategy) throws Exception {
        Field field = AbstractTaskStrategy.class.getDeclaredField("taskMapper");
        field.setAccessible(true);
        field.set(strategy, taskMapper);

        field = AbstractTaskStrategy.class.getDeclaredField("taskLogService");
        field.setAccessible(true);
        field.set(strategy, taskLogService);

        field = AbstractTaskStrategy.class.getDeclaredField("fileStorageService");
        field.setAccessible(true);
        field.set(strategy, fileStorageService);
    }

    @Test
    void acceptTask_updatesStatusWhenOwnerExecutes() {
        stpLogic.setLoginId(5L);
        TaskDO task =
                baseTask()
                        .id(1L)
                        .userId(5L)
                        .status(TaskStatusEnum.PENDING.getStatus())
                        .startTime(now())
                        .endTime(now().plusHours(2))
                        .build();
        TaskActionDTO dto =
                TaskActionDTO.builder()
                        .startTime(task.getStartTime())
                        .endTime(task.getEndTime())
                        .eventStartTime(task.getStartTime().minusHours(1))
                        .eventEndTime(task.getEndTime().plusHours(1))
                        .remark("accept")
                        .build();

        acceptTask.execute(task, dto);

        assertThat(task.getStatus()).isEqualTo(TaskStatusEnum.PROGRESS.getStatus());
        assertThat(taskMapper.updatedTasks).contains(task);
    }

    @Test
    void acceptTask_whenUserMismatchThrows() {
        stpLogic.setLoginId(42L);
        TaskDO task =
                baseTask().id(2L).userId(7L).status(TaskStatusEnum.PENDING.getStatus()).build();

        assertThatThrownBy(() -> acceptTask.execute(task, TaskActionDTO.builder().build()))
                .extracting("code")
                .isEqualTo(MODIFY_OTHER_TASK_ERROR.getCode());
    }

    @Test
    void approveTask_needsPermissionAndCreator() {
        stpLogic.setLoginId(9L);
        stpLogic.setPermissions(UPDATE_TASK);
        TaskDO task =
                baseTask()
                        .id(3L)
                        .status(TaskStatusEnum.PENDING_APPROVAL.getStatus())
                        .startTime(now())
                        .endTime(now().plusHours(2))
                        .build();
        task.setCreator("9");
        TaskActionDTO dto =
                TaskActionDTO.builder()
                        .eventStartTime(now().minusHours(1))
                        .eventEndTime(now().plusHours(3))
                        .remark("approve")
                        .build();

        approveTask.execute(task, dto);

        assertThat(task.getStatus()).isEqualTo(TaskStatusEnum.COMPLETED.getStatus());
    }

    @Test
    void approveTask_wrongStatusThrows() {
        stpLogic.setLoginId(9L);
        stpLogic.setPermissions(UPDATE_TASK);
        TaskDO task = baseTask().id(4L).status(TaskStatusEnum.PENDING.getStatus()).build();
        task.setCreator("9");

        assertThatThrownBy(() -> approveTask.execute(task, TaskActionDTO.builder().build()))
                .extracting("code")
                .isEqualTo(MODIFY_WRONG_TASK_STATUS.getCode());
    }

    @Test
    void assignTask_updatesAssigneeAndUploadsFiles() {
        stpLogic.setPermissions(ASSIGN_TASK);
        TaskDO task =
                baseTask()
                        .id(5L)
                        .eventId(100L)
                        .status(TaskStatusEnum.PROGRESS.getStatus())
                        .startTime(now())
                        .endTime(now().plusDays(1))
                        .build();
        MultipartFile attachment =
                new MockMultipartFile("files", "spec.txt", "text/plain", "spec".getBytes());
        TaskActionDTO dto =
                TaskActionDTO.builder()
                        .targetUserId(99L)
                        .eventStartTime(now().minusHours(1))
                        .eventEndTime(now().plusDays(2))
                        .files(List.of(attachment))
                        .remark("assign")
                        .build();

        assignTask.execute(task, dto);

        assertThat(task.getUserId()).isEqualTo(99L);
        assertThat(task.getStatus()).isEqualTo(TaskStatusEnum.PENDING.getStatus());
        assertThat(fileStorageService.requests).hasSize(1);
    }

    @Test
    void assignTask_updateFailureThrows() {
        stpLogic.setPermissions(ASSIGN_TASK);
        taskMapper.enqueueUpdateResult(0);
        TaskDO task = baseTask().id(6L).startTime(now()).endTime(now().plusHours(4)).build();
        TaskActionDTO dto =
                TaskActionDTO.builder()
                        .eventStartTime(now().minusHours(1))
                        .eventEndTime(now().plusHours(5))
                        .targetUserId(1L)
                        .build();

        assertThatThrownBy(() -> assignTask.execute(task, dto))
                .extracting("code")
                .isEqualTo(ASSIGN_TASK_FAILED.getCode());
    }

    @Test
    void blockTask_requiresProgressStatus() {
        TaskDO task =
                baseTask()
                        .id(7L)
                        .status(TaskStatusEnum.PROGRESS.getStatus())
                        .eventId(5L)
                        .startTime(now())
                        .endTime(now().plusHours(2))
                        .build();
        TaskActionDTO dto =
                TaskActionDTO.builder()
                        .eventStartTime(now().minusHours(1))
                        .eventEndTime(now().plusHours(3))
                        .files(
                                List.of(
                                        new MockMultipartFile(
                                                "f", "log.txt", "text/plain", "log".getBytes())))
                        .build();

        blockTask.execute(task, dto);

        assertThat(task.getStatus()).isEqualTo(TaskStatusEnum.BLOCKED.getStatus());
    }

    @Test
    void blockTask_wrongStatusThrows() {
        TaskDO task = baseTask().id(8L).status(TaskStatusEnum.PENDING.getStatus()).build();
        assertThatThrownBy(() -> blockTask.execute(task, TaskActionDTO.builder().build()))
                .extracting("code")
                .isEqualTo(MODIFY_WRONG_TASK_STATUS.getCode());
    }

    @Test
    void createTask_insertsTaskAndUploadsFiles() {
        MultipartFile file =
                new MockMultipartFile("files", "plan.txt", "text/plain", "plan".getBytes());
        TaskDO task =
                baseTask().id(9L).eventId(90L).startTime(now()).endTime(now().plusHours(5)).build();
        TaskActionDTO dto =
                TaskActionDTO.builder()
                        .targetUserId(2L)
                        .files(List.of(file))
                        .eventStartTime(now().minusHours(1))
                        .eventEndTime(now().plusHours(6))
                        .remark("create")
                        .build();

        createTask.execute(task, dto);

        assertThat(taskMapper.insertedTasks).contains(task);
        assertThat(fileStorageService.requests).hasSize(1);
    }

    @Test
    void deleteTask_successAndFailure() {
        TaskDO task = baseTask().id(10L).build();
        deleteTask.execute(task, TaskActionDTO.builder().build());
        assertThat(taskMapper.deletedIds).containsExactly(10L);

        taskMapper.enqueueDeleteResult(0);
        assertThatThrownBy(() -> deleteTask.execute(task, TaskActionDTO.builder().build()))
                .extracting("code")
                .isEqualTo(TASK_DELETE_FAILED.getCode());
    }

    @Test
    void rejectTask_updatesStatusOrFailsForUnauthorizedUser() {
        stpLogic.setLoginId(11L);
        TaskDO task =
                baseTask()
                        .id(11L)
                        .userId(11L)
                        .status(TaskStatusEnum.PENDING.getStatus())
                        .startTime(now())
                        .endTime(now().plusHours(4))
                        .build();
        TaskActionDTO dto =
                TaskActionDTO.builder()
                        .eventStartTime(now().minusHours(1))
                        .eventEndTime(now().plusHours(5))
                        .remark("reject")
                        .build();
        rejectTask.execute(task, dto);
        assertThat(task.getStatus()).isEqualTo(TaskStatusEnum.REJECTED.getStatus());

        stpLogic.setLoginId(12L);
        TaskDO other =
                baseTask().id(12L).userId(1L).status(TaskStatusEnum.PENDING.getStatus()).build();
        assertThatThrownBy(() -> rejectTask.execute(other, TaskActionDTO.builder().build()))
                .extracting("code")
                .isEqualTo(MODIFY_OTHER_TASK_ERROR.getCode());
    }

    @Test
    void submitTask_movesToPendingApprovalOrFailsForWrongStatus() {
        stpLogic.setLoginId(13L);
        TaskDO task =
                baseTask()
                        .id(13L)
                        .userId(13L)
                        .eventId(13L)
                        .status(TaskStatusEnum.PROGRESS.getStatus())
                        .startTime(now())
                        .endTime(now().plusHours(6))
                        .build();
        TaskActionDTO dto =
                TaskActionDTO.builder()
                        .eventStartTime(now().minusHours(1))
                        .eventEndTime(now().plusHours(7))
                        .files(
                                List.of(
                                        new MockMultipartFile(
                                                "f",
                                                "output.txt",
                                                "text/plain",
                                                "done".getBytes())))
                        .build();
        submitTask.execute(task, dto);
        assertThat(task.getStatus()).isEqualTo(TaskStatusEnum.PENDING_APPROVAL.getStatus());

        TaskDO wrong =
                baseTask().id(14L).userId(13L).status(TaskStatusEnum.BLOCKED.getStatus()).build();
        assertThatThrownBy(() -> submitTask.execute(wrong, TaskActionDTO.builder().build()))
                .extracting("code")
                .isEqualTo(MODIFY_WRONG_TASK_STATUS.getCode());
    }

    @Test
    void updateTask_appliesFieldsAndUploads() {
        stpLogic.setPermissions(UPDATE_TASK);
        TaskDO task =
                baseTask()
                        .id(15L)
                        .userId(5L)
                        .eventId(1L)
                        .status(TaskStatusEnum.PENDING.getStatus())
                        .name("orig")
                        .description("desc")
                        .startTime(now())
                        .endTime(now().plusHours(2))
                        .build();
        TaskActionDTO dto =
                TaskActionDTO.builder()
                        .targetUserId(6L)
                        .name("new")
                        .description("new-desc")
                        .startTime(now().plusHours(1))
                        .endTime(now().plusHours(3))
                        .eventStartTime(now().minusHours(1))
                        .eventEndTime(now().plusHours(4))
                        .files(
                                List.of(
                                        new MockMultipartFile(
                                                "f",
                                                "proof.txt",
                                                "text/plain",
                                                "proof".getBytes())))
                        .remark("update")
                        .build();

        updateTask.execute(task, dto);

        assertThat(task.getName()).isEqualTo("new");
        assertThat(task.getUserId()).isEqualTo(6L);
        assertThat(fileStorageService.requests).hasSize(1);
    }

    @Test
    void updateTask_whenCompletedThrows() {
        stpLogic.setPermissions(UPDATE_TASK);
        TaskDO task = baseTask().id(16L).status(TaskStatusEnum.COMPLETED.getStatus()).build();
        assertThatThrownBy(() -> updateTask.execute(task, TaskActionDTO.builder().build()))
                .extracting("code")
                .isEqualTo(MODIFY_WRONG_TASK_STATUS.getCode());
    }

    @Test
    void validateTimeRange_enforcesBoundaries() {
        TaskDO task = baseTask().id(17L).status(TaskStatusEnum.PENDING.getStatus()).build();
        LocalDateTime now = now();

        assertThatThrownBy(
                        () ->
                                acceptTask.validateTimeRange(
                                        task,
                                        now.plusHours(1),
                                        now,
                                        now.minusHours(1),
                                        now.plusHours(2)))
                .extracting("code")
                .isEqualTo(TASK_TIME_RANGE_INVALID.getCode());

        assertThatThrownBy(
                        () ->
                                acceptTask.validateTimeRange(
                                        task,
                                        now,
                                        now.plusHours(1),
                                        now.plusHours(1),
                                        now.plusHours(3)))
                .extracting("code")
                .isEqualTo(TASK_TIME_OUTSIDE_EVENT.getCode());

        assertThatThrownBy(
                        () ->
                                acceptTask.validateTimeRange(
                                        task,
                                        now,
                                        now.plusHours(5),
                                        now.minusHours(1),
                                        now.plusHours(2)))
                .extracting("code")
                .isEqualTo(TASK_TIME_OUTSIDE_EVENT.getCode());
        assertThat(task.getStatus()).isEqualTo(TaskStatusEnum.DELAYED.getStatus());
    }

    @Test
    void uploadFiles_handlesEmptyAndMissingInfo() {
        ExposedStrategy exposed = new ExposedStrategy();
        try {
            injectDependencies(exposed);
        } catch (Exception ignored) {
        }

        exposed.uploadFiles(1L, 1L, Collections.emptyList());
        assertThat(fileStorageService.requests).isEmpty();

        List<MultipartFile> files =
                List.of(new MockMultipartFile("f", "doc.txt", "text/plain", "doc".getBytes()));
        assertThatThrownBy(() -> exposed.uploadFiles(null, 1L, files))
                .extracting("code")
                .isEqualTo(TASK_LOG_FILE_FAILED.getCode());
    }

    @Test
    void taskActionFactory_mapsStrategies() {
        TaskStrategy first =
                new TaskStrategy() {
                    @Override
                    public void execute(
                            TaskDO task, TaskActionDTO taskActionDTO, Object... params) {}

                    @Override
                    public TaskActionEnum getType() {
                        return TaskActionEnum.CREATE;
                    }
                };
        TaskStrategy second =
                new TaskStrategy() {
                    @Override
                    public void execute(
                            TaskDO task, TaskActionDTO taskActionDTO, Object... params) {}

                    @Override
                    public TaskActionEnum getType() {
                        return TaskActionEnum.APPROVE;
                    }
                };

        TaskActionFactory factory = new TaskActionFactory(List.of(first, second));
        assertThat(factory.getStrategy(TaskActionEnum.APPROVE)).isSameAs(second);
        assertThat(factory.getStrategy(TaskActionEnum.DELETE)).isNull();
    }

    private TaskDO.TaskDOBuilder baseTask() {
        return TaskDO.builder();
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    private static final class ExposedStrategy extends AbstractTaskStrategy {
        @Override
        public void execute(TaskDO task, TaskActionDTO taskActionDTO, Object... params) {}

        @Override
        public TaskActionEnum getType() {
            return TaskActionEnum.CREATE;
        }
    }

    private static final class FakeTaskMapper extends BaseMapperAdapter<TaskDO>
            implements TaskMapper {
        private final List<TaskDO> updatedTasks = new ArrayList<>();
        private final List<TaskDO> insertedTasks = new ArrayList<>();
        private final List<Long> deletedIds = new ArrayList<>();
        private final Deque<Integer> updateResults = new ArrayDeque<>();
        private final Deque<Integer> deleteResults = new ArrayDeque<>();

        void enqueueUpdateResult(int result) {
            updateResults.add(result);
        }

        void enqueueDeleteResult(int result) {
            deleteResults.add(result);
        }

        @Override
        public int updateById(TaskDO entity) {
            updatedTasks.add(entity);
            return updateResults.isEmpty() ? 1 : updateResults.removeFirst();
        }

        @Override
        public int insert(TaskDO entity) {
            insertedTasks.add(entity);
            return 1;
        }

        @Override
        public int deleteById(Serializable id) {
            deletedIds.add((Long) id);
            return deleteResults.isEmpty() ? 1 : deleteResults.removeFirst();
        }
    }

    private static final class RecordingTaskLogService implements TaskLogService {
        private final List<TaskLogRecord> records = new ArrayList<>();
        private long nextId = 900L;

        @Override
        public Long insertTaskLog(Long taskId, Long targetUserId, Integer action, String remark) {
            records.add(new TaskLogRecord(taskId, targetUserId, action, remark));
            return nextId++;
        }

        @Override
        public List<nus.edu.u.system.domain.vo.task.TaskLogRespVO> getTaskLog(Long taskId) {
            return List.of();
        }
    }

    private static final class RecordingFileStorageService implements FileStorageService {
        private final List<FileUploadReqVO> requests = new ArrayList<>();

        @Override
        public List<FileResultVO> uploadToTaskLog(FileUploadReqVO req) {
            requests.add(req);
            return List.of();
        }

        @Override
        public FileResultVO downloadFile(Long fileId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<FileResultVO> downloadFilesByTaskLogId(Long taskLogId) {
            throw new UnsupportedOperationException();
        }
    }

    private record TaskLogRecord(Long taskId, Long targetUserId, Integer action, String remark) {}

    private static final class TestStpLogic extends StpLogic {
        private final SaSession session = new SaSession("test");
        private Long loginId = 0L;
        private final Set<String> permissions = new HashSet<>();

        private TestStpLogic() {
            super(StpUtil.TYPE);
        }

        void setLoginId(Long loginId) {
            this.loginId = loginId;
        }

        void setPermissions(String... perms) {
            permissions.clear();
            permissions.addAll(Arrays.asList(perms));
        }

        @Override
        public Object getLoginId() {
            return loginId;
        }

        @Override
        public SaSession getSession() {
            return session;
        }

        @Override
        public void checkPermission(String permission) {
            if (!permissions.contains(permission)) {
                throw new RuntimeException("missing permission");
            }
        }
    }

    private abstract static class BaseMapperAdapter<T> implements BaseMapper<T> {
        protected RuntimeException unsupported() {
            return new UnsupportedOperationException();
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
                Collection<? extends Serializable> idList,
                org.apache.ibatis.session.ResultHandler<T> resultHandler) {
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
        public void selectList(
                Wrapper<T> queryWrapper, org.apache.ibatis.session.ResultHandler<T> resultHandler) {
            throw unsupported();
        }

        @Override
        public List<T> selectList(IPage<T> page, Wrapper<T> queryWrapper) {
            throw unsupported();
        }

        @Override
        public void selectList(
                IPage<T> page,
                Wrapper<T> queryWrapper,
                org.apache.ibatis.session.ResultHandler<T> resultHandler) {
            throw unsupported();
        }

        @Override
        public List<Map<String, Object>> selectMaps(Wrapper<T> queryWrapper) {
            throw unsupported();
        }

        @Override
        public void selectMaps(
                Wrapper<T> queryWrapper,
                org.apache.ibatis.session.ResultHandler<Map<String, Object>> resultHandler) {
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
                org.apache.ibatis.session.ResultHandler<Map<String, Object>> resultHandler) {
            throw unsupported();
        }

        @Override
        public <E> List<E> selectObjs(Wrapper<T> queryWrapper) {
            throw unsupported();
        }

        @Override
        public <E> void selectObjs(
                Wrapper<T> queryWrapper, org.apache.ibatis.session.ResultHandler<E> resultHandler) {
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
}
