package nus.edu.u.system.controller.event;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.dev33.satoken.stp.StpUtil;
import java.util.List;
import nus.edu.u.system.domain.vo.file.FileResultVO;
import nus.edu.u.system.domain.vo.task.TaskCreateReqVO;
import nus.edu.u.system.domain.vo.task.TaskDashboardRespVO;
import nus.edu.u.system.domain.vo.task.TaskLogRespVO;
import nus.edu.u.system.domain.vo.task.TaskRespVO;
import nus.edu.u.system.domain.vo.task.TaskUpdateReqVO;
import nus.edu.u.system.service.task.TaskLogService;
import nus.edu.u.system.service.task.TaskService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock private TaskService taskService;
    @Mock private TaskLogService taskLogService;
    @InjectMocks private TaskController taskController;

    private MockMvc mockMvc;
    private MockedStatic<StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController).build();
        stpUtilMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    void createTask_returnsCustomMessage() throws Exception {
        TaskRespVO respVO = new TaskRespVO();
        respVO.setId(3L);
        respVO.setName("Design Deck");
        when(taskService.createTask(eq(5L), any(TaskCreateReqVO.class))).thenReturn(respVO);

        MockMultipartFile file =
                new MockMultipartFile("files", "note.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(
                        multipart("/system/task/{eventId}", 5L)
                                .file(file)
                                .param("name", "Design Deck")
                                .param("targetUserId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("task created successfully"))
                .andExpect(jsonPath("$.data.name").value("Design Deck"));

        ArgumentCaptor<TaskCreateReqVO> captor = ArgumentCaptor.forClass(TaskCreateReqVO.class);
        verify(taskService).createTask(eq(5L), captor.capture());
        Assertions.assertThat(captor.getValue().getFiles()).hasSize(1);
        Assertions.assertThat(captor.getValue().getTargetUserId()).isEqualTo(9L);
    }

    @Test
    void listTasksByEvent_returnsMessage() throws Exception {
        TaskRespVO respVO = new TaskRespVO();
        respVO.setId(22L);
        when(taskService.listTasksByEvent(9L)).thenReturn(List.of(respVO));

        mockMvc.perform(get("/system/task/{eventId}", 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("Tasks retrieved successfully"))
                .andExpect(jsonPath("$.data[0].id").value(22));
    }

    @Test
    void dashboard_usesLoggedInMember() throws Exception {
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(88L);
        TaskDashboardRespVO dashboardRespVO = new TaskDashboardRespVO();
        when(taskService.getByMemberId(88L)).thenReturn(dashboardRespVO);

        mockMvc.perform(get("/system/task/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(taskService).getByMemberId(88L);
    }

    @Test
    void updateTask_returnsCustomMessage() throws Exception {
        TaskRespVO respVO = new TaskRespVO();
        respVO.setId(2L);
        when(taskService.updateTask(eq(3L), eq(4L), any(TaskUpdateReqVO.class), eq(1)))
                .thenReturn(respVO);

        MockMultipartHttpServletRequestBuilder builder =
                multipart("/system/task/{eventId}/{taskId}", 3L, 4L);
        builder.with(
                request -> {
                    request.setMethod("PATCH");
                    return request;
                });

        mockMvc.perform(
                        builder.param("name", "Review deck")
                                .param("targetUserId", "7")
                                .param("type", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("task updated successfully"))
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    void deleteTask_returnsMessage() throws Exception {
        mockMvc.perform(delete("/system/task/{eventId}/{taskId}", 6L, 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("task deleted successfully"));

        verify(taskService).deleteTask(6L, 9L);
    }

    @Test
    void getTaskLog_returnsLogEntries() throws Exception {
        TaskLogRespVO logRespVO =
                TaskLogRespVO.builder()
                        .id(1L)
                        .fileResults(
                                List.of(
                                        FileResultVO.builder()
                                                .name("a.txt")
                                                .objectName("obj")
                                                .build()))
                        .build();
        when(taskLogService.getTaskLog(4L)).thenReturn(List.of(logRespVO));

        mockMvc.perform(get("/system/task/{eventId}/log/{taskId}", 1L, 4L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].fileResults[0].name").value("a.txt"));
    }
}
