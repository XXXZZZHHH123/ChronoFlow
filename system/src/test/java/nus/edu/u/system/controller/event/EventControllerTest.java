package nus.edu.u.system.controller.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.List;
import nus.edu.u.system.domain.vo.event.EventCreateReqVO;
import nus.edu.u.system.domain.vo.event.EventGroupRespVO;
import nus.edu.u.system.domain.vo.event.EventRespVO;
import nus.edu.u.system.domain.vo.event.EventUpdateReqVO;
import nus.edu.u.system.domain.vo.event.UpdateEventRespVO;
import nus.edu.u.system.service.event.EventService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock private EventService eventService;

    @InjectMocks private EventController eventController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private MockedStatic<StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        stpUtilMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    void createEvent_setsOrganizerFromContext() throws Exception {
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(42L);
        EventRespVO respVO = new EventRespVO();
        respVO.setId(10L);
        when(eventService.createEvent(any(EventCreateReqVO.class))).thenReturn(respVO);

        EventCreateReqVO reqVO = new EventCreateReqVO();
        reqVO.setEventName("Hackathon");
        reqVO.setStartTime(LocalDateTime.of(2025, 10, 1, 10, 0));
        reqVO.setEndTime(LocalDateTime.of(2025, 10, 1, 18, 0));

        mockMvc.perform(
                        post("/system/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(10));

        ArgumentCaptor<EventCreateReqVO> captor = ArgumentCaptor.forClass(EventCreateReqVO.class);
        verify(eventService).createEvent(captor.capture());
        Assertions.assertThat(captor.getValue().getOrganizerId()).isEqualTo(42L);
    }

    @Test
    void getByOrganizerId_returnsEventsForLoggedInUser() throws Exception {
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(99L);
        EventRespVO respVO = new EventRespVO();
        respVO.setId(1L);
        when(eventService.getByOrganizerId(99L)).thenReturn(List.of(respVO));

        mockMvc.perform(get("/system/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(eventService).getByOrganizerId(99L);
    }

    @Test
    void updateEvent_returnsUpdatedResult() throws Exception {
        UpdateEventRespVO updateRespVO = new UpdateEventRespVO();
        updateRespVO.setId(5L);
        updateRespVO.setEventName("Updated");
        when(eventService.updateEvent(eq(5L), any(EventUpdateReqVO.class)))
                .thenReturn(updateRespVO);

        EventUpdateReqVO reqVO = new EventUpdateReqVO();
        reqVO.setEventName("Updated");

        mockMvc.perform(
                        patch("/system/events/{id}", 5L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.name").value("Updated"));
    }

    @Test
    void assignableMember_returnsGroupMembers() throws Exception {
        EventGroupRespVO.Member member = new EventGroupRespVO.Member(8L, "alice");
        EventGroupRespVO group = new EventGroupRespVO(7L, "Volunteers", List.of(member));
        when(eventService.assignableMember(15L)).thenReturn(List.of(group));

        mockMvc.perform(get("/system/events/{eventId}/assignable-member", 15L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(7))
                .andExpect(jsonPath("$.data[0].members[0].username").value("alice"));
    }

    @Test
    void deleteEvent_returnsBooleanResult() throws Exception {
        when(eventService.deleteEvent(11L)).thenReturn(true);

        mockMvc.perform(delete("/system/events/{id}", 11L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(eventService).deleteEvent(11L);
    }
}
