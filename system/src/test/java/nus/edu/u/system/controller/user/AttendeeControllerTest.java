package nus.edu.u.system.controller.user;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import nus.edu.u.system.domain.vo.attendee.AttendeeInfoRespVO;
import nus.edu.u.system.domain.vo.attendee.AttendeeQrCodeRespVO;
import nus.edu.u.system.domain.vo.attendee.AttendeeReqVO;
import nus.edu.u.system.domain.vo.checkin.CheckInReqVO;
import nus.edu.u.system.domain.vo.checkin.CheckInRespVO;
import nus.edu.u.system.domain.vo.checkin.GenerateQrCodesReqVO;
import nus.edu.u.system.domain.vo.checkin.GenerateQrCodesRespVO;
import nus.edu.u.system.service.attendee.AttendeeService;
import nus.edu.u.system.service.excel.ExcelService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AttendeeControllerTest {

    @Mock private AttendeeService attendeeService;
    @Mock private ExcelService excelService;
    @InjectMocks private AttendeeController attendeeController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(attendeeController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void list_returnsQrCodes() throws Exception {
        AttendeeQrCodeRespVO respVO =
                AttendeeQrCodeRespVO.builder().id(3L).attendeeName("Amy").build();
        when(attendeeService.list(1L)).thenReturn(List.of(respVO));

        mockMvc.perform(get("/system/attendee/list/{eventId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].attendeeName").value("Amy"));

        verify(attendeeService).list(1L);
    }

    @Test
    void generateQrCodes_invokesService() throws Exception {
        AttendeeReqVO attendeeReqVO = new AttendeeReqVO();
        attendeeReqVO.setEmail("a@example.com");
        attendeeReqVO.setName("Alice");
        attendeeReqVO.setMobile("12345678");

        GenerateQrCodesReqVO reqVO = new GenerateQrCodesReqVO();
        reqVO.setEventId(6L);
        reqVO.setAttendees(List.of(attendeeReqVO));

        GenerateQrCodesRespVO respVO =
                GenerateQrCodesRespVO.builder().eventId(6L).totalCount(1).build();
        when(attendeeService.generateQrCodesForAttendees(any())).thenReturn(respVO);

        mockMvc.perform(
                        post("/system/attendee")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventId").value(6));

        verify(attendeeService).generateQrCodesForAttendees(any(GenerateQrCodesReqVO.class));
    }

    @Test
    void generateQrCodeByExcel_createsRequestFromFile() throws Exception {
        AttendeeReqVO attendeeReqVO = new AttendeeReqVO();
        attendeeReqVO.setEmail("bulk@example.com");
        attendeeReqVO.setName("Bulk");
        attendeeReqVO.setMobile("87654321");

        when(excelService.importAttendees(any())).thenReturn(List.of(attendeeReqVO));
        when(attendeeService.generateQrCodesForAttendees(any()))
                .thenReturn(GenerateQrCodesRespVO.builder().eventId(9L).build());

        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "attendees.xlsx", "application/vnd.ms-excel", new byte[] {1});

        mockMvc.perform(multipart("/system/attendee/bulk/{eventId}", 9L).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventId").value(9));

        ArgumentCaptor<GenerateQrCodesReqVO> captor =
                ArgumentCaptor.forClass(GenerateQrCodesReqVO.class);
        verify(attendeeService).generateQrCodesForAttendees(captor.capture());
        Assertions.assertThat(captor.getValue().getEventId()).isEqualTo(9L);
        Assertions.assertThat(captor.getValue().getAttendees()).hasSize(1);
    }

    @Test
    void attendeePreview_appendsFriendlyMessage() throws Exception {
        AttendeeInfoRespVO infoRespVO =
                AttendeeInfoRespVO.builder().attendeeEmail("info@example.com").build();
        when(attendeeService.getAttendeeInfo("token123")).thenReturn(infoRespVO);

        mockMvc.perform(get("/system/attendee/scan").param("token", "token123"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.message")
                                .value(
                                        "please show this QR code to the on-site staff for scanning"));
    }

    @Test
    void checkIn_translatesRequestBody() throws Exception {
        CheckInRespVO respVO = CheckInRespVO.builder().success(true).build();
        when(attendeeService.checkIn("abc")).thenReturn(respVO);

        CheckInReqVO reqVO = new CheckInReqVO();
        reqVO.setToken("abc");

        mockMvc.perform(
                        post("/system/attendee/staff-scan")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));

        verify(attendeeService).checkIn("abc");
    }

    @Test
    void update_returnsUpdatedQrCode() throws Exception {
        AttendeeReqVO reqVO = new AttendeeReqVO();
        reqVO.setEmail("update@example.com");
        reqVO.setName("Update");
        reqVO.setMobile("12312312");

        AttendeeQrCodeRespVO respVO =
                AttendeeQrCodeRespVO.builder().id(2L).attendeeEmail("update@example.com").build();
        when(attendeeService.update(eq(4L), any(AttendeeReqVO.class))).thenReturn(respVO);

        mockMvc.perform(
                        patch("/system/attendee/{attendeeId}", 4L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.attendeeEmail").value("update@example.com"));
    }

    @Test
    void delete_removesAttendee() throws Exception {
        mockMvc.perform(delete("/system/attendee/{attendeeId}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        verify(attendeeService).delete(5L);
    }
}
